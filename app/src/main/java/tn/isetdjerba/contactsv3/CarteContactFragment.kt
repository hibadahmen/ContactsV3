package tn.isetdjerba.contactsv3

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import tn.isetdjerba.contactsv3.databinding.FragmentCarteContactBinding

class CarteContactFragment : Fragment(R.layout.fragment_carte_contact) {
    private val model: ContactsViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentCarteContactBinding.bind(view)
        binding.back.setOnClickListener { parentFragmentManager.popBackStack() }

        val contact = model.find(requireArguments().getLong(CONTACT_ID))
        val latitude = contact?.latitude
        val longitude = contact?.longitude

        val issue = when {
            contact == null -> R.string.missing_contact
            !contact.hasPosition || latitude == null || longitude == null -> R.string.no_position
            !BuildConfig.MAPS_CONFIGURED -> R.string.maps_missing
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext()) !=
                    ConnectionResult.SUCCESS -> R.string.maps_unavailable
            else -> null
        }

        if (issue != null) {
            binding.message.setText(issue)
            binding.message.isVisible = true
            binding.mapContainer.isVisible = false
            return
        }

        val position = LatLng(checkNotNull(latitude), checkNotNull(longitude))
        val title = checkNotNull(contact).fullName

        val map = childFragmentManager.findFragmentById(R.id.map_container) as? SupportMapFragment
            ?: SupportMapFragment.newInstance().also {
                childFragmentManager.commitNow { add(R.id.map_container, it) }
            }

        val owner = viewLifecycleOwner

        map.getMapAsync { googleMap ->
            if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) return@getMapAsync

            googleMap.clear()
            googleMap.addMarker(
                MarkerOptions().position(position).title(title)
            )?.showInfoWindow()

            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 14f))
        }
    }
}