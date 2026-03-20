package io.github.erikgust2.asterix.cat062

import org.junit.Test
import java.nio.ByteBuffer
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class Cat062CodecGoldenVectorTest {
    @Test
    fun recordGoldenVectorsEncodeAndDecodeExactBytes() {
        listOf(
            minimalRecordGoldenVector,
            denseRecordGoldenVector,
        ).forEach { fixture ->
            val encoded = ByteBuffer.allocate(512).also { Cat062Codec.writeRecord(it, fixture.record) }.usedBytes()

            assertContentEquals(fixture.bytes, encoded, fixture.name)
            assertEquals(fixture.record, Cat062Codec.readRecord(ByteBuffer.wrap(fixture.bytes)), fixture.name)
        }
    }

    @Test
    fun dataBlockGoldenVectorsEncodeAndDecodeExactBytes() {
        listOf(
            minimalBlockGoldenVector,
            multiRecordBlockGoldenVector,
        ).forEach { fixture ->
            assertDeclaredAsterixLengthMatchesByteArraySize(fixture.bytes, fixture.name)

            val encoded =
                ByteBuffer
                    .allocate(1024)
                    .also { Cat062Codec.writeDataBlock(it, fixture.block) }
                    .usedBytes()

            assertContentEquals(fixture.bytes, encoded, fixture.name)
            assertEquals(fixture.block, Cat062Codec.readDataBlock(ByteBuffer.wrap(fixture.bytes)), fixture.name)
        }
    }
}

private data class RecordGoldenVector(
    val name: String,
    val record: Cat062Record,
    val bytes: ByteArray,
)

private data class DataBlockGoldenVector(
    val name: String,
    val block: Cat062DataBlock,
    val bytes: ByteArray,
)

private val minimalGoldenRecord =
    minimalValidRecord(
        dataSourceIdentifier = DataSourceIdentifier(1, 2),
        timeOfTrackInformationSeconds = 128.0,
        trackNumber = 42,
        trackStatus = TrackStatus(mon = true, spi = false, mrh = false, src = TrackSource.TRIANGULATION, cnf = true),
    )

private val denseGoldenRecord =
    Cat062Record(
        dataSourceIdentifier = DataSourceIdentifier(1, 2),
        serviceIdentification = 4,
        timeOfTrackInformationSeconds = 256.0,
        calculatedTrackPositionWgs84 = Wgs84Position(32 * WGS84_RESOLUTION, -64 * WGS84_RESOLUTION),
        trackMode3aCode = Mode3ACode(code = 0x750, codeChanged = true),
        targetIdentification =
            TargetIdentification(
                source = TargetIdentificationSource.CALLSIGN_OR_REGISTRATION_FROM_TRANSPONDER,
                value = "SAS123",
            ),
        trackNumber = 42,
        trackStatus = TrackStatus(mon = true, spi = true, mrh = false, src = TrackSource.THREE_D_RADAR, cnf = true),
        flightPlanRelatedData =
            FlightPlanRelatedData(
                ifpsFlightId = IfpsFlightId(typeCode = IfpsFlightIdType.UNIT_2_INTERNAL_FLIGHT_NUMBER, number = 0x00123456),
                preEmergencyMode3a = PreEmergencyMode3a(valid = true, code = 0x750),
                preEmergencyCallsign = "EMERG01",
            ),
        targetSizeAndOrientation = TargetSizeAndOrientation(lengthMeters = 12, orientationDegrees = 90.0, widthMeters = 7),
        vehicleFleetIdentification = VehicleFleetIdentification.FIRE,
        composedTrackNumber = ComposedTrackNumber(systemUnitIdentification = 17, trackNumber = 0x4567),
        reservedExpansionField = byteArrayOfInts(0x55, 0x33).toRawBytes(),
        specialPurposeField = byteArrayOfInts(0x11).toRawBytes(),
    )

private val minimalRecordGoldenVector =
    RecordGoldenVector(
        name = "minimalRecord",
        record = minimalGoldenRecord,
        bytes =
            byteArrayOfInts(
                0x91,
                0x0C,
                0x01,
                0x02,
                0x00,
                0x40,
                0x00,
                0x00,
                0x2A,
                0x8E,
            ),
    )

private val denseRecordGoldenVector =
    RecordGoldenVector(
        name = "denseRecord",
        record = denseGoldenRecord,
        bytes =
            concatBytes(
                byteArrayOfInts(
                    0xB9,
                    0x6D,
                    0x03,
                    0xC9,
                    0x06,
                    0x01,
                    0x02,
                    0x04,
                    0x00,
                    0x80,
                    0x00,
                    0x00,
                    0x00,
                    0x00,
                    0x20,
                    0xFF,
                    0xFF,
                    0xFF,
                    0xC0,
                    0x27,
                    0x50,
                    0x13,
                    0x14,
                    0xF1,
                    0xCB,
                    0x30,
                    0x00,
                    0x00,
                    0x2A,
                    0xCA,
                ),
                byteArrayOfInts(
                    0x21,
                    0x01,
                    0x30,
                    0x80,
                    0x12,
                    0x34,
                    0x56,
                    0x17,
                    0x50,
                    0x45,
                    0x4D,
                    0x45,
                    0x52,
                    0x47,
                    0x30,
                    0x31,
                ),
                byteArrayOfInts(
                    0x19,
                    0x41,
                    0x0E,
                    0x03,
                    0x11,
                    0x45,
                    0x67,
                    0x03,
                    0x55,
                    0x33,
                    0x02,
                    0x11,
                ),
            ),
    )

private val minimalBlockGoldenVector =
    DataBlockGoldenVector(
        name = "minimalBlock",
        block = Cat062DataBlock(listOf(minimalGoldenRecord)),
        bytes =
            concatBytes(
                byteArrayOfInts(0x3E, 0x00, 0x0D),
                minimalRecordGoldenVector.bytes,
            ),
    )

private val multiRecordBlockGoldenVector =
    DataBlockGoldenVector(
        name = "multiRecordBlock",
        block = Cat062DataBlock(listOf(minimalGoldenRecord, denseGoldenRecord)),
        bytes =
            concatBytes(
                byteArrayOfInts(0x3E, 0x00, 0x47),
                minimalRecordGoldenVector.bytes,
                denseRecordGoldenVector.bytes,
            ),
    )

private fun concatBytes(vararg parts: ByteArray): ByteArray {
    val size = parts.sumOf { it.size }
    val result = ByteArray(size)
    var offset = 0
    parts.forEach { part ->
        part.copyInto(result, offset)
        offset += part.size
    }
    return result
}

private fun assertDeclaredAsterixLengthMatchesByteArraySize(
    bytes: ByteArray,
    name: String,
) {
    val declaredLength = (bytes[1].toUnsignedInt() shl 8) or bytes[2].toUnsignedInt()
    assertEquals(bytes.size, declaredLength, "$name declared length")
}
