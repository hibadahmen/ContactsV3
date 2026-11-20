package tn.isetdjerba.contactsv3

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit

const val CONTACT_ID = "contact_id"

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findViewById<View>(R.id.nav_contacts).setOnClickListener {
            showContacts()
        }

        findViewById<View>(R.id.nav_add).setOnClickListener {
            val current = supportFragmentManager
                .findFragmentById(R.id.fragment_container)

            val creating = current is DetailContactFragment &&
                    current.arguments?.getLong(CONTACT_ID) == 0L

            if (!creating) {
                openDetail(0L)
            }
        }

        findViewById<View>(R.id.nav_settings).setOnClickListener {
            val current = supportFragmentManager
                .findFragmentById(R.id.fragment_container)

            if (current !is ParametresFragment) {
                open(ParametresFragment())
            }
        }

        supportFragmentManager.addOnBackStackChangedListener {
            updateNavigation()
        }

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(
                    R.id.fragment_container,
                    ListeContactsFragment()
                )
                runOnCommit {
                    updateNavigation()
                }
            }
        } else {
            updateNavigation()
        }
    }

    fun openDetail(id: Long) {
        val fragment = DetailContactFragment().apply {
            arguments = bundleOf(CONTACT_ID to id)
        }

        open(fragment)
    }

    fun openMap(id: Long) {
        val fragment = CarteContactFragment().apply {
            arguments = bundleOf(CONTACT_ID to id)
        }

        open(fragment)
    }

    private fun open(fragment: Fragment) {
        if (supportFragmentManager.isStateSaved) return

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container, fragment)
            addToBackStack(null)
        }

        supportFragmentManager.executePendingTransactions()
    }

    private fun showContacts() {
        if (supportFragmentManager.isStateSaved) return

        supportFragmentManager.popBackStackImmediate(
            null,
            FragmentManager.POP_BACK_STACK_INCLUSIVE
        )

        updateNavigation()
    }

    private fun updateNavigation() {
        val current = supportFragmentManager
            .findFragmentById(R.id.fragment_container)

        val creating = current is DetailContactFragment &&
                current.arguments?.getLong(CONTACT_ID) == 0L

        val settings = current is ParametresFragment

        val title = when {
            settings -> "Paramètres"
            creating -> "Nouveau contact"
            current is DetailContactFragment -> "Fiche du contact"
            current is CarteContactFragment -> "Position du contact"
            else -> getString(R.string.app_name)
        }

        findViewById<Toolbar>(R.id.main_toolbar).title = title

        styleNavigationLabel(
            R.id.nav_contacts_label,
            !settings && !creating
        )

        styleNavigationLabel(
            R.id.nav_add_label,
            creating
        )

        styleNavigationLabel(
            R.id.nav_settings_label,
            settings
        )

        findViewById<ImageView>(R.id.nav_contacts_icon).setColorFilter(
            navigationColor(!settings && !creating)
        )

        findViewById<ImageView>(R.id.nav_settings_icon).setColorFilter(
            navigationColor(settings)
        )
    }

    private fun styleNavigationLabel(id: Int, selected: Boolean) {
        findViewById<TextView>(id).apply {
            setTextColor(navigationColor(selected))
            setTypeface(
                null,
                if (selected) Typeface.BOLD else Typeface.NORMAL
            )
        }
    }

    private fun navigationColor(selected: Boolean): Int {
        val colorId = if (selected) {
            R.color.contacts_accent
        } else {
            R.color.contacts_muted
        }

        return ContextCompat.getColor(this, colorId)
    }
}