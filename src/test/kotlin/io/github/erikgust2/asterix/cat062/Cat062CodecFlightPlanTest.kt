package io.github.erikgust2.asterix.cat062

import org.junit.Test
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Cat062CodecFlightPlanTest {
    private val support = Cat062CodecSupport()

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
    fun flightPlanRelatedDataRoundTripsEachSubfieldIndependently() {
        val cases =
            listOf(
                FlightPlanRelatedData(tag = DataSourceIdentifier(1, 2)),
                FlightPlanRelatedData(callsign = "SAS123"),
                FlightPlanRelatedData(ifpsFlightId = IfpsFlightId(typeCode = 2, number = 0x00123456)),
                FlightPlanRelatedData(flightCategory = FlightCategory(gatOatCode = 1, flightRulesCode = 2, rvsmStatus = 3, hpr = true)),
                FlightPlanRelatedData(aircraftType = "A320"),
                FlightPlanRelatedData(wakeTurbulenceCategory = "M"),
                FlightPlanRelatedData(departureAerodrome = "ESSA"),
                FlightPlanRelatedData(destinationAerodrome = "EGLL"),
                FlightPlanRelatedData(runwayDesignation = "19L"),
                FlightPlanRelatedData(currentClearedFlightLevel = 240.0),
                FlightPlanRelatedData(currentControlPosition = ControlPosition(7, 9)),
                FlightPlanRelatedData(timesOfDepartureArrival = listOf(TimeOfDepartureArrival(byteArrayOf(1, 2, 3, 4)))),
                FlightPlanRelatedData(aircraftStand = "A12"),
                FlightPlanRelatedData(standStatus = StandStatus(emp = 2, avl = 1)),
                FlightPlanRelatedData(standardInstrumentDeparture = "NIDIS1A"),
                FlightPlanRelatedData(standardInstrumentArrival = "LAM3A"),
                FlightPlanRelatedData(preEmergencyMode3a = PreEmergencyMode3a(valid = true, code = 0x750)),
                FlightPlanRelatedData(preEmergencyCallsign = "EMERG01"),
            )

        cases.forEach { expected ->
            val buffer = ByteBuffer.allocate(128)
            support.writeFlightPlanRelatedData(buffer, expected)
            buffer.flip()
            assertEquals(expected, support.readFlightPlanRelatedData(buffer))
        }
    }

    @Test
    fun flightPlanRelatedDataRoundTripsFullPopulation() {
        val expected =
            FlightPlanRelatedData(
                tag = DataSourceIdentifier(1, 2),
                callsign = "SAS123",
                ifpsFlightId = IfpsFlightId(typeCode = 2, number = 0x00123456),
                flightCategory = FlightCategory(gatOatCode = 1, flightRulesCode = 2, rvsmStatus = 3, hpr = true),
                aircraftType = "A320",
                wakeTurbulenceCategory = "M",
                departureAerodrome = "ESSA",
                destinationAerodrome = "EGLL",
                runwayDesignation = "19L",
                currentClearedFlightLevel = 240.0,
                currentControlPosition = ControlPosition(7, 9),
                timesOfDepartureArrival =
                    listOf(
                        TimeOfDepartureArrival(byteArrayOf(1, 2, 3, 4)),
                        TimeOfDepartureArrival(byteArrayOf(5, 6, 7, 8)),
                    ),
                aircraftStand = "A12",
                standStatus = StandStatus(emp = 2, avl = 1),
                standardInstrumentDeparture = "NIDIS1A",
                standardInstrumentArrival = "LAM3A",
                preEmergencyMode3a = PreEmergencyMode3a(valid = true, code = 0x750),
                preEmergencyCallsign = "EMERG01",
            )

        val buffer = ByteBuffer.allocate(256)
        support.writeFlightPlanRelatedData(buffer, expected)
        buffer.flip()

        assertEquals(expected, support.readFlightPlanRelatedData(buffer))
    }

    @Test
    fun flightPlanRelatedDataRoundTripsEmptyTimesOfDepartureArrival() {
        val expected = FlightPlanRelatedData(timesOfDepartureArrival = emptyList())
        val buffer = ByteBuffer.allocate(16)

        support.writeFlightPlanRelatedData(buffer, expected)
        buffer.flip()

        assertEquals(expected, support.readFlightPlanRelatedData(buffer))
    }

    @Test
    fun writeFlightPlanRelatedDataRejectsFieldRangesAndAsciiViolations() {
        assertRangeFailure("flightPlanRelatedData.ifpsFlightId.typeCode out of range") {
            support.writeFlightPlanRelatedData(
                ByteBuffer.allocate(32),
                FlightPlanRelatedData(ifpsFlightId = IfpsFlightId(typeCode = 4, number = 1)),
            )
        }
        assertRangeFailure("flightPlanRelatedData.flightCategory.gatOatCode out of range") {
            support.writeFlightPlanRelatedData(
                ByteBuffer.allocate(32),
                FlightPlanRelatedData(
                    flightCategory = FlightCategory(gatOatCode = 4, flightRulesCode = 0, rvsmStatus = 0, hpr = false),
                ),
            )
        }
        assertRangeFailure("flightPlanRelatedData.standStatus.emp out of range") {
            support.writeFlightPlanRelatedData(
                ByteBuffer.allocate(32),
                FlightPlanRelatedData(standStatus = StandStatus(emp = 4, avl = 0)),
            )
        }
        assertRangeFailure("flightPlanRelatedData.preEmergencyMode3a.code out of range") {
            support.writeFlightPlanRelatedData(
                ByteBuffer.allocate(32),
                FlightPlanRelatedData(preEmergencyMode3a = PreEmergencyMode3a(valid = true, code = 0x1000)),
            )
        }
        assertRangeFailure("ASCII field contains non-ASCII characters") {
            support.writeFlightPlanRelatedData(
                ByteBuffer.allocate(32),
                FlightPlanRelatedData(callsign = "SÄS123"),
            )
        }
        assertRangeFailure("flightPlanRelatedData.timesOfDepartureArrival.raw must be 4 bytes but was 3") {
            support.writeFlightPlanRelatedData(
                ByteBuffer.allocate(32),
                FlightPlanRelatedData(timesOfDepartureArrival = listOf(TimeOfDepartureArrival(byteArrayOf(1, 2, 3)))),
            )
        }
    }

    @Test
    fun readFlightPlanRelatedDataRejectsTruncatedPayloads() {
        val bytes =
            ByteBuffer
                .allocate(128)
                .also {
                    support.writeFlightPlanRelatedData(
                        it,
                        FlightPlanRelatedData(
                            callsign = "SAS123",
                            timesOfDepartureArrival = listOf(TimeOfDepartureArrival(byteArrayOf(1, 2, 3, 4))),
                            preEmergencyCallsign = "EMERG01",
                        ),
                    )
                }.usedBytes()

        assertFailsWith<BufferUnderflowException> {
            support.readFlightPlanRelatedData(ByteBuffer.wrap(truncated(bytes)))
        }
    }
}
