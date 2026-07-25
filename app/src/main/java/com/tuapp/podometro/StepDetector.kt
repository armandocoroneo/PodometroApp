package com.tuapp.podometro

import kotlin.math.sqrt

class StepDetector(
    private val onStepDetected: () -> Unit
) {
    private var lastAccel = 0.0
    private var currentAccel = 0.0
    private var velocity = 0.0
    private val threshold = 11.0
    private var lastStepTime = 0L
    private val minStepInterval = 250L

    fun processAccelerometerData(x: Float, y: Float, z: Float, timestamp: Long) {
        val magnitude = sqrt((x * x + y * y + z * z).toDouble())
        currentAccel = magnitude
        val delta = currentAccel - lastAccel
        velocity = velocity * 0.9 + delta
        lastAccel = currentAccel

        if (velocity > threshold) {
            if (timestamp - lastStepTime > minStepInterval) {
                lastStepTime = timestamp
                onStepDetected()
            }
        }
    }
}
