package com.nuvio.app.features.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nuvio.app.features.addons.ManagedAddon
import com.nuvio.app.features.catalog.fetchCatalogPage
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

// The relay already resolves exact AniList entries and maps them to Kitsu IDs. Do not
// title-search here: that would turn a part/season back into its franchise again.
@Composable
internal fun rememberContinueWatchingSeasonArt(
    items: List<ContinueWatchingItem>,
    addons: List<ManagedAddon>,
    profileId: Int,
): List<ContinueWatchingItem> {
    val relayUrls = addons.filter { it.enabled && isAniListRelay(it.manifest?.id) }.map { it.manifestUrl }
    var artwork by remember(profileId, relayUrls) { mutableStateOf<List<MetaPreview>>(emptyList()) }
    LaunchedEffect(profileId, relayUrls) {
        artwork = loadContinueWatchingSeasonArt(relayUrls) { url ->
            fetchCatalogPage(
                manifestUrl = url,
                type = "anime",
                catalogId = "anilist-watching",
                forceRefresh = true,
            ).items
        }
    }
    return remember(items, artwork) { applyContinueWatchingSeasonArt(items, artwork) }
}

// iOS keeps only addon URLs on disk and the manifest it last fetched in memory, so a phone
// that installed the relay before its rename still reports the old id until a refresh.
private val AniListRelayIds = setOf("local.anilist-watching", "jacob.anilist-watching")

internal fun isAniListRelay(manifestId: String?): Boolean = manifestId in AniListRelayIds

internal suspend fun loadContinueWatchingSeasonArt(
    relayUrls: List<String>,
    fetch: suspend (String) -> List<MetaPreview>,
): List<MetaPreview> = relayUrls.flatMap { url ->
    try {
        withTimeoutOrNull(8_000L) { fetch(url) }.orEmpty()
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        emptyList()
    }
}

internal fun applyContinueWatchingSeasonArt(
    items: List<ContinueWatchingItem>,
    artwork: List<MetaPreview>,
): List<ContinueWatchingItem> {
    val exactEntries = artwork.filter {
        (it.id.startsWith("kitsu:") || it.id.startsWith("mal:")) &&
            !it.poster.isNullOrBlank()
    }.associateBy { it.id }
    return items.map { item ->
        val art = exactEntries[item.parentMetaId] ?: return@map item
        val banner = art.banner?.takeIf(String::isNotBlank)
        item.copy(
            poster = art.poster,
            background = banner,
            // A missing exact-entry banner/logo must not keep the old franchise's.
            logo = art.logo?.takeIf(String::isNotBlank),
            imageUrl = item.episodeThumbnail?.takeIf(String::isNotBlank) ?: banner ?: art.poster,
            rawPosterUrl = art.poster,
            rawBackgroundUrl = banner,
        )
    }
}

// Synced progress can arrive with no poster or backdrop (the phone never played it), which leaves
// the wide card's poster slot showing a spoiler-blurred episode still. Look the show up once
// (MetaDetailsRepository caches it) and fill in only what is missing, as the PC already stores.
@Composable
internal fun rememberContinueWatchingMissingPosters(
    items: List<ContinueWatchingItem>,
): List<ContinueWatchingItem> {
    val missing = items.filter { it.poster.isNullOrBlank() }
        .map { it.parentMetaType to it.parentMetaId }
        .distinct()
    var posters by remember { mutableStateOf<Map<String, MetaDetails>>(emptyMap()) }
    LaunchedEffect(missing) {
        missing.filterNot { (_, id) -> id in posters }.forEach { (type, id) ->
            val meta = try {
                MetaDetailsRepository.fetch(type, id)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                null
            } ?: return@forEach
            posters = posters + (id to meta)
        }
    }
    return remember(items, posters) { applyMissingPosters(items, posters) }
}

internal fun applyMissingPosters(
    items: List<ContinueWatchingItem>,
    metaById: Map<String, MetaDetails>,
): List<ContinueWatchingItem> = items.map { item ->
    if (!item.poster.isNullOrBlank()) return@map item
    val meta = metaById[item.parentMetaId] ?: return@map item
    val poster = meta.poster?.takeIf(String::isNotBlank) ?: return@map item
    item.copy(
        poster = poster,
        background = item.background?.takeIf(String::isNotBlank) ?: meta.background?.takeIf(String::isNotBlank),
        rawPosterUrl = item.rawPosterUrl?.takeIf(String::isNotBlank) ?: poster,
    )
}
