package pt.tiago.kaboom

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import pt.tiago.kaboom.audio.Feedback
import pt.tiago.kaboom.ui.ControlScheme
import pt.tiago.kaboom.ui.GameScreen
import pt.tiago.kaboom.ui.TitleScreen

class MainActivity : ComponentActivity() {
    private lateinit var feedback: Feedback

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()

        val prefs = getSharedPreferences("kaboom", MODE_PRIVATE)
        feedback = Feedback(this).apply { soundOn = prefs.getBoolean("sound", true) }

        setContent {
            var playing by remember { mutableStateOf(false) }
            var best by remember { mutableIntStateOf(prefs.getInt("best", 0)) }
            var bestLevel by remember { mutableIntStateOf(prefs.getInt("bestLevel", 0)) }
            var scheme by remember {
                mutableStateOf(
                    ControlScheme.entries.firstOrNull { it.name == prefs.getString("controls", null) }
                        ?: ControlScheme.DPAD,
                )
            }
            val changeScheme = { c: ControlScheme ->
                scheme = c
                prefs.edit { putString("controls", c.name) }
            }

            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFF97316))) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A))
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                ) {
                    if (playing) {
                        GameScreen(
                            feedback = feedback,
                            scheme = scheme,
                            onSchemeChange = changeScheme,
                            onGameOver = { score, level ->
                                if (score > best) best = score
                                if (level > bestLevel) bestLevel = level
                                prefs.edit {
                                    putInt("best", best)
                                    putInt("bestLevel", bestLevel)
                                }
                            },
                            onExit = {
                                prefs.edit { putBoolean("sound", feedback.soundOn) }
                                playing = false
                            },
                        )
                    } else {
                        TitleScreen(best, bestLevel, scheme, changeScheme, onPlay = { playing = true })
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onPause() {
        super.onPause()
        getSharedPreferences("kaboom", MODE_PRIVATE).edit { putBoolean("sound", feedback.soundOn) }
    }

    override fun onDestroy() {
        feedback.release()
        super.onDestroy()
    }

    /** Ecrã inteiro: as barras do sistema só aparecem ao deslizar da borda. */
    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).run {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
