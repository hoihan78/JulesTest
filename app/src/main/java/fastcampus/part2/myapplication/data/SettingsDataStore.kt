package fastcampus.part2.myapplication.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property for DataStore
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * DataStore-based settings storage for game preferences
 */
class SettingsDataStore(private val context: Context) {
    
    companion object {
        private val SOUND_ENABLED_KEY = booleanPreferencesKey("sound_enabled")
        private val MUSIC_ENABLED_KEY = booleanPreferencesKey("music_enabled")
        private val VIBRATION_ENABLED_KEY = booleanPreferencesKey("vibration_enabled")
        private val DIFFICULTY_KEY = stringPreferencesKey("difficulty")
    }
    
    private val dataStore = context.settingsDataStore
    
    /**
     * Flow of sound enabled state
     */
    val isSoundEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[SOUND_ENABLED_KEY] ?: true
    }
    
    /**
     * Flow of music enabled state
     */
    val isMusicEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[MUSIC_ENABLED_KEY] ?: true
    }
    
    /**
     * Flow of vibration enabled state
     */
    val isVibrationEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[VIBRATION_ENABLED_KEY] ?: true
    }
    
    /**
     * Flow of difficulty level
     */
    val difficulty: Flow<String> = dataStore.data.map { preferences ->
        preferences[DIFFICULTY_KEY] ?: Difficulty.NORMAL.name
    }
    
    /**
     * Set sound enabled state
     */
    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SOUND_ENABLED_KEY] = enabled
        }
    }
    
    /**
     * Set music enabled state
     */
    suspend fun setMusicEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[MUSIC_ENABLED_KEY] = enabled
        }
    }
    
    /**
     * Set vibration enabled state
     */
    suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[VIBRATION_ENABLED_KEY] = enabled
        }
    }
    
    /**
     * Set difficulty level
     */
    suspend fun setDifficulty(level: String) {
        dataStore.edit { preferences ->
            preferences[DIFFICULTY_KEY] = level
        }
    }
}

/**
 * Game difficulty levels with enhanced settings
 */
enum class Difficulty(
    val displayName: String, 
    val speedMultiplier: Float, 
    val shootFrequencyMultiplier: Float,
    val startingLives: Int,
    val powerUpDropRate: Float
) {
    EASY("쉬움", 0.7f, 0.5f, 5, 0.15f),
    NORMAL("보통", 1.0f, 1.0f, 3, 0.10f),
    HARD("어려움", 1.3f, 2.0f, 2, 0.05f)
}
