package tn.isetdjerba.contactsv3.data

object ContactContract {
    const val TABLE = "contacts"
    const val ID = "_id"
    const val LAST_NAME = "nom"
    const val FIRST_NAME = "prenom"
    const val PHONE = "telephone"
    const val EMAIL = "email"
    const val PHOTO_URI = "photo_uri"
    const val LATITUDE = "latitude"
    const val LONGITUDE = "longitude"

    val columns = arrayOf(
        ID, LAST_NAME, FIRST_NAME, PHONE, EMAIL,
        PHOTO_URI, LATITUDE, LONGITUDE
    )

    const val DEFAULT_ORDER = "$LAST_NAME COLLATE NOCASE, $FIRST_NAME COLLATE NOCASE"
}