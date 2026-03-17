package io.github.erikgust2.asterix.cat062

import org.junit.Test
import java.nio.ByteBuffer
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class Cat062CodecDataBlockTest {
    @Test
    fun writeDataBlockThenReadDataBlockRoundTripsSingleRecord() {
        val record =
            minimalValidRecord(timeOfTrackInformationSeconds = 256.0).copy(
                serviceIdentification = 4,
                calculatedTrackPositionWgs84 = Wgs84Position(32 * WGS84_RESOLUTION, -64 * WGS84_RESOLUTION),
                calculatedTrackPositionCartesian = CartesianPosition(-128.0, 256.5),
                trackMode3aCode = Mode3ACode(code = 0x750, codeChanged = true),
                targetIdentification =
                    TargetIdentification(
                        source = TargetIdentificationSource.CALLSIGN_NOT_FROM_TRANSPONDER,
                        value = "SK1234",
                    ),
                measuredFlightLevel = FlightLevelMeasurement(310.0),
                calculatedTrackBarometricAltitude = BarometricAltitude(qnhCorrectionApplied = true, altitudeFeet = 500.0),
                reservedExpansionField = byteArrayOf(0x55, 0x33).toRawBytes(),
                specialPurposeField = byteArrayOf(0x11).toRawBytes(),
            )

        val decoded = Cat062Codec.readDataBlock(ByteBuffer.wrap(writeDataBlockBytes(record)))

        assertEquals(Cat062DataBlock(listOf(record)), decoded)
    }

    @Test
    fun writeDataBlockThenReadDataBlockRoundTripsMultipleRecords() {
        val first =
            minimalValidRecord(trackNumber = 100).copy(
                serviceIdentification = 7,
                calculatedTrackPositionCartesian = CartesianPosition(32.0, -48.5),
            )
        val second =
            minimalValidRecord(trackNumber = 101, timeOfTrackInformationSeconds = 512.0).copy(
                targetIdentification =
                    TargetIdentification(
                        source = TargetIdentificationSource.REGISTRATION_NOT_FROM_TRANSPONDER,
                        value = "SEABC1",
                    ),
                measuredFlightLevel = FlightLevelMeasurement(120.0),
                vehicleFleetIdentification = VehicleFleetIdentification.EMERGENCY,
            )

        val decoded = Cat062Codec.readDataBlock(ByteBuffer.wrap(writeDataBlockBytes(first, second)))

        assertEquals(Cat062DataBlock(listOf(first, second)), decoded)
    }

    @Test
    fun readDataBlockRejectsWrongCategory() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                Cat062Codec.readDataBlock(bufferOf(61, 0x00, 0x03))
            }

        assertTrue(error.message?.contains("Expected ASTERIX category 62 but found 61") == true)
    }

    @Test
    fun readDataBlockRejectsBlockLengthsSmallerThanThree() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                Cat062Codec.readDataBlock(bufferOf(62, 0x00, 0x02))
            }

        assertTrue(error.message?.contains("Invalid ASTERIX block length 2") == true)
    }

    @Test
    fun readDataBlockRejectsDeclaredLengthBeyondRemainingBytes() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                Cat062Codec.readDataBlock(bufferOf(62, 0x00, 0x04))
            }

        assertTrue(error.message?.contains("exceeds remaining buffer") == true)
    }

    @Test
    fun readDataBlockRejectsBlocksThatDoNotEndOnRecordBoundary() {
        val bytes = writeDataBlockBytes(minimalValidRecord().copy(serviceIdentification = 4))
        val truncatedLength = bytes.size - 1
        bytes[1] = ((truncatedLength ushr 8) and 0xFF).toByte()
        bytes[2] = (truncatedLength and 0xFF).toByte()

        val error =
            assertFailsWith<IllegalArgumentException> {
                Cat062Codec.readDataBlock(ByteBuffer.wrap(bytes))
            }

        assertTrue(error.message?.contains("CAT062 block parsing ended at") == true)
    }
}
