package com.autodiag.app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private val Bg = Color(0xFF0A111B)
private val Panel = Color(0xFF151F2D)
private val Mint = Color(0xFF36D6B0)
private val SecondaryText = Color(0xFF9AAABD)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Mint, background = Bg, surface = Panel, onSurface = Color.White)) {
                AutoDiagApp()
            }
        }
    }
}

@Composable
private fun AutoDiagApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { Obd2Client() }
    val simulator = remember { VehicleSimulator() }
    var tab by remember { mutableIntStateOf(0) }
    var simMode by remember { mutableStateOf(true) }
    var devices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var connected by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Готово к подключению") }
    var data by remember { mutableStateOf(VehicleData(850, 90, 0, 3.5, 14.2)) }
    var faults by remember { mutableStateOf<List<String>>(emptyList()) }
    var faultMessage by remember { mutableStateOf("Ошибки ещё не считаны") }
    var wifiHost by remember { mutableStateOf("192.168.0.10") }
    var wifiPort by remember { mutableStateOf("35000") }
    var showClearConfirm by remember { mutableStateOf(false) }

    fun pairedDevices() {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        @SuppressLint("MissingPermission")
        val paired = manager.adapter?.bondedDevices?.toList().orEmpty()
        devices = paired
        status = if (paired.isEmpty()) "Нет сопряжённых устройств. Сначала добавьте OBD2 в настройках Bluetooth Android." else "Выберите сопряжённый OBD2 адаптер."
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pairedDevices() else status = "Нет разрешения Bluetooth. Разрешите доступ в настройках Android."
    }

    LaunchedEffect(simMode, connected) {
        if (simMode) {
            while (simMode) {
                data = simulator.next()
            }
        } else if (connected) {
            while (!simMode && connected) {
                try { data = client.readVehicleData() }
                catch (e: Exception) { status = "Ошибка чтения: ${e.message ?: "нет ответа"}"; break }
                delay(1500)
            }
        }
    }

    DisposableEffect(Unit) { onDispose { client.close() } }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Удалить коды ошибок?") },
            text = { Text("Команда OBD-II Mode 04 очистит сохранённые диагностические коды и может сбросить readiness-мониторы. Сначала сохраните результаты.") },
            confirmButton = { TextButton(onClick = {
                showClearConfirm = false
                scope.launch {
                    try { status = "Ответ ECU: ${client.clearTroubleCodes()}"; faults = emptyList(); faultMessage = "Коды очищены или команда принята ECU." }
                    catch (e: Exception) { status = "Не удалось очистить: ${e.message}" }
                }
            }) { Text("Очистить") } },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("Отмена") } }
        )
    }

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                val labels = listOf("Обзор", "Связь", "Данные", "Ошибки")
                val icons = listOf(Icons.Rounded.Dashboard, Icons.Rounded.Bluetooth, Icons.Rounded.MonitorHeart, Icons.Rounded.Warning)
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = { Icon(icons[index], null) }, label = { Text(label) })
                }
            }
        }
    ) { padding ->
        when (tab) {
            0 -> Overview(padding, simMode, { simMode = it }, connected, status, data) { tab = 1 }
            1 -> ConnectionScreen(
                padding, context, devices, status, busy, connected, wifiHost, { wifiHost = it }, wifiPort, { wifiPort = it },
                onPairList = {
                    if (Build.VERSION.SDK_INT >= 31 && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    } else pairedDevices()
                },
                onConnect = { device ->
                    scope.launch {
                        busy = true
                        status = "Подключение к ${device.name ?: device.address}…"
                        try { status = client.connect(device); connected = true; simMode = false }
                        catch (e: Exception) { status = "Ошибка Bluetooth: ${e.message ?: "не удалось подключиться"}"; connected = false; client.close() }
                        busy = false
                    }
                },
                onDisconnect = { client.close(); connected = false; status = "Адаптер отключён" },
                onWifiConnect = { host, port ->
                    scope.launch {
                        busy = true
                        status = "Подключение к Wi-Fi адаптеру…"
                        try { status = client.connectWifi(host, port); connected = true; simMode = false }
                        catch (e: Exception) { status = "Ошибка Wi-Fi: ${e.message ?: "нет соединения"}"; connected = false; client.close() }
                        busy = false
                    }
                }
            )
            2 -> LiveDataScreen(padding, data, simMode)
            else -> FaultScreen(padding, faults, faultMessage, status, simMode, connected,
                onRead = { mode ->
                    if (simMode) {
                        faults = simulator.sampleFaults()
                        faultMessage = "Демонстрационные коды симулятора — не реальные неисправности."
                    } else scope.launch {
                        busy = true
                        try {
                            faults = when (mode) {
                                7 -> client.readPendingTroubleCodes()
                                10 -> client.readPermanentTroubleCodes()
                                else -> client.readTroubleCodes()
                            }
                            faultMessage = if (faults.isEmpty()) "ECU не сообщил коды этого типа (или адаптер не поддерживает ответ)." else "Коды получены от ECU. Сохраните их и проверьте причину до очистки."
                        } catch (e: Exception) { faultMessage = "Ошибка чтения: ${e.message}" }
                        busy = false
                    }
                },
                onClear = { showClearConfirm = true },
                busy = busy
            )
        }
    }
}

@Composable
private fun Overview(padding: PaddingValues, sim: Boolean, onSim: (Boolean) -> Unit, connected: Boolean, status: String, data: VehicleData, goConnect: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(top = 22.dp, bottom = 24.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("AUTODIAG", color = Mint, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 12.sp)
                    Text("Автодиагностика", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("OBD-II • диагностика двигателя", color = SecondaryText, fontSize = 12.sp)
                }
                Icon(Icons.Rounded.DirectionsCar, null, tint = Mint, modifier = Modifier.size(42.dp))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (sim) "ВИРТУАЛЬНЫЙ АВТОМОБИЛЬ" else "РЕАЛЬНЫЙ АДАПТЕР", color = SecondaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(if (sim) "Режим симулятора" else if (connected) "OBD2 подключён" else "Нет подключения", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Switch(checked = sim, onCheckedChange = onSim)
                    }
                    Text(status, color = SecondaryText, fontSize = 12.sp)
                    OutlinedButton(onClick = goConnect, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Bluetooth, null); Spacer(Modifier.width(8.dp)); Text("Подключить адаптер") }
                }
            }
        }
        item { Text("ПАРАМЕТРЫ ДВИГАТЕЛЯ", color = SecondaryText, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metric("Обороты", data.rpm?.toString() ?: "—", "об/мин", Modifier.weight(1f))
            Metric("Температура", data.coolantC?.toString() ?: "—", "°C", Modifier.weight(1f))
        } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Metric("Скорость", data.speedKmh?.toString() ?: "—", "км/ч", Modifier.weight(1f))
            Metric("Напряжение", data.voltage?.let { "%.1f".format(it) } ?: "—", "В", Modifier.weight(1f))
        } }
        item { Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Возможности", fontWeight = FontWeight.Bold)
                FeatureLine("OBD-II двигатель", "Стандартные PID и DTC")
                FeatureLine("ABS / Airbag / ESP", "Только если поддерживается автомобилем, адаптером и протоколом")
                FeatureLine("Сервисные функции", "Добавляются по маркам и подтверждённой совместимости")
            }
        } }
        item { Text("Симулятор показывает тестовые данные. Реальные параметры доступны только при подключённом совместимом адаптере и включённом зажигании.", color = SecondaryText, fontSize = 11.sp) }
    }
}

@Composable
private fun Metric(title: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = SecondaryText, fontSize = 12.sp)
            Text(value, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text(unit, color = Mint, fontSize = 11.sp)
        }
    }
}

@Composable
private fun FeatureLine(title: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(detail, color = SecondaryText, fontSize = 11.sp)
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun ConnectionScreen(
    padding: PaddingValues, context: Context, devices: List<BluetoothDevice>, status: String, busy: Boolean, connected: Boolean,
    wifiHost: String, onHost: (String) -> Unit, wifiPort: String, onPort: (String) -> Unit,
    onPairList: () -> Unit, onConnect: (BluetoothDevice) -> Unit, onDisconnect: () -> Unit, onWifiConnect: (String, Int) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Text("Подключение OBD2", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Bluetooth Classic", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Сопрягите ELM327 в настройках Android, затем выберите его здесь. Часто адаптеры используют PIN 1234 или 0000.", color = SecondaryText, fontSize = 12.sp)
                    Button(onClick = onPairList, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Показать сопряжённые устройства") }
                    if (devices.isEmpty()) Text("Список пока пуст.", color = SecondaryText, fontSize = 12.sp)
                }
            }
        }
        items(devices) { device ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Bluetooth, null, tint = Mint)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(device.name ?: "OBD2 device", fontWeight = FontWeight.SemiBold)
                        Text(device.address, color = SecondaryText, fontSize = 11.sp)
                    }
                    TextButton(onClick = { onConnect(device) }, enabled = !busy && !connected) { Text("Подключить") }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Wi-Fi адаптер", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    OutlinedTextField(value = wifiHost, onValueChange = onHost, label = { Text("IP-адрес адаптера") }, singleLine = true)
                    OutlinedTextField(value = wifiPort, onValueChange = onPort, label = { Text("TCP-порт") }, singleLine = true)
                    Text("Для многих Wi-Fi ELM327 используются адрес 192.168.0.10 и порт 35000. Значения зависят от адаптера.", color = SecondaryText, fontSize = 11.sp)
                    OutlinedButton(onClick = { onWifiConnect(wifiHost.trim(), wifiPort.toIntOrNull() ?: 35000) }, enabled = !busy && wifiHost.isNotBlank() && wifiPort.toIntOrNull()?.let { it in 1..65535 } == true && !connected, modifier = Modifier.fillMaxWidth()) { Text("Подключить по Wi-Fi") }
                }
            }
        }
        item {
            if (connected) Button(onClick = onDisconnect, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B3946))) { Text("Отключить адаптер") }
            Text(status, color = SecondaryText, fontSize = 12.sp)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun LiveDataScreen(padding: PaddingValues, data: VehicleData, sim: Boolean) {
    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Text("Live Data", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        item { Text(if (sim) "Виртуальные данные • обновляются автоматически" else "Данные от подключённого ECU", color = SecondaryText, fontSize = 12.sp) }
        item { Metric("Обороты двигателя", data.rpm?.toString() ?: "—", "об/мин") }
        item { Metric("Температура охлаждающей жидкости", data.coolantC?.toString() ?: "—", "°C") }
        item { Metric("Скорость автомобиля", data.speedKmh?.toString() ?: "—", "км/ч") }
        item { Metric("Положение дросселя", data.throttlePercent?.let { "%.1f".format(it) } ?: "—", "%") }
        item { Metric("Напряжение ECU", data.voltage?.let { "%.2f".format(it) } ?: "—", "В") }
        item { Text("Некоторые PID могут не поддерживаться конкретным автомобилем или дешёвым адаптером. Отсутствующее значение отображается как «—».", color = SecondaryText, fontSize = 11.sp) }
    }
}

@Composable
private fun FaultScreen(padding: PaddingValues, faults: List<String>, message: String, status: String, sim: Boolean, connected: Boolean, onRead: (Int) -> Unit, onClear: () -> Unit, busy: Boolean) {
    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Text("Коды неисправностей", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        item { Text(if (sim) "Демо-сценарий" else if (connected) "OBD-II • ECU" else "Нет подключения", color = SecondaryText, fontSize = 12.sp) }
        item {
            Button(onClick = { onRead(3) }, enabled = !busy && (sim || connected), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Search, null); Spacer(Modifier.width(8.dp)); Text("Считать сохранённые (Mode 03)")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onRead(7) }, enabled = !busy && (sim || connected), modifier = Modifier.weight(1f)) { Text("Ожидающие 07") }
                OutlinedButton(onClick = { onRead(10) }, enabled = !busy && (sim || connected), modifier = Modifier.weight(1f)) { Text("Постоянные 0A") }
            }
        }
        items(faults) { code ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(code, color = Mint, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(DtcDecoder.description(code), fontSize = 13.sp)
                    Text(if (sim) "Сгенерировано симулятором" else "Код считан из ответа ECU; необходима диагностика причины.", color = SecondaryText, fontSize = 11.sp)
                }
            }
        }
        item { Text(message, color = SecondaryText, fontSize = 12.sp) }
        item {
            OutlinedButton(onClick = onClear, enabled = !sim && connected && !busy, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.DeleteSweep, null); Spacer(Modifier.width(8.dp)); Text("Очистить коды ECU")
            }
        }
        item { Text("Очистка кодов не устраняет неисправность. Не стирайте ошибки до сохранения кодов и freeze-frame данных.", color = Color(0xFFFFC46B), fontSize = 11.sp) }
        item { Text(status, color = SecondaryText, fontSize = 11.sp) }
    }
}
