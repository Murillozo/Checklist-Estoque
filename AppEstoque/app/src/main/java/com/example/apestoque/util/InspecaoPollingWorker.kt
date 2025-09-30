package com.example.apestoque.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
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
                if (storedIds.isNotEmpty() && (currentIds - storedIds).isNotEmpty()) {
                    notifyNewSolicitacao(currentIds - storedIds)
                } else if (storedIds.isEmpty() && currentIds.isNotEmpty()) {
                    // Primeira sincronização com itens já pendentes
                    notifyNewSolicitacao(currentIds)
                }
                prefs.edit()
                    .putStringSet(KEY_KNOWN_IDS, currentIds.map { it.toString() }.toSet())
                    .apply()
                Result.success()
            },
            onFailure = {
                Result.retry()
            }
        )
    }

    private fun notifyNewSolicitacao(newIds: Set<Int>) {
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

        val title = applicationContext.getString(R.string.notification_inspecao_title)
        val content = applicationContext.resources.getQuantityString(
            R.plurals.notification_inspecao_message,
            newIds.size,
            newIds.size
        )

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_inspecao_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(alarmSound)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            val existingChannel = manager?.getNotificationChannel(CHANNEL_ID)
            if (existingChannel == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    applicationContext.getString(R.string.notification_channel_inspecao),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setSound(alarmSound, audioAttributes)
                }
                manager?.createNotificationChannel(channel)
            } else {
                existingChannel.setSound(alarmSound, audioAttributes)
                manager?.createNotificationChannel(existingChannel)
            }
        }
    }

    companion object {
        const val WORK_NAME = "inspecao_polling"
        const val PREFS_NAME = "inspecao_sync"
        const val KEY_KNOWN_IDS = "known_ids"
        private const val CHANNEL_ID = "inspecao_channel"
        private const val NOTIFICATION_ID = 1001

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<InspecaoPollingWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
