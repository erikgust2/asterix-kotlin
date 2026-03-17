package io.github.erikgust2.asterix.cat062

import org.junit.Test
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Cat062CodecSupportTest {
    private val support = Cat062CodecSupport()

    @Test
    fun usesCat062V115FrnNumbersInFspec() {
        val bytes =
            writeRecordBytes(
                minimalValidRecord().copy(
                    serviceIdentification = 4,
                ),
            )

        assertEquals(0xB1, bytes[0].toUnsignedInt())
        assertEquals(0x0C, bytes[1].toUnsignedInt())
    }

    @Test
    fun publicReadRecordAndWriteRecordRoundTripThroughFspecDispatch() {
        val expected =
            minimalValidRecord(timeOfTrackInformationSeconds = 1024.0).copy(
                serviceIdentification = 4,
                calculatedTrackPositionCartesian = CartesianPosition(12.0, -6.5),
                trackMode3aCode = Mode3ACode(code = 0x750, codeChanged = true),
                targetIdentification =
                    TargetIdentification(
                        source = TargetIdentificationSource.CALLSIGN_OR_REGISTRATION_FROM_TRANSPONDER,
                        value = "SAS123",
                    ),
                targetSizeAndOrientation = TargetSizeAndOrientation(lengthMeters = 12, orientationDegrees = 90.0, widthMeters = 7),
                vehicleFleetIdentification = VehicleFleetIdentification.FIRE,
                composedTrackNumber = ComposedTrackNumber(systemUnitIdentification = 7, trackNumber = 1024),
                specialPurposeField = byteArrayOf(0x22, 0x11).toRawBytes(),
            )

        val decoded = Cat062Codec.readRecord(ByteBuffer.wrap(writeRecordBytes(expected)))

        assertEquals(expected, decoded)
    }

    @Test
    fun writeRecordRequiresEachMandatoryCat062Item() {
        val testCases =
            listOf(
                "CAT062 record missing mandatory I062/010 dataSourceIdentifier" to minimalValidRecord().copy(dataSourceIdentifier = null),
                "CAT062 record missing mandatory I062/040 trackNumber" to minimalValidRecord().copy(trackNumber = null),
                "CAT062 record missing mandatory I062/070 timeOfTrackInformationSeconds" to
                    minimalValidRecord().copy(timeOfTrackInformationSeconds = null),
                "CAT062 record missing mandatory I062/080 trackStatus" to minimalValidRecord().copy(trackStatus = null),
            )

        testCases.forEach { (expectedMessage, record) ->
            val error =
                assertFailsWith<IllegalArgumentException> {
                    Cat062Codec.writeRecord(ByteBuffer.allocate(64), record)
                }
            assertEquals(expectedMessage, error.message)
        }
    }

    @Test
    fun readRecordRejectsUnsupportedFrnInFirstFspecExtent() {
        val error =
            assertFailsWith<IllegalStateException> {
                Cat062Codec.readRecord(bufferOf(0x40))
            }

        assertEquals("Unsupported FRN 2 in CAT062", error.message)
    }

    @Test
    fun readRecordRejectsUnsupportedFrnInLaterFspecExtent() {
        val error =
            assertFailsWith<IllegalStateException> {
                Cat062Codec.readRecord(bufferOf(0x01, 0x01, 0x01, 0x01, 0x80))
            }

        assertEquals("Unsupported FRN 29 in CAT062", error.message)
    }

    @Test
    fun readRecordRejectsTruncatedFspecContinuation() {
        assertFailsWith<BufferUnderflowException> {
            Cat062Codec.readRecord(bufferOf(0x01))
        }
    }

    @Test
    fun readRecordRejectsTruncatedFieldPayload() {
        assertFailsWith<BufferUnderflowException> {
            Cat062Codec.readRecord(bufferOf(0x80, 0x01))
        }
    }

    @Test
    fun readRecordRejectsTruncatedReservedExpansionFieldPayload() {
        val bytes =
            writeRecordBytes(
                minimalValidRecord().copy(
                    reservedExpansionField = byteArrayOf(0x11, 0x22).toRawBytes(),
                ),
            )

        assertFailsWith<BufferUnderflowException> {
            Cat062Codec.readRecord(ByteBuffer.wrap(truncated(bytes)))
        }
    }

    @Test
    fun writeRecordRejectsTimeOfTrackInformation24BitOverflowWithFieldName() {
        assertRangeFailure("timeOfTrackInformationSeconds out of range") {
            writeRecordBytes(
                minimalValidRecord().copy(
                    timeOfTrackInformationSeconds = 131_072.0,
                    calculatedTrackPositionWgs84 = Wgs84Position(0.0, 0.0),
                ),
            )
        }
    }

    @Test
    fun publicReadRecordAndWriteRecordRoundTripGeometricAltitude() {
        val expected = minimalValidRecord().copy(calculatedTrackGeometricAltitudeFeet = 10_000.0)

        val decoded = Cat062Codec.readRecord(ByteBuffer.wrap(writeRecordBytes(expected)))

        assertEquals(expected, decoded)
    }

    @Test
    fun publicReadRecordAndWriteRecordRoundTripRateOfClimbDescent() {
        val expected = minimalValidRecord().copy(rateOfClimbDescentFeetPerMinute = -62.5)

        val decoded = Cat062Codec.readRecord(ByteBuffer.wrap(writeRecordBytes(expected)))

        assertEquals(expected, decoded)
    }

    @Test
    fun writeRecordRejectsOutOfRangeGeometricAltitudeAndRateOfClimbDescent() {
        assertRangeFailure("calculatedTrackGeometricAltitudeFeet out of range") {
            writeRecordBytes(
                minimalValidRecord().copy(
                    calculatedTrackGeometricAltitudeFeet = 204_800.0,
                ),
            )
        }
        assertRangeFailure("rateOfClimbDescentFeetPerMinute out of range") {
            writeRecordBytes(
                minimalValidRecord().copy(
                    rateOfClimbDescentFeetPerMinute = 204_800.0,
                ),
            )
        }
    }

    @Test
    fun readRecordRejectsTruncatedPayloadsForEveryFixedWidthFrn() {
        val testCases =
            listOf(
                "I062/010 dataSourceIdentifier" to
                    writeStandaloneRecordBytes(
                        1 to writeFieldBytes { support.writeDataSourceIdentifier(it, DataSourceIdentifier(1, 2)) },
                    ),
                "I062/015 serviceIdentification" to
                    writeStandaloneRecordBytes(
                        3 to writeFieldBytes { it.putUnsignedByte(4, "serviceIdentification") },
                    ),
                "I062/070 timeOfTrackInformation" to
                    writeStandaloneRecordBytes(
                        4 to writeFieldBytes { support.writeUnsignedInt24(it, 0x001000, "timeOfTrackInformationSeconds") },
                    ),
                "I062/105 calculatedTrackPositionWgs84" to
                    writeStandaloneRecordBytes(
                        5 to
                            writeFieldBytes {
                                support.writeWgs84Position(
                                    it,
                                    Wgs84Position(32 * WGS84_RESOLUTION, -64 * WGS84_RESOLUTION),
                                )
                            },
                    ),
                "I062/100 calculatedTrackPositionCartesian" to
                    writeStandaloneRecordBytes(
                        6 to writeFieldBytes { support.writeCartesianPosition(it, CartesianPosition(-128.0, 256.5)) },
                    ),
                "I062/185 calculatedTrackVelocityCartesian" to
                    writeStandaloneRecordBytes(
                        7 to writeFieldBytes { support.writeCartesianVelocity(it, CartesianVelocity(-25.0, 12.25)) },
                    ),
                "I062/210 calculatedAccelerationCartesian" to
                    writeStandaloneRecordBytes(
                        8 to writeFieldBytes { support.writeCartesianAcceleration(it, CartesianAcceleration(-4.0, 7.75)) },
                    ),
                "I062/060 trackMode3aCode" to
                    writeStandaloneRecordBytes(
                        9 to writeFieldBytes { support.writeMode3ACode(it, Mode3ACode(code = 0x750, codeChanged = true)) },
                    ),
                "I062/245 targetIdentification" to
                    writeStandaloneRecordBytes(
                        10 to
                            writeFieldBytes {
                                support.writeTargetIdentification(
                                    it,
                                    TargetIdentification(
                                        source = TargetIdentificationSource.CALLSIGN_OR_REGISTRATION_FROM_TRANSPONDER,
                                        value = "SAS123",
                                    ),
                                )
                            },
                    ),
                "I062/040 trackNumber" to
                    writeStandaloneRecordBytes(
                        12 to writeFieldBytes { it.putUnsignedShort(42, "trackNumber") },
                    ),
                "I062/200 modeOfMovement" to
                    writeStandaloneRecordBytes(
                        15 to
                            writeFieldBytes {
                                support.writeModeOfMovement(
                                    it,
                                    ModeOfMovement(
                                        transversalAccelerationClass = TransversalAccelerationClass.LEFT_TURN,
                                        longitudinalAccelerationClass = MovementAccelerationClass.INCREASING_GROUND_SPEED,
                                        verticalMovementClass = VerticalMovementClass.CLIMB,
                                        altitudeDiscrepancyFlag = true,
                                    ),
                                )
                            },
                    ),
                "I062/136 measuredFlightLevel" to
                    writeStandaloneRecordBytes(
                        17 to writeFieldBytes { support.writeFlightLevelMeasurement(it, FlightLevelMeasurement(310.0)) },
                    ),
                "I062/130 calculatedTrackGeometricAltitudeFeet" to
                    writeStandaloneRecordBytes(
                        18 to
                            writeFieldBytes {
                                it.putSignedShort(
                                    quantize(10_000.0, 6.25, "calculatedTrackGeometricAltitudeFeet"),
                                    "calculatedTrackGeometricAltitudeFeet",
                                )
                            },
                    ),
                "I062/135 calculatedTrackBarometricAltitude" to
                    writeStandaloneRecordBytes(
                        19 to
                            writeFieldBytes {
                                support.writeBarometricAltitude(
                                    it,
                                    BarometricAltitude(qnhCorrectionApplied = true, altitudeFeet = 500.0),
                                )
                            },
                    ),
                "I062/220 rateOfClimbDescentFeetPerMinute" to
                    writeStandaloneRecordBytes(
                        20 to
                            writeFieldBytes {
                                it.putSignedShort(
                                    quantize(-62.5, 6.25, "rateOfClimbDescentFeetPerMinute"),
                                    "rateOfClimbDescentFeetPerMinute",
                                )
                            },
                    ),
                "I062/300 vehicleFleetIdentification" to
                    writeStandaloneRecordBytes(
                        23 to
                            writeFieldBytes {
                                it.putUnsignedByte(VehicleFleetIdentification.FIRE.code, "vehicleFleetIdentification.code")
                            },
                    ),
                "I062/120 trackMode2Code" to
                    writeStandaloneRecordBytes(
                        25 to writeFieldBytes { support.writeMode2Code(it, Mode2Code(0x668)) },
                    ),
                "I062/510 composedTrackNumber" to
                    writeStandaloneRecordBytes(
                        26 to
                            writeFieldBytes {
                                support.writeComposedTrackNumber(
                                    it,
                                    ComposedTrackNumber(systemUnitIdentification = 17, trackNumber = 0x4567),
                                )
                            },
                    ),
            )

        testCases.forEach { (label, bytes) ->
            assertFailsWith<BufferUnderflowException>(label) {
                Cat062Codec.readRecord(ByteBuffer.wrap(truncated(bytes)))
            }
        }
    }

    @Test
    fun publicWriteRecordUsesExpectedFspecBytesForSparseAndExtendedRecords() {
        val sparse = writeRecordBytes(minimalValidRecord())
        val extended =
            writeRecordBytes(
                minimalValidRecord().copy(
                    serviceIdentification = 4,
                    measuredInformation =
                        MeasuredInformation(
                            sensorIdentification = DataSourceIdentifier(1, 1),
                        ),
                    specialPurposeField = byteArrayOf(0x01).toRawBytes(),
                ),
            )

        assertContentEquals(byteArrayOf(0x91.toByte(), 0x0C), sparse.copyOf(2))
        assertContentEquals(
            byteArrayOf(0xB1.toByte(), 0x0D, 0x01, 0x03, 0x02),
            extended.copyOf(5),
        )
    }
}
