package tn.isetdjerba.contactsv3

import android.content.Context
import android.graphics.Color
import androidx.preference.PreferenceManager

data class ContactPreferences(
    val textSizeSp: Float = 16f,
    val nameColor: Int = Color.BLACK,
    val showCallButton: Boolean = false
)

object PreferencesManager {

    fun read(context: Context): ContactPreferences {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val size = when (prefs.getString("taille_police", "16")) {
            "14" -> 14f
            "20" -> 20f
            else -> 16f
        }

        val colorValue = prefs.getString("couleur_preferee", "#000000") ?: "#000000"
        val color = try {
            Color.parseColor(colorValue)
        } catch (_: IllegalArgumentException) {
            Color.BLACK
        }

        return ContactPreferences(
            textSizeSp = size,
            nameColor = color,
            showCallButton = prefs.getBoolean("appel_depuis_liste", false)
        )
    }
}