package io.github.erikgust2.asterix.cat062

import org.junit.Test
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Cat062CodecMode5Test {
    private val support = Cat062CodecSupport()

    @Test
    fun mode5DataReportsMatchesSpecLayout() {
        val buffer = ByteBuffer.allocate(32)
        buffer.put(0xCA.toByte())
        buffer.put(0xD1.toByte())
        buffer.putInt((0x1234 shl 17) or (14 shl 9) or 7)
        buffer.putShort(0x0015)
        buffer.put(0x15)
        buffer.flip()

        val decoded = support.readMode5DataReports(buffer)
        assertTrue(assertNotNull(decoded.summary).m5)
        assertTrue(assertNotNull(decoded.summary).id)
        assertTrue(assertNotNull(decoded.summary).m1)
        assertTrue(assertNotNull(decoded.summary).x)
        assertEquals(0x1234, assertNotNull(decoded.pinNationalOriginMission).pin)
        assertEquals(14, assertNotNull(decoded.pinNationalOriginMission).nationalOrigin)
        assertEquals(7, assertNotNull(decoded.pinNationalOriginMission).missionCode)
        assertEquals(0x15, assertNotNull(decoded.extendedMode1Code).code)
        val xPulse = assertNotNull(decoded.xPulsePresence)
        assertTrue(xPulse.x5)
        assertFalse(xPulse.xc)
        assertTrue(xPulse.x3)
        assertFalse(xPulse.x2)
        assertTrue(xPulse.x1)
    }

    @Test
    fun mode5DataReportsRoundTripEachSubfieldIndependently() {
        val cases =
            listOf(
                Mode5DataReports(
                    summary = Mode5Summary(m5 = true, id = false, da = true, m1 = false, m2 = true, m3 = false, mc = true, x = false),
                ),
                Mode5DataReports(pinNationalOriginMission = PinNationalOriginMission(pin = 0x1234, nationalOrigin = 14, missionCode = 7)),
                Mode5DataReports(positionWgs84 = Wgs84Position(32 * WGS84_RESOLUTION, -64 * WGS84_RESOLUTION)),
                Mode5DataReports(geometricAltitudeFeet = 1250.0),
                Mode5DataReports(extendedMode1Code = ExtendedMode1Code(code = 0x123)),
                Mode5DataReports(timeOffsetSeconds = 0.5),
                Mode5DataReports(xPulsePresence = Mode5XPulsePresence(x5 = true, xc = false, x3 = true, x2 = false, x1 = true)),
            )

        cases.forEach { expected ->
            val buffer = ByteBuffer.allocate(64)
            support.writeMode5DataReports(buffer, expected)
            buffer.flip()
            assertEquals(expected, support.readMode5DataReports(buffer))
        }
    }

    @Test
    fun mode5DataReportsRoundTripFullPopulation() {
        val expected =
            Mode5DataReports(
                summary = Mode5Summary(m5 = true, id = true, da = false, m1 = true, m2 = false, m3 = true, mc = false, x = true),
                pinNationalOriginMission = PinNationalOriginMission(pin = 0x1234, nationalOrigin = 14, missionCode = 7),
                positionWgs84 = Wgs84Position(32 * WGS84_RESOLUTION, -64 * WGS84_RESOLUTION),
                geometricAltitudeFeet = 1250.0,
                extendedMode1Code = ExtendedMode1Code(code = 0x123),
                timeOffsetSeconds = -0.5,
                xPulsePresence = Mode5XPulsePresence(x5 = true, xc = true, x3 = false, x2 = true, x1 = false),
            )

        val buffer = ByteBuffer.allocate(64)
        support.writeMode5DataReports(buffer, expected)
        buffer.flip()

        assertEquals(expected, support.readMode5DataReports(buffer))
    }

    @Test
    fun writeMode5DataReportsRejectsFieldRanges() {
        assertRangeFailure("mode5DataReports.pinNationalOriginMission.pin out of range") {
            support.writeMode5DataReports(
                ByteBuffer.allocate(32),
                Mode5DataReports(pinNationalOriginMission = PinNationalOriginMission(pin = 0x4000, nationalOrigin = 0, missionCode = 0)),
            )
        }
        assertRangeFailure("mode5DataReports.pinNationalOriginMission.nationalOrigin out of range") {
            support.writeMode5DataReports(
                ByteBuffer.allocate(32),
                Mode5DataReports(pinNationalOriginMission = PinNationalOriginMission(pin = 0, nationalOrigin = 32, missionCode = 0)),
            )
        }
        assertRangeFailure("mode5DataReports.pinNationalOriginMission.missionCode out of range") {
            support.writeMode5DataReports(
                ByteBuffer.allocate(32),
                Mode5DataReports(pinNationalOriginMission = PinNationalOriginMission(pin = 0, nationalOrigin = 0, missionCode = 64)),
            )
        }
        assertRangeFailure("mode5DataReports.extendedMode1Code.code out of range") {
            support.writeMode5DataReports(
                ByteBuffer.allocate(32),
                Mode5DataReports(extendedMode1Code = ExtendedMode1Code(code = 0x1000)),
            )
        }
        assertRangeFailure("mode5DataReports.timeOffsetSeconds out of range") {
            support.writeMode5DataReports(
                ByteBuffer.allocate(32),
                Mode5DataReports(timeOffsetSeconds = 1.0),
            )
        }
    }

    @Test
    fun readMode5DataReportsRejectsTruncatedPayloads() {
        val bytes =
            ByteBuffer
                .allocate(64)
                .also {
                    support.writeMode5DataReports(
                        it,
                        Mode5DataReports(
                            pinNationalOriginMission = PinNationalOriginMission(pin = 0x1234, nationalOrigin = 14, missionCode = 7),
                            positionWgs84 = Wgs84Position(32 * WGS84_RESOLUTION, -64 * WGS84_RESOLUTION),
                        ),
                    )
                }.usedBytes()

        assertFailsWith<BufferUnderflowException> {
            support.readMode5DataReports(ByteBuffer.wrap(truncated(bytes)))
        }
    }
}
