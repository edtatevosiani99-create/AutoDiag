package com.autodiag.app

import org.junit.Assert.assertEquals
import org.junit.Test

class DtcDecoderTest {
    @Test fun decodesStoredPowertrainCode() {
        assertEquals(listOf("P0300"), DtcDecoder.decodeMode03("43 03 00"))
    }

    @Test fun decodesPendingPowertrainCode() {
        assertEquals(listOf("P0171"), DtcDecoder.decodeMode07("47 01 71"))
    }

    @Test fun decodesPermanentPowertrainCode() {
        assertEquals(listOf("P0420"), DtcDecoder.decodeMode0A("4A 04 20"))
    }

    @Test fun ignoresZeroPadding() {
        assertEquals(emptyList<String>(), DtcDecoder.decodeMode03("43 00 00 00 00"))
    }
}
