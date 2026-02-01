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
import kotlin.math.sqrt
import kotlin.random.Random

class GameViewModel : ViewModel() {
    private val _gameState = MutableStateFlow(createInitialState())
    val gameState: StateFlow<GameState> = _gameState

    private var gameLoopJob: Job? = null
    private var lastUpdateTime = System.currentTimeMillis()

    // 난이도 조절 상수
    private val baseEnemySpeed = 3f
    private val baseShootInterval = 2000L  // 2초
    private val speedIncreasePerWave = 0.05f  // 웨이브당 5% 속도 증가
    private val shootFrequencyIncreasePerWave = 0.10f  // 웨이브당 10% 발사 빈도 증가

    init {
        startGameLoop()
    }

    private fun startGameLoop() {
        gameLoopJob = viewModelScope.launch {
            while (true) {
                delay(16) // 약 60 FPS
                if (!_gameState.value.isPaused && !_gameState.value.isGameOver) {
                    updateGameState()
                }
            }
        }
    }

    private fun updateGameState() {
        val currentTime = System.currentTimeMillis()
        
        _gameState.update { currentState ->
            if (currentState.isGameOver || currentState.isPaused) return@update currentState

            val wave = currentState.currentWave
            val speedMultiplier = 1f + (wave - 1) * speedIncreasePerWave
            val shootIntervalMultiplier = 1f - (wave - 1) * shootFrequencyIncreasePerWave
            val adjustedShootInterval = (baseShootInterval * shootIntervalMultiplier.coerceAtLeast(0.3f)).toLong()

            // 플레이어 총알 이동
            val newBullets = currentState.bullets
                .map { it.copy(position = it.position.copy(y = it.position.y - 20f)) }
                .filter { it.position.y > 0 }

            // 적군 이동 (속도 조절)
            val enemySpeed = baseEnemySpeed * speedMultiplier
            var newEnemies = currentState.enemies.map { 
                it.copy(position = it.position.copy(y = it.position.y + enemySpeed)) 
            }

            // 적군 총알 발사
            val newEnemyBullets = currentState.enemyBullets.toMutableList()
            newEnemies = newEnemies.map { enemy ->
                if (currentTime - enemy.lastShotTime > adjustedShootInterval + Random.nextLong(500)) {
                    // 플레이어 방향으로 총알 발사
                    val direction = currentState.player.position - enemy.position
                    val distance = sqrt(direction.x * direction.x + direction.y * direction.y)
                    if (distance > 0) {
                        val normalizedVelocity = Offset(
                            direction.x / distance * 8f,
                            direction.y / distance * 8f
                        )
                        newEnemyBullets.add(EnemyBullet(enemy.position, normalizedVelocity))
                    }
                    enemy.copy(lastShotTime = currentTime)
                } else {
                    enemy
                }
            }

            // 적군 총알 이동
            val movedEnemyBullets = newEnemyBullets
                .map { it.copy(position = it.position + it.velocity) }
                .filter { it.position.y < 1600f && it.position.y > 0 && it.position.x > 0 && it.position.x < 1000f }

            // 플레이어 총알 - 적군 충돌 감지
            var score = currentState.score
            val remainingEnemies = newEnemies.toMutableList()
            val remainingBullets = newBullets.toMutableList()
            val enemiesToRemove = mutableListOf<Enemy>()
            val bulletsToRemove = mutableListOf<Bullet>()

            for (bullet in remainingBullets) {
                for (enemy in remainingEnemies) {
                    if (bullet.position.getDistance(enemy.position) < 40f) {
                        bulletsToRemove.add(bullet)
                        val newHealth = enemy.health - 1
                        if (newHealth <= 0) {
                            enemiesToRemove.add(enemy)
                            score += enemy.type.points
                        } else {
                            val index = remainingEnemies.indexOf(enemy)
                            if (index >= 0) {
                                remainingEnemies[index] = enemy.copy(health = newHealth)
                            }
                        }
                        break
                    }
                }
            }
            remainingBullets.removeAll(bulletsToRemove)
            remainingEnemies.removeAll(enemiesToRemove)

            // 적군과 플레이어 충돌 확인
            var lives = currentState.lives
            val enemiesHittingPlayer = remainingEnemies.filter { 
                it.position.getDistance(currentState.player.position) < 40f 
            }
            if (enemiesHittingPlayer.isNotEmpty()) {
                lives -= enemiesHittingPlayer.size
                remainingEnemies.removeAll(enemiesHittingPlayer)
            }

            // 적군 총알과 플레이어 충돌 확인
            val remainingEnemyBullets = movedEnemyBullets.toMutableList()
            val enemyBulletsHittingPlayer = remainingEnemyBullets.filter {
                it.position.getDistance(currentState.player.position) < 30f
            }
            if (enemyBulletsHittingPlayer.isNotEmpty()) {
                lives -= enemyBulletsHittingPlayer.size
                remainingEnemyBullets.removeAll(enemyBulletsHittingPlayer)
            }

            // 화면 밖으로 나간 적 제거
            remainingEnemies.removeAll { it.position.y > 1400f }

            // 웨이브 시스템: 모든 적 제거 시 다음 웨이브
            var newWave = wave
            var newFormation = currentState.formationPattern
            if (remainingEnemies.isEmpty()) {
                newWave = wave + 1
                newFormation = FormationPattern.entries[(newWave - 1) % FormationPattern.entries.size]
                remainingEnemies.addAll(createWaveEnemies(newWave, newFormation))
            }

            val isGameOver = lives <= 0
            if (isGameOver) {
                gameLoopJob?.cancel()
            }

            currentState.copy(
                bullets = remainingBullets,
                enemies = remainingEnemies,
                enemyBullets = remainingEnemyBullets,
                score = score,
                lives = lives,
                isGameOver = isGameOver,
                currentWave = newWave,
                formationPattern = newFormation
            )
        }
        
        lastUpdateTime = currentTime
    }

    // 포메이션에 따른 적 생성
    private fun createWaveEnemies(wave: Int, pattern: FormationPattern): List<Enemy> {
        val enemies = mutableListOf<Enemy>()
        val baseX = 400f
        val baseY = 100f
        val spacing = 80f
        
        // 웨이브 3, 6, 9마다 보스 추가
        val includeBoss = wave % 3 == 0
        
        when (pattern) {
            FormationPattern.V_SHAPE -> {
                // V자 대형
                for (row in 0 until 3) {
                    for (col in 0..row) {
                        val x = baseX - (row * spacing / 2) + (col * spacing)
                        val y = baseY + row * spacing
                        val type = when {
                            row == 0 && includeBoss -> EnemyType.BOSS
                            row == 0 -> EnemyType.BUTTERFLY
                            else -> EnemyType.BEE
                        }
                        enemies.add(Enemy(
                            position = Offset(x, y),
                            type = type,
                            health = type.health
                        ))
                    }
                }
            }
            FormationPattern.INVERTED_V -> {
                // 역V자 대형
                for (row in 0 until 3) {
                    val cols = 3 - row
                    for (col in 0 until cols) {
                        val x = baseX - ((cols - 1) * spacing / 2) + (col * spacing)
                        val y = baseY + row * spacing
                        val type = when {
                            row == 0 && col == cols / 2 && includeBoss -> EnemyType.BOSS
                            row == 0 -> EnemyType.BUTTERFLY
                            else -> EnemyType.BEE
                        }
                        enemies.add(Enemy(
                            position = Offset(x, y),
                            type = type,
                            health = type.health
                        ))
                    }
                }
            }
            FormationPattern.GRID -> {
                // 격자형 대형
                val rows = 3
                val cols = 4 + (wave / 2).coerceAtMost(3)  // 웨이브마다 열 증가
                for (row in 0 until rows) {
                    for (col in 0 until cols) {
                        val x = baseX - ((cols - 1) * spacing / 2) + (col * spacing)
                        val y = baseY + row * spacing
                        val type = when {
                            row == 0 && col == cols / 2 && includeBoss -> EnemyType.BOSS
                            row == 0 -> EnemyType.BUTTERFLY
                            else -> EnemyType.BEE
                        }
                        enemies.add(Enemy(
                            position = Offset(x, y),
                            type = type,
                            health = type.health
                        ))
                    }
                }
            }
        }
        
        return enemies
    }

    fun movePlayer(delta: Float) {
        _gameState.update {
            if (it.isPaused) return@update it
            val newPosition = it.player.position.x + delta
            it.copy(player = it.player.copy(position = it.player.position.copy(x = newPosition.coerceIn(0f, 800f))))
        }
    }

    fun shoot() {
        _gameState.update {
            if (it.isPaused || it.isGameOver) return@update it
            val newBullet = Bullet(position = it.player.position)
            it.copy(bullets = it.bullets + newBullet)
        }
    }

    // 일시정지 토글
    fun togglePause() {
        _gameState.update {
            if (it.isGameOver) return@update it
            it.copy(isPaused = !it.isPaused)
        }
    }

    // 게임 재시작
    fun restartGame() {
        gameLoopJob?.cancel()
        _gameState.value = createInitialState()
        startGameLoop()
    }

    private fun createInitialState(): GameState {
        val initialPattern = FormationPattern.V_SHAPE
        return GameState(
            player = Player(position = Offset(400f, 1200f)),
            enemies = createWaveEnemies(1, initialPattern),
            bullets = emptyList(),
            enemyBullets = emptyList(),
            score = 0,
            lives = 3,
            isGameOver = false,
            currentWave = 1,
            isPaused = false,
            formationPattern = initialPattern
        )
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
    }

    private fun Offset.getDistance(other: Offset): Float {
        val dx = this.x - other.x
        val dy = this.y - other.y
        return sqrt(dx * dx + dy * dy)
    }
}
