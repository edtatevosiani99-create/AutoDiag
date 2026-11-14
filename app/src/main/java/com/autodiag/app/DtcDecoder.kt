package com.autodiag.app

object DtcDecoder {
    private val descriptions = mapOf(
        "P0100" to "Неисправность цепи датчика массового расхода воздуха (MAF)",
        "P0101" to "Диапазон/характеристики датчика массового расхода воздуха",
        "P0115" to "Неисправность цепи датчика температуры охлаждающей жидкости",
        "P0171" to "Слишком бедная топливная смесь (Bank 1)",
        "P0172" to "Слишком богатая топливная смесь (Bank 1)",
        "P0300" to "Обнаружены случайные/множественные пропуски зажигания",
        "P0301" to "Пропуски зажигания в цилиндре 1",
        "P0302" to "Пропуски зажигания в цилиндре 2",
        "P0303" to "Пропуски зажигания в цилиндре 3",
        "P0304" to "Пропуски зажигания в цилиндре 4",
        "P0420" to "Эффективность каталитического нейтрализатора ниже порога (Bank 1)",
        "P0442" to "Обнаружена небольшая утечка в системе EVAP",
        "P0500" to "Неисправность датчика скорости автомобиля"
    )
    fun description(code: String): String = descriptions[code]
        ?: "Описание отсутствует в локальной базе. Требуется проверка по документации автомобиля."

    fun decodeMode03(raw: String): List<String> = decodeService(raw, "43")
    fun decodeMode07(raw: String): List<String> = decodeService(raw, "47")
    fun decodeMode0A(raw: String): List<String> = decodeService(raw, "4A")

    private fun decodeService(raw: String, service: String): List<String> {
        val hex = raw.uppercase().replace(Regex("[^0-9A-F]"), "")
        val start = hex.indexOf(service)
        if (start < 0) return emptyList()
        val payload = hex.substring(start + 2)
        val codes = mutableListOf<String>()
        var i = 0
        while (i + 4 <= payload.length) {
            val a = payload.substring(i, i + 2).toIntOrNull(16) ?: break
            val b = payload.substring(i + 2, i + 4).toIntOrNull(16) ?: break
            i += 4
            if (a == 0 && b == 0) continue
            val family = when ((a and 0xC0) shr 6) { 0 -> 'P'; 1 -> 'C'; 2 -> 'B'; else -> 'U' }
            val d1 = (a shr 4) and 0x03
            val d2 = a and 0x0F
            val d3 = (b shr 4) and 0x0F
            val d4 = b and 0x0F
            codes += "$family$d1${d2.toString(16).uppercase()}${d3.toString(16).uppercase()}${d4.toString(16).uppercase()}"
        }
        return codes.distinct()
    }
}
