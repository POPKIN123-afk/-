package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bank_cards")
data class BankCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardNumber: String,
    val holderName: String,
    val bankName: String, // e.g., "Сбербанк", "Т-Банк", "Альфа-Банк", "ВТБ", "Другой"
    val balance: Double,
    val expiryDate: String, // e.g., "08/28"
    val cardType: String, // e.g., "MIR", "VISA", "MASTERCARD"
    val colorHex: String // Background color in hex, e.g., "#007A33"
)
