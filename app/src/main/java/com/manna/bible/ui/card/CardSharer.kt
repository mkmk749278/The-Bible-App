package com.manna.bible.ui.card

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Saves a rendered verse card to the app's cache and fires a share chooser
 * (WhatsApp-ready), using a [FileProvider] so the image is shareable to other apps.
 *
 * The FileProvider authority (`<applicationId>.fileprovider`) and the
 * `images/` cache path are declared in the manifest + `res/xml/file_paths.xml`.
 */
object CardSharer {

    /** Writes [bitmap] to cache and launches a share chooser for it. */
    fun share(context: Context, bitmap: Bitmap) {
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "verse_card.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
