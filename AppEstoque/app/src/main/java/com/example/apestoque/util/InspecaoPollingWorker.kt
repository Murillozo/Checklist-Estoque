package com.example.apestoque.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.apestoque.MainActivity
import com.example.apestoque.R
import com.example.apestoque.data.NetworkModule
import com.example.apestoque.data.SolicitacaoRepository
import java.util.concurrent.TimeUnit

class InspecaoPollingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val repo by lazy { SolicitacaoRepository(NetworkModule.api(appContext)) }
    private val prefs by lazy {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun doWork(): Result {
        val response = repo.fetchInspecoes()
        return response.fold(
            onSuccess = { lista ->
                val storedIds = prefs.getStringSet(KEY_KNOWN_IDS, emptySet())
                    ?.mapNotNull { it.toIntOrNull() }
                    ?.toSet().orEmpty()
                val currentIds = lista.map { it.id }.toSet()

                prefs.edit()
                    .putStringSet(KEY_KNOWN_IDS, currentIds.map { it.toString() }.toSet())
                    .apply()

                if (currentIds.isNotEmpty()) {
                    val newIdsCount = (currentIds - storedIds).size
                    notifySolicitacoes(currentIds, newIdsCount)
                } else {
                    cancelNotifications()
                }

                Companion.scheduleNext(applicationContext)
                Result.success()
            },
            onFailure = {
                Result.retry()
            }
        )
    }

    private fun notifySolicitacoes(currentIds: Set<Int>, newIdsCount: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }
        ensureChannel()
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            flags
        )

        val title = if (newIdsCount > 0) {
            applicationContext.getString(R.string.notification_inspecao_title)
        } else {
            applicationContext.getString(R.string.notification_inspecao_reminder_title)
        }
        val content = if (newIdsCount > 0) {
            applicationContext.resources.getQuantityString(
                R.plurals.notification_inspecao_message,
                newIdsCount,
                newIdsCount
            )
        } else {
            applicationContext.resources.getQuantityString(
                R.plurals.notification_inspecao_reminder_message,
                currentIds.size,
                currentIds.size
            )
        }

        val alarmSound = customAlarmSound(applicationContext)


        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_inspecao_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(alarmSound, AudioManager.STREAM_ALARM)
            .setVibrate(VIBRATION_PATTERN)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .setOnlyAlertOnce(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }

    private fun cancelNotifications() {
        NotificationManagerCompat.from(applicationContext).cancel(NOTIFICATION_ID)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSound = customAlarmSound(applicationContext)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            val existingChannel = manager?.getNotificationChannel(CHANNEL_ID)

            if (existingChannel == null) {
                manager?.createNotificationChannel(
                    buildChannel(alarmSound, audioAttributes)
                )
            } else {
                val soundMatches = existingChannel.sound == alarmSound
                val usageMatches = existingChannel.audioAttributes?.usage == AudioAttributes.USAGE_ALARM
                if (!soundMatches || !usageMatches) {
                    manager?.deleteNotificationChannel(CHANNEL_ID)
                    manager?.createNotificationChannel(
                        buildChannel(alarmSound, audioAttributes)
                    )
                }
            }
        }
    }

    private fun buildChannel(
        alarmSound: Uri,
        audioAttributes: AudioAttributes
    ): NotificationChannel {
        return NotificationChannel(
            CHANNEL_ID,
            applicationContext.getString(R.string.notification_channel_inspecao),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(alarmSound, audioAttributes)
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
            enableLights(true)
        }
    }

    companion object {
        const val WORK_NAME = "inspecao_polling"
        const val PREFS_NAME = "inspecao_sync"
        const val KEY_KNOWN_IDS = "known_ids"
        private const val CHANNEL_ID = "inspecao_channel"
        private const val NOTIFICATION_ID = 1001
        private const val ALERT_INTERVAL_SECONDS = 30L
        private val VIBRATION_PATTERN = longArrayOf(0, 800, 400, 800, 400, 800, 400, 800)

        private fun customAlarmSound(context: Context): Uri {
            val packageName = context.packageName
            return Uri.parse(
                "${ContentResolver.SCHEME_ANDROID_RESOURCE}://$packageName/${R.raw.meualarme}"
            )
        }

        fun schedule(context: Context) {
            scheduleNext(context, 0)
        }

        internal fun scheduleNext(context: Context, delaySeconds: Long = ALERT_INTERVAL_SECONDS) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<InspecaoPollingWorker>()
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    ALERT_INTERVAL_SECONDS,
                    TimeUnit.SECONDS
                )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}