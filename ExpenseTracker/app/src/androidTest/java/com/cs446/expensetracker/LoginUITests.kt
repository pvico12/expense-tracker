package com.cs446.expensetracker

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.core.content.ContextCompat.startActivity
import com.cs446.expensetracker.ui.LoginScreen
import com.cs446.expensetracker.ui.SignupActivity
import com.cs446.expensetracker.ui.ui.theme.Pink40
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class LoginUITests {

    var mainScreenReached = false
    var registrationScreenReached = false

    @get:Rule
    val composeTestRule = createComposeRule()

    fun clickLogin() {
        composeTestRule.onNodeWithText("LOGIN").performClick()
    }

    @Before
    fun setScreen() {
        composeTestRule.setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Pink40
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.coin_half),
                        contentDescription = "Coin Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                    )
                }
                LoginScreen(onLoginSuccess = {
                    mainScreenReached = true
                }, onCreateAccountClick = {
                    registrationScreenReached = true
                })
            }
        }
    }

    fun performLogin(username: String, password: String) {
        composeTestRule.onNodeWithText("Username").performTextReplacement(username)
        composeTestRule.onNodeWithText("Password").performTextReplacement(password)
        clickLogin()

        // wait 2s for the login to complete
        val latch = CountDownLatch(1)
        latch.await(2, TimeUnit.SECONDS)
    }

    @Test
    fun testLogin_noUsername() {
        performLogin("", "PASSWORD")
        composeTestRule.onNodeWithText("Missing username!").assertExists()
        assert(!mainScreenReached)
        assert(!registrationScreenReached)
    }

    @Test
    fun testLogin_noPassword() {
        performLogin("USERNAME", "")
        composeTestRule.onNodeWithText("Missing password!").assertExists()
        assert(!mainScreenReached)
        assert(!registrationScreenReached)
    }

    @Test
    fun testLogin_noUsername_noPassword() {
        performLogin("", "")
        composeTestRule.onNodeWithText("Missing username and password!").assertExists()
        assert(!mainScreenReached)
        assert(!registrationScreenReached)
    }

    @Test
    fun testLogin_invalidCredentials() {
        performLogin("admin", "wrongpassword")
        composeTestRule.onNodeWithText("Invalid credentials. Please try again.", useUnmergedTree = true).assertExists()
        assert(!mainScreenReached)
        assert(!registrationScreenReached)
    }

    @Test
    fun testLogin_validCredentials() {
        performLogin("admin", "admin")

        // wait 2s for login to complete
        val latch = CountDownLatch(1)
        latch.await(2, TimeUnit.SECONDS)

        assert(mainScreenReached)
        assert(!registrationScreenReached)
    }

    @Test
    fun testLogin_navToCreateAccount() {
        composeTestRule.onNodeWithText("Sign up").performClick()
        assert(!mainScreenReached)
        assert(registrationScreenReached)
    }
}