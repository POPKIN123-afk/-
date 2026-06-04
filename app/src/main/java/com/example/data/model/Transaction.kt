package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardId: Int?, // NULL means cash or general transaction, not linked to specific card
    val amount: Double, // Negative for expenses, positive for income
    val category: String, // "Продукты", "Кафе", "Транспорт", "Развлечения", "Здоровье", "Переводы", "Поступления", etc.
    val timestamp: Long = System.currentTimeMillis(),
    val description: String, // e.g., "Пятерочка", "Латте"
    val isSmsParsed: Boolean = false // Was this automatically parsed from a bank SMS?
)
