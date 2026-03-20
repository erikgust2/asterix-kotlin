package io.github.erikgust2.asterix.cat062

import java.nio.ByteBuffer

internal fun Cat062CodecSupport.readTrackStatus(buffer: ByteBuffer): TrackStatus =
    decodeCat062Item("I062/080", "trackStatus") {
        val octet1 = decodeCat062Item("I062/080", "trackStatus.octet1") { buffer.get().toUnsignedInt() }
        val hasOctet2 = (octet1 and 0x01) != 0
        val octet2 = if (hasOctet2) decodeCat062Item("I062/080", "trackStatus.octet2") { buffer.get().toUnsignedInt() } else null
        val hasOctet3 = octet2 != null && (octet2 and 0x01) != 0
        val octet3 = if (hasOctet3) decodeCat062Item("I062/080", "trackStatus.octet3") { buffer.get().toUnsignedInt() } else null
        val hasOctet4 = octet3 != null && (octet3 and 0x01) != 0
        val octet4 = if (hasOctet4) decodeCat062Item("I062/080", "trackStatus.octet4") { buffer.get().toUnsignedInt() } else null
        val hasOctet5 = octet4 != null && (octet4 and 0x01) != 0
        val octet5 = if (hasOctet5) decodeCat062Item("I062/080", "trackStatus.octet5") { buffer.get().toUnsignedInt() } else null

        TrackStatus(
            mon = (octet1 and 0x80) != 0,
            spi = (octet1 and 0x40) != 0,
            mrh = (octet1 and 0x20) != 0,
            src = TrackSource.fromCode((octet1 ushr 2) and 0x07),
            cnf = (octet1 and 0x02) != 0,
            sim = octet2?.let { (it and 0x80) != 0 },
            tse = octet2?.let { (it and 0x40) != 0 },
            tsb = octet2?.let { (it and 0x20) != 0 },
            fpc = octet2?.let { (it and 0x10) != 0 },
            aff = octet2?.let { (it and 0x08) != 0 },
            stp = octet2?.let { (it and 0x04) != 0 },
            kos = octet2?.let { (it and 0x02) != 0 },
            ama = octet3?.let { (it and 0x80) != 0 },
            md4 = octet3?.let { Mode4Status.fromCode((it ushr 5) and 0x03) },
            me = octet3?.let { (it and 0x10) != 0 },
            mi = octet3?.let { (it and 0x08) != 0 },
            md5 = octet3?.let { Mode5Status.fromCode((it ushr 1) and 0x03) },
            cst = octet4?.let { (it and 0x80) != 0 },
            psr = octet4?.let { (it and 0x40) != 0 },
            ssr = octet4?.let { (it and 0x20) != 0 },
            mds = octet4?.let { (it and 0x10) != 0 },
            ads = octet4?.let { (it and 0x08) != 0 },
            suc = octet4?.let { (it and 0x04) != 0 },
            aac = octet4?.let { (it and 0x02) != 0 },
            sds = octet5?.let { SurveillanceDataStatus.fromCode((it ushr 6) and 0x03) },
            ems = octet5?.let { TrackEmergencyStatus.fromCode((it ushr 3) and 0x07) },
        )
    }

internal fun Cat062CodecSupport.writeTrackStatus(
    buffer: ByteBuffer,
    value: TrackStatus,
) {
    encodeCat062Item("I062/080") {
        val highestExtent = value.highestSpecifiedExtent()
        requireTrackStatusExtentComplete(
            listOf(value.mon, value.spi, value.mrh, value.src, value.cnf),
            "trackStatus.octet1 fields must all be specified",
        )
        if (highestExtent >= 2) {
            requireTrackStatusExtentComplete(
                listOf(value.sim, value.tse, value.tsb, value.fpc, value.aff, value.stp, value.kos),
                "trackStatus.octet2 fields must all be specified when any octet2-or-later field is present",
            )
        }
        if (highestExtent >= 3) {
            requireTrackStatusExtentComplete(
                listOf(value.ama, value.md4, value.me, value.mi, value.md5),
                "trackStatus.octet3 fields must all be specified when any octet3-or-later field is present",
            )
        }
        if (highestExtent >= 4) {
            requireTrackStatusExtentComplete(
                listOf(value.cst, value.psr, value.ssr, value.mds, value.ads, value.suc, value.aac),
                "trackStatus.octet4 fields must all be specified when any octet4-or-later field is present",
            )
        }
        if (highestExtent >= 5) {
            requireTrackStatusExtentComplete(
                listOf(value.sds, value.ems),
                "trackStatus.octet5 fields must all be specified when any octet5 field is present",
            )
        }

        val needsOctet2 = highestExtent >= 2
        val needsOctet3 = highestExtent >= 3
        val needsOctet4 = highestExtent >= 4
        val needsOctet5 = highestExtent >= 5

        var octet1 = 0
        if (value.mon == true) octet1 = octet1 or 0x80
        if (value.spi == true) octet1 = octet1 or 0x40
        if (value.mrh == true) octet1 = octet1 or 0x20
        octet1 = octet1 or (value.src!!.code shl 2)
        if (value.cnf == true) octet1 = octet1 or 0x02
        if (needsOctet2) octet1 = octet1 or 0x01
        buffer.put(octet1.toByte())

        if (needsOctet2) {
            var octet2 = 0
            if (value.sim == true) octet2 = octet2 or 0x80
            if (value.tse == true) octet2 = octet2 or 0x40
            if (value.tsb == true) octet2 = octet2 or 0x20
            if (value.fpc == true) octet2 = octet2 or 0x10
            if (value.aff == true) octet2 = octet2 or 0x08
            if (value.stp == true) octet2 = octet2 or 0x04
            if (value.kos == true) octet2 = octet2 or 0x02
            if (needsOctet3) octet2 = octet2 or 0x01
            buffer.put(octet2.toByte())
        }
        if (needsOctet3) {
            var octet3 = 0
            if (value.ama == true) octet3 = octet3 or 0x80
            octet3 = octet3 or ((value.md4!!.code and 0x03) shl 5)
            if (value.me == true) octet3 = octet3 or 0x10
            if (value.mi == true) octet3 = octet3 or 0x08
            octet3 = octet3 or ((value.md5!!.code and 0x03) shl 1)
            if (needsOctet4) octet3 = octet3 or 0x01
            buffer.put(octet3.toByte())
        }
        if (needsOctet4) {
            var octet4 = 0
            if (value.cst == true) octet4 = octet4 or 0x80
            if (value.psr == true) octet4 = octet4 or 0x40
            if (value.ssr == true) octet4 = octet4 or 0x20
            if (value.mds == true) octet4 = octet4 or 0x10
            if (value.ads == true) octet4 = octet4 or 0x08
            if (value.suc == true) octet4 = octet4 or 0x04
            if (value.aac == true) octet4 = octet4 or 0x02
            if (needsOctet5) octet4 = octet4 or 0x01
            buffer.put(octet4.toByte())
        }
        if (needsOctet5) {
            var octet5 = 0
            octet5 = octet5 or ((value.sds!!.code and 0x03) shl 6)
            octet5 = octet5 or ((value.ems!!.code and 0x07) shl 3)
            buffer.put(octet5.toByte())
        }
    }
}

private fun TrackStatus.highestSpecifiedExtent(): Int =
    when {
        listOf(sds, ems).any { it != null } -> 5
        listOf(cst, psr, ssr, mds, ads, suc, aac).any { it != null } -> 4
        listOf(ama, md4, me, mi, md5).any { it != null } -> 3
        listOf(sim, tse, tsb, fpc, aff, stp, kos).any { it != null } -> 2
        else -> 1
    }

private fun requireTrackStatusExtentComplete(
    fields: List<Any?>,
    message: String,
) {
    require(fields.all { it != null }) { message }
}

internal fun Cat062CodecSupport.readSystemTrackUpdateAges(buffer: ByteBuffer): SystemTrackUpdateAges =
    decodeCat062Item("I062/290", "systemTrackUpdateAges") {
        val indicator = decodeCat062Item("I062/290", "systemTrackUpdateAges.indicator") { readCompoundIndicator(buffer) }
        val mapping =
            listOf(
                1 to SystemTrackAgeType.TRACK,
                2 to SystemTrackAgeType.PSR,
                3 to SystemTrackAgeType.SSR,
                4 to SystemTrackAgeType.MDS,
                5 to SystemTrackAgeType.ADS_C,
                6 to SystemTrackAgeType.ADS_ES,
                7 to SystemTrackAgeType.VDL,
                8 to SystemTrackAgeType.UAT,
                9 to SystemTrackAgeType.LOP,
                10 to SystemTrackAgeType.MLT,
            )
        val ages = linkedMapOf<SystemTrackAgeType, Double>()
        mapping.forEach { (index, type) ->
            if (isCompoundSubfieldPresent(indicator, index)) {
                ages[type] = decodeCat062Item("I062/290", "systemTrackUpdateAges.${type.name}") { buffer.get().toUnsignedInt() * 0.25 }
            }
        }
        SystemTrackUpdateAges(ages)
    }

internal fun Cat062CodecSupport.writeSystemTrackUpdateAges(
    buffer: ByteBuffer,
    value: SystemTrackUpdateAges,
) {
    encodeCat062Item("I062/290") {
        val order =
            listOf(
                SystemTrackAgeType.TRACK,
                SystemTrackAgeType.PSR,
                SystemTrackAgeType.SSR,
                SystemTrackAgeType.MDS,
                SystemTrackAgeType.ADS_C,
                SystemTrackAgeType.ADS_ES,
                SystemTrackAgeType.VDL,
                SystemTrackAgeType.UAT,
                SystemTrackAgeType.LOP,
                SystemTrackAgeType.MLT,
            )
        val present = order.mapIndexedNotNull { index, type -> if (value.agesSeconds.containsKey(type)) index + 1 else null }.toSet()
        writeCompoundIndicator(buffer, present)
        order.forEach { type ->
            value.agesSeconds[type]?.let {
                buffer.putUnsignedByte(quantize(it, 0.25, "systemTrackUpdateAges.${type.name}"), "systemTrackUpdateAges.${type.name}")
            }
        }
    }
}

internal fun Cat062CodecSupport.readModeOfMovement(buffer: ByteBuffer): ModeOfMovement {
    val octet = buffer.get().toUnsignedInt()
    return ModeOfMovement(
        transversalAccelerationClass =
            when ((octet ushr 6) and 0x03) {
                1 -> TransversalAccelerationClass.RIGHT_TURN
                2 -> TransversalAccelerationClass.LEFT_TURN
                3 -> TransversalAccelerationClass.UNDETERMINED
                else -> TransversalAccelerationClass.CONSTANT_COURSE
            },
        longitudinalAccelerationClass =
            when ((octet ushr 4) and 0x03) {
                1 -> MovementAccelerationClass.INCREASING_GROUND_SPEED
                2 -> MovementAccelerationClass.DECREASING_GROUND_SPEED
                3 -> MovementAccelerationClass.UNDETERMINED
                else -> MovementAccelerationClass.CONSTANT_GROUND_SPEED
            },
        verticalMovementClass =
            when ((octet ushr 2) and 0x03) {
                1 -> VerticalMovementClass.CLIMB
                2 -> VerticalMovementClass.DESCENT
                3 -> VerticalMovementClass.UNDETERMINED
                else -> VerticalMovementClass.LEVEL
            },
        altitudeDiscrepancyFlag = (octet and 0x02) != 0,
    )
}

internal fun Cat062CodecSupport.writeModeOfMovement(
    buffer: ByteBuffer,
    value: ModeOfMovement,
) {
    val transCode =
        when (value.transversalAccelerationClass) {
            TransversalAccelerationClass.CONSTANT_COURSE -> 0
            TransversalAccelerationClass.RIGHT_TURN -> 1
            TransversalAccelerationClass.LEFT_TURN -> 2
            TransversalAccelerationClass.UNDETERMINED -> 3
        }
    val longCode =
        when (value.longitudinalAccelerationClass) {
            MovementAccelerationClass.CONSTANT_GROUND_SPEED -> 0
            MovementAccelerationClass.INCREASING_GROUND_SPEED -> 1
            MovementAccelerationClass.DECREASING_GROUND_SPEED -> 2
            MovementAccelerationClass.UNDETERMINED -> 3
        }
    val vertCode =
        when (value.verticalMovementClass) {
            VerticalMovementClass.LEVEL -> 0
            VerticalMovementClass.CLIMB -> 1
            VerticalMovementClass.DESCENT -> 2
            VerticalMovementClass.UNDETERMINED -> 3
        }
    val octet =
        ((transCode and 0x03) shl 6) or ((longCode and 0x03) shl 4) or ((vertCode and 0x03) shl 2) or
            (if (value.altitudeDiscrepancyFlag) 0x02 else 0)
    buffer.put(octet.toByte())
}

internal fun Cat062CodecSupport.readTrackDataAges(buffer: ByteBuffer): TrackDataAges =
    decodeCat062Item("I062/295", "trackDataAges") {
        val indicator = decodeCat062Item("I062/295", "trackDataAges.indicator") { readCompoundIndicator(buffer) }
        val ages = linkedMapOf<TrackDataAgeType, Double>()
        TrackDataAgeType.entries.forEachIndexed { index, type ->
            if (isCompoundSubfieldPresent(indicator, index + 1)) {
                ages[type] = decodeCat062Item("I062/295", "trackDataAges.${type.name}") { buffer.get().toUnsignedInt() * 0.25 }
            }
        }
        TrackDataAges(ages)
    }

internal fun Cat062CodecSupport.writeTrackDataAges(
    buffer: ByteBuffer,
    value: TrackDataAges,
) {
    encodeCat062Item("I062/295") {
        val present =
            TrackDataAgeType.entries
                .mapIndexedNotNull { index, type -> if (value.agesSeconds.containsKey(type)) index + 1 else null }
                .toSet()
        writeCompoundIndicator(buffer, present)
        TrackDataAgeType.entries.forEach { type ->
            value.agesSeconds[type]?.let {
                buffer.putUnsignedByte(
                    quantize(it, 0.25, "trackDataAges.${type.name}"),
                    "trackDataAges.${type.name}",
                )
            }
        }
    }
}
