package fastcampus.part2.myapplication.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fastcampus.part2.myapplication.data.Difficulty
import fastcampus.part2.myapplication.data.SettingsDataStore
import kotlinx.coroutines.launch

// Neon colors
private val neonCyan = Color(0xFF00D9FF)
private val neonGreen = Color(0xFF39FF14)
private val neonYellow = Color(0xFFFFEE00)
private val darkBackground = Color(0xFF0A0E27)

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsDataStore = remember { SettingsDataStore(context) }
    val scope = rememberCoroutineScope()
    
    val isSoundEnabled by settingsDataStore.isSoundEnabled.collectAsState(initial = true)
    val isMusicEnabled by settingsDataStore.isMusicEnabled.collectAsState(initial = true)
    val isVibrationEnabled by settingsDataStore.isVibrationEnabled.collectAsState(initial = true)
    val difficultyName by settingsDataStore.difficulty.collectAsState(initial = Difficulty.NORMAL.name)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF000000),
                        darkBackground,
                        Color(0xFF0D1B2A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header with back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                
                Text(
                    text = "설정",
                    style = TextStyle(
                        color = neonCyan,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(
                            color = neonCyan.copy(alpha = 0.7f),
                            offset = Offset(0f, 0f),
                            blurRadius = 15f
                        )
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Sound section
            SectionTitle("사운드")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SettingsSwitchRow(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                label = "효과음",
                isChecked = isSoundEnabled,
                onCheckedChange = { enabled ->
                    scope.launch { settingsDataStore.setSoundEnabled(enabled) }
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SettingsSwitchRow(
                icon = Icons.Default.MusicNote,
                label = "배경음악",
                isChecked = isMusicEnabled,
                onCheckedChange = { enabled ->
                    scope.launch { settingsDataStore.setMusicEnabled(enabled) }
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SettingsSwitchRow(
                icon = Icons.Default.Vibration,
                label = "진동",
                isChecked = isVibrationEnabled,
                onCheckedChange = { enabled ->
                    scope.launch { settingsDataStore.setVibrationEnabled(enabled) }
                }
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Difficulty section
            SectionTitle("난이도")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.White.copy(alpha = 0.05f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Difficulty.entries.forEach { difficulty ->
                    DifficultyOption(
                        difficulty = difficulty,
                        isSelected = difficultyName == difficulty.name,
                        onSelect = {
                            scope.launch { settingsDataStore.setDifficulty(difficulty.name) }
                        }
                    )
                    
                    if (difficulty != Difficulty.entries.last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Back button
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = neonCyan
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "돌아가기",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = TextStyle(
            color = neonGreen,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            shadow = Shadow(
                color = neonGreen.copy(alpha = 0.5f),
                offset = Offset(0f, 0f),
                blurRadius = 8f
            )
        )
    )
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    label: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onCheckedChange(!isChecked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isChecked) neonCyan else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 16.sp
            )
        }
        
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = neonCyan,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}

@Composable
private fun DifficultyOption(
    difficulty: Difficulty,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = neonYellow,
                unselectedColor = Color.Gray
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = difficulty.displayName,
                color = if (isSelected) neonYellow else Color.White,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = when (difficulty) {
                    Difficulty.EASY -> "적 속도 70%, 발사 빈도 60%"
                    Difficulty.NORMAL -> "기본 난이도"
                    Difficulty.HARD -> "적 속도 130%, 발사 빈도 140%"
                },
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}
