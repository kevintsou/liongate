package com.liongate.budget.util

import com.liongate.budget.domain.model.Category
import com.liongate.budget.domain.model.Transaction
import com.liongate.budget.domain.model.TransactionType

object CsvExporter {

    fun exportToCsv(
        transactions: List<Transaction>,
        categories: List<Category>
    ): String {
        val categoryMap = categories.associateBy { it.id }
        val sb = StringBuilder()

        // UTF-8 BOM for Excel compatibility
        sb.append('﻿')

        // Header
        sb.appendLine("date,type,category,description,amount")

        // Data rows
        transactions.forEach { transaction ->
            val category = categoryMap[transaction.categoryId]
            val dateStr = DateUtils.formatFullDate(transaction.date)
            val typeStr = if (transaction.type == TransactionType.INCOME) "INCOME" else "EXPENSE"
            val categoryName = category?.name ?: "Unknown"
            val description = transaction.description.replace(",", "，").replace("\n", " ")
            val amount = transaction.amount

            sb.appendLine("$dateStr,$typeStr,\"$categoryName\",\"$description\",$amount")
        }

        return sb.toString()
    }

    fun exportToByteArray(
        transactions: List<Transaction>,
        categories: List<Category>
    ): ByteArray {
        return exportToCsv(transactions, categories).toByteArray(Charsets.UTF_8)
    }
}
