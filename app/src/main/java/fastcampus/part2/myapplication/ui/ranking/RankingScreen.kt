package fastcampus.part2.myapplication.ui.ranking

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Composable
fun RankingScreen(
    playerName: String,
    score: Int,
    onNavigateToNameInput: () -> Unit
) {
    val context = LocalContext.current
    val rankings = remember { mutableStateOf<List<Ranking>>(emptyList()) }

    LaunchedEffect(Unit) {
        val newRanking = Ranking(playerName, score)
        val savedRankings = loadRankings(context)
        val updatedRankings = (savedRankings + newRanking)
            .sortedByDescending { it.score }
            .take(10)
        saveRankings(context, updatedRankings)
        rankings.value = updatedRankings
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("최고 점수", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Text("당신의 점수: $score", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)

        LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 16.dp)) {
            items(rankings.value) { ranking ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(ranking.playerName)
                    Text(ranking.score.toString())
                }
            }
        }

        Button(onClick = onNavigateToNameInput) {
            Text("다시 플레이")
        }
    }
}

private fun saveRankings(context: Context, rankings: List<Ranking>) {
    val sharedPreferences = context.getSharedPreferences("galaga_rankings", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    val json = Gson().toJson(rankings)
    editor.putString("rankings_list", json)
    editor.apply()
}

private fun loadRankings(context: Context): List<Ranking> {
    val sharedPreferences = context.getSharedPreferences("galaga_rankings", Context.MODE_PRIVATE)
    val json = sharedPreferences.getString("rankings_list", null)
    return if (json != null) {
        val type = object : TypeToken<List<Ranking>>() {}.type
        Gson().fromJson(json, type)
    } else {
        emptyList()
    }
}