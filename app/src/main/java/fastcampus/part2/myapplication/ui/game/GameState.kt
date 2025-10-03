package fastcampus.part2.myapplication.ui.game

import androidx.compose.ui.geometry.Offset

data class Player(val position: Offset)
data class Enemy(val position: Offset)
data class Bullet(val position: Offset)

data class GameState(
    val player: Player,
    val enemies: List<Enemy>,
    val bullets: List<Bullet>,
    val score: Int,
    val lives: Int,
    val isGameOver: Boolean
)