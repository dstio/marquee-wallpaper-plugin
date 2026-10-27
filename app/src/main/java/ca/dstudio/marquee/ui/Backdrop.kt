package ca.dstudio.marquee.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import ca.dstudio.marquee.compose.Composer
import ca.dstudio.marquee.compose.Look
import ca.dstudio.marquee.work.Generator
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * The picture behind the panel: the current set's first wallpaper, or a look being tried, composed
 * afresh. Everything is decoded or drawn on its own thread at the screen's size; only the swap
 * happens on the main thread.
 *
 * A look change waits [DEBOUNCE_MS] for the next one, and every request bumps a generation: work
 * for an older one is skipped before it starts and thrown away if it finishes late, so the picture
 * always ends on the newest request.
 */
class Backdrop(
    private val view: ImageView,
    private val generator: Generator,
    private val width: Int,
    private val height: Int,
) {
    private val main = Handler(Looper.getMainLooper())
    private val worker: ExecutorService = Executors.newSingleThreadExecutor { Thread(it, "marquee-preview") }
    private val generation = AtomicInteger()
    private var pendingLook: Runnable? = null

    /** The id of the wallpaper on show, when it is the set's own picture. */
    @Volatile
    var shownId: String? = null
        private set

    /** Shows the current set's first picture; nothing changes when there is no set. */
    fun showSet() {
        cancelPending()
        val gen = generation.incrementAndGet()
        worker.execute {
            if (gen != generation.get()) return@execute
            val entry = generator.store.current()?.entries?.firstOrNull() ?: return@execute
            val file = File(generator.store.dir, entry.file)
            val bitmap = cover(file) ?: return@execute
            deliver(gen, bitmap, entry.id)
        }
    }

    /** Recomposes the preview with [look], after a short pause in case another change follows. */
    fun showLook(look: Look) {
        cancelPending()
        val gen = generation.incrementAndGet()
        val task = Runnable {
            pendingLook = null
            worker.execute {
                if (gen != generation.get()) return@execute
                val bitmap = try {
                    generator.composePreview(look, width, height)
                } catch (e: Exception) {
                    Log.w(TAG, "preview failed: ${e.javaClass.simpleName}")
                    null
                } catch (e: OutOfMemoryError) {
                    null
                } ?: return@execute
                deliver(gen, bitmap, null)
            }
        }
        pendingLook = task
        main.postDelayed(task, DEBOUNCE_MS)
    }

    fun release() {
        cancelPending()
        generation.incrementAndGet()
        worker.shutdown()
    }

    private fun cancelPending() {
        pendingLook?.let(main::removeCallbacks)
        pendingLook = null
    }

    private fun deliver(gen: Int, bitmap: Bitmap, id: String?) {
        if (gen != generation.get()) {
            bitmap.recycle()
            return
        }
        main.post {
            if (gen != generation.get()) return@post
            view.setImageBitmap(bitmap)
            shownId = id
        }
    }

    /** [file] centre-cropped to the screen, decoded no larger than it needs to be. */
    private fun cover(file: File): Bitmap? {
        if (!file.isFile) return null
        val decoded = Composer.decodeCover(file, width, height) ?: return null
        if (decoded.width == width && decoded.height == height) return decoded
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Composer.drawCover(Canvas(out), decoded, RectF(0f, 0f, width.toFloat(), height.toFloat()))
        decoded.recycle()
        return out
    }

    private companion object {
        const val TAG = "Marquee"
        const val DEBOUNCE_MS = 150L
    }
}
