package io.github.erikgust2.asterix.cat062

import org.junit.Test
import java.nio.ByteBuffer
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class Cat062CodecEstimatedAccuraciesTest {
    private val support = Cat062CodecSupport()

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
    fun estimatedAccuraciesRoundTripEachSubfieldIndependently() {
        val cases =
            listOf(
                EstimatedAccuracies(positionCartesian = CartesianAccuracy(xMeters = 10.0, yMeters = 20.0)),
                EstimatedAccuracies(xyCovarianceMeters = -1.5),
                EstimatedAccuracies(
                    positionWgs84 =
                        Wgs84Accuracy(
                            latitudeDegrees = 32 * WGS84_RESOLUTION,
                            longitudeDegrees =
                                48 * WGS84_RESOLUTION,
                        ),
                ),
                EstimatedAccuracies(geometricAltitudeFeet = 50.0),
                EstimatedAccuracies(barometricAltitudeFeet = 125.0),
                EstimatedAccuracies(trackVelocity = CartesianVelocity(xMetersPerSecond = 1.0, yMetersPerSecond = 2.0)),
                EstimatedAccuracies(
                    trackAcceleration = CartesianAcceleration(xMetersPerSecondSquared = 0.5, yMetersPerSecondSquared = 1.0),
                ),
                EstimatedAccuracies(rateOfClimbDescentFeetPerMinute = 50.0),
            )

        cases.forEach { expected ->
            val buffer = ByteBuffer.allocate(64)
            support.writeEstimatedAccuracies(buffer, expected)
            buffer.flip()
            assertEquals(expected, support.readEstimatedAccuracies(buffer))
        }
    }

    @Test
    fun estimatedAccuraciesRoundTripCombinedPopulation() {
        val expected =
            EstimatedAccuracies(
                positionCartesian = CartesianAccuracy(xMeters = 10.0, yMeters = 20.0),
                xyCovarianceMeters = -1.5,
                positionWgs84 = Wgs84Accuracy(latitudeDegrees = 32 * WGS84_RESOLUTION, longitudeDegrees = 48 * WGS84_RESOLUTION),
                geometricAltitudeFeet = 50.0,
                barometricAltitudeFeet = 125.0,
                trackVelocity = CartesianVelocity(xMetersPerSecond = 1.0, yMetersPerSecond = 2.0),
                trackAcceleration = CartesianAcceleration(xMetersPerSecondSquared = 0.5, yMetersPerSecondSquared = 1.0),
                rateOfClimbDescentFeetPerMinute = 50.0,
            )

        val buffer = ByteBuffer.allocate(64)
        support.writeEstimatedAccuracies(buffer, expected)
        buffer.flip()

        assertEquals(expected, support.readEstimatedAccuracies(buffer))
    }

    @Test
    fun writeEstimatedAccuraciesRejectsOverflowingFields() {
        assertRangeFailure("estimatedAccuracies.positionCartesian.xMeters out of range") {
            support.writeEstimatedAccuracies(
                ByteBuffer.allocate(32),
                EstimatedAccuracies(positionCartesian = CartesianAccuracy(xMeters = 32_768.0, yMeters = 0.0)),
            )
        }
        assertRangeFailure("estimatedAccuracies.positionWgs84.latitudeDegrees out of range") {
            support.writeEstimatedAccuracies(
                ByteBuffer.allocate(32),
                EstimatedAccuracies(positionWgs84 = Wgs84Accuracy(latitudeDegrees = 1.0, longitudeDegrees = 0.0)),
            )
        }
        assertRangeFailure("estimatedAccuracies.geometricAltitudeFeet out of range") {
            support.writeEstimatedAccuracies(
                ByteBuffer.allocate(32),
                EstimatedAccuracies(geometricAltitudeFeet = 1_600.0),
            )
        }
        assertRangeFailure("estimatedAccuracies.barometricAltitudeFeet out of range") {
            support.writeEstimatedAccuracies(
                ByteBuffer.allocate(32),
                EstimatedAccuracies(barometricAltitudeFeet = 6_400.0),
            )
        }
        assertRangeFailure("estimatedAccuracies.trackVelocity.xMetersPerSecond out of range") {
            support.writeEstimatedAccuracies(
                ByteBuffer.allocate(32),
                EstimatedAccuracies(trackVelocity = CartesianVelocity(xMetersPerSecond = 64.0, yMetersPerSecond = 0.0)),
            )
        }
        assertRangeFailure("estimatedAccuracies.trackAcceleration.xMetersPerSecondSquared out of range") {
            support.writeEstimatedAccuracies(
                ByteBuffer.allocate(32),
                EstimatedAccuracies(
                    trackAcceleration = CartesianAcceleration(xMetersPerSecondSquared = 64.0, yMetersPerSecondSquared = 0.0),
                ),
            )
        }
        assertRangeFailure("estimatedAccuracies.rateOfClimbDescentFeetPerMinute out of range") {
            support.writeEstimatedAccuracies(
                ByteBuffer.allocate(32),
                EstimatedAccuracies(rateOfClimbDescentFeetPerMinute = 1_600.0),
            )
        }
    }

    @Test
    fun readEstimatedAccuraciesRejectsTruncatedPayloads() {
        val bytes =
            ByteBuffer
                .allocate(32)
                .also {
                    support.writeEstimatedAccuracies(
                        it,
                        EstimatedAccuracies(
                            positionCartesian = CartesianAccuracy(xMeters = 10.0, yMeters = 20.0),
                            trackVelocity = CartesianVelocity(xMetersPerSecond = 1.0, yMetersPerSecond = 2.0),
                        ),
                    )
                }.usedBytes()

        assertIllegalArgumentFailure("Truncated I062/500 estimatedAccuracies.trackVelocity payload") {
            support.readEstimatedAccuracies(ByteBuffer.wrap(truncated(bytes)))
        }
    }
}
