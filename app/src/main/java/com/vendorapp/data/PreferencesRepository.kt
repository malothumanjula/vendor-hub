package com.vendorapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore by preferencesDataStore(name = "gullygrub_preferences")

class PreferencesRepository(private val context: Context) {
    private companion object {
        val LANGUAGE_KEY = stringPreferencesKey("selected_language")
    }

    val selectedLanguage: Flow<AppLanguage?> = context.appDataStore.data
        .map { preferences -> AppLanguage.fromStorageKey(preferences[LANGUAGE_KEY]) }

    suspend fun saveLanguage(language: AppLanguage) {
        context.appDataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.storageKey
        }
    }
}
