package com.nuvio.app.core.ui

import coil3.ImageLoader

/** Full-size CDN artwork, resized with Lanczos3 instead of Coil's nearest-neighbour default. */
internal actual fun ImageLoader.Builder.configurePlatformImageLoader(): ImageLoader.Builder =
    components {
        add(FullSizeArtworkMapper())
        add(PosterBitmapDecoder.Factory())
    }

internal actual val platformProvidesImageLoader: Boolean = false
