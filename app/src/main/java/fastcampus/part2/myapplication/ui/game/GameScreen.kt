package fastcampus.part2.myapplication.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fastcampus.part2.myapplication.ui.game.SpriteRenderer.drawBeeEnemy
import fastcampus.part2.myapplication.ui.game.SpriteRenderer.drawBossEnemy
import fastcampus.part2.myapplication.ui.game.SpriteRenderer.drawBossBullet
import fastcampus.part2.myapplication.ui.game.SpriteRenderer.drawBossHealthBar
import fastcampus.part2.myapplication.ui.game.SpriteRenderer.drawButterflyEnemy
import fastcampus.part2.myapplication.ui.game.SpriteRenderer.drawEnemyBullet
import fastcampus.part2.myapplication.ui.game.SpriteRenderer.drawExplosion
import fastcampus.part2.myapplication.ui.game.SpriteRenderer.drawParticle
import fastcampus.part2.myapplication.ui.game.SpriteRenderer.drawPlayerBullet
import fastcampus.part2.myapplication.ui.game.SpriteRenderer.drawPlayerSpaceship
import fastcampus.part2.myapplication.ui.game.SpriteRenderer.drawPowerUp
import fastcampus.part2.myapplication.ui.game.SpriteRenderer.drawStageBoss
import fastcampus.part2.myapplication.ui.game.SpriteRenderer.drawStar
import kotlinx.coroutines.delay
import kotlin.math.sin

// Neon text style for UI
private val neonGreen = Color(0xFF39FF14)
private val neonCyan = Color(0xFF00D9FF)
private val neonRed = Color(0xFFFF5555)
private val neonYellow = Color(0xFFFFEE00)
private val neonPurple = Color(0xFF9D4EDD)

@Composable
fun GameScreen(
    playerName: String,
    onNavigateToRanking: (Int, Int) -> Unit,
    onNavigateToTitle: () -> Unit = {},
    gameViewModel: GameViewModel = viewModel()
) {
    val gameState by gameViewModel.gameState.collectAsState()
    val currentTime = remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    // Update time for animations
    LaunchedEffect(Unit) {
        while (true) {
            currentTime.longValue = System.currentTimeMillis()
            delay(16)  // ~60 FPS
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF000000),  // Pure black at top
                        Color(0xFF0A0E27),  // Dark navy
                        Color(0xFF0D1B2A)   // Slightly lighter at bottom
                    )
                )
            )
            .onSizeChanged { size ->
                gameViewModel.updateScreenSize(size.width.toFloat(), size.height.toFloat())
            }
    ) {
        // Calculate screen shake offset
        val shakeOffset = if (gameState.screenShake.intensity > 0) {
            val time = currentTime.longValue
            Offset(
                (sin(time * 0.1f) * gameState.screenShake.intensity).toFloat(),
                (sin(time * 0.13f) * gameState.screenShake.intensity).toFloat()
            )
        } else {
            Offset.Zero
        }
        
        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        gameViewModel.movePlayer(dragAmount.x)
                    },
                    onDragEnd = { }
                )
            }
            .pointerInput(Unit) {
                // Long press for auto-fire
                detectTapGestures(
                    onPress = {
                        gameViewModel.startAutoFire()
                        tryAwaitRelease()
                        gameViewModel.stopAutoFire()
                    },
                    onTap = {
                        gameViewModel.shoot()
                    }
                )
            }
        ) {
            val time = currentTime.longValue
            val screenWidth = size.width
            
            // Apply screen shake
            withTransform({
                translate(shakeOffset.x, shakeOffset.y)
            }) {
                // ===== Layer 1: Background Stars =====
                gameState.stars.forEach { star ->
                    drawStar(star)
                }
                
                // ===== Layer 2: Particles (behind entities) =====
                gameState.particles.forEach { particle ->
                    drawParticle(particle, time)
                }
                
                // ===== Layer 3: Power-ups =====
                gameState.powerUps.forEach { powerUp ->
                    drawPowerUp(powerUp, time)
                }
                
                // ===== Layer 4: Player Bullets =====
                gameState.bullets.forEach { bullet ->
                    drawPlayerBullet(bullet.position, time)
                }
                
                // ===== Layer 5: Enemy Bullets =====
                gameState.enemyBullets.forEach { bullet ->
                    drawEnemyBullet(bullet.position, time)
                }
                
                // ===== Layer 6: Boss Bullets =====
                gameState.bossBullets.forEach { bullet ->
                    drawBossBullet(bullet, time)
                }
                
                // ===== Layer 7: Enemies =====
                gameState.enemies.forEach { enemy ->
                    when (enemy.type) {
                        EnemyType.BEE -> drawBeeEnemy(enemy.position, enemy.animationPhase)
                        EnemyType.BUTTERFLY -> drawButterflyEnemy(enemy.position, enemy.animationPhase)
                        EnemyType.BOSS -> drawBossEnemy(enemy.position, enemy.animationPhase, enemy.health)
                    }
                }
                
                // ===== Layer 8: Stage Boss =====
                gameState.stageBoss?.let { boss ->
                    drawStageBoss(boss, time)
                    drawBossHealthBar(boss, screenWidth, time)
                }
                
                // ===== Layer 9: Player Spaceship =====
                drawPlayerSpaceship(
                    position = gameState.player.position,
                    isInvincible = gameState.player.isInvincible,
                    hasShield = gameState.hasShield,
                    time = time
                )
                
                // ===== Layer 10: Explosions (on top) =====
                gameState.explosions.forEach { explosion ->
                    drawExplosion(explosion, time)
                }
            }
        }

        // ===== UI Layer =====
        // Top UI: Player info, score, lives, wave
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                // Player name with neon effect
                Text(
                    text = playerName,
                    style = TextStyle(
                        color = neonGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(
                            color = neonGreen.copy(alpha = 0.7f),
                            offset = Offset(0f, 0f),
                            blurRadius = 10f
                        )
                    )
                )
                // Score
                Text(
                    text = "SCORE: ${gameState.score}",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        shadow = Shadow(
                            color = Color.White.copy(alpha = 0.5f),
                            offset = Offset(0f, 0f),
                            blurRadius = 5f
                        )
                    )
                )
                // Lives with heart icons
                Row {
                    Text(
                        text = "LIVES: ",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    repeat(gameState.lives.coerceAtLeast(0)) {
                        Text(
                            text = "❤",
                            style = TextStyle(
                                color = neonRed,
                                fontSize = 16.sp,
                                shadow = Shadow(
                                    color = neonRed.copy(alpha = 0.7f),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 8f
                                )
                            )
                        )
                    }
                }
                
                // Active power-ups indicator
                if (gameState.activePowerUps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        gameState.activePowerUps.forEach { (type, _) ->
                            val icon = when (type) {
                                PowerUpType.DOUBLE_SHOT -> "🔫🔫"
                                PowerUpType.TRIPLE_SHOT -> "🔫🔫🔫"
                                PowerUpType.SHIELD -> "🛡️"
                                PowerUpType.SPEED_UP -> "⚡"
                                PowerUpType.EXTRA_LIFE -> ""
                            }
                            if (icon.isNotEmpty()) {
                                Text(
                                    text = icon,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                // Wave indicator with neon effect
                val waveColor = if (gameState.isBossWave) neonRed else neonCyan
                Text(
                    text = if (gameState.isBossWave) "⚠️ BOSS WAVE ${gameState.currentWave}" else "WAVE ${gameState.currentWave}",
                    style = TextStyle(
                        color = waveColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(
                            color = waveColor.copy(alpha = 0.7f),
                            offset = Offset(0f, 0f),
                            blurRadius = 12f
                        )
                    )
                )
                // Pause button
                IconButton(
                    onClick = { gameViewModel.togglePause() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (gameState.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (gameState.isPaused) "Resume" else "Pause",
                        tint = Color.White
                    )
                }
            }
        }
        
        // Combo display
        if (gameState.combo.count >= 2 && currentTime.longValue < gameState.combo.displayUntil) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 120.dp)
            ) {
                Text(
                    text = "${gameState.combo.count}x COMBO!",
                    style = TextStyle(
                        color = neonYellow,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(
                            color = neonYellow.copy(alpha = 0.8f),
                            offset = Offset(0f, 0f),
                            blurRadius = 15f
                        )
                    )
                )
            }
        }
        
        // Floating texts (bonus points, power-up names)
        gameState.floatingTexts.forEach { floatingText ->
            val progress = (currentTime.longValue - floatingText.createdAt).toFloat() / floatingText.duration
            if (progress < 1f) {
                val alpha = 1f - progress
                val yOffset = progress * 50f
                
                // Position relative to game area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Text(
                        text = floatingText.text,
                        style = TextStyle(
                            color = floatingText.color.copy(alpha = alpha),
                            fontSize = floatingText.fontSize.sp,
                            fontWeight = FontWeight.Bold,
                            shadow = Shadow(
                                color = floatingText.color.copy(alpha = alpha * 0.7f),
                                offset = Offset(0f, 0f),
                                blurRadius = 10f
                            )
                        ),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
        
        // Boss Warning Overlay
        if (gameState.bossWarning) {
            val warningAlpha = (sin(currentTime.longValue * 0.01f) * 0.3f + 0.5f).toFloat()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(neonRed.copy(alpha = warningAlpha * 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "⚠️ WARNING ⚠️",
                        style = TextStyle(
                            color = neonRed,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            shadow = Shadow(
                                color = neonRed.copy(alpha = 0.9f),
                                offset = Offset(0f, 0f),
                                blurRadius = 25f
                            )
                        )
                    )
                    Text(
                        text = "BOSS APPROACHING",
                        style = TextStyle(
                            color = neonYellow,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            shadow = Shadow(
                                color = neonYellow.copy(alpha = 0.8f),
                                offset = Offset(0f, 0f),
                                blurRadius = 15f
                            )
                        )
                    )
                }
            }
        }

        // Fire button with auto-fire indicator
        Button(
            onClick = { gameViewModel.shoot() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .size(80.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (gameState.isAutoFiring) neonYellow.copy(alpha = 0.9f) 
                               else neonCyan.copy(alpha = 0.9f)
            ),
            shape = CircleShape,
            enabled = !gameState.isPaused && !gameState.isGameOver
        ) {
            Text(
                text = if (gameState.isAutoFiring) "AUTO" else "FIRE",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }

        // Pause overlay
        if (gameState.isPaused && !gameState.isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "PAUSED",
                        style = TextStyle(
                            color = neonGreen,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            shadow = Shadow(
                                color = neonGreen.copy(alpha = 0.8f),
                                offset = Offset(0f, 0f),
                                blurRadius = 20f
                            )
                        )
                    )
                    Text(
                        text = "Wave ${gameState.currentWave}",
                        style = TextStyle(
                            color = neonCyan,
                            fontSize = 24.sp,
                            shadow = Shadow(
                                color = neonCyan.copy(alpha = 0.6f),
                                offset = Offset(0f, 0f),
                                blurRadius = 10f
                            )
                        )
                    )
                    Text(
                        text = "Score: ${gameState.score}",
                        color = Color.White,
                        fontSize = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Resume button
                    Button(
                        onClick = { gameViewModel.togglePause() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = neonCyan
                        ),
                        modifier = Modifier.size(width = 200.dp, height = 56.dp)
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black
                        )
                        Text("  RESUME", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    
                    // Settings button
                    Button(
                        onClick = { /* Could open in-game settings */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = neonGreen.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.size(width = 200.dp, height = 56.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = neonGreen
                        )
                        Text("  설정", color = neonGreen, fontWeight = FontWeight.Bold)
                    }
                    
                    // Exit to title button
                    Button(
                        onClick = onNavigateToTitle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = neonRed.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.size(width = 200.dp, height = 56.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = neonRed
                        )
                        Text("  메인 메뉴", color = neonRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Game Over overlay
        if (gameState.isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "GAME OVER",
                        style = TextStyle(
                            color = neonRed,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            shadow = Shadow(
                                color = neonRed.copy(alpha = 0.8f),
                                offset = Offset(0f, 0f),
                                blurRadius = 25f
                            )
                        )
                    )
                    Text(
                        text = "Final Score",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 18.sp
                    )
                    Text(
                        text = "${gameState.score}",
                        style = TextStyle(
                            color = neonYellow,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            shadow = Shadow(
                                color = neonYellow.copy(alpha = 0.7f),
                                offset = Offset(0f, 0f),
                                blurRadius = 15f
                            )
                        )
                    )
                    Text(
                        text = "Wave Reached: ${gameState.currentWave}",
                        style = TextStyle(
                            color = neonCyan,
                            fontSize = 20.sp,
                            shadow = Shadow(
                                color = neonCyan.copy(alpha = 0.5f),
                                offset = Offset(0f, 0f),
                                blurRadius = 8f
                            )
                        )
                    )
                    
                    // Show max combo
                    if (gameState.combo.count >= 2) {
                        Text(
                            text = "Best Combo: ${gameState.combo.count}x",
                            style = TextStyle(
                                color = neonPurple,
                                fontSize = 18.sp,
                                shadow = Shadow(
                                    color = neonPurple.copy(alpha = 0.5f),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 8f
                                )
                            )
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Button(
                            onClick = { gameViewModel.restartGame() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00FF88)
                            ),
                            modifier = Modifier.size(width = 120.dp, height = 48.dp)
                        ) {
                            Text("RETRY", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onNavigateToRanking(gameState.score, gameState.currentWave) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = neonYellow
                            ),
                            modifier = Modifier.size(width = 120.dp, height = 48.dp)
                        ) {
                            Text("RANKING", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    // Main menu button
                    Button(
                        onClick = onNavigateToTitle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("메인 메뉴", color = Color.White)
                    }
                }
            }
        }
    }
}

// Backward compatible version for old navigation (single score parameter)
@Composable
fun GameScreen(
    playerName: String,
    onNavigateToRanking: (Int) -> Unit,
    gameViewModel: GameViewModel = viewModel()
) {
    GameScreen(
        playerName = playerName,
        onNavigateToRanking = { score, _ -> onNavigateToRanking(score) },
        onNavigateToTitle = {},
        gameViewModel = gameViewModel
    )
}
