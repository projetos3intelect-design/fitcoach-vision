package br.com.fitcoachvision

import android.content.Context

/**
 * Preferencias simples.
 *
 * Usa SharedPreferences de proposito: o DataStore entra na Fase 3, junto com as
 * configuracoes de voz. Ate la, uma dependencia a menos e um risco a menos no
 * primeiro build.
 */
class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("fitcoach", Context.MODE_PRIVATE)

    var disclaimerAccepted: Boolean
        get() = prefs.getBoolean(KEY_DISCLAIMER, false)
        set(value) = prefs.edit().putBoolean(KEY_DISCLAIMER, value).apply()

    var useFrontCamera: Boolean
        get() = prefs.getBoolean(KEY_FRONT_CAMERA, true)
        set(value) = prefs.edit().putBoolean(KEY_FRONT_CAMERA, value).apply()

    var useFullModel: Boolean
        get() = prefs.getBoolean(KEY_FULL_MODEL, false)
        set(value) = prefs.edit().putBoolean(KEY_FULL_MODEL, value).apply()

    var showDiagnostics: Boolean
        get() = prefs.getBoolean(KEY_DIAGNOSTICS, true)
        set(value) = prefs.edit().putBoolean(KEY_DIAGNOSTICS, value).apply()

    private companion object {
        const val KEY_DISCLAIMER = "disclaimer_accepted"
        const val KEY_FRONT_CAMERA = "use_front_camera"
        const val KEY_FULL_MODEL = "use_full_model"
        const val KEY_DIAGNOSTICS = "show_diagnostics"
    }
}
