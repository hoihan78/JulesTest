package fastcampus.part2.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameView(context: Context, attrs: AttributeSet?) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    interface OnGameOverListener {
        fun onGameOver(score: Int)
    }

    private val thread: GameThread
    private lateinit var player: Player
    private lateinit var playerName: String
    private val bullets = mutableListOf<Bullet>()
    private val enemies = mutableListOf<Enemy>()
    private var score = 0
    private var screenWidth = 0
    private var screenHeight = 0
    private var isGameOver = false

    private val playerPaint = Paint().apply { color = Color.CYAN }
    private val enemyPaint = Paint().apply { color = Color.RED }
    private val bulletPaint = Paint().apply { color = Color.YELLOW }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 50f
        textAlign = Paint.Align.CENTER
    }

    private var lastFrameTime = 0L
    private var onGameOverListener: OnGameOverListener? = null

    init {
        holder.addCallback(this)
        thread = GameThread(holder, this)
        if (context is OnGameOverListener) {
            onGameOverListener = context
        }
    }

    fun setPlayerName(name: String) {
        playerName = name
    }
    override fun surfaceCreated(holder: SurfaceHolder) {
        if (!isGameOver) {
            screenWidth = width
            screenHeight = height

            // Initialize player
            val playerWidth = screenWidth / 10f
            val playerHeight = screenHeight / 20f
            player = Player(
                RectF(
                    (screenWidth / 2f) - (playerWidth / 2f),
                    screenHeight - playerHeight - 20,
                    (screenWidth / 2f) + (playerWidth / 2f),
                    screenHeight - 20
                )
            )

            // Initialize enemies
            createEnemies()

            lastFrameTime = System.currentTimeMillis()
            thread.setRunning(true)
            thread.start()
        }
    }

    private fun createEnemies() {
        enemies.clear()
        val enemyWidth = screenWidth / 15f
        val enemyHeight = screenHeight / 25f
        val padding = enemyWidth / 2f
        for (row in 0 until 4) {
            for (col in 0 until 6) {
                val x = col * (enemyWidth + padding) + padding
                val y = row * (enemyHeight + padding) + padding
                enemies.add(Enemy(RectF(x, y, x + enemyWidth, y + enemyHeight)))
            }
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        var retry = true
        while (retry) {
            try {
                thread.setRunning(false)
                thread.join()
                retry = false
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    fun update() {
        if (isGameOver) return

        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastFrameTime) / 1000.0f
        lastFrameTime = currentTime

        // Update player (movement is handled by onTouchEvent)

        // Update bullets
        val bulletsIterator = bullets.iterator()
        while (bulletsIterator.hasNext()) {
            val bullet = bulletsIterator.next()
            bullet.rect.top += bullet.speed * deltaTime
            bullet.rect.bottom += bullet.speed * deltaTime
            if (bullet.rect.bottom < 0) {
                bulletsIterator.remove()
            }
        }

        // Update enemies
        val enemiesIterator = enemies.iterator()
        while (enemiesIterator.hasNext()) {
            val enemy = enemiesIterator.next()
            enemy.rect.top += enemy.speed * deltaTime
            enemy.rect.bottom += enemy.speed * deltaTime
            if (enemy.rect.top > screenHeight) {
                isGameOver = true
                thread.setRunning(false)
                (context as? Activity)?.runOnUiThread {
                    onGameOverListener?.onGameOver(score)
                }
                return
            }
        }

        // Collision detection
        val bulletsToRemove = mutableListOf<Bullet>()
        val enemiesToRemove = mutableListOf<Enemy>()

        for (bullet in bullets) {
            for (enemy in enemies) {
                if (RectF.intersects(bullet.rect, enemy.rect)) {
                    bulletsToRemove.add(bullet)
                    enemiesToRemove.add(enemy)
                    score += 10
                }
            }
        }

        bullets.removeAll(bulletsToRemove)
        enemies.removeAll(enemiesToRemove)

        if (enemies.isEmpty()) {
            isGameOver = true
            thread.setRunning(false)
            (context as? Activity)?.runOnUiThread {
                onGameOverListener?.onGameOver(score)
            }
        }
    }
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas.drawColor(Color.BLACK)

        if (isGameOver) {
            canvas.drawText("Game Over", screenWidth / 2f, screenHeight / 2f - 50, textPaint)
            canvas.drawText("Score: $score", screenWidth / 2f, screenHeight / 2f + 50, textPaint)
            return
        }

        // Draw player
        canvas.drawRect(player.rect, playerPaint)

        // Draw enemies
        for (enemy in enemies) {
            canvas.drawRect(enemy.rect, enemyPaint)
        }

        // Draw bullets
        for (bullet in bullets) {
            canvas.drawRect(bullet.rect, bulletPaint)
        }

        // Draw score
        val scorePaint = Paint(textPaint).apply { textAlign = Paint.Align.LEFT }
        canvas.drawText("Score: $score", 50f, 100f, scorePaint)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isGameOver) {
            // Restart or go to menu? For now, do nothing.
            return true
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                // Move player
                val playerWidth = player.rect.width()
                player.rect.left = event.x - playerWidth / 2
                player.rect.right = event.x + playerWidth / 2

                // Clamp player position to screen bounds
                if (player.rect.left < 0) {
                    player.rect.left = 0f
                    player.rect.right = playerWidth
                }
                if (player.rect.right > screenWidth) {
                    player.rect.right = screenWidth.toFloat()
                    player.rect.left = screenWidth - playerWidth
                }
            }
            MotionEvent.ACTION_UP -> {
                // Shoot a bullet
                val bulletWidth = 10f
                val bulletHeight = 30f
                val bulletX = player.rect.centerX() - bulletWidth / 2
                val bulletY = player.rect.top - bulletHeight
                bullets.add(Bullet(RectF(bulletX, bulletY, bulletX + bulletWidth, bulletY + bulletHeight)))
            }
        }
        return true
    }
}

class GameThread(private val surfaceHolder: SurfaceHolder, private val gameView: GameView) : Thread() {
    private var running: Boolean = false
    private val targetFPS = 60

    fun setRunning(isRunning: Boolean) {
        this.running = isRunning
    }

    override fun run() {
        var startTime: Long
        var timeMillis: Long
        var waitTime: Long
        val targetTime = (1000 / targetFPS).toLong()

        while (running) {
            startTime = System.nanoTime()
            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                synchronized(surfaceHolder) {
                    gameView.update()
                    if (canvas != null) {
                        gameView.draw(canvas)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
            }

            timeMillis = (System.nanoTime() - startTime) / 1000000
            waitTime = targetTime - timeMillis

            try {
                if (waitTime > 0) {
                    sleep(waitTime)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}