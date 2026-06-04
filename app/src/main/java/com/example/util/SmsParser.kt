package com.example.util

import android.util.Log
import com.example.data.model.Transaction

data class ParsedBankSms(
    val cardSuffix: String,    // e.g., "5678"
    val amount: Double,        // Negative for expense, positive for reload
    val description: String,   // e.g., "Magnit", "Yandex.Taxi"
    val balance: Double?,      // Balance after transaction
    val category: String,      // Structured category
    val bankName: String       // e.g., "Сбербанк", "Т-Банк", "Альфа-Банк"
)

object SmsParser {
    private const val TAG = "SmsParser"

    // Simple robust regex parsing for different patterns
    fun parse(smsBody: String, sender: String? = null): ParsedBankSms? {
        try {
            val normalized = smsBody.replace("\n", " ").trim()
            val senderUpper = sender?.uppercase() ?: ""

            // 1. Check for T-Bank (formerly Tinkoff) pattern:
            // "Pokupka 500 RUR. Karta *1234. Balans 4500 RUR. Magnit"
            if (senderUpper.contains("TINKOFF") || senderUpper.contains("T-BANK") || normalized.contains("Tinkoff") || normalized.contains("T-Bank")) {
                val cardRegex = """Karta\s*\*(\d{4})""".toRegex(RegexOption.IGNORE_CASE)
                val amountRegex = """(?:Pokupka|Spisanie|Oplata)\s+(\d+(?:\.\d+)?)\s*(?:RUR|RUB|р|руб)""".toRegex(RegexOption.IGNORE_CASE)
                val balanceRegex = """Balans\s+(\d+(?:\.\d+)?)\s*(?:RUR|RUB|р|руб)""".toRegex(RegexOption.IGNORE_CASE)

                val cardSuffix = cardRegex.find(normalized)?.groupValues?.get(1)
                val amountVal = amountRegex.find(normalized)?.groupValues?.get(1)?.toDoubleOrNull()
                val balanceVal = balanceRegex.find(normalized)?.groupValues?.get(1)?.toDoubleOrNull()

                // Try to extract merchant (usually word after Balans ... RUR. [Merchant])
                var merchant = "Покупка по карте"
                val merchantRegex = """Balans\s+\d+(?:\.\d+)?\s*(?:RUR|RUB|р|руб)\.\s+([A-Za-z0-9\sа-яА-Я._\-\*]+)""".toRegex(RegexOption.IGNORE_CASE)
                val matchMerchant = merchantRegex.find(normalized)
                if (matchMerchant != null) {
                    merchant = matchMerchant.groupValues[1].trim()
                }

                if (cardSuffix != null && amountVal != null) {
                    return ParsedBankSms(
                        cardSuffix = cardSuffix,
                        amount = -amountVal, // expense
                        description = merchant,
                        balance = balanceVal,
                        category = detectCategory(merchant),
                        bankName = "Т-Банк"
                    )
                }
            }

            // 2. Check for Sberbank (900) pattern:
            // "MIR-5678 17:05 Покупка 1200р Pyaterochka Баланс: 8400р"
            // "VISA1234 10:20 Списание 350.50р FastFood Баланс: 1200р"
            if (senderUpper.contains("900") || normalized.contains("Баланс") || normalized.contains("900")) {
                val cardRegex = """(MIR|VISA|ECMC|MASTERCARD|MC)[-\s]?(\d{4})""".toRegex(RegexOption.IGNORE_CASE)
                val amountRegex = """(?:Покупка|Оплата|Списание)\s+(\d+(?:\.\d+)?)\s*(?:р|rur|rub)""".toRegex(RegexOption.IGNORE_CASE)
                val balanceRegex = """Баланс:\s*(\d+(?:\.\d+)?)\s*(?:р|rur|rub)""".toRegex(RegexOption.IGNORE_CASE)

                val cardMatch = cardRegex.find(normalized)
                val cardSuffix = cardMatch?.groupValues?.get(2)
                val amountVal = amountRegex.find(normalized)?.groupValues?.get(1)?.toDoubleOrNull()
                val balanceVal = balanceRegex.find(normalized)?.groupValues?.get(1)?.toDoubleOrNull()

                // Extract details between amount and "Баланс"
                var merchant = "Покупка"
                val detailsRegex = """(?:Покупка|Оплата|Списание)\s+\d+(?:\.\d+)?\s*(?:р|rur|rub)\s+([A-Za-z0-9\sа-яА-Я._\-]+)\s+Баланс""".toRegex(RegexOption.IGNORE_CASE)
                val matchDetails = detailsRegex.find(normalized)
                if (matchDetails != null) {
                    merchant = matchDetails.groupValues[1].trim()
                }

                if (cardSuffix != null && amountVal != null) {
                    return ParsedBankSms(
                        cardSuffix = cardSuffix,
                        amount = -amountVal,
                        description = merchant,
                        balance = balanceVal,
                        category = detectCategory(merchant),
                        bankName = "Сбербанк"
                    )
                }
            }

            // 3. Check for Alfa-Bank pattern:
            // "Alfa-Bank: Spisanie 300 RUR. Karta *4321. Pyaterochka"
            if (senderUpper.contains("ALFA") || normalized.contains("Alfa-Bank")) {
                val cardRegex = """Karta\s*\*(\d{4})""".toRegex(RegexOption.IGNORE_CASE)
                val amountRegex = """(?:Spisanie|Oplata|Spisano)\s+(\d+(?:\.\d+)?)\s*(?:RUR|RUB|р|руб)""".toRegex(RegexOption.IGNORE_CASE)
                val cardSuffix = cardRegex.find(normalized)?.groupValues?.get(1)
                val amountVal = amountRegex.find(normalized)?.groupValues?.get(1)?.toDoubleOrNull()

                var merchant = "Покупка по карте"
                val merchantRegex = """Karta\s*\*?\d{4}\.\s+([A-Za-z0-9\sа-яА-Я._\-\*]+)""".toRegex(RegexOption.IGNORE_CASE)
                val matchMerchant = merchantRegex.find(normalized)
                if (matchMerchant != null) {
                    merchant = matchMerchant.groupValues[1].trim()
                }

                if (cardSuffix != null && amountVal != null) {
                    return ParsedBankSms(
                        cardSuffix = cardSuffix,
                        amount = -amountVal,
                        description = merchant,
                        balance = null, // Alfa-Bank SMS doesn't always contain immediate balance in this format
                        category = detectCategory(merchant),
                        bankName = "Альфа-Банк"
                    )
                }
            }

            // Fallback general pattern if it contains some indicators
            // "Карта *1234: Списание 1500 руб. Ozon"
            val genericCardRegex = """(?:карта|card|karta)[-\s]*\*?(\d{4})""".toRegex(RegexOption.IGNORE_CASE)
            val genericAmountRegex = """(?:списание|оплата|покупка|расход|spisanie|oplata)\s+(\d+(?:\.\d+)?)\s*(?:руб|руб\.|р|RUR|RUB)""".toRegex(RegexOption.IGNORE_CASE)

            val genCard = genericCardRegex.find(normalized)?.groupValues?.get(1)
            val genAmount = genericAmountRegex.find(normalized)?.groupValues?.get(1)?.toDoubleOrNull()

            if (genCard != null && genAmount != null) {
                // Find a word representing description
                var desc = "Покупка"
                val rest = normalized.substringAfter(genAmount.toString()).trim()
                if (rest.isNotEmpty() && rest.length > 3) {
                    desc = rest.split(".")[0].trim()
                    if (desc.contains("Баланс") || desc.contains("Balans")) {
                        desc = desc.split("Баланс")[0].trim()
                    }
                }
                return ParsedBankSms(
                    cardSuffix = genCard,
                    amount = -genAmount,
                    description = desc,
                    balance = null,
                    category = detectCategory(desc),
                    bankName = "Банк"
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing SMS: ${e.message}", e)
        }
        return null
    }

    fun detectCategory(description: String): String {
        val descLower = description.lowercase()
        return when {
            descLower.contains("pyaterochka") || descLower.contains("пятерочка") ||
            descLower.contains("perekrestok") || descLower.contains("перекресток") ||
            descLower.contains("magnit") || descLower.contains("магнит") ||
            descLower.contains("realke") || descLower.contains("vkusvill") ||
            descLower.contains("вкусвилл") || descLower.contains("auchan") || descLower.contains("ашан") ||
            descLower.contains("продукты") || descLower.contains("diksi") || descLower.contains("дикси") ||
            descLower.contains("lenta") || descLower.contains("лента") || descLower.contains("metro") -> "Продукты"

            descLower.contains("starbucks") || descLower.contains("кафе") || descLower.contains("shokoladnica") ||
            descLower.contains("burger") || descLower.contains("mcdonald") || descLower.contains("kfc") ||
            descLower.contains("вкусно и точка") || descLower.contains("додо") || descLower.contains("pizza") ||
            descLower.contains("ресторан") || descLower.contains("кофе") || descLower.contains("булочная") ||
            descLower.contains("теремок") -> "Кафе и рестораны"

            descLower.contains("taxi") || descLower.contains("такси") || descLower.contains("yandex.go") ||
            descLower.contains("metro") || descLower.contains("метро") || descLower.contains("bus") ||
            descLower.contains("автобус") || descLower.contains("жд") || descLower.contains("ржд") ||
            descLower.contains("troika") || descLower.contains("тройка") || descLower.contains("проезд") -> "Транспорт"

            descLower.contains("hospital") || descLower.contains("аптека") || descLower.contains("apteka") ||
            descLower.contains("клиника") || descLower.contains("мед") || descLower.contains("стоматология") ||
            descLower.contains("анализы") || descLower.contains("здоровье") || descLower.contains("врач") -> "Здоровье"

            descLower.contains("cinema") || descLower.contains("кино") || descLower.contains("teatr") ||
            descLower.contains("театр") || descLower.contains("steam") || descLower.contains("playstation") ||
            descLower.contains("развлечения") || descLower.contains("подписка") || descLower.contains("yandex.plus") ||
            descLower.contains("игрушки") || descLower.contains("парк") || descLower.contains("клуб") -> "Развлечения"

            descLower.contains("saloon") || descLower.contains("парикмахерская") || descLower.contains("барбер") ||
            descLower.contains("косметика") || descLower.contains("маникюр") || descLower.contains("красота") -> "Красота"

            descLower.contains("перевод") || descLower.contains("perevod") || descLower.contains("friend") ||
            descLower.contains("сбп") || descLower.contains("sbp") -> "Переводы"

            descLower.contains("зарплата") || descLower.contains("salary") || descLower.contains("поступление") ||
            descLower.contains("vplata") || descLower.contains("выплата") || descLower.contains("начисление") ||
            descLower.contains("премия") -> "Поступления"

            else -> "Другое"
        }
    }
}
