package com.liongate.budget.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val categoryId: Long,
    val description: String,
    val date: Long,
    val createdAt: Long = System.currentTimeMillis()
)
