package com.cs446.expensetracker

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.cs446.expensetracker.ui.LoginActivity
import com.cs446.expensetracker.ui.SignupActivity
import com.cs446.expensetracker.ui.SignupScreen
import com.cs446.expensetracker.ui.ui.theme.ExpenseTrackerTheme
import com.cs446.expensetracker.ui.ui.theme.Pink40
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SignupUITests {
    var loginScreenReached = false

    @get:Rule
    val composeTestRule = createComposeRule()

    fun clickSignup() {
        composeTestRule.onNodeWithText("SIGN UP").performClick()
    }

    @Before
    fun setScreen() {
        composeTestRule.setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Pink40
            ) {
                ExpenseTrackerTheme {
                    SignupScreen(onSignupSuccess = {
                        loginScreenReached = true
                    },
                        onLoginClick = {
                            loginScreenReached = true
                        })
                }
            }
        }
    }

    fun performSignup(firstName: String, lastName: String, username: String, password: String) {
        composeTestRule.onNodeWithText("First Name").performTextReplacement(firstName)
        composeTestRule.onNodeWithText("Last Name").performTextReplacement(lastName)
        composeTestRule.onNodeWithText("Username").performTextReplacement(username)
        composeTestRule.onNodeWithText("Password").performTextReplacement(password)
        clickSignup()
    }

    fun assertSignupError() {
        composeTestRule.onNodeWithText("Please fill in all fields.").assertExists()
        assert(!loginScreenReached)
    }

    fun assertPasswordComplexityError() {
        composeTestRule.onNodeWithText("Password must be at least 8 characters long and contain at least 1 uppercase letter, 1 lowercase letter, and 1 number.")
            .assertExists()
        assert(!loginScreenReached)
    }

    @Test
    fun testSignup_noFirstname() {
        performSignup("", "Last", "username", "password")
        assertSignupError()
    }

    @Test
    fun testSignup_noLastname() {
        performSignup("First", "", "username", "password")
        assertSignupError()
    }

    @Test
    fun testSignup_noUsername() {
        performSignup("First", "Last", "", "password")
        assertSignupError()
    }

    @Test
    fun testSignup_noPassword() {
        performSignup("First", "Last", "username", "")
        assertSignupError()
    }

    // Commenting out to avoid filling database with test data
//    @Test
//    fun testSignup_validCredentials() {
//        performSignup("First", "Last", "new_user", "Password1")
//
//        // wait 2s for signup to complete
//        val latch = CountDownLatch(1)
//        latch.await(2, TimeUnit.SECONDS)
//
//        assert(mainScreenReached)
//        assert(!loginScreenReached)
//    }

    @Test
    fun testSignup_navToLogin() {
        composeTestRule.onNodeWithText("Login").performClick()
        assert(loginScreenReached)
    }

    @Test
    fun testSignup_invalidPassword_notEnoughCharacters() {
        performSignup("First", "Last", "username", "asdf")
        assertPasswordComplexityError()
        assert(!loginScreenReached)
    }

    @Test
    fun testSignup_invalidPassword_noUppercase() {
        performSignup("First", "Last", "username", "password1")
        assertPasswordComplexityError()
        assert(!loginScreenReached)
    }

    @Test
    fun testSignup_invalidPassword_noLowercase() {
        performSignup("First", "Last", "username", "PASSWORD1")
        assertPasswordComplexityError()
        assert(!loginScreenReached)
    }

    @Test
    fun testSignup_invalidPassword_noNumber() {
        performSignup("First", "Last", "username", "Password")
        assertPasswordComplexityError()
        assert(!loginScreenReached)
    }
}