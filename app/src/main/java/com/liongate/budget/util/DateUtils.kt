package com.liongate.budget.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    private val monthYearFormat = SimpleDateFormat("yyyy-MM", Locale.TAIWAN)
    private val displayDateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN)
    private val displayMonthFormat = SimpleDateFormat("yyyy年M月", Locale.TAIWAN)
    private val shortDateFormat = SimpleDateFormat("M/d", Locale.TAIWAN)
    private val fullDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.TAIWAN)

    fun getCurrentMonthYear(): String {
        return monthYearFormat.format(Date())
    }

    fun formatMonthYear(monthYear: String): String {
        return try {
            val date = monthYearFormat.parse(monthYear) ?: return monthYear
            displayMonthFormat.format(date)
        } catch (e: Exception) {
            monthYear
        }
    }

    fun formatDate(timestamp: Long): String {
        return displayDateFormat.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        return shortDateFormat.format(Date(timestamp))
    }

    fun formatFullDate(timestamp: Long): String {
        return fullDateFormat.format(Date(timestamp))
    }

    fun getMonthYearFromTimestamp(timestamp: Long): String {
        return monthYearFormat.format(Date(timestamp))
    }

    fun getPreviousMonths(count: Int): List<String> {
        val calendar = Calendar.getInstance()
        val months = mutableListOf<String>()
        repeat(count) {
            months.add(monthYearFormat.format(calendar.time))
            calendar.add(Calendar.MONTH, -1)
        }
        return months.reversed()
    }

    fun timestampFromDateString(dateString: String): Long {
        return try {
            fullDateFormat.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    fun getNextMonth(monthYear: String): String {
        return try {
            val date = monthYearFormat.parse(monthYear) ?: return monthYear
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.add(Calendar.MONTH, 1)
            monthYearFormat.format(calendar.time)
        } catch (e: Exception) {
            monthYear
        }
    }

    fun getPreviousMonth(monthYear: String): String {
        return try {
            val date = monthYearFormat.parse(monthYear) ?: return monthYear
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.add(Calendar.MONTH, -1)
            monthYearFormat.format(calendar.time)
        } catch (e: Exception) {
            monthYear
        }
    }

    fun startOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
