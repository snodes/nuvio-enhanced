package com.nuvio.app.features.home

import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaExternalRating
import com.nuvio.app.features.tmdb.TitleArtwork
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeHeroMetaRepositoryTest {

    private val preview = MetaPreview(
        id = "tt1",
        type = "series",
        name = "Show",
        imdbRating = "7.1",
        genres = listOf("Drama"),
        runtime = "52 min",
        status = "Continuing",
    )

    @Test
    fun `artwork lookup sets backdrop tall art and English logo but not season poster`() {
        val original = preview.copy(poster = "season-poster", banner = "soft-backdrop", logo = "french-logo")
        val merged = original.withHeroMeta(
            HomeHeroMeta(artwork = TitleArtwork(backdrop = "largest-textless", tallArt = "key-art", logo = "english-logo")),
        )
        assertEquals("largest-textless", merged.banner)
        assertEquals("key-art", merged.heroTallArt)
        assertEquals("english-logo", merged.logo)
        assertEquals("season-poster", merged.poster)
        assertEquals("french-logo", original.withHeroMeta(HomeHeroMeta(artwork = TitleArtwork())).logo)
        assertEquals(original.id, merged.id)
        assertEquals(original.name, merged.name)
    }

    @Test
    fun `full meta fills hero fields over catalog values`() {
        val merged = preview.withHeroMeta(
            HomeHeroMeta(
                details = MetaDetails(
                    id = "tt1",
                    type = "series",
                    name = "Show",
                    imdbRating = "8.4",
                    genres = listOf("Crime"),
                    runtime = "58 min",
                    status = "Ended",
                    ageRating = "TV-MA",
                    language = "en",
                ),
            ),
        )

        assertEquals("8.4", merged.imdbRating)
        assertEquals(listOf("Crime"), merged.genres)
        assertEquals("58 min", merged.runtime)
        assertEquals("Ended", merged.status)
        assertEquals("TV-MA", merged.ageRating)
        assertEquals("en", merged.language)
    }

    @Test
    fun `catalog values stay when full meta has gaps`() {
        val merged = preview.withHeroMeta(
            HomeHeroMeta(
                details = MetaDetails(
                    id = "tt1",
                    type = "series",
                    name = "Show",
                    imdbRating = " ",
                    runtime = "",
                ),
            ),
        )

        assertEquals("7.1", merged.imdbRating)
        assertEquals(listOf("Drama"), merged.genres)
        assertEquals("52 min", merged.runtime)
        assertEquals("Continuing", merged.status)
        assertEquals(null, merged.ageRating)
    }

    @Test
    fun `ratings attach without full meta`() {
        val ratings = listOf(MetaExternalRating(source = "imdb", value = 8.4))

        val merged = preview.withHeroMeta(HomeHeroMeta(ratings = ratings))

        assertEquals(ratings, merged.externalRatings)
        assertEquals("7.1", merged.imdbRating)
        assertEquals("52 min", merged.runtime)
    }
}
