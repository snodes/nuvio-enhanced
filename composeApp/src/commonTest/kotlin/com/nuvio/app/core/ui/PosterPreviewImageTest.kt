package com.nuvio.app.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PosterPreviewImageTest {
    @Test
    fun upgradesTmdbThumbnailsAndPreservesTheArtworkAndQuery() {
        for (size in listOf(92, 154, 185, 342, 500)) {
            assertEquals(
                "https://image.tmdb.org/t/p/w780/frieren.jpg?language=en#poster",
                posterPreviewImageUrl("https://image.tmdb.org/t/p/w$size/frieren.jpg?language=en#poster"),
            )
        }
        assertEquals(
            "http://image.tmdb.org/t/p/w780/poster.png",
            posterPreviewImageUrl("http://image.tmdb.org/t/p/w185/poster.png"),
        )
    }

    @Test
    fun preservesLargeAndOriginalTmdbImages() {
        for (size in listOf("w780", "w1280", "original")) {
            val url = "https://image.tmdb.org/t/p/$size/frieren.jpg"
            assertEquals(url, posterPreviewImageUrl(url))
        }
    }

    @Test
    fun preservesOtherAddonUrlsAndMissingArtwork() {
        for (url in listOf(
            "https://addon.example/t/p/w185/poster.jpg",
            "https://image.tmdb.org.example/t/p/w185/poster.jpg",
            "https://addon.example/poster?url=https://image.tmdb.org/t/p/w185/poster.jpg",
            "",
        )) {
            assertEquals(url, posterPreviewImageUrl(url))
        }
        assertNull(posterPreviewImageUrl(null))
    }
}
