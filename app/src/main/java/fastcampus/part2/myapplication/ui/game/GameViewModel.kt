package fastcampus.part2.myapplication.ui.game

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fastcampus.part2.myapplication.data.Difficulty
import fastcampus.part2.myapplication.data.SettingsDataStore
import fastcampus.part2.myapplication.sound.SoundManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val _gameState = MutableStateFlow(createInitialState())
    val gameState: StateFlow<GameState> = _gameState

    // Sound and settings
    private val soundManager = SoundManager(application)
    private val settingsDataStore = SettingsDataStore(application)

    private var gameLoopJob: Job? = null
    private var lastUpdateTime = System.currentTimeMillis()
    private var animationTime = 0f  // For sprite animations

    // 난이도 조절 상수
    private val baseEnemySpeed = 3f
    private val baseShootInterval = 2000L  // 2초
    private val speedIncreasePerWave = 0.05f  // 웨이브당 5% 속도 증가
    private val shootFrequencyIncreasePerWave = 0.10f  // 웨이브당 10% 발사 빈도 증가

    // Difficulty multipliers (updated from settings)
    private var difficultySpeedMultiplier = 1.0f
    private var difficultyShootMultiplier = 1.0f

    // Screen dimensions (will be updated)
    private var screenWidth = 800f
    private var screenHeight = 1600f
    
    // Player invincibility duration after hit
    private val invincibilityDuration = 2000L  // 2 seconds
    
    // Particle colors
    private val explosionColors = listOf(
        Color(0xFFFF6B35),  // Orange
        Color(0xFFFFEE00),  // Yellow
        Color(0xFFFF0055),  // Red
        Color(0xFFFFFFFF)   // White
    )
    
    private val muzzleColors = listOf(
        Color(0xFFFFEE00),  // Yellow
        Color(0xFFFFFFFF),  // White
        Color(0xFF00D9FF)   // Cyan
    )

    init {
        // Load settings
        viewModelScope.launch {
            loadSettings()
        }
        startGameLoop()
    }

    private suspend fun loadSettings() {
        // Load sound settings
        val soundEnabled = settingsDataStore.isSoundEnabled.first()
        val musicEnabled = settingsDataStore.isMusicEnabled.first()
        val vibrationEnabled = settingsDataStore.isVibrationEnabled.first()
        soundManager.updateSettings(soundEnabled, musicEnabled, vibrationEnabled)

        // Load difficulty
        val difficultyName = settingsDataStore.difficulty.first()
        val difficulty = try {
            Difficulty.valueOf(difficultyName)
        } catch (e: Exception) {
            Difficulty.NORMAL
        }
        difficultySpeedMultiplier = difficulty.speedMultiplier
        difficultyShootMultiplier = difficulty.shootFrequencyMultiplier

        // Start BGM
        soundManager.playBGM()
        
        // Observe settings changes
        viewModelScope.launch {
            settingsDataStore.isSoundEnabled.collect { enabled ->
                soundManager.isSoundEnabled = enabled
            }
        }
        viewModelScope.launch {
            settingsDataStore.isMusicEnabled.collect { enabled ->
                if (enabled) {
                    soundManager.resumeBGM()
                } else {
                    soundManager.pauseBGM()
                }
                soundManager.isMusicEnabled = enabled
            }
        }
        viewModelScope.launch {
            settingsDataStore.isVibrationEnabled.collect { enabled ->
                soundManager.isVibrationEnabled = enabled
            }
        }
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
        animationTime += 16f  // Increment animation time
        
        _gameState.update { currentState ->
            if (currentState.isGameOver || currentState.isPaused) return@update currentState

            val wave = currentState.currentWave
            val waveSpeedMultiplier = 1f + (wave - 1) * speedIncreasePerWave
            val speedMultiplier = waveSpeedMultiplier * difficultySpeedMultiplier
            
            val waveShootMultiplier = 1f - (wave - 1) * shootFrequencyIncreasePerWave
            val shootIntervalMultiplier = (waveShootMultiplier / difficultyShootMultiplier).coerceAtLeast(0.3f)
            val adjustedShootInterval = (baseShootInterval * shootIntervalMultiplier).toLong()

            // ===== Update Stars (Background Scroll) =====
            val updatedStars = currentState.stars.map { star ->
                val newY = star.position.y + star.speed
                if (newY > screenHeight) {
                    // Respawn at top with random X
                    star.copy(
                        position = Offset(
                            Random.nextFloat() * screenWidth,
                            -10f
                        )
                    )
                } else {
                    star.copy(position = star.position.copy(y = newY))
                }
            }

            // ===== Update Explosions (remove expired) =====
            val activeExplosions = currentState.explosions.filter { explosion ->
                currentTime - explosion.startTime < explosion.duration
            }

            // ===== Update Particles =====
            val activeParticles = currentState.particles
                .filter { particle ->
                    currentTime - particle.createdAt < particle.lifetime
                }
                .map { particle ->
                    particle.copy(
                        position = particle.position + particle.velocity
                    )
                }

            // ===== Update Player Invincibility =====
            val updatedPlayer = if (currentState.player.isInvincible && 
                                    currentTime > currentState.player.invincibleUntil) {
                currentState.player.copy(isInvincible = false)
            } else {
                currentState.player
            }

            // 플레이어 총알 이동
            val newBullets = currentState.bullets
                .map { it.copy(position = it.position.copy(y = it.position.y - 20f)) }
                .filter { it.position.y > 0 }

            // 적군 이동 (속도 조절) + 애니메이션 업데이트
            val enemySpeed = baseEnemySpeed * speedMultiplier
            var newEnemies = currentState.enemies.map { 
                it.copy(
                    position = it.position.copy(y = it.position.y + enemySpeed),
                    animationPhase = it.animationPhase + 1f
                )
            }

            // 적군 총알 발사
            val newEnemyBullets = currentState.enemyBullets.toMutableList()
            newEnemies = newEnemies.map { enemy ->
                if (currentTime - enemy.lastShotTime > adjustedShootInterval + Random.nextLong(500)) {
                    // 플레이어 방향으로 총알 발사
                    val direction = updatedPlayer.position - enemy.position
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
                .filter { it.position.y < screenHeight && it.position.y > 0 && it.position.x > 0 && it.position.x < screenWidth }

            // 플레이어 총알 - 적군 충돌 감지
            var score = currentState.score
            val remainingEnemies = newEnemies.toMutableList()
            val remainingBullets = newBullets.toMutableList()
            val enemiesToRemove = mutableListOf<Enemy>()
            val bulletsToRemove = mutableListOf<Bullet>()
            val newExplosions = activeExplosions.toMutableList()
            val newParticles = activeParticles.toMutableList()

            for (bullet in remainingBullets) {
                for (enemy in remainingEnemies) {
                    if (bullet.position.getDistance(enemy.position) < 40f) {
                        bulletsToRemove.add(bullet)
                        val newHealth = enemy.health - 1
                        if (newHealth <= 0) {
                            enemiesToRemove.add(enemy)
                            score += enemy.type.points
                            
                            // Play explosion sound
                            soundManager.playSound(SoundManager.SFX_EXPLOSION)
                            soundManager.vibrate(SoundManager.VibrationType.EXPLOSION)
                            
                            // Create explosion at enemy position
                            newExplosions.add(
                                Explosion(
                                    position = enemy.position,
                                    startTime = currentTime,
                                    duration = when(enemy.type) {
                                        EnemyType.BOSS -> 500
                                        EnemyType.BUTTERFLY -> 350
                                        EnemyType.BEE -> 300
                                    },
                                    color = when(enemy.type) {
                                        EnemyType.BOSS -> Color(0xFFFF0055)
                                        EnemyType.BUTTERFLY -> Color(0xFF00FF88)
                                        EnemyType.BEE -> Color(0xFFFFEE00)
                                    }
                                )
                            )
                            
                            // Create particles for destruction
                            newParticles.addAll(
                                createDestructionParticles(enemy.position, enemy.type, currentTime)
                            )
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

            // 적군과 플레이어 충돌 확인 (only if not invincible)
            var lives = currentState.lives
            var playerAfterCollision = updatedPlayer
            
            if (!updatedPlayer.isInvincible) {
                val enemiesHittingPlayer = remainingEnemies.filter { 
                    it.position.getDistance(updatedPlayer.position) < 40f 
                }
                if (enemiesHittingPlayer.isNotEmpty()) {
                    lives -= enemiesHittingPlayer.size
                    remainingEnemies.removeAll(enemiesHittingPlayer)
                    playerAfterCollision = updatedPlayer.copy(
                        isInvincible = true,
                        invincibleUntil = currentTime + invincibilityDuration
                    )
                    
                    // Play hit sound
                    soundManager.playSound(SoundManager.SFX_HIT)
                    soundManager.vibrate(SoundManager.VibrationType.HIT)
                    
                    // Add explosion at player position
                    newExplosions.add(
                        Explosion(
                            position = updatedPlayer.position,
                            startTime = currentTime,
                            duration = 400,
                            color = Color(0xFF00D9FF)
                        )
                    )
                }
            }

            // 적군 총알과 플레이어 충돌 확인 (only if not invincible)
            val remainingEnemyBullets = movedEnemyBullets.toMutableList()
            if (!playerAfterCollision.isInvincible) {
                val enemyBulletsHittingPlayer = remainingEnemyBullets.filter {
                    it.position.getDistance(playerAfterCollision.position) < 30f
                }
                if (enemyBulletsHittingPlayer.isNotEmpty()) {
                    lives -= enemyBulletsHittingPlayer.size
                    remainingEnemyBullets.removeAll(enemyBulletsHittingPlayer)
                    playerAfterCollision = playerAfterCollision.copy(
                        isInvincible = true,
                        invincibleUntil = currentTime + invincibilityDuration
                    )
                    
                    // Play hit sound
                    soundManager.playSound(SoundManager.SFX_HIT)
                    soundManager.vibrate(SoundManager.VibrationType.HIT)
                    
                    // Add hit effect
                    newExplosions.add(
                        Explosion(
                            position = playerAfterCollision.position,
                            startTime = currentTime,
                            duration = 300,
                            color = Color(0xFFFF0055)
                        )
                    )
                }
            }

            // 화면 밖으로 나간 적 제거
            remainingEnemies.removeAll { it.position.y > screenHeight - 200f }

            // 웨이브 시스템: 모든 적 제거 시 다음 웨이브
            var newWave = wave
            var newFormation = currentState.formationPattern
            if (remainingEnemies.isEmpty()) {
                newWave = wave + 1
                newFormation = FormationPattern.entries[(newWave - 1) % FormationPattern.entries.size]
                remainingEnemies.addAll(createWaveEnemies(newWave, newFormation))
                
                // Play level up sound
                soundManager.playSound(SoundManager.SFX_LEVELUP)
            }

            val isGameOver = lives <= 0
            if (isGameOver) {
                gameLoopJob?.cancel()
                soundManager.playSound(SoundManager.SFX_GAMEOVER)
                soundManager.vibrate(SoundManager.VibrationType.GAMEOVER)
                soundManager.stopBGM()
            }

            currentState.copy(
                player = playerAfterCollision,
                bullets = remainingBullets,
                enemies = remainingEnemies,
                enemyBullets = remainingEnemyBullets,
                score = score,
                lives = lives,
                isGameOver = isGameOver,
                currentWave = newWave,
                formationPattern = newFormation,
                stars = updatedStars,
                explosions = newExplosions,
                particles = newParticles
            )
        }
        
        lastUpdateTime = currentTime
    }

    // Create particles when enemy is destroyed
    private fun createDestructionParticles(
        position: Offset,
        enemyType: EnemyType,
        currentTime: Long
    ): List<Particle> {
        val particles = mutableListOf<Particle>()
        val particleCount = when(enemyType) {
            EnemyType.BOSS -> 20
            EnemyType.BUTTERFLY -> 12
            EnemyType.BEE -> 8
        }
        
        val colors = when(enemyType) {
            EnemyType.BOSS -> listOf(Color(0xFFFF0055), Color(0xFFFFEE00), Color(0xFFFFFFFF))
            EnemyType.BUTTERFLY -> listOf(Color(0xFF00FF88), Color(0xFF00D9FF), Color(0xFFFFFFFF))
            EnemyType.BEE -> listOf(Color(0xFFFFEE00), Color(0xFFFF0055), Color(0xFFFFFFFF))
        }
        
        for (i in 0 until particleCount) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = 2f + Random.nextFloat() * 4f
            val velocity = Offset(
                cos(angle) * speed,
                sin(angle) * speed
            )
            
            particles.add(
                Particle(
                    position = position,
                    velocity = velocity,
                    color = colors.random(),
                    size = 2f + Random.nextFloat() * 4f,
                    lifetime = 200 + Random.nextInt(200),
                    createdAt = currentTime
                )
            )
        }
        
        return particles
    }

    // Create muzzle flash particles when shooting
    private fun createMuzzleFlashParticles(
        position: Offset,
        currentTime: Long
    ): List<Particle> {
        val particles = mutableListOf<Particle>()
        
        for (i in 0 until 5) {
            val angle = -Math.PI.toFloat() / 2 + (Random.nextFloat() - 0.5f) * 0.5f
            val speed = 3f + Random.nextFloat() * 3f
            val velocity = Offset(
                cos(angle) * speed,
                sin(angle) * speed
            )
            
            particles.add(
                Particle(
                    position = position.copy(y = position.y - 25f),
                    velocity = velocity,
                    color = muzzleColors.random(),
                    size = 2f + Random.nextFloat() * 3f,
                    lifetime = 80 + Random.nextInt(80),
                    createdAt = currentTime
                )
            )
        }
        
        return particles
    }

    // Initialize background stars
    private fun createStars(): List<Star> {
        val stars = mutableListOf<Star>()
        
        for (i in 0 until 150) {
            // Create stars in 3 layers for depth
            val layer = i % 3
            val speed = when(layer) {
                0 -> 0.5f + Random.nextFloat() * 0.5f  // Far (slow)
                1 -> 1.0f + Random.nextFloat() * 0.5f  // Middle
                else -> 1.5f + Random.nextFloat() * 1.0f  // Near (fast)
            }
            val brightness = when(layer) {
                0 -> 0.3f + Random.nextFloat() * 0.2f  // Dim
                1 -> 0.5f + Random.nextFloat() * 0.3f  // Medium
                else -> 0.7f + Random.nextFloat() * 0.3f  // Bright
            }
            val size = when(layer) {
                0 -> 1f  // Small
                1 -> 1.5f  // Medium
                else -> 2f + Random.nextFloat()  // Large
            }
            
            stars.add(
                Star(
                    position = Offset(
                        Random.nextFloat() * screenWidth,
                        Random.nextFloat() * screenHeight
                    ),
                    speed = speed,
                    size = size,
                    brightness = brightness
                )
            )
        }
        
        return stars
    }

    // 포메이션에 따른 적 생성
    private fun createWaveEnemies(wave: Int, pattern: FormationPattern): List<Enemy> {
        val enemies = mutableListOf<Enemy>()
        val baseX = screenWidth / 2
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
                            health = type.health,
                            animationPhase = Random.nextFloat() * 100f
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
                            health = type.health,
                            animationPhase = Random.nextFloat() * 100f
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
                            health = type.health,
                            animationPhase = Random.nextFloat() * 100f
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
            it.copy(
                player = it.player.copy(
                    position = it.player.position.copy(x = newPosition.coerceIn(30f, screenWidth - 30f))
                )
            )
        }
    }

    fun shoot() {
        val currentTime = System.currentTimeMillis()
        _gameState.update {
            if (it.isPaused || it.isGameOver) return@update it
            val newBullet = Bullet(position = it.player.position)
            
            // Play shoot sound and vibrate
            soundManager.playSound(SoundManager.SFX_SHOOT)
            soundManager.vibrate(SoundManager.VibrationType.SHOOT)
            
            // Add muzzle flash particles
            val muzzleParticles = createMuzzleFlashParticles(it.player.position, currentTime)
            
            it.copy(
                bullets = it.bullets + newBullet,
                particles = it.particles + muzzleParticles
            )
        }
    }

    // Update screen dimensions
    fun updateScreenSize(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
    }

    // 일시정지 토글
    fun togglePause() {
        _gameState.update {
            if (it.isGameOver) return@update it
            val newPaused = !it.isPaused
            
            // Pause/resume BGM
            if (newPaused) {
                soundManager.pauseBGM()
            } else {
                soundManager.resumeBGM()
            }
            
            it.copy(isPaused = newPaused)
        }
    }

    // 게임 재시작
    fun restartGame() {
        gameLoopJob?.cancel()
        _gameState.value = createInitialState()
        soundManager.playBGM()
        startGameLoop()
    }

    private fun createInitialState(): GameState {
        val initialPattern = FormationPattern.V_SHAPE
        return GameState(
            player = Player(position = Offset(screenWidth / 2, screenHeight - 200f)),
            enemies = createWaveEnemies(1, initialPattern),
            bullets = emptyList(),
            enemyBullets = emptyList(),
            score = 0,
            lives = 3,
            isGameOver = false,
            currentWave = 1,
            isPaused = false,
            formationPattern = initialPattern,
            stars = createStars(),
            explosions = emptyList(),
            particles = emptyList()
        )
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
        soundManager.release()
    }

    private fun Offset.getDistance(other: Offset): Float {
        val dx = this.x - other.x
        val dy = this.y - other.y
        return sqrt(dx * dx + dy * dy)
    }
}
