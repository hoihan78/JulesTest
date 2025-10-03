package fastcampus.part2.myapplication

import android.graphics.RectF

data class Player(
    var rect: RectF,
    val speed: Float = 350f
)

data class Enemy(
    var rect: RectF,
    val speed: Float = 150f
)

data class Bullet(
    var rect: RectF,
    val speed: Float = -800f // Negative because it moves up
)

data class Score(
    val playerName: String,
    val score: Int
)