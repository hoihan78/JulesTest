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
import kotlin.math.PI
import kotlin.math.atan2
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
    private var autoFireJob: Job? = null
    private var lastUpdateTime = System.currentTimeMillis()
    private var animationTime = 0f  // For sprite animations

    // 난이도 조절 상수
    private val baseEnemySpeed = 3f
    private val baseShootInterval = 2000L  // 2초
    private val speedIncreasePerWave = 0.05f  // 웨이브당 5% 속도 증가
    private val shootFrequencyIncreasePerWave = 0.10f  // 웨이브당 10% 발사 빈도 증가

    // Difficulty settings (updated from preferences)
    private var currentDifficulty = Difficulty.NORMAL
    private var difficultySpeedMultiplier = 1.0f
    private var difficultyShootMultiplier = 1.0f
    private var powerUpDropRate = 0.10f

    // Screen dimensions (will be updated)
    private var screenWidth = 800f
    private var screenHeight = 1600f
    
    // Player invincibility duration after hit
    private val invincibilityDuration = 2000L  // 2 seconds
    
    // Power-up durations
    private val powerUpDuration = mapOf(
        PowerUpType.DOUBLE_SHOT to 10000L,
        PowerUpType.TRIPLE_SHOT to 10000L,
        PowerUpType.SHIELD to 15000L,
        PowerUpType.SPEED_UP to 10000L,
        PowerUpType.EXTRA_LIFE to 0L  // Instant effect
    )
    
    // Combo settings
    private val comboTimeWindow = 2000L  // 2초 내 연속 킬
    private val comboDisplayDuration = 1500L  // 콤보 표시 시간
    
    // Auto-fire settings
    private val autoFireInterval = 200L  // 0.2초 간격
    
    // Boss settings
    private val bossWaveInterval = 5  // 웨이브 5마다 보스
    private val bossWarningDuration = 2000L  // 보스 경고 2초
    
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
    
    // Power-up colors
    private val powerUpColors = mapOf(
        PowerUpType.DOUBLE_SHOT to Color(0xFF00D9FF),   // Cyan
        PowerUpType.TRIPLE_SHOT to Color(0xFF9D4EDD),   // Purple
        PowerUpType.SHIELD to Color(0xFF00FF88),        // Green
        PowerUpType.SPEED_UP to Color(0xFFFFEE00),      // Yellow
        PowerUpType.EXTRA_LIFE to Color(0xFFFF0055)     // Red
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
        currentDifficulty = try {
            Difficulty.valueOf(difficultyName)
        } catch (e: Exception) {
            Difficulty.NORMAL
        }
        difficultySpeedMultiplier = currentDifficulty.speedMultiplier
        difficultyShootMultiplier = currentDifficulty.shootFrequencyMultiplier
        powerUpDropRate = currentDifficulty.powerUpDropRate
        
        // Apply starting lives based on difficulty
        _gameState.update { state ->
            state.copy(lives = currentDifficulty.startingLives)
        }

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

            // ===== Update Screen Shake =====
            val screenShake = if (currentState.screenShake.intensity > 0 && 
                currentTime - currentState.screenShake.startTime < currentState.screenShake.duration) {
                val progress = (currentTime - currentState.screenShake.startTime).toFloat() / currentState.screenShake.duration
                currentState.screenShake.copy(intensity = currentState.screenShake.intensity * (1f - progress))
            } else {
                ScreenShakeInfo()
            }

            // ===== Update Stars (Background Scroll) =====
            val updatedStars = currentState.stars.map { star ->
                val newY = star.position.y + star.speed
                if (newY > screenHeight) {
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
                
            // ===== Update Floating Texts =====
            val activeFloatingTexts = currentState.floatingTexts
                .filter { text ->
                    currentTime - text.createdAt < text.duration
                }
                .map { text ->
                    val progress = (currentTime - text.createdAt).toFloat() / text.duration
                    text.copy(position = text.position.copy(y = text.position.y - 1f))
                }

            // ===== Update Power-ups =====
            // Move power-ups down and remove if off screen
            val movedPowerUps = currentState.powerUps
                .map { powerUp ->
                    powerUp.copy(position = powerUp.position + powerUp.velocity)
                }
                .filter { it.position.y < screenHeight }
            
            // Check power-up expiration
            val activePowerUps = currentState.activePowerUps.filter { (_, expiry) ->
                currentTime < expiry
            }
            
            // Update shot level based on active power-ups
            val shotLevel = when {
                activePowerUps.containsKey(PowerUpType.TRIPLE_SHOT) -> 3
                activePowerUps.containsKey(PowerUpType.DOUBLE_SHOT) -> 2
                else -> 1
            }
            
            val hasShield = activePowerUps.containsKey(PowerUpType.SHIELD)
            val playerSpeed = if (activePowerUps.containsKey(PowerUpType.SPEED_UP)) 1.5f else 1.0f

            // ===== Update Player Invincibility =====
            val updatedPlayer = if (currentState.player.isInvincible && 
                                    currentTime > currentState.player.invincibleUntil) {
                currentState.player.copy(isInvincible = false)
            } else {
                currentState.player
            }

            // ===== Update Combo =====
            var combo = currentState.combo
            if (combo.count > 0 && currentTime - combo.lastKillTime > comboTimeWindow) {
                combo = ComboInfo()  // Reset combo
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

            // ===== Boss Battle Logic =====
            var stageBoss = currentState.stageBoss
            var bossBullets = currentState.bossBullets.toMutableList()
            var bossWarning = currentState.bossWarning
            var bossWarningStartTime = currentState.bossWarningStartTime
            var isBossWave = currentState.isBossWave
            
            // Update boss bullets
            bossBullets = bossBullets
                .map { it.copy(position = it.position + it.velocity) }
                .filter { it.position.y < screenHeight && it.position.y > 0 && 
                         it.position.x > 0 && it.position.x < screenWidth }
                .toMutableList()
            
            if (stageBoss != null) {
                stageBoss = updateBoss(stageBoss, updatedPlayer.position, currentTime, bossBullets)
            }

            // 플레이어 총알 - 적군 충돌 감지
            var score = currentState.score
            val remainingEnemies = newEnemies.toMutableList()
            val remainingBullets = newBullets.toMutableList()
            val enemiesToRemove = mutableListOf<Enemy>()
            val bulletsToRemove = mutableListOf<Bullet>()
            val newExplosions = activeExplosions.toMutableList()
            val newParticles = activeParticles.toMutableList()
            val newFloatingTexts = activeFloatingTexts.toMutableList()
            val newPowerUps = movedPowerUps.toMutableList()
            var newActivePowerUps = activePowerUps.toMutableMap()

            // Player bullets vs enemies
            for (bullet in remainingBullets) {
                for (enemy in remainingEnemies) {
                    if (bullet.position.getDistance(enemy.position) < 40f) {
                        bulletsToRemove.add(bullet)
                        val newHealth = enemy.health - 1
                        if (newHealth <= 0) {
                            enemiesToRemove.add(enemy)
                            
                            // Update combo
                            val newComboCount = if (currentTime - combo.lastKillTime < comboTimeWindow) {
                                combo.count + 1
                            } else {
                                1
                            }
                            val comboMultiplier = 1f + (newComboCount - 1) * 0.5f
                            combo = ComboInfo(
                                count = newComboCount,
                                lastKillTime = currentTime,
                                displayUntil = currentTime + comboDisplayDuration,
                                multiplier = comboMultiplier
                            )
                            
                            val basePoints = enemy.type.points
                            val earnedPoints = (basePoints * comboMultiplier).toInt()
                            score += earnedPoints
                            
                            // Show combo text
                            if (newComboCount >= 2) {
                                newFloatingTexts.add(
                                    FloatingText(
                                        text = "${newComboCount}x COMBO! +$earnedPoints",
                                        position = enemy.position.copy(y = enemy.position.y - 20f),
                                        color = Color(0xFFFFEE00),
                                        createdAt = currentTime
                                    )
                                )
                            }
                            
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
                            
                            // Power-up drop chance
                            if (Random.nextFloat() < powerUpDropRate) {
                                val powerUpType = PowerUpType.entries.random()
                                newPowerUps.add(
                                    PowerUp(
                                        position = enemy.position,
                                        type = powerUpType,
                                        createdAt = currentTime
                                    )
                                )
                            }
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
            
            // Player bullets vs Stage Boss
            if (stageBoss != null) {
                for (bullet in remainingBullets) {
                    if (bullet.position.getDistance(stageBoss.position) < 60f && !bulletsToRemove.contains(bullet)) {
                        bulletsToRemove.add(bullet)
                        val newHealth = stageBoss.health - 1
                        
                        if (newHealth <= 0) {
                            // Boss defeated!
                            score += 500 * wave  // Big bonus for boss
                            
                            soundManager.playSound(SoundManager.SFX_EXPLOSION)
                            soundManager.vibrate(SoundManager.VibrationType.GAMEOVER)
                            
                            // Big explosion
                            newExplosions.add(
                                Explosion(
                                    position = stageBoss.position,
                                    startTime = currentTime,
                                    duration = 800,
                                    color = Color(0xFFFF0055)
                                )
                            )
                            
                            // Lots of particles
                            for (i in 0 until 30) {
                                val angle = Random.nextFloat() * 2f * PI.toFloat()
                                val speed = 2f + Random.nextFloat() * 6f
                                newParticles.add(
                                    Particle(
                                        position = stageBoss.position,
                                        velocity = Offset(cos(angle) * speed, sin(angle) * speed),
                                        color = listOf(Color(0xFFFF0055), Color(0xFFFFEE00), Color(0xFFFFFFFF)).random(),
                                        size = 3f + Random.nextFloat() * 5f,
                                        lifetime = 400 + Random.nextInt(400),
                                        createdAt = currentTime
                                    )
                                )
                            }
                            
                            // Screen shake
                            val newScreenShake = ScreenShakeInfo(
                                intensity = 25f,
                                startTime = currentTime,
                                duration = 500
                            )
                            
                            // Drop guaranteed power-up from boss
                            newPowerUps.add(
                                PowerUp(
                                    position = stageBoss.position,
                                    type = PowerUpType.entries.random(),
                                    createdAt = currentTime
                                )
                            )
                            
                            // Show boss defeated text
                            newFloatingTexts.add(
                                FloatingText(
                                    text = "BOSS DEFEATED! +${500 * wave}",
                                    position = stageBoss.position,
                                    color = Color(0xFF00FF88),
                                    createdAt = currentTime,
                                    duration = 2000,
                                    fontSize = 32
                                )
                            )
                            
                            stageBoss = null
                            isBossWave = false
                            
                            return@update currentState.copy(
                                player = updatedPlayer,
                                bullets = remainingBullets - bulletsToRemove.toSet(),
                                enemies = remainingEnemies - enemiesToRemove.toSet(),
                                enemyBullets = movedEnemyBullets,
                                score = score,
                                currentWave = wave + 1,
                                formationPattern = FormationPattern.entries[(wave) % FormationPattern.entries.size],
                                stars = updatedStars,
                                explosions = newExplosions,
                                particles = newParticles,
                                floatingTexts = newFloatingTexts,
                                powerUps = newPowerUps,
                                activePowerUps = newActivePowerUps,
                                shotLevel = shotLevel,
                                hasShield = hasShield,
                                playerSpeed = playerSpeed,
                                stageBoss = null,
                                bossBullets = emptyList(),
                                isBossWave = false,
                                bossWarning = false,
                                combo = combo,
                                screenShake = newScreenShake
                            )
                        } else {
                            stageBoss = stageBoss.copy(
                                health = newHealth,
                                isHit = true,
                                hitTime = currentTime,
                                phase = when {
                                    newHealth > stageBoss.maxHealth * 0.66f -> 1
                                    newHealth > stageBoss.maxHealth * 0.33f -> 2
                                    else -> 3
                                }
                            )
                        }
                    }
                }
                
                // Reset hit flash
                if (stageBoss != null && stageBoss.isHit && currentTime - stageBoss.hitTime > 100) {
                    stageBoss = stageBoss.copy(isHit = false)
                }
            }
            
            remainingBullets.removeAll(bulletsToRemove)
            remainingEnemies.removeAll(enemiesToRemove)

            // ===== Power-up Collection =====
            val collectedPowerUps = mutableListOf<PowerUp>()
            for (powerUp in newPowerUps) {
                if (powerUp.position.getDistance(updatedPlayer.position) < 50f) {
                    collectedPowerUps.add(powerUp)
                    
                    when (powerUp.type) {
                        PowerUpType.EXTRA_LIFE -> {
                            // Instant effect - handled below
                        }
                        else -> {
                            val duration = powerUpDuration[powerUp.type] ?: 10000L
                            newActivePowerUps[powerUp.type] = currentTime + duration
                        }
                    }
                    
                    // Show power-up text
                    val powerUpName = when (powerUp.type) {
                        PowerUpType.DOUBLE_SHOT -> "DOUBLE SHOT!"
                        PowerUpType.TRIPLE_SHOT -> "TRIPLE SHOT!"
                        PowerUpType.SHIELD -> "SHIELD!"
                        PowerUpType.SPEED_UP -> "SPEED UP!"
                        PowerUpType.EXTRA_LIFE -> "+1 LIFE!"
                    }
                    newFloatingTexts.add(
                        FloatingText(
                            text = powerUpName,
                            position = powerUp.position,
                            color = powerUpColors[powerUp.type] ?: Color.White,
                            createdAt = currentTime,
                            fontSize = 28
                        )
                    )
                    
                    soundManager.playSound(SoundManager.SFX_LEVELUP)
                }
            }
            newPowerUps.removeAll(collectedPowerUps)
            
            // Handle extra life power-up
            var lives = currentState.lives
            if (collectedPowerUps.any { it.type == PowerUpType.EXTRA_LIFE }) {
                lives += 1
            }

            // 적군과 플레이어 충돌 확인 (only if not invincible and no shield)
            var playerAfterCollision = updatedPlayer
            var perfectWave = currentState.perfectWave
            
            if (!updatedPlayer.isInvincible && !hasShield) {
                val enemiesHittingPlayer = remainingEnemies.filter { 
                    it.position.getDistance(updatedPlayer.position) < 40f 
                }
                if (enemiesHittingPlayer.isNotEmpty()) {
                    lives -= enemiesHittingPlayer.size
                    perfectWave = false
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
                    
                    // Screen shake on hit
                    return@update currentState.copy(
                        screenShake = ScreenShakeInfo(
                            intensity = 15f,
                            startTime = currentTime,
                            duration = 300
                        )
                    ).let { state ->
                        state.copy(
                            player = playerAfterCollision,
                            bullets = remainingBullets,
                            enemies = remainingEnemies,
                            enemyBullets = movedEnemyBullets,
                            score = score,
                            lives = lives,
                            stars = updatedStars,
                            explosions = newExplosions,
                            particles = newParticles,
                            floatingTexts = newFloatingTexts,
                            powerUps = newPowerUps,
                            activePowerUps = newActivePowerUps,
                            shotLevel = shotLevel,
                            hasShield = hasShield,
                            playerSpeed = playerSpeed,
                            stageBoss = stageBoss,
                            bossBullets = bossBullets,
                            combo = combo,
                            perfectWave = perfectWave
                        )
                    }
                }
            }

            // 적군 총알과 플레이어 충돌 확인 (only if not invincible and no shield)
            val remainingEnemyBullets = movedEnemyBullets.toMutableList()
            if (!playerAfterCollision.isInvincible && !hasShield) {
                val enemyBulletsHittingPlayer = remainingEnemyBullets.filter {
                    it.position.getDistance(playerAfterCollision.position) < 30f
                }
                if (enemyBulletsHittingPlayer.isNotEmpty()) {
                    lives -= enemyBulletsHittingPlayer.size
                    perfectWave = false
                    remainingEnemyBullets.removeAll(enemyBulletsHittingPlayer)
                    playerAfterCollision = playerAfterCollision.copy(
                        isInvincible = true,
                        invincibleUntil = currentTime + invincibilityDuration
                    )
                    
                    soundManager.playSound(SoundManager.SFX_HIT)
                    soundManager.vibrate(SoundManager.VibrationType.HIT)
                    
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
            
            // Boss bullets vs player
            if (!playerAfterCollision.isInvincible && !hasShield) {
                val bossBulletsHittingPlayer = bossBullets.filter {
                    it.position.getDistance(playerAfterCollision.position) < 30f
                }
                if (bossBulletsHittingPlayer.isNotEmpty()) {
                    lives -= bossBulletsHittingPlayer.size
                    perfectWave = false
                    bossBullets.removeAll(bossBulletsHittingPlayer)
                    playerAfterCollision = playerAfterCollision.copy(
                        isInvincible = true,
                        invincibleUntil = currentTime + invincibilityDuration
                    )
                    
                    soundManager.playSound(SoundManager.SFX_HIT)
                    soundManager.vibrate(SoundManager.VibrationType.HIT)
                    
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
            
            // Check if it's time for a boss wave
            if (remainingEnemies.isEmpty() && stageBoss == null && !bossWarning) {
                // Perfect wave bonus
                if (perfectWave) {
                    val bonus = 100 * wave
                    score += bonus
                    newFloatingTexts.add(
                        FloatingText(
                            text = "PERFECT! +$bonus",
                            position = Offset(screenWidth / 2, screenHeight / 2),
                            color = Color(0xFF00FF88),
                            createdAt = currentTime,
                            duration = 2000,
                            fontSize = 36
                        )
                    )
                }
                
                newWave = wave + 1
                perfectWave = true  // Reset for new wave
                newFormation = FormationPattern.entries[(newWave - 1) % FormationPattern.entries.size]
                
                // Check if this is a boss wave
                if (newWave % bossWaveInterval == 0) {
                    bossWarning = true
                    bossWarningStartTime = currentTime
                    isBossWave = true
                    
                    // Screen shake for boss warning
                    return@update currentState.copy(
                        player = playerAfterCollision,
                        bullets = remainingBullets,
                        enemies = emptyList(),
                        enemyBullets = remainingEnemyBullets,
                        score = score,
                        lives = lives,
                        currentWave = newWave,
                        formationPattern = newFormation,
                        stars = updatedStars,
                        explosions = newExplosions,
                        particles = newParticles,
                        floatingTexts = newFloatingTexts,
                        powerUps = newPowerUps,
                        activePowerUps = newActivePowerUps,
                        shotLevel = shotLevel,
                        hasShield = hasShield,
                        playerSpeed = playerSpeed,
                        isBossWave = true,
                        bossWarning = true,
                        bossWarningStartTime = currentTime,
                        bossBullets = bossBullets,
                        combo = combo,
                        perfectWave = true,
                        screenShake = ScreenShakeInfo(
                            intensity = 20f,
                            startTime = currentTime,
                            duration = 2000
                        )
                    )
                } else {
                    remainingEnemies.addAll(createWaveEnemies(newWave, newFormation))
                    soundManager.playSound(SoundManager.SFX_LEVELUP)
                }
            }
            
            // Spawn boss after warning
            if (bossWarning && currentTime - bossWarningStartTime > bossWarningDuration && stageBoss == null) {
                bossWarning = false
                stageBoss = StageBoss(
                    position = Offset(screenWidth / 2, 150f),
                    health = 20 + (wave / 5) * 10,  // Health scales with wave
                    maxHealth = 20 + (wave / 5) * 10,
                    phase = 1,
                    attackPattern = BossAttackPattern.CIRCLE_SHOT,
                    lastAttackTime = currentTime
                )
            }

            val isGameOver = lives <= 0
            if (isGameOver) {
                gameLoopJob?.cancel()
                autoFireJob?.cancel()
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
                particles = newParticles,
                floatingTexts = newFloatingTexts,
                powerUps = newPowerUps,
                activePowerUps = newActivePowerUps,
                shotLevel = shotLevel,
                hasShield = hasShield,
                playerSpeed = playerSpeed,
                stageBoss = stageBoss,
                bossBullets = bossBullets,
                isBossWave = isBossWave,
                bossWarning = bossWarning,
                bossWarningStartTime = bossWarningStartTime,
                combo = combo,
                perfectWave = perfectWave,
                screenShake = screenShake
            )
        }
        
        lastUpdateTime = currentTime
    }
    
    // Boss AI update
    private fun updateBoss(
        boss: StageBoss,
        playerPosition: Offset,
        currentTime: Long,
        bossBullets: MutableList<BossBullet>
    ): StageBoss {
        var updatedBoss = boss.copy(animationPhase = boss.animationPhase + 1f)
        
        // Movement pattern - horizontal movement
        val moveSpeed = 2f + (boss.phase - 1) * 0.5f
        val moveRange = screenWidth * 0.35f
        val centerX = screenWidth / 2
        val targetX = centerX + sin(boss.animationPhase * 0.02f) * moveRange
        
        var newPosition = if (!boss.isDiving) {
            Offset(
                boss.position.x + (targetX - boss.position.x) * 0.02f,
                boss.position.y
            )
        } else {
            // Diving attack
            boss.position + Offset(0f, 8f)
        }
        
        // Check if dive attack should end
        if (boss.isDiving && newPosition.y > screenHeight * 0.7f) {
            updatedBoss = updatedBoss.copy(
                isDiving = false,
                position = boss.originalPosition ?: Offset(centerX, 150f)
            )
            newPosition = updatedBoss.position
        }
        
        // Attack patterns based on phase
        val attackInterval = when (boss.phase) {
            1 -> 3000L
            2 -> 2000L
            else -> 1000L
        }
        
        if (currentTime - boss.lastAttackTime > attackInterval && !boss.isDiving) {
            when (boss.phase) {
                1 -> {
                    // Circle shot - 8 directions
                    for (i in 0 until 8) {
                        val angle = (2 * PI * i / 8).toFloat()
                        val velocity = Offset(cos(angle) * 5f, sin(angle) * 5f)
                        bossBullets.add(BossBullet(newPosition, velocity, Color(0xFFFF0055)))
                    }
                    updatedBoss = updatedBoss.copy(
                        lastAttackTime = currentTime,
                        attackPattern = BossAttackPattern.CIRCLE_SHOT
                    )
                }
                2 -> {
                    // Spiral shot - 12 directions with rotation
                    val rotationOffset = (currentTime / 100f) % (2 * PI).toFloat()
                    for (i in 0 until 12) {
                        val angle = (2 * PI * i / 12).toFloat() + rotationOffset
                        val velocity = Offset(cos(angle) * 6f, sin(angle) * 6f)
                        bossBullets.add(BossBullet(newPosition, velocity, Color(0xFF9D4EDD)))
                    }
                    updatedBoss = updatedBoss.copy(
                        lastAttackTime = currentTime,
                        attackPattern = BossAttackPattern.SPIRAL_SHOT
                    )
                }
                3 -> {
                    // Phase 3: Mixed attacks
                    when (Random.nextInt(3)) {
                        0 -> {
                            // Dive attack
                            updatedBoss = updatedBoss.copy(
                                isDiving = true,
                                originalPosition = newPosition,
                                targetPosition = playerPosition,
                                lastAttackTime = currentTime,
                                attackPattern = BossAttackPattern.DIVE_ATTACK
                            )
                        }
                        1 -> {
                            // Aimed shot burst at player
                            val direction = playerPosition - newPosition
                            val distance = sqrt(direction.x * direction.x + direction.y * direction.y)
                            if (distance > 0) {
                                for (spread in -2..2) {
                                    val spreadAngle = spread * 0.15f
                                    val baseAngle = atan2(direction.y, direction.x)
                                    val finalAngle = baseAngle + spreadAngle
                                    val velocity = Offset(cos(finalAngle) * 7f, sin(finalAngle) * 7f)
                                    bossBullets.add(BossBullet(newPosition, velocity, Color(0xFFFF0055)))
                                }
                            }
                            updatedBoss = updatedBoss.copy(lastAttackTime = currentTime)
                        }
                        else -> {
                            // Circle shot (faster)
                            for (i in 0 until 16) {
                                val angle = (2 * PI * i / 16).toFloat()
                                val velocity = Offset(cos(angle) * 8f, sin(angle) * 8f)
                                bossBullets.add(BossBullet(newPosition, velocity, Color(0xFFFFEE00)))
                            }
                            updatedBoss = updatedBoss.copy(lastAttackTime = currentTime)
                        }
                    }
                }
            }
        }
        
        return updatedBoss.copy(position = newPosition)
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
            val angle = Random.nextFloat() * 2f * PI.toFloat()
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
            val angle = -PI.toFloat() / 2 + (Random.nextFloat() - 0.5f) * 0.5f
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
            val layer = i % 3
            val speed = when(layer) {
                0 -> 0.5f + Random.nextFloat() * 0.5f
                1 -> 1.0f + Random.nextFloat() * 0.5f
                else -> 1.5f + Random.nextFloat() * 1.0f
            }
            val brightness = when(layer) {
                0 -> 0.3f + Random.nextFloat() * 0.2f
                1 -> 0.5f + Random.nextFloat() * 0.3f
                else -> 0.7f + Random.nextFloat() * 0.3f
            }
            val size = when(layer) {
                0 -> 1f
                1 -> 1.5f
                else -> 2f + Random.nextFloat()
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
        
        // 웨이브 3, 6, 9마다 보스 추가 (not boss wave - those are separate)
        val includeBoss = wave % 3 == 0 && wave % bossWaveInterval != 0
        
        when (pattern) {
            FormationPattern.V_SHAPE -> {
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
                val rows = 3
                val cols = 4 + (wave / 2).coerceAtMost(3)
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

    fun movePlayer(delta: Offset) {
        _gameState.update {
            if (it.isPaused) return@update it
            val speedMultiplier = it.playerSpeed
            val newX = it.player.position.x + delta.x * speedMultiplier
            val newY = it.player.position.y + delta.y * speedMultiplier
            
            // Y축 이동 제한 (화면 하단 25% 영역만 허용)
            val minY = screenHeight * 0.75f
            val maxY = screenHeight - 100f

            it.copy(
                player = it.player.copy(
                    position = Offset(
                        x = newX.coerceIn(30f, screenWidth - 30f),
                        y = newY.coerceIn(minY, maxY)
                    )
                )
            )
        }
    }

    fun shoot() {
        val currentTime = System.currentTimeMillis()
        _gameState.update {
            if (it.isPaused || it.isGameOver) return@update it
            
            // Create bullets based on shot level
            val newBullets = when (it.shotLevel) {
                3 -> listOf(
                    Bullet(position = it.player.position.copy(x = it.player.position.x - 20f)),
                    Bullet(position = it.player.position),
                    Bullet(position = it.player.position.copy(x = it.player.position.x + 20f))
                )
                2 -> listOf(
                    Bullet(position = it.player.position.copy(x = it.player.position.x - 10f)),
                    Bullet(position = it.player.position.copy(x = it.player.position.x + 10f))
                )
                else -> listOf(Bullet(position = it.player.position))
            }
            
            // Play shoot sound and vibrate
            soundManager.playSound(SoundManager.SFX_SHOOT)
            soundManager.vibrate(SoundManager.VibrationType.SHOOT)
            
            // Add muzzle flash particles
            val muzzleParticles = createMuzzleFlashParticles(it.player.position, currentTime)
            
            it.copy(
                bullets = it.bullets + newBullets,
                particles = it.particles + muzzleParticles
            )
        }
    }
    
    // Start auto-fire mode
    fun startAutoFire() {
        if (autoFireJob?.isActive == true) return
        
        _gameState.update { it.copy(isAutoFiring = true) }
        
        autoFireJob = viewModelScope.launch {
            while (_gameState.value.isAutoFiring && !_gameState.value.isGameOver && !_gameState.value.isPaused) {
                shoot()
                delay(autoFireInterval)
            }
        }
    }
    
    // Stop auto-fire mode
    fun stopAutoFire() {
        autoFireJob?.cancel()
        _gameState.update { it.copy(isAutoFiring = false) }
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
                autoFireJob?.cancel()
            } else {
                soundManager.resumeBGM()
            }
            
            it.copy(isPaused = newPaused, isAutoFiring = false)
        }
    }

    // 게임 재시작
    fun restartGame() {
        gameLoopJob?.cancel()
        autoFireJob?.cancel()
        
        // Reload difficulty settings
        viewModelScope.launch {
            val difficultyName = settingsDataStore.difficulty.first()
            currentDifficulty = try {
                Difficulty.valueOf(difficultyName)
            } catch (e: Exception) {
                Difficulty.NORMAL
            }
            difficultySpeedMultiplier = currentDifficulty.speedMultiplier
            difficultyShootMultiplier = currentDifficulty.shootFrequencyMultiplier
            powerUpDropRate = currentDifficulty.powerUpDropRate
            
            _gameState.value = createInitialState()
            soundManager.playBGM()
            startGameLoop()
        }
    }

    private fun createInitialState(): GameState {
        val initialPattern = FormationPattern.V_SHAPE
        return GameState(
            player = Player(position = Offset(screenWidth / 2, screenHeight - 200f)),
            enemies = createWaveEnemies(1, initialPattern),
            bullets = emptyList(),
            enemyBullets = emptyList(),
            score = 0,
            lives = currentDifficulty.startingLives,
            isGameOver = false,
            currentWave = 1,
            isPaused = false,
            formationPattern = initialPattern,
            stars = createStars(),
            explosions = emptyList(),
            particles = emptyList(),
            floatingTexts = emptyList(),
            powerUps = emptyList(),
            activePowerUps = emptyMap(),
            shotLevel = 1,
            hasShield = false,
            playerSpeed = 1.0f,
            stageBoss = null,
            bossBullets = emptyList(),
            isBossWave = false,
            bossWarning = false,
            combo = ComboInfo(),
            perfectWave = true
        )
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
        autoFireJob?.cancel()
        soundManager.release()
    }

    private fun Offset.getDistance(other: Offset): Float {
        val dx = this.x - other.x
        val dy = this.y - other.y
        return sqrt(dx * dx + dy * dy)
    }
}
