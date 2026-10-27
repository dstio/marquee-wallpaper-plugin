package ca.dstudio.marquee.work

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import ca.dstudio.marquee.BuildConfig
import ca.dstudio.marquee.catalog.Lang
import ca.dstudio.marquee.catalog.ListInput
import ca.dstudio.marquee.catalog.Media
import ca.dstudio.marquee.catalog.Picker
import ca.dstudio.marquee.catalog.RunReport
import ca.dstudio.marquee.catalog.Skip
import ca.dstudio.marquee.catalog.TitleDetails
import ca.dstudio.marquee.compose.Composer
import ca.dstudio.marquee.compose.Labels
import ca.dstudio.marquee.compose.Layout
import ca.dstudio.marquee.compose.Look
import ca.dstudio.marquee.compose.Scene
import ca.dstudio.marquee.settings.MarqueeSettings
import ca.dstudio.marquee.settings.Settings
import ca.dstudio.marquee.source.ImageKind
import ca.dstudio.marquee.source.ListKey
import ca.dstudio.marquee.source.Source
import ca.dstudio.marquee.source.SourceCache
import ca.dstudio.marquee.source.sample.SampleSource
import ca.dstudio.marquee.source.tmdb.TmdbSource
import ca.dstudio.marquee.store.SetEntry
import ca.dstudio.marquee.store.StoredSet
import ca.dstudio.marquee.store.WallpaperStore
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import kotlin.math.min

/**
 * Makes the set, one run at a time on its own thread. A full run reads the lists, picks the titles,
 * fetches their details and pictures (through the source cache), composes each wallpaper, stores
 * the set and writes the report. A recompose run - the look changed - redraws the last set's titles
 * from the source cache without the lists. A second request for a run already under way joins it;
 * any other waits its turn behind it.
 */
class Generator private constructor(context: Context) {

    enum class Kind { FULL, RECOMPOSE }

    /** How a run ended. */
    enum class Outcome {
        DONE,

        /** No list could be read and the device has no connection. */
        NO_CONNECTION,

        /** No list could be read, though there is a connection. */
        LISTS_FAILED,

        /** The lists were read but no title passed the filters. */
        NOTHING_PICKED,

        /** A build with no way to reach TMDB. */
        NO_SOURCE,

        FAILED,
    }

    /**
     * Where the generator is: the run under way (or null) and, while it composes, the picture it is
     * on ([step] of [steps]; 0 before composing starts); then how the last run ended.
     */
    data class Status(
        val running: Kind?,
        val step: Int,
        val steps: Int,
        val lastKind: Kind?,
        val lastOutcome: Outcome?,
        val finishedAt: Long,
    )

    fun interface Listener {
        /** A run stored a new set. Called on the generator's thread. */
        fun onSet(set: StoredSet)
    }

    private val app = context.applicationContext
    val settings = MarqueeSettings(app)
    val store = WallpaperStore(app)
    private val cache = SourceCache(app)
    private val listeners = CopyOnWriteArraySet<Listener>()

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { Thread(it, "marquee-generator") }

    /** For quick reads off the main and binder threads (the set, for an answer). */
    val io: ExecutorService = Executors.newSingleThreadExecutor { Thread(it, "marquee-io") }

    private class Job(val kind: Kind, val task: FutureTask<Boolean>)

    private val lock = Any()
    private var running: Job? = null
    private var queued: Job? = null

    /** When the last run ended without a set to show, for the service's backoff. */
    @Volatile
    var lastFailureAt = 0L
        private set

    /** When the last full run started, so a due set that cannot be renewed is not retried every minute. */
    @Volatile
    var lastFullStartedAt = 0L
        private set

    @Volatile private var step = 0
    @Volatile private var steps = 0
    @Volatile private var lastKind: Kind? = null
    @Volatile private var lastOutcome: Outcome? = null
    @Volatile private var finishedAt = 0L

    fun addListener(l: Listener) = listeners.add(l)
    fun removeListener(l: Listener) = listeners.remove(l)

    /** The run under way, or null. */
    fun runningKind(): Kind? = synchronized(lock) { running?.kind }

    /** The last run to finish of those under way or waiting, or null when there is none. */
    fun pending(): Future<Boolean>? = synchronized(lock) { queued?.task ?: running?.task }

    fun status(): Status = Status(runningKind(), step, steps, lastKind, lastOutcome, finishedAt)

    /** The language runs fetch in: the Language setting, else the device's. */
    fun lang(s: Settings = settings.read()): Lang = Lang.of(s.language)

    /** Starts (or joins) a run of [kind]; the future answers whether it stored a set. */
    fun request(kind: Kind): Future<Boolean> = synchronized(lock) {
        val r = running
        val q = queued
        when {
            // A full run under way reads everything afresh: join it. A recompose under way may
            // have read the look already, so another one waits behind it.
            r != null && r.kind == Kind.FULL && kind == Kind.FULL -> r.task
            q != null && (q.kind == kind || q.kind == Kind.FULL) -> q.task
            else -> submit(kind).task
        }
    }

    private fun submit(kind: Kind): Job {
        lateinit var job: Job
        val task = FutureTask {
            synchronized(lock) {
                if (queued === job) queued = null
                running = job
            }
            try {
                run(kind)
            } finally {
                synchronized(lock) { if (running === job) running = null }
            }
        }
        job = Job(kind, task)
        synchronized(lock) { queued = job }
        executor.execute(task)
        return job
    }

    private fun run(kind: Kind): Boolean {
        val started = System.currentTimeMillis()
        step = 0
        steps = 0
        val outcome = try {
            when (kind) {
                Kind.FULL -> full()
                Kind.RECOMPOSE -> recompose()
            }
        } catch (e: Exception) {
            Log.e(TAG, "run failed: ${Net.describe(e)}")
            Outcome.FAILED
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "run ran out of memory")
            Outcome.FAILED
        }
        val ok = outcome == Outcome.DONE
        step = 0
        steps = 0
        lastKind = kind
        lastOutcome = outcome
        finishedAt = System.currentTimeMillis()
        if (!ok && store.current() == null) lastFailureAt = System.currentTimeMillis()
        Log.i(TAG, "${kind.name.lowercase()} run ${if (ok) "done" else "made nothing"} in ${System.currentTimeMillis() - started} ms")
        return ok
    }

    /** The source a new run reads, or null when there is none (a release build with no token). */
    fun source(): Source? = when {
        BuildConfig.TMDB_TOKEN.isNotBlank() -> TmdbSource(app, BuildConfig.TMDB_TOKEN, cache)
        BuildConfig.DEBUG -> SampleSource(app, cache)
        else -> null
    }

    /** The source a stored set or report names, or null when this build cannot read it. */
    fun sourceNamed(name: String): Source? = when (name) {
        "tmdb" -> if (BuildConfig.TMDB_TOKEN.isNotBlank()) TmdbSource(app, BuildConfig.TMDB_TOKEN, cache) else null
        "sample" -> if (BuildConfig.DEBUG) SampleSource(app, cache) else null
        else -> null
    }

    /** The picture size: the launcher's screen when known, else 1080p, within the picture size setting. */
    fun outputSize(s: Settings): Pair<Int, Int> {
        var w = settings.screenWidth.takeIf { it > 0 } ?: 1920
        var h = settings.screenHeight.takeIf { it > 0 } ?: 1080
        val (capW, capH) = if (s.pictureSize >= 2160) 3840 to 2160 else 1920 to 1080
        if (w > capW || h > capH) {
            val f = min(capW.toFloat() / w, capH.toFloat() / h)
            w = (w * f).toInt()
            h = (h * f).toInt()
        }
        return w to h
    }

    /** When the next full run is due for [set]. */
    fun dueAt(set: StoredSet, s: Settings): Long = set.fetchedAt + s.intervalHours * HOUR

    private fun full(): Outcome {
        lastFullStartedAt = System.currentTimeMillis()
        val s = settings.read()
        val source = source()
        if (source == null) {
            Log.i(TAG, "no TMDB token in this build: no pictures are made")
            return Outcome.NO_SOURCE
        }
        val lang = lang(s)
        val (w, h) = outputSize(s)
        val previous = store.current()
        val inputs = s.lists.mapNotNull { ListKey.of(it) }.map { key ->
            val list = source.list(key, lang)
            val titles = try {
                list.titles(Picker.POOL)
            } catch (e: IOException) {
                Log.w(TAG, "list ${key.key}: ${Net.describe(e)}")
                null
            }
            ListInput(list.key, list.label, titles)
        }
        val previousKeys = previous?.entries?.map { it.details.key }?.toSet().orEmpty()
        val result = Picker(s, lang.two).pick(
            inputs,
            previousKeys,
            details = { source.details(it, lang) },
            assets = { d -> fetchPictures(source, d) },
        )
        store.writeReport(
            RunReport(System.currentTimeMillis(), source.name, result.lists, lang.tag, s.releasedWithinYears, s.pick, s.titlesPerList),
        )
        Log.i(TAG, "picked ${result.picks.size} from ${inputs.size} lists (${source.name})")
        if (result.picks.isEmpty()) {
            Log.w(TAG, "nothing picked; the current set stays")
            val unread = inputs.all { it.titles == null } ||
                result.lists.all { l -> l.candidates.all { it.status == Skip.FAILED.key } }
            return when {
                unread && !online() -> Outcome.NO_CONNECTION
                unread -> Outcome.LISTS_FAILED
                else -> Outcome.NOTHING_PICKED
            }
        }
        val placed = result.picks.map { Placed(it.listKey, it.listLabel, it.details) }
        val now = System.currentTimeMillis()
        return finish(composeSet(source, placed, s, w, h, lang, fetchedAt = now))
    }

    private fun recompose(): Outcome {
        val current = store.current() ?: return full()
        val source = sourceNamed(current.source) ?: return full()
        val s = settings.read()
        val (w, h) = outputSize(s)
        val lang = Lang.of(current.lang)
        return finish(composeSet(source, placedOf(current), s, w, h, lang, fetchedAt = current.fetchedAt))
    }

    private fun finish(set: StoredSet?): Outcome {
        if (set == null || set.entries.isEmpty()) return Outcome.FAILED
        store.commit(set)
        cache.trim()
        Log.i(TAG, "set of ${set.entries.size} at ${set.width}x${set.height}")
        listeners.forEach { it.onSet(set) }
        return Outcome.DONE
    }

    /** Whether the device has a network that reaches the internet. */
    private fun online(): Boolean {
        val cm = app.getSystemService(ConnectivityManager::class.java) ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * One picture drawn with [look] for the settings' live preview: the current set's first title,
     * or with no set, the first title of the first list. Runs on the caller's thread - never the
     * generator's, so a run under way does not hold it up. Null when there is nothing to draw.
     */
    fun composePreview(look: Look, width: Int, height: Int): Bitmap? {
        val composer = Composer(Labels(app))
        val current = store.current()?.takeIf { it.entries.isNotEmpty() }
        if (current != null) {
            val source = sourceNamed(current.source) ?: return null
            val placed = placedOf(current)
            val first = placed.first()
            val sameList = placed.filter { it.listKey == first.listKey }
            val wall = if (look.layout == Layout.POSTER_WALL) wallOf(source, sameList) else emptyList()
            val scene = sceneFor(source, first, sameList, wall) ?: return null
            return composer.compose(scene, look, width, height)
        }
        val s = settings.read()
        val source = source() ?: return null
        val lang = lang(s)
        val key = s.lists.firstNotNullOfOrNull { ListKey.of(it) } ?: return null
        val list = source.list(key, lang)
        val title = try {
            list.titles(1).firstOrNull()
        } catch (e: IOException) {
            null
        } ?: return null
        val d = source.details(title, lang) ?: return null
        fetchPictures(source, d) ?: return null
        val only = Placed(list.key, list.label, d)
        val scene = sceneFor(source, only, listOf(only), emptyList()) ?: return null
        return composer.compose(scene, look, width, height)
    }

    /** The backdrop (needed), the logo and the poster (wanted), into the source cache. Null when the backdrop failed. */
    private fun fetchPictures(source: Source, d: TitleDetails): Unit? {
        val backdrop = d.backdrop ?: return null
        source.image(backdrop, ImageKind.BACKDROP) ?: return null
        d.logo?.let { source.image(it, ImageKind.LOGO) }
        d.poster?.let { source.image(it, ImageKind.POSTER) }
        return Unit
    }

    private class Placed(val listKey: String, val listLabel: String, val details: TitleDetails)

    /** A stored set's titles, with each list's name in the app's current language. */
    private fun placedOf(set: StoredSet): List<Placed> = set.entries.map { e ->
        Placed(e.list, ListKey.of(e.list)?.let { app.getString(it.label) } ?: e.listLabel, e.details)
    }

    /** The posters of [items] (one list's titles, in set order) that the source can give. */
    private fun wallOf(source: Source, items: List<Placed>): List<Pair<Placed, File>> =
        items.mapNotNull { p -> p.details.poster?.let { ref -> source.image(ref, ImageKind.POSTER)?.let { p to it } } }

    /** [p]'s scene among [listItems], or null when its backdrop cannot be had. */
    private fun sceneFor(source: Source, p: Placed, listItems: List<Placed>, wall: List<Pair<Placed, File>>): Scene? {
        val d = p.details
        val backdrop = d.backdrop?.let { source.image(it, ImageKind.BACKDROP) } ?: return null
        val logo = d.logo?.let { source.image(it, ImageKind.LOGO) }
        return Scene(
            details = d,
            listLabel = p.listLabel,
            indexInList = listItems.indexOf(p),
            listCount = listItems.size,
            backdrop = backdrop,
            logo = logo,
            wall = wall.map { it.second },
            wallIndex = wall.indexOfFirst { it.first === p },
        )
    }

    private fun composeSet(
        source: Source,
        placed: List<Placed>,
        s: Settings,
        w: Int,
        h: Int,
        lang: Lang,
        fetchedAt: Long,
    ): StoredSet {
        val look = s.look
        val composer = Composer(Labels(app))
        val byList = placed.groupBy { it.listKey }
        // Each list's posters, in set order, for Poster wall.
        val walls = if (look.layout == Layout.POSTER_WALL) {
            byList.mapValues { (_, items) -> wallOf(source, items) }
        } else {
            emptyMap()
        }
        val entries = ArrayList<SetEntry>()
        steps = placed.size
        for ((i, p) in placed.withIndex()) {
            step = i + 1
            val d = p.details
            val listItems = byList.getValue(p.listKey)
            val wall = walls[p.listKey].orEmpty()
            val scene = sceneFor(source, p, listItems, wall)
            if (scene == null) {
                Log.w(TAG, "no backdrop for ${d.key}; left out")
                continue
            }
            val layout = composer.effectiveLayout(scene, look)
            val id = itemId(scene, layout, look, lang, w, h, scene.logo, wall.map { it.first.details.poster })
            if (!store.has(id)) {
                val bitmap = composer.compose(scene, look, w, h)
                try {
                    store.writePicture(id, bitmap)
                } finally {
                    bitmap.recycle()
                }
            }
            val media = Media.of(d.media)
            entries += SetEntry(
                id = id,
                file = WallpaperStore.fileName(id),
                title = d.name,
                url = media?.let { source.pageUrl(it, d.id) },
                list = p.listKey,
                listLabel = p.listLabel,
                details = d,
            )
        }
        return StoredSet(
            createdAt = System.currentTimeMillis(),
            fetchedAt = fetchedAt,
            source = source.name,
            width = w,
            height = h,
            lang = lang.tag,
            entries = entries,
        )
    }

    /**
     * The first 24 hex of SHA-1 over `media/id|layout|position|lang|WxH|options|STYLE_VERSION`, where
     * the options are everything else the picture shows: so a changed picture gets a new id and an
     * unchanged one keeps its own.
     */
    private fun itemId(
        scene: Scene,
        layout: Layout,
        look: Look,
        lang: Lang,
        w: Int,
        h: Int,
        logo: File?,
        wallRefs: List<String?>,
    ): String {
        val d = scene.details
        val options = buildString {
            append("syn=").append(look.synopsisLines)
            append(";shade=").append(look.bottomShade)
            append(";list=").append(scene.listLabel)
            append(";n=").append(scene.indexInList).append('/').append(scene.listCount)
            append(";logo=").append(logo != null)
            append(";details=").append(DETAILS_JSON.encodeToString(TitleDetails.serializer(), d))
            if (layout == Layout.POSTER_WALL) append(";wall=").append(wallRefs.joinToString(","))
        }
        val text = "${d.media}/${d.id}|${layout.key}|${look.position.key}|${lang.tag}|${w}x$h|" +
            "${SourceCache.sha1(options)}|${Composer.STYLE_VERSION}"
        return SourceCache.sha1(text).take(24)
    }

    companion object {
        private const val TAG = "Marquee"
        const val HOUR = 3_600_000L

        private val DETAILS_JSON = Json { encodeDefaults = true }

        @Volatile
        private var instance: Generator? = null

        fun get(context: Context): Generator =
            instance ?: synchronized(this) { instance ?: Generator(context).also { instance = it } }
    }
}
