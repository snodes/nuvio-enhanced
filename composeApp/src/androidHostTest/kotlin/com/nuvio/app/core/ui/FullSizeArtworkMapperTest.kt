package com.nuvio.app.core.ui

import coil3.ImageLoader
import android.app.Application
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import coil3.request.ImageRequest
import coil3.request.Options
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class FullSizeArtworkMapperTest {
    @Test
    fun mapperUpgradesPlainUrlsAndImageRequestDataBeforeCoilUriMapping() {
        val context = RuntimeEnvironment.getApplication()
        val loader = ImageLoader.Builder(context).components { add(FullSizeArtworkMapper()) }.build()
        try {
            val original = "https://image.tmdb.org/t/p/original/art.jpg"
            val small = "https://image.tmdb.org/t/p/w500/art.jpg"
            val request = ImageRequest.Builder(context).data(small).build()
            for (data in listOf(small, request.data)) {
                val mapped = loader.components.map(data, Options(context))
                assertEquals(original, mapped.toString())
            }
            // CustomPosterFallbackInterceptor retries with String data through the same registry.
            val fallback = request.newBuilder().data("https://media.kitsu.app/anime/poster_images/12/small.jpg").build()
            assertEquals("https://media.kitsu.app/anime/poster_images/12/original.jpg",
                loader.components.map(fallback.data, Options(context)).toString())
        } finally {
            loader.shutdown()
        }
    }
}
