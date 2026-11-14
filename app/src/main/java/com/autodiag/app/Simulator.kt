package com.autodiag.app

import kotlinx.coroutines.delay
import kotlin.math.sin

class VehicleSimulator {
    private var tick = 0
    suspend fun next(): VehicleData {
        delay(900)
        tick++
        return VehicleData(
            rpm = 820 + (sin(tick / 5.0) * 110).toInt(),
            coolantC = 88 + (sin(tick / 18.0) * 3).toInt(),
            speedKmh = if (tick % 45 < 12) (tick % 45) * 3 else 0,
            throttlePercent = 4.0 + sin(tick / 7.0) * 1.5,
            voltage = 14.1 + sin(tick / 12.0) * 0.15
        )
    }
    fun sampleFaults(): List<String> = listOf("P0300", "P0420")
}
