package com.cs446.expensetracker

import com.cs446.expensetracker.ui.login
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LoginUnitTests {
    @Test
    fun testLogin_noUsername() {
        runBlocking {
            var response = login("", "password")
            assertEquals(false, response)
        }
    }

    @Test
    fun testLogin_noPassword() {
        runBlocking {
            var response = login("username", "")
            assertEquals(false, response)
        }
    }

    @Test
    fun testLogin_invalidCredentials() {
        runBlocking {
            var response = login("admin", "wrongPassword")
            assertEquals(false, response)
        }
    }

    @Test
    fun testLogin_validCredentials() {
        runBlocking {
            var response = login("admin", "admin")
            assertEquals(true, response)
        }
    }
}