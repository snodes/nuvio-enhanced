package com.nuvio.app.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ListCardArtworkTest {
    @Test
    fun `poster wins over progress screenshots and episode thumbnails`() {
        assertEquals("poster", listCardArtworkUrl(" poster ", "episode", "background"))
    }

    @Test
    fun `fallback is used only when poster is absent or blank`() {
        assertEquals("episode", listCardArtworkUrl(null, "episode", "background"))
        assertEquals("background", listCardArtworkUrl(" ", null, " background "))
        assertNull(listCardArtworkUrl(null, null, " "))
    }
}
