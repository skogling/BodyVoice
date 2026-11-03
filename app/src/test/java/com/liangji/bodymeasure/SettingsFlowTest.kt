package com.liangji.bodymeasure

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "w393dp-h852dp-xxhdpi")
class SettingsFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun opensSettingsSwitchesThemeAndReturns() {
        composeRule.onNodeWithContentDescription("设置").performClick()
        composeRule.onNodeWithText("设置").assertIsDisplayed()

        composeRule.onNode(hasText("深色").and(isSelectable())).performClick()
        composeRule.onNode(hasText("深色").and(isSelectable())).assertIsSelected()

        composeRule.onNodeWithContentDescription("返回").performClick()
        composeRule.onNodeWithText("身语").assertIsDisplayed()
        composeRule.onNodeWithText("听见身体 · 看见变化").assertIsDisplayed()
    }

    @Test
    fun opensAuthorRewardDialog() {
        composeRule.onNodeWithContentDescription("设置").performClick()

        composeRule.onNodeWithText("赞赏作者").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("微信赞赏二维码").assertIsDisplayed()
        composeRule.onNodeWithText("微信扫一扫 · skogling").assertIsDisplayed()

        composeRule.onNodeWithText("关闭").performClick()
        composeRule.onNodeWithContentDescription("微信赞赏二维码").assertDoesNotExist()
    }
}
