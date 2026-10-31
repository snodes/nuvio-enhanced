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
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Artwork-only lookup: independent of enrichment and never changes season metadata. */
internal object TmdbBackdrops {
    private val picker = TmdbBackdropPicker(
        apiKey = { TmdbSettingsRepository.snapshot().apiKey.trim() },
        get = ::httpGetText,
    )

    suspend fun resolve(id: String, type: String, original: String?, imdbId: String? = null): String? =
        picker.resolve(id, type, original, imdbId)
}

internal class TmdbBackdropPicker(
    private val apiKey: () -> String,
    private val get: suspend (String) -> String,
) {
    private val cache = linkedMapOf<String, String?>()
    private val mutex = Mutex()

    suspend fun resolve(id: String, type: String, original: String?, imdbId: String? = null): String? {
        val key = apiKey()
        if (key.isBlank()) return original
        val mediaType = TmdbService.normalizeMediaType(type)
        if (mediaType != "movie" && mediaType != "tv") return original
        // Do not turn unrelated custom/season artwork into franchise artwork via a details fallback ID.
        val sourceId = supportedId(id)
            ?: imdbId?.let(::supportedId)?.takeIf { original != null && isTmdbArtwork(original) }
            ?: return original
        return try {
            mutex.withLock {
                val cacheKey = "$key:$mediaType:$sourceId"
                if (cache.containsKey(cacheKey)) return@withLock cache[cacheKey] ?: original
                val selected = withTimeoutOrNull(5_000L) {
                    val tmdbId = if (sourceId.startsWith("tt")) {
                        val found = Json.parseToJsonElement(get(buildTmdbUrl(
                            "find/$sourceId", key, mapOf("external_source" to "imdb_id"),
                        ))) as JsonObject
                        val results = found[if (mediaType == "tv") "tv_results" else "movie_results"] as? JsonArray
                        (results?.firstOrNull() as? JsonObject)?.get("id")?.jsonPrimitive?.intOrNull
                            ?.takeIf { it > 0 }?.toString() ?: return@withTimeoutOrNull null
                    } else sourceId
                    val images = get(buildTmdbUrl(
                        "$mediaType/$tmdbId/images", key, mapOf("include_image_language" to "null,xx"),
                    ))
                    largestTextlessBackdrop(images)
                }
                cache[cacheKey] = selected
                if (cache.size > 256) cache.remove(cache.keys.first())
                selected ?: original
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            original
        }
    }

    private fun supportedId(id: String): String? {
        val normalized = id.removePrefix("tmdb:").removePrefix("movie:").removePrefix("series:")
            .substringBefore(':').substringBefore('/').trim()
        return normalized.takeIf { it.matches(Regex("(?:[0-9]+|tt[0-9]+)")) }
    }
}

internal fun largestTextlessBackdrop(body: String): String? {
    val images = (Json.parseToJsonElement(body) as? JsonObject)?.get("backdrops") as? JsonArray
        ?: return null
    return images.mapNotNull { it as? JsonObject }
        .filter { image ->
            val language = image["iso_639_1"]?.jsonPrimitive?.contentOrNull
            val path = image["file_path"]?.jsonPrimitive?.contentOrNull.orEmpty()
            (language == null || language == "xx") &&
                path.matches(Regex("/[A-Za-z0-9_]+\\.[A-Za-z0-9]+")) &&
                (image["width"]?.jsonPrimitive?.intOrNull ?: 0) > 0 &&
                (image["height"]?.jsonPrimitive?.intOrNull ?: 0) > 0
        }
        .maxWithOrNull(compareBy<JsonObject> {
            it.getValue("width").jsonPrimitive.intOrNull!!.toLong() *
                it.getValue("height").jsonPrimitive.intOrNull!!
        }.thenBy { it["vote_count"]?.jsonPrimitive?.intOrNull ?: 0 })
        ?.get("file_path")?.jsonPrimitive?.contentOrNull
        ?.let { "https://image.tmdb.org/t/p/original$it" }
}

private fun isTmdbArtwork(url: String): Boolean =
    url.startsWith("https://image.tmdb.org/t/p/") || url.startsWith("http://image.tmdb.org/t/p/")

/** Keep phone hero source pixels, including its poster/fallback mode. */
internal fun originalHeroArtworkUrl(url: String?): String? {
    if (url == null || !isTmdbArtwork(url)) return url
    val path = url.substringAfter("/t/p/").substringAfter('/', "")
    return if (path.isBlank()) url else "https://image.tmdb.org/t/p/original/$path"
}
