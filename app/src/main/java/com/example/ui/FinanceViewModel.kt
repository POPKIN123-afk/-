package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BankCard
import com.example.data.model.Transaction
import com.example.data.repository.FinanceRepository
import com.example.util.SmsParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinanceViewModel(
    application: Application,
    private val repository: FinanceRepository
) : AndroidViewModel(application) {

    // Exposure of cards and transactions flow
    val cards: StateFlow<List<BankCard>> = repository.allCards
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current selected card (null for no specific card or "All Cards")
    private val _selectedCardId = MutableStateFlow<Int?>(null)
    val selectedCardId = _selectedCardId.asStateFlow()

    // Filtered transactions stream based on active card
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        repository.allTransactions,
        _selectedCardId
    ) { allTx, cardId ->
        if (cardId == null) {
            allTx
        } else {
            allTx.filter { it.cardId == cardId }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // UI Feedback or status
    private val _smsSimulationResult = MutableStateFlow<String?>(null)
    val smsSimulationResult = _smsSimulationResult.asStateFlow()

    fun selectCard(cardId: Int?) {
        _selectedCardId.value = cardId
    }

    fun addCard(
        cardNumber: String,
        holderName: String,
        bankName: String,
        balance: Double,
        expiryDate: String,
        cardType: String,
        colorHex: String
    ) {
        viewModelScope.launch {
            val card = BankCard(
                cardNumber = formatCardNumber(cardNumber),
                holderName = holderName.uppercase(),
                bankName = bankName,
                balance = balance,
                expiryDate = expiryDate,
                cardType = cardType,
                colorHex = colorHex
            )
            repository.insertCard(card)
        }
    }

    fun deleteCard(card: BankCard) {
        viewModelScope.launch {
            repository.deleteCard(card)
            if (_selectedCardId.value == card.id) {
                _selectedCardId.value = null
            }
        }
    }

    fun addManualTransaction(
        cardId: Int?,
        amount: Double,
        category: String,
        description: String
    ) {
        viewModelScope.launch {
            val tx = Transaction(
                cardId = cardId,
                amount = amount,
                category = category,
                description = description,
                isSmsParsed = false
            )
            repository.addTransaction(tx)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun clearSmsResult() {
        _smsSimulationResult.value = null
    }

    // Process a simulated or actual bank SMS text
    fun simulateBankSms(smsText: String, sender: String = "900") {
        viewModelScope.launch {
            val parsedResult = SmsParser.parse(smsText, sender)
            if (parsedResult == null) {
                _smsSimulationResult.value = "Ошибка: не удалось распознать формат СМС как банковское уведомление."
                return@launch
            }

            // Look up matching card ends with suffix in database
            val currentCards = repository.allCards.first()
            val matchedCard = currentCards.find { card ->
                val cleanNum = card.cardNumber.replace(" ", "")
                cleanNum.endsWith(parsedResult.cardSuffix)
            }

            if (matchedCard != null) {
                val tx = Transaction(
                    cardId = matchedCard.id,
                    amount = parsedResult.amount,
                    category = parsedResult.category,
                    description = parsedResult.description,
                    isSmsParsed = true
                )
                repository.addTransaction(tx)

                if (parsedResult.balance != null) {
                    repository.updateCard(matchedCard.copy(balance = parsedResult.balance))
                }

                _smsSimulationResult.value = "СМС от ${parsedResult.bankName} успешно обработано! " +
                        "Списано ${parsedResult.amount} руб., добавлено в категорию '${parsedResult.category}' для карты *${parsedResult.cardSuffix}."
            } else {
                // Card not found in DB
                val tx = Transaction(
                    cardId = null,
                    amount = parsedResult.amount,
                    category = parsedResult.category,
                    description = "${parsedResult.bankName} *${parsedResult.cardSuffix}: ${parsedResult.description}",
                    isSmsParsed = true
                )
                repository.addTransaction(tx)

                _smsSimulationResult.value = "СМС от ${parsedResult.bankName} обработано как общий расход! " +
                        "Снятие ${parsedResult.amount} руб. для карты *${parsedResult.cardSuffix} (карта не найдена в системе, транзакция добавлена как свободный расход)."
            }
        }
    }

    // Auto generate typical pre-defined cards and transactions if the list is empty
    fun generateInitialDemoData() {
        viewModelScope.launch {
            val currentCards = cards.value
            if (currentCards.isEmpty()) {
                // Add demo cards
                val card1Id = repository.insertCard(
                    BankCard(
                        cardNumber = "2202 2011 2233 4321",
                        holderName = "ALEXEY SMIRNOV",
                        bankName = "Сбербанк",
                        balance = 45300.22,
                        expiryDate = "09/28",
                        cardType = "MIR",
                        colorHex = "#2E7D32" // Sberbank Green
                    )
                ).toInt()

                val card2Id = repository.insertCard(
                    BankCard(
                        cardNumber = "5463 8812 3456 7890",
                        holderName = "ALEXEY SMIRNOV",
                        bankName = "Т-Банк",
                        balance = 125000.00,
                        expiryDate = "11/29",
                        cardType = "MASTERCARD",
                        colorHex = "#FFDD2D" // Tinkoff Yellow-Gold
                    )
                ).toInt()

                val card3Id = repository.insertCard(
                    BankCard(
                        cardNumber = "4150 9922 8844 9876",
                        holderName = "ALEXEY SMIRNOV",
                        bankName = "Альфа-Банк",
                        balance = 8240.50,
                        expiryDate = "02/30",
                        cardType = "VISA",
                        colorHex = "#D32F2F" // Alfa Red
                    )
                ).toInt()

                // Add demo transactions
                repository.addTransaction(
                    Transaction(
                        cardId = card1Id,
                        amount = -3420.00,
                        category = "Продукты",
                        description = "Супермаркет Перекресток",
                        isSmsParsed = false
                    )
                )

                repository.addTransaction(
                    Transaction(
                        cardId = card2Id,
                        amount = -500.00,
                        category = "Транспорт",
                        description = "Пополнение Тройки",
                        isSmsParsed = true
                    )
                )

                repository.addTransaction(
                    Transaction(
                        cardId = card2Id,
                        amount = -1200.00,
                        category = "Кафе и рестораны",
                        description = "Вкусно и точка",
                        isSmsParsed = true
                    )
                )

                repository.addTransaction(
                    Transaction(
                        cardId = card3Id,
                        amount = -250.00,
                        category = "Здоровье",
                        description = "Аптека Ригла",
                        isSmsParsed = false
                    )
                )

                repository.addTransaction(
                    Transaction(
                        cardId = card2Id,
                        amount = 45000.00,
                        category = "Поступления",
                        description = "Зарплата",
                        isSmsParsed = false
                    )
                )
            }
        }
    }

    private fun formatCardNumber(rawNum: String): String {
        val clean = rawNum.replace("[^0-9]".toRegex(), "")
        if (clean.length != 16) return rawNum
        return clean.chunked(4).joinToString(" ")
    }
}

class FinanceViewModelFactory(
    private val application: Application,
    private val repository: FinanceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
