package com.nuvio.app.features.home

import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeContinueWatchingSeasonArtTest {
    private fun item(id: String = "kitsu:49847") = ContinueWatchingItem(
        parentMetaId = id, parentMetaType = "series", videoId = "$id:1",
        title = "Steel Ball Run", subtitle = "S1 E1", imageUrl = "old-background",
        poster = "old-poster", background = "old-background", logo = "old-logo",
        rawPosterUrl = "cached-poster", rawBackgroundUrl = "cached-background",
        seasonNumber = 1, episodeNumber = 1,
        resumePositionMs = 14514, durationMs = 2924203, progressFraction = 0.005f,
    )
    private fun art(id: String = "kitsu:49847", banner: String? = "season-banner") = MetaPreview(
        id = id, type = "series", name = "Relay name", poster = "season-poster", banner = banner,
    )

    @Test fun capturedRelayArtReplacesBothReportedStalePosters() {
        run {
            val before = item("kitsu:49847").copy(poster = "https://image.tmdb.org/t/p/w600_and_h900_bestv2/ogAWwbh3frWtiTyyXrZaVFtqCgp.jpg", background = "https://image.tmdb.org/t/p/original/l2gc2dPRUPe3bAfYCRTjc6uAMmB.jpg")
            val parsed = HomeCatalogParser.parseCatalog("""{"metas":[{"id":"kitsu:49847","type":"series","name":"Anime","poster":"https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx190327-riJNCFL7w9y4.jpg","background":"https://s4.anilist.co/file/anilistcdn/media/anime/banner/190327-bXr8tFZUdxOw.jpg"}]}""")
            val after = applyContinueWatchingSeasonArt(listOf(before), parsed).single()
            assertEquals("https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx190327-riJNCFL7w9y4.jpg", after.poster)
            assertEquals("https://s4.anilist.co/file/anilistcdn/media/anime/banner/190327-bXr8tFZUdxOw.jpg", after.background)
        }
        run {
            val before = item("kitsu:12").copy(poster = "https://image.tmdb.org/t/p/w600_and_h900_bestv2/uiIB9ctqZFbfRXXimtpmZb5dusi.jpg", background = "https://image.tmdb.org/t/p/original/v38qp4bySLTXYu3MF8r5GD51FN3.jpg")
            val parsed = HomeCatalogParser.parseCatalog("""{"metas":[{"id":"kitsu:12","type":"series","name":"Anime","poster":"https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx21-ELSYx3yMPcKM.jpg","background":"https://s4.anilist.co/file/anilistcdn/media/anime/banner/21-wf37VakJmZqs.jpg"}]}""")
            val after = applyContinueWatchingSeasonArt(listOf(before), parsed).single()
            assertEquals("https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/bx21-ELSYx3yMPcKM.jpg", after.poster)
            assertEquals("https://s4.anilist.co/file/anilistcdn/media/anime/banner/21-wf37VakJmZqs.jpg", after.background)
        }
    }

    @Test fun freshExactEntryOverridesStoredAndCachedArtWithoutChangingPlayback() {
        val before = item()
        val after = applyContinueWatchingSeasonArt(listOf(before), listOf(art())).single()
        assertEquals(before.copy(poster = "season-poster", background = "season-banner",
            logo = null, imageUrl = "season-banner", rawPosterUrl = "season-poster",
            rawBackgroundUrl = "season-banner"), after)
    }

    @Test fun onePieceAndNextUpUseSameExactIdProjection() {
        val before = item("kitsu:12").copy(isNextUp = true, episodeNumber = 1178)
        val after = applyContinueWatchingSeasonArt(listOf(before), listOf(art("kitsu:12"))).single()
        assertEquals("season-poster", after.poster)
        assertEquals(before.videoId, after.videoId)
        assertEquals(before.episodeNumber, after.episodeNumber)
        assertEquals(true, after.isNextUp)
    }

    @Test fun noBannerClearsFranchiseBackdropButPreservesEpisodeThumbnail() {
        val before = item().copy(episodeThumbnail = "episode-thumb")
        val after = applyContinueWatchingSeasonArt(listOf(before), listOf(art(banner = null))).single()
        assertEquals(null, after.background)
        assertEquals(null, after.rawBackgroundUrl)
        assertEquals(null, after.logo)
        assertEquals("episode-thumb", after.imageUrl)
    }

    @Test fun differentPartOrFranchiseAndMissingPosterLeaveOriginalUntouched() {
        val before = item()
        for (entries in listOf(emptyList(), listOf(art("kitsu:other-part")),
            listOf(art().copy(poster = "")), listOf(art("tt123")))) {
            assertEquals(listOf(before), applyContinueWatchingSeasonArt(listOf(before), entries))
        }
    }

    @Test fun unavailableRelayFallsBackAndCancellationPropagates() = runTest {
        assertEquals(emptyList(), loadContinueWatchingSeasonArt(listOf("relay")) { error("offline") })
        assertEquals(emptyList(), loadContinueWatchingSeasonArt(emptyList()) { error("must not fetch") })
        assertEquals(emptyList(), loadContinueWatchingSeasonArt(listOf("slow-relay")) {
            delay(9_000L)
            listOf(art())
        })
        assertFailsWith<CancellationException> {
            loadContinueWatchingSeasonArt(listOf("relay")) { throw CancellationException("profile changed") }
        }
    }

    @Test fun usesInstalledRelayUrlRatherThanLocalhost() = runTest {
        val seen = mutableListOf<String>()
        val result = loadContinueWatchingSeasonArt(listOf("https://phone-relay/manifest.json")) {
            seen += it
            listOf(art())
        }
        assertEquals(listOf("https://phone-relay/manifest.json"), seen)
        assertEquals(listOf(art()), result)
    }

    @Test fun relayMatchesItsCurrentAndPreRenameIds() {
        assertTrue(isAniListRelay("local.anilist-watching"))
        assertTrue(isAniListRelay("jacob.anilist-watching"))
        assertFalse(isAniListRelay("community.anilist"))
        assertFalse(isAniListRelay(null))
    }
}
