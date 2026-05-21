package com.liongate.budget.util

import android.content.Context
import android.net.Uri
import com.liongate.budget.domain.model.Category
import com.liongate.budget.domain.model.Transaction
import com.liongate.budget.domain.model.TransactionType

data class CsvImportResult(
    val transactions: List<Transaction>,
    val skippedRows: Int,
    val errors: List<String>
)

object CsvImporter {

    fun importFromUri(
        context: Context,
        uri: Uri,
        categories: List<Category>
    ): CsvImportResult {
        val categoryByName = categories.associateBy { it.name.lowercase() }
        val transactions = mutableListOf<Transaction>()
        val errors = mutableListOf<String>()
        var skippedRows = 0

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return CsvImportResult(emptyList(), 0, listOf("無法讀取檔案"))

        val lines = inputStream.bufferedReader(Charsets.UTF_8)
            .readLines()
            .map { it.trimStart('﻿') }

        if (lines.isEmpty()) return CsvImportResult(emptyList(), 0, listOf("檔案為空"))

        val dataLines = if (lines.first().lowercase().let { it.contains("date") || it.contains("日期") }) {
            lines.drop(1)
        } else {
            lines
        }

        dataLines.forEachIndexed { index, line ->
            if (line.isBlank()) return@forEachIndexed
            val lineNum = index + 2
            val cols = parseCsvLine(line)
            if (cols.size < 5) {
                skippedRows++
                errors.add("第${lineNum}行: 欄位不足 (需要5欄: 日期,類型,分類,備注,金額)")
                return@forEachIndexed
            }

            val dateStr = cols[0].trim()
            val typeStr = cols[1].trim().uppercase()
            val categoryName = cols[2].trim()
            val description = cols[3].trim()
            val amountStr = cols[4].trim()

            val date = DateUtils.timestampFromDateString(dateStr)
            val type = when (typeStr) {
                "INCOME" -> TransactionType.INCOME
                "EXPENSE" -> TransactionType.EXPENSE
                else -> {
                    skippedRows++
                    errors.add("第${lineNum}行: 無效類型 '$typeStr' (應為 INCOME 或 EXPENSE)")
                    return@forEachIndexed
                }
            }
            val amount = amountStr.replace(",", "").toDoubleOrNull()
            if (amount == null || amount <= 0) {
                skippedRows++
                errors.add("第${lineNum}行: 無效金額 '$amountStr'")
                return@forEachIndexed
            }

            val category = categoryByName[categoryName.lowercase()]
                ?: categories.firstOrNull { (it.type.name == type.name || it.type.name == "BOTH") }
                ?: categories.firstOrNull()

            if (category == null) {
                skippedRows++
                errors.add("第${lineNum}行: 找不到分類 '$categoryName'")
                return@forEachIndexed
            }

            transactions.add(
                Transaction(
                    amount = amount,
                    type = type,
                    categoryId = category.id,
                    description = description,
                    date = date
                )
            )
        }

        return CsvImportResult(transactions, skippedRows, errors)
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        val current = StringBuilder()
        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> { result.add(current.toString()); current.clear() }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        return result
    }
}
