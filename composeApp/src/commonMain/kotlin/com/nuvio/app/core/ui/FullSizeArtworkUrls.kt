package com.nuvio.app.core.ui

/** Only upgrade documented CDN variants; never guess at arbitrary provider URLs or query signatures. */
internal fun fullSizeArtworkUrl(url: String): String = when {
    TmdbVariant.matches(url) -> url.replaceFirst(TmdbSize, "/t/p/original/")
    MetahubVariant.matches(url) -> url.replaceFirst(MetahubSize, "/large/")
    KitsuVariant.matches(url) -> url.replaceFirst(KitsuSize, "/original.")
    else -> url
}

private val TmdbVariant = Regex("https://image\\.tmdb\\.org/t/p/(?:w[0-9]+(?:_and_h[0-9]+_bestv2)?|h[0-9]+)/[^?#]+")
private val TmdbSize = Regex("/t/p/[^/]+/")
private val MetahubVariant = Regex("https://images\\.metahub\\.space/(?:poster|background|logo)/(?:small|medium)/[^?#]+")
private val MetahubSize = Regex("/(?:small|medium)/")
private val KitsuVariant = Regex("https://(?:media\\.kitsu\\.(?:app|io)|media\\.kitsu\\.io)/anime/(?:poster_images|cover_images)/[0-9]+/(?:tiny|small|medium|large)\\.[^?#]+")
private val KitsuSize = Regex("/(?:tiny|small|medium|large)\\.")
