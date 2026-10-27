package ca.dstudio.marquee.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

/**
 * Makes a new set every `interval_hours`, with a network. The first one is made when the launcher
 * first asks, so the schedule starts one interval later.
 */
class UpdateWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result = try {
        if (Generator.get(applicationContext).request(Generator.Kind.FULL).get()) Result.success() else Result.retry()
    } catch (e: ExecutionException) {
        Result.retry()
    } catch (e: InterruptedException) {
        Result.retry()
    }

    companion object {
        private const val NAME = "marquee-update"

        /**
         * Schedules the periodic run. At start-up an existing schedule is kept ([replace] false); a
         * changed interval replaces it.
         */
        fun schedule(context: Context, intervalHours: Int, replace: Boolean) {
            val request = PeriodicWorkRequest.Builder(UpdateWorker::class.java, intervalHours.toLong(), TimeUnit.HOURS)
                .setInitialDelay(intervalHours.toLong(), TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME,
                if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
