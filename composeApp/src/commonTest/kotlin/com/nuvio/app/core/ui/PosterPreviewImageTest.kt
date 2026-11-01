package com.nuvio.app.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PosterPreviewImageTest {
    @Test
    fun loadsOriginalTmdbPosterAndPreservesTheArtworkAndQuery() {
        for (size in listOf("w92", "w185", "w500", "w780", "w600_and_h900_bestv2")) {
            assertEquals(
                "https://image.tmdb.org/t/p/original/frieren.jpg?language=en#poster",
                posterPreviewImageUrl("https://image.tmdb.org/t/p/$size/frieren.jpg?language=en#poster"),
            )
        }
        assertEquals(
            "http://image.tmdb.org/t/p/original/poster.png",
            posterPreviewImageUrl("http://image.tmdb.org/t/p/w185/poster.png"),
        )
    }

    @Test
    fun preservesOriginalTmdbImages() {
        for (size in listOf("original")) {
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
