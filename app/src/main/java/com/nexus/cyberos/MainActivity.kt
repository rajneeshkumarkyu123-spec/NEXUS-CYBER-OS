 package com.nexus.cyberos

import android.Manifest
import android.app.ActivityManager
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// ---------- 20 themes + 20 fonts ----------
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

// ---------- settings ----------
object Cfg {
    private lateinit var sp: SharedPreferences
    var accent by mutableStateOf(0)
    var font by mutableStateOf(0)
    var radarOn by mutableStateOf(true)
    var dtLock by mutableStateOf(true)
    var dtUnlock by mutableStateOf(true)
    var askedHome by mutableStateOf(false)
    var passcode by mutableStateOf("1337")
    val dock = mutableStateListOf("", "", "", "")
    val side = mutableStateListOf("", "", "", "", "")

    private fun load(l: MutableList<String>, s: String) {
        val p = s.split(",")
        for (i in l.indices) l[i] = p.getOrElse(i) { "" }
    }

    fun init(c: Context) {
        sp = c.getSharedPreferences("nexus2", Context.MODE_PRIVATE)
        accent = sp.getInt("accent", 0).coerceIn(0, THEMES.size - 1)
        font = sp.getInt("font", 0).coerceIn(0, FONTS.size - 1)
        radarOn = sp.getBoolean("radarOn", true)
        dtLock = sp.getBoolean("dtLock", true)
        dtUnlock = sp.getBoolean("dtUnlock", true)
        askedHome = sp.getBoolean("askedHome", false)
        passcode = sp.getString("passcode", "1337") ?: "1337"
        load(dock, sp.getString("dock", "") ?: "")
        load(side, sp.getString("side", "") ?: "")
    }

    fun save() {
        sp.edit()
            .putInt("accent", accent).putInt("font", font)
            .putBoolean("radarOn", radarOn).putBoolean("dtLock", dtLock)
            .putBoolean("dtUnlock", dtUnlock).putBoolean("askedHome", askedHome)
            .putString("passcode", passcode)
            .putString("dock", dock.joinToString(","))
            .putString("side", side.joinToString(","))
            .apply()
    }
}

val G: Color get() = Color(THEMES[Cfg.accent].second)
val DG: Color get() = Color(THEMES[Cfg.accent].second).copy(alpha = 0.55f)
val PK = Color(0xFFFF2BD6)
val FF: FontFamily get() = FONTS[Cfg.font].second

var screenState by mutableStateOf("lock")
var panelOpen by mutableStateOf(false)
var torch by mutableStateOf(false)

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

// ---------- activity ----------
class MainActivity : ComponentActivity() {
    private val off = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (screenState != "aod") { screenState = "lock"; panelOpen = false }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Cfg.init(this)
        ContextCompat.registerReceiver(this, off, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
        setContent { NexusApp() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME) && screenState != "lock" && screenState != "aod") {
            screenState = "home"
            panelOpen = false
        }
    }

    override fun onDestroy() {
        unregisterReceiver(off)
        super.onDestroy()
    }
}

// ---------- helpers ----------
data class AppInfo(val label: String, val pkg: String)

fun loadApps(c: Context): List<AppInfo> {
    val pm = c.packageManager
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(i, 0)
        .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
        .distinctBy { it.pkg }
        .sortedBy { it.label.lowercase() }
}

fun autoFill(apps: List<AppInfo>) {
    if (!Cfg.dock.all { it.isBlank() } || !Cfg.side.any { it.isBlank() }) return
    if (!Cfg.side.all { it.isBlank() }) return
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

fun requestHomeRole(c: Context, l: ActivityResultLauncher<Intent>) {
    if (Build.VERSION.SDK_INT >= 29) {
        val rm = c.getSystemService(RoleManager::class.java)
        if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
            l.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
        }
    }
}

fun toggleTorch(c: Context) {
    runCatching {
        val cm = c.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
        if (id != null) { torch = !torch; cm.setTorchMode(id, torch) }
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
        if (!hasPerm(c)) { status = "Location permission do (Wi-Fi scan ke liye)"; return }
        if (!wm.isWifiEnabled) { status = "Wi-Fi off hai, on karo"; return }
        try {
            val ok = wm.startScan()
            res = wm.scanResults.sortedByDescending { it.level }
            val t = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            status = (if (ok) "scan ok" else "Android ne scan throttle kiya, purane results") + " @ " + t
        } catch (e: SecurityException) {
            status = "permission error"
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
fun MatrixRain() {
    val t by rememberInfiniteTransition(label = "rain").animateFloat(
        0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "t"
    )
    val chars = remember { "01ABCDEF#<>@%".toList() }
    Canvas(Modifier.fillMaxSize()) {
        val cw = 24f
        val cols = (size.width / cw).toInt()
        val paint = android.graphics.Paint().apply { color = G.toArgb(); textSize = 26f }
        for (i in 0 until cols) {
            val speed = 0.6f + ((i * 37) % 10) / 10f
            val head = ((t * speed * 3f + ((i * 13) % 7) / 7f) % 1f) * size.height
            for (k in 0 until 12) {
                val y = head - k * 28f
                if (y < 0f) continue
                paint.alpha = (90 - k * 7).coerceAtLeast(5)
                val ch = chars[(i * 7 + k + (t * 40).toInt()) % chars.size].toString()
                drawContext.canvas.nativeCanvas.drawText(ch, i * cw, y, paint)
            }
        }
    }
}

@Composable
fun RadarCanvas(dpSize: Int) {
    val sweep by rememberInfiniteTransition(label = "sw").animateFloat(
        0f, 360f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "a"
    )
    val res = WifiHub.res
    Canvas(Modifier.size(dpSize.dp)) {
        val c0 = center
        val r0 = size.minDimension / 2f
        for (f in listOf(0.33f, 0.66f, 1f)) drawCircle(DG, r0 * f, c0, style = Stroke(2f))
        drawLine(DG, Offset(c0.x - r0, c0.y), Offset(c0.x + r0, c0.y))
        drawLine(DG, Offset(c0.x, c0.y - r0), Offset(c0.x, c0.y + r0))
        rotate(sweep, c0) { drawLine(G, c0, Offset(c0.x + r0, c0.y), strokeWidth = 4f) }
        res.forEach { r ->
            val key = (r.BSSID ?: r.SSID ?: "").hashCode()
            val a = Math.toRadians(key.mod(360).toDouble()).toFloat()
            val strength = ((r.level + 100) / 70f).coerceIn(0f, 1f)
            val d = ((1f - strength) * 0.85f + 0.1f) * r0
            drawCircle(PK, 7f, Offset(c0.x + cos(a) * d, c0.y + sin(a) * d))
        }
    }
}

@Composable
fun StatusBar() {
    val c = LocalContext.current
    var now by remember { mutableStateOf(Date()) }
    var bat by remember { mutableStateOf(0) }
    var net by remember { mutableStateOf("...") }
    var spd by remember { mutableStateOf("0 KB/s") }
    LaunchedEffect(Unit) {
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
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Mono(SimpleDateFormat("HH:mm", Locale.getDefault()).format(now), G, 14)
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
    BackHandler(true) {
        when {
            panelOpen -> panelOpen = false
            s != "home" && s != "lock" && s != "aod" -> screenState = "home"
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (s != "aod") MatrixRain()
        Column(Modifier.fillMaxSize()) {
            if (s != "lock" && s != "aod") StatusBar()
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (s) {
                    "lock" -> LockScreen()
                    "aod" -> Aod()
                    "settings" -> SettingsScreen()
                    "drawer" -> Drawer()
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
fun LockScreen() {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(1000) } }
    var pin by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("> passcode daalo ya clock par double tap") }
    var busy by remember { mutableStateOf(false) }
    val log = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()

    fun tryUnlock() {
        if (busy) return
        if (pin != Cfg.passcode) {
            Snd.err(); msg = "ACCESS DENIED (galat passcode)"; pin = ""; return
        }
        busy = true
        log.clear()
        val steps = listOf("hashing input...", "bruteforcing vault", "matching sha256 " + Integer.toHexString(pin.hashCode()), "decrypting keystore", "ACCESS GRANTED")
        scope.launch {
            for (l in steps) { log.add("[*] $l"); Snd.click(); delay(350) }
            Snd.ok(); delay(300)
            pin = ""; busy = false; screenState = "home"
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(30.dp))
        Image(painterResource(R.drawable.ic_fg), null, Modifier.size(96.dp))
        Column(
            Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onTap = { Snd.click(); screenState = "aod" },
                    onDoubleTap = { if (Cfg.dtUnlock) { Snd.ok(); screenState = "home" } else Snd.err() }
                )
            },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Mono(SimpleDateFormat("HH:mm", Locale.getDefault()).format(now), G, 60)
            Mono(SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(now), DG, 16)
            Mono("tap = AOD | double tap = unlock", DG, 11)
        }
        Spacer(Modifier.height(36.dp))
        Mono(msg, if (msg.startsWith("ACCESS")) PK else DG, 13)
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = pin, onValueChange = { if (!busy) pin = it }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { tryUnlock() }),
            textStyle = TextStyle(color = G, fontFamily = FF, fontSize = 24.sp, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(G),
            modifier = Modifier.fillMaxWidth().border(1.dp, G).padding(12.dp)
        )
        Spacer(Modifier.height(12.dp))
        Tile("UNLOCK", Modifier.fillMaxWidth()) { tryUnlock() }
        Spacer(Modifier.height(12.dp))
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
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(10_000) } }
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
            Mono(SimpleDateFormat("HH:mm", Locale.getDefault()).format(now), DG, 56)
            Mono(SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(now), DG, 14)
            Mono("double tap = ON", DG, 11)
        }
    }
}

// ---------- home ----------
@Composable
fun AppCell(a: AppInfo) {
    val c = LocalContext.current
    val icon = remember(a.pkg) {
        runCatching { c.packageManager.getApplicationIcon(a.pkg).toBitmap(96, 96).asImageBitmap() }.getOrNull()
    }
    Column(
        Modifier.padding(2.dp)
            .tap { c.packageManager.getLaunchIntentForPackage(a.pkg)?.let { c.startActivity(it) } }
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) {
            Image(icon, null, Modifier.size(42.dp).border(1.dp, DG, RoundedCornerShape(10.dp)).padding(2.dp))
        } else {
            Box(Modifier.size(42.dp).border(1.dp, G), contentAlignment = Alignment.Center) { Mono(a.label.take(1), G, 20) }
        }
        Text(
            a.label, color = G, fontFamily = FF, fontSize = 9.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
        )
    }
}

@Composable
fun Slot(pkg: String, map: Map<String, AppInfo>, m: Modifier = Modifier) {
    val a = map[pkg]
    if (a == null) {
        Box(m.padding(4.dp).size(42.dp).border(1.dp, DG).tap { screenState = "settings" }, contentAlignment = Alignment.Center) {
            Mono("+", DG, 20)
        }
    } else {
        Box(m, contentAlignment = Alignment.Center) { AppCell(a) }
    }
}

@Composable
fun Home() {
    val c = LocalContext.current
    val apps = remember { loadApps(c) }
    val map = remember(apps) { apps.associateBy { it.pkg } }
    val roleL = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    fun askHome() {
        if (!Cfg.askedHome) { Cfg.askedHome = true; Cfg.save(); requestHomeRole(c, roleL) }
    }
    val permL = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { WifiHub.scan(c); askHome() }
    LaunchedEffect(Unit) {
        autoFill(apps)
        if (!WifiHub.hasPerm(c)) permL.launch(Manifest.permission.ACCESS_FINE_LOCATION) else askHome()
        while (true) {
            if (WifiHub.hasPerm(c)) WifiHub.scan(c)
            delay(30_000)
        }
    }
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(1000) } }

    Column(
        Modifier.fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { if (Cfg.dtLock) { Snd.ok(); screenState = "lock" } })
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
            Column {
                Mono(SimpleDateFormat("HH:mm", Locale.getDefault()).format(now), G, 46)
                Mono(SimpleDateFormat("EEEE", Locale.getDefault()).format(now), G, 15)
                Mono(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(now), DG, 14)
            }
            if (Cfg.radarOn) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.tap { screenState = "wifi" }) { RadarCanvas(150) }
                    Mono("${WifiHub.res.size} networks", DG, 10)
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
                Mono("swipe up = apps\nswipe down = panel\ndouble tap = lock", DG, 10)
                Spacer(Modifier.height(8.dp))
                Tile("ALL APPS", Modifier.width(120.dp), 10, 12) { screenState = "drawer" }
                Spacer(Modifier.height(6.dp))
                Tile("SETTINGS", Modifier.width(120.dp), 10, 12) { screenState = "settings" }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp)
                .border(1.dp, DG, RoundedCornerShape(14.dp)).padding(6.dp)
        ) {
            for (i in 0 until 4) Slot(Cfg.dock[i], map, Modifier.weight(1f))
        }
    }
}

// ---------- app drawer ----------
@Composable
fun Drawer() {
    val c = LocalContext.current
    val apps = remember { loadApps(c) }
    var q by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Mono("> ALL APPS (${apps.size})", G, 16)
            Tile("HOME", Modifier.width(90.dp), 8, 12) { screenState = "home" }
        }
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = q, onValueChange = { q = it }, singleLine = true,
            textStyle = TextStyle(color = G, fontFamily = FF, fontSize = 18.sp),
            cursorBrush = SolidColor(G),
            modifier = Modifier.fillMaxWidth().border(1.dp, DG).padding(10.dp),
            decorationBox = { inner ->
                if (q.isEmpty()) Mono("search...", DG, 18)
                inner()
            }
        )
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(GridCells.Fixed(4), Modifier.weight(1f).fillMaxWidth()) {
            gridItems(apps.filter { it.label.contains(q, ignoreCase = true) }) { a -> AppCell(a) }
        }
    }
}

// ---------- control panel ----------
@Composable
fun ControlPanel() {
    val c = LocalContext.current
    fun go(i: Intent) { panelOpen = false; runCatching { c.startActivity(i) } }
    val tiles = listOf<Pair<String, () -> Unit>>(
        "WIFI" to { go(Intent(if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_WIFI else Settings.ACTION_WIFI_SETTINGS)) },
        "DATA" to { go(Intent(if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_INTERNET_CONNECTIVITY else Settings.ACTION_DATA_ROAMING_SETTINGS)) },
        "BLUETOOTH" to { go(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
        "AIRPLANE" to { go(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)) },
        "VOLUME" to { go(Intent(if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_VOLUME else Settings.ACTION_SOUND_SETTINGS)) },
        (if (torch) "TORCH: ON" else "TORCH: OFF") to { toggleTorch(c) },
        "PHONE SETTINGS" to { go(Intent(Settings.ACTION_SETTINGS)) },
        "LOCK" to { panelOpen = false; screenState = "lock" },
        "ALL APPS" to { panelOpen = false; screenState = "drawer" },
        "RADAR" to { panelOpen = false; screenState = "wifi" },
        "SYSTEM" to { panelOpen = false; screenState = "sys" },
        "TERMINAL" to { panelOpen = false; screenState = "term" },
        "LAUNCHER SETTINGS" to { panelOpen = false; screenState = "settings" },
        "HOME" to { panelOpen = false; screenState = "home" },
        "CLOSE" to { panelOpen = false }
    )
    Box(
        Modifier.fillMaxSize().background(Color(0xF2000000))
            .pointerInput(Unit) { detectTapGestures(onTap = { panelOpen = false }) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(onVerticalDrag = { _, d -> if (d < -20f) panelOpen = false })
            }
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Mono("> CONTROL PANEL", G, 18)
            Mono("upar swipe ya CLOSE se band karo", DG, 11)
            Spacer(Modifier.height(10.dp))
            tiles.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (n, f) -> Tile(n, Modifier.weight(1f), 16, 11, f) }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }
            Mono("Wi-Fi/Data seedha on-off Android allow nahi karta, tile se phone ka panel khulta hai.", DG, 10)
        }
    }
}

// ---------- settings ----------
@Composable
fun SettingsScreen() {
    val c = LocalContext.current
    val apps = remember { loadApps(c) }
    val roleL = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    var newPin by remember { mutableStateOf("") }
    var pick by remember { mutableStateOf<Pair<Boolean, Int>?>(null) }
    val p = pick

    if (p != null) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Mono("> SLOT KE LIYE APP CHUNO", G, 16)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tile("CLEAR", Modifier.weight(1f), 10, 13) {
                    if (p.first) Cfg.dock[p.second] = "" else Cfg.side[p.second] = ""
                    Cfg.save(); pick = null
                }
                Tile("CANCEL", Modifier.weight(1f), 10, 13) { pick = null }
            }
            LazyColumn(Modifier.weight(1f)) {
                items(apps) { a ->
                    Mono(
                        a.label, G, 16,
                        Modifier.fillMaxWidth().tap {
                            if (p.first) Cfg.dock[p.second] = a.pkg else Cfg.side[p.second] = a.pkg
                            Cfg.save(); pick = null
                        }.padding(vertical = 10.dp)
                    )
                }
            }
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize().padding(14.dp)) {
        item {
            Column {
                Mono("> SETTINGS", G, 18)
                Spacer(Modifier.height(10.dp))
                Tile("BACK TO HOME", Modifier.fillMaxWidth(), 12, 13) { screenState = "home" }
                Spacer(Modifier.height(8.dp))
                Tile("SET AS DEFAULT HOME APP", Modifier.fillMaxWidth(), 12, 13) { requestHomeRole(c, roleL) }
                Spacer(Modifier.height(8.dp))
                Tile("HOME APP SETTINGS (phone)", Modifier.fillMaxWidth(), 12, 13) {
                    runCatching { c.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                }
                Spacer(Modifier.height(14.dp))
                Mono(
                    if (Cfg.radarOn) "[x] Radar home par dikhao" else "[ ] Radar home par dikhao", G, 15,
                    Modifier.fillMaxWidth().tap { Cfg.radarOn = !Cfg.radarOn; Cfg.save() }.padding(vertical = 8.dp)
                )
                Mono(
                    if (Cfg.dtLock) "[x] Home par double tap = lock" else "[ ] Home par double tap = lock", G, 15,
                    Modifier.fillMaxWidth().tap { Cfg.dtLock = !Cfg.dtLock; Cfg.save() }.padding(vertical = 8.dp)
                )
                Mono(
                    if (Cfg.dtUnlock) "[x] Lock par double tap = unlock (bina passcode)" else "[ ] Lock par double tap = unlock (bina passcode)", G, 15,
                    Modifier.fillMaxWidth().tap { Cfg.dtUnlock = !Cfg.dtUnlock; Cfg.save() }.padding(vertical = 8.dp)
                )
                Spacer(Modifier.height(14.dp))
                Mono("DOCK (neeche ke 4 apps) - tap karke badlo", DG, 12)
                for (i in 0 until 4) {
                    val lab = apps.find { it.pkg == Cfg.dock[i] }?.label ?: "(khaali)"
                    Mono("${i + 1}. $lab", G, 15, Modifier.fillMaxWidth().tap { pick = true to i }.padding(vertical = 8.dp))
                }
                Spacer(Modifier.height(10.dp))
                Mono("LEFT SIDE (5 apps) - tap karke badlo", DG, 12)
                for (i in 0 until 5) {
                    val lab = apps.find { it.pkg == Cfg.side[i] }?.label ?: "(khaali)"
                    Mono("${i + 1}. $lab", G, 15, Modifier.fillMaxWidth().tap { pick = false to i }.padding(vertical = 8.dp))
                }
                Spacer(Modifier.height(14.dp))
                Mono("NAYA PASSCODE (min 3 akshar)", DG, 12)
                Spacer(Modifier.height(6.dp))
                BasicTextField(
                    value = newPin, onValueChange = { newPin = it }, singleLine = true,
                    textStyle = TextStyle(color = G, fontFamily = FF, fontSize = 18.sp),
                    cursorBrush = SolidColor(G),
                    modifier = Modifier.fillMaxWidth().border(1.dp, DG).padding(10.dp)
                )
                Spacer(Modifier.height(8.dp))
                Tile("SAVE PASSCODE", Modifier.fillMaxWidth(), 12, 13) {
                    if (newPin.length >= 3) { Cfg.passcode = newPin; Cfg.save(); newPin = "" }
                }
                Spacer(Modifier.height(14.dp))
                Mono("THEME COLOR (20)", DG, 12)
            }
        }
        items(THEMES.size) { i ->
            val col = Color(THEMES[i].second)
            Row(
                Modifier.fillMaxWidth().tap { Cfg.accent = i; Cfg.save() }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(22.dp).background(col))
                Spacer(Modifier.width(10.dp))
                Mono((if (Cfg.accent == i) "> " else "  ") + THEMES[i].first, col, 15)
            }
        }
        item {
            Column {
                Spacer(Modifier.height(14.dp))
                Mono("FONT STYLE (20)", DG, 12)
            }
        }
        items(FONTS.size) { i ->
            Text(
                (if (Cfg.font == i) "> " else "  ") + FONTS[i].first + "  Abc 123",
                color = G, fontFamily = FONTS[i].second, fontSize = 17.sp,
                modifier = Modifier.fillMaxWidth().tap { Cfg.font = i; Cfg.save() }.padding(vertical = 8.dp)
            )
        }
        item { Spacer(Modifier.height(30.dp)) }
    }
}

// ---------- wifi detail ----------
@Composable
fun WifiList() {
    val c = LocalContext.current
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { WifiHub.scan(c) }
    LaunchedEffect(Unit) {
        if (!WifiHub.hasPerm(c)) perm.launch(Manifest.permission.ACCESS_FINE_LOCATION) else WifiHub.scan(c)
    }
    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Mono("> WIFI RADAR  ${WifiHub.res.size} networks")
        Mono(WifiHub.status, DG, 12)
        RadarCanvas(240)
        Mono("distance = signal strength (real). angle = sirf display ke liye.", DG, 11)
        Tile("SCAN NOW", Modifier.fillMaxWidth().padding(vertical = 8.dp), 12) { WifiHub.scan(c) }
        LazyColumn(Modifier.weight(1f)) {
            items(WifiHub.res) { r ->
                val name = if (r.SSID.isNullOrEmpty()) "(hidden)" else r.SSID
                Mono(
                    "$name  ${r.level}dBm  ${r.frequency}MHz\n${r.capabilities}", G, 12,
                    Modifier.padding(vertical = 6.dp)
                )
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
    LaunchedEffect(Unit) { while (true) { lines = sysInfo(c); delay(2000) } }
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
            "apps" -> log.add("${loadApps(c).size} apps installed")
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
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        LazyColumn(Modifier.weight(1f)) { items(log) { Mono(it, G, 14, Modifier.padding(vertical = 2.dp)) } }
        BasicTextField(
            value = inp, onValueChange = { inp = it }, singleLine = true,
            textStyle = TextStyle(color = G, fontFamily = FF, fontSize = 18.sp),
            cursorBrush = SolidColor(G),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { exec(inp); inp = "" }),
            modifier = Modifier.fillMaxWidth().border(1.dp, G).padding(10.dp)
        )
        Spacer(Modifier.height(8.dp))
        Tile("BACK", Modifier.fillMaxWidth(), 12, 13) { screenState = "home" }
    }
}
