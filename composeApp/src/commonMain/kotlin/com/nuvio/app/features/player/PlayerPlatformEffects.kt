package com.nuvio.app.features.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface PlayerGestureController {
    fun currentBrightness(): Float?
    fun setBrightness(level: Float): Float?
    fun currentVolume(): PlayerAudioLevel?
    fun setVolume(level: Float): PlayerAudioLevel?
}

data class PlayerAudioLevel(
    val fraction: Float,
    val isMuted: Boolean,
)

object PlayerOrientationPreference {
    private val _portrait = MutableStateFlow(false)
    val portrait: StateFlow<Boolean> = _portrait.asStateFlow()

    fun toggle() {
        _portrait.value = !_portrait.value
    }

    internal fun reset() {
        _portrait.value = false
    }
}

@Composable
expect fun LockPlayerToLandscape()

@Composable
expect fun rememberPlayerOrientationToggleSupported(): Boolean

@Composable
expect fun FullscreenPlayerDialog(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
)

@Composable
expect fun HidePlayerSystemBars()

@Composable
expect fun EnterImmersivePlayerMode(keepScreenAwake: Boolean)

@Composable
expect fun ManagePlayerPictureInPicture(
    isPlaying: Boolean,
    videoSize: IntSize,
)

@Composable
expect fun rememberIsInPictureInPicture(): Boolean

// Gesture controller for the full-screen player. Brightness swipes last only for this
// session: playback starts at the current system brightness, and the platform controller
// restores the pre-player brightness when the player leaves.
@Composable
fun rememberPlayerGestureController(): PlayerGestureController? {
    val controller = rememberPlatformPlayerGestureController() ?: return null
    return remember(controller) {
        object : PlayerGestureController by controller {
            override fun setBrightness(level: Float): Float? {
                if (!level.isFinite()) return null
                return controller.setBrightness(level.coerceIn(0f, 1f))
            }
        }
    }
}

@Composable
internal expect fun rememberPlatformPlayerGestureController(): PlayerGestureController?
