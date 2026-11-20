package tn.isetdjerba.contactsv3

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tn.isetdjerba.contactsv3.data.Contact
import tn.isetdjerba.contactsv3.data.ContactDao
import tn.isetdjerba.contactsv3.data.ContactDbHelper
import java.io.File
import java.io.IOException

class ContactsViewModel(application: Application) :
    AndroidViewModel(application) {

    private val dao = ContactDao(
        ContactDbHelper.getInstance(application)
    )

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
                val result = withContext(Dispatchers.IO) {
                    dao.getAll()
                }

                items.value = result
                loadError.value = null
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                reportError("Chargement des contacts", exception)
            }
        }
    }

    fun find(id: Long): Contact? =
        items.value.orEmpty().firstOrNull {
            it.id == id
        }

    fun save(
        contact: Contact,
        onFinished: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val success = try {
                val saved = withContext(Dispatchers.IO) {
                    persistContact(contact)
                }

                if (!saved) {
                    reportError(
                        "Enregistrement du contact",
                        IOException(
                            "La base n'a pas confirmé l'enregistrement. " +
                                    "Vérifiez aussi les messages SQLiteLog."
                        )
                    )
                }

                saved
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                reportError("Enregistrement du contact", exception)
                false
            }

            if (success) {
                loadError.value = null
                loadContacts()
            }

            onFinished(success)
        }
    }

    fun delete(
        id: Long,
        onFinished: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val success = try {
                withContext(Dispatchers.IO) {
                    dao.delete(id) == 1
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                reportError("Suppression du contact", exception)
                false
            }

            if (success) {
                loadError.value = null
                loadContacts()
            }

            onFinished(success)
        }
    }

    private fun reportError(
        operation: String,
        exception: Exception
    ) {
        Log.e(
            "CONTACTS_DB",
            "$operation : ${exception.message}",
            exception
        )

        loadError.value =
            "$operation : ${exception.message ?: exception.javaClass.simpleName}"
    }

    private fun persistContact(contact: Contact): Boolean {
        val source = contact.photoUri?.let {
            Uri.parse(it)
        }

        val application = getApplication<Application>()

        var copiedPhoto: File? = null
        var saved = false

        try {
            if (source?.scheme == "content") {
                val directory = File(
                    application.filesDir,
                    "photos"
                )

                if (!directory.exists() && !directory.mkdirs()) {
                    throw IOException(
                        "Impossible de créer le dossier des photos."
                    )
                }

                val destination = File.createTempFile(
                    "photo_",
                    ".img",
                    directory
                )

                copiedPhoto = destination

                val input = application.contentResolver
                    .openInputStream(source)
                    ?: throw IOException("Photo inaccessible")

                input.use { stream ->
                    val bytes = destination.outputStream().use { output ->
                        stream.copyTo(output)
                    }

                    if (bytes == 0L) {
                        throw IOException("Photo vide")
                    }
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
            if (!saved) {
                copiedPhoto?.delete()
            }
        }
    }
}