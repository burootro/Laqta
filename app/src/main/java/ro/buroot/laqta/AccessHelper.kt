package ro.buroot.laqta

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

object AccessHelper {

    private fun me(c: Context) = ComponentName(c, ShotAccessibilityService::class.java)

    private fun containsMe(c: Context, list: List<String>): Boolean {
        val mine = me(c)
        return list.any { ComponentName.unflattenFromString(it) == mine }
    }

    fun isEnabled(c: Context): Boolean {
        if (ShotAccessibilityService.instance != null) return true
        val s = Settings.Secure.getString(
            c.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return containsMe(c, s.split(':'))
    }

    fun enableViaRoot(c: Context): Boolean {
        val cur = RootShell.exec("settings get secure enabled_accessibility_services").out.trim()
        val parts = if (cur.isEmpty() || cur == "null") emptyList()
        else cur.split(':').filter { it.isNotBlank() }
        val list = if (containsMe(c, parts)) parts else parts + me(c).flattenToString()
        val r = RootShell.exec("settings put secure enabled_accessibility_services '${list.joinToString(":")}'")
        RootShell.exec("settings put secure accessibility_enabled 1")
        return r.code == 0
    }
}
