package ro.buroot.laqta

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val resumeTick = mutableIntStateOf(0)

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            LaqtaTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MainScreen(resumeTick.intValue)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick.intValue++
    }
}

private fun toast(c: Context, msg: String) = Toast.makeText(c, msg, Toast.LENGTH_SHORT).show()

private fun sensLabel(level: Int) = when (level) {
    1 -> "خفيفة جداً (هزة قوية)"
    2 -> "خفيفة"
    3 -> "متوسطة"
    4 -> "عالية"
    else -> "عالية جداً (هزة بسيطة)"
}

@Composable
fun MainScreen(tick: Int) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme

    var root by remember { mutableStateOf<Boolean?>(null) }
    var shakeOn by remember { mutableStateOf(Prefs.shakeEnabled(ctx)) }
    var sensitivity by remember { mutableFloatStateOf(Prefs.sensitivity(ctx).toFloat()) }
    var shakes by remember { mutableIntStateOf(Prefs.shakeCount(ctx)) }
    var vibrate by remember { mutableStateOf(Prefs.vibrate(ctx)) }
    var method by remember { mutableIntStateOf(Prefs.method(ctx)) }
    var accOn by remember { mutableStateOf(false) }
    var shots by remember { mutableIntStateOf(0) }
    var countdown by remember { mutableIntStateOf(0) }
    var showAbout by remember { mutableStateOf(false) }
    var expRoot by remember { mutableStateOf(false) }
    var expShake by remember { mutableStateOf(true) }
    var expMethod by remember { mutableStateOf(false) }

    LaunchedEffect(tick) {
        accOn = AccessHelper.isEnabled(ctx)
        shots = Prefs.shots(ctx)
        shakeOn = Prefs.shakeEnabled(ctx)
        if (root != true) {
            root = withContext(Dispatchers.IO) { RootShell.hasRoot() }
            if (root == false) expRoot = true
        }
    }

    LaunchedEffect(root) {
        if (root == true) {
            withContext(Dispatchers.IO) {
                RootShell.exec("dumpsys deviceidle whitelist +${ctx.packageName}")
            }
            if (Prefs.shakeEnabled(ctx) && !ShakeService.isRunning) ShakeService.start(app)
        }
    }

    fun toggleShake() {
        if (!shakeOn) {
            if (root != true && !accOn) {
                toast(ctx, "محتاج روت أو خدمة الوصول الأول")
                return
            }
            Prefs.setShakeEnabled(ctx, true)
            ShakeService.start(app)
            shakeOn = true
            toast(ctx, "اتفعلت، هز الموبايل وجرب")
        } else {
            Prefs.setShakeEnabled(ctx, false)
            ShakeService.stop(app)
            shakeOn = false
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(cs.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(40.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Text("لقطة", fontSize = 52.sp, color = cs.onBackground)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showAbout = true }) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "الإعدادات",
                    tint = cs.onBackground,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Spacer(Modifier.height(36.dp))

        // ===== كارت الهز =====
        SectionCard(
            icon = painterResource(R.drawable.ic_shake),
            title = "هز للتصوير",
            desc = "هز الموبايل وهيتاخد سكرين شوت الموبايل نفسه فوراً، بالصوت والمعاينة وأدوات التعديل بتاعة النظام.",
            expanded = expShake,
            onToggle = { expShake = !expShake }
        ) {
            StatusLine(
                ok = shakeOn,
                okText = "شغال • عدد اللقطات: $shots",
                badText = "متوقف"
            )
            Spacer(Modifier.height(20.dp))
            PillButton(
                text = if (shakeOn) "إيقاف" else "تشغيل",
                icon = if (shakeOn) Icons.Filled.Close else Icons.Filled.PlayArrow,
                style = if (shakeOn) PillStyle.Tonal else PillStyle.Filled,
                onClick = { toggleShake() }
            )
            Spacer(Modifier.height(26.dp))

            Text(
                "الحساسية: ${sensLabel(sensitivity.roundToInt())}",
                fontSize = 17.sp,
                color = cs.onSurface,
                fontWeight = FontWeight.Medium
            )
            Slider(
                value = sensitivity,
                onValueChange = { sensitivity = it },
                valueRange = 1f..5f,
                steps = 3,
                onValueChangeFinished = {
                    Prefs.setSensitivity(ctx, sensitivity.roundToInt())
                    ShakeService.reload(app)
                }
            )
            Spacer(Modifier.height(12.dp))

            Text("عدد الهزات", fontSize = 17.sp, color = cs.onSurface, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(1 to "هزة", 2 to "هزتين", 3 to "3 هزات").forEach { (n, label) ->
                    FilterChip(
                        selected = shakes == n,
                        onClick = {
                            shakes = n
                            Prefs.setShakeCount(ctx, n)
                            ShakeService.reload(app)
                        },
                        label = { Text(label, fontSize = 15.sp) },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = cs.secondaryContainer,
                            selectedLabelColor = cs.onSecondaryContainer
                        )
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("اهتزاز خفيف", fontSize = 17.sp, color = cs.onSurface, fontWeight = FontWeight.Medium)
                    Text("يأكدلك إن الهزة اتسجلت", fontSize = 14.sp, color = cs.onSurfaceVariant)
                }
                Switch(
                    checked = vibrate,
                    onCheckedChange = {
                        vibrate = it
                        Prefs.setVibrate(ctx, it)
                    }
                )
            }
        }

        // ===== كارت طريقة اللقطة =====
        SectionCard(
            icon = painterResource(R.drawable.ic_logo_glyph),
            title = "طريقة اللقطة",
            desc = "إزاي نطلب السكرين شوت من النظام. التلقائي بيجرب الأسرع الأول وبعدين التاني.",
            expanded = expMethod,
            onToggle = { expMethod = !expMethod }
        ) {
            OptionRow(
                selected = method == Prefs.METHOD_AUTO,
                title = "تلقائي (مُستحسن)",
                sub = "خدمة الوصول لو شغالة، وإلا زرار النظام بالروت"
            ) { method = Prefs.METHOD_AUTO; Prefs.setMethod(ctx, method) }
            OptionRow(
                selected = method == Prefs.METHOD_ROOT,
                title = "زرار النظام بالروت",
                sub = "بيبعت مفتاح السكرين شوت (SysRq) للنظام"
            ) { method = Prefs.METHOD_ROOT; Prefs.setMethod(ctx, method) }
            OptionRow(
                selected = method == Prefs.METHOD_ACCESS,
                title = "خدمة الوصول",
                sub = "أمر السكرين شوت الرسمي في أندرويد"
            ) { method = Prefs.METHOD_ACCESS; Prefs.setMethod(ctx, method) }

            Spacer(Modifier.height(16.dp))
            StatusLine(ok = accOn, okText = "خدمة الوصول مفعّلة", badText = "خدمة الوصول مش مفعّلة")
            Spacer(Modifier.height(20.dp))

            if (!accOn) {
                PillButton(
                    text = if (root == true) "تفعيل بالروت" else "فتح الإعدادات",
                    icon = Icons.Filled.Build,
                    style = PillStyle.Tonal,
                    onClick = {
                        scope.launch {
                            val ok = if (root == true) {
                                withContext(Dispatchers.IO) { AccessHelper.enableViaRoot(app) }
                            } else false
                            if (!ok) {
                                ctx.startActivity(
                                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                            delay(1200)
                            accOn = AccessHelper.isEnabled(ctx)
                        }
                    }
                )
                Spacer(Modifier.height(14.dp))
            }

            PillButton(
                text = if (countdown > 0) "اللقطة بعد $countdown..." else "جرّب لقطة",
                icon = Icons.Filled.PlayArrow,
                style = PillStyle.Filled,
                enabled = countdown == 0,
                onClick = {
                    scope.launch {
                        for (i in 3 downTo 1) {
                            countdown = i
                            delay(1000)
                        }
                        countdown = 0
                        delay(150)
                        val ok = withContext(Dispatchers.IO) { ScreenshotTrigger.take(app) }
                        if (ok) shots = Prefs.shots(ctx)
                        else toast(ctx, "اللقطة فشلت، جرب طريقة تانية")
                    }
                }
            )
        }

        // ===== كارت الروت =====
        SectionCard(
            icon = painterResource(R.drawable.ic_root),
            title = "صلاحية الروت",
            desc = "الروت بيخلي لقطة تبعت زرار السكرين شوت للنظام مباشرة، وتفعّل خدمة الوصول وتستثني نفسها من توفير البطارية.",
            expanded = expRoot,
            onToggle = { expRoot = !expRoot }
        ) {
            when (root) {
                null -> StatusLine(ok = false, okText = "", badText = "جاري الفحص...")
                true -> StatusLine(ok = true, okText = "الروت شغال", badText = "")
                false -> StatusLine(ok = false, okText = "", badText = "مفيش روت أو الصلاحية اترفضت")
            }
            Spacer(Modifier.height(20.dp))
            PillButton(
                text = "إعادة الفحص",
                icon = Icons.Filled.Refresh,
                style = PillStyle.Outlined,
                onClick = {
                    scope.launch {
                        root = null
                        root = withContext(Dispatchers.IO) { RootShell.hasRoot() }
                    }
                }
            )
        }

        Spacer(Modifier.height(40.dp))
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("تمام") }
            },
            title = { Text("لقطة") },
            text = {
                Text(
                    "سكرين شوت بهزة واحدة، بنظام الموبايل نفسه.\nالإصدار 1.0\nro.buroot.laqta",
                    lineHeight = 24.sp
                )
            }
        )
    }
}

@Composable
fun SectionCard(
    icon: Painter,
    title: String,
    desc: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val circle = if (isSystemInDarkTheme()) NavyDark else Navy
    Surface(
        shape = RoundedCornerShape(40.dp),
        color = cs.surfaceVariant,
        border = BorderStroke(1.dp, cs.outline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 26.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .clickable(onClick = onToggle)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(circle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.width(20.dp))
                Text(
                    title,
                    fontSize = 26.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Medium,
                    color = cs.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (!expanded) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = cs.onSurface)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(desc, fontSize = 16.sp, lineHeight = 26.sp, color = cs.onSurfaceVariant)
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(24.dp))
                    content()
                }
            }
        }
    }
}

@Composable
fun StatusLine(ok: Boolean, okText: String, badText: String) {
    val color = if (ok) OkGreen else BadRed
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            if (ok) okText else badText,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun OptionRow(selected: Boolean, title: String, sub: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(Modifier.padding(start = 4.dp)) {
            Text(title, fontSize = 17.sp, color = cs.onSurface)
            Text(sub, fontSize = 13.sp, color = cs.onSurfaceVariant)
        }
    }
}

enum class PillStyle { Filled, Tonal, Outlined }

@Composable
fun PillButton(
    text: String,
    icon: ImageVector?,
    style: PillStyle,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(50)
    val mod = Modifier.height(60.dp)
    val padding = PaddingValues(horizontal = 30.dp)
    val content: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(text, fontSize = 18.sp)
    }
    when (style) {
        PillStyle.Filled -> Button(
            onClick = onClick,
            modifier = mod,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (dark) NavyDark else Navy,
                contentColor = Color.White
            ),
            contentPadding = padding,
            content = content
        )
        PillStyle.Tonal -> Button(
            onClick = onClick,
            modifier = mod,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (dark) MauveDark else Mauve,
                contentColor = Color.White
            ),
            contentPadding = padding,
            content = content
        )
        PillStyle.Outlined -> OutlinedButton(
            onClick = onClick,
            modifier = mod,
            enabled = enabled,
            shape = shape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            contentPadding = padding,
            content = content
        )
    }
}
