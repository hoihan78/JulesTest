package fastcampus.part2.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fastcampus.part2.myapplication.ui.game.GameScreen
import fastcampus.part2.myapplication.ui.name.NameInputScreen
import fastcampus.part2.myapplication.ui.ranking.RankingScreen

@Composable
fun Navigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "name_input") {
        composable("name_input") {
            NameInputScreen(
                onNavigateToGame = { playerName ->
                    navController.navigate("game/$playerName")
                }
            )
        }
        composable(
            "game/{playerName}",
            arguments = listOf(navArgument("playerName") { type = NavType.StringType })
        ) { backStackEntry ->
            val playerName = backStackEntry.arguments?.getString("playerName") ?: ""
            GameScreen(
                playerName = playerName,
                onNavigateToRanking = { score ->
                    navController.navigate("ranking/$playerName/$score") {
                        popUpTo("name_input")
                    }
                }
            )
        }
        composable(
            "ranking/{playerName}/{score}",
            arguments = listOf(
                navArgument("playerName") { type = NavType.StringType },
                navArgument("score") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val playerName = backStackEntry.arguments?.getString("playerName") ?: ""
            val score = backStackEntry.arguments?.getInt("score") ?: 0
            RankingScreen(
                playerName = playerName,
                score = score,
                onNavigateToNameInput = {
                    navController.navigate("name_input") {
                        popUpTo("name_input") {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}