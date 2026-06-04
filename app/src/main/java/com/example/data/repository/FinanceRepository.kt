package com.example.data.repository

import com.example.data.local.FinanceDao
import com.example.data.model.BankCard
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val financeDao: FinanceDao) {

    val allCards: Flow<List<BankCard>> = financeDao.getAllCards()
    val allTransactions: Flow<List<Transaction>> = financeDao.getAllTransactions()

    fun getTransactionsByCard(cardId: Int): Flow<List<Transaction>> {
        return financeDao.getTransactionsByCard(cardId)
    }

    suspend fun insertCard(card: BankCard): Long {
        return financeDao.insertCard(card)
    }

    suspend fun updateCard(card: BankCard) {
        financeDao.updateCard(card)
    }

    suspend fun deleteCard(card: BankCard) {
        financeDao.deleteCard(card)
    }

    suspend fun addTransaction(transaction: Transaction) {
        // Insert transaction first
        financeDao.insertTransaction(transaction)
        // If it belongs to a card, update that card's balance
        if (transaction.cardId != null) {
            financeDao.adjustCardBalance(transaction.cardId, transaction.amount)
        }
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        // Remove transaction
        financeDao.deleteTransaction(transaction)
        // If it belonged to a card, reverse the balance adjustment
        if (transaction.cardId != null) {
            financeDao.adjustCardBalance(transaction.cardId, -transaction.amount)
        }
    }
}
