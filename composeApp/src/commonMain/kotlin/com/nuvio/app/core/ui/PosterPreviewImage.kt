package com.nuvio.app.core.ui

// TMDB posters load at their original size (decoding is bounded below), whatever size the
// addon linked: plain widths (w342) and AIOMetadata's w600_and_h900_bestv2 alike. Other URLs stay.
internal fun posterPreviewImageUrl(imageUrl: String?): String? = imageUrl?.let { url ->
    tmdbThumbnailSize.replace(url) { match -> "${match.groupValues[1]}original/" }
}

private val tmdbThumbnailSize = Regex("^(https?://image\\.tmdb\\.org/t/p/)w[0-9]+(?:_and_h[0-9]+[A-Za-z0-9_]*)?/")

// Decode bound for the preview poster: about the preview's size on a 3x screen, 2:3 aspect.
internal const val PosterPreviewDecodeWidthPx = 1080
internal const val PosterPreviewDecodeHeightPx = 1620
