package com.cs446.expensetracker

import com.cs446.expensetracker.ui.*
//import com.cs446.expensetracker.ui.isValidHexColor
//import com.cs446.expensetracker.ui.parseCsvTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddExpenseUnitTests {

    @Test
    fun testIsValidHexColor_validColors_returnTrue() {
        val validColors = listOf(
            "#FFFFFF",
            "#ffffff",
            "#123456",
            "#000000",
            "#FF5733",
            "#FF0000",
            "#00FF00",
            "#0000FF",
            "#123456",
            "#ABCDEF",
            "#abcdef",
            "#123abc"
        )

        validColors.forEach {
            assertTrue("Expected $it to be valid", isValidHexColor(it))
        }
    }

    @Test
    fun testIsValidHexColor_invalidColors_returnFalse() {
        val invalidColors = listOf(
            "FFFFFF",
            "#FFF",
            "#ZZZZZZ",
            "blue",
            "#12345G",
            "",
            "#12345",
            "#1234567",
            "123456",
            "#GHIJKL",
            "#gggggg"
        )

        invalidColors.forEach {
            assertFalse("Expected $it to be invalid", isValidHexColor(it))
        }
    }

    @Test
    fun testIsValidAmount_validValues_returnTrue() {
        val validInputs = listOf("0", "0.00", "10", "100.5", "999999", "12.34")

        validInputs.forEach {
            assertTrue("Expected $it to be valid", isValidAmount(it))
        }
    }

    @Test
    fun testIsValidAmount_invalidValues_returnFalse() {
        val invalidInputs = listOf("-1", "-0.01", "abc", "", " ", ".", "10.10.10")

        invalidInputs.forEach {
            assertFalse("Expected $it to be invalid", isValidAmount(it))
        }
    }

    @Test
    fun testParseCsvTemplate_withSimpleCSV_returnsParsedList() {
        val csv = "amount,category,transaction_type\n100,Food,EXPENSE\n200,Transport,EXPENSE"
        val result = parseCsvTemplate(csv)

        assertEquals(3, result.size)
        assertEquals(listOf("amount", "category", "transaction_type"), result[0])
        assertEquals(listOf("100", "Food", "EXPENSE"), result[1])
        assertEquals(listOf("200", "Transport", "EXPENSE"), result[2])
    }

    @Test
    fun testParseCsvTemplate_withEmptyInput_returnsEmptyList() {
        val csv = ""
        val result = parseCsvTemplate(csv)
        assertTrue("Expected $result to be empty list", result.isEmpty())
    }

    @Test
    fun testParseCsvTemplate_withIrregularCSV_stillParses() {
        val csv = "amount,category\n100\n200,Transport,Extra"
        val result = parseCsvTemplate(csv)

        assertEquals(3, result.size)
        assertEquals(listOf("amount", "category"), result[0])
        assertEquals(listOf("100"), result[1])
        assertEquals(listOf("200", "Transport", "Extra"), result[2])
    }
}