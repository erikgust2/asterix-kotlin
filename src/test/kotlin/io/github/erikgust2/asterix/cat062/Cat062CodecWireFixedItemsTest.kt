package io.github.erikgust2.asterix.cat062

import org.junit.Test
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class Cat062CodecWireFixedItemsTest {
    private val support = Cat062CodecSupport()

    @Test
    fun writesSpecBitLayoutsForFixedItems() {
        val mode3aBuffer = ByteBuffer.allocate(2)
        support.writeMode3ACode(mode3aBuffer, Mode3ACode(code = 0x750, codeChanged = true))
        assertContentEquals(byteArrayOf(0x27, 0x50), mode3aBuffer.usedBytes())
        mode3aBuffer.flip()
        assertEquals(Mode3ACode(code = 0x750, codeChanged = true), support.readMode3ACode(mode3aBuffer))

        val mode2Buffer = ByteBuffer.allocate(2)
        support.writeMode2Code(mode2Buffer, Mode2Code(code = 0x668))
        assertContentEquals(byteArrayOf(0x06, 0x68), mode2Buffer.usedBytes())
        mode2Buffer.flip()
        assertEquals(Mode2Code(code = 0x668), support.readMode2Code(mode2Buffer))

        val measuredFlBuffer = ByteBuffer.allocate(2)
        support.writeFlightLevelMeasurement(measuredFlBuffer, FlightLevelMeasurement(flightLevel = 310.0))
        assertContentEquals(byteArrayOf(0x04, 0xD8.toByte()), measuredFlBuffer.usedBytes())
        measuredFlBuffer.flip()
        assertEquals(310.0, support.readFlightLevelMeasurement(measuredFlBuffer).flightLevel, 0.001)
    }

    @Test
    fun wgs84PositionRoundTripsExactQuantizedCoordinates() {
        val buffer = ByteBuffer.allocate(8)
        val expected = Wgs84Position(latitudeDegrees = 1024 * WGS84_RESOLUTION, longitudeDegrees = -2048 * WGS84_RESOLUTION)

        support.writeWgs84Position(buffer, expected)
        buffer.flip()

        assertEquals(expected, support.readWgs84Position(buffer))
    }

    @Test
    fun cartesianVelocityAndAccelerationRoundTrip() {
        val velocityBuffer = ByteBuffer.allocate(4)
        val expectedVelocity = CartesianVelocity(xMetersPerSecond = -25.0, yMetersPerSecond = 12.25)
        support.writeCartesianVelocity(velocityBuffer, expectedVelocity)
        velocityBuffer.flip()
        assertEquals(expectedVelocity, support.readCartesianVelocity(velocityBuffer))

        val accelerationBuffer = ByteBuffer.allocate(2)
        val expectedAcceleration = CartesianAcceleration(xMetersPerSecondSquared = -4.0, yMetersPerSecondSquared = 7.75)
        support.writeCartesianAcceleration(accelerationBuffer, expectedAcceleration)
        accelerationBuffer.flip()
        assertEquals(expectedAcceleration, support.readCartesianAcceleration(accelerationBuffer))
    }

    @Test
    fun targetIdentificationRoundTripsEverySource() {
        TargetIdentificationSource.entries.forEach { source ->
            val expected = TargetIdentification(source = source, value = "AB12")
            val buffer = ByteBuffer.allocate(6)

            support.writeTargetIdentification(buffer, expected)
            buffer.flip()

            assertEquals(expected, support.readTargetIdentification(buffer))
        }
    }

    @Test
    fun writeTargetIdentificationRejectsOverlengthCallsign() {
        assertRangeFailure("Callsign too long for 8 characters") {
            support.writeTargetIdentification(
                ByteBuffer.allocate(6),
                TargetIdentification(
                    source = TargetIdentificationSource.CALLSIGN_OR_REGISTRATION_FROM_TRANSPONDER,
                    value = "ABCDEFGHJ",
                ),
            )
        }
    }

    @Test
    fun writeTargetIdentificationRejectsNormalizationExpansion() {
        assertRangeFailure("Callsign too long for 8 characters after normalization") {
            support.writeTargetIdentification(
                ByteBuffer.allocate(6),
                TargetIdentification(
                    source = TargetIdentificationSource.CALLSIGN_OR_REGISTRATION_FROM_TRANSPONDER,
                    value = "ABCDEFG\u00DF",
                ),
            )
        }
    }

    @Test
    fun writeTargetIdentificationRejectsUnsupportedCharacters() {
        assertRangeFailure("Callsign contains unsupported characters") {
            support.writeTargetIdentification(
                ByteBuffer.allocate(6),
                TargetIdentification(
                    source = TargetIdentificationSource.CALLSIGN_OR_REGISTRATION_FROM_TRANSPONDER,
                    value = "AB*12",
                ),
            )
        }
    }

    @Test
    fun targetSizeAndOrientationRoundTripsWidthExtension() {
        val buffer = ByteBuffer.allocate(3)
        val expected = TargetSizeAndOrientation(lengthMeters = 12, orientationDegrees = 90.0, widthMeters = 7)

        support.writeTargetSizeAndOrientation(buffer, expected)
        buffer.flip()

        assertEquals(expected, support.readTargetSizeAndOrientation(buffer))
    }

    @Test
    fun targetSizeAndOrientationRejectsWrappedOrientationAndWidthOverflow() {
        assertRangeFailure("targetSizeAndOrientation.orientationDegrees out of range") {
            support.writeTargetSizeAndOrientation(
                ByteBuffer.allocate(3),
                TargetSizeAndOrientation(lengthMeters = 12, orientationDegrees = 360.0, widthMeters = null),
            )
        }

        assertRangeFailure("targetSizeAndOrientation.widthMeters out of range") {
            support.writeTargetSizeAndOrientation(
                ByteBuffer.allocate(3),
                TargetSizeAndOrientation(lengthMeters = 12, orientationDegrees = 90.0, widthMeters = 128),
            )
        }
    }

    @Test
    fun vehicleFleetIdentificationRoundTripsKnownAndUnknownCodes() {
        val known = support.readVehicleFleetIdentification(bufferOf(0x03))
        val unknown = support.readVehicleFleetIdentification(bufferOf(0xFE))

        assertEquals(VehicleFleetIdentification.FIRE, known)
        assertEquals(VehicleFleetIdentification.Unknown(0xFE), unknown)
    }

    @Test
    fun mode2CodeAndComposedTrackNumberRoundTrip() {
        val mode2Buffer = ByteBuffer.allocate(2)
        support.writeMode2Code(mode2Buffer, Mode2Code(0x123))
        mode2Buffer.flip()
        assertEquals(Mode2Code(0x123), support.readMode2Code(mode2Buffer))

        val trackBuffer = ByteBuffer.allocate(3)
        val expectedTrack = ComposedTrackNumber(systemUnitIdentification = 17, trackNumber = 0x4567)
        support.writeComposedTrackNumber(trackBuffer, expectedTrack)
        trackBuffer.flip()
        assertEquals(expectedTrack, support.readComposedTrackNumber(trackBuffer))
    }

    @Test
    fun lengthPrefixedFieldsRoundTripEmptyAndMaximumPayloads() {
        listOf(byteArrayOf(), ByteArray(254) { index -> index.toByte() }).forEach { payload ->
            val buffer = ByteBuffer.allocate(256)

            support.writeLengthPrefixedField(buffer, payload.toRawBytes())
            buffer.flip()

            assertEquals(payload.toRawBytes(), support.readLengthPrefixedField(buffer))
        }
    }

    @Test
    fun readLengthPrefixedFieldRejectsZeroLengthAndTruncatedPayloads() {
        val valid = ByteBuffer.allocate(8).also { support.writeLengthPrefixedField(it, byteArrayOf(0x01, 0x02).toRawBytes()) }.usedBytes()

        val zeroLengthError =
            assertFailsWith<IllegalArgumentException> {
                support.readLengthPrefixedField(bufferOf(0x00))
            }
        assertTrue(zeroLengthError.message?.contains("Invalid length-prefixed field length 0") == true)

        assertFailsWith<BufferUnderflowException> {
            support.readLengthPrefixedField(ByteBuffer.wrap(truncated(valid)))
        }
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

    @Test
    fun fixedWidthIntegerEncodersAcceptBoundaryValues() {
        val unsignedByteBuffer = ByteBuffer.allocate(2)
        unsignedByteBuffer.putUnsignedByte(0x00, "test.unsignedByte")
        unsignedByteBuffer.putUnsignedByte(0xFF, "test.unsignedByte")
        assertContentEquals(byteArrayOf(0x00, 0xFF.toByte()), unsignedByteBuffer.usedBytes())

        val signedByteBuffer = ByteBuffer.allocate(2)
        signedByteBuffer.putSignedByte(Byte.MIN_VALUE.toInt(), "test.signedByte")
        signedByteBuffer.putSignedByte(Byte.MAX_VALUE.toInt(), "test.signedByte")
        assertContentEquals(byteArrayOf(0x80.toByte(), 0x7F), signedByteBuffer.usedBytes())

        val unsignedShortBuffer = ByteBuffer.allocate(4)
        unsignedShortBuffer.putUnsignedShort(0x0000, "test.unsignedShort")
        unsignedShortBuffer.putUnsignedShort(0xFFFF, "test.unsignedShort")
        assertContentEquals(byteArrayOf(0x00, 0x00, 0xFF.toByte(), 0xFF.toByte()), unsignedShortBuffer.usedBytes())

        val signedShortBuffer = ByteBuffer.allocate(4)
        signedShortBuffer.putSignedShort(Short.MIN_VALUE.toInt(), "test.signedShort")
        signedShortBuffer.putSignedShort(Short.MAX_VALUE.toInt(), "test.signedShort")
        assertContentEquals(byteArrayOf(0x80.toByte(), 0x00, 0x7F, 0xFF.toByte()), signedShortBuffer.usedBytes())

        val unsigned24Buffer = ByteBuffer.allocate(6)
        support.writeUnsignedInt24(unsigned24Buffer, 0x000000, "test.unsigned24")
        support.writeUnsignedInt24(unsigned24Buffer, 0xFFFFFF, "test.unsigned24")
        assertContentEquals(
            byteArrayOf(0x00, 0x00, 0x00, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
            unsigned24Buffer.usedBytes(),
        )

        val unsigned56Buffer = ByteBuffer.allocate(14)
        support.writeUnsignedInt56(unsigned56Buffer, 0x00000000000000, "test.unsigned56")
        support.writeUnsignedInt56(unsigned56Buffer, 0x00FFFFFFFFFFFFFF, "test.unsigned56")
        assertContentEquals(
            byteArrayOf(
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
                0xFF.toByte(),
            ),
            unsigned56Buffer.usedBytes(),
        )
    }

    @Test
    fun writeCartesianPositionAcceptsSigned24Boundaries() {
        val buffer = ByteBuffer.allocate(6)
        support.writeCartesianPosition(
            buffer,
            CartesianPosition(xMeters = -4_194_304.0, yMeters = 4_194_303.5),
        )

        buffer.flip()
        val decoded = support.readCartesianPosition(buffer)
        assertEquals(-4_194_304.0, decoded.xMeters, 0.001)
        assertEquals(4_194_303.5, decoded.yMeters, 0.001)
    }

    @Test
    fun writeCartesianPositionRejects24BitOverflow() {
        assertRangeFailure("calculatedTrackPositionCartesian.xMeters out of range") {
            support.writeCartesianPosition(
                ByteBuffer.allocate(6),
                CartesianPosition(xMeters = 4_194_304.0, yMeters = 0.0),
            )
        }
    }

    @Test
    fun barometricAltitudeRoundTripsAndRejectsOverflow() {
        val buffer = ByteBuffer.allocate(2)
        val expected = BarometricAltitude(qnhCorrectionApplied = true, altitudeFeet = -125.0)

        support.writeBarometricAltitude(buffer, expected)
        buffer.flip()

        assertEquals(expected, support.readBarometricAltitude(buffer))

        assertRangeFailure("calculatedTrackBarometricAltitude.altitudeFeet out of range") {
            support.writeBarometricAltitude(
                ByteBuffer.allocate(2),
                BarometricAltitude(qnhCorrectionApplied = false, altitudeFeet = 500_000.0),
            )
        }
    }
}
