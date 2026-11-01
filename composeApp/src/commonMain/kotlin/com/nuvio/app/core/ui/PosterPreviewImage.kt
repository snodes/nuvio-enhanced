package com.nuvio.app.core.ui

// Only enlarge TMDB thumbnails. Preserve original artwork and other addon URLs.
internal fun posterPreviewImageUrl(imageUrl: String?): String? = imageUrl?.let { url ->
    tmdbThumbnailSize.replace(url) { match ->
        val width = match.groupValues[2].toIntOrNull()
        if (width != null && width < 780) "${match.groupValues[1]}w780/" else match.value
    }
}

private val tmdbThumbnailSize = Regex("^(https?://image\\.tmdb\\.org/t/p/)w([0-9]+)/")

// Decode bound for the preview poster: about the preview's size on a 3x screen, 2:3 aspect.
internal const val PosterPreviewDecodeWidthPx = 1080
internal const val PosterPreviewDecodeHeightPx = 1620
