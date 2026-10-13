package pt.tiago.kaboom.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import pt.tiago.kaboom.game.GameEvent
import pt.tiago.kaboom.ui.Level
import java.io.File

/** Som e vibração para os eventos do jogo. */
class Feedback(context: Context) {
    var volume = 0.8f
    var vibration = Level.MEDIUM

    private val pool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val sounds = HashMap<GameEvent, Int>()

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    init {
        // O SoundPool só carrega ficheiros: os sons gerados vão para a cache como WAV.
        val dir = File(context.cacheDir, "sfx").apply { mkdirs() }
        for (e in GameEvent.entries) {
            val f = File(dir, "${e.name.lowercase()}.wav")
            Synth.writeWav(f, Synth.forEvent(e))
            sounds[e] = pool.load(f.path, 1)
        }
    }

    fun on(e: GameEvent) {
        playSound(e)
        vibrate(e)
    }

    /** Para as definições: ouvir o volume escolhido. */
    fun previewSound() = playSound(GameEvent.ITEM)

    /** Para as definições: sentir a força de vibração escolhida. */
    fun previewVibration() = vibrate(GameEvent.EXPLOSION)

    private fun playSound(e: GameEvent) {
        if (volume > 0f) sounds[e]?.let { pool.play(it, volume, volume, 1, 0, 1f) }
    }

    private fun vibrate(e: GameEvent) {
        val v = vibrator ?: return
        if (vibration == Level.OFF) return
        if (e == GameEvent.BOMB_PLACED) {
            // Um "clique" curto ao pôr a bomba, para se sentir o botão
            if (Build.VERSION.SDK_INT >= 29) v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            return
        }
        val base = when (e) {
            GameEvent.EXPLOSION -> 50L
            GameEvent.PLAYER_DIED -> 300L
            GameEvent.SHIELD_BREAK -> 120L
            else -> return
        }
        // Força = amplitude (se o telemóvel a suportar) e duração.
        val (amp, scale) = when (vibration) {
            Level.LOW -> 70 to 0.7f
            Level.MEDIUM -> 160 to 1f
            else -> 255 to 1.6f
        }
        val amplitude = if (v.hasAmplitudeControl()) amp else VibrationEffect.DEFAULT_AMPLITUDE
        v.vibrate(VibrationEffect.createOneShot((base * scale).toLong(), amplitude))
    }

    fun release() = pool.release()
}
