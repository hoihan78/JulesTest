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

// 파워업 종류
enum class PowerUpType {
    DOUBLE_SHOT,    // 더블샷
    TRIPLE_SHOT,    // 트리플샷
    SHIELD,         // 보호막
    SPEED_UP,       // 속도 증가
    EXTRA_LIFE      // 생명 추가
}

// 보스 공격 패턴
enum class BossAttackPattern {
    CIRCLE_SHOT,     // 원형 탄막
    SPIRAL_SHOT,     // 나선형 탄막
    LASER_BEAM,      // 레이저 빔
    DIVE_ATTACK      // 급강하 공격
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

// 파워업 아이템
data class PowerUp(
    val position: Offset,
    val type: PowerUpType,
    val velocity: Offset = Offset(0f, 3f),
    val createdAt: Long = System.currentTimeMillis()
)

// 스테이지 보스
data class StageBoss(
    val position: Offset,
    val health: Int,
    val maxHealth: Int,
    val phase: Int = 1,
    val attackPattern: BossAttackPattern = BossAttackPattern.CIRCLE_SHOT,
    val lastAttackTime: Long = 0L,
    val lastPatternChangeTime: Long = 0L,
    val animationPhase: Float = 0f,
    val isHit: Boolean = false,  // 피격 플래시용
    val hitTime: Long = 0L,
    val targetPosition: Offset? = null,  // 급강하 공격 타겟
    val isDiving: Boolean = false,
    val originalPosition: Offset? = null,  // 급강하 후 복귀 위치
    val laserActive: Boolean = false,  // 레이저 활성화
    val laserStartTime: Long = 0L,
    val laserAngle: Float = 0f
)

// 보스 총알 (원형/나선형 탄막용)
data class BossBullet(
    val position: Offset,
    val velocity: Offset,
    val color: Color = Color(0xFFFF0055)
)

// 화면 흔들림 정보
data class ScreenShakeInfo(
    val intensity: Float = 0f,
    val startTime: Long = 0L,
    val duration: Int = 0
)

// 콤보 정보
data class ComboInfo(
    val count: Int = 0,
    val lastKillTime: Long = 0L,
    val displayUntil: Long = 0L,
    val multiplier: Float = 1f
)

// 플로팅 텍스트 (콤보, 보너스 점수 표시)
data class FloatingText(
    val text: String,
    val position: Offset,
    val color: Color,
    val createdAt: Long,
    val duration: Int = 1000,
    val fontSize: Int = 24
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
    val screenShake: ScreenShakeInfo = ScreenShakeInfo(),  // 화면 흔들림
    // Phase 4: Power-up system
    val powerUps: List<PowerUp> = emptyList(),             // 파워업 아이템
    val activePowerUps: Map<PowerUpType, Long> = emptyMap(), // 활성 파워업 -> 종료 시간
    val shotLevel: Int = 1,                                 // 1=single, 2=double, 3=triple
    val hasShield: Boolean = false,                         // 실드 활성화
    val playerSpeed: Float = 1.0f,                          // 플레이어 속도 배율
    // Phase 4: Boss battle
    val stageBoss: StageBoss? = null,                       // 스테이지 보스
    val bossBullets: List<BossBullet> = emptyList(),        // 보스 총알
    val isBossWave: Boolean = false,                        // 보스 웨이브인지
    val bossWarning: Boolean = false,                       // 보스 경고 표시
    val bossWarningStartTime: Long = 0L,
    // Phase 4: Auto-fire
    val isAutoFiring: Boolean = false,                      // 자동 발사 모드
    val lastAutoFireTime: Long = 0L,                        // 마지막 자동 발사 시간
    // Phase 4: Combo system
    val combo: ComboInfo = ComboInfo(),                     // 콤보 정보
    val perfectWave: Boolean = true,                        // 이 웨이브에서 피격 없음
    val floatingTexts: List<FloatingText> = emptyList(),   // 플로팅 텍스트
    // High score display
    val highScore: Int = 0
)
