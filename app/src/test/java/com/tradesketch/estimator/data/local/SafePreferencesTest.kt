package com.tradesketch.estimator.data.local

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SafePreferencesTest {
    @Test
    fun recoverPreferences_emitsEmptyPreferences_whenIoReadFails() = runTest {
        val recovered = flow<Preferences> {
            throw IOException("disk read failed")
        }
            .recoverPreferences()
            .toList()

        assertEquals(listOf(emptyPreferences()), recovered)
    }
}
