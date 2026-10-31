package com.nuvio.app.features.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nuvio.app.features.addons.ManagedAddon
import com.nuvio.app.features.catalog.fetchCatalogPage
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
    val relayUrls = addons.filter {
        it.enabled && it.manifest?.id == "local.anilist-watching"
    }.map { it.manifestUrl }
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
