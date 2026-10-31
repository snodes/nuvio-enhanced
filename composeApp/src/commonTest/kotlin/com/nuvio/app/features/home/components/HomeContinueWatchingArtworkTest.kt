package com.nuvio.app.features.home.components

import com.nuvio.app.core.ui.listCardArtworkUrl
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import kotlin.test.assertEquals
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeContinueWatchingArtworkTest {
    @Test
    fun inProgressEpisodeThumbnailRemainsBlurredUntilWatched() {
        val item = item(progressFraction = 0.5f)

        assertTrue(
            item.shouldBlurContinueWatchingArtwork(
                blurUnwatchedEpisodes = true,
                useEpisodeThumbnails = true,
                artworkUrl = "thumb.jpg",
            ),
        )
    }

    @Test
    fun completedEpisodeThumbnailIsNotBlurred() {
        val item = item(progressFraction = 0.9f)

        assertFalse(
            item.shouldBlurContinueWatchingArtwork(
                blurUnwatchedEpisodes = true,
                useEpisodeThumbnails = true,
                artworkUrl = "thumb.jpg",
            ),
        )
    }

    @Test
    fun fallbackArtworkIsNotBlurred() {
        val item = item(progressFraction = 0.5f)

        assertFalse(
            item.shouldBlurContinueWatchingArtwork(
                blurUnwatchedEpisodes = true,
                useEpisodeThumbnails = true,
                artworkUrl = "backdrop.jpg",
            ),
        )
    }

    @Test
    fun wideListPosterIgnoresThumbnailPreferenceAndNextUpBlur() {
        val item = item(progressFraction = 0f).copy(isNextUp = true)
        for (useThumbnails in listOf(false, true)) {
            val artwork = listCardArtworkUrl(item.poster, item.background, item.episodeThumbnail, item.imageUrl)
            assertEquals("poster.jpg", artwork)
            assertFalse(item.shouldBlurContinueWatchingArtwork(true, useThumbnails, artwork))
        }
    }

    @Test
    fun posterIsNotBlurredEvenWhenAddonReusesItAsThumbnail() {
        val item = item(progressFraction = 0f).copy(isNextUp = true, episodeThumbnail = "poster.jpg")
        assertFalse(item.shouldBlurContinueWatchingArtwork(true, true, "poster.jpg"))
    }

    @Test
    fun wideListWithoutPosterOnlyBlursAnActualEpisodeThumbnail() {
        val item = item(progressFraction = 0f).copy(isNextUp = true, poster = null, background = null)
        val artwork = listCardArtworkUrl(item.poster, item.background, item.episodeThumbnail, item.imageUrl)
        assertEquals("thumb.jpg", artwork)
        assertTrue(item.shouldBlurContinueWatchingArtwork(true, true, artwork))
        assertFalse(item.shouldBlurContinueWatchingArtwork(false, true, artwork))
    }

    private fun item(progressFraction: Float) = ContinueWatchingItem(
        parentMetaId = "show",
        parentMetaType = "series",
        videoId = "show:1:1",
        title = "Show",
        subtitle = "S1 E1",
        imageUrl = "thumb.jpg",
        poster = "poster.jpg",
        background = "backdrop.jpg",
        seasonNumber = 1,
        episodeNumber = 1,
        episodeThumbnail = "thumb.jpg",
        resumePositionMs = 300_000L,
        durationMs = 600_000L,
        progressFraction = progressFraction,
    )
}
