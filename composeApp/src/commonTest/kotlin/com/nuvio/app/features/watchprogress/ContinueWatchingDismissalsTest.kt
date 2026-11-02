package com.nuvio.app.features.watchprogress

import kotlin.test.Test
import kotlin.test.assertEquals

class ContinueWatchingDismissalsTest {
    @Test
    fun readsRemovalKeysFromTheOtherDevicesPayload() {
        val phonePayload = """{"isVisible":true,"style":"Poster","dismissedNextUpKeys":["tt10233448|1|1","tt0877057|1|1"],"some_newer_field":1}"""
        assertEquals(setOf("tt10233448|1|1", "tt0877057|1|1"), ContinueWatchingPreferencesRepository.dismissedNextUpKeysIn(phonePayload))
    }

    @Test
    fun unreadablePayloadMergesNothing() {
        assertEquals(emptySet(), ContinueWatchingPreferencesRepository.dismissedNextUpKeysIn(""))
        assertEquals(emptySet(), ContinueWatchingPreferencesRepository.dismissedNextUpKeysIn("not json"))
    }
}
