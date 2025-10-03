package fastcampus.part2.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent

class GameActivity : AppCompatActivity(), GameView.OnGameOverListener {

    private lateinit var gameView: GameView
    private lateinit var playerName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerName = intent.getStringExtra("PLAYER_NAME") ?: "Player"
        gameView = GameView(this, null)
        gameView.setPlayerName(playerName)
        setContentView(gameView)
    }

    override fun onPause() {
        super.onPause()
        gameView.surfaceDestroyed(gameView.holder)
    }

    override fun onResume() {
        super.onResume()
        if (gameView.holder.surface.isValid) {
            gameView.surfaceCreated(gameView.holder)
        }
    }

    override fun onGameOver(score: Int) {
        val intent = Intent()
        intent.putExtra("PLAYER_NAME", playerName)
        intent.putExtra("SCORE", score)
        setResult(RESULT_OK, intent)
        finish()
    }
}