package com.cs446.expensetracker

import androidx.activity.ComponentActivity

/**
 * A simple test host activity that does not set any content.
 * This allows ComposeTestRule.setContent {} to be the sole source of UI content during testing.
 */
class TestActivity : ComponentActivity()
