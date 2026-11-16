package tn.isetdjerba.contactsv3

import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceFragmentCompat
import kotlin.math.roundToInt

class ParametresFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val density = resources.displayMetrics.density
        val horizontalPadding = (16 * density).roundToInt()
        val topPadding = (12 * density).roundToInt()

        view.setBackgroundResource(R.color.contacts_background)

        listView.clipToPadding = false
        listView.setPadding(
            horizontalPadding,
            topPadding,
            horizontalPadding,
            horizontalPadding
        )
        setDivider(null)
    }
}