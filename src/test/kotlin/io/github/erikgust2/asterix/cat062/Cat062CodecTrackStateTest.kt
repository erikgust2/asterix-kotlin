package io.github.erikgust2.asterix.cat062

import org.junit.Test
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Cat062CodecTrackStateTest {
    private val support = Cat062CodecSupport()

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

        assertContentEquals(byteArrayOf(0x56), buffer.usedBytes())
        buffer.flip()

        val decoded = support.readModeOfMovement(buffer)
        assertEquals(TransversalAccelerationClass.RIGHT_TURN, decoded.transversalAccelerationClass)
        assertEquals(MovementAccelerationClass.INCREASING_GROUND_SPEED, decoded.longitudinalAccelerationClass)
        assertEquals(VerticalMovementClass.CLIMB, decoded.verticalMovementClass)
        assertTrue(decoded.altitudeDiscrepancyFlag)
    }

    @Test
    fun trackStatusMatchesV115ExtentLayout() {
        val expected =
            TrackStatus(
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
            buffer.usedBytes(),
        )

        buffer.flip()
        assertEquals(expected, support.readTrackStatus(buffer))
    }

    @Test
    fun trackStatusUsesSingleOctetWhenOnlyFirstExtentIsPresent() {
        val buffer = ByteBuffer.allocate(1)
        val expected = TrackStatus(mon = true, spi = true, mrh = false, src = 2, cnf = true)

        support.writeTrackStatus(buffer, expected)

        assertContentEquals(byteArrayOf(0xCA.toByte()), buffer.usedBytes())
        buffer.flip()
        assertEquals(expected, support.readTrackStatus(buffer))
    }

    @Test
    fun readTrackStatusLeavesLaterExtentFieldsNullWhenExtentsAreAbsent() {
        val decoded = support.readTrackStatus(bufferOf(0x8A))

        assertEquals(TrackStatus(mon = true, spi = false, mrh = false, src = 2, cnf = true), decoded)
        assertNull(decoded.sim)
        assertNull(decoded.md4)
        assertNull(decoded.cst)
        assertNull(decoded.sds)
    }

    @Test
    fun readTrackStatusUsesConcreteDefaultValuesForPresentExtents() {
        val decoded = support.readTrackStatus(bufferOf(0x8B, 0x00))

        assertEquals(
            firstExtentTrackStatus().copy(
                sim = false,
                tse = false,
                tsb = false,
                fpc = false,
                aff = false,
                stp = false,
                kos = false,
            ),
            decoded,
        )
    }

    @Test
    fun readTrackStatusUsesConcreteDefaultValuesForLaterPresentExtents() {
        val decoded = support.readTrackStatus(bufferOf(0x8B, 0x01, 0x01, 0x01, 0x00))

        assertEquals(
            firstFiveExtentsTrackStatus().copy(
                sim = false,
                tse = false,
                tsb = false,
                fpc = false,
                aff = false,
                stp = false,
                kos = false,
                ama = false,
                md4 = 0,
                me = false,
                mi = false,
                md5 = 0,
                cst = false,
                psr = false,
                ssr = false,
                mds = false,
                ads = false,
                suc = false,
                aac = false,
                sds = 0,
                ems = 0,
            ),
            decoded,
        )
    }

    @Test
    fun writeTrackStatusRoundTripsExplicitDefaultValuesInPresentExtents() {
        val expected =
            firstExtentTrackStatus().copy(
                sim = false,
                tse = false,
                tsb = false,
                fpc = false,
                aff = false,
                stp = false,
                kos = false,
            )

        val buffer = ByteBuffer.allocate(2)
        support.writeTrackStatus(buffer, expected)

        assertContentEquals(byteArrayOf(0x8B.toByte(), 0x00), buffer.usedBytes())
        buffer.flip()
        assertEquals(expected, support.readTrackStatus(buffer))
    }

    @Test
    fun writeTrackStatusRoundTripsExplicitDefaultValuesInLaterPresentExtents() {
        val expected =
            firstFiveExtentsTrackStatus().copy(
                sim = false,
                tse = false,
                tsb = false,
                fpc = false,
                aff = false,
                stp = false,
                kos = false,
                ama = false,
                md4 = 0,
                me = false,
                mi = false,
                md5 = 0,
                cst = false,
                psr = false,
                ssr = false,
                mds = false,
                ads = false,
                suc = false,
                aac = false,
                sds = 0,
                ems = 0,
            )

        val buffer = ByteBuffer.allocate(5)
        support.writeTrackStatus(buffer, expected)

        assertContentEquals(byteArrayOf(0x8B.toByte(), 0x01, 0x01, 0x01, 0x00), buffer.usedBytes())
        buffer.flip()
        assertEquals(expected, support.readTrackStatus(buffer))
    }

    @Test
    fun writeTrackStatusRejectsIncompleteFirstExtent() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                support.writeTrackStatus(
                    ByteBuffer.allocate(5),
                    TrackStatus(mon = true, spi = false, mrh = false, cnf = true),
                )
            }

        assertEquals("trackStatus.octet1 fields must all be specified", error.message)
    }

    @Test
    fun writeTrackStatusRejectsIncompletePresentExtents() {
        val octet2Error =
            assertFailsWith<IllegalArgumentException> {
                support.writeTrackStatus(
                    ByteBuffer.allocate(5),
                    firstExtentTrackStatus().copy(sim = true),
                )
            }
        assertEquals("trackStatus.octet2 fields must all be specified when any octet2-or-later field is present", octet2Error.message)

        val impliedOctet2Error =
            assertFailsWith<IllegalArgumentException> {
                support.writeTrackStatus(
                    ByteBuffer.allocate(5),
                    firstExtentTrackStatus().copy(
                        ama = false,
                        md4 = 0,
                        me = false,
                        mi = false,
                        md5 = 0,
                    ),
                )
            }
        assertEquals(
            "trackStatus.octet2 fields must all be specified when any octet2-or-later field is present",
            impliedOctet2Error.message,
        )
    }

    @Test
    fun writeTrackStatusRejectsOutOfRangeValues() {
        assertRangeFailure("trackStatus.src out of range") {
            support.writeTrackStatus(ByteBuffer.allocate(5), firstExtentTrackStatus().copy(src = 8))
        }
        assertRangeFailure("trackStatus.md4 out of range") {
            support.writeTrackStatus(
                ByteBuffer.allocate(5),
                firstTwoExtentsTrackStatus().copy(md4 = 4, ama = false, me = false, mi = false, md5 = 0),
            )
        }
        assertRangeFailure("trackStatus.md5 out of range") {
            support.writeTrackStatus(
                ByteBuffer.allocate(5),
                firstTwoExtentsTrackStatus().copy(ama = false, md4 = 0, me = false, mi = false, md5 = 4),
            )
        }
        assertRangeFailure("trackStatus.sds out of range") {
            support.writeTrackStatus(
                ByteBuffer.allocate(5),
                firstFourExtentsTrackStatus().copy(sds = 4, ems = 0),
            )
        }
        assertRangeFailure("trackStatus.ems out of range") {
            support.writeTrackStatus(
                ByteBuffer.allocate(5),
                firstFourExtentsTrackStatus().copy(sds = 0, ems = 8),
            )
        }
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

        assertContentEquals(byteArrayOf(0x84.toByte(), 0x05, 0x03), buffer.usedBytes())
        buffer.flip()

        val decoded = support.readSystemTrackUpdateAges(buffer)
        assertEquals(1.25, assertNotNull(decoded.agesSeconds[SystemTrackAgeType.TRACK]), 0.001)
        assertEquals(0.75, assertNotNull(decoded.agesSeconds[SystemTrackAgeType.ADS_ES]), 0.001)
    }

    @Test
    fun systemTrackUpdateAgesRoundTripEmptyAndDensePopulations() {
        val emptyBuffer = ByteBuffer.allocate(4)
        support.writeSystemTrackUpdateAges(emptyBuffer, SystemTrackUpdateAges(emptyMap()))
        emptyBuffer.flip()
        assertEquals(SystemTrackUpdateAges(emptyMap()), support.readSystemTrackUpdateAges(emptyBuffer))

        val dense =
            SystemTrackUpdateAges(
                SystemTrackAgeType.entries.associateWith { (it.ordinal + 1) * 0.25 },
            )
        val denseBuffer = ByteBuffer.allocate(32)
        support.writeSystemTrackUpdateAges(denseBuffer, dense)
        denseBuffer.flip()
        assertEquals(dense, support.readSystemTrackUpdateAges(denseBuffer))
    }

    @Test
    fun trackDataAgesRoundTripSparseAndMultiOctetIndicator() {
        val expected =
            TrackDataAges(
                linkedMapOf(
                    TrackDataAgeType.MFL to 0.25,
                    TrackDataAgeType.BVR to 1.75,
                    TrackDataAgeType.BPS to 2.5,
                ),
            )

        val buffer = ByteBuffer.allocate(64)
        support.writeTrackDataAges(buffer, expected)
        val bytes = buffer.usedBytes()

        assertTrue(bytes.size > 4)
        assertEquals(0x81, bytes[0].toUnsignedInt())
        assertEquals(0x01, bytes[1].toUnsignedInt())
        assertEquals(0x41, bytes[2].toUnsignedInt())
        assertEquals(0x01, bytes[3].toUnsignedInt())
        assertEquals(0x20, bytes[4].toUnsignedInt())

        buffer.flip()
        assertEquals(expected, support.readTrackDataAges(buffer))
    }

    @Test
    fun trackDataAgesRejectOutOfRangeAgesAndTruncatedPayloads() {
        assertRangeFailure("trackDataAges.MFL out of range") {
            support.writeTrackDataAges(
                ByteBuffer.allocate(8),
                TrackDataAges(mapOf(TrackDataAgeType.MFL to 64.0)),
            )
        }

        val bytes =
            ByteBuffer
                .allocate(16)
                .also {
                    support.writeTrackDataAges(
                        it,
                        TrackDataAges(mapOf(TrackDataAgeType.MFL to 1.0, TrackDataAgeType.MAC to 2.0)),
                    )
                }.usedBytes()

        assertFailsWith<BufferUnderflowException> {
            support.readTrackDataAges(ByteBuffer.wrap(truncated(bytes)))
        }
    }
}

private fun firstExtentTrackStatus(): TrackStatus = TrackStatus(mon = true, spi = false, mrh = false, src = 2, cnf = true)

private fun firstTwoExtentsTrackStatus(): TrackStatus =
    firstExtentTrackStatus().copy(
        sim = false,
        tse = false,
        tsb = false,
        fpc = false,
        aff = false,
        stp = false,
        kos = false,
    )

private fun firstFourExtentsTrackStatus(): TrackStatus =
    firstTwoExtentsTrackStatus().copy(
        ama = false,
        md4 = 0,
        me = false,
        mi = false,
        md5 = 0,
        cst = false,
        psr = false,
        ssr = false,
        mds = false,
        ads = false,
        suc = false,
        aac = false,
    )

private fun firstFiveExtentsTrackStatus(): TrackStatus =
    firstFourExtentsTrackStatus().copy(
        sds = 0,
        ems = 0,
    )
