package tn.isetdjerba.contactsv3

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import java.io.IOException

fun ImageView.showPhoto(value: String?) {
    setImageResource(R.drawable.ic_contact)

    val uri = value?.let(Uri::parse) ?: return
    val resolver = context.contentResolver

    try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }

        if (options.outWidth <= 0 || options.outHeight <= 0) return

        options.inSampleSize = 1
        while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 512) {
            options.inSampleSize *= 2
        }

        options.inJustDecodeBounds = false
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }

        if (bitmap != null) setImageBitmap(bitmap)
    } catch (_: IOException) {
        // Garde l’icône si le fichier ne peut pas être lu.
    } catch (_: SecurityException) {
        // Garde l’icône si l’accès à la photo a été révoqué.
    }
}