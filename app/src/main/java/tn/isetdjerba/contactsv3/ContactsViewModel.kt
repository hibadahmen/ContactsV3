package tn.isetdjerba.contactsv3

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ContactsViewModel : ViewModel() {
    private val items = MutableLiveData(
        listOf(
            Contact(
                1, "Ben Salem", "Amira", "20000101", "amira@example.com",
                latitude = 33.875,
                longitude = 10.857
            ),
            Contact(
                2, "Trabelsi", "Youssef", "21000202", "youssef@example.com",
                latitude = 36.8065,
                longitude = 10.1815
            ),
            Contact(3, "Mansour", "Inès", "22000303", "ines@example.com")
        )
    )

    val contacts: LiveData<List<Contact>> = items

    fun find(id: Long): Contact? =
        items.value.orEmpty().firstOrNull { it.id == id }

    fun save(contact: Contact) {
        items.value = items.value.orEmpty().map {
            if (it.id == contact.id) contact else it
        }
    }
}