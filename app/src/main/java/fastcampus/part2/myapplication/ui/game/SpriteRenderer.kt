package fastcampus.part2.myapplication.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Galaga-style sprite renderer using Path-based vector graphics
 * Designed for 60 FPS performance
 */
object SpriteRenderer {
    
    // ===== Color Palette =====
    val PLAYER_BODY = Color(0xFFFFFFFF)         // White body
    val PLAYER_ACCENT = Color(0xFFFF0000)       // Red accents
    val PLAYER_COCKPIT = Color(0xFF0000FF)      // Blue cockpit
    val PLAYER_ENGINE = Color(0xFFFF6B35)       // Orange engine glow
    val PLAYER_SHIELD = Color(0xFF00FF88)       // Shield green
    
    // Galaga Classic Colors
    val ZAKO_YELLOW = Color(0xFFFFEE00)         // Yellow Bee
    val ZAKO_RED = Color(0xFFFF0000)            // Red Bee eyes/details
    val ZAKO_BLUE = Color(0xFF0000FF)           // Blue Bee wings
    
    val GOEI_RED = Color(0xFFFF0000)            // Red Butterfly
    val GOEI_BLUE = Color(0xFF0088FF)           // Blue Butterfly
    val GOEI_YELLOW = Color(0xFFFFEE00)         // Yellow details
    
    val BOSS_GREEN = Color(0xFF00AA00)          // Green Boss
    val BOSS_RED = Color(0xFFFF0055)            // Red Boss details
    val BOSS_YELLOW = Color(0xFFFFEE00)         // Yellow Boss details
    
    val STAGE_BOSS_PHASE1 = Color(0xFFFF0055)   // Red
    val STAGE_BOSS_PHASE2 = Color(0xFF9D4EDD)   // Purple
    val STAGE_BOSS_PHASE3 = Color(0xFFFF6B35)   // Orange
    
    val BULLET_PLAYER = Color(0xFF55FFFF)       // Cyan/White bullets
    val BULLET_ENEMY = Color(0xFFFF0000)        // Red bullets
    
    // Power-up colors
    val POWERUP_DOUBLE_SHOT = Color(0xFF00D9FF)  // Cyan
    val POWERUP_TRIPLE_SHOT = Color(0xFF9D4EDD)  // Purple
    val POWERUP_SHIELD = Color(0xFF00FF88)       // Green
    val POWERUP_SPEED = Color(0xFFFFEE00)        // Yellow
    val POWERUP_LIFE = Color(0xFFFF0055)         // Red
    
    // ===== Player Spaceship (Classic Galaga Fighter) =====
    fun DrawScope.drawPlayerSpaceship(
        position: Offset,
        isInvincible: Boolean = false,
        hasShield: Boolean = false,
        time: Long = 0L
    ) {
        if (isInvincible && (time / 100) % 2 == 0L) {
            if (hasShield) drawShieldEffect(position, time)
            return
        }
        
        val centerX = position.x
        val centerY = position.y
        val scale = 1.3f
        
        if (hasShield) drawShieldEffect(position, time)
        
        // Engine glow
        val glowIntensity = 0.5f + 0.5f * sin(time * 0.01f).toFloat()
        drawCircle(
            color = PLAYER_ENGINE.copy(alpha = glowIntensity * 0.6f),
            radius = 12f * scale,
            center = Offset(centerX, centerY + 22f * scale)
        )
        
        // Main Body (White)
        val bodyPath = Path().apply {
            moveTo(centerX, centerY - 25f * scale) // Nose
            lineTo(centerX + 6f * scale, centerY - 15f * scale)
            lineTo(centerX + 6f * scale, centerY + 10f * scale)
            lineTo(centerX + 15f * scale, centerY + 20f * scale) // Wing tip
            lineTo(centerX + 15f * scale, centerY + 25f * scale)
            lineTo(centerX + 8f * scale, centerY + 25f * scale)
            lineTo(centerX + 5f * scale, centerY + 15f * scale) // Engine inner
            lineTo(centerX - 5f * scale, centerY + 15f * scale)
            lineTo(centerX - 8f * scale, centerY + 25f * scale)
            lineTo(centerX - 15f * scale, centerY + 25f * scale)
            lineTo(centerX - 15f * scale, centerY + 20f * scale) // Wing tip
            lineTo(centerX - 6f * scale, centerY + 10f * scale)
            lineTo(centerX - 6f * scale, centerY - 15f * scale)
            close()
        }
        drawPath(bodyPath, PLAYER_BODY, style = Fill)
        
        // Red Accents (Wings & Nose)
        val accentsPath = Path().apply {
            // Nose tip
            moveTo(centerX, centerY - 25f * scale)
            lineTo(centerX + 3f * scale, centerY - 20f * scale)
            lineTo(centerX - 3f * scale, centerY - 20f * scale)
            close()
            
            // Right Wing detail
            moveTo(centerX + 8f * scale, centerY + 10f * scale)
            lineTo(centerX + 13f * scale, centerY + 18f * scale)
            lineTo(centerX + 8f * scale, centerY + 18f * scale)
            close()

            // Left Wing detail
            moveTo(centerX - 8f * scale, centerY + 10f * scale)
            lineTo(centerX - 13f * scale, centerY + 18f * scale)
            lineTo(centerX - 8f * scale, centerY + 18f * scale)
            close()
        }
        drawPath(accentsPath, PLAYER_ACCENT, style = Fill)
        
        // Cockpit (Blue)
        drawRect(
            color = PLAYER_COCKPIT,
            topLeft = Offset(centerX - 2f * scale, centerY + 5f * scale),
            size = Size(4f * scale, 6f * scale)
        )
    }
    
    // Shield effect around player
    private fun DrawScope.drawShieldEffect(position: Offset, time: Long) {
        val pulseScale = 1f + 0.1f * sin(time * 0.005f).toFloat()
        val rotation = (time * 0.001f) % (2 * PI).toFloat()
        
        drawCircle(
            color = PLAYER_SHIELD.copy(alpha = 0.2f),
            radius = 55f * pulseScale,
            center = position
        )
        drawCircle(
            color = PLAYER_SHIELD.copy(alpha = 0.5f),
            radius = 45f * pulseScale,
            center = position,
            style = Stroke(width = 3f)
        )
    }
    
    // ===== Bee Enemy (Zako) =====
    fun DrawScope.drawBeeEnemy(
        position: Offset,
        animationPhase: Float
    ) {
        val centerX = position.x
        val centerY = position.y
        val scale = 1.2f
        val wingFlap = sin(animationPhase * 0.3f) * 4f
        
        // Wings (Blue, flapping)
        val wingsPath = Path().apply {
            moveTo(centerX, centerY - 5f * scale)
            // Left wing
            lineTo(centerX - 15f * scale, centerY - 12f * scale + wingFlap)
            lineTo(centerX - 12f * scale, centerY + 2f * scale + wingFlap)
            lineTo(centerX - 5f * scale, centerY + 5f * scale)
            // Right wing
            lineTo(centerX + 12f * scale, centerY + 2f * scale + wingFlap)
            lineTo(centerX + 15f * scale, centerY - 12f * scale + wingFlap)
            close()
        }
        drawPath(wingsPath, ZAKO_BLUE, style = Fill)
        
        // Body (Yellow)
        val bodyPath = Path().apply {
            moveTo(centerX, centerY - 10f * scale) // Head top
            lineTo(centerX + 6f * scale, centerY - 4f * scale)
            lineTo(centerX + 8f * scale, centerY + 2f * scale)
            lineTo(centerX + 4f * scale, centerY + 8f * scale) // Tail
            lineTo(centerX - 4f * scale, centerY + 8f * scale)
            lineTo(centerX - 8f * scale, centerY + 2f * scale)
            lineTo(centerX - 6f * scale, centerY - 4f * scale)
            close()
        }
        drawPath(bodyPath, ZAKO_YELLOW, style = Fill)
        
        // Eyes/Mouth (Red details)
        drawRect(
            color = ZAKO_RED,
            topLeft = Offset(centerX - 4f * scale, centerY - 2f * scale),
            size = Size(3f * scale, 3f * scale)
        )
        drawRect(
            color = ZAKO_RED,
            topLeft = Offset(centerX + 1f * scale, centerY - 2f * scale),
            size = Size(3f * scale, 3f * scale)
        )
    }
    
    // ===== Butterfly Enemy (Goei) =====
    fun DrawScope.drawButterflyEnemy(
        position: Offset,
        animationPhase: Float
    ) {
        val centerX = position.x
        val centerY = position.y
        val scale = 1.3f
        val wingFlap = sin(animationPhase * 0.25f) * 5f
        
        // Wings (Red main color)
        val wingsPath = Path().apply {
            moveTo(centerX, centerY + 8f * scale) // Bottom center
            // Left wing
            lineTo(centerX - 6f * scale, centerY + 2f * scale)
            lineTo(centerX - 18f * scale + wingFlap, centerY - 10f * scale) // Wing tip
            lineTo(centerX - 4f * scale, centerY - 8f * scale)
            // Right wing
            lineTo(centerX + 18f * scale - wingFlap, centerY - 10f * scale) // Wing tip
            lineTo(centerX + 6f * scale, centerY + 2f * scale)
            close()
        }
        drawPath(wingsPath, GOEI_RED, style = Fill)
        
        // Inner detail (Blue)
        drawCircle(
            color = GOEI_BLUE,
            radius = 4f * scale,
            center = Offset(centerX - 8f * scale + wingFlap/2, centerY - 5f * scale)
        )
        drawCircle(
            color = GOEI_BLUE,
            radius = 4f * scale,
            center = Offset(centerX + 8f * scale - wingFlap/2, centerY - 5f * scale)
        )
        
        // Antennae/Head (Yellow)
        drawLine(
            color = GOEI_YELLOW,
            start = Offset(centerX, centerY - 5f * scale),
            end = Offset(centerX - 5f * scale, centerY - 15f * scale),
            strokeWidth = 2f
        )
        drawLine(
            color = GOEI_YELLOW,
            start = Offset(centerX, centerY - 5f * scale),
            end = Offset(centerX + 5f * scale, centerY - 15f * scale),
            strokeWidth = 2f
        )
    }
    
    // ===== Boss Enemy (Galaga Boss) =====
    fun DrawScope.drawBossEnemy(
        position: Offset,
        animationPhase: Float,
        health: Int
    ) {
        val centerX = position.x
        val centerY = position.y
        val scale = 1.5f
        
        // Main Body (Green) - Shaped like a tractor beam emitter
        val bodyPath = Path().apply {
            moveTo(centerX, centerY + 12f * scale) // Bottom point
            lineTo(centerX + 10f * scale, centerY + 5f * scale)
            lineTo(centerX + 15f * scale, centerY - 5f * scale)
            lineTo(centerX + 8f * scale, centerY - 15f * scale) // Top right
            lineTo(centerX - 8f * scale, centerY - 15f * scale) // Top left
            lineTo(centerX - 15f * scale, centerY - 5f * scale)
            lineTo(centerX - 10f * scale, centerY + 5f * scale)
            close()
        }
        val bodyColor = if (health == 1) Color(0xFF0055AA) else BOSS_GREEN // Blue when damaged (classic behavior)
        drawPath(bodyPath, bodyColor, style = Fill)
        
        // Mandibles/Arms (Red)
        val armsPath = Path().apply {
            moveTo(centerX - 8f * scale, centerY - 15f * scale)
            lineTo(centerX - 12f * scale, centerY - 22f * scale)
            lineTo(centerX - 4f * scale, centerY - 18f * scale)
            
            moveTo(centerX + 8f * scale, centerY - 15f * scale)
            lineTo(centerX + 12f * scale, centerY - 22f * scale)
            lineTo(centerX + 4f * scale, centerY - 18f * scale)
        }
        drawPath(armsPath, BOSS_RED, style = Stroke(width = 3f * scale))
        
        // Center Detail (Yellow)
        drawRect(
            color = BOSS_YELLOW,
            topLeft = Offset(centerX - 4f * scale, centerY - 5f * scale),
            size = Size(8f * scale, 6f * scale)
        )
        
        // Health Bar (Mini)
        if (health > 1) {
             drawRect(
                color = Color.Green,
                topLeft = Offset(centerX - 10f, centerY - 30f),
                size = Size(20f * (health/3f), 4f)
            )
        }
    }
    
    // ===== Stage Boss (large boss) =====
    fun DrawScope.drawStageBoss(
        boss: StageBoss,
        time: Long
    ) {
        val centerX = boss.position.x
        val centerY = boss.position.y
        val scale = 2.5f
        val pulse = 1f + sin(boss.animationPhase * 0.1f) * 0.08f
        
        val phaseColor = when (boss.phase) {
            1 -> STAGE_BOSS_PHASE1
            2 -> STAGE_BOSS_PHASE2
            else -> STAGE_BOSS_PHASE3
        }
        
        val displayColor = if (boss.isHit) Color.White else phaseColor
        
        drawCircle(
            color = phaseColor.copy(alpha = 0.15f),
            radius = 90f * scale * pulse,
            center = boss.position
        )
        
        val bodyPath = Path().apply {
            val radius = 40f * scale
            for (i in 0 until 6) {
                val angle = (PI / 3 * i - PI / 2).toFloat()
                val x = centerX + radius * cos(angle)
                val y = centerY + radius * sin(angle)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(bodyPath, displayColor, style = Fill)
        drawPath(bodyPath, Color.White, style = Stroke(width = 4f))
        
        drawCircle(
            color = BOSS_YELLOW,
            radius = 15f * scale,
            center = boss.position
        )
    }
    
    // ===== Boss Health Bar =====
    fun DrawScope.drawBossHealthBar(
        boss: StageBoss,
        screenWidth: Float,
        time: Long
    ) {
        val barWidth = screenWidth * 0.8f
        val barHeight = 16f
        val barX = (screenWidth - barWidth) / 2
        val barY = 80f
        
        val healthRatio = boss.health.toFloat() / boss.maxHealth
        
        drawRect(
            color = Color.Black.copy(alpha = 0.7f),
            topLeft = Offset(barX - 4f, barY - 4f),
            size = Size(barWidth + 8f, barHeight + 8f)
        )
        drawRect(
            color = Color.DarkGray,
            topLeft = Offset(barX, barY),
            size = Size(barWidth, barHeight)
        )
        drawRect(
            color = Color(0xFFFF0055),
            topLeft = Offset(barX, barY),
            size = Size(barWidth * healthRatio, barHeight)
        )
    }
    
    // ===== Power-up Item =====
    fun DrawScope.drawPowerUp(
        powerUp: PowerUp,
        time: Long
    ) {
        val pulse = 1f + 0.2f * sin(time * 0.008f)
        val color = when (powerUp.type) {
            PowerUpType.DOUBLE_SHOT -> POWERUP_DOUBLE_SHOT
            PowerUpType.TRIPLE_SHOT -> POWERUP_TRIPLE_SHOT
            PowerUpType.SHIELD -> POWERUP_SHIELD
            PowerUpType.SPEED_UP -> POWERUP_SPEED
            PowerUpType.EXTRA_LIFE -> POWERUP_LIFE
        }
        
        drawCircle(
            color = color.copy(alpha = 0.3f),
            radius = 28f * pulse.toFloat(),
            center = powerUp.position
        )
        drawCircle(
            color = color,
            radius = 18f * pulse.toFloat(),
            center = powerUp.position
        )
    }
    
    // ===== Boss Bullet =====
    fun DrawScope.drawBossBullet(
        bullet: BossBullet,
        time: Long
    ) {
        drawCircle(
            color = bullet.color,
            radius = 8f,
            center = bullet.position
        )
    }
    
    // ===== Player Bullet (Classic Galaga Style) =====
    fun DrawScope.drawPlayerBullet(position: Offset, time: Long) {
        // Simple distinct rectangle/oval for bullets
        drawRect(
            color = BULLET_PLAYER,
            topLeft = Offset(position.x - 4f, position.y - 15f),
            size = Size(8f, 30f)
        )
        drawRect(
            color = Color.White,
            topLeft = Offset(position.x - 2f, position.y - 10f),
            size = Size(4f, 20f)
        )
    }
    
    // ===== Enemy Bullet =====
    fun DrawScope.drawEnemyBullet(position: Offset, time: Long) {
        drawCircle(
            color = BULLET_ENEMY,
            radius = 8f,
            center = position
        )
    }
    
    // ===== Background Star =====
    fun DrawScope.drawStar(star: Star) {
        drawCircle(
            color = Color.White.copy(alpha = star.brightness),
            radius = star.size,
            center = star.position
        )
    }
    
    // ===== Explosion Effect =====
    fun DrawScope.drawExplosion(
        explosion: Explosion,
        currentTime: Long
    ) {
        val elapsed = currentTime - explosion.startTime
        val progress = (elapsed.toFloat() / explosion.duration).coerceIn(0f, 1f)
        if (progress >= 1f) return
        
        val alpha = 1f - progress
        val maxRadius = 50f
        
        drawCircle(
            color = explosion.color.copy(alpha = alpha),
            radius = maxRadius * progress,
            center = explosion.position,
            style = Stroke(width = 4f)
        )
        
        // Particles
        for (i in 0 until 8) {
            val angle = (2 * PI * i / 8 + progress * 2).toFloat()
            val dist = maxRadius * progress * 1.5f
            val x = explosion.position.x + dist * cos(angle)
            val y = explosion.position.y + dist * sin(angle)
            drawCircle(
                color = explosion.color.copy(alpha = alpha),
                radius = 3f,
                center = Offset(x, y)
            )
        }
    }
    
    // ===== Particle Effect =====
    fun DrawScope.drawParticle(
        particle: Particle,
        currentTime: Long
    ) {
        val elapsed = currentTime - particle.createdAt
        val progress = (elapsed.toFloat() / particle.lifetime).coerceIn(0f, 1f)
        if (progress >= 1f) return
        
        drawCircle(
            color = particle.color.copy(alpha = 1f - progress),
            radius = particle.size * (1f - progress * 0.5f),
            center = particle.position
        )
    }
}
