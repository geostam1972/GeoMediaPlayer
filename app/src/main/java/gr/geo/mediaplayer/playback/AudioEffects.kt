package gr.geo.mediaplayer.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import gr.geo.mediaplayer.data.LibraryStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Equalizer + bass boost attached to the music player's audio session. */
object AudioEffects {

    data class Band(val index: Int, val centerHz: Int, val levelMb: Int)

    data class State(
        val available: Boolean = false,
        val enabled: Boolean = false,
        val minMb: Int = -1500,
        val maxMb: Int = 1500,
        val bands: List<Band> = emptyList(),
        val presets: List<String> = emptyList(),
        val currentPreset: Int = -1,
        val bassStrength: Int = 0, // 0..1000
        val bassSupported: Boolean = false,
    )

    private var equalizer: Equalizer? = null
    private var bass: BassBoost? = null
    private var sessionId = 0

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == sessionId) return
        release()
        sessionId = audioSessionId
        runCatching {
            val eq = Equalizer(0, audioSessionId)
            equalizer = eq
            val enabled = LibraryStore.getBool("eq_enabled", false)
            val preset = LibraryStore.getInt("eq_preset", -1)
            val range = eq.bandLevelRange
            for (b in 0 until eq.numberOfBands) {
                val saved = LibraryStore.getInt("eq_band_$b", Int.MIN_VALUE)
                if (saved != Int.MIN_VALUE) {
                    eq.setBandLevel(b.toShort(), saved.coerceIn(range[0].toInt(), range[1].toInt()).toShort())
                }
            }
            eq.setEnabled(enabled)
        }
        runCatching {
            val bb = BassBoost(0, audioSessionId)
            bass = bb
            val strength = LibraryStore.getInt("bass_strength", 0)
            if (bb.strengthSupported) bb.setStrength(strength.toShort())
            bb.setEnabled(LibraryStore.getBool("eq_enabled", false) && strength > 0)
        }
        publish()
    }

    fun setEnabled(on: Boolean) {
        LibraryStore.putBool("eq_enabled", on)
        runCatching { equalizer?.setEnabled(on) }
        runCatching { bass?.setEnabled(on && LibraryStore.getInt("bass_strength", 0) > 0) }
        publish()
    }

    fun setBand(index: Int, levelMb: Int) {
        val eq = equalizer ?: return
        runCatching {
            eq.setBandLevel(index.toShort(), levelMb.toShort())
            LibraryStore.putInt("eq_band_$index", levelMb)
            LibraryStore.putInt("eq_preset", -1)
        }
        publish()
    }

    fun usePreset(preset: Int) {
        val eq = equalizer ?: return
        runCatching {
            eq.usePreset(preset.toShort())
            LibraryStore.putInt("eq_preset", preset)
            for (b in 0 until eq.numberOfBands) {
                LibraryStore.putInt("eq_band_$b", eq.getBandLevel(b.toShort()).toInt())
            }
        }
        publish()
    }

    fun reset() {
        val eq = equalizer ?: return
        runCatching {
            for (b in 0 until eq.numberOfBands) {
                eq.setBandLevel(b.toShort(), 0)
                LibraryStore.putInt("eq_band_$b", 0)
            }
            LibraryStore.putInt("eq_preset", -1)
        }
        setBass(0)
    }

    fun setBass(strength: Int) {
        LibraryStore.putInt("bass_strength", strength)
        runCatching {
            bass?.let {
                if (it.strengthSupported) it.setStrength(strength.toShort())
                it.setEnabled(LibraryStore.getBool("eq_enabled", false) && strength > 0)
            }
        }
        publish()
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bass?.release() }
        equalizer = null
        bass = null
        sessionId = 0
        _state.value = State()
    }

    private fun publish() {
        val eq = equalizer
        if (eq == null) {
            _state.value = State()
            return
        }
        runCatching {
            val range = eq.bandLevelRange
            _state.value = State(
                available = true,
                enabled = eq.enabled,
                minMb = range[0].toInt(),
                maxMb = range[1].toInt(),
                bands = (0 until eq.numberOfBands).map { b ->
                    Band(b, eq.getCenterFreq(b.toShort()) / 1000, eq.getBandLevel(b.toShort()).toInt())
                },
                presets = (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) },
                currentPreset = LibraryStore.getInt("eq_preset", -1),
                bassStrength = LibraryStore.getInt("bass_strength", 0),
                bassSupported = bass?.strengthSupported == true,
            )
        }
    }
}
