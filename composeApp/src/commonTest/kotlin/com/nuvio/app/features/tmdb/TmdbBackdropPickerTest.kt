package com.nuvio.app.features.tmdb

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TmdbBackdropPickerTest {
    private val images = """{"backdrops":[
        {"file_path":"/soft.jpg","width":1920,"height":1080,"iso_639_1":null,"vote_count":999},
        {"file_path":"/text.jpg","width":8000,"height":4500,"iso_639_1":"en","vote_count":20},
        {"file_path":"/large.jpg","width":3840,"height":2160,"iso_639_1":null,"vote_count":4},
        {"file_path":"/winner.jpg","width":3840,"height":2160,"iso_639_1":"xx","vote_count":5}
    ]}"""

    @Test
    fun largestTextlessWinsThenVoteCount() {
        assertEquals("https://image.tmdb.org/t/p/original/winner.jpg", largestTextlessBackdrop(images))
        assertNull(largestTextlessBackdrop("""{"backdrops":[{"file_path":"/text.jpg","width":8000,"height":4500,"iso_639_1":"ja"}]}"""))
        assertNull(largestTextlessBackdrop("""{"backdrops":[{"file_path":"/bad.jpg","width":0,"height":1080}]}"""))
    }

    @Test
    fun imdbLookupAndCacheWorkWithoutEnrichment() = runTest {
        val requests = mutableListOf<String>()
        val picker = TmdbBackdropPicker({ "setting-key" }) { url ->
            requests += url
            if ("/find/" in url) """{"tv_results":[{"id":1063}]}""" else images
        }
        val expected = "https://image.tmdb.org/t/p/original/winner.jpg"
        assertEquals(expected, picker.resolve("tt0423731", "series", "before"))
        assertEquals(expected, picker.resolve("tt0423731", "series", "another-original"))
        assertEquals(2, requests.size)
        assertTrue(requests.last().contains("/tv/1063/images?api_key=setting-key&include_image_language=null,xx"))
    }

    @Test
    fun noKeyFailureAndEmptyImagesKeepOriginal() = runTest {
        val noKey = TmdbBackdropPicker({ "" }) { error("must not fetch") }
        assertEquals("before", noKey.resolve("tmdb:1063", "series", "before"))
        val failure = TmdbBackdropPicker({ "key" }) { error("offline") }
        assertEquals("before", failure.resolve("tmdb:1063", "series", "before"))
        val malformed = TmdbBackdropPicker({ "key" }) { "not json" }
        assertEquals("before", malformed.resolve("tmdb:1063", "series", "before"))
        val empty = TmdbBackdropPicker({ "key" }) { """{"backdrops":[]}""" }
        assertEquals("before", empty.resolve("tmdb:1063", "series", "before"))
        assertEquals("other", empty.resolve("tmdb:1063", "series", "other"))
    }

    @Test
    fun customSeasonArtworkDoesNotUseFranchiseFallbackId() = runTest {
        val picker = TmdbBackdropPicker({ "key" }) { error("must not fetch") }
        assertEquals("anilist-banner", picker.resolve("kitsu:49847", "series", "anilist-banner", "tt123"))
    }

    @Test
    fun tmdbArtworkCanUseDetailsImdbId() = runTest {
        val picker = TmdbBackdropPicker({ "key" }) { url ->
            if ("/find/" in url) """{"movie_results":[{"id":123}]}""" else images
        }
        assertEquals("https://image.tmdb.org/t/p/original/winner.jpg",
            picker.resolve("custom:1", "movie", "https://image.tmdb.org/t/p/w1280/old.jpg", "tt123"))
    }

    @Test
    fun cancellationPropagates() = runTest {
        val picker = TmdbBackdropPicker({ "key" }) { throw CancellationException("leaving screen") }
        assertFailsWith<CancellationException> { picker.resolve("tmdb:1063", "series", "before") }
    }

    @Test
    fun phoneHeroUsesOriginalForBackdropAndPosterVariants() {
        assertEquals("https://image.tmdb.org/t/p/original/show.jpg",
            originalHeroArtworkUrl("https://image.tmdb.org/t/p/w1280/show.jpg"))
        assertEquals("https://image.tmdb.org/t/p/original/show.jpg",
            originalHeroArtworkUrl("https://image.tmdb.org/t/p/w500/show.jpg"))
        assertEquals("https://other.example/w500/show.jpg", originalHeroArtworkUrl("https://other.example/w500/show.jpg"))
        assertNull(originalHeroArtworkUrl(null))
    }
}
