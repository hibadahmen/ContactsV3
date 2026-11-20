package tn.isetdjerba.contactsv3.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

class ContactDao(private val helper: ContactDbHelper) {

    fun insert(contact: Contact): Long =
        helper.writableDatabase.insert(
            ContactContract.TABLE,
            null,
            toValues(contact)
        )

    fun update(contact: Contact): Int =
        helper.writableDatabase.update(
            ContactContract.TABLE,
            toValues(contact),
            "${ContactContract.ID} = ?",
            arrayOf(contact.id.toString())
        )

    fun delete(id: Long): Int =
        helper.writableDatabase.delete(
            ContactContract.TABLE,
            "${ContactContract.ID} = ?",
            arrayOf(id.toString())
        )

    fun getAll(): List<Contact> = query()

    fun getById(id: Long): Contact? =
        query(
            "${ContactContract.ID} = ?",
            arrayOf(id.toString())
        ).firstOrNull()

    fun getFavorites(): List<Contact> =
        query("$COLUMN_FAVORITE = ?", arrayOf("1"))

    fun getFamily(): List<Contact> =
        query("$COLUMN_FAMILY = ?", arrayOf("1"))

    fun search(texte: String): List<Contact> {
        val term = texte.trim()

        if (term.isEmpty()) return getAll()

        val selection = """
            instr(lower(${ContactContract.LAST_NAME}), lower(?)) > 0
            OR instr(lower(${ContactContract.FIRST_NAME}), lower(?)) > 0
            OR instr(${ContactContract.PHONE}, ?) > 0
        """.trimIndent()

        return query(selection, arrayOf(term, term, term))
    }

    private fun query(
        selection: String? = null,
        selectionArgs: Array<String>? = null
    ): List<Contact> {

        val projection = arrayOf(
            ContactContract.ID,
            ContactContract.LAST_NAME,
            ContactContract.FIRST_NAME,
            ContactContract.PHONE,
            ContactContract.EMAIL,
            ContactContract.PHOTO_URI,
            ContactContract.LATITUDE,
            ContactContract.LONGITUDE,
            COLUMN_FAVORITE,
            COLUMN_FAMILY
        )

        return helper.readableDatabase.query(
            ContactContract.TABLE,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            ContactContract.DEFAULT_ORDER
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.toContact())
                }
            }
        }
    }


    private fun Cursor.toContact(): Contact =
        Contact(
            id = getLong(
                getColumnIndexOrThrow(ContactContract.ID)
            ),
            lastName = getString(
                getColumnIndexOrThrow(ContactContract.LAST_NAME)
            ),
            firstName = getString(
                getColumnIndexOrThrow(ContactContract.FIRST_NAME)
            ),
            phone = getString(
                getColumnIndexOrThrow(ContactContract.PHONE)
            ),
            email = getString(
                getColumnIndexOrThrow(ContactContract.EMAIL)
            ),
            photoUri = getString(
                getColumnIndexOrThrow(ContactContract.PHOTO_URI)
            ),
            latitude = readNullableDouble(ContactContract.LATITUDE),
            longitude = readNullableDouble(ContactContract.LONGITUDE),
            favorite = getInt(
                getColumnIndexOrThrow(COLUMN_FAVORITE)
            ) == 1,
            family = getInt(
                getColumnIndexOrThrow(COLUMN_FAMILY)
            ) == 1
        )

    private fun Cursor.readNullableDouble(column: String): Double? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getDouble(index)
    }

    companion object {

        private const val COLUMN_FAVORITE = "is_favorite"
        private const val COLUMN_FAMILY = "is_family"

        internal fun createAndSeed(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE ${ContactContract.TABLE} (
                    ${ContactContract.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                    ${ContactContract.LAST_NAME} TEXT NOT NULL,
                    ${ContactContract.FIRST_NAME} TEXT NOT NULL,
                    ${ContactContract.PHONE} TEXT NOT NULL,
                    ${ContactContract.EMAIL} TEXT,
                    ${ContactContract.PHOTO_URI} TEXT,
                    ${ContactContract.LATITUDE} REAL,
                    ${ContactContract.LONGITUDE} REAL,
                    $COLUMN_FAVORITE INTEGER NOT NULL DEFAULT 0,
                    $COLUMN_FAMILY INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )

            val sampleContacts = listOf(
                Contact(
                    lastName = "Ben Salem",
                    firstName = "Amira",
                    phone = "20000101",
                    email = "amira@example.com",
                    latitude = 33.875,
                    longitude = 10.857
                ),
                Contact(
                    lastName = "Trabelsi",
                    firstName = "Youssef",
                    phone = "21000202",
                    email = "youssef@example.com",
                    latitude = 36.8065,
                    longitude = 10.1815
                ),
                Contact(
                    lastName = "Mansour",
                    firstName = "Inès",
                    phone = "22000303"
                ),
                Contact(
                    lastName = "Jaziri",
                    firstName = "Sami",
                    phone = "+216 23 000 404",
                    latitude = 35.8256,
                    longitude = 10.6369
                ),
                Contact(
                    lastName = "Haddad",
                    firstName = "Nour",
                    phone = "24000505"
                )
            )

            sampleContacts.forEach { contact ->
                db.insertOrThrow(
                    ContactContract.TABLE,
                    null,
                    toValues(contact)
                )
            }
        }

        private fun toValues(contact: Contact): ContentValues =
            ContentValues().apply {
                put(ContactContract.LAST_NAME, contact.lastName)
                put(ContactContract.FIRST_NAME, contact.firstName)
                put(ContactContract.PHONE, contact.phone)
                put(ContactContract.EMAIL, contact.email)
                put(ContactContract.PHOTO_URI, contact.photoUri)
                put(ContactContract.LATITUDE, contact.latitude)
                put(ContactContract.LONGITUDE, contact.longitude)
                put(COLUMN_FAVORITE, if (contact.favorite) 1 else 0)
                put(COLUMN_FAMILY, if (contact.family) 1 else 0)
            }
    }
}