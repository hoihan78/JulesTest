package fastcampus.part2.myapplication.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * SoundManager handles all game audio including BGM and SFX.
 * Safely handles missing audio files with try-catch.
 */
class SoundManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SoundManager"
        
        // Sound effect names
        const val SFX_SHOOT = "shoot"
        const val SFX_EXPLOSION = "explosion"
        const val SFX_HIT = "hit"
        const val SFX_GAMEOVER = "gameover"
        const val SFX_LEVELUP = "levelup"
    }
    
    private var bgmPlayer: MediaPlayer? = null
    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<String, Int>()
    private val loadedSounds = mutableSetOf<String>()
    
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    
    var isSoundEnabled = true
    var isMusicEnabled = true
    var isVibrationEnabled = true
    
    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(attributes)
            .build()
        
        loadSounds()
    }
    
    private fun loadSounds() {
        // Try to load each sound effect, silently fail if not present
        tryLoadSound(SFX_SHOOT, "sfx_shoot")
        tryLoadSound(SFX_EXPLOSION, "sfx_explosion")
        tryLoadSound(SFX_HIT, "sfx_hit")
        tryLoadSound(SFX_GAMEOVER, "sfx_gameover")
        tryLoadSound(SFX_LEVELUP, "sfx_levelup")
    }
    
    private fun tryLoadSound(name: String, resourceName: String) {
        try {
            val resId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
            if (resId != 0) {
                val soundId = soundPool.load(context, resId, 1)
                soundMap[name] = soundId
                loadedSounds.add(name)
                Log.d(TAG, "Loaded sound: $name")
            } else {
                Log.d(TAG, "Sound resource not found: $resourceName")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load sound $name: ${e.message}")
        }
    }
    
    /**
     * Start playing background music (looped)
     */
    fun playBGM() {
        if (!isMusicEnabled) return
        
        try {
            val resId = context.resources.getIdentifier("bgm_game", "raw", context.packageName)
            if (resId == 0) {
                Log.d(TAG, "BGM resource not found")
                return
            }
            
            stopBGM()
            bgmPlayer = MediaPlayer.create(context, resId)?.apply {
                isLooping = true
                setVolume(0.5f, 0.5f)
                start()
            }
            Log.d(TAG, "BGM started")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play BGM: ${e.message}")
        }
    }
    
    /**
     * Stop background music
     */
    fun stopBGM() {
        try {
            bgmPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            bgmPlayer = null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop BGM: ${e.message}")
        }
    }
    
    /**
     * Pause background music
     */
    fun pauseBGM() {
        try {
            bgmPlayer?.pause()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to pause BGM: ${e.message}")
        }
    }
    
    /**
     * Resume background music
     */
    fun resumeBGM() {
        if (!isMusicEnabled) return
        try {
            bgmPlayer?.start()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resume BGM: ${e.message}")
        }
    }
    
    /**
     * Play a sound effect by name
     */
    fun playSound(soundName: String) {
        if (!isSoundEnabled) return
        
        val soundId = soundMap[soundName]
        if (soundId != null) {
            try {
                soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to play sound $soundName: ${e.message}")
            }
        }
    }
    
    /**
     * Trigger haptic feedback for game events
     */
    fun vibrate(type: VibrationType) {
        if (!isVibrationEnabled) return
        
        try {
            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = when (type) {
                        VibrationType.SHOOT -> VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
                        VibrationType.HIT -> VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                        VibrationType.EXPLOSION -> VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                        VibrationType.GAMEOVER -> VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100, 50, 200), -1)
                    }
                    v.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    val duration = when (type) {
                        VibrationType.SHOOT -> 20L
                        VibrationType.HIT -> 100L
                        VibrationType.EXPLOSION -> 50L
                        VibrationType.GAMEOVER -> 500L
                    }
                    v.vibrate(duration)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to vibrate: ${e.message}")
        }
    }
    
    /**
     * Update settings from preferences
     */
    fun updateSettings(soundEnabled: Boolean, musicEnabled: Boolean, vibrationEnabled: Boolean) {
        isSoundEnabled = soundEnabled
        isVibrationEnabled = vibrationEnabled
        
        if (isMusicEnabled != musicEnabled) {
            isMusicEnabled = musicEnabled
            if (musicEnabled) {
                resumeBGM()
            } else {
                pauseBGM()
            }
        }
    }
    
    /**
     * Release all audio resources
     */
    fun release() {
        try {
            stopBGM()
            soundPool.release()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release resources: ${e.message}")
        }
    }
    
    enum class VibrationType {
        SHOOT,
        HIT,
        EXPLOSION,
        GAMEOVER
    }
}
