package com.cs446.expensetracker

import com.cs446.expensetracker.ui.signup
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SignupUnitTests {
    @Test
    fun testSignup_noFirstname() {
        runBlocking {
            val response = signup("", "Last", "username", "password")
            assertEquals(false, response)
        }
    }

    @Test
    fun testSignup_noLastname() {
        runBlocking {
            val response = signup("First", "", "username", "password")
            assertEquals(false, response)
        }
    }

    @Test
    fun testSignup_noUsername() {
        runBlocking {
            val response = signup("First", "Last", "", "password")
            assertEquals(false, response)
        }
    }

    @Test
    fun testSignup_noPassword() {
        runBlocking {
            val response = signup("First", "Last", "username", "")
            assertEquals(false, response)
        }
    }

//     Commenting this out because I don't want to fill database with test data
//    @Test
//    fun testSignup_validCredentials() {
//        runBlocking {
//            val response = signup("First", "Last", "admin", "admin")
//            assertEquals(true, response)
//        }
//    }
}