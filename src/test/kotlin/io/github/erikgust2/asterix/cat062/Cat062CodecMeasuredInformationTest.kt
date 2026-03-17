package io.github.erikgust2.asterix.cat062

import org.junit.Test
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Cat062CodecMeasuredInformationTest {
    private val support = Cat062CodecSupport()

    @Test
    fun measuredInformationMatchesSpecLayout() {
        val buffer = ByteBuffer.allocate(32)
        buffer.put(0xFC.toByte())
        buffer.put(0x11)
        buffer.put(0x22)
        buffer.putShort(0x0C80.toShort())
        buffer.putShort(0x2000.toShort())
        buffer.putShort(0x0078.toShort())
        buffer.putShort(0x01E0.toShort())
        buffer.putShort(0xE640.toShort())
        buffer.put(0x68)
        buffer.flip()

        val decoded = support.readMeasuredInformation(buffer)
        assertEquals(DataSourceIdentifier(0x11, 0x22), decoded.sensorIdentification)
        assertEquals(12.5, assertNotNull(decoded.position).rangeNm, 1.0 / 256.0)
        assertEquals(45.0, assertNotNull(decoded.position).azimuthDegrees, 0.01)
        assertEquals(3000.0, assertNotNull(decoded.heightFeet), 0.001)
        assertEquals(120.0, assertNotNull(decoded.lastMeasuredModeCCode).flightLevel, 0.001)
        assertTrue(assertNotNull(decoded.lastMeasuredModeCCode).validated)
        val lastMode3a = assertNotNull(decoded.lastMeasuredMode3aCode)
        assertEquals(0x0640, lastMode3a.code)
        assertFalse(lastMode3a.validated)
        assertTrue(lastMode3a.garbled)
        assertTrue(lastMode3a.smoothed)
        assertEquals(3, assertNotNull(decoded.reportType).typ)
        assertTrue(assertNotNull(decoded.reportType).rab)
    }

    @Test
    fun measuredInformationRoundTripEachSubfieldIndependently() {
        val cases =
            listOf(
                MeasuredInformation(sensorIdentification = DataSourceIdentifier(1, 2)),
                MeasuredInformation(position = PolarPosition(rangeNm = 12.5, azimuthDegrees = 45.0)),
                MeasuredInformation(heightFeet = 3000.0),
                MeasuredInformation(lastMeasuredModeCCode = MeasuredModeCCode(validated = true, garbled = false, flightLevel = 120.0)),
                MeasuredInformation(
                    lastMeasuredMode3aCode = MeasuredMode3ACode(code = 0x640, validated = false, garbled = true, smoothed = true),
                ),
                MeasuredInformation(reportType = ReportType(typ = 3, simulated = true, rab = true, testTarget = false)),
            )

        cases.forEach { expected ->
            val buffer = ByteBuffer.allocate(32)
            support.writeMeasuredInformation(buffer, expected)
            buffer.flip()
            assertEquals(expected, support.readMeasuredInformation(buffer))
        }
    }

    @Test
    fun measuredInformationRoundTripFullPopulation() {
        val expected =
            MeasuredInformation(
                sensorIdentification = DataSourceIdentifier(1, 2),
                position = PolarPosition(rangeNm = 12.5, azimuthDegrees = 45.0),
                heightFeet = 3000.0,
                lastMeasuredModeCCode = MeasuredModeCCode(validated = true, garbled = false, flightLevel = 120.0),
                lastMeasuredMode3aCode = MeasuredMode3ACode(code = 0x640, validated = false, garbled = true, smoothed = true),
                reportType = ReportType(typ = 3, simulated = true, rab = true, testTarget = false),
            )

        val buffer = ByteBuffer.allocate(32)
        support.writeMeasuredInformation(buffer, expected)
        buffer.flip()

        assertEquals(expected, support.readMeasuredInformation(buffer))
    }

    @Test
    fun writeMeasuredInformationRejectsReportTypeAndMode3aOverflow() {
        assertRangeFailure("measuredInformation.detectedTargetType.typ out of range") {
            support.writeMeasuredInformation(
                ByteBuffer.allocate(16),
                MeasuredInformation(reportType = ReportType(typ = 8, simulated = false, rab = false, testTarget = false)),
            )
        }
        assertRangeFailure("measuredInformation.lastMeasuredMode3aCode.code out of range") {
            support.writeMeasuredInformation(
                ByteBuffer.allocate(16),
                MeasuredInformation(
                    lastMeasuredMode3aCode =
                        MeasuredMode3ACode(
                            code = 0x1000,
                            validated = true,
                            garbled = false,
                            smoothed = false,
                        ),
                ),
            )
        }
    }

    @Test
    fun readMeasuredInformationRejectsTruncatedPayloads() {
        val bytes =
            ByteBuffer
                .allocate(32)
                .also {
                    support.writeMeasuredInformation(
                        it,
                        MeasuredInformation(
                            sensorIdentification = DataSourceIdentifier(1, 2),
                            lastMeasuredMode3aCode =
                                MeasuredMode3ACode(
                                    code = 0x640,
                                    validated = false,
                                    garbled = true,
                                    smoothed = true,
                                ),
                        ),
                    )
                }.usedBytes()

        assertFailsWith<BufferUnderflowException> {
            support.readMeasuredInformation(ByteBuffer.wrap(truncated(bytes)))
        }
    }
}
