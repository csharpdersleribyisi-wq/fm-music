package com.example.player

import android.content.Context
import android.os.Bundle

data class EqualizerBandSpec(
    val index: Int,
    val frequencyHz: Float,
    val label: String,
    val roleLabel: String
)

enum class EqualizerPreset(
    val label: String,
    val description: String,
    val bandGainsDb: List<Float>,
    val preampDb: Float = 0f
) {
    FLAT(
        label = "Flat",
        description = "Neutral studio reference",
        bandGainsDb = listOf(0f, 0f, 0f, 0f, 0f),
        preampDb = 0f
    ),
    BASS_BOOST(
        label = "Bass Boost",
        description = "Deep sub-bass punch and warmth",
        bandGainsDb = listOf(7.5f, 5.0f, 0.5f, -1.0f, 0.5f),
        preampDb = -1.5f
    ),
    JAZZ(
        label = "Jazz",
        description = "Smooth brass presence & warm upright",
        bandGainsDb = listOf(3.5f, 2.5f, -2.0f, 3.0f, 4.0f),
        preampDb = -0.5f
    ),
    VOCAL_BOOST(
        label = "Vocal",
        description = "Focused midrange clarity for vocals",
        bandGainsDb = listOf(-2.5f, -1.0f, 5.5f, 4.5f, 1.5f),
        preampDb = -0.5f
    ),
    CLASSICAL(
        label = "Classical",
        description = "Balanced acoustic resonance & air",
        bandGainsDb = listOf(4.5f, 3.0f, -1.5f, 2.5f, 5.0f),
        preampDb = -0.5f
    ),
    ROCK(
        label = "Rock",
        description = "Crisp guitars & driving low-end",
        bandGainsDb = listOf(5.5f, 3.0f, -1.5f, 3.5f, 5.0f),
        preampDb = -1.0f
    ),
    POP(
        label = "Pop",
        description = "Forward vocals & radio sheen",
        bandGainsDb = listOf(-1.5f, 2.5f, 4.5f, 3.5f, -1.0f),
        preampDb = -0.5f
    ),
    ELECTRONIC(
        label = "Electronic",
        description = "Tight sub-bass & sparkling synths",
        bandGainsDb = listOf(6.5f, 3.5f, 0.0f, 2.5f, 6.0f),
        preampDb = -1.5f
    ),
    CUSTOM(
        label = "Custom",
        description = "Your hand-tuned frequency curve",
        bandGainsDb = listOf(0f, 0f, 0f, 0f, 0f),
        preampDb = 0f
    )
}

data class EqualizerSettings(
    val enabled: Boolean = true,
    val preset: EqualizerPreset = EqualizerPreset.FLAT,
    val bandGainsDb: List<Float> = EqualizerPreset.FLAT.bandGainsDb,
    val preampDb: Float = 0f
) {
    val isModifiedFromFlat: Boolean
        get() = enabled && (
            preset != EqualizerPreset.FLAT ||
                bandGainsDb.any { kotlin.math.abs(it) > 0.05f } ||
                kotlin.math.abs(preampDb) > 0.05f
            )

    fun toBundle(): Bundle = Bundle().apply {
        putBoolean(EqualizerProtocol.KEY_ENABLED, enabled)
        putString(EqualizerProtocol.KEY_PRESET, preset.name)
        putFloatArray(EqualizerProtocol.KEY_BAND_GAINS, normalizedGains().toFloatArray())
        putFloat(EqualizerProtocol.KEY_PREAMP_DB, preampDb.coerceIn(PREAMP_MIN_DB, PREAMP_MAX_DB))
    }

    fun normalizedGains(): List<Float> {
        return List(BAND_SPECS.size) { index ->
            bandGainsDb.getOrElse(index) { 0f }.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
        }
    }

    companion object {
        const val MIN_GAIN_DB = -12f
        const val MAX_GAIN_DB = 12f
        const val PREAMP_MIN_DB = -6f
        const val PREAMP_MAX_DB = 6f

        val BAND_SPECS = listOf(
            EqualizerBandSpec(index = 0, frequencyHz = 60f, label = "60 Hz", roleLabel = "Sub"),
            EqualizerBandSpec(index = 1, frequencyHz = 230f, label = "230 Hz", roleLabel = "Bass"),
            EqualizerBandSpec(index = 2, frequencyHz = 910f, label = "910 Hz", roleLabel = "Mid"),
            EqualizerBandSpec(index = 3, frequencyHz = 3600f, label = "3.6 kHz", roleLabel = "Pres"),
            EqualizerBandSpec(index = 4, frequencyHz = 14000f, label = "14 kHz", roleLabel = "Air")
        )

        fun fromBundle(bundle: Bundle?): EqualizerSettings {
            if (bundle == null) return EqualizerSettings()
            val enabled = bundle.getBoolean(EqualizerProtocol.KEY_ENABLED, true)
            val presetName = bundle.getString(EqualizerProtocol.KEY_PRESET, EqualizerPreset.FLAT.name)
            val preset = EqualizerPreset.entries.firstOrNull { it.name == presetName }
                ?: EqualizerPreset.FLAT
            val rawArray = bundle.getFloatArray(EqualizerProtocol.KEY_BAND_GAINS)
            val gains = if (rawArray != null && rawArray.size == BAND_SPECS.size) {
                rawArray.map { it.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB) }
            } else {
                preset.bandGainsDb
            }
            val preamp = bundle.getFloat(EqualizerProtocol.KEY_PREAMP_DB, 0f)
                .coerceIn(PREAMP_MIN_DB, PREAMP_MAX_DB)
            return EqualizerSettings(
                enabled = enabled,
                preset = preset,
                bandGainsDb = gains,
                preampDb = preamp
            )
        }
    }
}

object EqualizerProtocol {
    const val COMMAND_APPLY_EQUALIZER = "com.example.player.APPLY_EQUALIZER"
    const val KEY_ENABLED = "eq_enabled"
    const val KEY_PRESET = "eq_preset"
    const val KEY_BAND_GAINS = "eq_band_gains"
    const val KEY_PREAMP_DB = "eq_preamp_db"
}

object EqualizerStore {
    private const val PREFS_NAME = "sonora_equalizer_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_PRESET = "preset"
    private const val KEY_PREAMP = "preamp_db"
    private const val KEY_BAND_PREFIX = "band_"

    fun load(context: Context): EqualizerSettings {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_ENABLED, true)
        val presetName = prefs.getString(KEY_PRESET, EqualizerPreset.FLAT.name)
        val preset = EqualizerPreset.entries.firstOrNull { it.name == presetName }
            ?: EqualizerPreset.FLAT
        val gains = List(EqualizerSettings.BAND_SPECS.size) { idx ->
            prefs.getFloat(
                "$KEY_BAND_PREFIX$idx",
                preset.bandGainsDb.getOrElse(idx) { 0f }
            ).coerceIn(EqualizerSettings.MIN_GAIN_DB, EqualizerSettings.MAX_GAIN_DB)
        }
        val preamp = prefs.getFloat(KEY_PREAMP, 0f)
            .coerceIn(EqualizerSettings.PREAMP_MIN_DB, EqualizerSettings.PREAMP_MAX_DB)
        return EqualizerSettings(
            enabled = enabled,
            preset = preset,
            bandGainsDb = gains,
            preampDb = preamp
        )
    }

    fun save(context: Context, settings: EqualizerSettings) {
        val normalized = settings.normalizedGains()
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putString(KEY_PRESET, settings.preset.name)
            .putFloat(KEY_PREAMP, settings.preampDb)
            .apply {
                normalized.forEachIndexed { idx, value ->
                    putFloat("$KEY_BAND_PREFIX$idx", value)
                }
            }
            .apply()
    }
}
