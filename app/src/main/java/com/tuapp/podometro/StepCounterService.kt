package com.tuapp.podometro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class StepCounterService : Service(), SensorEventListener {

    private val binder = LocalBinder()
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val stepDetector = StepDetector { onStepDetected() }

    var steps = 0
        private set
    var startTime = 0L
    var isRunning = false
        private set

    private var callback: ((Int, Long) -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): StepCounterService = this@StepCounterService
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    fun startCounting() {
        if (isRunning) return
        isRunning = true
        startTime = System.currentTimeMillis()
        steps = 0
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        startForeground(1, createNotification())
    }

    fun stopCounting() {
        isRunning = false
        sensorManager.unregisterListener(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    fun setCallback(cb: (steps: Int, elapsedMs: Long) -> Unit) {
        callback = cb
    }

    private fun onStepDetected() {
        steps++
        val elapsed = if (startTime > 0) System.currentTimeMillis() - startTime else 0
        callback?.invoke(steps, elapsed)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            stepDetector.processAccelerometerData(
                it.values[0], it.values[1], it.values[2], it.timestamp
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val channelId = "podometro_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Podómetro", NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Podómetro activo")
            .setContentText("Contando pasos...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
