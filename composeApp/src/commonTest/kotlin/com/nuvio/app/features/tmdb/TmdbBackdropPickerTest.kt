package com.nuvio.app.features.tmdb

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TmdbBackdropPickerTest {
    private val backdrops = """[
        {"file_path":"/soft.jpg","width":1920,"height":1080,"iso_639_1":null,"vote_count":999},
        {"file_path":"/text.jpg","width":8000,"height":4500,"iso_639_1":"en","vote_count":20},
        {"file_path":"/large.jpg","width":3840,"height":2160,"iso_639_1":null,"vote_count":4},
        {"file_path":"/winner.jpg","width":3840,"height":2160,"iso_639_1":"xx","vote_count":5}
    ]"""
    private val posters = """[
        {"file_path":"/titled.jpg","width":4000,"height":6000,"iso_639_1":"en","vote_count":50},
        {"file_path":"/key_art.jpg","width":2000,"height":3000,"iso_639_1":null,"vote_count":1},
        {"file_path":"/small_art.jpg","width":1000,"height":1500,"iso_639_1":null,"vote_count":9}
    ]"""
    private val logos = """[
        {"file_path":"/french.png","iso_639_1":"fr","vote_average":9.0,"vote_count":9},
        {"file_path":"/english.svg","iso_639_1":"en","vote_average":8.0,"vote_count":9},
        {"file_path":"/english.png","iso_639_1":"en","vote_average":5.0,"vote_count":3}
    ]"""
    private val details = """{"id":1063,
        "images":{"backdrops":$backdrops,"posters":$posters,"logos":$logos}}"""
    @Test
    fun largestTextlessWinsThenVoteCount() {
        assertEquals("https://image.tmdb.org/t/p/original/winner.jpg", largestTextlessBackdrop("""{"backdrops":$backdrops}"""))
        assertNull(largestTextlessBackdrop("""{"backdrops":[{"file_path":"/text.jpg","width":8000,"height":4500,"iso_639_1":"ja"}]}"""))
        assertNull(largestTextlessBackdrop("""{"backdrops":[{"file_path":"/bad.jpg","width":0,"height":1080}]}"""))
    }

    @Test
    fun oneLookupPicksBackdropTallArtAndEnglishPngLogo() = runTest {
        val requests = mutableListOf<String>()
        val picker = TmdbBackdropPicker({ "key" }) { url -> requests += url; details }
        val artwork = picker.artwork("tmdb:1063", "series", "before")
        assertEquals("https://image.tmdb.org/t/p/original/winner.jpg", artwork.backdrop)
        assertEquals("https://image.tmdb.org/t/p/original/key_art.jpg", artwork.tallArt)
        assertEquals("https://image.tmdb.org/t/p/original/english.png", artwork.logo)
        assertEquals(listOf("https://api.themoviedb.org/3/tv/1063?api_key=key&append_to_response=images&include_image_language=en,null,xx"), requests)
    }

    @Test
    fun smallKeyArtIsNotUsedAsTallArt() = runTest {
        val onlySmall = details.replace("\"width\":2000,\"height\":3000", "\"width\":1000,\"height\":1500")
        val picker = TmdbBackdropPicker({ "key" }) { onlySmall }
        assertNull(picker.artwork("tmdb:1063", "series", "x").tallArt)
    }

    @Test
    fun imdbLookupAndCacheWorkWithoutEnrichment() = runTest {
        val requests = mutableListOf<String>()
        val picker = TmdbBackdropPicker({ "setting-key" }) { url ->
            requests += url
            if ("/find/" in url) """{"tv_results":[{"id":1063}]}""" else details
        }
        val expected = "https://image.tmdb.org/t/p/original/winner.jpg"
        assertEquals(expected, picker.artwork("tt0423731", "series", "before").backdrop)
        assertEquals(expected, picker.artwork("tt0423731", "series", "another-original").backdrop)
        assertEquals(2, requests.size)
        assertTrue(requests.last().contains("/tv/1063?api_key=setting-key"))
    }

    // Details pages read this before drawing, so a known pick must come back without a fetch.
    @Test
    fun cachedReturnsEarlierPickWithoutWaiting() = runTest {
        val picker = TmdbBackdropPicker({ "key" }) { details }
        assertNull(picker.cached("tmdb:1063", "series", "addon-1080"))
        picker.artwork("tmdb:1063", "series", "hero-original")
        assertEquals("https://image.tmdb.org/t/p/original/winner.jpg", picker.cached("tmdb:1063", "series", "addon-1080")?.backdrop)
        val noKey = TmdbBackdropPicker({ "" }) { error("must not fetch") }
        assertEquals(TitleArtwork(), noKey.cached("tmdb:1063", "series", "addon-1080"))
    }

    @Test
    fun noKeyFailureAndEmptyImagesGiveNothing() = runTest {
        val empty = TitleArtwork()
        assertEquals(empty, TmdbBackdropPicker({ "" }) { error("must not fetch") }.artwork("tmdb:1063", "series", "before"))
        assertEquals(empty, TmdbBackdropPicker({ "key" }) { error("offline") }.artwork("tmdb:1063", "series", "before"))
        assertEquals(empty, TmdbBackdropPicker({ "key" }) { "not json" }.artwork("tmdb:1063", "series", "before"))
        assertEquals(empty, TmdbBackdropPicker({ "key" }) { """{"images":{}}""" }.artwork("tmdb:1063", "series", "before"))
    }

    @Test
    fun customSeasonArtworkDoesNotUseFranchiseFallbackId() = runTest {
        val picker = TmdbBackdropPicker({ "key" }) { error("must not fetch") }
        assertEquals(TitleArtwork(), picker.artwork("kitsu:49847", "series", "anilist-banner", "tt123"))
    }

    @Test
    fun tmdbArtworkCanUseDetailsImdbId() = runTest {
        val picker = TmdbBackdropPicker({ "key" }) { url ->
            if ("/find/" in url) """{"movie_results":[{"id":123}]}""" else details
        }
        assertEquals("https://image.tmdb.org/t/p/original/winner.jpg",
            picker.artwork("custom:1", "movie", "https://image.tmdb.org/t/p/w1280/old.jpg", "tt123").backdrop)
    }

    @Test
    fun cancellationPropagates() = runTest {
        val picker = TmdbBackdropPicker({ "key" }) { throw CancellationException("leaving screen") }
        assertFailsWith<CancellationException> { picker.artwork("tmdb:1063", "series", "before") }
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
