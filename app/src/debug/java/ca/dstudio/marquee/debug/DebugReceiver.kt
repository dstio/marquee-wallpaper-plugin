package ca.dstudio.marquee.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import ca.dstudio.marquee.BuildConfig
import ca.dstudio.marquee.catalog.Lang
import ca.dstudio.marquee.compose.Layout
import ca.dstudio.marquee.compose.Look
import ca.dstudio.marquee.compose.TextPosition
import ca.dstudio.marquee.settings.MarqueeSettings
import ca.dstudio.marquee.source.ListKey
import ca.dstudio.marquee.work.Generator
import ca.dstudio.marquee.work.UpdateWorker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Debug commands, answered in the broadcast's result data:
 *
 * ```
 * adb shell am broadcast -n ca.dstudio.marquee/.debug.DebugReceiver --es cmd <cmd> [--es key <k> --es value <v>]
 * ```
 *
 * - `generate`: a full run.
 * - `recompose`: the last set redrawn with the current look.
 * - `set`: one setting; a look setting recomposes, `interval_hours` reschedules.
 * - `preview [--es value <n>]`: the plugin's preview picture from made-up title n, to `files/preview.png`.
 * - `dump`: the settings, the set, the last report's counts, and whether a TMDB token is built in
 *   (yes or no, never the token).
 *
 * The receiver requires `android.permission.DUMP`, which only the shell holds.
 */
class DebugReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        Thread({
            val answer = try {
                handle(app, intent)
            } catch (e: Exception) {
                "error: ${e.javaClass.simpleName}"
            }
            pending.resultData = answer
            pending.finish()
        }, "marquee-debug").start()
    }

    private fun handle(context: Context, intent: Intent): String {
        val generator = Generator.get(context)
        return when (val cmd = intent.getStringExtra("cmd")) {
            "generate" -> run(generator, Generator.Kind.FULL)
            "recompose" -> run(generator, Generator.Kind.RECOMPOSE)
            "set" -> {
                val key = intent.getStringExtra("key") ?: return "set: --es key <k> --es value <v>"
                val value = intent.getStringExtra("value") ?: return "set: --es key <k> --es value <v>"
                generator.settings.set(key, value)?.let { return it }
                when (key) {
                    in MarqueeSettings.LOOK_KEYS -> "$key = $value; " + run(generator, Generator.Kind.RECOMPOSE)
                    MarqueeSettings.INTERVAL_HOURS -> {
                        UpdateWorker.schedule(context, generator.settings.read().intervalHours, replace = true)
                        "$key = $value; rescheduled"
                    }
                    else -> "$key = $value; takes effect at the next generate"
                }
            }
            "dump" -> dump(generator)
            "preview" -> preview(context, generator, intent.getStringExtra("value")?.toIntOrNull() ?: 0)
            else -> "unknown cmd '$cmd': generate | recompose | set | dump | preview"
        }
    }

    /**
     * The plugin's preview picture (`wallpaperPreview`, 960 x 540): made-up title [index] of the
     * sample Trending list in Cinematic at the top left, written losslessly to `files/preview.png`.
     */
    private fun preview(context: Context, generator: Generator, index: Int): String {
        val source = generator.sourceNamed("sample") ?: return "preview: no sample source"
        val look = Look(Layout.CINEMATIC, TextPosition.TOP_LEFT, 3, true)
        val bitmap = generator.composeTitle(source, ListKey.TRENDING, index, Lang.of("en-US"), look, 960, 540)
            ?: return "preview: nothing drawn"
        val file = File(context.filesDir, "preview.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return "preview: ${file.path}"
    }

    /** Starts a run and waits up to [WAIT_S] s for it, under the broadcast's own time limit. */
    private fun run(generator: Generator, kind: Generator.Kind): String {
        val started = System.currentTimeMillis()
        val future = generator.request(kind)
        return try {
            val ok = future.get(WAIT_S, TimeUnit.SECONDS)
            "${kind.name.lowercase()}: ${if (ok) "done" else "made nothing"} in ${System.currentTimeMillis() - started} ms"
        } catch (e: TimeoutException) {
            "${kind.name.lowercase()}: still running after $WAIT_S s (see dump)"
        }
    }

    private fun dump(generator: Generator): String = buildString {
        appendLine("token: ${if (BuildConfig.TMDB_TOKEN.isNotBlank()) "yes" else "no"}")
        appendLine("running: ${generator.runningKind()?.name?.lowercase() ?: "no"}")
        appendLine("settings:")
        generator.settings.dump().forEach { (k, v) -> appendLine("  $k = $v") }
        val set = generator.store.current()
        if (set == null) {
            appendLine("set: none")
        } else {
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
            appendLine("set: ${set.entries.size} from ${set.source} at ${set.width}x${set.height}, ${set.lang}, made ${time.format(Date(set.createdAt))}, fetched ${time.format(Date(set.fetchedAt))}")
            set.entries.forEachIndexed { i, e -> appendLine("  ${i + 1}. ${e.id} ${e.list} ${e.title}") }
        }
        val report = generator.store.report()
        if (report == null) {
            appendLine("report: none")
        } else {
            appendLine("report (${report.source}):")
            for (list in report.lists) {
                val counts = list.candidates.groupingBy { it.status }.eachCount().entries.joinToString(", ") { "${it.key} ${it.value}" }
                appendLine("  ${list.key}: ${list.candidates.size} candidates${list.error?.let { ", list $it" } ?: ""}; $counts")
            }
        }
    }

    private companion object {
        const val WAIT_S = 45L
    }
}
