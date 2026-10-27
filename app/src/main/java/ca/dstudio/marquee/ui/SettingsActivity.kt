package ca.dstudio.marquee.ui

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import ca.dstudio.marquee.compose.Look
import ca.dstudio.marquee.settings.MarqueeSettings
import ca.dstudio.marquee.ui.explore.ExploreView
import ca.dstudio.marquee.ui.pages.LookPage
import ca.dstudio.marquee.ui.pages.SetSummary
import ca.dstudio.marquee.ui.pages.SettingsHost
import ca.dstudio.marquee.ui.pages.StartPage
import ca.dstudio.marquee.ui.pages.Words
import ca.dstudio.marquee.ui.panel.Page
import ca.dstudio.marquee.ui.panel.PanelView
import ca.dstudio.marquee.ui.panel.px
import ca.dstudio.marquee.work.Generator
import ca.dstudio.marquee.work.UpdateWorker

/**
 * Marquee's settings, as the launcher opens them: full screen, the current wallpaper behind a side
 * panel on the right. Nothing in the starting intent is read.
 *
 * Changes are kept as they are made. What they cost is paid once: a look change redraws the set
 * when the Look page is left; a change to the lists, the filters or the language makes a new set
 * when the settings close; a new picture size redraws the set then.
 */
class SettingsActivity : Activity(), SettingsHost {

    private val main = Handler(Looper.getMainLooper())
    private lateinit var generator: Generator
    private lateinit var backdropView: ImageView
    private lateinit var backdrop: Backdrop
    private lateinit var panel: PanelView
    private lateinit var root: FrameLayout
    private var explore: ExploreView? = null

    override val context: Context get() = this
    override val settings: MarqueeSettings get() = generator.settings

    override var set: SetSummary? = null
        private set
    override var status: Generator.Status = Generator.Status(null, 0, 0, null, null, 0L)
        private set
    override var updateRequestedAt = 0L
        private set

    private var lookPending = false
    private var titlesPending = false
    private var sizePending = false
    private var polling = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        generator = Generator.get(this)

        @Suppress("DEPRECATION")
        val metrics = DisplayMetrics().also { windowManager.defaultDisplay.getRealMetrics(it) }
        val width = maxOf(metrics.widthPixels, metrics.heightPixels)
        val height = minOf(metrics.widthPixels, metrics.heightPixels)

        root = FrameLayout(this)
        backdropView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            isFocusable = false
        }
        root.addView(backdropView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        panel = PanelView(this) { finish() }
        root.addView(panel, FrameLayout.LayoutParams(px(640f), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END).apply {
            topMargin = px(48f)
            bottomMargin = px(48f)
            rightMargin = px(48f)
        })
        setContentView(root)

        backdrop = Backdrop(backdropView, generator, width, height)
        backdrop.showSet()
        panel.push(StartPage(this))
    }

    override fun onStart() {
        super.onStart()
        polling = true
        poll()
    }

    override fun onPause() {
        // Before the launcher comes back and asks: its question then finds the run already started.
        apply()
        super.onPause()
    }

    override fun onStop() {
        polling = false
        main.removeCallbacks(pollTask)
        super.onStop()
    }

    override fun onDestroy() {
        backdrop.release()
        super.onDestroy()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        explore?.let { if (it.handleKey(event)) return true else return super.dispatchKeyEvent(event) }
        if (panel.handleKey(event)) return true
        return super.dispatchKeyEvent(event)
    }

    // --- The store and the generator, looked at off the main thread ---

    private val pollTask = Runnable { poll() }

    private fun poll() {
        if (!polling) return
        generator.io.execute {
            val current = generator.store.current()
            val summary = current?.takeIf { it.entries.isNotEmpty() }?.let { SetSummary(it.entries.size, it.createdAt) }
            val firstId = current?.entries?.firstOrNull()?.id
            val now = generator.status()
            main.post { took(summary, firstId, now) }
        }
    }

    private fun took(summary: SetSummary?, firstId: String?, now: Generator.Status) {
        if (!polling) return
        val newSet = summary != null && summary != set
        val changed = summary != set || now != status
        set = summary
        status = now
        // A new set shows behind the panel, unless a look is being tried on the Look page.
        if (newSet && panel.page !is LookPage && firstId != backdrop.shownId) backdrop.showSet()
        if (changed) panel.refresh()
        main.postDelayed(pollTask, if (now.running != null) POLL_RUNNING_MS else POLL_IDLE_MS)
    }

    // --- SettingsHost ---

    override fun open(page: Page) = panel.push(page)

    override fun refresh() = panel.refresh()

    override fun preview(look: Look) = backdrop.showLook(look)

    override fun lookChanged() {
        lookPending = true
    }

    override fun leftLook() {
        if (lookPending) {
            lookPending = false
            generator.request(Generator.Kind.RECOMPOSE)
        }
    }

    override fun titlesChanged() {
        titlesPending = true
    }

    override fun sizeChanged() {
        sizePending = true
    }

    override fun intervalChanged() {
        UpdateWorker.schedule(this, settings.read().intervalHours, replace = true)
    }

    override fun updateNow() {
        updateRequestedAt = System.currentTimeMillis()
        titlesPending = false
        generator.request(Generator.Kind.FULL)
        main.removeCallbacks(pollTask)
        poll()
        panel.refresh()
    }

    override fun openExplore() {
        if (explore != null) return
        val view = ExploreView(this, generator.io) { closeExplore() }
        explore = view
        panel.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        root.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        generator.io.execute {
            val report = generator.store.report()
            val source = report?.let { generator.sourceNamed(it.source) }
            main.post {
                if (explore === view) view.show(report, source, report?.let { Words.age(this, it.createdAt) })
            }
        }
    }

    private fun closeExplore() {
        val view = explore ?: return
        explore = null
        root.removeView(view)
        panel.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        panel.refresh()
        panel.restoreFocus()
    }

    /** Starts the one run the changes made here call for, if any. */
    private fun apply() {
        when {
            titlesPending -> generator.request(Generator.Kind.FULL)
            lookPending || sizePending -> generator.request(Generator.Kind.RECOMPOSE)
            else -> return
        }
        titlesPending = false
        lookPending = false
        sizePending = false
    }

    private companion object {
        const val POLL_IDLE_MS = 2_000L
        const val POLL_RUNNING_MS = 500L
    }
}
