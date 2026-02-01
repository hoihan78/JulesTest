package fastcampus.part2.myapplication.ui.title

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin
import kotlin.random.Random

// Neon colors
private val neonCyan = Color(0xFF00D9FF)
private val neonGreen = Color(0xFF39FF14)
private val neonYellow = Color(0xFFFFEE00)
private val neonPink = Color(0xFFFF00FF)

// Star data class for title screen
private data class TitleStar(
    val x: Float,
    val y: Float,
    val size: Float,
    val brightness: Float,
    val twinkleOffset: Float
)

@Composable
fun TitleScreen(
    onStartGame: () -> Unit,
    onSettings: () -> Unit,
    onRanking: () -> Unit
) {
    // Create stars for background
    val stars = remember {
        List(100) {
            TitleStar(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = 1f + Random.nextFloat() * 2f,
                brightness = 0.3f + Random.nextFloat() * 0.7f,
                twinkleOffset = Random.nextFloat() * 2f * Math.PI.toFloat()
            )
        }
    }
    
    // Animation for twinkling stars and title glow
    val infiniteTransition = rememberInfiniteTransition(label = "title_animation")
    
    val twinklePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "twinkle"
    )
    
    val titleGlow by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF000000),
                        Color(0xFF0A0E27),
                        Color(0xFF0D1B2A)
                    )
                )
            )
    ) {
        // Starfield background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            stars.forEach { star ->
                val twinkleBrightness = star.brightness * 
                    (0.5f + 0.5f * sin(twinklePhase + star.twinkleOffset))
                
                drawCircle(
                    color = Color.White.copy(alpha = twinkleBrightness),
                    radius = star.size,
                    center = Offset(star.x * canvasWidth, star.y * canvasHeight)
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(0.3f))
            
            // Title: GALAGA
            Text(
                text = "GALAGA",
                style = TextStyle(
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    brush = Brush.linearGradient(
                        colors = listOf(neonCyan, neonPink, neonCyan)
                    ),
                    shadow = Shadow(
                        color = neonCyan.copy(alpha = titleGlow),
                        offset = Offset(0f, 0f),
                        blurRadius = 30f * titleGlow
                    )
                )
            )
            
            // Subtitle
            Text(
                text = "Compose Edition",
                style = TextStyle(
                    color = neonGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    shadow = Shadow(
                        color = neonGreen.copy(alpha = 0.5f),
                        offset = Offset(0f, 0f),
                        blurRadius = 10f
                    )
                )
            )
            
            Spacer(modifier = Modifier.weight(0.3f))
            
            // Menu buttons
            MenuButton(
                text = "게임 시작",
                color = neonCyan,
                onClick = onStartGame
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            MenuButton(
                text = "설정",
                color = neonGreen,
                onClick = onSettings
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            MenuButton(
                text = "랭킹",
                color = neonYellow,
                onClick = onRanking
            )
            
            Spacer(modifier = Modifier.weight(0.4f))
            
            // Footer
            Text(
                text = "© 2026 Galaga Compose",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun MenuButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(220.dp)
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 2.dp,
            brush = Brush.linearGradient(listOf(color, color.copy(alpha = 0.5f)))
        )
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(
                    color = color.copy(alpha = 0.7f),
                    offset = Offset(0f, 0f),
                    blurRadius = 10f
                )
            )
        )
    }
}
