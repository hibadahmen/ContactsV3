package tn.isetdjerba.contactsv3

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import tn.isetdjerba.contactsv3.databinding.ItemContactBinding

class ContactsAdapter(private val onClick: (Long) -> Unit) :
    ListAdapter<Contact, ContactsAdapter.Holder>(ContactDiff) {

    private var preferences = ContactPreferences()

    class Holder(val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun applyPreferences(newPreferences: ContactPreferences) {
        if (preferences == newPreferences) return
        preferences = newPreferences
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemContactBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val contact = getItem(position)

        holder.binding.apply {
            name.text = contact.fullName
            name.textSize = preferences.textSizeSp
            name.setTextColor(preferences.nameColor)

            phone.text = contact.phone
            phone.textSize = preferences.textSizeSp

            photo.showPhoto(contact.photoUri)

            root.setOnClickListener { onClick(contact.id) }
            callContact.isVisible = preferences.showCallButton

            callContact.setOnClickListener { button ->
                val context = button.context
                val number = contact.phone.trim()

                if (number.isEmpty()) {
                    Toast.makeText(
                        context,
                        "Ce contact n’a pas de numéro.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val uri = Uri.fromParts("tel", number, null)

                    try {
                        context.startActivity(Intent(Intent.ACTION_DIAL, uri))
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, R.string.no_app, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private object ContactDiff : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean =
            oldItem == newItem
    }
}