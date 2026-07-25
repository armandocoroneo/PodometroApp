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
import android.util.Log
import androidx.core.app.NotificationCompat

class StepCounterService : Service(), SensorEventListener {

    companion object {
        private const val TAG = "StepCounterService"
    }

    private val binder = LocalBinder()
    private lateinit var sensorManager: SensorManager

    private var stepCounterSensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private val stepDetector = StepDetector { onStepDetected() }

    var steps = 0
        private set
    var startTime = 0L
    var isRunning = false
        private set
    var sensorType = "none"
        private set

    private var callback: ((Int, Long, String) -> Unit)? = null
    private var stepCounterOffset = -1

    inner class LocalBinder : Binder() {
        fun getService(): StepCounterService = this@StepCounterService
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        when {
            stepCounterSensor != null -> {
                sensorType = "hardware_step_counter"
                Log.i(TAG, "Usando sensor de pasos dedicado del hardware")
            }
            accelerometer != null -> {
                sensorType = "accelerometer"
                Log.i(TAG, "Usando acelerómetro como fallback")
            }
            else -> {
                sensorType = "none"
                Log.e(TAG, "No hay sensores disponibles en este dispositivo")
            }
        }
    }

    fun startCounting() {
        if (isRunning) return

        if (sensorType == "none") {
            Log.e(TAG, "No se puede iniciar: sin sensores disponibles")
            return
        }

        isRunning = true
        startTime = System.currentTimeMillis()
        steps = 0
        stepCounterOffset = -1

        when (sensorType) {
            "hardware_step_counter" -> {
                sensorManager.registerListener(
                    this, 
                    stepCounterSensor, 
                    SensorManager.SENSOR_DELAY_UI
                )
            }
            "accelerometer" -> {
                stepDetector.resetCalibration()
                sensorManager.registerListener(
                    this, 
                    accelerometer, 
                    SensorManager.SENSOR_DELAY_GAME
                )
            }
        }

        startForeground(1, createNotification())
        callback?.invoke(steps, 0L, sensorType)
    }

    fun stopCounting() {
        isRunning = false
        sensorManager.unregisterListener(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    fun setCallback(cb: (steps: Int, elapsedMs: Long, sensorType: String) -> Unit) {
        callback = cb
    }

    private fun onStepDetected() {
        steps++
        val elapsed = if (startTime > 0) System.currentTimeMillis() - startTime else 0
        callback?.invoke(steps, elapsed, sensorType)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { ev ->
            when (ev.sensor.type) {
                Sensor.TYPE_STEP_COUNTER -> {
                    val totalSteps = ev.values[0].toInt()
                    if (stepCounterOffset == -1) {
                        stepCounterOffset = totalSteps
                    }
                    steps = totalSteps - stepCounterOffset
                    val elapsed = if (startTime > 0) System.currentTimeMillis() - startTime else 0
                    callback?.invoke(steps, elapsed, sensorType)
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    stepDetector.processAccelerometerData(
                        ev.values[0], ev.values[1], ev.values[2], System.currentTimeMillis()
                    )
                }
                else -> {
                    // Ignorar otros tipos de sensores
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "Accuracy changed: ${sensor?.name} -> $accuracy")
    }

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
