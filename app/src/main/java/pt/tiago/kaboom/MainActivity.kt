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
import androidx.activity.compose.BackHandler
import pt.tiago.kaboom.ui.GameScreen
import pt.tiago.kaboom.ui.LevelSelectScreen
import pt.tiago.kaboom.ui.Settings
import pt.tiago.kaboom.ui.SettingsScreen
import pt.tiago.kaboom.ui.TitleScreen
import pt.tiago.kaboom.ui.Ui

private enum class Screen { TITLE, SELECT, GAME }

class MainActivity : ComponentActivity() {
    private lateinit var feedback: Feedback

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()

        val prefs = getSharedPreferences("kaboom", MODE_PRIVATE)
        feedback = Feedback(this)

        setContent {
            var screen by remember { mutableStateOf(Screen.TITLE) }
            var startLevel by remember { mutableIntStateOf(1) }
            // Nível mais alto que se pode escolher (Levels.count + 1 = modo infinito)
            var unlocked by remember { mutableIntStateOf(prefs.getInt("unlocked", 1)) }
            var best by remember { mutableIntStateOf(prefs.getInt("best", 0)) }
            var bestLevel by remember { mutableIntStateOf(prefs.getInt("bestLevel", 0)) }
            var showSettings by remember { mutableStateOf(false) }
            var settings by remember { mutableStateOf(Settings.load(prefs)) }
            feedback.volume = settings.volume
            feedback.vibration = settings.vibration
            val changeSettings = { s: Settings ->
                settings = s
                s.save(prefs)
            }

            MaterialTheme(colorScheme = darkColorScheme(primary = Ui.Orange, surface = Ui.Panel)) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Ui.Night)
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                ) {
                    when (screen) {
                        Screen.GAME -> GameScreen(
                            startLevel = startLevel,
                            feedback = feedback,
                            settings = settings,
                            onSettingsChange = changeSettings,
                            onGameOver = { score, level ->
                                if (score > best) best = score
                                if (level > bestLevel) bestLevel = level
                                prefs.edit {
                                    putInt("best", best)
                                    putInt("bestLevel", bestLevel)
                                }
                            },
                            onLevelCleared = { level ->
                                if (level + 1 > unlocked) {
                                    unlocked = level + 1
                                    prefs.edit { putInt("unlocked", unlocked) }
                                }
                            },
                            onExit = { screen = Screen.SELECT },
                        )
                        Screen.SELECT -> {
                            BackHandler { screen = Screen.TITLE }
                            LevelSelectScreen(
                                unlocked = unlocked,
                                onPick = { n ->
                                    startLevel = n
                                    screen = Screen.GAME
                                },
                                onBack = { screen = Screen.TITLE },
                            )
                        }
                        Screen.TITLE -> TitleScreen(best, bestLevel, onSettings = { showSettings = true }, onPlay = { screen = Screen.SELECT })
                    }
                    if (screen != Screen.GAME) {
                        if (showSettings) {
                            BackHandler { showSettings = false }
                            SettingsScreen(
                                settings = settings,
                                onChange = changeSettings,
                                onPreviewSound = feedback::previewSound,
                                onPreviewVibration = feedback::previewVibration,
                                onBack = { showSettings = false },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
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
