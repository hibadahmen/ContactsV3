package tn.isetdjerba.contactsv3

data class Contact(
    val id: Long,
    val lastName: String,
    val firstName: String,
    val phone: String,
    val email: String,
    val photoUri: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val fullName get() = "$firstName $lastName"

    val hasPosition get() =
        latitude != null && latitude in -90.0..90.0 &&
                longitude != null && longitude in -180.0..180.0

    fun matches(query: String): Boolean =
        lastName.contains(query, ignoreCase = true) ||
                firstName.contains(query, ignoreCase = true) ||
                fullName.contains(query, ignoreCase = true) ||
                phone.contains(query)
}