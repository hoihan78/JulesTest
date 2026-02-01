package fastcampus.part2.myapplication.ui.ranking

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fastcampus.part2.myapplication.data.ScoreEntry
import fastcampus.part2.myapplication.data.ScoreRepository

// Neon colors
private val neonCyan = Color(0xFF00D9FF)
private val neonGreen = Color(0xFF39FF14)
private val neonYellow = Color(0xFFFFEE00)
private val neonGold = Color(0xFFFFD700)
private val neonSilver = Color(0xFFC0C0C0)
private val neonBronze = Color(0xFFCD7F32)

@Composable
fun RankingScreen(
    playerName: String? = null,
    currentScore: Int? = null,
    currentWave: Int? = null,
    onNavigateToTitle: () -> Unit,
    onNavigateToNameInput: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scoreRepository = remember { ScoreRepository(context) }
    val topScores by scoreRepository.topScores.collectAsState(initial = emptyList())
    
    // Save current score if provided
    LaunchedEffect(playerName, currentScore, currentWave) {
        if (playerName != null && currentScore != null) {
            scoreRepository.saveScore(
                ScoreEntry(
                    playerName = playerName,
                    score = currentScore,
                    wave = currentWave ?: 1
                )
            )
        }
    }
    
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateToTitle) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                
                Text(
                    text = "최고 기록",
                    style = TextStyle(
                        color = neonYellow,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(
                            color = neonYellow.copy(alpha = 0.7f),
                            offset = Offset(0f, 0f),
                            blurRadius = 15f
                        )
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Current score display (if applicable)
            if (currentScore != null && playerName != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    neonCyan.copy(alpha = 0.2f),
                                    neonGreen.copy(alpha = 0.2f)
                                )
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Your Score",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "$currentScore",
                            style = TextStyle(
                                color = neonCyan,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                shadow = Shadow(
                                    color = neonCyan.copy(alpha = 0.7f),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 15f
                                )
                            )
                        )
                        if (currentWave != null) {
                            Text(
                                text = "Wave $currentWave",
                                color = neonGreen,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Leaderboard header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "#",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Player",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Score",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(80.dp)
                )
                Text(
                    text = "Wave",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(50.dp)
                )
            }
            
            // Scores list
            if (topScores.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No scores yet!\nBe the first to play!",
                        color = Color.Gray,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(topScores) { index, entry ->
                        RankingRow(
                            rank = index + 1,
                            entry = entry,
                            isCurrentPlayer = entry.playerName == playerName && entry.score == currentScore
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (onNavigateToNameInput != null) {
                    Button(
                        onClick = onNavigateToNameInput,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = neonGreen
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "다시 플레이",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                
                Button(
                    onClick = onNavigateToTitle,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = neonCyan
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "메인 메뉴",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RankingRow(
    rank: Int,
    entry: ScoreEntry,
    isCurrentPlayer: Boolean
) {
    val rankColor = when (rank) {
        1 -> neonGold
        2 -> neonSilver
        3 -> neonBronze
        else -> Color.White
    }
    
    val backgroundColor = if (isCurrentPlayer) {
        neonCyan.copy(alpha = 0.15f)
    } else {
        Color.White.copy(alpha = 0.03f)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank badge
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (rank <= 3) rankColor.copy(alpha = 0.2f) else Color.Transparent,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                style = TextStyle(
                    color = rankColor,
                    fontSize = 16.sp,
                    fontWeight = if (rank <= 3) FontWeight.Bold else FontWeight.Normal,
                    shadow = if (rank <= 3) Shadow(
                        color = rankColor.copy(alpha = 0.5f),
                        blurRadius = 5f
                    ) else null
                )
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Player name
        Text(
            text = entry.playerName,
            color = if (isCurrentPlayer) neonCyan else Color.White,
            fontSize = 16.sp,
            fontWeight = if (isCurrentPlayer) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        
        // Score
        Text(
            text = "${entry.score}",
            style = TextStyle(
                color = if (rank <= 3) rankColor else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                shadow = if (rank <= 3) Shadow(
                    color = rankColor.copy(alpha = 0.3f),
                    blurRadius = 5f
                ) else null
            ),
            textAlign = TextAlign.End,
            modifier = Modifier.width(80.dp)
        )
        
        // Wave
        Text(
            text = "${entry.wave}",
            color = neonGreen,
            fontSize = 14.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.width(50.dp)
        )
    }
}

// Backward compatible composable for existing navigation
@Composable
fun RankingScreen(
    playerName: String,
    score: Int,
    onNavigateToNameInput: () -> Unit
) {
    RankingScreen(
        playerName = playerName,
        currentScore = score,
        currentWave = null,
        onNavigateToTitle = onNavigateToNameInput,
        onNavigateToNameInput = onNavigateToNameInput
    )
}
