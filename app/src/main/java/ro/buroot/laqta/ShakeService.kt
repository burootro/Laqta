package ro.buroot.laqta

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ShakeService : Service() {

    companion object {
        private const val CHANNEL_ID = "laqta_shake"
        private const val NOTIF_ID = 1107
        const val ACTION_STOP = "ro.buroot.laqta.action.STOP"

        @Volatile
        var isRunning = false
            private set

        fun start(c: Context) {
            ContextCompat.startForegroundService(c, Intent(c, ShakeService::class.java))
        }

        fun stop(c: Context) {
            c.stopService(Intent(c, ShakeService::class.java))
        }

        fun reload(c: Context) {
            if (isRunning) start(c)
        }
    }

    private lateinit var sensorManager: SensorManager
    private var accel: Sensor? = null
    private var detector: ShakeDetector? = null
    private var registered = false
    private val main = Handler(Looper.getMainLooper())
    private val worker: ExecutorService = Executors.newSingleThreadExecutor()

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> registerSensor()
                Intent.ACTION_SCREEN_OFF -> unregisterSensor()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        createChannel()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Prefs.setShakeEnabled(this, false)
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(), type)
        applySettings()
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (pm.isInteractive) registerSensor()
        return START_STICKY
    }

    private fun applySettings() {
        val d = detector ?: ShakeDetector { onShake() }.also { detector = it }
        d.threshold = Prefs.thresholdFor(Prefs.sensitivity(this))
        d.requiredShakes = Prefs.shakeCount(this)
    }

    private fun registerSensor() {
        if (registered) return
        val s = accel ?: return
        val d = detector ?: return
        registered = sensorManager.registerListener(d, s, SensorManager.SENSOR_DELAY_GAME)
    }

    private fun unregisterSensor() {
        if (!registered) return
        detector?.let { sensorManager.unregisterListener(it) }
        detector?.reset()
        registered = false
    }

    private fun onShake() {
        if (Prefs.vibrate(this)) vibrate()
        main.postDelayed({
            worker.execute { ScreenshotTrigger.take(applicationContext) }
        }, 250)
    }

    private fun vibrate() {
        try {
            getVibrator().vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {
        }
    }

    @Suppress("DEPRECATION")
    private fun getVibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= 31) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

    private fun createChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        // MIN = من غير أيقونة في الشريط العلوي، عشان متظهرش في السكرين شوت
        val ch = NotificationChannel(CHANNEL_ID, "تشغيل الهز", NotificationManager.IMPORTANCE_MIN).apply {
            setShowBadge(false)
            description = "إشعار ثابت طول ما خاصية الهز شغالة"
        }
        nm.createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            flags
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, ShakeService::class.java).setAction(ACTION_STOP),
            flags
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("لقطة شغالة")
            .setContentText("هز الموبايل عشان تاخد سكرين شوت")
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setColor(0xFF2B4174.toInt())
            .setContentIntent(open)
            .addAction(0, "إيقاف", stop)
            .build()
    }

    override fun onDestroy() {
        unregisterSensor()
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {
        }
        main.removeCallbacksAndMessages(null)
        worker.shutdown()
        isRunning = false
        super.onDestroy()
    }
}
