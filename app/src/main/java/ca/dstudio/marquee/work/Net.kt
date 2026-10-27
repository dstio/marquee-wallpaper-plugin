package ca.dstudio.marquee.work

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

/**
 * Marquee's one HTTP client: https only (a redirect may not leave it), 15 s to connect, 30 s to
 * read, and every body capped while it is read, so a wrong or hostile answer cannot fill memory or
 * the disk.
 *
 * Nothing here logs: callers log the path alone, never a query or a header.
 */
object Net {

    const val JSON_CAP = 2L * 1024 * 1024
    const val IMAGE_CAP = 25L * 1024 * 1024

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followSslRedirects(false)
            .build()
    }

    /** An HTTP answer that was not 2xx. */
    class HttpError(val code: Int, val path: String) : IOException("HTTP $code for $path")

    /** The body of a GET of [url], at most [cap] bytes. */
    @Throws(IOException::class)
    fun get(url: HttpUrl, cap: Long, headers: Map<String, String> = emptyMap()): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        fetch(url, cap, headers) { input -> copyCapped(input, out, cap) }
        return out.toByteArray()
    }

    /**
     * Downloads [url] into [dest] through a `.tmp` beside it, at most [cap] bytes; [dest] appears
     * only when the whole body arrived.
     */
    @Throws(IOException::class)
    fun download(url: HttpUrl, dest: File, cap: Long) {
        dest.parentFile?.mkdirs()
        val tmp = File(dest.path + ".tmp")
        try {
            fetch(url, cap, emptyMap()) { input -> tmp.outputStream().use { copyCapped(input, it, cap) } }
            if (!tmp.renameTo(dest)) throw IOException("could not move ${dest.name} into place")
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }

    private inline fun fetch(url: HttpUrl, cap: Long, headers: Map<String, String>, read: (InputStream) -> Unit) {
        if (!url.isHttps) throw IOException("not https: ${url.encodedPath}")
        val request = Request.Builder().url(url).apply { headers.forEach { (k, v) -> header(k, v) } }.build()
        client.newCall(request).execute().use { response ->
            if (!response.request.url.isHttps) throw IOException("redirected off https: ${url.encodedPath}")
            if (!response.isSuccessful) throw HttpError(response.code, url.encodedPath)
            val body = response.body ?: throw IOException("no body: ${url.encodedPath}")
            val length = body.contentLength()
            if (length > cap) throw IOException("body of $length bytes over the cap: ${url.encodedPath}")
            body.byteStream().use(read)
        }
    }

    private fun copyCapped(input: InputStream, output: OutputStream, cap: Long) {
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            total += n
            if (total > cap) throw IOException("body over the cap of $cap bytes")
            output.write(buffer, 0, n)
        }
    }

    /** An exception as a log line, with any URL query cut off. */
    fun describe(e: Throwable): String =
        "${e.javaClass.simpleName}: ${e.message.orEmpty().replace(Regex("\\?\\S*"), "?…")}"
}
