package fastcampus.part2.myapplication.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

// 적 타입별 색상
private fun getEnemyColor(type: EnemyType): Color {
    return when (type) {
        EnemyType.BEE -> Color(0xFFFF5555)        // 빨간색 (벌)
        EnemyType.BUTTERFLY -> Color(0xFFFFEE00)  // 노란색 (나비)
        EnemyType.BOSS -> Color(0xFF00FF88)       // 초록색 (보스)
    }
}

// 적 타입별 크기
private fun getEnemyRadius(type: EnemyType): Float {
    return when (type) {
        EnemyType.BEE -> 18f
        EnemyType.BUTTERFLY -> 22f
        EnemyType.BOSS -> 30f
    }
}

@Composable
fun GameScreen(
    playerName: String,
    onNavigateToRanking: (Int) -> Unit,
    gameViewModel: GameViewModel = viewModel()
) {
    val gameState by gameViewModel.gameState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E27))  // 어두운 우주 배경
    ) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    gameViewModel.movePlayer(dragAmount.x)
                }
            }
        ) {
            // 플레이어 그리기 (파란색 우주선)
            drawCircle(
                color = Color(0xFF00D9FF),
                radius = 25f,
                center = gameState.player.position
            )

            // 적군 그리기 (타입별 색상과 크기)
            gameState.enemies.forEach { enemy ->
                drawCircle(
                    color = getEnemyColor(enemy.type),
                    radius = getEnemyRadius(enemy.type),
                    center = enemy.position
                )
                // 보스는 추가 링 표시
                if (enemy.type == EnemyType.BOSS) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        radius = getEnemyRadius(enemy.type) + 5f,
                        center = enemy.position,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                }
            }

            // 플레이어 총알 그리기 (노란색)
            gameState.bullets.forEach { bullet ->
                drawCircle(
                    color = Color(0xFFFFEE00),
                    radius = 8f,
                    center = bullet.position
                )
            }

            // 적군 총알 그리기 (주황색)
            gameState.enemyBullets.forEach { bullet ->
                drawCircle(
                    color = Color(0xFFFF6B35),
                    radius = 6f,
                    center = bullet.position
                )
            }
        }

        // 상단 UI: 플레이어 정보, 점수, 생명, 웨이브
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = playerName,
                    color = Color(0xFF39FF14),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "SCORE: ${gameState.score}",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "LIVES: ${"❤".repeat(gameState.lives.coerceAtLeast(0))}",
                    color = Color(0xFFFF5555),
                    fontSize = 14.sp
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "WAVE ${gameState.currentWave}",
                    color = Color(0xFF00D9FF),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                // 일시정지 버튼
                IconButton(
                    onClick = { gameViewModel.togglePause() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (gameState.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (gameState.isPaused) "Resume" else "Pause",
                        tint = Color.White
                    )
                }
            }
        }

        // 발사 버튼
        Button(
            onClick = { gameViewModel.shoot() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .size(80.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00D9FF)
            ),
            shape = CircleShape,
            enabled = !gameState.isPaused && !gameState.isGameOver
        ) {
            Text(
                "FIRE",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }

        // 일시정지 오버레이
        if (gameState.isPaused && !gameState.isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "PAUSED",
                        color = Color(0xFF39FF14),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Wave ${gameState.currentWave}",
                        color = Color.White,
                        fontSize = 24.sp
                    )
                    Text(
                        text = "Score: ${gameState.score}",
                        color = Color.White,
                        fontSize = 20.sp
                    )
                    Button(
                        onClick = { gameViewModel.togglePause() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00D9FF)
                        )
                    ) {
                        Text("RESUME", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 게임오버
        if (gameState.isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "GAME OVER",
                        color = Color(0xFFFF5555),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Final Score: ${gameState.score}",
                        color = Color.White,
                        fontSize = 24.sp
                    )
                    Text(
                        text = "Wave Reached: ${gameState.currentWave}",
                        color = Color(0xFF00D9FF),
                        fontSize = 20.sp
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { gameViewModel.restartGame() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00FF88)
                            )
                        ) {
                            Text("RETRY", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onNavigateToRanking(gameState.score) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFEE00)
                            )
                        ) {
                            Text("RANKING", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
