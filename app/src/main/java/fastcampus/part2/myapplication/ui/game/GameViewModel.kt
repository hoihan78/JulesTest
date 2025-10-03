package fastcampus.part2.myapplication.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel : ViewModel() {
    private val _gameState = MutableStateFlow(createInitialState())
    val gameState: StateFlow<GameState> = _gameState

    private var gameLoopJob: Job? = null

    init {
        startGameLoop()
    }

    private fun startGameLoop() {
        gameLoopJob = viewModelScope.launch {
            while (true) {
                delay(16) // 약 60 FPS
                updateGameState()
            }
        }
    }

    private fun updateGameState() {
        _gameState.update { currentState ->
            if (currentState.isGameOver) return@update currentState

            // 총알 이동
            val newBullets = currentState.bullets.map { it.copy(position = it.position.copy(y = it.position.y - 20f)) }
                .filter { it.position.y > 0 }

            // 적군 이동
            val newEnemies = currentState.enemies.map { it.copy(position = it.position.copy(y = it.position.y + 5f)) }

            // 충돌 감지
            var score = currentState.score
            val remainingEnemies = newEnemies.toMutableList()
            val remainingBullets = newBullets.toMutableList()
            val enemiesToRemove = mutableListOf<Enemy>()
            val bulletsToRemove = mutableListOf<Bullet>()

            for (bullet in remainingBullets) {
                for (enemy in remainingEnemies) {
                    if (bullet.position.getDistance(enemy.position) < 40f) {
                        bulletsToRemove.add(bullet)
                        enemiesToRemove.add(enemy)
                        score += 10
                    }
                }
            }
            remainingBullets.removeAll(bulletsToRemove)
            remainingEnemies.removeAll(enemiesToRemove)

            // 적군과 플레이어 충돌 확인
            var lives = currentState.lives
            val enemiesHittingPlayer = remainingEnemies.filter { it.position.getDistance(currentState.player.position) < 40f }
            if (enemiesHittingPlayer.isNotEmpty()) {
                lives -= enemiesHittingPlayer.size
                remainingEnemies.removeAll(enemiesHittingPlayer)
            }

            // 모든 적군을 제거하면 새로 생성
            if (remainingEnemies.isEmpty()) {
                remainingEnemies.addAll((1..5).map { Enemy(position = Offset(Random.nextFloat() * 800f, 200f)) })
            }

            val isGameOver = lives <= 0
            if (isGameOver) {
                gameLoopJob?.cancel()
            }

            currentState.copy(
                bullets = remainingBullets,
                enemies = remainingEnemies,
                score = score,
                lives = lives,
                isGameOver = isGameOver
            )
        }
    }

    fun movePlayer(delta: Float) {
        _gameState.update {
            val newPosition = it.player.position.x + delta
            it.copy(player = it.player.copy(position = it.player.position.copy(x = newPosition.coerceIn(0f, 800f))))
        }
    }

    fun shoot() {
        _gameState.update {
            val newBullet = Bullet(position = it.player.position)
            it.copy(bullets = it.bullets + newBullet)
        }
    }

    private fun createInitialState(): GameState {
        return GameState(
            player = Player(position = Offset(400f, 1200f)),
            enemies = (1..5).map { Enemy(position = Offset(Random.nextFloat() * 800f, 200f)) },
            bullets = emptyList(),
            score = 0,
            lives = 3,
            isGameOver = false
        )
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
    }

    private fun Offset.getDistance(other: Offset): Float {
        val dx = this.x - other.x
        val dy = this.y - other.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}