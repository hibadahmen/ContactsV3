package tn.isetdjerba.contactsv3
import android.net.Uri
import java.io.File
import java.io.IOException
import android.app.Application
import android.database.sqlite.SQLiteException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tn.isetdjerba.contactsv3.data.ContactDao
import tn.isetdjerba.contactsv3.data.ContactDbHelper

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = ContactDao(ContactDbHelper.getInstance(application))
    private val items = MutableLiveData<List<Contact>>(emptyList())
    val contacts: LiveData<List<Contact>> = items

    private val loadError = MutableLiveData<String?>(null)
    val error: LiveData<String?> = loadError

    init {
        loadContacts()
    }

    fun loadContacts() {
        viewModelScope.launch {
            try {
                items.value = withContext(Dispatchers.IO) { dao.getAll() }
                loadError.value = null
            } catch (_: SQLiteException) {
                loadError.value = "Impossible de charger les contacts."
            }
        }
    }

    fun find(id: Long): Contact? =
        items.value.orEmpty().firstOrNull { it.id == id }

    fun save(contact: Contact, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = try {
                withContext(Dispatchers.IO) {
                    persistContact(contact)
                }
            } catch (_: SQLiteException) {
                false
            } catch (_: IOException) {
                false
            } catch (_: SecurityException) {
                false
            }

            if (success) loadContacts()
            onFinished(success)
        }
    }

    fun delete(id: Long, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = try {
                withContext(Dispatchers.IO) { dao.delete(id) == 1 }
            } catch (_: SQLiteException) {
                false
            }

            if (success) loadContacts()
            onFinished(success)
        }
    }

    private fun persistContact(contact: Contact): Boolean {
        val source = contact.photoUri?.let { Uri.parse(it) }
        val application = getApplication<Application>()
        var copiedPhoto: File? = null
        var saved = false

        try {
            // Une URI content:// doit être copiée dans le stockage de l’application.
            if (source?.scheme == "content") {
                val directory = File(application.filesDir, "photos")
                directory.mkdirs()

                val destination = File.createTempFile("photo_", ".img", directory)
                copiedPhoto = destination

                val input = application.contentResolver.openInputStream(source)
                    ?: throw IOException("Photo inaccessible")

                input.use { stream ->
                    val bytes = destination.outputStream().use { output ->
                        stream.copyTo(output)
                    }
                    if (bytes == 0L) throw IOException("Photo vide")
                }
            }

            val record = contact.copy(
                photoUri = copiedPhoto?.absolutePath ?: contact.photoUri
            )

            saved = if (record.id == 0L) {
                dao.insert(record) > 0L
            } else {
                dao.update(record) == 1
            }

            return saved
        } finally {
            if (!saved) copiedPhoto?.delete()
        }
    }
}