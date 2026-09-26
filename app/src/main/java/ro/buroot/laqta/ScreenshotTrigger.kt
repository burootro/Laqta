package ro.buroot.laqta

import android.content.Context

object ScreenshotTrigger {

    fun take(c: Context): Boolean {
        val acc = ShotAccessibilityService.instance
        val ok = when (Prefs.method(c)) {
            Prefs.METHOD_ROOT -> rootShot()
            Prefs.METHOD_ACCESS -> acc?.takeShot() ?: false
            else -> (acc?.takeShot() ?: false) || rootShot()
        }
        if (ok) Prefs.incShots(c)
        return ok
    }

    // KEYCODE_SYSRQ (120) = زرار السكرين شوت بتاع النظام نفسه
    private fun rootShot(): Boolean =
        RootShell.fire("cmd input keyevent 120 || input keyevent 120")
}
