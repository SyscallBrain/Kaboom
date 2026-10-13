package pt.tiago.kaboom.ui

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit

enum class Level(val label: String) {
    OFF("Desligado"),
    LOW("Fraco"),
    MEDIUM("Médio"),
    HIGH("Forte"),
}

data class Settings(
    val volume: Float = 0.8f,
    val vibration: Level = Level.MEDIUM,
    val shake: Level = Level.MEDIUM,
    val scheme: ControlScheme = ControlScheme.FLOATING,
    val leftHanded: Boolean = false,
) {
    companion object {
        fun load(p: SharedPreferences) = Settings(
            volume = p.getFloat("volume", if (p.getBoolean("sound", true)) 0.8f else 0f),
            vibration = enumOr(p.getString("vibration", null), Level.MEDIUM),
            shake = enumOr(p.getString("shake", null), Level.MEDIUM),
            scheme = enumOr(p.getString("controls", null), ControlScheme.FLOATING),
            leftHanded = p.getBoolean("leftHanded", false),
        )

        private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
            enumValues<E>().firstOrNull { it.name == name } ?: default
    }

    fun save(p: SharedPreferences) = p.edit {
        putFloat("volume", volume)
        putString("vibration", vibration.name)
        putString("shake", shake.name)
        putString("controls", scheme.name)
        putBoolean("leftHanded", leftHanded)
    }
}

/**
 * [onPreview] é chamado quando uma opção muda e deve ser "sentida" logo
 * (vibração, volume), para o jogador não ter de ir jogar para experimentar.
 */
@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onPreviewSound: () -> Unit,
    onPreviewVibration: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xF20F172A))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Text("DEFINIÇÕES", color = Color(0xFFFACC15), fontSize = 32.sp, fontWeight = FontWeight.Black)

        Section("🔊 Volume do som: ${(settings.volume * 100).toInt()}%") {
            Slider(
                value = settings.volume,
                onValueChange = { onChange(settings.copy(volume = it)) },
                onValueChangeFinished = onPreviewSound,
            )
        }
        Section("📳 Vibração") {
            Choice(Level.entries, settings.vibration, { it.label }) {
                onChange(settings.copy(vibration = it))
                onPreviewVibration()
            }
        }
        Section("💥 Tremor do ecrã nas explosões") {
            Choice(Level.entries, settings.shake, { it.label }) { onChange(settings.copy(shake = it)) }
        }
        Section("🎮 Controlos") {
            Choice(ControlScheme.entries, settings.scheme, { it.short }) { onChange(settings.copy(scheme = it)) }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("✋ Bomba à esquerda", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Para quem joga com a mão trocada", color = Color(0xFF94A3B8), fontSize = 14.sp)
            }
            Switch(settings.leftHanded, { onChange(settings.copy(leftHanded = it)) })
        }
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text("Voltar", fontSize = 20.sp, fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun <T> Choice(options: List<T>, selected: T, label: (T) -> String, onPick: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, o ->
            SegmentedButton(
                selected = o == selected,
                onClick = { onPick(o) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                icon = {},
            ) { Text(label(o), fontSize = 13.sp, maxLines = 1) }
        }
    }
}
