package ro.buroot.laqta

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.sqrt

class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {

    @Volatile var threshold = 2.5f
    @Volatile var requiredShakes = 2

    private var count = 0
    private var firstTs = 0L
    private var lastPeakTs = 0L
    private var lastTrigger = 0L

    fun reset() {
        count = 0
        firstTs = 0L
    }

    override fun onSensorChanged(event: SensorEvent) {
        val gx = event.values[0] / SensorManager.GRAVITY_EARTH
        val gy = event.values[1] / SensorManager.GRAVITY_EARTH
        val gz = event.values[2] / SensorManager.GRAVITY_EARTH
        val g = sqrt(gx * gx + gy * gy + gz * gz)
        if (g < threshold) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastTrigger < 1800) return
        if (now - lastPeakTs < 130) return
        if (now - firstTs > 1100) {
            count = 0
            firstTs = now
        }
        lastPeakTs = now
        count++
        if (count >= requiredShakes) {
            reset()
            lastTrigger = now
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
