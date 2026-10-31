package com.nuvio.app.core.ui

import coil3.map.Mapper
import coil3.request.Options

/** Runs before Coil's String-to-URI mapper, covering plain URLs and ImageRequest data/fallbacks. */
internal class FullSizeArtworkMapper : Mapper<String, String> {
    override fun map(data: String, options: Options): String = fullSizeArtworkUrl(data)
}
