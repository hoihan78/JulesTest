package fastcampus.part2.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView

class RankingActivity : AppCompatActivity() {

    private lateinit var scoreRepository: ScoreRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ranking)

        scoreRepository = ScoreRepository(this)

        val newPlayerName = intent.getStringExtra("PLAYER_NAME")
        val newScore = intent.getIntExtra("SCORE", -1)

        if (newPlayerName != null && newScore != -1) {
            scoreRepository.saveScore(Score(newPlayerName, newScore))
        }

        val rankingListView = findViewById<ListView>(R.id.rankingListView)
        val backToMenuButton = findViewById<Button>(R.id.backToMenuButton)

        val scores = scoreRepository.getScores()
        val scoreStrings = scores.mapIndexed { index, score ->
            "${index + 1}. ${score.playerName}: ${score.score}"
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, scoreStrings)
        rankingListView.adapter = adapter

        backToMenuButton.setOnClickListener {
            finish()
        }
    }
}