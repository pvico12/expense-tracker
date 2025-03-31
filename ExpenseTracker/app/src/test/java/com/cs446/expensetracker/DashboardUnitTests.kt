package com.cs446.expensetracker

import com.cs446.expensetracker.api.RetrofitInstance
import com.cs446.expensetracker.api.models.Category
import com.cs446.expensetracker.api.models.CategoryBreakdown
import com.cs446.expensetracker.api.models.GoalRetrievalGoals
import com.cs446.expensetracker.api.models.GoalRetrievalResponse
import com.cs446.expensetracker.api.models.LevelRequest
import com.cs446.expensetracker.api.models.SpendingSummaryResponse
import com.cs446.expensetracker.utils.formatCurrency
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.cs446.expensetracker.ui.login
import kotlinx.coroutines.runBlocking

class DashboardUnitTests {
    // helper function unit tests
    @Test
    fun testFormatCurrency() {
        assertEquals(formatCurrency(0.0), "0.00")
        assertEquals(formatCurrency(123.45), "123.45")
        assertEquals(formatCurrency(123.0), "123.00")
        assertEquals(formatCurrency(1000.40), "1,000.40")
        assertEquals(formatCurrency(1000.4), "1,000.40")
        assertEquals(formatCurrency(1000000.99), "1,000,000.99")
    }

    @Test
    fun testSpendingSummaryAPIValidResponse() : Unit = runBlocking {
        val currentDate = LocalDateTime.now()
        val firstDayOfMonth = currentDate.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).format(
            DateTimeFormatter.ISO_DATE_TIME)
        val lastDayOfMonth = currentDate.withDayOfMonth(currentDate.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59).format(
            DateTimeFormatter.ISO_DATE_TIME)

        val response = login("admin", "admin")
        assertEquals(true, response)

        if (response) {
            val spendingResponse = RetrofitInstance.apiService.getSpendingSummary(
                firstDayOfMonth,
                lastDayOfMonth
            )
            if (spendingResponse.isSuccessful) {
                val responseBody = spendingResponse.body() as SpendingSummaryResponse
                assertEquals(responseBody.total_spend > 0.0, true)
                val spendingSummary = responseBody.category_breakdown.map { x ->
                    CategoryBreakdown(
                        category_name = x.category_name,
                        total_amount = x.total_amount,
                        percentage = x.percentage,
                        color = x.color,
                    )
                }
                for (summary in spendingSummary) {
                    assertEquals(summary.category_name != "", true)
                    assertEquals(summary.total_amount >= 0.0, true)
                    assertEquals(summary.percentage in 0.0..100.0, true)
                }
            }
        }
    }

    @Test
    fun testSpendingSummaryAPIFailure() : Unit = runBlocking {

        val response = login("admin", "admin")
        assertEquals(true, response)

        if (response) {
            val spendingResponse = RetrofitInstance.apiService.getSpendingSummary(
                "firstDayOfMonth",
                "lastDayOfMonth"
            )
            assertEquals(false, spendingResponse.isSuccessful)
        }

    }

    @Test
    fun testLevelAPIValidResponse() : Unit = runBlocking {
        val response = login("admin", "admin")
        assertEquals(true, response)

        if (response) {
            val levelResponse = RetrofitInstance.apiService.getLevel()

            if (levelResponse.isSuccessful) {
                val responseBody = levelResponse.body() as LevelRequest
                assertEquals(responseBody.level > 0, true)
                assertEquals(responseBody.current_xp >= 0, true)
                assertEquals(responseBody.total_xp_for_next_level >= 0, true)
                assertEquals(responseBody.remaining_xp_until_next_level >= 0, true)
            }
        }
    }

    @Test
    fun testCategoryAPIValidResponse() : Unit = runBlocking {
        val response = login("admin", "admin")
        assertEquals(true, response)

        if (response) {
            val categoriesResponse = RetrofitInstance.apiService.getCategories()
            val categories = categoriesResponse.body() as List<Category>
            for(category in categories) {
                assertEquals(category.id > 0, true)
                assertEquals(category.name != "", true)
            }
        }
    }

    @Test
    fun testGoalsAPIValidResponse() : Unit = runBlocking {
        val currentDate = LocalDateTime.now()
        val firstDayOfMonth = currentDate.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).format(
            DateTimeFormatter.ISO_DATE_TIME)
        val lastDayOfMonth = currentDate.withDayOfMonth(currentDate.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59).format(
            DateTimeFormatter.ISO_DATE_TIME)

        val response = login("admin", "admin")
        assertEquals(true, response)

        if (response) {
            val categoriesResponse = RetrofitInstance.apiService.getCategories()
            if (categoriesResponse.isSuccessful) {
                val categories = categoriesResponse.body() as List<Category>
                val goalsResponse = RetrofitInstance.apiService.getGoals(firstDayOfMonth, lastDayOfMonth)

                if (goalsResponse.isSuccessful) {
                    val responseBody = goalsResponse.body() as GoalRetrievalResponse
                    val goalStats = responseBody.stats
                    val listOfGoals = responseBody.goals.map { x ->
                        GoalRetrievalGoals(
                            id = x.id,
                            category_id = x.category_id,
                            goal_type = x.goal_type,
                            limit = x.limit,
                            start_date = x.start_date,
                            end_date = x.end_date,
                            period = x.period,
                            on_track = x.on_track,
                            time_left = x.time_left,
                            amount_spent = x.amount_spent,
                        )
                    } // should automatically make an empty list if no goals are found

                    for (goal in listOfGoals) {
                        goal.category_string = categories.find { it.id == goal.category_id }?.name ?: "Deleted Category"
                        assertEquals(goal.id >= 0, true)
                        assertEquals(goal.category_id == null ||  goal.category_id!! >= 0, true)
                        assertEquals(goal.goal_type == "amount" || goal.goal_type == "percentage", true)
                        assertEquals(goal.limit >= 0.0, true)
                        assertEquals(goal.period == 7 || goal.period == 30 || goal.period == 31, true)
                        assertEquals(goal.time_left >= 0 && goal.amount_spent >= 0, true)

                        // will throw an error and fail if this fails
                        val formatter = DateTimeFormatter.ISO_DATE_TIME
                        val endDateTime = LocalDateTime.parse(goal.end_date, formatter)
                        val startDateTime = LocalDateTime.parse(goal.start_date, formatter)
                    }
                }
            }
        }
    }

    @Test
    fun testGoalsAPIFailure() : Unit = runBlocking {

        val response = login("admin", "admin")
        assertEquals(true, response)

        if (response) {
            val goalResponse = RetrofitInstance.apiService.getGoals(
                "firstDayOfMonth",
                "lastDayOfMonth"
            )
            assertEquals(false, goalResponse.isSuccessful)
        }

    }


}