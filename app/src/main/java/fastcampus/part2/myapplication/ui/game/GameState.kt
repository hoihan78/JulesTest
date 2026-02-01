package fastcampus.part2.myapplication.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

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

data class Player(
    val position: Offset,
    val isInvincible: Boolean = false,  // 피격 후 무적 상태
    val invincibleUntil: Long = 0L      // 무적 종료 시간
)

data class Enemy(
    val position: Offset,
    val type: EnemyType = EnemyType.BEE,
    val health: Int = 1,
    val lastShotTime: Long = 0L,
    val animationPhase: Float = 0f  // 애니메이션 페이즈 (날개 펄럭임 등)
)

data class Bullet(val position: Offset)

// 적군 총알
data class EnemyBullet(
    val position: Offset,
    val velocity: Offset  // 플레이어 방향으로의 속도
)

// 배경 별
data class Star(
    val position: Offset,
    val speed: Float,       // 스크롤 속도 (레이어별 차이)
    val size: Float,        // 별 크기
    val brightness: Float   // 밝기 (0.0 ~ 1.0)
)

// 폭발 효과
data class Explosion(
    val position: Offset,
    val startTime: Long,
    val duration: Int = 300,  // ms
    val color: Color = Color(0xFFFF6B35)  // 기본 주황색
)

// 파티클
data class Particle(
    val position: Offset,
    val velocity: Offset,
    val color: Color,
    val size: Float = 4f,
    val lifetime: Int,      // ms
    val createdAt: Long
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
    val formationPattern: FormationPattern, // 현재 포메이션 패턴
    // Phase 2: Visual enhancements
    val stars: List<Star> = emptyList(),           // 배경 별
    val explosions: List<Explosion> = emptyList(), // 폭발 효과
    val particles: List<Particle> = emptyList(),   // 파티클
    val screenShake: Float = 0f                    // 화면 흔들림 (선택)
)
