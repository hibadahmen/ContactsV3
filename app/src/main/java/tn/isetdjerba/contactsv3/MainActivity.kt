package tn.isetdjerba.contactsv3

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
const val CONTACT_ID = "contact_id"

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val toolbar = findViewById<Toolbar>(R.id.main_toolbar)
        toolbar.setTitle(R.string.app_name)

        toolbar.menu.add("Paramètres").apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

            setOnMenuItemClickListener {
                val current = supportFragmentManager.findFragmentById(R.id.fragment_container)

                if (current !is ParametresFragment) {
                    open(ParametresFragment(), 0L)
                }

                true
            }
        }
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.fragment_container, ListeContactsFragment())
            }
        }
    }

    fun openDetail(id: Long) = open(DetailContactFragment(), id)

    fun openMap(id: Long) = open(CarteContactFragment(), id)

    private fun open(screen: Fragment, id: Long) {
        if (supportFragmentManager.isStateSaved) return

        screen.arguments = bundleOf(CONTACT_ID to id)

        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container, screen)
            addToBackStack(null)
        }

        supportFragmentManager.executePendingTransactions()
    }
}