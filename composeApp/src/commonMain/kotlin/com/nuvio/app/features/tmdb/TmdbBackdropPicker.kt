package com.nuvio.app.features.tmdb

import com.nuvio.app.features.addons.httpGetText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Artwork picked for one title; any field is null when no source has it.
 * [tallArt] is textless portrait key art for tall phone heroes, only when the title has no 4K backdrop
 * (textless TMDB posters are often fan art, so a 4K backdrop wins whenever one exists). [logo] is TMDB's best English logo (the addon's can be any language).
 */
internal data class TitleArtwork(
    val backdrop: String? = null,
    val tallArt: String? = null,
    val logo: String? = null,
)

/** [logo] unless the TMDB setting shows titles in their original language; then the addon's logo stays. */
internal fun englishLogo(logo: String?): String? =
    logo?.takeUnless { TmdbSettingsRepository.snapshot().let { it.enabled && it.titleLanguage == TmdbTitleLanguage.ORIGINAL } }

/** Artwork-only lookup: independent of enrichment and never changes season metadata. */
internal object TmdbBackdrops {
    private val picker = TmdbBackdropPicker(
        // The typed-in key, else the build's own TMDB key (phones usually have no typed key).
        apiKey = { TmdbSettingsRepository.effectiveApiKey().trim() },
        get = ::httpGetText,
    )

    suspend fun artwork(id: String, type: String, original: String?, imdbId: String? = null): TitleArtwork =
        picker.artwork(id, type, original, imdbId)

    fun cached(id: String, type: String, original: String?, imdbId: String? = null): TitleArtwork? =
        picker.cached(id, type, original, imdbId)
}

internal class TmdbBackdropPicker(
    private val apiKey: () -> String,
    private val get: suspend (String) -> String,
) {
    private val cache = linkedMapOf<String, TitleArtwork>()
    private val mutex = Mutex()

    private data class Lookup(val key: String, val mediaType: String, val sourceId: String) {
        val cacheKey get() = "$key:$mediaType:$sourceId"
    }

    // Null when this title can't be looked up on TMDB; callers then keep the addon's artwork.
    private fun lookup(id: String, type: String, original: String?, imdbId: String?): Lookup? {
        val key = apiKey().takeIf { it.isNotBlank() } ?: return null
        val mediaType = TmdbService.normalizeMediaType(type).takeIf { it == "movie" || it == "tv" } ?: return null
        // Do not turn unrelated custom/season artwork into franchise artwork via a details fallback ID.
        val sourceId = supportedId(id)
            ?: imdbId?.let(::supportedId)?.takeIf { original != null && isTmdbArtwork(original) }
            ?: return null
        return Lookup(key, mediaType, sourceId)
    }

    /**
     * The final artwork without waiting: an earlier pick (e.g. the home hero's), or an empty result when
     * TMDB can't help. Null only when a network lookup is still needed, so screens can hold off on the
     * addon's lower-resolution artwork instead of flashing it before the pick arrives.
     */
    fun cached(id: String, type: String, original: String?, imdbId: String? = null): TitleArtwork? {
        val cacheKey = lookup(id, type, original, imdbId)?.cacheKey ?: return TitleArtwork()
        if (!mutex.tryLock()) return null
        return try {
            cache[cacheKey]
        } finally {
            mutex.unlock()
        }
    }

    suspend fun artwork(id: String, type: String, original: String?, imdbId: String? = null): TitleArtwork {
        val lookup = lookup(id, type, original, imdbId) ?: return TitleArtwork()
        val cacheKey = lookup.cacheKey
        // The lock guards only the cache, never the network, so cached() can always read earlier picks.
        mutex.withLock { cache[cacheKey]?.let { return it } }
        return try {
            val selected = withTimeoutOrNull(5_000L) { fetch(lookup) } ?: TitleArtwork()
            mutex.withLock {
                cache[cacheKey] = selected
                if (cache.size > 256) cache.remove(cache.keys.first())
            }
            selected
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            TitleArtwork()
        }
    }

    private suspend fun fetch(lookup: Lookup): TitleArtwork? {
        val (key, mediaType, sourceId) = lookup
        val tmdbId = if (sourceId.startsWith("tt")) {
            val found = Json.parseToJsonElement(get(buildTmdbUrl(
                "find/$sourceId", key, mapOf("external_source" to "imdb_id"),
            ))) as JsonObject
            val results = found[if (mediaType == "tv") "tv_results" else "movie_results"] as? JsonArray
            (results?.firstOrNull() as? JsonObject)?.get("id")?.jsonPrimitive?.intOrNull
                ?.takeIf { it > 0 }?.toString() ?: return null
        } else sourceId
        // One request: textless backdrops and posters plus English logos.
        val details = Json.parseToJsonElement(get(buildTmdbUrl(
            "$mediaType/$tmdbId", key,
            mapOf("append_to_response" to "images", "include_image_language" to "en,null,xx"),
        ))) as JsonObject
        val images = details["images"] as? JsonObject ?: JsonObject(emptyMap())
        val backdrop = largestTextless(images, "backdrops")
        val tallArt = largestTextless(images, "posters")
            ?.takeIf { it.width >= TALL_ART_MIN_WIDTH && it.height > it.width }
            ?.takeIf { (backdrop?.width ?: 0) < UHD_BACKDROP_WIDTH }
        return TitleArtwork(
            backdrop = backdrop?.url,
            tallArt = tallArt?.url,
            logo = tmdbEnglishLogo(images),
        )
    }

    private fun supportedId(id: String): String? {
        val normalized = id.removePrefix("tmdb:").removePrefix("movie:").removePrefix("series:")
            .substringBefore(':').substringBefore('/').trim()
        return normalized.takeIf { it.matches(Regex("(?:[0-9]+|tt[0-9]+)")) }
    }
}

// A 2:3 poster this wide shows at least as many source pixels per screen pixel on a tall phone hero
// as a 3840x2160 backdrop does (the backdrop is cropped to about a third of its width there).
internal const val TALL_ART_MIN_WIDTH = 1500

// Titles with a backdrop this wide keep it on tall heroes too. Tall art only beat a 4K backdrop on paper:
// on the phone, HxH got a pencil fan sketch and MHA got Season 1 key art that didn't match its details page.
internal const val UHD_BACKDROP_WIDTH = 3840

private data class SizedImage(val url: String, val width: Int, val height: Int)

private val tmdbImagePath = Regex("/[A-Za-z0-9_]+\\.[A-Za-z0-9]+")

private fun JsonObject.int(name: String): Int = this[name]?.jsonPrimitive?.intOrNull ?: 0

private fun JsonObject.language(): String? = this["iso_639_1"]?.jsonPrimitive?.contentOrNull

private fun JsonObject.path(): String = this["file_path"]?.jsonPrimitive?.contentOrNull.orEmpty()

/** Largest textless (no-language) image of [kind] ("backdrops" or "posters"), ties broken by votes. */
private fun largestTextless(images: JsonObject, kind: String): SizedImage? =
    (images[kind] as? JsonArray).orEmpty()
        .mapNotNull { it as? JsonObject }
        .filter { image ->
            val language = image.language()
            (language == null || language == "xx") && image.path().matches(tmdbImagePath) &&
                image.int("width") > 0 && image.int("height") > 0
        }
        .maxWithOrNull(compareBy<JsonObject> { it.int("width").toLong() * it.int("height") }
            .thenBy { it.int("vote_count") })
        ?.let { SizedImage("https://image.tmdb.org/t/p/original${it.path()}", it.int("width"), it.int("height")) }

internal fun largestTextlessBackdrop(body: String): String? =
    (Json.parseToJsonElement(body) as? JsonObject)?.let { largestTextless(it, "backdrops") }?.url

/** TMDB's top-rated English PNG logo (SVG logos don't render everywhere). */
internal fun tmdbEnglishLogo(images: JsonObject): String? =
    (images["logos"] as? JsonArray).orEmpty()
        .mapNotNull { it as? JsonObject }
        .filter { it.language() == "en" && it.path().matches(tmdbImagePath) && it.path().endsWith(".png") }
        .maxWithOrNull(compareBy<JsonObject> { it["vote_average"]?.jsonPrimitive?.doubleOrNull ?: 0.0 }
            .thenBy { it.int("vote_count") })
        ?.let { "https://image.tmdb.org/t/p/original${it.path()}" }

private fun isTmdbArtwork(url: String): Boolean =
    url.startsWith("https://image.tmdb.org/t/p/") || url.startsWith("http://image.tmdb.org/t/p/")

/** Keep phone hero source pixels, including its poster/fallback mode. */
internal fun originalHeroArtworkUrl(url: String?): String? {
    if (url == null || !isTmdbArtwork(url)) return url
    val path = url.substringAfter("/t/p/").substringAfter('/', "")
    return if (path.isBlank()) url else "https://image.tmdb.org/t/p/original/$path"
}
