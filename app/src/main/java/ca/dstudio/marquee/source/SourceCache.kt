package ca.dstudio.marquee.source

import android.content.Context
import android.util.Log
import java.io.File
import java.security.MessageDigest

/**
 * The downloaded (or painted) source pictures, in `cacheDir/sources/<sha1 of the reference>.<ext>`,
 * kept up to [MAX_BYTES], the oldest used going first. A change of look recomposes from here
 * without the network.
 */
class SourceCache(context: Context) {

    private val dir = File(context.applicationContext.cacheDir, "sources")

    /** Where [ref] lives (or will), whether it is there or not. */
    fun fileFor(ref: String, ext: String): File {
        dir.mkdirs()
        return File(dir, "${sha1(ref)}.$ext")
    }

    /** [file] if it is there, marked as just used. */
    fun hit(file: File): File? {
        if (!file.isFile || file.length() == 0L) return null
        file.setLastModified(System.currentTimeMillis())
        return file
    }

    /** Drops the least recently used files until the cache fits [MAX_BYTES]. */
    fun trim() {
        val files = dir.listFiles()?.filter { it.isFile } ?: return
        var total = files.sumOf { it.length() }
        if (total <= MAX_BYTES) return
        for (f in files.sortedBy { it.lastModified() }) {
            if (total <= MAX_BYTES) break
            val size = f.length()
            if (f.delete()) total -= size
        }
        Log.i(TAG, "source cache trimmed to ${total / 1_048_576} MB")
    }

    companion object {
        private const val TAG = "Marquee"
        const val MAX_BYTES = 300L * 1024 * 1024

        private const val HEX = "0123456789abcdef"

        /** SHA-1 of [text] in lower-case hex. */
        fun sha1(text: String): String {
            val bytes = MessageDigest.getInstance("SHA-1").digest(text.toByteArray())
            val out = StringBuilder(bytes.size * 2)
            for (b in bytes) {
                val v = b.toInt() and 0xFF
                out.append(HEX[v ushr 4]).append(HEX[v and 0x0F])
            }
            return out.toString()
        }
    }
}
