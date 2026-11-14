package com.autodiag.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

private val Background = Color(0xFF0B1018)
private val SurfaceDark = Color(0xFF141D29)
private val Accent = Color(0xFF35C6A2)
private val Muted = Color(0xFF91A0B4)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Accent,
                    background = Background,
                    surface = SurfaceDark,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                AutoDiagApp()
            }
        }
    }
}

@Composable
fun AutoDiagApp() {
    var selectedTab by remember { mutableStateOf(0) }
    var simulator by remember { mutableStateOf(true) }
    var scanning by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            NavigationBar(containerColor = SurfaceDark, contentColor = Color.White) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Rounded.Dashboard, null) },
                    label = { Text("Обзор") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Rounded.Bluetooth, null) },
                    label = { Text("Подключение") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Rounded.Tune, null) },
                    label = { Text("Данные") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Rounded.Settings, null) },
                    label = { Text("Ещё") }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> OverviewScreen(
                padding = padding,
                simulator = simulator,
                onSimulatorChange = { simulator = it },
                scanning = scanning,
                onScan = { scanning = !scanning }
            )
            1 -> ConnectionScreen(padding, simulator, onSimulatorChange = { simulator = it })
            2 -> DataScreen(padding)
            else -> MoreScreen(padding)
        }
    }
}

@Composable
private fun OverviewScreen(
    padding: PaddingValues,
    simulator: Boolean,
    onSimulatorChange: (Boolean) -> Unit,
    scanning: Boolean,
    onScan: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("AUTODIAG", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Диагностика автомобиля", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                }
                Surface(shape = RoundedCornerShape(12.dp), color = SurfaceDark) {
                    Icon(Icons.Rounded.DirectionsCar, null, tint = Accent, modifier = Modifier.padding(12.dp).size(28.dp))
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (simulator) "ВИРТУАЛЬНЫЙ АВТОМОБИЛЬ" else "РЕАЛЬНОЕ ПОДКЛЮЧЕНИЕ", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(if (simulator) "Тестовый режим" else "OBD2 не подключён", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Rounded.Cable, null, tint = Accent, modifier = Modifier.size(30.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Симулятор", modifier = Modifier.weight(1f), color = Muted)
                        Switch(checked = simulator, onCheckedChange = onSimulatorChange)
                    }
                    Button(
                        onClick = onScan,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color(0xFF06130F)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Radar, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (scanning) "Остановить сканирование" else "Начать диагностику", fontWeight = FontWeight.Bold)
                    }
                    if (scanning) Text("Демонстрационное сканирование запущено. Данные пока тестовые.", color = Accent, fontSize = 12.sp)
                }
            }
        }
        item {
            Text("МОНИТОРИНГ ДВИГАТЕЛЯ", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Обороты", "850", "об/мин", Modifier.weight(1f))
                MetricCard("Температура", " อ90", "°C", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Напряжение", "14.2", "В", Modifier.weight(1f))
                MetricCard("Скорость", "0", "км/ч", Modifier.weight(1f))
            }
        }
        item {
            Text("СИСТЕМЫ АВТОМОБИЛЯ", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SystemRow("Двигатель / ECU", "OBD-II", Icons.Rounded.Memory)
                SystemRow("ABS / ESP", "Ожидает совместимого адаптера", Icons.Rounded.Security)
                SystemRow("Airbag / SRS", "Ожидает совместимого адаптера", Icons.Rounded.Air)
                SystemRow("Трансмиссия", "Ожидает совместимого адаптера", Icons.Rounded.Settings)
            }
        }
        item {
            Text("Важно: показания сейчас демонстрационные. Реальные данные появятся после подключения совместимого OBD2-адаптера.", color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = Muted, fontSize = 12.sp)
            Text(value, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(unit, color = Accent, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SystemRow(name: String, status: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(status, color = Muted, fontSize = 11.sp)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Muted)
        }
    }
}

@Composable
private fun ConnectionScreen(padding: PaddingValues, simulator: Boolean, onSimulatorChange: (Boolean) -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Подключение", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.BluetoothSearching, tint = Accent, contentDescription = null, modifier = Modifier.size(34.dp))
                Text("OBD2 адаптер", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("Подключение Bluetooth и Wi-Fi будет реализовано следующим этапом.", color = Muted)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Режим симулятора", modifier = Modifier.weight(1f))
                    Switch(checked = simulator, onCheckedChange = onSimulatorChange)
                }
            }
        }
        Text("Поддержка адаптеров будет зависеть от модели устройства и используемого протокола.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun DataScreen(padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Live Data", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("Демонстрационные параметры двигателя", color = Muted)
        MetricCard("Обороты двигателя", "850", "об/мин")
        MetricCard("Температура охлаждающей жидкости", "90", "°C")
        MetricCard("Напряжение бортовой сети", "14.2", "В")
        MetricCard("Положение дросселя", "3.5", "%")
    }
}

@Composable
private fun MoreScreen(padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Дополнительно", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        SystemRow("Коды неисправностей", "Чтение и расшифровка DTC", Icons.Rounded.Warning)
        SystemRow("Сервисные функции", "Для поддерживаемых автомобилей", Icons.Rounded.Build)
        SystemRow("Выбор автомобиля", "Марка, модель и год", Icons.Rounded.DirectionsCar)
        SystemRow("Настройки", "Язык и параметры приложения", Icons.Rounded.Settings)
        Text("AutoDiag 0.1.0 • Android", color = Muted, fontSize = 12.sp)
    }
}
