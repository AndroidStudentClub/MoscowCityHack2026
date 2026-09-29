package com.mikhailskiy.finni.data

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.mikhailskiy.finni.MainActivity
import com.mikhailskiy.finni.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

object StepUpdateBus {
    private val mutableUpdates = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val updates = mutableUpdates.asSharedFlow()

    fun notifyChanged() {
        mutableUpdates.tryEmit(Unit)
    }
}

class StepCounterService : Service(), SensorEventListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository by lazy { FinniRepository(applicationContext) }
    private val sensorManager by lazy { getSystemService(SENSOR_SERVICE) as SensorManager }
    private val stepCounter by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) }
    private var listenerRegistered = false

    override fun onCreate() {
        super.onCreate()
        if (!hasActivityRecognitionPermission(this)) {
            stopSelf()
            return
        }
        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!hasActivityRecognitionPermission(this) || stepCounter == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!listenerRegistered) {
            listenerRegistered = sensorManager.registerListener(
                this,
                stepCounter,
                SensorManager.SENSOR_DELAY_NORMAL,
            )
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val sensorTotal = event.values.firstOrNull()?.toLong() ?: return
        scope.launch {
            if (repository.recordStepCounter(sensorTotal)) {
                StepUpdateBus.notifyChanged()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        if (listenerRegistered) sensorManager.unregisterListener(this)
        listenerRegistered = false
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Подсчёт шагов",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Шаги влияют на сытость героя"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Финни считает шаги")
            .setContentText("Каждые 500 шагов уменьшают сытость героя на 1")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "finni_steps"
        private const val NOTIFICATION_ID = 1001

        fun hasStepCounter(context: Context): Boolean {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            return sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
        }

        fun hasActivityRecognitionPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        fun start(context: Context) {
            if (!hasStepCounter(context) || !hasActivityRecognitionPermission(context)) return
            ContextCompat.startForegroundService(
                context,
                Intent(context, StepCounterService::class.java),
            )
        }
    }
}
