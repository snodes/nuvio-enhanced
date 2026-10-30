package com.nuvio.app.features.player

import android.app.Application
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

// Brightness swipes last only for one player session; nothing carries into the next one.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PlayerBrightnessTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val playerVisible = mutableStateOf(true)
    private var controller: PlayerGestureController? = null

    @Before
    fun initialize() {
        compose.runOnUiThread {
            val activity = compose.activity
            activity.window.attributes = activity.window.attributes.apply { screenBrightness = -1f }
            Settings.System.putInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 77)
        }
    }

    @Test
    fun swipedBrightnessDoesNotCarryIntoNextPlayback() {
        showPlayer()
        compose.runOnIdle {
            assertEquals(77f / 255f, assertNotNull(controller).currentBrightness())
            assertEquals(0f, assertNotNull(controller).setBrightness(0f))
            assertEquals(0f, compose.activity.window.attributes.screenBrightness)
            playerVisible.value = false
        }
        compose.runOnIdle {
            assertEquals(-1f, compose.activity.window.attributes.screenBrightness)
            Settings.System.putInt(compose.activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 102)
            playerVisible.value = true
        }
        compose.runOnIdle {
            assertEquals(-1f, compose.activity.window.attributes.screenBrightness)
            assertEquals(102f / 255f, assertNotNull(controller).currentBrightness())
        }
    }

    @Test
    fun leavingPlayerRestoresPreviousWindowOverride() {
        compose.runOnUiThread {
            compose.activity.window.attributes = compose.activity.window.attributes.apply { screenBrightness = 0.25f }
        }
        showPlayer()
        compose.runOnIdle {
            assertEquals(0.6f, assertNotNull(controller).setBrightness(0.6f))
            playerVisible.value = false
        }
        compose.runOnIdle {
            assertEquals(0.25f, compose.activity.window.attributes.screenBrightness)
        }
    }

    private fun showPlayer() {
        compose.setContent {
            if (playerVisible.value) {
                val activeController = rememberPlayerGestureController()
                SideEffect { controller = activeController }
            }
        }
        compose.waitForIdle()
    }
}
