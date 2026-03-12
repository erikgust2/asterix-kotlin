package com.erik.asterix.cat062

import java.nio.ByteBuffer
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Cat062CodecTest {
    private val support = Cat062CodecSupport()

    @Test
    fun usesCat062V115FrnNumbersInFspec() {
        val bytes = writeRecord(
            Cat062Record(
                dataSourceIdentifier = DataSourceIdentifier(1, 2),
                serviceIdentification = 4,
                trackNumber = 42,
                timeOfTrackInformationSeconds = 0.0,
                trackStatus = TrackStatus(),
            ),
        )

        assertEquals(0xB1, bytes[0].toUnsignedInt())
        assertEquals(0x0C, bytes[1].toUnsignedInt())
    }

    @Test
    fun writeRecordRequiresMandatoryCat062Items() {
        val buffer = ByteBuffer.allocate(64)

        val error = assertFailsWith<IllegalArgumentException> {
            Cat062Codec.writeRecord(
                buffer,
                Cat062Record(serviceIdentification = 4),
            )
        }

        assertEquals("CAT062 record missing mandatory I062/010 dataSourceIdentifier", error.message)
    }

    @Test
    fun writesSpecBitLayoutsForFixedItems() {
        val mode3aBuffer = ByteBuffer.allocate(2)
        support.writeMode3ACode(mode3aBuffer, Mode3ACode(code = 0x750, codeChanged = true))
        assertContentEquals(byteArrayOf(0x27, 0x50), mode3aBuffer.toByteArray())
        mode3aBuffer.flip()
        assertEquals(Mode3ACode(code = 0x750, codeChanged = true), support.readMode3ACode(mode3aBuffer))

        val mode2Buffer = ByteBuffer.allocate(2)
        support.writeMode2Code(mode2Buffer, Mode2Code(code = 0x668))
        assertContentEquals(byteArrayOf(0x06, 0x68), mode2Buffer.toByteArray())
        mode2Buffer.flip()
        assertEquals(Mode2Code(code = 0x668), support.readMode2Code(mode2Buffer))

        val measuredFlBuffer = ByteBuffer.allocate(2)
        support.writeFlightLevelMeasurement(measuredFlBuffer, FlightLevelMeasurement(flightLevel = 310.0))
        assertContentEquals(byteArrayOf(0x04, (0xD8).toByte()), measuredFlBuffer.toByteArray())
        measuredFlBuffer.flip()
        assertEquals(310.0, support.readFlightLevelMeasurement(measuredFlBuffer).flightLevel, 0.001)
    }

    @Test
    fun modeOfMovementUsesSingleOctetAndAdfBit() {
        val buffer = ByteBuffer.allocate(1)
        support.writeModeOfMovement(
            buffer,
            ModeOfMovement(
                transversalAccelerationClass = TransversalAccelerationClass.RIGHT_TURN,
                longitudinalAccelerationClass = MovementAccelerationClass.INCREASING_GROUND_SPEED,
                verticalMovementClass = VerticalMovementClass.CLIMB,
                altitudeDiscrepancyFlag = true,
            ),
        )

        assertContentEquals(byteArrayOf(0x56), buffer.toByteArray())
        buffer.flip()

        val decoded = support.readModeOfMovement(buffer)
        assertEquals(TransversalAccelerationClass.RIGHT_TURN, decoded.transversalAccelerationClass)
        assertEquals(MovementAccelerationClass.INCREASING_GROUND_SPEED, decoded.longitudinalAccelerationClass)
        assertEquals(VerticalMovementClass.CLIMB, decoded.verticalMovementClass)
        assertTrue(decoded.altitudeDiscrepancyFlag)
    }

    @Test
    fun trackStatusMatchesV115ExtentLayout() {
        val expected = TrackStatus(
            mon = true,
            spi = false,
            mrh = true,
            src = 5,
            cnf = true,
            sim = true,
            tse = true,
            tsb = true,
            fpc = true,
            aff = true,
            stp = true,
            kos = true,
            ama = true,
            md4 = 2,
            me = true,
            mi = true,
            md5 = 1,
            cst = true,
            psr = false,
            ssr = true,
            mds = false,
            ads = true,
            suc = false,
            aac = true,
            sds = 2,
            ems = 5,
        )

        val buffer = ByteBuffer.allocate(8)
        support.writeTrackStatus(buffer, expected)

        assertContentEquals(
            byteArrayOf(0xB7.toByte(), 0xFF.toByte(), 0xDB.toByte(), 0xAB.toByte(), 0xA8.toByte()),
            buffer.toByteArray(),
        )

        buffer.flip()
        assertEquals(expected, support.readTrackStatus(buffer))
    }

    @Test
    fun systemTrackUpdateAgesUsesTrackAgeSubfield() {
        val buffer = ByteBuffer.allocate(8)
        support.writeSystemTrackUpdateAges(
            buffer,
            SystemTrackUpdateAges(
                mapOf(
                    SystemTrackAgeType.TRACK to 1.25,
                    SystemTrackAgeType.ADS_ES to 0.75,
                ),
            ),
        )

        assertContentEquals(byteArrayOf(0x84.toByte(), 0x05, 0x03), buffer.toByteArray())
        buffer.flip()

        val decoded = support.readSystemTrackUpdateAges(buffer)
        assertEquals(1.25, assertNotNull(decoded.agesSeconds[SystemTrackAgeType.TRACK]), 0.001)
        assertEquals(0.75, assertNotNull(decoded.agesSeconds[SystemTrackAgeType.ADS_ES]), 0.001)
    }

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
    fun flightPlanRelatedDataMatchesSpecLayout() {
        val buffer = ByteBuffer.allocate(32)
        buffer.put(0x21)
        buffer.put(0x01)
        buffer.put(0x30)
        buffer.putInt((2 shl 30) or 0x00123456)
        buffer.putShort(0x1750.toShort())
        buffer.put("EMERG01".toByteArray())
        buffer.flip()

        val decoded = support.readFlightPlanRelatedData(buffer)
        assertEquals(2, assertNotNull(decoded.ifpsFlightId).typeCode)
        assertEquals(0x00123456, assertNotNull(decoded.ifpsFlightId).number)
        assertEquals(0x750, assertNotNull(decoded.preEmergencyMode3a).code)
        assertTrue(assertNotNull(decoded.preEmergencyMode3a).valid)
        assertEquals("EMERG01", decoded.preEmergencyCallsign)
    }

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
    fun estimatedAccuraciesMatchesSpecLayout() {
        val buffer = ByteBuffer.allocate(32)
        buffer.put(0xC7.toByte())
        buffer.put(0x80.toByte())
        buffer.putShort(0x0014)
        buffer.putShort(0x0028)
        buffer.putShort(0xFFFD.toShort())
        buffer.put(0x04)
        buffer.put(0x08)
        buffer.put(0x02)
        buffer.put(0x04)
        buffer.put(0x08)
        buffer.flip()

        val decoded = support.readEstimatedAccuracies(buffer)
        assertEquals(10.0, assertNotNull(decoded.positionCartesian).xMeters, 0.001)
        assertEquals(20.0, assertNotNull(decoded.positionCartesian).yMeters, 0.001)
        assertEquals(-1.5, assertNotNull(decoded.xyCovarianceMeters), 0.001)
        assertEquals(1.0, assertNotNull(decoded.trackVelocity).xMetersPerSecond, 0.001)
        assertEquals(2.0, assertNotNull(decoded.trackVelocity).yMetersPerSecond, 0.001)
        assertEquals(0.5, assertNotNull(decoded.trackAcceleration).xMetersPerSecondSquared, 0.001)
        assertEquals(1.0, assertNotNull(decoded.trackAcceleration).yMetersPerSecondSquared, 0.001)
        assertEquals(50.0, assertNotNull(decoded.rateOfClimbDescentFeetPerMinute), 0.001)
    }

    @Test
    fun aircraftDerivedDataUsesSplitAirspeedFieldsAndAdsbStatus() {
        val buffer = ByteBuffer.allocate(32)
        buffer.put(0x01)
        buffer.put(0x11)
        buffer.put(0x01)
        buffer.put(0x0E)
        buffer.putShort(0x9A05.toShort())
        buffer.putShort(250.toShort())
        buffer.putShort(100.toShort())
        buffer.putShort(2132.toShort())
        buffer.flip()

        val decoded = support.readAircraftDerivedData(buffer)
        assertEquals(2, assertNotNull(decoded.adsbStatus).ac)
        assertEquals(1, assertNotNull(decoded.adsbStatus).mn)
        assertEquals(2, assertNotNull(decoded.adsbStatus).dc)
        assertTrue(assertNotNull(decoded.adsbStatus).gbs)
        assertEquals(5, assertNotNull(decoded.adsbStatus).stat)
        assertEquals(250, decoded.indicatedAirspeedKnots)
        assertEquals(0.8, assertNotNull(decoded.machNumber), 0.001)
        assertEquals(213.2, assertNotNull(decoded.barometricPressureSettingHpa), 0.001)
    }

    @Test
    fun byteArrayBackedModelTypesUseContentEquality() {
        assertEquals(TrajectoryIntentPoint(byteArrayOf(1, 2, 3)), TrajectoryIntentPoint(byteArrayOf(1, 2, 3)))
        assertEquals(
            ModeSMessage(byteArrayOf(1, 2, 3, 4, 5, 6, 7), 4, 0),
            ModeSMessage(byteArrayOf(1, 2, 3, 4, 5, 6, 7), 4, 0),
        )
        assertEquals(TimeOfDepartureArrival(byteArrayOf(1, 2, 3, 4)), TimeOfDepartureArrival(byteArrayOf(1, 2, 3, 4)))
        assertEquals(
            Cat062Record(reservedExpansionField = byteArrayOf(9, 8, 7).toRawBytes(), specialPurposeField = byteArrayOf(6, 5).toRawBytes()),
            Cat062Record(reservedExpansionField = byteArrayOf(9, 8, 7).toRawBytes(), specialPurposeField = byteArrayOf(6, 5).toRawBytes()),
        )
    }

    private fun writeRecord(record: Cat062Record): ByteArray {
        val buffer = ByteBuffer.allocate(256)
        Cat062Codec.writeRecord(buffer, record)
        return buffer.toByteArray()
    }

    private fun ByteBuffer.toByteArray(): ByteArray = array().copyOf(position())
}
