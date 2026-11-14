package com.autodiag.app

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

data class VehicleData(
    val rpm: Int? = null,
    val coolantC: Int? = null,
    val speedKmh: Int? = null,
    val throttlePercent: Double? = null,
    val voltage: Double? = null
)

class Obd2Client {
    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    val isConnected: Boolean get() = socket?.isConnected == true

    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice): String = withContext(Dispatchers.IO) {
        close()
        val s = device.createRfcommSocketToServiceRecord(SPP_UUID)
        s.connect()
        socket = s
        input = s.inputStream
        output = s.outputStream
        val adapter = send("ATZ", 5000)
        send("ATE0"); send("ATL0"); send("ATS0"); send("ATH0"); send("ATSP0")
        val protocol = send("ATDP")
        "ELM327 ready. " + adapter.trim().ifBlank { "Adapter initialized" } + " " + protocol.trim()
    }

    suspend fun command(command: String): String = withContext(Dispatchers.IO) { send(command) }

    suspend fun readVehicleData(): VehicleData = withContext(Dispatchers.IO) {
        val rpm = parsePid(send("010C"), "410C")?.let { ((it[0] * 256) + it[1]) / 4 }
        val coolant = parsePid(send("0105"), "4105")?.firstOrNull()?.minus(40)
        val speed = parsePid(send("010D"), "410D")?.firstOrNull()
        val throttle = parsePid(send("0111"), "4111")?.firstOrNull()?.let { it * 100.0 / 255.0 }
        val voltage = parsePid(send("0142"), "4142")?.takeIf { it.size >= 2 }?.let { ((it[0] * 256) + it[1]) / 1000.0 }
        VehicleData(rpm, coolant, speed, throttle, voltage)
    }

    suspend fun readTroubleCodes(): List<String> = withContext(Dispatchers.IO) {
        DtcDecoder.decodeMode03(send("03"))
    }
    suspend fun clearTroubleCodes(): String = command("04")

    private fun parsePid(response: String, header: String): List<Int>? {
        val clean = response.replace(" ", "").replace("\r", "").replace("\n", "").uppercase()
        val index = clean.indexOf(header)
        if (index < 0) return null
        val hex = clean.substring(index + header.length).takeWhile { it in "0123456789ABCDEF" }
        if (hex.length < 2 || hex.length % 2 != 0) return null
        return hex.chunked(2).mapNotNull { it.toIntOrNull(16) }
    }

    private fun send(command: String, timeoutMs: Long = 3500): String {
        val out = output ?: error("OBD2 adapter is not connected")
        val ins = input ?: error("OBD2 adapter is not connected")
        while (ins.available() > 0) ins.read()
        out.write((command.trim() + "\r").toByteArray(Charsets.US_ASCII))
        out.flush()
        val result = StringBuilder()
        val start = System.currentTimeMillis()
        val buffer = ByteArray(256)
        while (System.currentTimeMillis() - start < timeoutMs) {
            val available = ins.available()
            if (available > 0) {
                val count = ins.read(buffer, 0, minOf(buffer.size, available))
                if (count > 0) {
                    result.append(String(buffer, 0, count, Charsets.US_ASCII))
                    if (result.contains(">")) break
                }
            } else Thread.sleep(30)
        }
        result.toString().replace(">", "").trim()
    }

    fun close() {
        try { input?.close() } catch (_: Exception) {}
        try { output?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        input = null; output = null; socket = null
    }
}
