package ca.dstudio.marquee.provider

import android.util.Log
import ca.dstudio.atvlauncher.wallpaper.api.WallpaperBatch
import ca.dstudio.atvlauncher.wallpaper.api.WallpaperContract
import ca.dstudio.atvlauncher.wallpaper.api.WallpaperEnvironment
import ca.dstudio.atvlauncher.wallpaper.api.WallpaperItem
import ca.dstudio.atvlauncher.wallpaper.api.WallpaperProviderService
import ca.dstudio.marquee.store.StoredSet
import ca.dstudio.marquee.work.Generator
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Marquee's door for a launcher on the wallpaper-api protocol. Every request is answered with
 * the current set (or "nothing yet" while the first one is made) - after the settings close, with
 * the set their changes make, once it is drawn; a finished run pushes its new set. Nothing is
 * read, fetched or drawn on the main or binder thread.
 */
class MarqueeWallpaperService : WallpaperProviderService() {

    private lateinit var generator: Generator

    private val waiter = Executors.newSingleThreadExecutor { Thread(it, "marquee-answer") }

    @Volatile private var requestedAt = 0L
    @Volatile private var pushedAt = 0L

    private val onSet = Generator.Listener { set -> send(batchOf(set)) }

    private fun send(batch: WallpaperBatch) {
        pushedAt = System.currentTimeMillis()
        push(batch)
    }

    override fun onCreate() {
        super.onCreate()
        generator = Generator.get(this)
        generator.addListener(onSet)
    }

    override fun onDestroy() {
        generator.removeListener(onSet)
        waiter.shutdownNow()
        super.onDestroy()
    }

    override fun onAttach(env: WallpaperEnvironment) {
        Log.i(TAG, "attached: ${env.widthPx}x${env.heightPx}, api ${env.apiVersion}")
        if (env.widthPx > 0 && env.heightPx > 0) {
            generator.io.execute {
                generator.settings.screenWidth = env.widthPx
                generator.settings.screenHeight = env.heightPx
            }
        }
    }

    override fun onRequest(reason: Int) {
        requestedAt = System.currentTimeMillis()
        val pending = if (reason == WallpaperContract.REASON_SETTINGS_CLOSED) generator.pending() else null
        if (pending == null) {
            generator.io.execute { answer(reason) }
            return
        }
        // The settings just closed and their changes are being drawn: answer with the result, so
        // the new pictures arrive now and not at the next timed ask.
        Log.i(TAG, "request $reason: waiting for the run the settings started")
        waiter.execute {
            try {
                pending.get(SETTINGS_WAIT_MS, TimeUnit.MILLISECONDS)
            } catch (e: Exception) {
                Log.i(TAG, "the run is still going; answering with the current set")
            }
            // A finished run has already pushed its set, which answers this request.
            if (pushedAt < requestedAt) generator.io.execute { answer(reason) }
        }
    }

    override fun onShown(itemId: String) {
        Log.d(TAG, "shown $itemId")
    }

    override fun onFailed(itemId: String, why: Int) {
        Log.w(TAG, "the launcher could not use $itemId: $why")
    }

    /** On the io thread: push what there is, and start a run when it is missing, due or stale. */
    private fun answer(reason: Int) {
        val set = generator.store.current()
        if (set == null || set.entries.isEmpty()) {
            Log.i(TAG, "request $reason: nothing yet")
            send(WallpaperBatch(emptyList(), WallpaperContract.MIN_REQUEST_INTERVAL_MS))
            if (System.currentTimeMillis() - generator.lastFailureAt > RETRY_AFTER_FAILURE_MS) {
                generator.request(Generator.Kind.FULL)
            }
            return
        }
        Log.i(TAG, "request $reason: ${set.entries.size} pictures")
        send(batchOf(set))
        val s = generator.settings.read()
        val (w, h) = generator.outputSize(s)
        when {
            (System.currentTimeMillis() >= generator.dueAt(set, s) || set.lang != generator.lang(s).tag) &&
                System.currentTimeMillis() - generator.lastFullStartedAt > RETRY_AFTER_FAILURE_MS ->
                generator.request(Generator.Kind.FULL)
            set.width != w || set.height != h -> generator.request(Generator.Kind.RECOMPOSE)
        }
    }

    private fun batchOf(set: StoredSet): WallpaperBatch {
        val items = set.entries.take(WallpaperContract.MAX_ITEMS).map { e ->
            WallpaperItem(
                id = e.id,
                uri = "content://$packageName.wallpapers/${e.file}",
                type = WallpaperContract.TYPE_IMAGE,
                mode = WallpaperContract.MODE_FILL,
                title = e.title,
                sourceUrl = e.url,
            )
        }
        val untilDue = generator.dueAt(set, generator.settings.read()) - System.currentTimeMillis()
        return WallpaperBatch(items, maxOf(WallpaperContract.MIN_REQUEST_INTERVAL_MS, untilDue))
    }

    private companion object {
        const val TAG = "Marquee"

        /** After a run that made nothing, the next request waits this long before trying again. */
        const val RETRY_AFTER_FAILURE_MS = 5 * 60_000L

        /** How long a settings-closed answer waits for the run the settings started (the launcher allows 30 s). */
        const val SETTINGS_WAIT_MS = 25_000L
    }
}
