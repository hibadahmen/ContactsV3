package tn.isetdjerba.contactsv3

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import tn.isetdjerba.contactsv3.databinding.ItemContactBinding

class ContactsAdapter(private val onClick: (Long) -> Unit) :
    ListAdapter<Contact, ContactsAdapter.Holder>(ContactDiff) {

    class Holder(val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemContactBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val contact = getItem(position)

        holder.binding.apply {
            name.text = contact.fullName
            phone.text = contact.phone
            photo.showPhoto(contact.photoUri)
            root.setOnClickListener { onClick(contact.id) }
        }
    }

    private object ContactDiff : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean =
            oldItem == newItem
    }
}