package com.tradesketch.estimator.data.local

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOf

internal fun preferencesCorruptionHandler(): ReplaceFileCorruptionHandler<Preferences> {
    return ReplaceFileCorruptionHandler { emptyPreferences() }
}

internal fun Flow<Preferences>.recoverPreferences(): Flow<Preferences> {
    return catch { exception ->
        if (exception is IOException) {
            emitAll(flowOf(emptyPreferences()))
        } else {
            throw exception
        }
    }
}
