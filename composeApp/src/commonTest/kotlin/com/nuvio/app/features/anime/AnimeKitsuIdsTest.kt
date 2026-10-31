package com.nuvio.app.features.anime

import com.nuvio.app.features.watched.WatchedItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AnimeKitsuIdsTest {
    @Test fun seasonsMapToTheirOwnKitsuEntries() {
        // Haikyu!!: IMDb season 2 is Kitsu's "Haikyu!! 2nd Season".
        assertEquals(AnimeKitsuIds.KitsuEpisode("kitsu:10076", 1, 3), AnimeKitsuIds.toKitsu("tt3398540", 2, 3))
        assertEquals("kitsu:10076:3", AnimeKitsuIds.toKitsu("tt3398540", 2, 3)?.videoId)
        // Cyberpunk: Edgerunners season 2 is Kitsu's Edgerunners 2.
        assertEquals("kitsu:50016", AnimeKitsuIds.toKitsu("tt12590266", 2, 4)?.kitsuMetaId)
    }

    @Test fun unsplitShowsMapOnlyTheirFirstSeason() {
        assertEquals("kitsu:6448", AnimeKitsuIds.toKitsu("tt2098220", 1, 15)?.kitsuMetaId)
        assertNull(AnimeKitsuIds.toKitsu("tt2098220", 2, 1))
    }

    @Test fun ambiguousUnknownAndNonImdbIdsStayPut() {
        assertNull(AnimeKitsuIds.toKitsu("tt3398540", 4, 1)) // season 4 is split across two Kitsu entries
        assertNull(AnimeKitsuIds.toKitsu("tt0903747", 1, 1)) // not anime
        assertNull(AnimeKitsuIds.toKitsu("kitsu:6448", 1, 1))
        assertNull(AnimeKitsuIds.toKitsu("tt3398540", 1, null))
    }

    @Test fun watchedMarksConvertButTrackerItemsDoNot() {
        val mark = WatchedItem(id = "tt3398540", type = "series", name = "Haikyu!!", season = 2, episode = 2, markedAtEpochMs = 5L)
        val kitsu = mark.toKitsuAnime()
        assertEquals("kitsu:10076", kitsu.id)
        assertEquals(1, kitsu.season)
        assertEquals(2, kitsu.episode)
        assertEquals(5L, kitsu.markedAtEpochMs)
        val tracked = mark.copy(trackingProviderId = "trakt")
        assertEquals(tracked, tracked.toKitsuAnime())
    }
}
