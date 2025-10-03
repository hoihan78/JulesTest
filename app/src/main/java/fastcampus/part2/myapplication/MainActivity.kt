package fastcampus.part2.myapplication

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    companion object {
        private const val GAME_ACTIVITY_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val nameEditText = findViewById<EditText>(R.id.nameEditText)
        val startGameButton = findViewById<Button>(R.id.startGameButton)
        val highScoresButton = findViewById<Button>(R.id.highScoresButton)

        startGameButton.setOnClickListener {
            val name = nameEditText.text.toString()
            if (name.isNotEmpty()) {
                val intent = Intent(this, GameActivity::class.java)
                intent.putExtra("PLAYER_NAME", name)
                startActivityForResult(intent, GAME_ACTIVITY_REQUEST_CODE)
            } else {
                Toast.makeText(this, "Please enter your name.", Toast.LENGTH_SHORT).show()
            }
        }

        highScoresButton.setOnClickListener {
            val intent = Intent(this, RankingActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GAME_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val score = data?.getIntExtra("SCORE", 0) ?: 0
            val name = data?.getStringExtra("PLAYER_NAME") ?: "Player"

            val intent = Intent(this, RankingActivity::class.java)
            intent.putExtra("PLAYER_NAME", name)
            intent.putExtra("SCORE", score)
            startActivity(intent)
        }
    }
}