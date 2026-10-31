package com.nuvio.app.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class FullSizeArtworkUrlsTest {
    @Test
    fun `TMDB posters episode stills backdrops and logos use original assets`() {
        for (size in listOf("w185", "w342", "w500", "w780", "w1280", "w600_and_h900_bestv2", "h632")) {
            assertEquals("https://image.tmdb.org/t/p/original/art.jpg",
                fullSizeArtworkUrl("https://image.tmdb.org/t/p/$size/art.jpg"))
        }
    }

    @Test
    fun `metahub and kitsu small variants use largest documented variant`() {
        for (kind in listOf("poster", "background", "logo")) {
            for (size in listOf("small", "medium")) {
                assertEquals("https://images.metahub.space/$kind/large/tt123/img",
                    fullSizeArtworkUrl("https://images.metahub.space/$kind/$size/tt123/img"))
            }
        }
        for (host in listOf("media.kitsu.app", "media.kitsu.io")) {
            for (kind in listOf("poster_images", "cover_images")) {
                for (size in listOf("tiny", "small", "medium", "large")) {
                    assertEquals("https://$host/anime/$kind/12/original.jpg",
                        fullSizeArtworkUrl("https://$host/anime/$kind/12/$size.jpg"))
                }
            }
        }
    }

    @Test
    fun `original unknown signed and non artwork URLs are untouched`() {
        for (url in listOf("https://image.tmdb.org/t/p/original/a.jpg",
            "https://image.tmdb.org/t/p/w500/a.jpg?signature=private",
            "https://image.tmdb.org/t/p/w500/a.jpg#fragment",
            "https://images.metahub.space/poster/small/tt123/img?token=private",
            "https://media.kitsu.app/anime/cover_images/12/small.jpg?token=private",
            "https://media.kitsu.app/anime/cover_images/12/original.jpg",
            "https://image.tmdb.org.example.org/t/p/w500/a.jpg",
            "https://example.org/t/p/w500/a.jpg",
            "https://cdn.myanimelist.net/images/anime/1/2.jpg",
            "https://media.kitsu.app/users/12/small.jpg")) {
            assertEquals(url, fullSizeArtworkUrl(url))
        }
    }
}
