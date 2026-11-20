package tn.isetdjerba.contactsv3

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import tn.isetdjerba.contactsv3.data.Contact
import tn.isetdjerba.contactsv3.databinding.FragmentListeContactsBinding
import java.text.Collator
import java.util.Locale

class ListeContactsFragment : Fragment(R.layout.fragment_liste_contacts) {

    private val model: ContactsViewModel by activityViewModels()

    private var binding: FragmentListeContactsBinding? = null

    private var selectedFilter = Filter.ALL

    private val nameCollator = Collator.getInstance(Locale.FRENCH).apply {
        strength = Collator.PRIMARY
    }

    private enum class Filter {
        ALL,
        FAVORITES,
        FAMILY,
        DUPLICATES
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val savedFilter = savedInstanceState?.getString("selected_filter")

        selectedFilter = Filter.values().firstOrNull {
            it.name == savedFilter
        } ?: Filter.ALL
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val screen = FragmentListeContactsBinding.bind(view)
        binding = screen

        val mainActivity = requireActivity() as MainActivity

        val contactsAdapter = ContactsAdapter { id ->
            mainActivity.openDetail(id)
        }

        contactsAdapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        screen.contacts.layoutManager =
            LinearLayoutManager(requireContext())

        screen.contacts.adapter = contactsAdapter

        screen.addContact.setOnClickListener {
            mainActivity.openDetail(0L)
        }

        screen.filterAll.setOnClickListener {
            selectFilter(Filter.ALL)
        }

        screen.filterFavorites.setOnClickListener {
            selectFilter(Filter.FAVORITES)
        }

        screen.filterFamily.setOnClickListener {
            selectFilter(Filter.FAMILY)
        }

        screen.filterDuplicates.setOnClickListener {
            selectFilter(Filter.DUPLICATES)
        }

        screen.search.doAfterTextChanged {
            showResults()
        }

        model.contacts.observe(viewLifecycleOwner) {
            showResults()
        }

        model.error.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        updateFilterButtons()
        showResults()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        // Réapplique le filtre après restauration du champ de recherche.
        showResults()
    }

    private fun selectFilter(filter: Filter) {
        selectedFilter = filter

        updateFilterButtons()
        showResults()
    }

    private fun showResults() {
        val screen = binding ?: return
        val adapter = screen.contacts.adapter as? ContactsAdapter ?: return

        val allContacts = model.contacts.value.orEmpty()
        val query = screen.search.text.toString().trim()

        val filteredContacts = when (selectedFilter) {
            Filter.ALL -> allContacts

            Filter.FAVORITES -> allContacts.filter {
                it.favorite
            }

            Filter.FAMILY -> allContacts.filter {
                it.family
            }

            Filter.DUPLICATES -> {
                val duplicateNumbers = allContacts
                    .map { it.phoneKey }
                    .filter { it.isNotEmpty() }
                    .groupingBy { it }
                    .eachCount()
                    .filterValues { it > 1 }
                    .keys

                allContacts.filter {
                    it.phoneKey.isNotEmpty() &&
                            it.phoneKey in duplicateNumbers
                }
            }
        }

        val results = filteredContacts
            .filter { it.matches(query) }
            .sortedWith(Comparator<Contact> { first, second ->
                val lastNameComparison = nameCollator.compare(
                    first.lastName,
                    second.lastName
                )

                if (lastNameComparison != 0) {
                    lastNameComparison
                } else {
                    val firstNameComparison = nameCollator.compare(
                        first.firstName,
                        second.firstName
                    )

                    if (firstNameComparison != 0) {
                        firstNameComparison
                    } else {
                        first.id.compareTo(second.id)
                    }
                }
            })

        adapter.submitList(results)

        screen.empty.isVisible = results.isEmpty()

        screen.listTitle.text = when (selectedFilter) {
            Filter.ALL -> "All Contacts"
            Filter.FAVORITES -> "Favorites"
            Filter.FAMILY -> "My Family"
            Filter.DUPLICATES -> "Duplicate Contacts"
        }

        screen.contactCount.text = when (results.size) {
            0 -> "0 contact"
            1 -> "1 contact"
            else -> "${results.size} contacts"
        }

        screen.empty.text = if (query.isNotEmpty()) {
            "Aucun contact ne correspond à votre recherche."
        } else {
            when (selectedFilter) {
                Filter.ALL ->
                    "Aucun contact. Appuyez sur Ajouter un contact."

                Filter.FAVORITES ->
                    "Aucun favori. Ouvrez un contact et cochez Ajouter aux favoris."

                Filter.FAMILY ->
                    "Aucun contact dans Ma famille."

                Filter.DUPLICATES ->
                    "Aucun numéro de téléphone en double."
            }
        }
    }

    private fun updateFilterButtons() {
        val screen = binding ?: return

        styleFilter(screen.filterAll, selectedFilter == Filter.ALL)

        styleFilter(
            screen.filterFavorites,
            selectedFilter == Filter.FAVORITES
        )

        styleFilter(
            screen.filterFamily,
            selectedFilter == Filter.FAMILY
        )

        styleFilter(
            screen.filterDuplicates,
            selectedFilter == Filter.DUPLICATES
        )
    }

    private fun styleFilter(
        button: MaterialButton,
        selected: Boolean
    ) {
        val darkBlue = android.graphics.Color.parseColor("#1B263B")
        val lightBlue = android.graphics.Color.parseColor("#DCE3EB")

        button.backgroundTintList = ColorStateList.valueOf(
            if (selected) darkBlue else lightBlue
        )

        button.setTextColor(
            if (selected) android.graphics.Color.WHITE else darkBlue
        )

        button.isSelected = selected
    }

    override fun onResume() {
        super.onResume()

        val screen = binding ?: return
        val adapter = screen.contacts.adapter as? ContactsAdapter ?: return

        adapter.applyPreferences(
            PreferencesManager.read(requireContext())
        )

        showResults()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(
            "selected_filter",
            selectedFilter.name
        )
    }

    override fun onDestroyView() {
        binding?.contacts?.adapter = null
        binding = null

        super.onDestroyView()
    }
}