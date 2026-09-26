package ro.buroot.laqta

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class ShotAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: ShotAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun takeShot(): Boolean = performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
}
