package ca.dstudio.marquee.compose

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import java.util.concurrent.ConcurrentHashMap

/**
 * The two bundled families, Jost and Fraunces (SIL Open Font License; the texts are in
 * `assets/licences/`). Both are shipped as their upstream variable files, so a weight is an
 * instance chosen at run time with font variation settings - available from Android 8.0. Before
 * that, the system's own sans-serif and serif stand in, at the nearest weight they have.
 *
 * [attach] once with any context; until then, and if a file cannot be read, the fallbacks are used.
 */
object Fonts {

    private const val JOST = "fonts/jost_variable.ttf"
    private const val FRAUNCES = "fonts/fraunces_variable.ttf"

    private val faces = ConcurrentHashMap<String, Typeface>()

    @Volatile
    private var context: Context? = null

    fun attach(context: Context) {
        this.context = context.applicationContext
    }

    /** Jost at [weight]: 300 for the synopsis, 400 for labels, 500 for facts and figures. */
    fun jost(weight: Int): Typeface = instance(JOST, "'wght' $weight", weight) {
        when {
            weight <= 300 -> Typeface.create("sans-serif-light", Typeface.NORMAL)
            weight >= 500 -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
            else -> Typeface.SANS_SERIF
        }
    }

    /** Fraunces at [weight], at its display optical size (`opsz` 144), for a title set in type. */
    fun fraunces(weight: Int): Typeface = instance(FRAUNCES, "'wght' $weight, 'opsz' 144", weight) {
        Typeface.create(Typeface.SERIF, if (weight >= 600) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun instance(path: String, axes: String, weight: Int, fallback: () -> Typeface): Typeface {
        val key = "$path|$axes"
        faces[key]?.let { return it }
        val assets = context?.assets
        val face = if (assets != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                Typeface.Builder(assets, path)
                    .setFontVariationSettings(axes)
                    .setWeight(weight)
                    .build()
            }.getOrNull()
        } else {
            null
        } ?: fallback()
        faces[key] = face
        return face
    }
}
