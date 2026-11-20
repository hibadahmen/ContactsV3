package tn.isetdjerba.contactsv3.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ContactDbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        ContactDao.createAndSeed(db)
        ajouterColonnesGroupes(db)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        ajouterColonnesGroupes(db)
    }

    private fun ajouterColonnesGroupes(db: SQLiteDatabase) {
        val colonnes = mutableSetOf<String>()

        db.rawQuery("PRAGMA table_info(contacts)", null).use { cursor ->
            val indexNom = cursor.getColumnIndexOrThrow("name")

            while (cursor.moveToNext()) {
                colonnes.add(cursor.getString(indexNom))
            }
        }

        if ("is_favorite" !in colonnes) {
            db.execSQL(
                """
                ALTER TABLE contacts
                ADD COLUMN is_favorite INTEGER NOT NULL DEFAULT 0
                """.trimIndent()
            )
        }

        if ("is_family" !in colonnes) {
            db.execSQL(
                """
                ALTER TABLE contacts
                ADD COLUMN is_family INTEGER NOT NULL DEFAULT 0
                """.trimIndent()
            )
        }
    }

    companion object {
        private const val DATABASE_NAME = "contacts.db"
        private const val DATABASE_VERSION = 3

        @Volatile
        private var instance: ContactDbHelper? = null

        fun getInstance(context: Context): ContactDbHelper =
            instance ?: synchronized(this) {
                instance ?: ContactDbHelper(context.applicationContext).also {
                    instance = it
                }
            }
    }
}