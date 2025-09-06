package com.hereliesaz.halfhashedkitty

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testAzNavRailIsDisplayed() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        // Start the app
        composeTestRule.setContent {
            MainScreen(
                viewModel = MainViewModel(context, HashcatApiClient(), Cap2HashcatApiClient(), ToolManager(context)),
                hashtopolisViewModel = HashtopolisViewModel(),
                piControlViewModel = PiControlViewModel()
            )
        }

        // Check that the "Input" tab is displayed, which is part of the AzNavRail
        composeTestRule.onNodeWithText("Input").assertExists()
    }
}
