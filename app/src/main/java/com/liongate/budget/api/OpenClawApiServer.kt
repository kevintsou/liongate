package com.liongate.budget.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.liongate.budget.domain.model.Budget
import com.liongate.budget.domain.model.Category
import com.liongate.budget.domain.model.CategoryType
import com.liongate.budget.domain.model.Transaction
import com.liongate.budget.domain.model.TransactionType
import com.liongate.budget.domain.repository.BudgetRepository
import com.liongate.budget.domain.repository.CategoryRepository
import com.liongate.budget.domain.repository.TransactionRepository
import com.liongate.budget.util.CsvExporter
import com.liongate.budget.util.DateUtils
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenClawApiServer @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository
) : NanoHTTPD(8080) {

    private val gson = Gson()

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        return try {
            when {
                uri == "/api/transactions" && method == Method.GET -> handleGetTransactions(session)
                uri == "/api/transactions" && method == Method.POST -> handlePostTransaction(session)
                uri.matches(Regex("/api/transactions/\\d+")) && method == Method.DELETE ->
                    handleDeleteTransaction(uri)
                uri == "/api/categories" && method == Method.GET -> handleGetCategories()
                uri == "/api/budget" && method == Method.GET -> handleGetBudget()
                uri == "/api/summary" && method == Method.GET -> handleGetSummary()
                uri == "/api/export/csv" && method == Method.GET -> handleExportCsv()
                else -> notFound()
            }
        } catch (e: Exception) {
            errorResponse(500, "Internal server error: ${e.message}")
        }
    }

    private fun handleGetTransactions(session: IHTTPSession): Response {
        val month = session.parameters["month"]?.firstOrNull()
        val transactions = runBlocking {
            if (month != null) {
                transactionRepository.getTransactionsByMonth(month).first()
            } else {
                transactionRepository.getAllTransactions().first()
            }
        }
        val categories = runBlocking { categoryRepository.getAllCategoriesList() }
        val categoryMap = categories.associateBy { it.id }

        val result = transactions.map { t ->
            mapOf(
                "id" to t.id,
                "amount" to t.amount,
                "type" to t.type.name,
                "categoryId" to t.categoryId,
                "categoryName" to (categoryMap[t.categoryId]?.name ?: ""),
                "description" to t.description,
                "date" to t.date,
                "dateFormatted" to DateUtils.formatDate(t.date)
            )
        }
        return jsonResponse(result)
    }

    private fun handlePostTransaction(session: IHTTPSession): Response {
        val contentLength = session.headers["content-length"]?.toIntOrNull() ?: 0
        val buffer = ByteArray(contentLength)
        session.inputStream.read(buffer, 0, contentLength)
        val body = String(buffer)

        return try {
            val json = gson.fromJson(body, JsonObject::class.java)
            val amount = json.get("amount")?.asDouble
                ?: return errorResponse(400, "Missing required field: amount")
            val type = json.get("type")?.asString
                ?: return errorResponse(400, "Missing required field: type")
            val categoryId = json.get("categoryId")?.asLong
                ?: return errorResponse(400, "Missing required field: categoryId")
            val description = json.get("description")?.asString ?: ""
            val date = json.get("date")?.asLong ?: System.currentTimeMillis()

            val transactionType = when (type.uppercase()) {
                "INCOME" -> TransactionType.INCOME
                "EXPENSE" -> TransactionType.EXPENSE
                else -> return errorResponse(400, "Invalid type: must be INCOME or EXPENSE")
            }

            val transaction = Transaction(
                amount = amount,
                type = transactionType,
                categoryId = categoryId,
                description = description,
                date = date
            )

            val id = runBlocking { transactionRepository.insertTransaction(transaction) }
            jsonResponse(mapOf("success" to true, "id" to id), 201)
        } catch (e: Exception) {
            errorResponse(400, "Invalid request body: ${e.message}")
        }
    }

    private fun handleDeleteTransaction(uri: String): Response {
        val id = uri.split("/").lastOrNull()?.toLongOrNull()
            ?: return errorResponse(400, "Invalid transaction ID")

        runBlocking { transactionRepository.deleteTransactionById(id) }
        return jsonResponse(mapOf("success" to true, "id" to id))
    }

    private fun handleGetCategories(): Response {
        val categories = runBlocking { categoryRepository.getAllCategoriesList() }
        val result = categories.map { c ->
            mapOf(
                "id" to c.id,
                "name" to c.name,
                "colorHex" to c.colorHex,
                "icon" to c.icon,
                "type" to c.type.name
            )
        }
        return jsonResponse(result)
    }

    private fun handleGetBudget(): Response {
        val currentMonth = DateUtils.getCurrentMonthYear()
        val budgets = runBlocking { budgetRepository.getBudgetsByMonthList(currentMonth) }
        val categories = runBlocking { categoryRepository.getAllCategoriesList() }
        val categoryMap = categories.associateBy { it.id }

        val transactions = runBlocking {
            transactionRepository.getTransactionsByMonth(currentMonth).first()
        }
        val spentByCategory = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }

        val result = budgets.map { budget ->
            val category = categoryMap[budget.categoryId]
            val spent = spentByCategory[budget.categoryId] ?: 0.0
            mapOf(
                "id" to budget.id,
                "categoryId" to budget.categoryId,
                "categoryName" to (category?.name ?: ""),
                "monthYear" to budget.monthYear,
                "limitAmount" to budget.limitAmount,
                "spent" to spent,
                "remaining" to (budget.limitAmount - spent),
                "isOverBudget" to (spent > budget.limitAmount),
                "percentUsed" to if (budget.limitAmount > 0) (spent / budget.limitAmount * 100) else 0.0
            )
        }
        return jsonResponse(mapOf("month" to currentMonth, "budgets" to result))
    }

    private fun handleGetSummary(): Response {
        val currentMonth = DateUtils.getCurrentMonthYear()
        val summary = runBlocking { transactionRepository.getMonthSummary(currentMonth) }
        val result = mapOf(
            "month" to currentMonth,
            "totalIncome" to (summary?.totalIncome ?: 0.0),
            "totalExpense" to (summary?.totalExpense ?: 0.0),
            "netAmount" to ((summary?.totalIncome ?: 0.0) - (summary?.totalExpense ?: 0.0))
        )
        return jsonResponse(result)
    }

    private fun handleExportCsv(): Response {
        val transactions = runBlocking { transactionRepository.getAllTransactionsList() }
        val categories = runBlocking { categoryRepository.getAllCategoriesList() }
        val csv = CsvExporter.exportToCsv(transactions, categories)
        val response = newFixedLengthResponse(
            Response.Status.OK,
            "text/csv; charset=utf-8",
            csv
        )
        response.addHeader("Content-Disposition", "attachment; filename=\"transactions.csv\"")
        return response
    }

    private fun <T> jsonResponse(data: T, statusCode: Int = 200): Response {
        val json = gson.toJson(data)
        val status = when (statusCode) {
            200 -> Response.Status.OK
            201 -> Response.Status.CREATED
            400 -> Response.Status.BAD_REQUEST
            404 -> Response.Status.NOT_FOUND
            500 -> Response.Status.INTERNAL_ERROR
            else -> Response.Status.OK
        }
        val response = newFixedLengthResponse(status, "application/json", json)
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS")
        response.addHeader("Access-Control-Allow-Headers", "Content-Type")
        return response
    }

    private fun errorResponse(code: Int, message: String): Response {
        return jsonResponse(mapOf("error" to message, "code" to code), code)
    }

    private fun notFound(): Response {
        return errorResponse(404, "Endpoint not found")
    }

    fun startServer() {
        if (!isAlive) {
            start(SOCKET_READ_TIMEOUT, false)
        }
    }

    fun stopServer() {
        if (isAlive) {
            stop()
        }
    }
}
