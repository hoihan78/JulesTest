package fastcampus.part2.myapplication.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Extension property for DataStore
val Context.scoresDataStore: DataStore<Preferences> by preferencesDataStore(name = "scores")

/**
 * Score entry for high score tracking
 */
data class ScoreEntry(
    val playerName: String,
    val score: Int,
    val wave: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Get formatted date string
     */
    fun getFormattedDate(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }
}

/**
 * Repository for storing and retrieving high scores using DataStore
 */
class ScoreRepository(private val context: Context) {
    
    companion object {
        private val SCORES_KEY = stringPreferencesKey("high_scores")
        private const val MAX_SCORES = 10
    }
    
    private val dataStore = context.scoresDataStore
    private val gson = Gson()
    
    /**
     * Flow of top scores, sorted by score descending
     */
    val topScores: Flow<List<ScoreEntry>> = dataStore.data.map { preferences ->
        val json = preferences[SCORES_KEY]
        if (json.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                val type = object : TypeToken<List<ScoreEntry>>() {}.type
                gson.fromJson<List<ScoreEntry>>(json, type)
                    .sortedByDescending { it.score }
                    .take(MAX_SCORES)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    /**
     * Save a new score, maintaining only top 10
     */
    suspend fun saveScore(entry: ScoreEntry) {
        dataStore.edit { preferences ->
            val currentJson = preferences[SCORES_KEY]
            val currentScores = if (currentJson.isNullOrEmpty()) {
                mutableListOf()
            } else {
                try {
                    val type = object : TypeToken<List<ScoreEntry>>() {}.type
                    gson.fromJson<List<ScoreEntry>>(currentJson, type).toMutableList()
                } catch (e: Exception) {
                    mutableListOf()
                }
            }
            
            // Add new score and keep only top 10
            currentScores.add(entry)
            val updatedScores = currentScores
                .sortedByDescending { it.score }
                .take(MAX_SCORES)
            
            preferences[SCORES_KEY] = gson.toJson(updatedScores)
        }
    }
    
    /**
     * Check if a score would make it to the leaderboard
     */
    suspend fun isHighScore(score: Int): Boolean {
        var isHigh = false
        dataStore.data.collect { preferences ->
            val json = preferences[SCORES_KEY]
            isHigh = if (json.isNullOrEmpty()) {
                true // First score is always a high score
            } else {
                try {
                    val type = object : TypeToken<List<ScoreEntry>>() {}.type
                    val scores = gson.fromJson<List<ScoreEntry>>(json, type)
                    scores.size < MAX_SCORES || scores.minOfOrNull { it.score }?.let { score > it } ?: true
                } catch (e: Exception) {
                    true
                }
            }
        }
        return isHigh
    }
    
    /**
     * Clear all scores
     */
    suspend fun clearScores() {
        dataStore.edit { preferences ->
            preferences.remove(SCORES_KEY)
        }
    }
}
