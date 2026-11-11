package com.nexus.cyberos

import android.Manifest
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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

// ---------- settings (saved on phone) ----------
val ACCENTS = listOf(Color(0xFF00FF41), Color(0xFF00E5FF), Color(0xFFFF2BD6), Color(0xFFFFB300))

object Cfg {
    private lateinit var sp: android.content.SharedPreferences
    var accent by mutableStateOf(0)
    var cols by mutableStateOf(4)
    var showAll by mutableStateOf(true)
    var radarOn by mutableStateOf(true)
    var passcode by mutableStateOf("1337")
    val pinned = mutableStateListOf<String>()

    fun init(c: Context) {
        sp = c.getSharedPreferences("nexus", Context.MODE_PRIVATE)
        accent = sp.getInt("accent", 0)
        cols = sp.getInt("cols", 4)
        showAll = sp.getBoolean("showAll", true)
        radarOn = sp.getBoolean("radarOn", true)
        passcode = sp.getString("passcode", "1337") ?: "1337"
        pinned.clear()
        pinned.addAll(sp.getStringSet("pinned", emptySet()) ?: emptySet())
    }

    fun save() {
        sp.edit()
            .putInt("accent", accent).putInt("cols", cols)
            .putBoolean("showAll", showAll).putBoolean("radarOn", radarOn)
            .putString("passcode", passcode).putStringSet("pinned", pinned.toSet())
            .apply()
    }
}

val G: Color get() = ACCENTS[Cfg.accent]
val DG: Color get() = ACCENTS[Cfg.accent].copy(alpha = 0.55f)
val PK = Color(0xFFFF2BD6)

var screenState by mutableStateOf("lock")

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
            if (screenState != "aod") screenState = "lock"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Cfg.init(this)
        ContextCompat.registerReceiver(this, off, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
        setContent { NexusApp() }
    }

    override fun onDestroy() {
        unregisterReceiver(off)
        super.onDestroy()
    }
}

// ---------- data helpers ----------
data class AppInfo(val label: String, val pkg: String)

fun loadApps(c: Context): List<AppInfo> {
    val pm = c.packageManager
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(i, 0)
        .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
        .distinctBy { it.pkg }
        .sortedBy { it.label.lowercase() }
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
    Text(s, color = c, fontFamily = FontFamily.Monospace, fontSize = sz.sp, modifier = m)
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
        val paint = android.graphics.Paint().apply { color = 0xFF00FF41.toInt(); textSize = 26f }
        for (i in 0 until cols) {
            val speed = 0.6f + ((i * 37) % 10) / 10f
            val head = ((t * speed * 3f + ((i * 13) % 7) / 7f) % 1f) * size.height
            for (k in 0 until 12) {
                val y = head - k * 28f
                if (y < 0f) continue
                paint.alpha = (110 - k * 8).coerceAtLeast(6)
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

// ---------- root ----------
@Composable
fun NexusApp() {
    val s = screenState
    BackHandler(true) {
        if (s != "home" && s != "lock" && s != "aod") screenState = "home"
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (s != "aod") MatrixRain()
        when (s) {
            "lock" -> LockScreen()
            "aod" -> Aod()
            "custom" -> Customize()
            "wifi" -> WifiList()
            "sys" -> Sys()
            "term" -> Terminal()
            else -> Home()
        }
    }
}

// ---------- lock screen ----------
@Composable
fun LockScreen() {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(1000) } }
    var pin by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("> passcode daalo, vault decrypt hoga") }
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
        Spacer(Modifier.height(60.dp))
        Column(
            Modifier.pointerInput(Unit) { detectTapGestures(onTap = { Snd.click(); screenState = "aod" }) },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Mono(SimpleDateFormat("HH:mm", Locale.getDefault()).format(now), G, 64)
            Mono(SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(now), DG, 16)
            Mono("(clock par tap = AOD)", DG, 11)
        }
        Spacer(Modifier.height(48.dp))
        Mono(msg, if (msg.startsWith("ACCESS")) PK else DG, 13)
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = pin, onValueChange = { if (!busy) pin = it }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { tryUnlock() }),
            textStyle = TextStyle(color = G, fontFamily = FontFamily.Monospace, fontSize = 24.sp, textAlign = TextAlign.Center),
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
        Modifier.padding(4.dp)
            .tap { c.packageManager.getLaunchIntentForPackage(a.pkg)?.let { c.startActivity(it) } }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) {
            Image(icon, null, Modifier.size(44.dp).border(1.dp, DG, RoundedCornerShape(10.dp)).padding(2.dp))
        } else {
            Box(Modifier.size(44.dp).border(1.dp, G), contentAlignment = Alignment.Center) { Mono(a.label.take(1), G, 20) }
        }
        Text(
            a.label, color = G, fontFamily = FontFamily.Monospace, fontSize = 10.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
        )
    }
}

@Composable
fun Home() {
    val c = LocalContext.current
    val apps = remember { loadApps(c) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { WifiHub.scan(c) }
    LaunchedEffect(Unit) {
        if (!WifiHub.hasPerm(c)) perm.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        while (true) {
            if (WifiHub.hasPerm(c)) WifiHub.scan(c)
            delay(30_000)
        }
    }
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(1000) } }
    val shown = if (Cfg.showAll) apps else apps.filter { it.pkg in Cfg.pinned }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Mono(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now), G, 36)
        Mono(SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(now), DG, 13)
        if (Cfg.radarOn) {
            Spacer(Modifier.height(6.dp))
            Box(Modifier.tap { screenState = "wifi" }) { RadarCanvas(190) }
            Mono("${WifiHub.res.size} networks | ${WifiHub.status}", DG, 11)
        }
        Spacer(Modifier.height(6.dp))
        LazyVerticalGrid(GridCells.Fixed(Cfg.cols), Modifier.weight(1f).fillMaxWidth()) {
            gridItems(shown) { a -> AppCell(a) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Tile("CUSTOM", Modifier.weight(1f), 12, 11) { screenState = "custom" }
            Tile("WIFI", Modifier.weight(1f), 12, 11) { screenState = "wifi" }
            Tile("SYS", Modifier.weight(1f), 12, 11) { screenState = "sys" }
            Tile("TERM", Modifier.weight(1f), 12, 11) { screenState = "term" }
            Tile("LOCK", Modifier.weight(1f), 12, 11) { screenState = "lock" }
        }
    }
}

// ---------- customize ----------
@Composable
fun Customize() {
    val c = LocalContext.current
    val apps = remember { loadApps(c) }
    var newPin by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(14.dp)) {
        item {
            Column {
                Mono("> CUSTOMIZE", G, 18)
                Spacer(Modifier.height(10.dp))
                Tile("BACK TO HOME", Modifier.fillMaxWidth(), 12, 13) { screenState = "home" }
                Spacer(Modifier.height(16.dp))
                Mono("COLOR", DG, 12)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ACCENTS.forEachIndexed { i, col ->
                        Box(
                            Modifier.size(44.dp).background(col)
                                .border(3.dp, if (Cfg.accent == i) Color.White else Color.Transparent)
                                .tap { Cfg.accent = i; Cfg.save() }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Mono("APP COLUMNS", DG, 12)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 4, 5).forEach { n ->
                        Tile(if (Cfg.cols == n) "[$n]" else "$n", Modifier.weight(1f), 10, 14) { Cfg.cols = n; Cfg.save() }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Mono(
                    if (Cfg.radarOn) "[x] Radar home par dikhao" else "[ ] Radar home par dikhao", G, 15,
                    Modifier.fillMaxWidth().tap { Cfg.radarOn = !Cfg.radarOn; Cfg.save() }.padding(vertical = 10.dp)
                )
                Mono(
                    if (Cfg.showAll) "[x] Saare apps home par dikhao" else "[ ] Saare apps home par dikhao", G, 15,
                    Modifier.fillMaxWidth().tap { Cfg.showAll = !Cfg.showAll; Cfg.save() }.padding(vertical = 10.dp)
                )
                Spacer(Modifier.height(10.dp))
                Mono("NAYA PASSCODE (min 3 akshar)", DG, 12)
                Spacer(Modifier.height(6.dp))
                BasicTextField(
                    value = newPin, onValueChange = { newPin = it }, singleLine = true,
                    textStyle = TextStyle(color = G, fontFamily = FontFamily.Monospace, fontSize = 18.sp),
                    cursorBrush = SolidColor(G),
                    modifier = Modifier.fillMaxWidth().border(1.dp, DG).padding(10.dp)
                )
                Spacer(Modifier.height(8.dp))
                Tile("SAVE PASSCODE", Modifier.fillMaxWidth(), 12, 13) {
                    if (newPin.length >= 3) { Cfg.passcode = newPin; Cfg.save(); newPin = "" }
                }
                Spacer(Modifier.height(16.dp))
                Mono("APPS (\"saare apps\" off ho to inhe tap karke pin karo)", DG, 12)
                Spacer(Modifier.height(6.dp))
            }
        }
        items(apps) { a ->
            val on = a.pkg in Cfg.pinned
            Mono(
                (if (on) "[x] " else "[ ] ") + a.label, G, 15,
                Modifier.fillMaxWidth().tap {
                    if (on) Cfg.pinned.remove(a.pkg) else Cfg.pinned.add(a.pkg)
                    Cfg.save()
                }.padding(vertical = 8.dp)
            )
        }
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
        RadarCanvas(260)
        Mono("distance = signal strength (real). angle = sirf display ke liye.", DG, 11)
        Tile("SCAN NOW", Modifier.fillMaxWidth().padding(vertical = 8.dp), 14) { WifiHub.scan(c) }
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
            textStyle = TextStyle(color = G, fontFamily = FontFamily.Monospace, fontSize = 18.sp),
            cursorBrush = SolidColor(G),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { exec(inp); inp = "" }),
            modifier = Modifier.fillMaxWidth().border(1.dp, G).padding(10.dp)
        )
        Spacer(Modifier.height(8.dp))
        Tile("BACK", Modifier.fillMaxWidth(), 12, 13) { screenState = "home" }
    }
}
