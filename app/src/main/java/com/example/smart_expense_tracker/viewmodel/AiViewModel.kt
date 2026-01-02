package com.example.smart_expense_tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smart_expense_tracker.network.DeepSeekApiService
import com.example.smart_expense_tracker.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Calendar

class AiViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ExpenseRepository.getInstance(application)
    private val deepSeekService = DeepSeekApiService()

    private val _analysisResult = MutableStateFlow("")
    val analysisResult: StateFlow<String> = _analysisResult.asStateFlow()

    private val _spendingInsights = MutableStateFlow("")
    val spendingInsights: StateFlow<String> = _spendingInsights.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refreshAnalysis()
    }

    fun refreshAnalysis() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val prompt = generateAnalysisPrompt()
                val result = deepSeekService.getAnalysis(prompt)

                result.onSuccess { content ->
                    // 尝试将结果分割为洞察和详细分析两部分
                    val parts = content.split("\n\n", limit = 2)
                    _spendingInsights.value = parts.getOrNull(0) ?: content
                    _analysisResult.value = parts.getOrNull(1) ?: ""
                }.onFailure { exception ->
                    // 将异常转化为用户友好的错误信息
                    handleAnalysisError(exception)
                }

            } catch (e: Exception) {
                handleAnalysisError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun handleAnalysisError(exception: Throwable) {
        val errorMessage = when (exception) {
            is java.io.IOException -> "网络连接错误，请检查您的网络设置。\n详细信息: ${exception.message}"
            is org.json.JSONException -> "解析API响应失败，可能是API格式已更新。\n详细信息: ${exception.message}"
            else -> "发生未知错误，请稍后重试。\n详细信息: ${exception.message}"
        }
        _error.value = errorMessage
    }

    private suspend fun generateAnalysisPrompt(): String {
        // Get current year and month
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        // Fetch all transactions and filter for the current month
        val allTransactions = repository.getAllTransactions()
        val monthlyTransactions = allTransactions.filter {
            val txCalendar = Calendar.getInstance()
            txCalendar.timeInMillis = it.recordTime
            txCalendar.get(Calendar.YEAR) == currentYear && txCalendar.get(Calendar.MONTH) == currentMonth
        }

        val accounts = repository.getAllAccounts()
        val monthlyStats = repository.getMonthlyStats(currentYear, currentMonth + 1)

        // 构建详细的JSON字符串作为prompt
        val data = JSONObject()
        data.put("monthly_expense", monthlyStats.expense / 100.0)
        data.put("monthly_income", monthlyStats.income / 100.0)

        val transactionArray = org.json.JSONArray()
        monthlyTransactions.take(20).forEach { // 只取最近20条交易记录以简化prompt
            val t = JSONObject()
            t.put("amount", it.amount / 100.0)
            t.put("type", if(it.type == 0) "支出" else "收入")
            t.put("category", repository.getCategoryById(it.categoryId ?: -1)?.name ?: "未知")
            t.put("remark", it.remark)
            transactionArray.put(t)
        }
        data.put("recent_transactions", transactionArray)

        val accountArray = org.json.JSONArray()
        accounts.forEach {
            val a = JSONObject()
            a.put("name", it.name)
            a.put("balance", it.balance / 100.0)
            accountArray.put(a)
        }
        data.put("accounts", accountArray)


        return """
        你是一个专业的个人理财顾问。请根据以下从用户的一个记账软件拉取的当月的JSON格式的个人财务数据，提供两部分内容：
        1.  **一句话快速洞察**：用一句话总结用户最需要关注的财务亮点或问题。这句话必须简短、精炼，直击要点。
        2.  **详细分析报告**：提供一份详细的财务分析报告，使用Markdown格式。报告应包括但不限于：
            *   **消费结构分析**：分析各项支出的占比，指出哪些是主要开销。
            *   **收支平衡评估**：评估用户的收入是否能覆盖支出，是否存在财务风险。
            *   **资产状况诊断**：简要评估用户的资产和负债情况。
            *   **个性化建议**：根据以上分析，提供2-3条具体的、可操作的理财建议。

        请确保“一句话快速洞察”和“详细分析报告”之间用两个换行符分隔。返回的格式必须严格遵守此约定。

        财务数据如下：
        ```json
        $data
        ```
        """
    }

    fun clearError() {
        _error.value = null
    }
}
