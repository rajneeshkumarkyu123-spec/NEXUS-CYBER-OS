package com.nexus.cyberos

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

val G = Color(0xFF00FF41)
val DG = Color(0xFF0A7A24)
val PK = Color(0xFFFF2BD6)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NexusApp() }
    }
}

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

@Composable
fun Mono(s: String, c: Color = G, sz: Int = 14, m: Modifier = Modifier) {
    androidx.compose.material3.Text(s, color = c, fontFamily = FontFamily.Monospace, fontSize = sz.sp, modifier = m)
}

@Composable
fun Tile(t: String, m: Modifier = Modifier, on: () -> Unit) {
    Box(
        m.border(1.dp, G, RoundedCornerShape(8.dp))
            .background(Color(0x99001A08))
            .clickable { on() }
            .padding(vertical = 22.dp),
        contentAlignment = Alignment.Center
    ) { Mono(t, G, 16) }
}

@Composable
fun NexusApp() {
    var screen by remember { mutableStateOf("home") }
    BackHandler(screen != "home") { screen = "home" }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        MatrixRain()
        when (screen) {
            "home" -> Home { screen = it }
            "apps" -> Drawer()
            "radar" -> Radar()
            "sys" -> Sys()
            else -> Terminal()
        }
    }
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
                paint.alpha = (140 - k * 11).coerceAtLeast(8)
                val ch = chars[(i * 7 + k + (t * 40).toInt()) % chars.size].toString()
                drawContext.canvas.nativeCanvas.drawText(ch, i * cw, y, paint)
            }
        }
    }
}

@Composable
fun Clock() {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(1000) } }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Mono(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now), G, 48)
        Mono(SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(now), DG, 16)
    }
}

@Composable
fun Home(go: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(48.dp))
        Clock()
        Spacer(Modifier.height(40.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile("APPS", Modifier.weight(1f)) { go("apps") }
            Tile("WIFI RADAR", Modifier.weight(1f)) { go("radar") }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile("SYSTEM", Modifier.weight(1f)) { go("sys") }
            Tile("TERMINAL", Modifier.weight(1f)) { go("term") }
        }
    }
}

@Composable
fun Drawer() {
    val c = LocalContext.current
    val apps = remember { loadApps(c) }
    var q by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Mono("> APPS (${apps.size})")
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = q, onValueChange = { q = it }, singleLine = true,
            textStyle = TextStyle(color = G, fontFamily = FontFamily.Monospace, fontSize = 18.sp),
            cursorBrush = SolidColor(G),
            modifier = Modifier.fillMaxWidth().border(1.dp, DG).padding(10.dp),
            decorationBox = { inner ->
                if (q.isEmpty()) Mono("search...", DG, 18)
                inner()
            }
        )
        LazyColumn {
            items(apps.filter { it.label.contains(q, ignoreCase = true) }) { a ->
                Mono(
                    a.label, G, 18,
                    Modifier.fillMaxWidth().clickable {
                        c.packageManager.getLaunchIntentForPackage(a.pkg)?.let { c.startActivity(it) }
                    }.padding(vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
fun Radar() {
    val c = LocalContext.current
    val wm = remember { c.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    var res by remember { mutableStateOf(listOf<ScanResult>()) }
    var status by remember { mutableStateOf("ready") }
    fun hasPerm() = c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    fun scan() {
        if (!hasPerm()) { status = "Location permission chahiye (Wi-Fi scan ke liye)"; return }
        if (!wm.isWifiEnabled) { status = "Wi-Fi off hai, on karo"; return }
        val ok = wm.startScan()
        res = wm.scanResults.sortedByDescending { it.level }
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        status = (if (ok) "scan ok" else "Android ne scan throttle kiya, purane results") + " @ " + time
    }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { scan() }
    LaunchedEffect(Unit) {
        if (hasPerm()) scan() else perm.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    val sweep by rememberInfiniteTransition(label = "sw").animateFloat(
        0f, 360f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "a"
    )
    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Mono("> WIFI RADAR  ${res.size} networks")
        Mono(status, DG, 12)
        Canvas(Modifier.size(280.dp)) {
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
        Mono("distance = signal strength (real). angle = sirf display ke liye.", DG, 11)
        Tile("SCAN NOW", Modifier.fillMaxWidth().padding(vertical = 8.dp)) { scan() }
        LazyColumn(Modifier.weight(1f)) {
            items(res) { r ->
                val name = if (r.SSID.isNullOrEmpty()) "(hidden)" else r.SSID
                Mono(
                    "$name  ${r.level}dBm  ${r.frequency}MHz\n${r.capabilities}", G, 12,
                    Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun Sys() {
    val c = LocalContext.current
    var lines by remember { mutableStateOf(listOf<String>()) }
    LaunchedEffect(Unit) { while (true) { lines = sysInfo(c); delay(2000) } }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Mono("> SYSTEM MONITOR (live)")
        Spacer(Modifier.height(16.dp))
        lines.forEach { Mono(it, G, 15, Modifier.padding(vertical = 6.dp)) }
    }
}

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
    }
}
