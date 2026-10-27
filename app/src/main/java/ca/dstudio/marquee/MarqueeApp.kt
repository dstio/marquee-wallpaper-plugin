package ca.dstudio.marquee

import android.app.Application
import ca.dstudio.marquee.compose.Fonts
import ca.dstudio.marquee.settings.MarqueeSettings
import ca.dstudio.marquee.work.UpdateWorker

class MarqueeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Fonts.attach(this)
        UpdateWorker.schedule(this, MarqueeSettings(this).read().intervalHours, replace = false)
    }
}
