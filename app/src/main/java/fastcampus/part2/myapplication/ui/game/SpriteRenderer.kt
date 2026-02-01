package fastcampus.part2.myapplication.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

/**
 * Galaga-style sprite renderer using Path-based vector graphics
 * Designed for 60 FPS performance
 */
object SpriteRenderer {
    
    // ===== Color Palette =====
    val PLAYER_PRIMARY = Color(0xFF00D9FF)      // Bright cyan
    val PLAYER_SECONDARY = Color(0xFF0088AA)    // Darker cyan
    val PLAYER_ENGINE = Color(0xFFFF6B35)       // Orange engine glow
    
    val BEE_PRIMARY = Color(0xFFFFEE00)         // Yellow
    val BEE_SECONDARY = Color(0xFFFF0055)       // Red
    
    val BUTTERFLY_PRIMARY = Color(0xFF00FF88)   // Green
    val BUTTERFLY_SECONDARY = Color(0xFF00D9FF) // Cyan
    
    val BOSS_PRIMARY = Color(0xFFFF0055)        // Red
    val BOSS_SECONDARY = Color(0xFFFFFFFF)      // White
    val BOSS_ACCENT = Color(0xFFFFEE00)         // Yellow
    
    val BULLET_PLAYER = Color(0xFFFFEE00)       // Yellow
    val BULLET_ENEMY = Color(0xFFFF0055)        // Red
    
    // ===== Player Spaceship =====
    fun DrawScope.drawPlayerSpaceship(
        position: Offset,
        isInvincible: Boolean = false,
        time: Long = 0L
    ) {
        // Blinking effect when invincible
        if (isInvincible && (time / 100) % 2 == 0L) {
            return  // Skip drawing for blink effect
        }
        
        val centerX = position.x
        val centerY = position.y
        val scale = 1.2f
        
        // Engine glow (animated)
        val glowIntensity = 0.5f + 0.5f * sin(time * 0.01f).toFloat()
        drawCircle(
            color = PLAYER_ENGINE.copy(alpha = glowIntensity * 0.6f),
            radius = 15f * scale,
            center = Offset(centerX, centerY + 25f * scale)
        )
        
        // Main body (triangle with wings)
        val bodyPath = Path().apply {
            // Main body triangle
            moveTo(centerX, centerY - 30f * scale)  // Top
            lineTo(centerX - 8f * scale, centerY + 10f * scale)
            lineTo(centerX + 8f * scale, centerY + 10f * scale)
            close()
        }
        drawPath(bodyPath, PLAYER_PRIMARY, style = Fill)
        
        // Left wing
        val leftWing = Path().apply {
            moveTo(centerX - 8f * scale, centerY)
            lineTo(centerX - 25f * scale, centerY + 20f * scale)
            lineTo(centerX - 25f * scale, centerY + 25f * scale)
            lineTo(centerX - 8f * scale, centerY + 15f * scale)
            close()
        }
        drawPath(leftWing, PLAYER_PRIMARY, style = Fill)
        
        // Right wing
        val rightWing = Path().apply {
            moveTo(centerX + 8f * scale, centerY)
            lineTo(centerX + 25f * scale, centerY + 20f * scale)
            lineTo(centerX + 25f * scale, centerY + 25f * scale)
            lineTo(centerX + 8f * scale, centerY + 15f * scale)
            close()
        }
        drawPath(rightWing, PLAYER_PRIMARY, style = Fill)
        
        // Cockpit
        val cockpit = Path().apply {
            moveTo(centerX, centerY - 20f * scale)
            lineTo(centerX - 5f * scale, centerY - 5f * scale)
            lineTo(centerX + 5f * scale, centerY - 5f * scale)
            close()
        }
        drawPath(cockpit, PLAYER_SECONDARY, style = Fill)
        
        // Engine flames
        val flameHeight = 10f + 5f * sin(time * 0.02f).toFloat()
        val flamePath = Path().apply {
            moveTo(centerX - 5f * scale, centerY + 15f * scale)
            lineTo(centerX, centerY + 15f * scale + flameHeight * scale)
            lineTo(centerX + 5f * scale, centerY + 15f * scale)
            close()
        }
        drawPath(flamePath, PLAYER_ENGINE, style = Fill)
    }
    
    // ===== Bee Enemy =====
    fun DrawScope.drawBeeEnemy(
        position: Offset,
        animationPhase: Float
    ) {
        val centerX = position.x
        val centerY = position.y
        val scale = 0.9f
        val wingFlap = sin(animationPhase * 0.3f) * 5f
        
        // Wings (animated)
        // Left wing
        val leftWing = Path().apply {
            moveTo(centerX - 5f * scale, centerY)
            lineTo(centerX - 18f * scale, centerY - 8f * scale + wingFlap)
            lineTo(centerX - 15f * scale, centerY + 5f * scale + wingFlap)
            close()
        }
        drawPath(leftWing, BEE_SECONDARY.copy(alpha = 0.8f), style = Fill)
        
        // Right wing
        val rightWing = Path().apply {
            moveTo(centerX + 5f * scale, centerY)
            lineTo(centerX + 18f * scale, centerY - 8f * scale + wingFlap)
            lineTo(centerX + 15f * scale, centerY + 5f * scale + wingFlap)
            close()
        }
        drawPath(rightWing, BEE_SECONDARY.copy(alpha = 0.8f), style = Fill)
        
        // Body
        drawCircle(
            color = BEE_PRIMARY,
            radius = 10f * scale,
            center = Offset(centerX, centerY)
        )
        
        // Stripes
        drawRect(
            color = BEE_SECONDARY,
            topLeft = Offset(centerX - 8f * scale, centerY - 3f * scale),
            size = Size(16f * scale, 3f * scale)
        )
        drawRect(
            color = BEE_SECONDARY,
            topLeft = Offset(centerX - 6f * scale, centerY + 3f * scale),
            size = Size(12f * scale, 3f * scale)
        )
        
        // Eyes
        drawCircle(
            color = Color.White,
            radius = 3f * scale,
            center = Offset(centerX - 4f * scale, centerY - 5f * scale)
        )
        drawCircle(
            color = Color.White,
            radius = 3f * scale,
            center = Offset(centerX + 4f * scale, centerY - 5f * scale)
        )
    }
    
    // ===== Butterfly Enemy =====
    fun DrawScope.drawButterflyEnemy(
        position: Offset,
        animationPhase: Float
    ) {
        val centerX = position.x
        val centerY = position.y
        val scale = 1.1f
        val wingFlap = sin(animationPhase * 0.25f) * 8f
        
        // Left wing (large)
        val leftWing = Path().apply {
            moveTo(centerX - 5f * scale, centerY - 5f * scale)
            lineTo(centerX - 22f * scale + wingFlap, centerY - 15f * scale)
            lineTo(centerX - 25f * scale + wingFlap, centerY + 5f * scale)
            lineTo(centerX - 18f * scale, centerY + 18f * scale)
            lineTo(centerX - 5f * scale, centerY + 10f * scale)
            close()
        }
        drawPath(leftWing, BUTTERFLY_PRIMARY, style = Fill)
        drawPath(leftWing, BUTTERFLY_SECONDARY, style = Stroke(width = 2f))
        
        // Right wing (large)
        val rightWing = Path().apply {
            moveTo(centerX + 5f * scale, centerY - 5f * scale)
            lineTo(centerX + 22f * scale - wingFlap, centerY - 15f * scale)
            lineTo(centerX + 25f * scale - wingFlap, centerY + 5f * scale)
            lineTo(centerX + 18f * scale, centerY + 18f * scale)
            lineTo(centerX + 5f * scale, centerY + 10f * scale)
            close()
        }
        drawPath(rightWing, BUTTERFLY_PRIMARY, style = Fill)
        drawPath(rightWing, BUTTERFLY_SECONDARY, style = Stroke(width = 2f))
        
        // Wing patterns
        drawCircle(
            color = BUTTERFLY_SECONDARY.copy(alpha = 0.7f),
            radius = 6f * scale,
            center = Offset(centerX - 15f * scale + wingFlap / 2, centerY)
        )
        drawCircle(
            color = BUTTERFLY_SECONDARY.copy(alpha = 0.7f),
            radius = 6f * scale,
            center = Offset(centerX + 15f * scale - wingFlap / 2, centerY)
        )
        
        // Body
        val bodyPath = Path().apply {
            moveTo(centerX, centerY - 12f * scale)
            lineTo(centerX - 4f * scale, centerY)
            lineTo(centerX - 3f * scale, centerY + 15f * scale)
            lineTo(centerX + 3f * scale, centerY + 15f * scale)
            lineTo(centerX + 4f * scale, centerY)
            close()
        }
        drawPath(bodyPath, BUTTERFLY_SECONDARY, style = Fill)
        
        // Antennae
        drawLine(
            color = BUTTERFLY_SECONDARY,
            start = Offset(centerX - 3f * scale, centerY - 12f * scale),
            end = Offset(centerX - 8f * scale, centerY - 20f * scale),
            strokeWidth = 2f
        )
        drawLine(
            color = BUTTERFLY_SECONDARY,
            start = Offset(centerX + 3f * scale, centerY - 12f * scale),
            end = Offset(centerX + 8f * scale, centerY - 20f * scale),
            strokeWidth = 2f
        )
    }
    
    // ===== Boss Enemy =====
    fun DrawScope.drawBossEnemy(
        position: Offset,
        animationPhase: Float,
        health: Int
    ) {
        val centerX = position.x
        val centerY = position.y
        val scale = 1.5f
        val pulse = 1f + sin(animationPhase * 0.2f) * 0.05f
        
        // Outer shield/aura (glowing ring)
        drawCircle(
            color = BOSS_ACCENT.copy(alpha = 0.3f),
            radius = 35f * scale * pulse,
            center = position
        )
        
        // Main body (hexagonal shape)
        val bodyPath = Path().apply {
            val radius = 25f * scale
            for (i in 0 until 6) {
                val angle = (Math.PI / 3 * i - Math.PI / 2).toFloat()
                val x = centerX + radius * cos(angle)
                val y = centerY + radius * sin(angle)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(bodyPath, BOSS_PRIMARY, style = Fill)
        drawPath(bodyPath, BOSS_SECONDARY, style = Stroke(width = 3f))
        
        // Inner core
        drawCircle(
            color = BOSS_ACCENT,
            radius = 12f * scale,
            center = position
        )
        
        // Eyes (menacing)
        val eyeOffset = 8f * scale
        drawCircle(
            color = Color.Black,
            radius = 5f * scale,
            center = Offset(centerX - eyeOffset, centerY - 3f * scale)
        )
        drawCircle(
            color = Color.Black,
            radius = 5f * scale,
            center = Offset(centerX + eyeOffset, centerY - 3f * scale)
        )
        drawCircle(
            color = BOSS_SECONDARY,
            radius = 3f * scale,
            center = Offset(centerX - eyeOffset, centerY - 3f * scale)
        )
        drawCircle(
            color = BOSS_SECONDARY,
            radius = 3f * scale,
            center = Offset(centerX + eyeOffset, centerY - 3f * scale)
        )
        
        // Tentacles/appendages (animated)
        for (i in 0 until 4) {
            val baseAngle = (Math.PI / 2 * i + Math.PI / 4).toFloat()
            val tentacleWave = sin(animationPhase * 0.15f + i) * 3f
            val startX = centerX + 20f * scale * cos(baseAngle)
            val startY = centerY + 20f * scale * sin(baseAngle)
            val endX = centerX + 35f * scale * cos(baseAngle) + tentacleWave
            val endY = centerY + 35f * scale * sin(baseAngle) + tentacleWave
            
            drawLine(
                color = BOSS_PRIMARY,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 4f
            )
            drawCircle(
                color = BOSS_ACCENT,
                radius = 4f * scale,
                center = Offset(endX, endY)
            )
        }
        
        // Health indicator (mini health bar)
        if (health > 0) {
            val barWidth = 40f * scale
            val barHeight = 4f * scale
            val healthRatio = health / 3f  // Boss has 3 health
            
            drawRect(
                color = Color.DarkGray,
                topLeft = Offset(centerX - barWidth / 2, centerY + 30f * scale),
                size = Size(barWidth, barHeight)
            )
            drawRect(
                color = when {
                    healthRatio > 0.6f -> Color.Green
                    healthRatio > 0.3f -> BOSS_ACCENT
                    else -> BOSS_PRIMARY
                },
                topLeft = Offset(centerX - barWidth / 2, centerY + 30f * scale),
                size = Size(barWidth * healthRatio, barHeight)
            )
        }
    }
    
    // ===== Player Bullet =====
    fun DrawScope.drawPlayerBullet(position: Offset, time: Long) {
        val glow = 0.7f + 0.3f * sin(time * 0.03f).toFloat()
        
        // Glow effect
        drawOval(
            color = BULLET_PLAYER.copy(alpha = 0.3f * glow),
            topLeft = Offset(position.x - 8f, position.y - 18f),
            size = Size(16f, 36f)
        )
        
        // Main bullet (elongated oval)
        drawOval(
            color = BULLET_PLAYER,
            topLeft = Offset(position.x - 4f, position.y - 12f),
            size = Size(8f, 24f)
        )
        
        // Bright core
        drawOval(
            color = Color.White,
            topLeft = Offset(position.x - 2f, position.y - 8f),
            size = Size(4f, 12f)
        )
    }
    
    // ===== Enemy Bullet =====
    fun DrawScope.drawEnemyBullet(position: Offset, time: Long) {
        val pulse = 0.8f + 0.2f * sin(time * 0.04f).toFloat()
        
        // Glow
        drawCircle(
            color = BULLET_ENEMY.copy(alpha = 0.4f * pulse),
            radius = 12f,
            center = position
        )
        
        // Main bullet (teardrop shape)
        val bulletPath = Path().apply {
            moveTo(position.x, position.y - 8f)
            lineTo(position.x - 6f, position.y + 2f)
            lineTo(position.x, position.y + 10f)
            lineTo(position.x + 6f, position.y + 2f)
            close()
        }
        drawPath(bulletPath, BULLET_ENEMY, style = Fill)
        
        // Bright core
        drawCircle(
            color = Color.White,
            radius = 3f,
            center = Offset(position.x, position.y)
        )
    }
    
    // ===== Background Star =====
    fun DrawScope.drawStar(star: Star) {
        // Twinkle effect based on brightness
        drawCircle(
            color = Color.White.copy(alpha = star.brightness * 0.8f),
            radius = star.size * 1.5f,
            center = star.position
        )
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
        
        // Multiple concentric circles (explosion rings)
        for (i in 0 until 3) {
            val ringProgress = (progress + i * 0.15f).coerceIn(0f, 1f)
            val ringRadius = maxRadius * ringProgress
            val ringAlpha = alpha * (1f - i * 0.25f)
            
            val ringColor = when (i) {
                0 -> Color(0xFFFFFFFF)  // White core
                1 -> Color(0xFFFFEE00)  // Yellow
                else -> explosion.color  // Orange outer
            }
            
            drawCircle(
                color = ringColor.copy(alpha = ringAlpha * 0.8f),
                radius = ringRadius,
                center = explosion.position
            )
        }
        
        // Spark particles
        val sparkCount = 8
        for (i in 0 until sparkCount) {
            val angle = (2 * Math.PI * i / sparkCount + progress * 2).toFloat()
            val sparkDistance = maxRadius * progress * 1.2f
            val sparkX = explosion.position.x + sparkDistance * cos(angle)
            val sparkY = explosion.position.y + sparkDistance * sin(angle)
            
            drawCircle(
                color = Color(0xFFFFEE00).copy(alpha = alpha * 0.9f),
                radius = 4f * (1f - progress),
                center = Offset(sparkX, sparkY)
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
        
        val alpha = 1f - progress
        val size = particle.size * (1f - progress * 0.5f)
        
        // Glow
        drawCircle(
            color = particle.color.copy(alpha = alpha * 0.4f),
            radius = size * 2f,
            center = particle.position
        )
        
        // Core
        drawCircle(
            color = particle.color.copy(alpha = alpha),
            radius = size,
            center = particle.position
        )
    }
    
    // ===== Muzzle Flash =====
    fun DrawScope.drawMuzzleFlash(
        position: Offset,
        time: Long
    ) {
        val flashDuration = 100  // 100ms
        val flashPhase = (time % flashDuration).toFloat() / flashDuration
        
        if (flashPhase < 0.5f) {
            val alpha = 1f - flashPhase * 2
            
            // Flash burst
            drawCircle(
                color = Color(0xFFFFFFFF).copy(alpha = alpha * 0.8f),
                radius = 15f,
                center = position
            )
            drawCircle(
                color = Color(0xFFFFEE00).copy(alpha = alpha * 0.6f),
                radius = 25f,
                center = position
            )
        }
    }
}
