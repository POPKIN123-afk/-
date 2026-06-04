package com.example.data.local

import androidx.room.*
import com.example.data.model.BankCard
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {

    // --- Bank Cards ---
    @Query("SELECT * FROM bank_cards ORDER BY id ASC")
    fun getAllCards(): Flow<List<BankCard>>

    @Query("SELECT * FROM bank_cards WHERE id = :id")
    suspend fun getCardById(id: Int): BankCard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: BankCard): Long

    @Update
    suspend fun updateCard(card: BankCard)

    @Delete
    suspend fun deleteCard(card: BankCard)

    @Query("UPDATE bank_cards SET balance = balance + :amount WHERE id = :cardId")
    suspend fun adjustCardBalance(cardId: Int, amount: Double)

    // --- Transactions ---
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE cardId = :cardId ORDER BY timestamp DESC")
    fun getTransactionsByCard(cardId: Int): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
}
