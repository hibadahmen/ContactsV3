package tn.isetdjerba.contactsv3

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import tn.isetdjerba.contactsv3.data.Contact
import tn.isetdjerba.contactsv3.databinding.FragmentDetailContactBinding
import java.io.File
import java.io.IOException

class DetailContactFragment : Fragment(R.layout.fragment_detail_contact) {

    private val model: ContactsViewModel by activityViewModels()

    private val contactId: Long
        get() = arguments?.getLong(CONTACT_ID) ?: 0L

    private var binding: FragmentDetailContactBinding? = null
    private var editing = false
    private var draftPhoto: String? = null
    private var pendingCameraPath: String? = null

    private val gallery = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && editing) {
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                updatePhoto(uri)
            } catch (_: SecurityException) {
                message(R.string.photo_error)
            }
        }
    }

    private val camera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraPath?.let(::File)
        pendingCameraPath = null

        if (file != null) {
            if (success && editing && file.length() > 0) {
                updatePhoto(cameraUri(file))
            } else {
                file.delete()
                if (success) message(R.string.photo_error)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        editing = savedInstanceState?.getBoolean("editing") ?: false
        draftPhoto = savedInstanceState?.getString("draft_photo")
        pendingCameraPath = savedInstanceState?.getString("camera_path")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val screen = FragmentDetailContactBinding.bind(view)
        binding = screen

        screen.back.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        screen.cancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        screen.gallery.setOnClickListener {
            launchExternal {
                gallery.launch(arrayOf("image/*"))
            }
        }

        screen.camera.setOnClickListener {
            takePhoto()
        }

        if (contactId == 0L) {
            editing = true
            screen.photo.showPhoto(draftPhoto)
            renderMode(screen)

            screen.save.setOnClickListener {
                save(
                    screen,
                    Contact(
                        lastName = "",
                        firstName = "",
                        phone = ""
                    )
                )
            }
            return
        }

        model.contacts.observe(viewLifecycleOwner) { contacts ->
            val contact = contacts.firstOrNull { it.id == contactId }

            if (contact == null) {
                screen.displayName.setText(R.string.missing_contact)
                renderMode(screen, available = false)
            } else {
                showContact(screen, contact)
            }
        }
    }

    private fun showContact(
        screen: FragmentDetailContactBinding,
        contact: Contact
    ) {
        screen.displayName.text = contact.fullName
        screen.displayPhone.text = contact.phone
        screen.displayEmail.text = contact.email.orEmpty()

        screen.displayLatitude.text = getString(
            R.string.latitude_label,
            coordinateText(contact.latitude)
        )

        screen.displayLongitude.text = getString(
            R.string.longitude_label,
            coordinateText(contact.longitude)
        )

        val groups = mutableListOf<String>()

        if (contact.favorite) groups.add("Favoris")
        if (contact.family) groups.add("Ma famille")

        screen.displayGroups.text = groups.joinToString(" • ")
        screen.displayGroups.isVisible = groups.isNotEmpty()

        screen.photo.showPhoto(
            if (editing) draftPhoto else contact.photoUri
        )

        renderMode(screen)

        screen.edit.setOnClickListener {
            editing = true
            draftPhoto = contact.photoUri

            screen.lastName.setText(contact.lastName)
            screen.firstName.setText(contact.firstName)
            screen.phone.setText(contact.phone)
            screen.email.setText(contact.email.orEmpty())
            screen.latitude.setText(
                contact.latitude?.toString().orEmpty()
            )
            screen.longitude.setText(
                contact.longitude?.toString().orEmpty()
            )

            screen.chkFavorite.isChecked = contact.favorite
            screen.chkFamily.isChecked = contact.family

            screen.photo.showPhoto(draftPhoto)
            renderMode(screen)
        }

        screen.save.setOnClickListener {
            save(screen, contact)
        }

        screen.deleteContact.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Supprimer le contact")
                .setMessage("Supprimer ${contact.fullName} ?")
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Supprimer") { _, _ ->
                    screen.deleteContact.isEnabled = false

                    model.delete(contact.id) finished@{ success ->
                        val currentScreen = binding ?: return@finished
                        currentScreen.deleteContact.isEnabled = true

                        if (success) {
                            if (!parentFragmentManager.isStateSaved) {
                                parentFragmentManager.popBackStack()
                            }
                        } else {
                            message("Impossible de supprimer le contact.")
                        }
                    }
                }
                .show()
        }

        screen.call.setOnClickListener {
            if (contact.phone.isBlank()) {
                message("Aucun numéro pour ce contact.")
            } else {
                launchExternal {
                    val uri = Uri.fromParts("tel", contact.phone, null)
                    startActivity(Intent(Intent.ACTION_DIAL, uri))
                }
            }
        }

        screen.mail.setOnClickListener {
            val address = contact.email

            if (address.isNullOrBlank()) {
                message("Aucune adresse e-mail pour ce contact.")
            } else {
                launchExternal {
                    val uri = Uri.fromParts("mailto", address, null)
                    startActivity(Intent(Intent.ACTION_SENDTO, uri))
                }
            }
        }

        screen.mapAction.setOnClickListener {
            if (contact.hasPosition) {
                (requireActivity() as MainActivity).openMap(contact.id)
            } else {
                message(R.string.no_position)
            }
        }
    }

    private fun renderMode(
        screen: FragmentDetailContactBinding,
        available: Boolean = true
    ) {
        screen.form.isVisible = available && editing
        screen.summary.isVisible = !editing || !available

        listOf(
            screen.save,
            screen.cancel,
            screen.gallery,
            screen.camera
        ).forEach {
            it.isVisible = available && editing
        }

        listOf(
            screen.edit,
            screen.call,
            screen.mail,
            screen.mapAction
        ).forEach {
            it.isVisible = available && !editing
        }

        screen.deleteContact.isVisible =
            available && !editing && contactId != 0L
    }

    private fun save(
        screen: FragmentDetailContactBinding,
        contact: Contact
    ) {
        val requiredFields = listOf(
            screen.lastName,
            screen.firstName,
            screen.phone
        )

        requiredFields.forEach { it.error = null }
        screen.email.error = null
        screen.latitude.error = null
        screen.longitude.error = null

        val missing = requiredFields.filter {
            it.text.isBlank()
        }

        missing.forEach {
            it.error = getString(R.string.required)
        }

        if (missing.isNotEmpty()) return

        val email = screen.email.text.toString().trim()

        if (
            email.isNotEmpty() &&
            !Patterns.EMAIL_ADDRESS.matcher(email).matches()
        ) {
            screen.email.error = getString(R.string.bad_email)
            return
        }

        val latitude = readCoordinate(screen.latitude)
        val longitude = readCoordinate(screen.longitude)

        val invalidLatitude =
            screen.latitude.text.isNotBlank() &&
                    (latitude == null || latitude !in -90.0..90.0)

        val invalidLongitude =
            screen.longitude.text.isNotBlank() &&
                    (longitude == null || longitude !in -180.0..180.0)

        if (invalidLatitude) {
            screen.latitude.error = getString(R.string.bad_lat)
        }

        if (invalidLongitude) {
            screen.longitude.error = getString(R.string.bad_lon)
        }

        if (invalidLatitude || invalidLongitude) return

        if ((latitude == null) != (longitude == null)) {
            val missingCoordinate =
                if (latitude == null) screen.latitude else screen.longitude

            missingCoordinate.error = getString(R.string.no_position)
            return
        }

        val updatedContact = contact.copy(
            lastName = screen.lastName.text.toString().trim(),
            firstName = screen.firstName.text.toString().trim(),
            phone = screen.phone.text.toString().trim(),
            email = email.takeIf { it.isNotEmpty() },
            photoUri = draftPhoto,
            latitude = latitude,
            longitude = longitude,
            favorite = screen.chkFavorite.isChecked,
            family = screen.chkFamily.isChecked
        )

        screen.save.isEnabled = false

        model.save(updatedContact) finished@{ success ->
            val currentScreen = binding ?: return@finished
            currentScreen.save.isEnabled = true

            if (success) {
                if (!parentFragmentManager.isStateSaved) {
                    parentFragmentManager.popBackStack()
                }
            } else {
                message("Impossible d’enregistrer le contact.")
            }
        }
    }

    private fun readCoordinate(field: EditText): Double? =
        field.text.toString()
            .trim()
            .replace(',', '.')
            .toDoubleOrNull()

    private fun coordinateText(value: Double?): String =
        value?.toString() ?: getString(R.string.undefined_coordinate)

    private fun takePhoto() {
        try {
            val directory = File(requireContext().filesDir, "photos")
            directory.mkdirs()

            val file = File.createTempFile(
                "contact_",
                ".jpg",
                directory
            )

            pendingCameraPath = file.absolutePath

            try {
                camera.launch(cameraUri(file))
            } catch (_: ActivityNotFoundException) {
                pendingCameraPath = null
                file.delete()
                message(R.string.no_app)
            } catch (_: SecurityException) {
                pendingCameraPath = null
                file.delete()
                message(R.string.no_app)
            }
        } catch (_: IOException) {
            message(R.string.photo_error)
        }
    }

    private fun cameraUri(file: File): Uri =
        FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.photos",
            file
        )

    private fun updatePhoto(uri: Uri) {
        draftPhoto = uri.toString()
        binding?.photo?.showPhoto(draftPhoto)
    }

    private fun launchExternal(action: () -> Unit) {
        try {
            action()
        } catch (_: ActivityNotFoundException) {
            message(R.string.no_app)
        } catch (_: SecurityException) {
            message(R.string.no_app)
        }
    }

    private fun message(text: Int) {
        message(getString(text))
    }

    private fun message(text: String) {
        context?.let {
            Toast.makeText(it, text, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean("editing", editing)
        outState.putString("draft_photo", draftPhoto)
        outState.putString("camera_path", pendingCameraPath)
    }

    override fun onResume() {
        super.onResume()

        val screen = binding ?: return
        val preferences = PreferencesManager.read(requireContext())

        screen.displayName.textSize = preferences.textSizeSp
        screen.displayName.setTextColor(preferences.nameColor)
        screen.displayPhone.textSize = preferences.textSizeSp

        screen.firstName.textSize = preferences.textSizeSp
        screen.firstName.setTextColor(preferences.nameColor)
        screen.lastName.textSize = preferences.textSizeSp
        screen.lastName.setTextColor(preferences.nameColor)
        screen.phone.textSize = preferences.textSizeSp
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}