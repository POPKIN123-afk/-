package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.model.Transaction
import com.example.util.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BankSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages: Array<SmsMessage> = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } catch (e: Exception) {
            Log.e("BankSmsReceiver", "Failed to retrieve SMS messages: ${e.message}")
            return
        }

        val scope = CoroutineScope(Dispatchers.IO)
        for (message in messages) {
            val body = message.messageBody ?: continue
            val sender = message.originatingAddress ?: ""
            Log.d("BankSmsReceiver", "Received SMS from $sender: $body")

            val parsed = SmsParser.parse(body, sender) ?: continue
            Log.d("BankSmsReceiver", "Successfully parsed bank SMS: $parsed")

            scope.launch {
                try {
                    val database = AppDatabase.getDatabase(context)
                    val dao = database.financeDao()

                    // Find card that ends with cardSuffix
                    val cards = dao.getAllCards().first()
                    val targetCard = cards.find { card ->
                        val cleanNum = card.cardNumber.replace(" ", "")
                        cleanNum.endsWith(parsed.cardSuffix)
                    }

                    if (targetCard != null) {
                        // Insert transaction
                        val transaction = Transaction(
                            cardId = targetCard.id,
                            amount = parsed.amount,
                            category = parsed.category,
                            description = parsed.description,
                            isSmsParsed = true
                        )
                        dao.insertTransaction(transaction)

                        if (parsed.balance != null) {
                            // If SMS contained exact balance, use it directly to keep it perfectly in sync with Bank
                            dao.updateCard(targetCard.copy(balance = parsed.balance))
                        } else {
                            // Otherwise adjust relative to transaction amount
                            dao.adjustCardBalance(targetCard.id, parsed.amount)
                        }

                        // Send success notification
                        showNotification(
                            context,
                            "Списание по карте ${targetCard.bankName} *${parsed.cardSuffix}",
                            "${parsed.category}: ${parsed.amount} руб. (${parsed.description})"
                        )
                    } else {
                        // Card not found in DB - save as general transaction but note the suffix
                        val transaction = Transaction(
                            cardId = null,
                            amount = parsed.amount,
                            category = parsed.category,
                            description = "${parsed.bankName} *${parsed.cardSuffix}: ${parsed.description}",
                            isSmsParsed = true
                        )
                        dao.insertTransaction(transaction)

                        showNotification(
                            context,
                            "Транзакция по новой карте *${parsed.cardSuffix}",
                            "Покупка: ${parsed.amount} руб. Добавьте карту в приложении, чтобы связать баланс."
                        )
                    }
                } catch (e: Exception) {
                    Log.e("BankSmsReceiver", "Error processing sms transaction: ${e.message}", e)
                }
            }
        }
    }

    private fun showNotification(context: Context, title: String, messageHex: String) {
        val channelId = "bank_sms_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Банковские транзакции",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Using standard small launcher icon for notifying
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_edit) // fallback standard icon
            .setContentTitle(title)
            .setContentText(messageHex)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
