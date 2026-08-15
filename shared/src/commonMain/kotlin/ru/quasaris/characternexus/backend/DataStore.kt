package ru.quasaris.characternexus.backend

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferencesSerializer
import ru.quasaris.characternexus.platformFileSystem
import okio.Path.Companion.toPath

fun createDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = platformFileSystem,
            producePath = { producePath().toPath() },
            serializer = PreferencesSerializer
        )
    )
