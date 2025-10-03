package fastcampus.part2.myapplication.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GameScreen(
    playerName: String,
    onNavigateToRanking: (Int) -> Unit,
    gameViewModel: GameViewModel = viewModel()
) {
    val gameState by gameViewModel.gameState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    gameViewModel.movePlayer(dragAmount.x)
                }
            }
        ) {
            // 플레이어 그리기
            drawCircle(
                color = Color.Blue,
                radius = 20f,
                center = gameState.player.position
            )

            // 적군 그리기
            gameState.enemies.forEach { enemy ->
                drawCircle(
                    color = Color.Red,
                    radius = 20f,
                    center = enemy.position
                )
            }

            // 총알 그리기
            gameState.bullets.forEach { bullet ->
                drawCircle(
                    color = Color.Yellow,
                    radius = 10f,
                    center = bullet.position
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "플레이어: $playerName", color = Color.White)
            Text(text = "점수: ${gameState.score}", color = Color.White)
            Text(text = "생명: ${gameState.lives}", color = Color.White)
        }

        Button(
            onClick = { gameViewModel.shoot() },
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("발사")
        }

        if (gameState.isGameOver) {
            onNavigateToRanking(gameState.score)
        }
    }
}