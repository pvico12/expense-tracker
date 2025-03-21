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
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.core.content.ContextCompat.startActivity
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.cs446.expensetracker.ui.LoginActivity
import com.cs446.expensetracker.ui.LoginScreen
import com.cs446.expensetracker.ui.SignupActivity
import com.cs446.expensetracker.ui.ui.theme.Pink40
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ProfileUITests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val activityRule = ActivityScenarioRule(LoginActivity::class.java)

    @Before
    fun setScreen() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), LoginActivity::class.java)
        activityRule.scenario.onActivity { activity ->
            activity.startActivity(intent)
        }
    }

    fun performLogin() {
        composeTestRule.onNodeWithText("Username").performTextReplacement("admin")
        composeTestRule.onNodeWithText("Password").performTextReplacement("admin")
        composeTestRule.onNodeWithText("LOGIN").performClick()

        // wait 2s for the login to complete
        val latch = CountDownLatch(1)
        latch.await(2, TimeUnit.SECONDS)
    }

    fun openMenu() {
        composeTestRule.onNodeWithContentDescription("Menu Button").performClick()
    }

    @Test
    fun testProfileNav() {
        performLogin()
        openMenu()
        composeTestRule.onNodeWithText("Profile").performClick()
        composeTestRule.onNodeWithText("User Profile").assertExists()
    }

    @Test
    fun testProfileInfo() {
        performLogin()
        openMenu()
        composeTestRule.onNodeWithText("Profile").performClick()
        composeTestRule.onNodeWithText("Username").assertExists()
        composeTestRule.onNodeWithText("First Name").assertExists()
        composeTestRule.onNodeWithText("Last Name").assertExists()
    }

    @Test
    fun testProfile_emptyFirstname() {
        performLogin()
        openMenu()
        composeTestRule.onNodeWithText("Profile").performClick()
        composeTestRule.onNodeWithContentDescription("First Name Input").performTextReplacement("")
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.onNodeWithText("Please fill in all fields!").assertExists()
    }

    @Test
    fun testProfile_emptyLastname() {
        performLogin()
        openMenu()
        composeTestRule.onNodeWithText("Profile").performClick()
        composeTestRule.onNodeWithContentDescription("Last Name Input").performTextReplacement("")
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.onNodeWithText("Please fill in all fields!").assertExists()
    }

    @Test
    fun testProfile_emptyUsername() {
        performLogin()
        openMenu()
        composeTestRule.onNodeWithText("Profile").performClick()
        composeTestRule.onNodeWithContentDescription("Username Input").performTextReplacement("")
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.onNodeWithText("Please fill in all fields!").assertExists()
    }
}