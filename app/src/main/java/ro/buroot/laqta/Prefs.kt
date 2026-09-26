package ro.buroot.laqta

import android.content.Context

object Prefs {
    const val METHOD_AUTO = 0
    const val METHOD_ROOT = 1
    const val METHOD_ACCESS = 2

    private fun sp(c: Context) = c.getSharedPreferences("laqta_prefs", Context.MODE_PRIVATE)

    fun shakeEnabled(c: Context) = sp(c).getBoolean("shake_enabled", false)
    fun setShakeEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean("shake_enabled", v).apply()

    fun sensitivity(c: Context) = sp(c).getInt("sensitivity", 3).coerceIn(1, 5)
    fun setSensitivity(c: Context, v: Int) = sp(c).edit().putInt("sensitivity", v.coerceIn(1, 5)).apply()

    fun shakeCount(c: Context) = sp(c).getInt("shake_count", 2).coerceIn(1, 3)
    fun setShakeCount(c: Context, v: Int) = sp(c).edit().putInt("shake_count", v.coerceIn(1, 3)).apply()

    fun vibrate(c: Context) = sp(c).getBoolean("vibrate", true)
    fun setVibrate(c: Context, v: Boolean) = sp(c).edit().putBoolean("vibrate", v).apply()

    fun method(c: Context) = sp(c).getInt("method", METHOD_AUTO)
    fun setMethod(c: Context, v: Int) = sp(c).edit().putInt("method", v).apply()

    fun shots(c: Context) = sp(c).getInt("shots", 0)
    fun incShots(c: Context) = sp(c).edit().putInt("shots", shots(c) + 1).apply()

    fun thresholdFor(level: Int): Float = when (level) {
        1 -> 3.4f
        2 -> 2.9f
        3 -> 2.5f
        4 -> 2.1f
        else -> 1.8f
    }
}
