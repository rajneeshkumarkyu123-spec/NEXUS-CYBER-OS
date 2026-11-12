package com.nexus.cyberos

import android.Manifest
import android.app.ActivityManager
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.role.RoleManager
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult as BleResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Typeface
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.Uri
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.IBinder
import android.os.StatFs
import android.provider.Settings
import android.util.LruCache
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ---------- constants ----------
val THEMES = listOf<Pair<String, Long>>(
    "MATRIX" to 0xFF00FF41, "CYAN" to 0xFF00E5FF, "MAGENTA" to 0xFFFF2BD6, "AMBER" to 0xFFFFB300,
    "RED ALERT" to 0xFFFF3B30, "ICE" to 0xFFB3E5FC, "LIME" to 0xFFC6FF00, "ORANGE" to 0xFFFF6D00,
    "PURPLE" to 0xFFB388FF, "GOLD" to 0xFFFFD700, "MINT" to 0xFF69F0AE, "PINK" to 0xFFFF80AB,
    "SKY" to 0xFF40C4FF, "TEAL" to 0xFF1DE9B6, "CORAL" to 0xFFFF8A65, "VIOLET" to 0xFF7C4DFF,
    "WHITE" to 0xFFFFFFFF, "BLOOD" to 0xFFD50000, "TOXIC" to 0xFF76FF03, "STEEL" to 0xFF90A4AE
)

fun tf(n: String, s: Int = Typeface.NORMAL) = FontFamily(Typeface.create(n, s))

val FONTS: List<Pair<String, FontFamily>> by lazy {
    listOf(
        "MONO" to tf("monospace"), "MONO BOLD" to tf("monospace", Typeface.BOLD),
        "MONO ITALIC" to tf("monospace", Typeface.ITALIC), "SERIF MONO" to tf("serif-monospace"),
        "SANS" to tf("sans-serif"), "SANS LIGHT" to tf("sans-serif-light"),
        "SANS THIN" to tf("sans-serif-thin"), "SANS MEDIUM" to tf("sans-serif-medium"),
        "SANS BLACK" to tf("sans-serif-black"), "CONDENSED" to tf("sans-serif-condensed"),
        "COND LIGHT" to tf("sans-serif-condensed-light"), "COND BOLD" to tf("sans-serif-condensed", Typeface.BOLD),
        "SANS ITALIC" to tf("sans-serif", Typeface.ITALIC), "LIGHT ITALIC" to tf("sans-serif-light", Typeface.ITALIC),
        "SERIF" to tf("serif"), "SERIF BOLD" to tf("serif", Typeface.BOLD),
        "SERIF ITALIC" to tf("serif", Typeface.ITALIC), "CASUAL" to tf("casual"),
        "CURSIVE" to tf("cursive"), "SMALL CAPS" to tf("sans-serif-smallcaps")
    )
}

val ICON_SIZES = listOf(36, 42, 50)
val CLOCKS = listOf("DIGITAL", "SECONDS", "STACKED", "COMPACT", "ANALOG", "HEX")
const val Q1 = "Aapka nickname?"
const val Q2 = "Aapki favourite jagah?"
val BLUE = Color(0xFF2979FF)
val PK = Color(0xFFFF2BD6)

data class Placed(val pkg: String, val x: Float, val y: Float)
data class AppInfo(val label: String, val pkg: String)
data class BtDev(val name: String, val addr: String, val rssi: Int)

fun sha(s: String): String =
    MessageDigest.getInstance("SHA-256").digest(s.trim().lowercase().toByteArray()).joinToString("") { "%02x".format(it) }

fun fm(p: String, d: Date): String = SimpleDateFormat(p, Locale.getDefault()).format(d)

// ---------- settings ----------
object Cfg {
    private lateinit var sp: SharedPreferences
    var accent by mutableStateOf(0)
    var font by mutableStateOf(0)
    var bg by mutableStateOf(0)
    var iconShape by mutableStateOf(0)
    var iconSize by mutableStateOf(1)
    var iconTint by mutableStateOf(0)
    var clockStyle by mutableStateOf(0)
    var labels by mutableStateOf(true)
    var hideBar by mutableStateOf(true)
    var overLock by mutableStateOf(true)
    var powerLock by mutableStateOf(false)
    var radarOn by mutableStateOf(true)
    var showWifi by mutableStateOf(true)
    var showBt by mutableStateOf(true)
    var dtLock by mutableStateOf(true)
    var dtUnlock by mutableStateOf(true)
    var askedHome by mutableStateOf(false)
    var passcode by mutableStateOf("1337")
    var ans1 by mutableStateOf("")
    var ans2 by mutableStateOf("")
    val dock = mutableStateListOf("", "", "", "")
    val side = mutableStateListOf("", "", "", "", "")
    val placed = mutableStateListOf<Placed>()

    private fun load(l: MutableList<String>, s: String) {
        val p = s.split(",")
        for (i in l.indices) l[i] = p.getOrElse(i) { "" }
    }

    fun init(c: Context) {
        sp = c.getSharedPreferences("nexus2", Context.MODE_PRIVATE)
        accent = sp.getInt("accent", 0).coerceIn(0, THEMES.size - 1)
        font = sp.getInt("font", 0).coerceIn(0, FONTS.size - 1)
        bg = sp.getInt("bg", 0).coerceIn(0, 2)
        iconShape = sp.getInt("iconShape", 0).coerceIn(0, 2)
        iconSize = sp.getInt("iconSize", 1).coerceIn(0, 2)
        iconTint = sp.getInt("iconTint", 0).coerceIn(0, 1)
        clockStyle = sp.getInt("clockStyle", 0).coerceIn(0, CLOCKS.size - 1)
        labels = sp.getBoolean("labels", true)
        hideBar = sp.getBoolean("hideBar", true)
        overLock = sp.getBoolean("overLock", true)
        powerLock = sp.getBoolean("powerLock", false)
        radarOn = sp.getBoolean("radarOn", true)
        showWifi = sp.getBoolean("showWifi", true)
        showBt = sp.getBoolean("showBt", true)
        dtLock = sp.getBoolean("dtLock", true)
        dtUnlock = sp.getBoolean("dtUnlock", true)
        askedHome = sp.getBoolean("askedHome", false)
        passcode = sp.getString("passcode", "1337") ?: "1337"
        ans1 = sp.getString("ans1", "") ?: ""
        ans2 = sp.getString("ans2", "") ?: ""
        load(dock, sp.getString("dock", "") ?: "")
        load(side, sp.getString("side", "") ?: "")
        placed.clear()
        (sp.getString("placed", "") ?: "").split(";").forEach {
            val p = it.split("|")
            if (p.size == 3) {
                val x = p[1].toFloatOrNull()
                val y = p[2].toFloatOrNull()
                if (x != null && y != null) placed.add(Placed(p[0], x, y))
            }
        }
    }

    fun save() {
        sp.edit()
            .putInt("accent", accent).putInt("font", font).putInt("bg", bg)
            .putInt("iconShape", iconShape).putInt("iconSize", iconSize).putInt("iconTint", iconTint)
            .putInt("clockStyle", clockStyle)
            .putBoolean("labels", labels).putBoolean("hideBar", hideBar).putBoolean("overLock", overLock)
            .putBoolean("powerLock", powerLock).putBoolean("radarOn", radarOn)
            .putBoolean("showWifi", showWifi).putBoolean("showBt", showBt)
            .putBoolean("dtLock", dtLock).putBoolean("dtUnlock", dtUnlock).putBoolean("askedHome", askedHome)
            .putString("passcode", passcode).putString("ans1", ans1).putString("ans2", ans2)
            .putString("dock", dock.joinToString(","))
            .putString("side", side.joinToString(","))
            .putString("placed", placed.joinToString(";") { "${it.pkg}|${it.x}|${it.y}" })
            .apply()
    }
}

val G: Color get() = Color(THEMES[Cfg.accent].second)
val DG: Color get() = Color(THEMES[Cfg.accent].second).copy(alpha = 0.55f)
val FF: FontFamily get() = FONTS[Cfg.font].second

var screenState by mutableStateOf("lock")
var panelOpen by mutableStateOf(false)
var torch by mutableStateOf(false)
var setPage by mutableStateOf("main")
var tick by mutableStateOf(0)
var appVisible by mutableStateOf(true)
var placeX by mutableStateOf(0.4f)
var placeY by mutableStateOf(0.45f)
var dragPkg by mutableStateOf<String?>(null)
var askedPerms = false
var winApply: () -> Unit = {}

fun openSettings(page: String = "main") { setPage = page; screenState = "settings"; panelOpen = false }

object Repo { var apps by mutableStateOf(listOf<AppInfo>()) }

object Icons {
    private val cache = LruCache<String, ImageBitmap>(120)
    fun peek(p: String): ImageBitmap? = cache.get(p)
    fun load(c: Context, p: String): ImageBitmap? {
        cache.get(p)?.let { return it }
        val b = runCatching { c.packageManager.getApplicationIcon(p).toBitmap(96, 96).asImageBitmap() }.getOrNull()
        if (b != null) cache.put(p, b)
        return b
    }
}

// ---------- sound ----------
object Snd {
    private var tg: ToneGenerator? = null
    private fun play(t: Int, ms: Int) {
        try {
            if (tg == null) tg = ToneGenerator(AudioManager.STREAM_MUSIC, 40)
            tg?.startTone(t, ms)
        } catch (e: Exception) {
        }
    }
    fun click() = play(ToneGenerator.TONE_PROP_BEEP2, 40)
    fun ok() = play(ToneGenerator.TONE_PROP_ACK, 200)
    fun err() = play(ToneGenerator.TONE_PROP_NACK, 200)
}

fun Modifier.tap(on: () -> Unit): Modifier = this.clickable { Snd.click(); on() }

// ---------- service (power button) ----------
class LockService : Service() {
    private val rcv = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, i: Intent) {
            if (i.action == Intent.ACTION_SCREEN_OFF) {
                if (screenState != "aod") screenState = "lock"
                panelOpen = false
            } else if (i.action == Intent.ACTION_SCREEN_ON) {
                runCatching {
                    ctx.startActivity(
                        Intent(ctx, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    )
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("nexus", "NEXUS lock", NotificationManager.IMPORTANCE_MIN))
        val n = Notification.Builder(this, "nexus")
            .setContentTitle("NEXUS CYBER OS")
            .setContentText("lock service active")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
        ServiceCompat.startForeground(
            this, 1, n,
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )
        val f = IntentFilter()
        f.addAction(Intent.ACTION_SCREEN_OFF)
        f.addAction(Intent.ACTION_SCREEN_ON)
        ContextCompat.registerReceiver(this, rcv, f, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(rcv) }
        super.onDestroy()
    }
}

fun setService(c: Context, on: Boolean) {
    val i = Intent(c, LockService::class.java)
    if (on) runCatching { ContextCompat.startForegroundService(c, i) } else c.stopService(i)
}

// ---------- activity ----------
class MainActivity : ComponentActivity() {
    private val off = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (screenState != "aod") { screenState = "lock"; panelOpen = false }
        }
    }

    fun applyWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        if (Build.VERSION.SDK_INT >= 28) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(Cfg.overLock)
            setTurnScreenOn(Cfg.overLock)
        } else {
            val f = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            if (Cfg.overLock) window.addFlags(f) else window.clearFlags(f)
        }
        val ic = WindowCompat.getInsetsController(window, window.decorView)
        if (Cfg.hideBar) {
            ic.hide(WindowInsetsCompat.Type.statusBars())
            ic.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            ic.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Cfg.init(this)
        winApply = { applyWindow() }
        applyWindow()
        if (Cfg.powerLock) setService(this, true)
        ContextCompat.registerReceiver(this, off, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
        setContent { NexusApp() }
    }

    override fun onStart() { super.onStart(); appVisible = true }
    override fun onStop() { appVisible = false; super.onStop() }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyWindow()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME) && screenState != "lock" && screenState != "aod") {
            screenState = "home"
            panelOpen = false
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(off) }
        super.onDestroy()
    }
}

// ---------- helpers ----------
fun loadApps(c: Context): List<AppInfo> {
    val pm = c.packageManager
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(i, 0)
        .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
        .distinctBy { it.pkg }
        .sortedBy { it.label.lowercase() }
}

fun autoFill(apps: List<AppInfo>) {
    if (apps.isEmpty()) return
    if (!Cfg.dock.all { it.isBlank() } || !Cfg.side.all { it.isBlank() }) return
    val pref = listOf(
        "com.google.android.dialer", "com.android.dialer", "com.whatsapp", "com.android.chrome",
        "com.google.android.youtube", "com.instagram.android", "com.google.android.apps.maps",
        "com.google.android.gm", "com.android.vending", "org.telegram.messenger",
        "com.google.android.apps.photos", "com.android.camera", "com.motorola.camera3"
    )
    val ordered = pref.mapNotNull { p -> apps.find { it.pkg == p } } + apps.filter { a -> pref.none { it == a.pkg } }
    val it = ordered.iterator()
    for (i in Cfg.dock.indices) if (it.hasNext()) Cfg.dock[i] = it.next().pkg
    for (i in Cfg.side.indices) if (it.hasNext()) Cfg.side[i] = it.next().pkg
    Cfg.save()
}

fun allPerms(): Array<String> {
    val l = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
    if (Build.VERSION.SDK_INT >= 31) { l.add(Manifest.permission.BLUETOOTH_SCAN); l.add(Manifest.permission.BLUETOOTH_CONNECT) }
    if (Build.VERSION.SDK_INT >= 33) l.add(Manifest.permission.POST_NOTIFICATIONS)
    return l.toTypedArray()
}

fun requestHomeRole(c: Context, l: ActivityResultLauncher<Intent>) {
    if (Build.VERSION.SDK_INT >= 29) {
        val rm = c.getSystemService(RoleManager::class.java)
        if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
            l.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
        }
    }
}

fun proceedUnlock(act: ComponentActivity) {
    val km = act.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    if (km.isKeyguardLocked) {
        km.requestDismissKeyguard(act, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() { screenState = "home" }
            override fun onDismissError() { Snd.err() }
            override fun onDismissCancelled() { Snd.err() }
        })
    } else {
        screenState = "home"
    }
}

fun startFinger(act: ComponentActivity, onOk: () -> Unit, onFail: (String) -> Unit) {
    if (Build.VERSION.SDK_INT < 28) { onFail("Android 9+ chahiye"); return }
    try {
        val b = BiometricPrompt.Builder(act)
            .setTitle("NEXUS CYBER OS")
            .setSubtitle("Fingerprint scan")
            .setNegativeButton("PASSCODE", act.mainExecutor) { _, _ -> onFail("") }
        if (Build.VERSION.SDK_INT >= 30) {
            b.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
        }
        b.build().authenticate(CancellationSignal(), act.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) { onOk() }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) { onFail(errString?.toString() ?: "") }
            override fun onAuthenticationFailed() { Snd.err() }
        })
    } catch (e: Exception) {
        onFail("Fingerprint nahi mila, pehle add karo")
    }
}

fun enrollFinger(c: Context) {
    val i = if (Build.VERSION.SDK_INT >= 30) {
        Intent(Settings.ACTION_BIOMETRIC_ENROLL).putExtra(
            Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BiometricManager.Authenticators.BIOMETRIC_STRONG
        )
    } else if (Build.VERSION.SDK_INT >= 28) Intent(Settings.ACTION_FINGERPRINT_ENROLL)
    else Intent(Settings.ACTION_SECURITY_SETTINGS)
    runCatching { c.startActivity(i) }
}

fun toggleTorch(c: Context) {
    runCatching {
        val cm = c.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
        if (id != null) { torch = !torch; cm.setTorchMode(id, torch) }
    }
}

fun setBright(c: Context, v: Float) {
    if (Settings.System.canWrite(c)) {
        Settings.System.putInt(c.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0)
        Settings.System.putInt(c.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (v * 255).toInt().coerceIn(1, 255))
    }
}

fun sysInfo(c: Context): List<String> {
    val am = c.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val mi = ActivityManager.MemoryInfo()
    am.getMemoryInfo(mi)
    val st = StatFs(Environment.getDataDirectory().path)
    val bm = c.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val bat = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    fun gb(b: Long) = String.format("%.1f GB", b / 1073741824.0)
    return listOf(
        "DEVICE  : ${Build.MANUFACTURER} ${Build.MODEL}",
        "ANDROID : ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "CPU     : ${Runtime.getRuntime().availableProcessors()} cores, ${Build.SUPPORTED_ABIS.first()}",
        "RAM     : ${gb(mi.totalMem - mi.availMem)} / ${gb(mi.totalMem)}",
        "STORAGE : ${gb(st.totalBytes - st.availableBytes)} / ${gb(st.totalBytes)}",
        "BATTERY : $bat%"
    )
}

object WifiHub {
    var res by mutableStateOf(listOf<ScanResult>())
    var status by mutableStateOf("ready")

    fun hasPerm(c: Context) =
        c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun scan(c: Context) {
        val wm = c.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!hasPerm(c)) { status = "Location permission do"; return }
        if (!wm.isWifiEnabled) { status = "Wi-Fi off hai"; return }
        try {
            val ok = wm.startScan()
            res = wm.scanResults.sortedByDescending { it.level }
            status = (if (ok) "wifi ok" else "wifi throttled") + " @ " + fm("HH:mm:ss", Date())
        } catch (e: SecurityException) {
            status = "permission error"
        }
    }
}

object BtHub {
    var devs by mutableStateOf(listOf<BtDev>())
    var status by mutableStateOf("bt ready")

    fun has(c: Context) = allPerms().all {
        it == Manifest.permission.POST_NOTIFICATIONS || c.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun scanOnce(c: Context) {
        if (!has(c)) { status = "Bluetooth permission do"; return }
        val ad = (c.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (ad == null || !ad.isEnabled) { status = "Bluetooth off hai"; return }
        val sc = ad.bluetoothLeScanner ?: return
        val found = HashMap<String, BtDev>()
        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: BleResult) {
                val n = result.scanRecord?.deviceName ?: runCatching { result.device.name }.getOrNull() ?: "BLE device"
                found[result.device.address] = BtDev(n, result.device.address, result.rssi)
            }
        }
        try {
            sc.startScan(cb)
            status = "bt scanning..."
            delay(5000)
            sc.stopScan(cb)
            devs = found.values.sortedByDescending { it.rssi }
            status = "bt ok"
        } catch (e: SecurityException) {
            status = "bt permission error"
        }
    }
}

// ---------- small UI pieces ----------
@Composable
fun Mono(s: String, c: Color = G, sz: Int = 14, m: Modifier = Modifier) {
    Text(s, color = c, fontFamily = FF, fontSize = sz.sp, modifier = m)
}

@Composable
fun Tile(t: String, m: Modifier = Modifier, pad: Int = 22, sz: Int = 16, on: () -> Unit) {
    Box(
        m.border(1.dp, G, RoundedCornerShape(8.dp))
            .background(Color(0x99001A08))
            .tap(on)
            .padding(vertical = pad.dp),
        contentAlignment = Alignment.Center
    ) { Mono(t, G, sz) }
}

@Composable
fun Toggle(label: String, on: Boolean, f: () -> Unit) {
    Mono(if (on) "[x] $label" else "[ ] $label", G, 15, Modifier.fillMaxWidth().tap(f).padding(vertical = 10.dp))
}

@Composable
fun Field(v: String, on: (String) -> Unit, hint: String, pw: Boolean = false, m: Modifier = Modifier, done: (() -> Unit)? = null) {
    BasicTextField(
        value = v, onValueChange = on, singleLine = true,
        visualTransformation = if (pw) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { done?.invoke() }),
        textStyle = TextStyle(color = G, fontFamily = FF, fontSize = 18.sp),
        cursorBrush = SolidColor(G),
        modifier = m.fillMaxWidth().border(1.dp, DG).padding(10.dp),
        decorationBox = { inner ->
            if (v.isEmpty()) Mono(hint, DG, 16)
            inner()
        }
    )
}

// ---------- backgrounds ----------
val CODE = listOf(
    "> compiling :app:compileDebugKotlin ...", "[ OK ] kotlinc 2.0.20 loaded", "import android.net.wifi.WifiManager",
    "fun scan(c: Context) {", "    val wm = c.getSystemService(WIFI_SERVICE)", "    wm.startScan()", "}",
    "[ OK ] dex merge complete", "> :app:mergeDebugResources UP-TO-DATE", "val key = sha256(input)",
    "while (true) { delay(1000) }", "[WARN] unreachable code", "  at kernel.sched.run(sched.c:204)",
    "0x7ffc1a2b : 48 89 e5 48 83 ec 10", "BUILD SUCCESSFUL in 8s", "\$ gradle assembleDebug --stacktrace",
    "class NexusOS : Launcher() {", "    override fun onHome() = lock()", "}", "[ OK ] proxy tor:9050 up",
    "ssh root@10.0.0.1 -p 22", "ld: linking libnexus.so", "if (access == GRANTED) open(vault)",
    "Task :app:packageDebug", "[ OK ] signing apk ...", "git push origin main", "ERROR: segfault (ignored)",
    "nmap -sS 192.168.1.0/24"
)

@Composable
fun CodeBg() {
    val paint = remember {
        android.graphics.Paint().apply { textSize = 24f; typeface = Typeface.MONOSPACE }
    }
    Canvas(Modifier.fillMaxSize()) {
        val lh = 34f
        val n = (size.height / lh).toInt() + 2
        val off = tick * 3f
        val base = (off / lh).toInt()
        val shift = off % lh
        paint.color = G.toArgb()
        for (i in 0 until n) {
            val line = CODE[(base + i) % CODE.size]
            paint.alpha = if (line.startsWith("[ OK ]") || line.startsWith("BUILD")) 130 else 70
            drawContext.canvas.nativeCanvas.drawText(line, 12f, i * lh - shift, paint)
        }
    }
}

@Composable
fun MatrixRain() {
    val chars = remember { "01ABCDEF#<>@%".toList() }
    val paint = remember { android.graphics.Paint().apply { textSize = 26f } }
    Canvas(Modifier.fillMaxSize()) {
        val cw = 30f
        val cols = (size.width / cw).toInt()
        paint.color = G.toArgb()
        for (i in 0 until cols) {
            val y0 = ((tick * (5f + (i * 37) % 7) + i * 131f) % (size.height + 300f))
            for (k in 0 until 8) {
                val y = y0 - k * 30f
                if (y < 0f || y > size.height) continue
                paint.alpha = (110 - k * 12).coerceAtLeast(6)
                val ch = chars[(i * 7 + k + tick / 3) % chars.size].toString()
                drawContext.canvas.nativeCanvas.drawText(ch, i * cw, y, paint)
            }
        }
    }
}

// ---------- clock ----------
@Composable
fun Analog(now: Date, sz: Int) {
    val cal = Calendar.getInstance().apply { time = now }
    val h = cal.get(Calendar.HOUR) + cal.get(Calendar.MINUTE) / 60f
    val m = cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f
    val s = cal.get(Calendar.SECOND).toFloat()
    Canvas(Modifier.size(sz.dp)) {
        val c0 = center
        val r = size.minDimension / 2f
        drawCircle(DG, r, c0, style = Stroke(2f))
        for (i in 0 until 12) {
            val a = Math.toRadians(i * 30.0).toFloat()
            drawLine(DG, Offset(c0.x + cos(a) * r * 0.88f, c0.y + sin(a) * r * 0.88f), Offset(c0.x + cos(a) * r, c0.y + sin(a) * r), 2f)
        }
        fun hand(deg: Float, len: Float, col: Color, w: Float) {
            val a = Math.toRadians((deg - 90f).toDouble()).toFloat()
            drawLine(col, c0, Offset(c0.x + cos(a) * r * len, c0.y + sin(a) * r * len), w)
        }
        hand(h * 30f, 0.5f, G, 6f)
        hand(m * 6f, 0.75f, G, 4f)
        hand(s * 6f, 0.9f, PK, 2f)
    }
}

@Composable
fun ClockView(now: Date, style: Int) {
    when (style) {
        0 -> Column { Mono(fm("HH:mm", now), G, 46); Mono(fm("EEEE", now), G, 15); Mono(fm("dd MMM yyyy", now), DG, 14) }
        1 -> Column { Mono(fm("HH:mm:ss", now), G, 34); Mono(fm("EEE, dd MMM", now), DG, 13) }
        2 -> Column { Mono(fm("HH", now), G, 52); Mono(fm("mm", now), PK, 52); Mono(fm("dd MMM", now), DG, 13) }
        3 -> Column { Mono(fm("HH:mm", now), G, 26); Mono(fm("EEE dd MMM", now), DG, 12) }
        4 -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Analog(now, 120); Mono(fm("EEE dd MMM", now), DG, 12) }
        else -> {
            val cal = Calendar.getInstance().apply { time = now }
            Column {
                Mono("0x" + Integer.toHexString(cal.get(Calendar.HOUR_OF_DAY)) + ":0x" + Integer.toHexString(cal.get(Calendar.MINUTE)), G, 24)
                Mono(fm("HH:mm", now), DG, 14)
                Mono(fm("EEE dd MMM", now), DG, 12)
            }
        }
    }
}

// ---------- radar ----------
@Composable
fun RadarCanvas(dpSize: Int) {
    val sweep by rememberInfiniteTransition(label = "sw").animateFloat(
        0f, 360f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "a"
    )
    val wifi = WifiHub.res
    val bt = BtHub.devs
    Canvas(Modifier.size(dpSize.dp)) {
        val c0 = center
        val r0 = size.minDimension / 2f
        for (f in listOf(0.33f, 0.66f, 1f)) drawCircle(DG, r0 * f, c0, style = Stroke(2f))
        drawLine(DG, Offset(c0.x - r0, c0.y), Offset(c0.x + r0, c0.y))
        drawLine(DG, Offset(c0.x, c0.y - r0), Offset(c0.x, c0.y + r0))
        rotate(sweep, c0) { drawLine(G, c0, Offset(c0.x + r0, c0.y), strokeWidth = 4f) }
        if (Cfg.showWifi) wifi.forEach { r ->
            val key = (r.BSSID ?: r.SSID ?: "").hashCode()
            val a = Math.toRadians(key.mod(360).toDouble()).toFloat()
            val strength = ((r.level + 100) / 70f).coerceIn(0f, 1f)
            val d = ((1f - strength) * 0.85f + 0.1f) * r0
            drawCircle(PK, 7f, Offset(c0.x + cos(a) * d, c0.y + sin(a) * d))
        }
        if (Cfg.showBt) bt.forEach { b ->
            val a = Math.toRadians(b.addr.hashCode().mod(360).toDouble()).toFloat()
            val strength = ((b.rssi + 100) / 60f).coerceIn(0f, 1f)
            val d = ((1f - strength) * 0.85f + 0.1f) * r0
            val p = Offset(c0.x + cos(a) * d, c0.y + sin(a) * d)
            drawCircle(BLUE, 7f, p)
            drawCircle(BLUE, 12f, p, style = Stroke(2f))
        }
    }
}

@Composable
fun RadarLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Mono("● WIFI ${WifiHub.res.size}", PK, 10)
        Mono("● BT ${BtHub.devs.size}", BLUE, 10)
    }
}

// ---------- status bar ----------
@Composable
fun StatusBar() {
    val c = LocalContext.current
    var now by remember { mutableStateOf(Date()) }
    var bat by remember { mutableStateOf(0) }
    var net by remember { mutableStateOf("...") }
    var spd by remember { mutableStateOf("0 KB/s") }
    LaunchedEffect(appVisible) {
        if (appVisible) {
            val bm = c.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val cm = c.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            var last = TrafficStats.getTotalRxBytes().coerceAtLeast(0L) + TrafficStats.getTotalTxBytes().coerceAtLeast(0L)
            while (true) {
                now = Date()
                bat = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                val caps = cm.getNetworkCapabilities(cm.activeNetwork)
                net = when {
                    caps == null -> "NO NET"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "DATA ON"
                    else -> "NET"
                }
                val cur = TrafficStats.getTotalRxBytes().coerceAtLeast(0L) + TrafficStats.getTotalTxBytes().coerceAtLeast(0L)
                val d = (cur - last).coerceAtLeast(0L)
                last = cur
                spd = if (d >= 1048576L) String.format("%.1f MB/s", d / 1048576.0) else "${d / 1024} KB/s"
                delay(1000)
            }
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Mono(fm("HH:mm", now), G, 14)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Mono(spd, DG, 11)
            Mono(net, G, 11)
            Box(Modifier.size(22.dp, 11.dp).border(1.dp, G)) {
                Box(
                    Modifier.fillMaxHeight().fillMaxWidth(bat.coerceIn(0, 100) / 100f)
                        .background(if (bat < 20) PK else G)
                )
            }
            Mono("$bat%", G, 11)
        }
    }
}

// ---------- root ----------
@Composable
fun NexusApp() {
    val s = screenState
    val c = LocalContext.current
    LaunchedEffect(Unit) {
        if (Repo.apps.isEmpty()) Repo.apps = withContext(Dispatchers.IO) { loadApps(c) }
        autoFill(Repo.apps)
    }
    LaunchedEffect(appVisible) {
        if (appVisible) while (true) { tick += 1; delay(70) }
    }
    BackHandler(true) {
        when {
            panelOpen -> panelOpen = false
            s == "settings" && setPage != "main" -> setPage = "main"
            s != "home" && s != "lock" && s != "aod" -> screenState = "home"
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (s != "aod") {
            when (Cfg.bg) {
                0 -> CodeBg()
                1 -> MatrixRain()
                else -> {}
            }
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            if (s != "lock" && s != "aod") StatusBar()
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (s) {
                    "lock" -> LockScreen()
                    "aod" -> Aod()
                    "settings" -> SettingsScreen()
                    "drawer" -> Drawer()
                    "place" -> PlacePicker()
                    "wifi" -> WifiList()
                    "sys" -> Sys()
                    "term" -> Terminal()
                    else -> Home()
                }
            }
        }
        if (panelOpen && s != "lock" && s != "aod") ControlPanel()
    }
}

// ---------- lock screen ----------
@Composable
fun FingerAnim(active: Boolean, onTap: () -> Unit) {
    val t by rememberInfiniteTransition(label = "fp").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "fpt"
    )
    Canvas(Modifier.size(92.dp).clip(CircleShape).border(1.dp, if (active) PK else G, CircleShape).tap(onTap)) {
        val c0 = center
        val col = if (active) PK else G
        for (i in 1..6) {
            val r = size.minDimension / 2f * (0.18f + i * 0.13f)
            drawArc(col, 200f + i * 12f, 250f - i * 8f, false, Offset(c0.x - r, c0.y - r), Size(2 * r, 2 * r), style = Stroke(3f))
        }
        if (active) {
            val y = size.height * t
            drawLine(PK, Offset(0f, y), Offset(size.width, y), 4f)
        }
    }
}

@Composable
fun LockScreen() {
    val act = LocalContext.current as ComponentActivity
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(appVisible) { if (appVisible) while (true) { now = Date(); delay(1000) } }
    var pin by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var reset by remember { mutableStateOf(false) }
    var a1 by remember { mutableStateOf("") }
    var a2 by remember { mutableStateOf("") }
    var np by remember { mutableStateOf("") }
    val log = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()

    fun anim(first: String) {
        if (busy) return
        busy = true
        log.clear()
        scope.launch {
            for (l in listOf(first, "bruteforcing vault", "decrypting keystore", "ACCESS GRANTED")) {
                log.add("[*] $l"); Snd.click(); delay(350)
            }
            Snd.ok(); delay(300)
            busy = false; pin = ""
            proceedUnlock(act)
        }
    }

    fun tryUnlock() {
        if (busy) return
        if (pin != Cfg.passcode) { Snd.err(); msg = "ACCESS DENIED"; pin = ""; return }
        anim("sha256 match " + Integer.toHexString(pin.hashCode()))
    }

    fun finger() {
        if (busy) return
        scanning = true
        startFinger(act, { scanning = false; anim("FINGERPRINT MATCHED") }, { scanning = false; msg = it })
    }

    Column(
        Modifier.fillMaxSize().imePadding().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Image(painterResource(R.drawable.ic_fg), null, Modifier.size(80.dp))
        Box(
            Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onTap = { Snd.click(); screenState = "aod" },
                    onDoubleTap = { if (Cfg.dtUnlock) { Snd.ok(); proceedUnlock(act) } else Snd.err() }
                )
            }
        ) { ClockView(now, Cfg.clockStyle) }
        Spacer(Modifier.height(20.dp))
        FingerAnim(scanning || busy) { finger() }
        Spacer(Modifier.height(8.dp))
        if (msg.isNotEmpty()) Mono(msg, PK, 13)
        Spacer(Modifier.height(8.dp))
        if (!reset) {
            Field(pin, { if (!busy) pin = it }, "PASSCODE", true, done = { tryUnlock() })
            Spacer(Modifier.height(10.dp))
            Tile("UNLOCK", Modifier.fillMaxWidth(), 14, 15) { tryUnlock() }
            Spacer(Modifier.height(8.dp))
            Mono("FORGOT PASSCODE?", DG, 12, Modifier.tap { reset = true; msg = "" }.padding(8.dp))
        } else {
            Mono(Q1, DG, 12)
            Field(a1, { a1 = it }, "answer 1")
            Spacer(Modifier.height(6.dp))
            Mono(Q2, DG, 12)
            Field(a2, { a2 = it }, "answer 2")
            Spacer(Modifier.height(6.dp))
            Field(np, { np = it }, "NAYA PASSCODE", true)
            Spacer(Modifier.height(10.dp))
            Tile("RESET PASSCODE", Modifier.fillMaxWidth(), 12, 14) {
                if (Cfg.ans1.isEmpty() || Cfg.ans2.isEmpty()) {
                    msg = "Pehle Settings me answers set karo"
                } else if (sha(a1) == Cfg.ans1 && sha(a2) == Cfg.ans2 && np.length >= 3) {
                    Cfg.passcode = np; Cfg.save(); Snd.ok(); msg = "PASSCODE RESET OK"; reset = false; a1 = ""; a2 = ""; np = ""
                } else { Snd.err(); msg = "ANSWERS GALAT (ya passcode chhota)" }
            }
            Spacer(Modifier.height(6.dp))
            Mono("BACK", DG, 12, Modifier.tap { reset = false }.padding(8.dp))
        }
        Spacer(Modifier.height(10.dp))
        log.forEach { Mono(it, DG, 13) }
    }
}

// ---------- AOD ----------
@Composable
fun Aod() {
    val act = LocalContext.current as ComponentActivity
    DisposableEffect(Unit) {
        val w = act.window
        val lp = w.attributes
        val old = lp.screenBrightness
        lp.screenBrightness = 0.02f
        w.attributes = lp
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            val l2 = w.attributes
            l2.screenBrightness = old
            w.attributes = l2
            w.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(appVisible) { if (appVisible) while (true) { now = Date(); delay(10_000) } }
    val shift = ((now.time / 60000) % 6).toInt() * 8
    Box(
        Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) {
            detectTapGestures(onDoubleTap = { Snd.ok(); screenState = "lock" })
        }
    ) {
        Column(
            Modifier.align(Alignment.Center).offset(x = shift.dp, y = (-shift).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Mono(fm("HH:mm", now), DG, 56)
            Mono(fm("EEE, dd MMM", now), DG, 14)
        }
    }
}

// ---------- home ----------
@Composable
fun AppCell(a: AppInfo) {
    val c = LocalContext.current
    val icon by produceState<ImageBitmap?>(Icons.peek(a.pkg), a.pkg) {
        if (value == null) value = withContext(Dispatchers.Default) { Icons.load(c, a.pkg) }
    }
    val sz = ICON_SIZES[Cfg.iconSize].dp
    val shape = when (Cfg.iconShape) {
        0 -> RoundedCornerShape(10.dp)
        1 -> CircleShape
        else -> RoundedCornerShape(0.dp)
    }
    Column(
        Modifier.padding(2.dp)
            .tap { c.packageManager.getLaunchIntentForPackage(a.pkg)?.let { c.startActivity(it) } }
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val ic = icon
        if (ic != null) {
            Image(
                ic, null,
                Modifier.size(sz).clip(shape).border(1.dp, DG, shape),
                colorFilter = if (Cfg.iconTint == 1) ColorFilter.tint(G, BlendMode.Modulate) else null
            )
        } else {
            Box(Modifier.size(sz).clip(shape).border(1.dp, G, shape), contentAlignment = Alignment.Center) {
                Mono(a.label.take(1), G, 20)
            }
        }
        if (Cfg.labels) {
            Text(
                a.label, color = G, fontFamily = FF, fontSize = 9.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun Slot(pkg: String, map: Map<String, AppInfo>, m: Modifier = Modifier) {
    val a = map[pkg]
    if (a == null) {
        Box(
            m.padding(4.dp).size(ICON_SIZES[Cfg.iconSize].dp).border(1.dp, DG).tap { openSettings("slots") },
            contentAlignment = Alignment.Center
        ) { Mono("+", DG, 20) }
    } else {
        Box(m, contentAlignment = Alignment.Center) { AppCell(a) }
    }
}

@Composable
fun PlacedApp(p: Placed, w: Float, h: Float, map: Map<String, AppInfo>) {
    val a = map[p.pkg] ?: return
    Box(
        Modifier.offset { IntOffset((p.x * w).roundToInt(), (p.y * h).roundToInt()) }
            .pointerInput(p.pkg) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { dragPkg = p.pkg },
                    onDragEnd = {
                        val i = Cfg.placed.indexOfFirst { it.pkg == p.pkg }
                        if (i >= 0 && Cfg.placed[i].y > 0.86f) Cfg.placed.removeAt(i)
                        Cfg.save()
                        dragPkg = null
                    },
                    onDragCancel = { dragPkg = null },
                    onDrag = { ch, d ->
                        ch.consume()
                        val i = Cfg.placed.indexOfFirst { it.pkg == p.pkg }
                        if (i >= 0) {
                            val q = Cfg.placed[i]
                            Cfg.placed[i] = q.copy(
                                x = (q.x + d.x / w).coerceIn(0f, 0.88f),
                                y = (q.y + d.y / h).coerceIn(0f, 0.97f)
                            )
                        }
                    }
                )
            }
    ) { AppCell(a) }
}

@Composable
fun Home() {
    val c = LocalContext.current
    val map = remember(Repo.apps) { Repo.apps.associateBy { it.pkg } }
    val roleL = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    fun askHome() {
        if (!Cfg.askedHome) { Cfg.askedHome = true; Cfg.save(); requestHomeRole(c, roleL) }
    }
    val permL = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        WifiHub.scan(c); askHome()
    }
    LaunchedEffect(Unit) {
        if (!askedPerms) {
            askedPerms = true
            val need = allPerms().filter { c.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
            if (need.isNotEmpty()) permL.launch(need.toTypedArray()) else askHome()
        }
    }
    LaunchedEffect(appVisible) {
        if (appVisible) while (true) {
            if (WifiHub.hasPerm(c)) WifiHub.scan(c)
            BtHub.scanOnce(c)
            delay(25_000)
        }
    }
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(appVisible) { if (appVisible) while (true) { now = Date(); delay(1000) } }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = with(LocalDensity.current) { maxWidth.toPx() }
        val h = with(LocalDensity.current) { maxHeight.toPx() }
        Column(
            Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { if (Cfg.dtLock) { Snd.ok(); screenState = "lock" } },
                        onLongPress = { o ->
                            placeX = (o.x / size.width).coerceIn(0f, 0.85f)
                            placeY = (o.y / size.height).coerceIn(0f, 0.85f)
                            Snd.click(); screenState = "place"
                        }
                    )
                }
                .pointerInput(Unit) {
                    var total = 0f
                    var startY = 0f
                    detectVerticalDragGestures(
                        onDragStart = { o -> startY = o.y; total = 0f },
                        onDragEnd = {
                            if (total > 120f && startY < size.height * 0.4f) { Snd.click(); panelOpen = true }
                            else if (total < -120f) { Snd.click(); screenState = "drawer" }
                        },
                        onVerticalDrag = { _, d -> total += d }
                    )
                }
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { Cfg.clockStyle = (Cfg.clockStyle + 1) % CLOCKS.size; Cfg.save(); Snd.click() },
                            onDoubleTap = { if (Cfg.dtLock) { Snd.ok(); screenState = "lock" } }
                        )
                    }
                ) { ClockView(now, Cfg.clockStyle) }
                if (Cfg.radarOn) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.tap { screenState = "wifi" }) { RadarCanvas(150) }
                        RadarLegend()
                    }
                }
            }
            Row(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp)) {
                Column(Modifier.fillMaxHeight().width(76.dp), verticalArrangement = Arrangement.SpaceEvenly) {
                    for (i in 0 until 5) Slot(Cfg.side[i], map)
                }
                Column(
                    Modifier.weight(1f).fillMaxHeight().padding(end = 4.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Tile("ALL APPS", Modifier.width(110.dp), 8, 12) { screenState = "drawer" }
                    Spacer(Modifier.height(6.dp))
                    Tile("SETTINGS", Modifier.width(110.dp), 8, 12) { openSettings() }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp)
                    .border(1.dp, DG, RoundedCornerShape(14.dp)).padding(6.dp)
            ) {
                for (i in 0 until 4) Slot(Cfg.dock[i], map, Modifier.weight(1f))
            }
        }
        Cfg.placed.forEach { p -> key(p.pkg) { PlacedApp(p, w, h, map) } }
        if (dragPkg != null) {
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 4.dp)
                    .border(1.dp, PK).background(Color(0xCC200010)).padding(10.dp),
                contentAlignment = Alignment.Center
            ) { Mono("YAHAN CHHODO = HATAO", PK, 13) }
        }
    }
}

// ---------- pickers / drawer ----------
@Composable
fun AppPicker(title: String, clearOpt: Boolean, onPick: (AppInfo?) -> Unit, onCancel: () -> Unit) {
    var q by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Mono("> $title", G, 16)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (clearOpt) Tile("CLEAR", Modifier.weight(1f), 10, 13) { onPick(null) }
            Tile("CANCEL", Modifier.weight(1f), 10, 13) { onCancel() }
        }
        Spacer(Modifier.height(8.dp))
        Field(q, { q = it }, "search...")
        LazyColumn(Modifier.weight(1f)) {
            items(Repo.apps.filter { it.label.contains(q, ignoreCase = true) }) { a ->
                Mono(a.label, G, 16, Modifier.fillMaxWidth().tap { onPick(a) }.padding(vertical = 10.dp))
            }
        }
    }
}

@Composable
fun PlacePicker() {
    AppPicker("APP CHUNO (yahan lagega)", false, { a ->
        if (a != null) {
            Cfg.placed.removeAll { it.pkg == a.pkg }
            Cfg.placed.add(Placed(a.pkg, placeX, placeY))
            Cfg.save()
        }
        screenState = "home"
    }, { screenState = "home" })
}

@Composable
fun Drawer() {
    val c = LocalContext.current
    var q by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { Repo.apps = withContext(Dispatchers.IO) { loadApps(c) } }
    Column(Modifier.fillMaxSize().padding(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Mono("> ALL APPS (${Repo.apps.size})", G, 16)
            Tile("HOME", Modifier.width(90.dp), 8, 12) { screenState = "home" }
        }
        Spacer(Modifier.height(8.dp))
        Field(q, { q = it }, "search...")
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(GridCells.Fixed(4), Modifier.weight(1f).fillMaxWidth()) {
            gridItems(Repo.apps.filter { it.label.contains(q, ignoreCase = true) }) { a -> AppCell(a) }
        }
    }
}

// ---------- control panel ----------
class PT(val sym: String, val name: String, val on: Boolean = false, val f: () -> Unit)

@Composable
fun PanelTile(t: PT, m: Modifier) {
    Column(
        m.border(1.dp, if (t.on) PK else G, RoundedCornerShape(10.dp))
            .background(Color(0x99001A08)).tap(t.f).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Mono(t.sym, if (t.on) PK else G, 22)
        Mono(t.name, G, 10)
    }
}

@Composable
fun SliderRow(sym: String, name: String, value: Float, on: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Mono("$sym $name", G, 12, Modifier.width(96.dp))
        Slider(
            value = value, onValueChange = on, modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = G, activeTrackColor = G, inactiveTrackColor = DG)
        )
    }
}

@Composable
fun ControlPanel() {
    val c = LocalContext.current
    val am = remember { c.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val mx = remember { am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var vol by remember { mutableStateOf(am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / mx) }
    var bright by remember {
        mutableStateOf(runCatching { Settings.System.getInt(c.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f }.getOrDefault(0.5f))
    }
    val canW = Settings.System.canWrite(c)
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(appVisible) { if (appVisible) while (true) { now = Date(); delay(1000) } }
    fun go(i: Intent) { panelOpen = false; runCatching { c.startActivity(i) } }
    fun page(s: String) { panelOpen = false; screenState = s }
    val tiles = listOf(
        PT("◉", "WIFI") { go(Intent(if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_WIFI else Settings.ACTION_WIFI_SETTINGS)) },
        PT("⇅", "DATA") { go(Intent(if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_INTERNET_CONNECTIVITY else Settings.ACTION_DATA_ROAMING_SETTINGS)) },
        PT("✦", "BLUETOOTH") { go(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
        PT("✈", "AIRPLANE") { go(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)) },
        PT("☼", if (torch) "TORCH ON" else "TORCH", torch) { toggleTorch(c) },
        PT("♪", "SOUND") { go(Intent(Settings.ACTION_SOUND_SETTINGS)) },
        PT("☎", "PHONE SET") { go(Intent(Settings.ACTION_SETTINGS)) },
        PT("⏻", "LOCK") { page("lock") },
        PT("▣", "APPS") { page("drawer") },
        PT("◎", "RADAR") { page("wifi") },
        PT("⚙", "SYSTEM") { page("sys") },
        PT(">_", "TERMINAL") { page("term") },
        PT("☰", "SETTINGS") { openSettings() },
        PT("⌂", "HOME") { page("home") },
        PT("✖", "CLOSE") { panelOpen = false }
    )
    Box(
        Modifier.fillMaxSize().background(Color(0xF2000000))
            .pointerInput(Unit) { detectTapGestures(onTap = { panelOpen = false }) }
            .pointerInput(Unit) { detectVerticalDragGestures(onVerticalDrag = { _, d -> if (d < -20f) panelOpen = false }) }
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(14.dp).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Mono("> CONTROL PANEL", G, 16)
                Mono(fm("HH:mm", now), G, 16)
            }
            Spacer(Modifier.height(10.dp))
            SliderRow("☼", "BRIGHT", bright) { v -> bright = v; setBright(c, v) }
            SliderRow("♪", "VOLUME", vol) { v -> vol = v; am.setStreamVolume(AudioManager.STREAM_MUSIC, (v * mx).roundToInt(), 0) }
            if (!canW) {
                Mono(
                    "BRIGHTNESS PERMISSION DO (tap)", PK, 11,
                    Modifier.tap {
                        go(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + c.packageName)))
                    }.padding(vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            tiles.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { PanelTile(it, Modifier.weight(1f)) }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ---------- settings ----------
@Composable
fun PageFrame(title: String, body: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
        Mono("> $title", G, 18)
        Spacer(Modifier.height(10.dp))
        Tile("BACK", Modifier.fillMaxWidth(), 12, 13) { setPage = "main" }
        Spacer(Modifier.height(12.dp))
        body()
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun SettingsScreen() {
    when (setPage) {
        "colors" -> PageColors()
        "fonts" -> PageFonts()
        "icons" -> PageIcons()
        "clock" -> PageClock()
        "bg" -> PageBg()
        "radar" -> PageRadar()
        "slots" -> PageSlots()
        "lock" -> PageLock()
        "launcher" -> PageLauncher()
        else -> SettingsMain()
    }
}

@Composable
fun SettingsMain() {
    val items = listOf(
        "colors" to "[+] COLORS (20 naam ke saath)",
        "fonts" to "[+] FONTS (20)",
        "icons" to "[+] APP ICONS",
        "clock" to "[+] CLOCK STYLE (6)",
        "bg" to "[+] BACKGROUND",
        "radar" to "[+] RADAR (wifi / bluetooth)",
        "slots" to "[+] HOME APPS",
        "lock" to "[+] LOCK, FINGERPRINT, RESET",
        "launcher" to "[+] LAUNCHER"
    )
    Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
        Mono("> SETTINGS", G, 18)
        Spacer(Modifier.height(10.dp))
        Tile("BACK TO HOME", Modifier.fillMaxWidth(), 12, 13) { screenState = "home" }
        Spacer(Modifier.height(12.dp))
        items.forEach { (k, n) ->
            Tile(n, Modifier.fillMaxWidth(), 16, 14) { setPage = k }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
fun PageColors() = PageFrame("COLORS") {
    THEMES.forEachIndexed { i, th ->
        val col = Color(th.second)
        Row(
            Modifier.fillMaxWidth().tap { Cfg.accent = i; Cfg.save() }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(24.dp).background(col))
            Spacer(Modifier.width(12.dp))
            Mono((if (Cfg.accent == i) "> " else "  ") + th.first, col, 16)
        }
    }
}

@Composable
fun PageFonts() = PageFrame("FONTS") {
    FONTS.forEachIndexed { i, f ->
        Text(
            (if (Cfg.font == i) "> " else "  ") + f.first + "  Abc 123",
            color = G, fontFamily = f.second, fontSize = 17.sp,
            modifier = Modifier.fillMaxWidth().tap { Cfg.font = i; Cfg.save() }.padding(vertical = 10.dp)
        )
    }
}

@Composable
fun PageIcons() = PageFrame("APP ICONS") {
    Mono("SHAPE", DG, 12)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("ROUND", "CIRCLE", "SQUARE").forEachIndexed { i, n ->
            Tile(if (Cfg.iconShape == i) "[$n]" else n, Modifier.weight(1f), 10, 11) { Cfg.iconShape = i; Cfg.save() }
        }
    }
    Spacer(Modifier.height(14.dp))
    Mono("SIZE", DG, 12)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("SMALL", "MEDIUM", "LARGE").forEachIndexed { i, n ->
            Tile(if (Cfg.iconSize == i) "[$n]" else n, Modifier.weight(1f), 10, 11) { Cfg.iconSize = i; Cfg.save() }
        }
    }
    Spacer(Modifier.height(14.dp))
    Mono("COLOR", DG, 12)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("ORIGINAL", "HACKER TINT").forEachIndexed { i, n ->
            Tile(if (Cfg.iconTint == i) "[$n]" else n, Modifier.weight(1f), 10, 11) { Cfg.iconTint = i; Cfg.save() }
        }
    }
    Spacer(Modifier.height(10.dp))
    Toggle("App ke naam (label) dikhao", Cfg.labels) { Cfg.labels = !Cfg.labels; Cfg.save() }
}

@Composable
fun PageClock() {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(appVisible) { if (appVisible) while (true) { now = Date(); delay(1000) } }
    PageFrame("CLOCK STYLE") {
        ClockView(now, Cfg.clockStyle)
        Spacer(Modifier.height(14.dp))
        CLOCKS.forEachIndexed { i, n ->
            Tile(if (Cfg.clockStyle == i) "[x] $n" else n, Modifier.fillMaxWidth(), 14, 14) { Cfg.clockStyle = i; Cfg.save() }
            Spacer(Modifier.height(8.dp))
        }
        Mono("Home par clock ko long-press karo = style badlo", DG, 11)
    }
}

@Composable
fun PageBg() = PageFrame("BACKGROUND") {
    listOf("COMPILER CODE (scrolling)", "MATRIX RAIN", "PLAIN BLACK").forEachIndexed { i, n ->
        Tile(if (Cfg.bg == i) "[x] $n" else n, Modifier.fillMaxWidth(), 16, 13) { Cfg.bg = i; Cfg.save() }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun PageRadar() {
    val l = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    PageFrame("RADAR") {
        Toggle("Radar home par dikhao", Cfg.radarOn) { Cfg.radarOn = !Cfg.radarOn; Cfg.save() }
        Toggle("Wi-Fi dikhao (pink)", Cfg.showWifi) { Cfg.showWifi = !Cfg.showWifi; Cfg.save() }
        Toggle("Bluetooth dikhao (neela)", Cfg.showBt) { Cfg.showBt = !Cfg.showBt; Cfg.save() }
        Spacer(Modifier.height(10.dp))
        Tile("GRANT WIFI + BLUETOOTH PERMISSION", Modifier.fillMaxWidth(), 12, 12) { l.launch(allPerms()) }
    }
}

@Composable
fun PageSlots() {
    var pick by remember { mutableStateOf<Pair<Boolean, Int>?>(null) }
    val p = pick
    if (p != null) {
        AppPicker("APP CHUNO", true, { a ->
            val v = a?.pkg ?: ""
            if (p.first) Cfg.dock[p.second] = v else Cfg.side[p.second] = v
            Cfg.save(); pick = null
        }, { pick = null })
        return
    }
    PageFrame("HOME APPS") {
        Mono("DOCK (neeche ke 4 apps)", DG, 12)
        for (i in 0 until 4) {
            val lab = Repo.apps.find { it.pkg == Cfg.dock[i] }?.label ?: "(khaali)"
            Mono("${i + 1}. $lab", G, 15, Modifier.fillMaxWidth().tap { pick = true to i }.padding(vertical = 8.dp))
        }
        Spacer(Modifier.height(12.dp))
        Mono("LEFT SIDE (5 apps)", DG, 12)
        for (i in 0 until 5) {
            val lab = Repo.apps.find { it.pkg == Cfg.side[i] }?.label ?: "(khaali)"
            Mono("${i + 1}. $lab", G, 15, Modifier.fillMaxWidth().tap { pick = false to i }.padding(vertical = 8.dp))
        }
        Spacer(Modifier.height(14.dp))
        Mono("KAHIN BHI LAGAO", DG, 12)
        Spacer(Modifier.height(6.dp))
        Tile("ADD APP ON HOME", Modifier.fillMaxWidth(), 12, 13) { placeX = 0.4f; placeY = 0.45f; screenState = "place" }
        Spacer(Modifier.height(8.dp))
        Tile("CLEAR ALL FREE APPS (${Cfg.placed.size})", Modifier.fillMaxWidth(), 12, 13) { Cfg.placed.clear(); Cfg.save() }
        Spacer(Modifier.height(8.dp))
        Mono("Home par khaali jagah long-press = app lagao. App long-press + drag = move, neeche chhodo = hatao.", DG, 11)
    }
}

@Composable
fun PageLock() {
    val c = LocalContext.current
    var newPin by remember { mutableStateOf("") }
    var s1 by remember { mutableStateOf("") }
    var s2 by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
    PageFrame("LOCK SCREEN") {
        Toggle("Home par double tap = lock", Cfg.dtLock) { Cfg.dtLock = !Cfg.dtLock; Cfg.save() }
        Toggle("Lock par double tap = unlock", Cfg.dtUnlock) { Cfg.dtUnlock = !Cfg.dtUnlock; Cfg.save() }
        Toggle("Hamara lock system lock ke upar dikhao", Cfg.overLock) { Cfg.overLock = !Cfg.overLock; Cfg.save(); winApply() }
        Toggle("POWER BUTTON = NEXUS LOCK (service)", Cfg.powerLock) {
            Cfg.powerLock = !Cfg.powerLock; Cfg.save(); setService(c, Cfg.powerLock)
        }
        Spacer(Modifier.height(6.dp))
        Tile("ALLOW DISPLAY OVER OTHER APPS", Modifier.fillMaxWidth(), 12, 12) {
            runCatching { c.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + c.packageName))) }
        }
        Spacer(Modifier.height(8.dp))
        Tile("ADD FINGERPRINT (phone settings)", Modifier.fillMaxWidth(), 12, 12) { enrollFinger(c) }
        Spacer(Modifier.height(8.dp))
        Tile("PHONE LOCK SCREEN SETTINGS", Modifier.fillMaxWidth(), 12, 12) {
            runCatching { c.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
        }
        Spacer(Modifier.height(16.dp))
        Mono("NAYA PASSCODE (min 3 akshar)", DG, 12)
        Field(newPin, { newPin = it }, "naya passcode")
        Spacer(Modifier.height(6.dp))
        Tile("SAVE PASSCODE", Modifier.fillMaxWidth(), 12, 13) {
            if (newPin.length >= 3) { Cfg.passcode = newPin; Cfg.save(); newPin = ""; info = "passcode save ho gaya" }
        }
        Spacer(Modifier.height(16.dp))
        Mono("RESET KE LIYE 2 ANSWERS", DG, 12)
        Mono(Q1, G, 13)
        Field(s1, { s1 = it }, "answer 1")
        Mono(Q2, G, 13)
        Field(s2, { s2 = it }, "answer 2")
        Spacer(Modifier.height(6.dp))
        Tile("SAVE ANSWERS", Modifier.fillMaxWidth(), 12, 13) {
            if (s1.isNotBlank() && s2.isNotBlank()) {
                Cfg.ans1 = sha(s1); Cfg.ans2 = sha(s2); Cfg.save(); s1 = ""; s2 = ""; info = "answers save ho gaye"
            }
        }
        if (info.isNotEmpty()) Mono(info, PK, 12, Modifier.padding(top = 8.dp))
    }
}

@Composable
fun PageLauncher() {
    val c = LocalContext.current
    val roleL = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    PageFrame("LAUNCHER") {
        Tile("SET AS DEFAULT HOME APP", Modifier.fillMaxWidth(), 12, 13) { requestHomeRole(c, roleL) }
        Spacer(Modifier.height(8.dp))
        Tile("HOME APP SETTINGS (phone)", Modifier.fillMaxWidth(), 12, 13) {
            runCatching { c.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
        }
        Spacer(Modifier.height(14.dp))
        Toggle("Phone ka status bar chhupao", Cfg.hideBar) { Cfg.hideBar = !Cfg.hideBar; Cfg.save(); winApply() }
    }
}

// ---------- wifi + bluetooth detail ----------
@Composable
fun WifiList() {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { WifiHub.scan(c) }
    LaunchedEffect(Unit) {
        if (!WifiHub.hasPerm(c) || !BtHub.has(c)) perm.launch(allPerms()) else WifiHub.scan(c)
    }
    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Mono("> RADAR", G, 16)
        Mono(WifiHub.status + " | " + BtHub.status, DG, 11)
        RadarCanvas(230)
        RadarLegend()
        Tile("SCAN NOW", Modifier.fillMaxWidth().padding(vertical = 8.dp), 12) {
            WifiHub.scan(c)
            scope.launch { BtHub.scanOnce(c) }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(WifiHub.res) { r ->
                val name = if (r.SSID.isNullOrEmpty()) "(hidden)" else r.SSID
                Mono("WIFI  $name  ${r.level}dBm  ${r.frequency}MHz", PK, 12, Modifier.padding(vertical = 5.dp))
            }
            items(BtHub.devs) { b ->
                Mono("BT  ${b.name}  ${b.rssi}dBm  ${b.addr}", BLUE, 12, Modifier.padding(vertical = 5.dp))
            }
        }
        Tile("BACK", Modifier.fillMaxWidth(), 12, 13) { screenState = "home" }
    }
}

// ---------- system monitor ----------
@Composable
fun Sys() {
    val c = LocalContext.current
    var lines by remember { mutableStateOf(listOf<String>()) }
    LaunchedEffect(appVisible) { if (appVisible) while (true) { lines = sysInfo(c); delay(2000) } }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Mono("> SYSTEM MONITOR (live)")
        Spacer(Modifier.height(16.dp))
        lines.forEach { Mono(it, G, 15, Modifier.padding(vertical = 6.dp)) }
        Spacer(Modifier.height(16.dp))
        Tile("BACK", Modifier.fillMaxWidth(), 12, 13) { screenState = "home" }
    }
}

// ---------- terminal ----------
@Composable
fun Terminal() {
    val c = LocalContext.current
    val log = remember { mutableStateListOf("NEXUS shell. 'help' likho.") }
    var inp by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    fun exec(cmd: String) {
        log.add("# $cmd")
        val p = cmd.trim().split(" ")
        when (p[0]) {
            "" -> {}
            "help" -> log.add("help clear date sys apps ping <host> dns <host>")
            "clear" -> log.clear()
            "date" -> log.add(Date().toString())
            "sys" -> log.addAll(sysInfo(c))
            "apps" -> log.add("${Repo.apps.size} apps installed")
            "ping" -> scope.launch {
                val h = p.getOrElse(1) { "8.8.8.8" }
                val r = withContext(Dispatchers.IO) {
                    runCatching {
                        val s = System.currentTimeMillis()
                        if (InetAddress.getByName(h).isReachable(2000)) "${System.currentTimeMillis() - s} ms" else "timeout"
                    }.getOrElse { "error: ${it.message}" }
                }
                log.add("$h : $r")
            }
            "dns" -> scope.launch {
                val h = p.getOrElse(1) { "google.com" }
                val r = withContext(Dispatchers.IO) {
                    runCatching {
                        InetAddress.getAllByName(h).joinToString { it.hostAddress ?: "" }
                    }.getOrElse { "error: ${it.message}" }
                }
                log.add("$h -> $r")
            }
            else -> log.add("command not found: ${p[0]}")
        }
    }
    Column(Modifier.fillMaxSize().imePadding().padding(12.dp)) {
        LazyColumn(Modifier.weight(1f)) { items(log) { Mono(it, G, 14, Modifier.padding(vertical = 2.dp)) } }
        Field(inp, { inp = it }, "command", done = { exec(inp); inp = "" })
        Spacer(Modifier.height(8.dp))
        Tile("BACK", Modifier.fillMaxWidth(), 12, 13) { screenState = "home" }
    }
}
