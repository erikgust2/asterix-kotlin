package io.github.erikgust2.asterix.cat062

import org.junit.Test
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Cat062CodecAircraftDerivedDataTest {
    private val support = Cat062CodecSupport()
    private val trajectoryIntentPoint =
        TrajectoryIntentPoint(
            tcpNumberAvailable = true,
            tcpNonCompliance = true,
            tcpNumber = 5,
            altitudeFeet = 1_000.0,
            positionWgs84 = Wgs84Position(0.0, 0.0),
            pointType = 6,
            turnDirectionCode = 2,
            turnRadiusAvailable = true,
            timeOverPointAvailable = true,
            timeOverPointSeconds = 258,
            turnRadiusNm = 12.34,
        )
    private val secondTrajectoryIntentPoint =
        TrajectoryIntentPoint(
            tcpNumberAvailable = false,
            tcpNonCompliance = false,
            tcpNumber = 12,
            altitudeFeet = 34_560.0,
            positionWgs84 =
                Wgs84Position(
                    12 * WGS84_THREE_OCTET_RESOLUTION,
                    -7 * WGS84_THREE_OCTET_RESOLUTION,
                ),
            pointType = 8,
            turnDirectionCode = 3,
            turnRadiusAvailable = false,
            timeOverPointAvailable = false,
            timeOverPointSeconds = 86_399,
            turnRadiusNm = 0.0,
        )

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
        assertEquals(1013.2, assertNotNull(decoded.barometricPressureSettingHpa), 0.001)
    }

    @Test
    fun aircraftDerivedDataUsesSpecLayoutForRollAngleTrackAngleRateAndPressureSetting() {
        val buffer = ByteBuffer.allocate(32)
        support.writeAircraftDerivedData(
            buffer,
            AircraftDerivedData(
                rollAngleDegrees = 1.25,
                trackAngleRateDegreesPerSecond = -3.5,
                positionUncertaintyCode = 10,
                barometricPressureSettingHpa = 1013.2,
            ),
        )

        assertContentEquals(
            byteArrayOf(
                0x01,
                0x01,
                0xC1.toByte(),
                0x22,
                0x00,
                0x7D,
                0x40,
                0xE4.toByte(),
                0x0A,
                0x08,
                0x54,
            ),
            buffer.usedBytes(),
        )

        buffer.flip()
        val decoded = support.readAircraftDerivedData(buffer)
        assertEquals(1.25, assertNotNull(decoded.rollAngleDegrees), 0.001)
        assertEquals(-3.5, assertNotNull(decoded.trackAngleRateDegreesPerSecond), 0.001)
        assertEquals(10, assertNotNull(decoded.positionUncertaintyCode))
        assertEquals(1013.2, assertNotNull(decoded.barometricPressureSettingHpa), 0.001)
    }

    @Test
    fun aircraftDerivedDataUsesSpecLayoutForTrajectoryIntentPoint() {
        val buffer = ByteBuffer.allocate(32)
        support.writeAircraftDerivedData(
            buffer,
            AircraftDerivedData(
                trajectoryIntentData = listOf(trajectoryIntentPoint),
            ),
        )

        assertContentEquals(
            byteArrayOf(
                0x01,
                0x40,
                0x01,
                0x45,
                0x00,
                0x64,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x6A,
                0x00,
                0x01,
                0x02,
                0x04,
                0xD2.toByte(),
            ),
            buffer.usedBytes(),
        )

        buffer.flip()
        assertEquals(
            AircraftDerivedData(trajectoryIntentData = listOf(trajectoryIntentPoint)),
            support.readAircraftDerivedData(buffer),
        )
    }

    @Test
    fun aircraftDerivedDataRoundTripsEachSubfieldIndependently() {
        val cases =
            listOf(
                "targetAddress" to AircraftDerivedData(targetAddress = 0xABCDEE),
                "targetIdentification" to AircraftDerivedData(targetIdentification = "SAS123"),
                "magneticHeadingDegrees" to AircraftDerivedData(magneticHeadingDegrees = 90.0),
                "indicatedAirspeed" to AircraftDerivedData(indicatedAirspeed = Airspeed(AirspeedType.MACH, 0.8)),
                "trueAirspeedKnots" to AircraftDerivedData(trueAirspeedKnots = 400),
                "selectedAltitude" to
                    AircraftDerivedData(
                        selectedAltitude = SelectedAltitude(sourceAvailable = true, sourceCode = 2, flightLevel = 200.0),
                    ),
                "finalStateSelectedAltitude" to
                    AircraftDerivedData(
                        finalStateSelectedAltitude =
                            FinalStateSelectedAltitude(
                                managedVerticalModeActive = true,
                                altitudeHoldActive = false,
                                approachModeActive = true,
                                flightLevel = 150.0,
                            ),
                    ),
                "trajectoryIntentStatus" to
                    AircraftDerivedData(
                        trajectoryIntentStatus = TrajectoryIntentStatus(available = true, valid = false),
                    ),
                "trajectoryIntentData" to
                    AircraftDerivedData(
                        trajectoryIntentData = listOf(trajectoryIntentPoint),
                    ),
                "communicationsCapabilities" to
                    AircraftDerivedData(
                        communicationsCapabilities =
                            CommunicationsCapabilities(
                                comCode = 3,
                                statCode = 5,
                                ssc = true,
                                arcCode = true,
                                aic = false,
                                b1a = 1,
                                b1b = 9,
                            ),
                    ),
                "adsbStatus" to AircraftDerivedData(adsbStatus = AdsbStatus(ac = 2, mn = 1, dc = 3, gbs = true, stat = 5)),
                "acasResolutionAdvisoryReport" to
                    AircraftDerivedData(
                        acasResolutionAdvisoryReport = AcasResolutionAdvisory(0x01020304050607),
                    ),
                "barometricVerticalRateFeetPerMinute" to AircraftDerivedData(barometricVerticalRateFeetPerMinute = 62.5),
                "geometricVerticalRateFeetPerMinute" to AircraftDerivedData(geometricVerticalRateFeetPerMinute = -62.5),
                "rollAngleDegrees" to AircraftDerivedData(rollAngleDegrees = 1.25),
                "trackAngleRateDegreesPerSecond" to AircraftDerivedData(trackAngleRateDegreesPerSecond = -3.5),
                "trackAngleDegrees" to AircraftDerivedData(trackAngleDegrees = 180.0),
                "groundSpeedKnots" to AircraftDerivedData(groundSpeedKnots = 450.0),
                "velocityUncertaintyCategory" to AircraftDerivedData(velocityUncertaintyCategory = 6),
                "meteorologicalData.windSpeedKnots" to
                    AircraftDerivedData(
                        meteorologicalData = MeteorologicalData(windSpeedKnots = 120),
                    ),
                "meteorologicalData.windDirectionDegrees" to
                    AircraftDerivedData(
                        meteorologicalData = MeteorologicalData(windDirectionDegrees = 180.0),
                    ),
                "meteorologicalData.temperatureCelsius" to
                    AircraftDerivedData(
                        meteorologicalData = MeteorologicalData(temperatureCelsius = -12.5),
                    ),
                "meteorologicalData.turbulenceCode" to
                    AircraftDerivedData(
                        meteorologicalData = MeteorologicalData(turbulenceCode = 7),
                    ),
                "emitterCategory" to AircraftDerivedData(emitterCategory = 11),
                "positionWgs84" to
                    AircraftDerivedData(
                        positionWgs84 = Wgs84Position(64 * WGS84_RESOLUTION, -128 * WGS84_RESOLUTION),
                    ),
                "geometricAltitudeFeet" to AircraftDerivedData(geometricAltitudeFeet = 10_000.0),
                "positionUncertaintyCode" to AircraftDerivedData(positionUncertaintyCode = 10),
                "modeSMessages" to
                    AircraftDerivedData(
                        modeSMessages = listOf(ModeSMessage(byteArrayOf(1, 2, 3, 4, 5, 6, 7), 4, 0)),
                    ),
                "indicatedAirspeedKnots" to AircraftDerivedData(indicatedAirspeedKnots = 250),
                "machNumber" to AircraftDerivedData(machNumber = 1.6),
                "barometricPressureSettingHpa" to AircraftDerivedData(barometricPressureSettingHpa = 1013.2),
            )

        cases.forEach { (label, expected) ->
            val buffer = ByteBuffer.allocate(256)
            support.writeAircraftDerivedData(buffer, expected)
            buffer.flip()
            assertEquals(expected, support.readAircraftDerivedData(buffer), label)
        }
    }

    @Test
    fun aircraftDerivedDataRoundTripsDensePopulation() {
        val expected =
            AircraftDerivedData(
                targetAddress = 0xABCDEE,
                targetIdentification = "SAS123",
                magneticHeadingDegrees = 90.0,
                indicatedAirspeed = Airspeed(AirspeedType.MACH, 0.8),
                trueAirspeedKnots = 400,
                selectedAltitude = SelectedAltitude(sourceAvailable = true, sourceCode = 2, flightLevel = 200.0),
                finalStateSelectedAltitude =
                    FinalStateSelectedAltitude(
                        managedVerticalModeActive = true,
                        altitudeHoldActive = false,
                        approachModeActive = true,
                        flightLevel = 150.0,
                    ),
                trajectoryIntentStatus = TrajectoryIntentStatus(available = true, valid = false),
                trajectoryIntentData =
                    listOf(
                        trajectoryIntentPoint,
                        secondTrajectoryIntentPoint,
                    ),
                communicationsCapabilities =
                    CommunicationsCapabilities(
                        comCode = 3,
                        statCode = 5,
                        ssc = true,
                        arcCode = true,
                        aic = false,
                        b1a = 1,
                        b1b = 9,
                    ),
                adsbStatus = AdsbStatus(ac = 2, mn = 1, dc = 3, gbs = true, stat = 5),
                acasResolutionAdvisoryReport = AcasResolutionAdvisory(0x01020304050607),
                barometricVerticalRateFeetPerMinute = 62.5,
                geometricVerticalRateFeetPerMinute = -62.5,
                rollAngleDegrees = 1.25,
                trackAngleRateDegreesPerSecond = -3.5,
                trackAngleDegrees = 180.0,
                groundSpeedKnots = 450.0,
                velocityUncertaintyCategory = 6,
                meteorologicalData =
                    MeteorologicalData(
                        windSpeedKnots = 120,
                        windDirectionDegrees = 180.0,
                        temperatureCelsius = -12.5,
                        turbulenceCode = 7,
                    ),
                emitterCategory = 11,
                positionWgs84 = Wgs84Position(64 * WGS84_RESOLUTION, -128 * WGS84_RESOLUTION),
                geometricAltitudeFeet = 10_000.0,
                positionUncertaintyCode = 10,
                modeSMessages =
                    listOf(
                        ModeSMessage(byteArrayOf(1, 2, 3, 4, 5, 6, 7), 4, 0),
                        ModeSMessage(byteArrayOf(8, 9, 10, 11, 12, 13, 14), 5, 6),
                    ),
                indicatedAirspeedKnots = 250,
                machNumber = 1.6,
                barometricPressureSettingHpa = 1013.2,
            )

        val buffer = ByteBuffer.allocate(256)
        support.writeAircraftDerivedData(buffer, expected)
        buffer.flip()

        assertEquals(expected, support.readAircraftDerivedData(buffer))
    }

    @Test
    fun aircraftDerivedDataRoundTripsSparseMeteorologicalData() {
        val expected =
            AircraftDerivedData(
                meteorologicalData =
                    MeteorologicalData(
                        windDirectionDegrees = 180.0,
                        turbulenceCode = 3,
                    ),
            )

        val buffer = ByteBuffer.allocate(16)
        support.writeAircraftDerivedData(buffer, expected)
        buffer.flip()

        assertEquals(expected, support.readAircraftDerivedData(buffer))
    }

    @Test
    fun writeAircraftDerivedDataRejectsIndicatedAirspeedOverflowIntoMachBit() {
        assertRangeFailure("aircraftDerivedData.indicatedAirspeed out of range") {
            support.writeAircraftDerivedData(
                ByteBuffer.allocate(8),
                AircraftDerivedData(
                    indicatedAirspeed =
                        Airspeed(
                            type = AirspeedType.INDICATED_AIRSPEED_KNOTS,
                            value = 8_000.0,
                        ),
                ),
            )
        }
    }

    @Test
    fun writeAircraftDerivedDataEnforcesSpecBoundsForSelectedAltitudeAndTrueAirspeed() {
        val buffer = ByteBuffer.allocate(8)
        support.writeAircraftDerivedData(
            buffer,
            AircraftDerivedData(
                trueAirspeedKnots = 2046,
                selectedAltitude =
                    SelectedAltitude(
                        sourceAvailable = true,
                        sourceCode = 2,
                        flightLevel = 1000.0,
                    ),
                finalStateSelectedAltitude =
                    FinalStateSelectedAltitude(
                        managedVerticalModeActive = true,
                        altitudeHoldActive = true,
                        approachModeActive = false,
                        flightLevel = -13.0,
                    ),
            ),
        )
        assertTrue(buffer.position() > 0)

        assertRangeFailure("aircraftDerivedData.trueAirspeedKnots out of range") {
            support.writeAircraftDerivedData(
                ByteBuffer.allocate(8),
                AircraftDerivedData(trueAirspeedKnots = 2047),
            )
        }
        assertRangeFailure("aircraftDerivedData.selectedAltitude.flightLevel out of range") {
            support.writeAircraftDerivedData(
                ByteBuffer.allocate(8),
                AircraftDerivedData(
                    selectedAltitude =
                        SelectedAltitude(
                            sourceAvailable = true,
                            sourceCode = 1,
                            flightLevel = 1000.25,
                        ),
                ),
            )
        }
        assertRangeFailure("aircraftDerivedData.finalStateSelectedAltitude.flightLevel out of range") {
            support.writeAircraftDerivedData(
                ByteBuffer.allocate(8),
                AircraftDerivedData(
                    finalStateSelectedAltitude =
                        FinalStateSelectedAltitude(
                            managedVerticalModeActive = false,
                            altitudeHoldActive = false,
                            approachModeActive = false,
                            flightLevel = -13.25,
                        ),
                ),
            )
        }
    }

    @Test
    fun writeAircraftDerivedDataRejectsBarometricPressureOverflowIntoReservedBits() {
        assertRangeFailure("aircraftDerivedData.barometricPressureSettingHpa out of range") {
            support.writeAircraftDerivedData(
                ByteBuffer.allocate(8),
                AircraftDerivedData(barometricPressureSettingHpa = 1_300.0),
            )
        }
    }

    @Test
    fun writeAircraftDerivedDataEnforcesTrackAngleRateSpecRange() {
        val buffer = ByteBuffer.allocate(8)
        support.writeAircraftDerivedData(
            buffer,
            AircraftDerivedData(trackAngleRateDegreesPerSecond = 15.0),
        )
        assertTrue(buffer.position() > 0)

        assertRangeFailure("aircraftDerivedData.trackAngleRateDegreesPerSecond out of range") {
            support.writeAircraftDerivedData(
                ByteBuffer.allocate(8),
                AircraftDerivedData(trackAngleRateDegreesPerSecond = 15.25),
            )
        }
    }

    @Test
    fun writeAircraftDerivedDataRejectsAcasResolutionOverflow() {
        assertRangeFailure("aircraftDerivedData.acasResolutionAdvisoryReport out of range") {
            support.writeAircraftDerivedData(
                ByteBuffer.allocate(16),
                AircraftDerivedData(acasResolutionAdvisoryReport = AcasResolutionAdvisory(1L shl 56)),
            )
        }
    }

    @Test
    fun writeAircraftDerivedDataRejectsTargetAddress24BitOverflowWithFieldName() {
        assertRangeFailure("aircraftDerivedData.targetAddress out of range") {
            support.writeAircraftDerivedData(
                ByteBuffer.allocate(8),
                AircraftDerivedData(targetAddress = 1 shl 24),
            )
        }
    }

    @Test
    fun writeAircraftDerivedDataRejectsInvalidCommunicationsAndModeSMessageShapes() {
        assertRangeFailure("aircraftDerivedData.communicationsCapabilities.comCode out of range") {
            support.writeAircraftDerivedData(
                ByteBuffer.allocate(32),
                AircraftDerivedData(
                    communicationsCapabilities =
                        CommunicationsCapabilities(
                            comCode = 8,
                            statCode = 0,
                            ssc = false,
                            arcCode = false,
                            aic = false,
                            b1a = 0,
                            b1b = 0,
                        ),
                ),
            )
        }

        assertRangeFailure("aircraftDerivedData.trajectoryIntentData.pointType out of range") {
            support.writeAircraftDerivedData(
                ByteBuffer.allocate(32),
                AircraftDerivedData(
                    trajectoryIntentData =
                        listOf(
                            trajectoryIntentPoint.copy(pointType = 12),
                        ),
                ),
            )
        }

        assertRangeFailure("aircraftDerivedData.modeSMessages.message must be 7 bytes but was 3") {
            support.writeAircraftDerivedData(
                ByteBuffer.allocate(32),
                AircraftDerivedData(
                    modeSMessages = listOf(ModeSMessage(byteArrayOf(1, 2, 3), 4, 0)),
                ),
            )
        }

        assertRangeFailure("aircraftDerivedData.modeSMessages.bds1 out of range") {
            support.writeAircraftDerivedData(
                ByteBuffer.allocate(32),
                AircraftDerivedData(
                    modeSMessages = listOf(ModeSMessage(byteArrayOf(1, 2, 3, 4, 5, 6, 7), 16, 0)),
                ),
            )
        }
    }

    @Test
    fun writeAircraftDerivedDataRejectsVelocityUncertaintyOverflow() {
        assertRangeFailure("aircraftDerivedData.velocityUncertaintyCategory out of range") {
            support.writeAircraftDerivedData(
                ByteBuffer.allocate(8),
                AircraftDerivedData(velocityUncertaintyCategory = 8),
            )
        }
    }

    @Test
    fun readAircraftDerivedDataRejectsTruncatedPayloads() {
        val bytes =
            ByteBuffer
                .allocate(128)
                .also {
                    support.writeAircraftDerivedData(
                        it,
                        AircraftDerivedData(
                            targetAddress = 0x123456,
                            trajectoryIntentData = listOf(trajectoryIntentPoint),
                            modeSMessages = listOf(ModeSMessage(byteArrayOf(1, 2, 3, 4, 5, 6, 7), 4, 0)),
                        ),
                    )
                }.usedBytes()

        assertFailsWith<BufferUnderflowException> {
            support.readAircraftDerivedData(ByteBuffer.wrap(truncated(bytes)))
        }
    }
}
