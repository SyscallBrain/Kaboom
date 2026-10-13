package pt.tiago.kaboom.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import pt.tiago.kaboom.game.GameEvent
import java.io.File

/** Som e vibração para os eventos do jogo. */
class Feedback(context: Context) {
    var soundOn = true

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
        if (soundOn) sounds[e]?.let { pool.play(it, 1f, 1f, 1, 0, 1f) }
        val ms = when (e) {
            GameEvent.EXPLOSION -> 40L
            GameEvent.PLAYER_DIED -> 300L
            else -> return
        }
        vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun release() = pool.release()
}
