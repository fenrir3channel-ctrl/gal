package com.minimal.gallery.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.minimal.gallery.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    
    companion object {
        val THEME_KEY = stringPreferencesKey("theme")
        val EXCLUDED_FOLDERS_KEY = stringPreferencesKey("excluded_folders")
    }
    
    val themeFlow: Flow<AppTheme> = context.dataStore.data
        .map { preferences ->
            when (preferences[THEME_KEY]) {
                "LIGHT" -> AppTheme.LIGHT
                "DARK" -> AppTheme.DARK
                else -> AppTheme.SYSTEM
            }
        }
    
    val excludedFoldersFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[EXCLUDED_FOLDERS_KEY]
                ?.split("|")
                ?.filter { it.isNotEmpty() }
                ?.toSet()
                ?: emptySet()
        }
    
    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }
    
    suspend fun toggleFolderExclusion(folderPath: String, isExcluded: Boolean) {
        context.dataStore.edit { preferences ->
            val currentExcluded = preferences[EXCLUDED_FOLDERS_KEY]
                ?.split("|")
                ?.filter { it.isNotEmpty() }
                ?.toMutableSet()
                ?: mutableSetOf()
            
            if (isExcluded) {
                currentExcluded.add(folderPath)
            } else {
                currentExcluded.remove(folderPath)
            }
            
            preferences[EXCLUDED_FOLDERS_KEY] = currentExcluded.joinToString("|")
        }
    }
}
