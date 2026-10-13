package pt.tiago.kaboom.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors

/**
 * Toca a música de fundo em ciclo sem cortes (AudioTrack estático com pontos de ciclo).
 * As músicas são geradas pelo [MusicSynth] na primeira vez que são precisas, numa
 * thread própria — o jogo nunca espera por isso — e ficam em memória.
 */
class MusicPlayer {
    private val worker = Executors.newSingleThreadExecutor()
    private val cache = HashMap<Song, ShortArray>()
    private var track: AudioTrack? = null
    private var playing: Song? = null

    @Volatile private var wanted: Song? = null
    @Volatile private var volume = 0.6f
    @Volatile private var ducked = false
    @Volatile private var suspended = false

    /** Muda de música (null = silêncio). Pedir a que já está a tocar não faz nada. */
    fun play(song: Song?) {
        if (song == wanted) return
        wanted = song
        worker.execute { safely { apply() } }
    }

    /** A música é um extra: se o áudio falhar neste telemóvel, o jogo continua em silêncio. */
    private inline fun safely(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            track = null
            playing = null
        }
    }

    fun setVolume(v: Float) {
        volume = v
        worker.execute { safely { applyVolume() } }
    }

    /** Mais baixinho enquanto o jogo está em pausa. */
    fun duck(on: Boolean) {
        ducked = on
        worker.execute { safely { applyVolume() } }
    }

    /** A app foi para segundo plano. */
    fun suspend() {
        suspended = true
        worker.execute { safely { track?.pause() } }
    }

    fun resume() {
        suspended = false
        worker.execute { safely { if (playing != null) track?.play() } }
    }

    fun release() {
        worker.execute {
            track?.release()
            track = null
        }
        worker.shutdown()
    }

    private fun applyVolume() {
        track?.setVolume(volume * if (ducked) 0.35f else 1f)
    }

    /** Corre sempre na thread do [worker]: só ela mexe no AudioTrack. */
    private fun apply() {
        val song = wanted
        if (song == playing) return
        track?.run {
            stop()
            release()
        }
        track = null
        playing = null
        if (song == null) return
        val pcm = cache.getOrPut(song) { MusicSynth.render(song.def) }
        // Entretanto já pediram outra música? Então esta já não interessa.
        if (wanted != song) {
            apply()
            return
        }
        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(MusicSynth.RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        t.write(pcm, 0, pcm.size)
        t.setLoopPoints(0, pcm.size, -1)
        track = t
        playing = song
        applyVolume()
        if (!suspended) t.play()
    }
}
