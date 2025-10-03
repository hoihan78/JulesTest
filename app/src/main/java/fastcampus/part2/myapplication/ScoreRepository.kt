package fastcampus.part2.myapplication

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ScoreRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "galaga_scores"
        private const val SCORES_KEY = "scores"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveScore(score: Score) {
        val scores = getScores().toMutableList()
        scores.add(score)
        scores.sortByDescending { it.score }
        val scoresJson = gson.toJson(scores)
        prefs.edit().putString(SCORES_KEY, scoresJson).apply()
    }

    fun getScores(): List<Score> {
        val scoresJson = prefs.getString(SCORES_KEY, null)
        return if (scoresJson != null) {
            val type = object : TypeToken<List<Score>>() {}.type
            gson.fromJson(scoresJson, type)
        } else {
            emptyList()
        }
    }
}