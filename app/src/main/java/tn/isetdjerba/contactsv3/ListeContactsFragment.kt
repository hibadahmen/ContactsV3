package tn.isetdjerba.contactsv3

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import tn.isetdjerba.contactsv3.databinding.FragmentListeContactsBinding

class ListeContactsFragment : Fragment(R.layout.fragment_liste_contacts) {

    private val model: ContactsViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentListeContactsBinding.bind(view)
        val mainActivity = requireActivity() as MainActivity

        binding.addContact.setOnClickListener {
            mainActivity.openDetail(0L)
        }

        val adapter = ContactsAdapter { id ->
            mainActivity.openDetail(id)
        }

        adapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        binding.contacts.layoutManager = LinearLayoutManager(requireContext())
        binding.contacts.adapter = adapter

        fun showResults(contacts: List<Contact>) {
            val query = binding.search.text.toString().trim()
            val results = contacts.filter { it.matches(query) }

            adapter.submitList(results)
            binding.empty.isVisible = results.isEmpty()
        }

        binding.search.doAfterTextChanged {
            showResults(model.contacts.value.orEmpty())
        }

        model.contacts.observe(viewLifecycleOwner) { contacts ->
            showResults(contacts)
        }

        model.error.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val currentView = view ?: return
        val binding = FragmentListeContactsBinding.bind(currentView)
        val adapter = binding.contacts.adapter as? ContactsAdapter ?: return

        adapter.applyPreferences(PreferencesManager.read(requireContext()))
    }

}