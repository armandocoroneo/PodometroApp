package com.tuapp.podometro

import kotlin.math.sqrt
import kotlin.math.abs

/**
 * Detector de pasos mejorado con:
 * - Filtro de paso alto para eliminar gravedad y vibraciones de baja frecuencia
 * - Umbral adaptativo basado en el historial reciente
 * - Ventana de tiempo más estricta entre pasos
 * - Rechazo de picos dobles
 */
class StepDetector(
    private val onStepDetected: () -> Unit
) {
    // Historial de aceleración para filtro
    private val historySize = 50
    private val accelHistory = ArrayDeque<Double>(historySize)

    // Para detección de picos
    private var lastPeakTime = 0L
    private val minStepInterval = 350L  // ms mínimo entre pasos (evita dobles)
    private val maxStepInterval = 1200L // ms máximo entre pasos (evita falsos positivos espaciados)

    // Umbral base y adaptativo
    private val baseThreshold = 2.5     // m/s² sobre la gravedad
    private var dynamicThreshold = baseThreshold
    private val thresholdDecay = 0.95   // Decaimiento del umbral adaptativo
    private val thresholdBoost = 0.3    // Cuánto sube el umbral tras detectar un paso

    // Estado del pico
    private var wasAboveThreshold = false
    private var lastMagnitude = 0.0

    // Calibración de vibración de fondo
    private var calibrationSamples = 0
    private val calibrationNeeded = 30
    private var noiseLevel = 0.0

    fun processAccelerometerData(x: Float, y: Float, z: Float, timestamp: Long) {
        val magnitude = sqrt((x * x + y * y + z * z).toDouble())

        // Filtro de paso alto: restamos la gravedad (~9.81) y suavizamos
        val filtered = highPassFilter(magnitude)

        // Calibración inicial (primeros N samples para medir ruido de fondo)
        if (calibrationSamples < calibrationNeeded) {
            noiseLevel += abs(filtered)
            calibrationSamples++
            if (calibrationSamples == calibrationNeeded) {
                noiseLevel /= calibrationNeeded
                // Ajustar umbral según el ruido ambiente
                dynamicThreshold = maxOf(baseThreshold, noiseLevel * 3.0)
            }
            return
        }

        // Decaimiento adaptativo del umbral
        dynamicThreshold = dynamicThreshold * thresholdDecay + baseThreshold * (1 - thresholdDecay)

        // Detección de pico: debe subir por encima del umbral y luego bajar
        val isAboveThreshold = filtered > dynamicThreshold

        if (wasAboveThreshold && !isAboveThreshold && lastMagnitude > dynamicThreshold) {
            // Pico detectado (subió y bajó)
            val timeSinceLast = timestamp - lastPeakTime

            if (timeSinceLast in minStepInterval..maxStepInterval) {
                lastPeakTime = timestamp
                dynamicThreshold += thresholdBoost  // Subir umbral temporalmente
                onStepDetected()
            }
        }

        wasAboveThreshold = isAboveThreshold
        lastMagnitude = filtered
    }

    private fun highPassFilter(magnitude: Double): Double {
        // Mantener historial
        accelHistory.addLast(magnitude)
        if (accelHistory.size > historySize) {
            accelHistory.removeFirst()
        }

        // Media móvil (filtro de paso bajo de la gravedad)
        val avg = accelHistory.average()

        // Diferencia = paso alto (solo cambios bruscos)
        return magnitude - avg
    }

    fun resetCalibration() {
        calibrationSamples = 0
        noiseLevel = 0.0
        accelHistory.clear()
    }
}
