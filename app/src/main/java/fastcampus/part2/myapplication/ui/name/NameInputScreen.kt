package fastcampus.part2.myapplication.ui.name

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NameInputScreen(onNavigateToGame: (String) -> Unit) {
    val playerName = remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("플레이어 이름을 입력하세요")
        OutlinedTextField(
            value = playerName.value,
            onValueChange = { playerName.value = it },
            label = { Text("이름") },
            singleLine = true,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Button(
            onClick = {
                if (playerName.value.isNotBlank()) {
                    onNavigateToGame(playerName.value)
                }
            },
            enabled = playerName.value.isNotBlank()
        ) {
            Text("게임 시작")
        }
    }
}