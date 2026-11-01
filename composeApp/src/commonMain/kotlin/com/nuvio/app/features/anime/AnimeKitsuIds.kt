package com.nuvio.app.features.anime

/**
 * Anime is tracked under Kitsu ids. Rows backed by TMDB/IMDb (search, MDBList lists, TMDB catalogs)
 * hand out IMDb ids for the same shows, which split one show's history into two Continue Watching
 * cards. [toKitsu] rewrites an IMDb episode to its Kitsu entry before it is saved, and the one-time
 * history migration uses it to fold old IMDb records into Kitsu.
 *
 * Kitsu has one entry per season or sequel, numbered from episode 1, and the app stores Kitsu
 * episodes as season 1. So IMDb (show, season S, episode E) becomes (Kitsu entry for S, 1, E).
 * Seasons that map to zero or several Kitsu entries (e.g. a cour split) stay on IMDb.
 */
internal object AnimeKitsuIds {
    /** Kitsu equivalent of one episode, or null when the IMDb episode has no unambiguous Kitsu entry. */
    data class KitsuEpisode(val kitsuMetaId: String, val season: Int, val episode: Int) {
        val videoId: String get() = "$kitsuMetaId:$episode"
    }

    private val table: Map<String, Map<Int, Int>> by lazy {
        val result = HashMap<String, HashMap<Int, Int>>(8192)
        for (chunk in AnimeKitsuIdTableChunks) {
            for (line in chunk.split('\n')) {
                val parts = line.split(' ')
                if (parts.size != 3) continue
                val season = parts[1].toIntOrNull() ?: continue
                val kitsu = parts[2].toIntOrNull() ?: continue
                result.getOrPut("tt${parts[0]}") { HashMap() }[season] = kitsu
            }
        }
        result
    }

    fun isImdbId(id: String?): Boolean = id != null && id.startsWith("tt") && id.drop(2).all(Char::isDigit) && id.length > 2

    /** Kitsu meta id for an IMDb series season, or null when unknown or ambiguous. */
    fun kitsuMetaIdFor(imdbId: String, season: Int?): String? {
        val seasons = table[imdbId] ?: return null
        val kitsu = when {
            season == null -> seasons[0]
            seasons.containsKey(season) -> seasons[season]
            // An IMDb show with a single unsplit Kitsu entry: only its first season lines up.
            season == 1 -> seasons[0]
            else -> null
        } ?: return null
        return "kitsu:$kitsu"
    }

    fun toKitsu(imdbId: String, season: Int?, episode: Int?): KitsuEpisode? {
        if (!isImdbId(imdbId) || episode == null || episode <= 0) return null
        val kitsu = kitsuMetaIdFor(imdbId, season ?: 1) ?: return null
        return KitsuEpisode(kitsuMetaId = kitsu, season = 1, episode = episode)
    }
}

/** The Kitsu form of an IMDb anime episode mark; anything else comes back unchanged. */
internal fun com.nuvio.app.features.watched.WatchedItem.toKitsuAnime(): com.nuvio.app.features.watched.WatchedItem {
    if (trackingProviderId != null) return this
    val kitsu = AnimeKitsuIds.toKitsu(id, season, episode) ?: return this
    return copy(id = kitsu.kitsuMetaId, season = kitsu.season, episode = kitsu.episode, videoId = kitsu.videoId)
}

/** The Kitsu form of IMDb anime playback progress; anything else comes back unchanged. */
internal fun com.nuvio.app.features.watchprogress.WatchProgressEntry.toKitsuAnime():
    com.nuvio.app.features.watchprogress.WatchProgressEntry {
    if (trackingProviderId != null) return this
    val kitsu = AnimeKitsuIds.toKitsu(parentMetaId, seasonNumber, episodeNumber) ?: return this
    return copy(
        parentMetaId = kitsu.kitsuMetaId,
        videoId = kitsu.videoId,
        seasonNumber = kitsu.season,
        episodeNumber = kitsu.episode,
        progressKey = null,
    )
}
