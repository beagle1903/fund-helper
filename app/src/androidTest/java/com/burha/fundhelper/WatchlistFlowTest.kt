package com.burha.fundhelper

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WatchlistFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private fun str(id: Int) = compose.activity.getString(id)

    @Test
    fun empty_watchlist_shows_empty_state() {
        compose.waitUntilAtLeastOneExists(hasText(str(R.string.watchlist_empty_title)), TIMEOUT_MS)
    }

    @Test
    fun search_then_follow_adds_fund_to_watchlist() {
        compose.onNodeWithText(str(R.string.watchlist_empty_cta)).performClick()

        compose.onNodeWithText(str(R.string.search_hint)).performTextInput("altın")
        compose.onNodeWithText(str(R.string.search_action)).performClick()

        compose.waitUntilAtLeastOneExists(hasText("AAA"), TIMEOUT_MS)
        compose.onAllNodesWithContentDescription(str(R.string.follow))[0].performClick()
        compose.waitUntilAtLeastOneExists(hasContentDescription(str(R.string.unfollow)), TIMEOUT_MS)

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

        compose.waitUntilAtLeastOneExists(hasText("Test Altın Fonu"), TIMEOUT_MS)
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
