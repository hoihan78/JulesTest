package fastcampus.part2.myapplication.ui.game

import androidx.compose.ui.geometry.Offset

// 적 종류 enum
enum class EnemyType(val points: Int, val health: Int) {
    BEE(10, 1),        // 일반 적 (벌)
    BUTTERFLY(20, 2),   // 중형 적 (나비)
    BOSS(50, 3)         // 보스 적
}

// 포메이션 패턴
enum class FormationPattern {
    V_SHAPE,        // V자 대형
    INVERTED_V,     // 역V자 대형
    GRID            // 격자형
}

data class Player(val position: Offset)

data class Enemy(
    val position: Offset,
    val type: EnemyType = EnemyType.BEE,
    val health: Int = 1,
    val lastShotTime: Long = 0L
)

data class Bullet(val position: Offset)

// 적군 총알
data class EnemyBullet(
    val position: Offset,
    val velocity: Offset  // 플레이어 방향으로의 속도
)

data class GameState(
    val player: Player,
    val enemies: List<Enemy>,
    val bullets: List<Bullet>,
    val enemyBullets: List<EnemyBullet>,  // 적군 총알 리스트
    val score: Int,
    val lives: Int,
    val isGameOver: Boolean,
    val currentWave: Int,                  // 현재 웨이브
    val isPaused: Boolean,                 // 일시정지 상태
    val formationPattern: FormationPattern // 현재 포메이션 패턴
)
