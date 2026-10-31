package com.nuvio.app.core.ui

/** Vertical list-card slots prefer the show poster, regardless of episode-thumbnail preferences. */
internal fun listCardArtworkUrl(poster: String?, vararg fallbacks: String?): String? =
    (listOf(poster) + fallbacks).firstOrNull { !it.isNullOrBlank() }?.trim()
