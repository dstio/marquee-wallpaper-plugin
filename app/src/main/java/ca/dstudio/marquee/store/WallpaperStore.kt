package ca.dstudio.marquee.store

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import ca.dstudio.marquee.catalog.RunReport
import ca.dstudio.marquee.catalog.TitleDetails
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/** One wallpaper of the current set. */
@Serializable
data class SetEntry(
    /** Names the picture's content; the file is `<id>.jpg`. */
    val id: String,
    val file: String,
    val title: String,
    /** The title's page on TMDB, or null for a made-up sample. */
    val url: String? = null,
    val list: String,
    val listLabel: String,
    val details: TitleDetails,
)

/** The set the launcher is given, in the order it is shown. */
@Serializable
data class StoredSet(
    val createdAt: Long,
    /** When the titles were last fetched; a recompose keeps it, so the schedule does not move. */
    val fetchedAt: Long,
    val source: String,
    val width: Int,
    val height: Int,
    val lang: String,
    val entries: List<SetEntry>,
)

/**
 * The pictures (`filesDir/wallpapers/<id>.jpg`, which the provider serves) and the set that orders
 * them (`filesDir/set.json`, which it does not). A picture is written as `.tmp` and renamed; the
 * set is written last, the same way, and only then are pictures it no longer names deleted - so
 * the launcher never meets a set naming a missing file.
 */
class WallpaperStore(context: Context) {

    private val files = context.applicationContext.filesDir
    val dir = File(files, DIR)
    private val setFile = File(files, "set.json")
    private val reportFile = File(files, "report.json")

    @Synchronized
    fun current(): StoredSet? = try {
        if (setFile.isFile) JSON.decodeFromString<StoredSet>(setFile.readText()) else null
    } catch (e: Exception) {
        Log.w(TAG, "set.json unreadable: ${e.javaClass.simpleName}")
        null
    }

    fun has(id: String): Boolean = File(dir, fileName(id)).let { it.isFile && it.length() > 0 }

    /** Writes [bitmap] as `<id>.jpg`, JPEG quality 90. */
    @Throws(IOException::class)
    fun writePicture(id: String, bitmap: Bitmap) {
        dir.mkdirs()
        val target = File(dir, fileName(id))
        val tmp = File(dir, fileName(id) + ".tmp")
        try {
            tmp.outputStream().use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)) throw IOException("could not encode $id")
            }
            if (!tmp.renameTo(target)) throw IOException("could not move $id into place")
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }

    /** Makes [set] the current one, then deletes the pictures it does not name. */
    @Synchronized
    @Throws(IOException::class)
    fun commit(set: StoredSet) {
        writeAtomically(setFile, JSON.encodeToString(set))
        val keep = set.entries.map { it.file }.toSet()
        dir.listFiles()?.filter { it.isFile && it.name !in keep }?.forEach { it.delete() }
    }

    @Throws(IOException::class)
    fun writeReport(report: RunReport) = writeAtomically(reportFile, JSON.encodeToString(report))

    fun report(): RunReport? = try {
        if (reportFile.isFile) JSON.decodeFromString<RunReport>(reportFile.readText()) else null
    } catch (e: Exception) {
        null
    }

    private fun writeAtomically(file: File, text: String) {
        val tmp = File(file.path + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            tmp.delete()
            throw IOException("could not move ${file.name} into place")
        }
    }

    companion object {
        private const val TAG = "Marquee"

        /** The folder the provider serves (its default root). */
        const val DIR = "wallpapers"

        fun fileName(id: String) = "$id.jpg"

        private val JSON = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }
    }
}
