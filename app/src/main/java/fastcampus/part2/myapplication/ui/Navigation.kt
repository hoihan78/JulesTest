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
import fastcampus.part2.myapplication.ui.settings.SettingsScreen
import fastcampus.part2.myapplication.ui.title.TitleScreen

/**
 * Navigation routes for the game
 */
sealed class Screen(val route: String) {
    data object Title : Screen("title")
    data object NameInput : Screen("name_input")
    data object Game : Screen("game/{playerName}") {
        fun createRoute(playerName: String) = "game/$playerName"
    }
    data object Settings : Screen("settings")
    data object Ranking : Screen("ranking")
    data object RankingWithScore : Screen("ranking/{playerName}/{score}/{wave}") {
        fun createRoute(playerName: String, score: Int, wave: Int) = "ranking/$playerName/$score/$wave"
    }
}

@Composable
fun Navigation() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = Screen.Title.route) {
        
        // Title Screen (new start destination)
        composable(Screen.Title.route) {
            TitleScreen(
                onStartGame = {
                    navController.navigate(Screen.NameInput.route)
                },
                onSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onRanking = {
                    navController.navigate(Screen.Ranking.route)
                }
            )
        }
        
        // Name Input Screen
        composable(Screen.NameInput.route) {
            NameInputScreen(
                onNavigateToGame = { playerName ->
                    navController.navigate(Screen.Game.createRoute(playerName))
                }
            )
        }
        
        // Game Screen
        composable(
            route = Screen.Game.route,
            arguments = listOf(navArgument("playerName") { type = NavType.StringType })
        ) { backStackEntry ->
            val playerName = backStackEntry.arguments?.getString("playerName") ?: ""
            GameScreen(
                playerName = playerName,
                onNavigateToRanking = { score, wave ->
                    navController.navigate(Screen.RankingWithScore.createRoute(playerName, score, wave)) {
                        popUpTo(Screen.Title.route)
                    }
                },
                onNavigateToTitle = {
                    navController.navigate(Screen.Title.route) {
                        popUpTo(Screen.Title.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Settings Screen
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Ranking Screen (from title - view only)
        composable(Screen.Ranking.route) {
            RankingScreen(
                playerName = null,
                currentScore = null,
                currentWave = null,
                onNavigateToTitle = {
                    navController.navigate(Screen.Title.route) {
                        popUpTo(Screen.Title.route) { inclusive = true }
                    }
                },
                onNavigateToNameInput = null
            )
        }
        
        // Ranking Screen (after game - with score)
        composable(
            route = Screen.RankingWithScore.route,
            arguments = listOf(
                navArgument("playerName") { type = NavType.StringType },
                navArgument("score") { type = NavType.IntType },
                navArgument("wave") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val playerName = backStackEntry.arguments?.getString("playerName") ?: ""
            val score = backStackEntry.arguments?.getInt("score") ?: 0
            val wave = backStackEntry.arguments?.getInt("wave") ?: 1
            
            RankingScreen(
                playerName = playerName,
                currentScore = score,
                currentWave = wave,
                onNavigateToTitle = {
                    navController.navigate(Screen.Title.route) {
                        popUpTo(Screen.Title.route) { inclusive = true }
                    }
                },
                onNavigateToNameInput = {
                    navController.navigate(Screen.NameInput.route) {
                        popUpTo(Screen.Title.route)
                    }
                }
            )
        }
    }
}
