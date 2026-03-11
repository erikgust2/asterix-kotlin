package com.erik.asterix.cat062

import java.nio.ByteBuffer
import kotlin.math.roundToInt

internal fun Cat062CodecSupport.readAircraftDerivedData(buffer: ByteBuffer): AircraftDerivedData {
    val indicator = readCompoundIndicator(buffer)
    var targetAddress: Int? = null
    var targetIdentification: String? = null
    var magneticHeadingDegrees: Double? = null
    var indicatedAirspeed: Airspeed? = null
    var trueAirspeedKnots: Int? = null
    var selectedAltitude: SelectedAltitude? = null
    var finalStateSelectedAltitude: FinalStateSelectedAltitude? = null
    var trajectoryIntentStatus: TrajectoryIntentStatus? = null
    var trajectoryIntentData: List<TrajectoryIntentPoint>? = null
    var communicationsCapabilities: CommunicationsCapabilities? = null
    var adsbStatus: AdsbStatus? = null
    var acasResolutionAdvisoryReport: AcasResolutionAdvisory? = null
    var barometricVerticalRateFeetPerMinute: Double? = null
    var geometricVerticalRateFeetPerMinute: Double? = null
    var rollAngleDegrees: Double? = null
    var trackAngleRateDegreesPerSecond: Double? = null
    var trackAngleDegrees: Double? = null
    var groundSpeedKnots: Double? = null
    var velocityUncertaintyCategory: Int? = null
    var meteorologicalData: MeteorologicalData? = null
    var emitterCategory: Int? = null
    var positionWgs84: Wgs84Position? = null
    var geometricAltitudeFeet: Double? = null
    var positionUncertaintyMeters: Double? = null
    var modeSMessages: List<ModeSMessage>? = null
    var indicatedAirspeedKnots: Int? = null
    var machNumber: Double? = null
    var barometricPressureSettingHpa: Double? = null

    if (isCompoundSubfieldPresent(indicator, 1)) targetAddress = readUnsignedInt24(buffer)
    if (isCompoundSubfieldPresent(indicator, 2)) targetIdentification = decodePackedCallsign(readBytes(buffer, 6))
    if (isCompoundSubfieldPresent(indicator, 3)) magneticHeadingDegrees = buffer.short.toUnsignedInt() * (360.0 / 65536.0)
    if (isCompoundSubfieldPresent(indicator, 4)) {
        val raw = buffer.short.toUnsignedInt()
        val mach = (raw and 0x8000) != 0
        val value = raw and 0x7FFF
        indicatedAirspeed = if (mach) {
            Airspeed(AirspeedType.MACH, value / 1000.0)
        } else {
            Airspeed(AirspeedType.INDICATED_AIRSPEED_KNOTS, value * (1.0 / 16384.0) * 3600.0)
        }
    }
    if (isCompoundSubfieldPresent(indicator, 5)) trueAirspeedKnots = buffer.short.toUnsignedInt()
    if (isCompoundSubfieldPresent(indicator, 6)) {
        val raw = buffer.short.toUnsignedInt()
        selectedAltitude = SelectedAltitude(
            sourceAvailable = (raw and 0x8000) != 0,
            sourceCode = (raw ushr 13) and 0x03,
            flightLevel = signExtend(raw and 0x1FFF, 13) * 0.25,
        )
    }
    if (isCompoundSubfieldPresent(indicator, 7)) {
        val raw = buffer.short.toUnsignedInt()
        finalStateSelectedAltitude = FinalStateSelectedAltitude(
            managedVerticalModeActive = (raw and 0x8000) != 0,
            altitudeHoldActive = (raw and 0x4000) != 0,
            approachModeActive = (raw and 0x2000) != 0,
            flightLevel = signExtend(raw and 0x1FFF, 13) * 0.25,
        )
    }
    if (isCompoundSubfieldPresent(indicator, 8)) {
        val octet = buffer.get().toUnsignedInt()
        trajectoryIntentStatus = TrajectoryIntentStatus(
            available = (octet and 0x80) != 0,
            valid = (octet and 0x40) != 0,
        )
    }
    if (isCompoundSubfieldPresent(indicator, 9)) {
        val rep = buffer.get().toUnsignedInt()
        trajectoryIntentData = List(rep) { TrajectoryIntentPoint(readBytes(buffer, 15)) }
    }
    if (isCompoundSubfieldPresent(indicator, 10)) {
        val b1 = buffer.get().toUnsignedInt()
        val b2 = buffer.get().toUnsignedInt()
        communicationsCapabilities = CommunicationsCapabilities(
            comCode = b1 ushr 5,
            statCode = (b1 ushr 2) and 0x07,
            ssc = (b1 and 0x02) != 0,
            arcCode = (b2 and 0x40) != 0,
            aic = (b2 and 0x20) != 0,
            b1a = (b2 ushr 4) and 0x01,
            b1b = b2 and 0x0F,
        )
    }
    if (isCompoundSubfieldPresent(indicator, 11)) {
        val raw = buffer.short.toUnsignedInt()
        adsbStatus = AdsbStatus(
            ac = (raw ushr 14) and 0x03,
            mn = (raw ushr 12) and 0x03,
            dc = (raw ushr 10) and 0x03,
            gbs = (raw and 0x0200) != 0,
            stat = raw and 0x07,
        )
    }
    if (isCompoundSubfieldPresent(indicator, 12)) acasResolutionAdvisoryReport = AcasResolutionAdvisory(readUnsignedInt56(buffer))
    if (isCompoundSubfieldPresent(indicator, 13)) barometricVerticalRateFeetPerMinute = signExtend(buffer.short.toUnsignedInt() and 0x7FFF, 15) * 6.25
    if (isCompoundSubfieldPresent(indicator, 14)) geometricVerticalRateFeetPerMinute = signExtend(buffer.short.toUnsignedInt() and 0x7FFF, 15) * 6.25
    if (isCompoundSubfieldPresent(indicator, 15)) rollAngleDegrees = buffer.short.toDouble() * (45.0 / 256.0)
    if (isCompoundSubfieldPresent(indicator, 16)) trackAngleRateDegreesPerSecond = buffer.short.toDouble() * (8.0 / 32.0)
    if (isCompoundSubfieldPresent(indicator, 17)) trackAngleDegrees = buffer.short.toUnsignedInt() * (360.0 / 65536.0)
    if (isCompoundSubfieldPresent(indicator, 18)) groundSpeedKnots = buffer.short.toUnsignedInt() * (1.0 / 16384.0) * 3600.0
    if (isCompoundSubfieldPresent(indicator, 19)) velocityUncertaintyCategory = buffer.get().toUnsignedInt() ushr 5
    if (isCompoundSubfieldPresent(indicator, 20)) meteorologicalData = readMeteorologicalData(buffer)
    if (isCompoundSubfieldPresent(indicator, 21)) emitterCategory = buffer.get().toUnsignedInt()
    if (isCompoundSubfieldPresent(indicator, 22)) positionWgs84 = readWgs84Position(buffer)
    if (isCompoundSubfieldPresent(indicator, 23)) geometricAltitudeFeet = buffer.short.toDouble() * 6.25
    if (isCompoundSubfieldPresent(indicator, 24)) positionUncertaintyMeters = buffer.get().toUnsignedInt().toDouble()
    if (isCompoundSubfieldPresent(indicator, 25)) {
        val rep = buffer.get().toUnsignedInt()
        modeSMessages = List(rep) {
            val payload = readBytes(buffer, 7)
            val bds = buffer.get().toUnsignedInt()
            ModeSMessage(payload, bds ushr 4, bds and 0x0F)
        }
    }
    if (isCompoundSubfieldPresent(indicator, 26)) indicatedAirspeedKnots = buffer.short.toUnsignedInt()
    if (isCompoundSubfieldPresent(indicator, 27)) machNumber = buffer.short.toUnsignedInt() * 0.008
    if (isCompoundSubfieldPresent(indicator, 28)) barometricPressureSettingHpa = (buffer.short.toUnsignedInt() and 0x0FFF) * 0.1

    return AircraftDerivedData(
        targetAddress = targetAddress,
        targetIdentification = targetIdentification,
        magneticHeadingDegrees = magneticHeadingDegrees,
        indicatedAirspeed = indicatedAirspeed,
        trueAirspeedKnots = trueAirspeedKnots,
        selectedAltitude = selectedAltitude,
        finalStateSelectedAltitude = finalStateSelectedAltitude,
        trajectoryIntentStatus = trajectoryIntentStatus,
        trajectoryIntentData = trajectoryIntentData,
        communicationsCapabilities = communicationsCapabilities,
        adsbStatus = adsbStatus,
        acasResolutionAdvisoryReport = acasResolutionAdvisoryReport,
        barometricVerticalRateFeetPerMinute = barometricVerticalRateFeetPerMinute,
        geometricVerticalRateFeetPerMinute = geometricVerticalRateFeetPerMinute,
        rollAngleDegrees = rollAngleDegrees,
        trackAngleRateDegreesPerSecond = trackAngleRateDegreesPerSecond,
        trackAngleDegrees = trackAngleDegrees,
        groundSpeedKnots = groundSpeedKnots,
        velocityUncertaintyCategory = velocityUncertaintyCategory,
        meteorologicalData = meteorologicalData,
        emitterCategory = emitterCategory,
        positionWgs84 = positionWgs84,
        geometricAltitudeFeet = geometricAltitudeFeet,
        positionUncertaintyMeters = positionUncertaintyMeters,
        modeSMessages = modeSMessages,
        indicatedAirspeedKnots = indicatedAirspeedKnots,
        machNumber = machNumber,
        barometricPressureSettingHpa = barometricPressureSettingHpa,
    )
}

internal fun Cat062CodecSupport.writeAircraftDerivedData(buffer: ByteBuffer, value: AircraftDerivedData) {
    val present = mutableSetOf<Int>()
    if (value.targetAddress != null) present += 1
    if (value.targetIdentification != null) present += 2
    if (value.magneticHeadingDegrees != null) present += 3
    if (value.indicatedAirspeed != null) present += 4
    if (value.trueAirspeedKnots != null) present += 5
    if (value.selectedAltitude != null) present += 6
    if (value.finalStateSelectedAltitude != null) present += 7
    if (value.trajectoryIntentStatus != null) present += 8
    if (value.trajectoryIntentData != null) present += 9
    if (value.communicationsCapabilities != null) present += 10
    if (value.adsbStatus != null) present += 11
    if (value.acasResolutionAdvisoryReport != null) present += 12
    if (value.barometricVerticalRateFeetPerMinute != null) present += 13
    if (value.geometricVerticalRateFeetPerMinute != null) present += 14
    if (value.rollAngleDegrees != null) present += 15
    if (value.trackAngleRateDegreesPerSecond != null) present += 16
    if (value.trackAngleDegrees != null) present += 17
    if (value.groundSpeedKnots != null) present += 18
    if (value.velocityUncertaintyCategory != null) present += 19
    if (value.meteorologicalData != null) present += 20
    if (value.emitterCategory != null) present += 21
    if (value.positionWgs84 != null) present += 22
    if (value.geometricAltitudeFeet != null) present += 23
    if (value.positionUncertaintyMeters != null) present += 24
    if (value.modeSMessages != null) present += 25
    if (value.indicatedAirspeedKnots != null) present += 26
    if (value.machNumber != null) present += 27
    if (value.barometricPressureSettingHpa != null) present += 28
    writeCompoundIndicator(buffer, present)

    value.targetAddress?.let { writeUnsignedInt24(buffer, it) }
    value.targetIdentification?.let { encodePackedCallsign(buffer, it) }
    value.magneticHeadingDegrees?.let { buffer.putUnsignedShort((it / (360.0 / 65536.0)).roundToInt(), "aircraftDerivedData.magneticHeadingDegrees") }
    value.indicatedAirspeed?.let {
        val raw = if (it.type == AirspeedType.MACH) {
            0x8000 or (it.value * 1000.0).roundToInt()
        } else {
            ((it.value / 3600.0) / (1.0 / 16384.0)).roundToInt()
        }
        buffer.putUnsignedShort(raw, "aircraftDerivedData.indicatedAirspeed")
    }
    value.trueAirspeedKnots?.let { buffer.putUnsignedShort(it, "aircraftDerivedData.trueAirspeedKnots") }
    value.selectedAltitude?.let {
        require(it.sourceCode in 0..0x03) { "aircraftDerivedData.selectedAltitude.sourceCode out of range: ${it.sourceCode}" }
        var raw = (if (it.sourceAvailable) 0x8000 else 0) or ((it.sourceCode and 0x03) shl 13)
        raw = raw or encodeSignedBits((it.flightLevel / 0.25).roundToInt(), 13, "aircraftDerivedData.selectedAltitude.flightLevel")
        buffer.putShort(raw.toShort())
    }
    value.finalStateSelectedAltitude?.let {
        var raw = 0
        if (it.managedVerticalModeActive) raw = raw or 0x8000
        if (it.altitudeHoldActive) raw = raw or 0x4000
        if (it.approachModeActive) raw = raw or 0x2000
        raw = raw or encodeSignedBits((it.flightLevel / 0.25).roundToInt(), 13, "aircraftDerivedData.finalStateSelectedAltitude.flightLevel")
        buffer.putShort(raw.toShort())
    }
    value.trajectoryIntentStatus?.let {
        var octet = 0
        if (it.available) octet = octet or 0x80
        if (it.valid) octet = octet or 0x40
        buffer.put(octet.toByte())
    }
    value.trajectoryIntentData?.let {
        buffer.putUnsignedByte(it.size, "aircraftDerivedData.trajectoryIntentData.size")
        it.forEach { point ->
            requireRawLength(point.raw, 15, "aircraftDerivedData.trajectoryIntentData.raw")
            buffer.put(point.raw.unsafeBytes())
        }
    }
    value.communicationsCapabilities?.let {
        require(it.comCode in 0..0x07) { "aircraftDerivedData.communicationsCapabilities.comCode out of range: ${it.comCode}" }
        require(it.statCode in 0..0x07) { "aircraftDerivedData.communicationsCapabilities.statCode out of range: ${it.statCode}" }
        require(it.b1a in 0..0x01) { "aircraftDerivedData.communicationsCapabilities.b1a out of range: ${it.b1a}" }
        require(it.b1b in 0..0x0F) { "aircraftDerivedData.communicationsCapabilities.b1b out of range: ${it.b1b}" }
        val b1 = ((it.comCode and 0x07) shl 5) or ((it.statCode and 0x07) shl 2) or (if (it.ssc) 0x02 else 0)
        val b2 = (if (it.arcCode) 0x40 else 0) or (if (it.aic) 0x20 else 0) or ((it.b1a and 0x01) shl 4) or (it.b1b and 0x0F)
        buffer.put(b1.toByte())
        buffer.put(b2.toByte())
    }
    value.adsbStatus?.let {
        require(it.ac in 0..0x03) { "aircraftDerivedData.adsbStatus.ac out of range: ${it.ac}" }
        require(it.mn in 0..0x03) { "aircraftDerivedData.adsbStatus.mn out of range: ${it.mn}" }
        require(it.dc in 0..0x03) { "aircraftDerivedData.adsbStatus.dc out of range: ${it.dc}" }
        require(it.stat in 0..0x07) { "aircraftDerivedData.adsbStatus.stat out of range: ${it.stat}" }
        val raw = ((it.ac and 0x03) shl 14) or ((it.mn and 0x03) shl 12) or ((it.dc and 0x03) shl 10) or
            (if (it.gbs) 0x0200 else 0) or (it.stat and 0x07)
        buffer.putUnsignedShort(raw, "aircraftDerivedData.adsbStatus")
    }
    value.acasResolutionAdvisoryReport?.let { writeUnsignedInt56(buffer, it.raw) }
    value.barometricVerticalRateFeetPerMinute?.let {
        buffer.putUnsignedShort(encodeSignedBits((it / 6.25).roundToInt(), 15, "aircraftDerivedData.barometricVerticalRateFeetPerMinute"), "aircraftDerivedData.barometricVerticalRateFeetPerMinute")
    }
    value.geometricVerticalRateFeetPerMinute?.let {
        buffer.putUnsignedShort(encodeSignedBits((it / 6.25).roundToInt(), 15, "aircraftDerivedData.geometricVerticalRateFeetPerMinute"), "aircraftDerivedData.geometricVerticalRateFeetPerMinute")
    }
    value.rollAngleDegrees?.let { buffer.putSignedShort((it / (45.0 / 256.0)).roundToInt(), "aircraftDerivedData.rollAngleDegrees") }
    value.trackAngleRateDegreesPerSecond?.let { buffer.putSignedShort((it / 0.25).roundToInt(), "aircraftDerivedData.trackAngleRateDegreesPerSecond") }
    value.trackAngleDegrees?.let { buffer.putUnsignedShort((it / (360.0 / 65536.0)).roundToInt(), "aircraftDerivedData.trackAngleDegrees") }
    value.groundSpeedKnots?.let { buffer.putUnsignedShort(((it / 3600.0) / (1.0 / 16384.0)).roundToInt(), "aircraftDerivedData.groundSpeedKnots") }
    value.velocityUncertaintyCategory?.let {
        require(it in 0..0x07) { "aircraftDerivedData.velocityUncertaintyCategory out of range: $it" }
        buffer.put((it shl 5).toByte())
    }
    value.meteorologicalData?.let { writeMeteorologicalData(buffer, it) }
    value.emitterCategory?.let { buffer.putUnsignedByte(it, "aircraftDerivedData.emitterCategory") }
    value.positionWgs84?.let { writeWgs84Position(buffer, it) }
    value.geometricAltitudeFeet?.let { buffer.putSignedShort((it / 6.25).roundToInt(), "aircraftDerivedData.geometricAltitudeFeet") }
    value.positionUncertaintyMeters?.let { buffer.putUnsignedByte(it.roundToInt(), "aircraftDerivedData.positionUncertaintyMeters") }
    value.modeSMessages?.let {
        buffer.putUnsignedByte(it.size, "aircraftDerivedData.modeSMessages.size")
        it.forEach { message ->
            requireRawLength(message.message, 7, "aircraftDerivedData.modeSMessages.message")
            require(message.bds1 in 0..0x0F) { "aircraftDerivedData.modeSMessages.bds1 out of range: ${message.bds1}" }
            require(message.bds2 in 0..0x0F) { "aircraftDerivedData.modeSMessages.bds2 out of range: ${message.bds2}" }
            buffer.put(message.message.unsafeBytes())
            buffer.put(((message.bds1 shl 4) or message.bds2).toByte())
        }
    }
    value.indicatedAirspeedKnots?.let { buffer.putUnsignedShort(it, "aircraftDerivedData.indicatedAirspeedKnots") }
    value.machNumber?.let { buffer.putUnsignedShort((it / 0.008).roundToInt(), "aircraftDerivedData.machNumber") }
    value.barometricPressureSettingHpa?.let { buffer.putUnsignedShort((it / 0.1).roundToInt(), "aircraftDerivedData.barometricPressureSettingHpa") }
}

internal fun Cat062CodecSupport.readTrackStatus(buffer: ByteBuffer): TrackStatus {
    val octet1 = buffer.get().toUnsignedInt()
    val hasOctet2 = (octet1 and 0x01) != 0
    val octet2 = if (hasOctet2) buffer.get().toUnsignedInt() else null
    val hasOctet3 = octet2 != null && (octet2 and 0x01) != 0
    val octet3 = if (hasOctet3) buffer.get().toUnsignedInt() else null
    val hasOctet4 = octet3 != null && (octet3 and 0x01) != 0
    val octet4 = if (hasOctet4) buffer.get().toUnsignedInt() else null
    val hasOctet5 = octet4 != null && (octet4 and 0x01) != 0
    val octet5 = if (hasOctet5) buffer.get().toUnsignedInt() else null

    return TrackStatus(
        mon = (octet1 and 0x80) != 0,
        spi = (octet1 and 0x40) != 0,
        mrh = (octet1 and 0x20) != 0,
        src = (octet1 ushr 2) and 0x07,
        cnf = (octet1 and 0x02) != 0,
        sim = octet2?.let { (it and 0x80) != 0 },
        tse = octet2?.let { (it and 0x40) != 0 },
        tsb = octet2?.let { (it and 0x20) != 0 },
        fpc = octet2?.let { (it and 0x10) != 0 },
        aff = octet2?.let { (it and 0x08) != 0 },
        stp = octet2?.let { (it and 0x04) != 0 },
        kos = octet2?.let { (it and 0x02) != 0 },
        ama = octet3?.let { (it and 0x80) != 0 },
        md4 = octet3?.let { (it ushr 5) and 0x03 },
        me = octet3?.let { (it and 0x10) != 0 },
        mi = octet3?.let { (it and 0x08) != 0 },
        md5 = octet3?.let { (it ushr 1) and 0x03 },
        cst = octet4?.let { (it and 0x80) != 0 },
        psr = octet4?.let { (it and 0x40) != 0 },
        ssr = octet4?.let { (it and 0x20) != 0 },
        mds = octet4?.let { (it and 0x10) != 0 },
        ads = octet4?.let { (it and 0x08) != 0 },
        suc = octet4?.let { (it and 0x04) != 0 },
        aac = octet4?.let { (it and 0x02) != 0 },
        sds = octet5?.let { (it ushr 6) and 0x03 },
        ems = octet5?.let { (it ushr 3) and 0x07 },
    )
}

internal fun Cat062CodecSupport.writeTrackStatus(buffer: ByteBuffer, value: TrackStatus) {
    val needsOctet5 = listOf(value.sds, value.ems).any { it != null }
    val needsOctet4 = needsOctet5 || listOf(value.cst, value.psr, value.ssr, value.mds, value.ads, value.suc, value.aac).any { it != null }
    val needsOctet3 = needsOctet4 || listOf(value.ama, value.md4, value.me, value.mi, value.md5).any { it != null }
    val needsOctet2 = needsOctet3 || listOf(value.sim, value.tse, value.tsb, value.fpc, value.aff, value.stp, value.kos).any { it != null }

    var octet1 = 0
    if (value.mon == true) octet1 = octet1 or 0x80
    if (value.spi == true) octet1 = octet1 or 0x40
    if (value.mrh == true) octet1 = octet1 or 0x20
    require(value.src == null || value.src in 0..0x07) { "trackStatus.src out of range: ${value.src}" }
    octet1 = octet1 or ((value.src ?: 0) shl 2)
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
        require(value.md4 == null || value.md4 in 0..0x03) { "trackStatus.md4 out of range: ${value.md4}" }
        octet3 = octet3 or (((value.md4 ?: 0) and 0x03) shl 5)
        if (value.me == true) octet3 = octet3 or 0x10
        if (value.mi == true) octet3 = octet3 or 0x08
        require(value.md5 == null || value.md5 in 0..0x03) { "trackStatus.md5 out of range: ${value.md5}" }
        octet3 = octet3 or (((value.md5 ?: 0) and 0x03) shl 1)
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
        require(value.sds == null || value.sds in 0..0x03) { "trackStatus.sds out of range: ${value.sds}" }
        require(value.ems == null || value.ems in 0..0x07) { "trackStatus.ems out of range: ${value.ems}" }
        octet5 = octet5 or (((value.sds ?: 0) and 0x03) shl 6)
        octet5 = octet5 or (((value.ems ?: 0) and 0x07) shl 3)
        buffer.put(octet5.toByte())
    }
}

internal fun Cat062CodecSupport.readSystemTrackUpdateAges(buffer: ByteBuffer): SystemTrackUpdateAges {
    val indicator = readCompoundIndicator(buffer)
    val mapping = listOf(
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
            ages[type] = buffer.get().toUnsignedInt() * 0.25
        }
    }
    return SystemTrackUpdateAges(ages)
}

internal fun Cat062CodecSupport.writeSystemTrackUpdateAges(buffer: ByteBuffer, value: SystemTrackUpdateAges) {
    val order = listOf(
        SystemTrackAgeType.TRACK, SystemTrackAgeType.PSR, SystemTrackAgeType.SSR, SystemTrackAgeType.MDS,
        SystemTrackAgeType.ADS_C, SystemTrackAgeType.ADS_ES, SystemTrackAgeType.VDL, SystemTrackAgeType.UAT,
        SystemTrackAgeType.LOP, SystemTrackAgeType.MLT,
    )
    val present = order.mapIndexedNotNull { index, type -> if (value.agesSeconds.containsKey(type)) index + 1 else null }.toSet()
    writeCompoundIndicator(buffer, present)
    order.forEach { type ->
        value.agesSeconds[type]?.let { buffer.putUnsignedByte((it / 0.25).roundToInt(), "systemTrackUpdateAges.${type.name}") }
    }
}

internal fun Cat062CodecSupport.readModeOfMovement(buffer: ByteBuffer): ModeOfMovement {
    val octet = buffer.get().toUnsignedInt()
    return ModeOfMovement(
        transversalAccelerationClass = when ((octet ushr 6) and 0x03) {
            1 -> TransversalAccelerationClass.RIGHT_TURN
            2 -> TransversalAccelerationClass.LEFT_TURN
            3 -> TransversalAccelerationClass.UNDETERMINED
            else -> TransversalAccelerationClass.CONSTANT_COURSE
        },
        longitudinalAccelerationClass = when ((octet ushr 4) and 0x03) {
            1 -> MovementAccelerationClass.INCREASING_GROUND_SPEED
            2 -> MovementAccelerationClass.DECREASING_GROUND_SPEED
            3 -> MovementAccelerationClass.UNDETERMINED
            else -> MovementAccelerationClass.CONSTANT_GROUND_SPEED
        },
        verticalMovementClass = when ((octet ushr 2) and 0x03) {
            1 -> VerticalMovementClass.CLIMB
            2 -> VerticalMovementClass.DESCENT
            3 -> VerticalMovementClass.UNDETERMINED
            else -> VerticalMovementClass.LEVEL
        },
        altitudeDiscrepancyFlag = (octet and 0x02) != 0,
    )
}

internal fun Cat062CodecSupport.writeModeOfMovement(buffer: ByteBuffer, value: ModeOfMovement) {
    val transCode = when (value.transversalAccelerationClass) {
        TransversalAccelerationClass.CONSTANT_COURSE -> 0
        TransversalAccelerationClass.RIGHT_TURN -> 1
        TransversalAccelerationClass.LEFT_TURN -> 2
        TransversalAccelerationClass.UNDETERMINED -> 3
    }
    val longCode = when (value.longitudinalAccelerationClass) {
        MovementAccelerationClass.CONSTANT_GROUND_SPEED -> 0
        MovementAccelerationClass.INCREASING_GROUND_SPEED -> 1
        MovementAccelerationClass.DECREASING_GROUND_SPEED -> 2
        MovementAccelerationClass.UNDETERMINED -> 3
    }
    val vertCode = when (value.verticalMovementClass) {
        VerticalMovementClass.LEVEL -> 0
        VerticalMovementClass.CLIMB -> 1
        VerticalMovementClass.DESCENT -> 2
        VerticalMovementClass.UNDETERMINED -> 3
    }
    val octet = ((transCode and 0x03) shl 6) or ((longCode and 0x03) shl 4) or ((vertCode and 0x03) shl 2) or
        (if (value.altitudeDiscrepancyFlag) 0x02 else 0)
    buffer.put(octet.toByte())
}

internal fun Cat062CodecSupport.readTrackDataAges(buffer: ByteBuffer): TrackDataAges {
    val indicator = readCompoundIndicator(buffer)
    val ages = linkedMapOf<TrackDataAgeType, Double>()
    TrackDataAgeType.entries.forEachIndexed { index, type ->
        if (isCompoundSubfieldPresent(indicator, index + 1)) {
            ages[type] = buffer.get().toUnsignedInt() * 0.25
        }
    }
    return TrackDataAges(ages)
}

internal fun Cat062CodecSupport.writeTrackDataAges(buffer: ByteBuffer, value: TrackDataAges) {
    val present = TrackDataAgeType.entries
        .mapIndexedNotNull { index, type -> if (value.agesSeconds.containsKey(type)) index + 1 else null }
        .toSet()
    writeCompoundIndicator(buffer, present)
    TrackDataAgeType.entries.forEach { type ->
        value.agesSeconds[type]?.let { buffer.putUnsignedByte((it / 0.25).roundToInt(), "trackDataAges.${type.name}") }
    }
}

internal fun Cat062CodecSupport.readFlightPlanRelatedData(buffer: ByteBuffer): FlightPlanRelatedData {
    val indicator = readCompoundIndicator(buffer)
    var tag: DataSourceIdentifier? = null
    var callsign: String? = null
    var ifpsFlightId: IfpsFlightId? = null
    var flightCategory: FlightCategory? = null
    var aircraftType: String? = null
    var wakeTurbulenceCategory: String? = null
    var departureAerodrome: String? = null
    var destinationAerodrome: String? = null
    var runwayDesignation: String? = null
    var currentClearedFlightLevel: Double? = null
    var currentControlPosition: ControlPosition? = null
    var timesOfDepartureArrival: List<TimeOfDepartureArrival>? = null
    var aircraftStand: String? = null
    var standStatus: StandStatus? = null
    var standardInstrumentDeparture: String? = null
    var standardInstrumentArrival: String? = null
    var preEmergencyMode3a: PreEmergencyMode3a? = null
    var preEmergencyCallsign: String? = null

    if (isCompoundSubfieldPresent(indicator, 1)) tag = readDataSourceIdentifier(buffer)
    if (isCompoundSubfieldPresent(indicator, 2)) callsign = readAscii(buffer, 7).trim()
    if (isCompoundSubfieldPresent(indicator, 3)) {
        val raw = buffer.int
        ifpsFlightId = IfpsFlightId(raw ushr 30, raw and 0x07FFFFFF)
    }
    if (isCompoundSubfieldPresent(indicator, 4)) {
        val octet = buffer.get().toUnsignedInt()
        flightCategory = FlightCategory(
            gatOatCode = octet ushr 6,
            flightRulesCode = (octet ushr 4) and 0x03,
            rvsmStatus = (octet ushr 2) and 0x03,
            hpr = (octet and 0x02) != 0,
        )
    }
    if (isCompoundSubfieldPresent(indicator, 5)) aircraftType = readAscii(buffer, 4).trim()
    if (isCompoundSubfieldPresent(indicator, 6)) wakeTurbulenceCategory = readAscii(buffer, 1)
    if (isCompoundSubfieldPresent(indicator, 7)) departureAerodrome = readAscii(buffer, 4).trim()
    if (isCompoundSubfieldPresent(indicator, 8)) destinationAerodrome = readAscii(buffer, 4).trim()
    if (isCompoundSubfieldPresent(indicator, 9)) runwayDesignation = readAscii(buffer, 3).trim()
    if (isCompoundSubfieldPresent(indicator, 10)) currentClearedFlightLevel = signExtend(buffer.short.toUnsignedInt(), 16) * 0.25
    if (isCompoundSubfieldPresent(indicator, 11)) currentControlPosition = ControlPosition(buffer.get().toUnsignedInt(), buffer.get().toUnsignedInt())
    if (isCompoundSubfieldPresent(indicator, 12)) {
        val rep = buffer.get().toUnsignedInt()
        timesOfDepartureArrival = List(rep) { TimeOfDepartureArrival(readBytes(buffer, 4)) }
    }
    if (isCompoundSubfieldPresent(indicator, 13)) aircraftStand = readAscii(buffer, 6).trim()
    if (isCompoundSubfieldPresent(indicator, 14)) {
        val octet = buffer.get().toUnsignedInt()
        standStatus = StandStatus((octet ushr 6) and 0x03, (octet ushr 4) and 0x03)
    }
    if (isCompoundSubfieldPresent(indicator, 15)) standardInstrumentDeparture = readAscii(buffer, 7).trim()
    if (isCompoundSubfieldPresent(indicator, 16)) standardInstrumentArrival = readAscii(buffer, 7).trim()
    if (isCompoundSubfieldPresent(indicator, 17)) {
        val raw = buffer.short.toUnsignedInt()
        preEmergencyMode3a = PreEmergencyMode3a(valid = (raw and 0x1000) != 0, code = raw and 0x0FFF)
    }
    if (isCompoundSubfieldPresent(indicator, 18)) preEmergencyCallsign = readAscii(buffer, 7).trim()

    return FlightPlanRelatedData(
        tag = tag,
        callsign = callsign,
        ifpsFlightId = ifpsFlightId,
        flightCategory = flightCategory,
        aircraftType = aircraftType,
        wakeTurbulenceCategory = wakeTurbulenceCategory,
        departureAerodrome = departureAerodrome,
        destinationAerodrome = destinationAerodrome,
        runwayDesignation = runwayDesignation,
        currentClearedFlightLevel = currentClearedFlightLevel,
        currentControlPosition = currentControlPosition,
        timesOfDepartureArrival = timesOfDepartureArrival,
        aircraftStand = aircraftStand,
        standStatus = standStatus,
        standardInstrumentDeparture = standardInstrumentDeparture,
        standardInstrumentArrival = standardInstrumentArrival,
        preEmergencyMode3a = preEmergencyMode3a,
        preEmergencyCallsign = preEmergencyCallsign,
    )
}

internal fun Cat062CodecSupport.writeFlightPlanRelatedData(buffer: ByteBuffer, value: FlightPlanRelatedData) {
    val present = mutableSetOf<Int>()
    if (value.tag != null) present += 1
    if (value.callsign != null) present += 2
    if (value.ifpsFlightId != null) present += 3
    if (value.flightCategory != null) present += 4
    if (value.aircraftType != null) present += 5
    if (value.wakeTurbulenceCategory != null) present += 6
    if (value.departureAerodrome != null) present += 7
    if (value.destinationAerodrome != null) present += 8
    if (value.runwayDesignation != null) present += 9
    if (value.currentClearedFlightLevel != null) present += 10
    if (value.currentControlPosition != null) present += 11
    if (value.timesOfDepartureArrival != null) present += 12
    if (value.aircraftStand != null) present += 13
    if (value.standStatus != null) present += 14
    if (value.standardInstrumentDeparture != null) present += 15
    if (value.standardInstrumentArrival != null) present += 16
    if (value.preEmergencyMode3a != null) present += 17
    if (value.preEmergencyCallsign != null) present += 18
    writeCompoundIndicator(buffer, present)

    value.tag?.let { writeDataSourceIdentifier(buffer, it) }
    value.callsign?.let { writeAscii(buffer, it, 7) }
    value.ifpsFlightId?.let {
        require(it.typeCode in 0..0x03) { "flightPlanRelatedData.ifpsFlightId.typeCode out of range: ${it.typeCode}" }
        require(it.number in 0..0x07FFFFFF) { "flightPlanRelatedData.ifpsFlightId.number out of range: ${it.number}" }
        buffer.putInt(((it.typeCode and 0x03) shl 30) or (it.number and 0x07FFFFFF))
    }
    value.flightCategory?.let {
        require(it.gatOatCode in 0..0x03) { "flightPlanRelatedData.flightCategory.gatOatCode out of range: ${it.gatOatCode}" }
        require(it.flightRulesCode in 0..0x03) { "flightPlanRelatedData.flightCategory.flightRulesCode out of range: ${it.flightRulesCode}" }
        require(it.rvsmStatus in 0..0x03) { "flightPlanRelatedData.flightCategory.rvsmStatus out of range: ${it.rvsmStatus}" }
        val octet = ((it.gatOatCode and 0x03) shl 6) or ((it.flightRulesCode and 0x03) shl 4) or
            ((it.rvsmStatus and 0x03) shl 2) or (if (it.hpr) 0x02 else 0)
        buffer.put(octet.toByte())
    }
    value.aircraftType?.let { writeAscii(buffer, it, 4) }
    value.wakeTurbulenceCategory?.let { writeAscii(buffer, it, 1) }
    value.departureAerodrome?.let { writeAscii(buffer, it, 4) }
    value.destinationAerodrome?.let { writeAscii(buffer, it, 4) }
    value.runwayDesignation?.let { writeAscii(buffer, it, 3) }
    value.currentClearedFlightLevel?.let { buffer.putSignedShort((it / 0.25).roundToInt(), "flightPlanRelatedData.currentClearedFlightLevel") }
    value.currentControlPosition?.let {
        buffer.putUnsignedByte(it.centre, "flightPlanRelatedData.currentControlPosition.centre")
        buffer.putUnsignedByte(it.position, "flightPlanRelatedData.currentControlPosition.position")
    }
    value.timesOfDepartureArrival?.let {
        buffer.putUnsignedByte(it.size, "flightPlanRelatedData.timesOfDepartureArrival.size")
        it.forEach { entry ->
            requireRawLength(entry.raw, 4, "flightPlanRelatedData.timesOfDepartureArrival.raw")
            buffer.put(entry.raw.unsafeBytes())
        }
    }
    value.aircraftStand?.let { writeAscii(buffer, it, 6) }
    value.standStatus?.let {
        require(it.emp in 0..0x03) { "flightPlanRelatedData.standStatus.emp out of range: ${it.emp}" }
        require(it.avl in 0..0x03) { "flightPlanRelatedData.standStatus.avl out of range: ${it.avl}" }
        buffer.put((((it.emp and 0x03) shl 6) or ((it.avl and 0x03) shl 4)).toByte())
    }
    value.standardInstrumentDeparture?.let { writeAscii(buffer, it, 7) }
    value.standardInstrumentArrival?.let { writeAscii(buffer, it, 7) }
    value.preEmergencyMode3a?.let {
        require(it.code in 0..0x0FFF) { "flightPlanRelatedData.preEmergencyMode3a.code out of range: ${it.code}" }
        val raw = (if (it.valid) 0x1000 else 0) or (it.code and 0x0FFF)
        buffer.putUnsignedShort(raw, "flightPlanRelatedData.preEmergencyMode3a")
    }
    value.preEmergencyCallsign?.let { writeAscii(buffer, it, 7) }
}

internal fun Cat062CodecSupport.readMode5DataReports(buffer: ByteBuffer): Mode5DataReports {
    val indicator = readCompoundIndicator(buffer)
    var summary: Mode5Summary? = null
    var pinNationalOriginMission: PinNationalOriginMission? = null
    var positionWgs84: Wgs84Position? = null
    var geometricAltitudeFeet: Double? = null
    var extendedMode1Code: ExtendedMode1Code? = null
    var timeOffsetSeconds: Double? = null
    var xPulsePresence: Mode5XPulsePresence? = null

    if (isCompoundSubfieldPresent(indicator, 1)) {
        val octet = buffer.get().toUnsignedInt()
        summary = Mode5Summary(
            m5 = (octet and 0x80) != 0,
            id = (octet and 0x40) != 0,
            da = (octet and 0x20) != 0,
            m1 = (octet and 0x10) != 0,
            m2 = (octet and 0x08) != 0,
            m3 = (octet and 0x04) != 0,
            mc = (octet and 0x02) != 0,
            x = (octet and 0x01) != 0,
        )
    }
    if (isCompoundSubfieldPresent(indicator, 2)) {
        val raw = buffer.int
        pinNationalOriginMission = PinNationalOriginMission(
            pin = (raw ushr 17) and 0x3FFF,
            nationalOrigin = (raw ushr 9) and 0x1F,
            missionCode = raw and 0x3F,
        )
    }
    if (isCompoundSubfieldPresent(indicator, 3)) positionWgs84 = readWgs84Position(buffer)
    if (isCompoundSubfieldPresent(indicator, 4)) geometricAltitudeFeet = buffer.short.toDouble() * 25.0
    if (isCompoundSubfieldPresent(indicator, 5)) extendedMode1Code = ExtendedMode1Code(buffer.short.toUnsignedInt() and 0x0FFF)
    if (isCompoundSubfieldPresent(indicator, 6)) timeOffsetSeconds = buffer.get().toDouble() / 128.0
    if (isCompoundSubfieldPresent(indicator, 7)) {
        val octet = buffer.get().toUnsignedInt()
        xPulsePresence = Mode5XPulsePresence(
            x5 = (octet and 0x10) != 0,
            xc = (octet and 0x08) != 0,
            x3 = (octet and 0x04) != 0,
            x2 = (octet and 0x02) != 0,
            x1 = (octet and 0x01) != 0,
        )
    }

    return Mode5DataReports(
        summary = summary,
        pinNationalOriginMission = pinNationalOriginMission,
        positionWgs84 = positionWgs84,
        geometricAltitudeFeet = geometricAltitudeFeet,
        extendedMode1Code = extendedMode1Code,
        timeOffsetSeconds = timeOffsetSeconds,
        xPulsePresence = xPulsePresence,
    )
}

internal fun Cat062CodecSupport.writeMode5DataReports(buffer: ByteBuffer, value: Mode5DataReports) {
    val present = mutableSetOf<Int>()
    if (value.summary != null) present += 1
    if (value.pinNationalOriginMission != null) present += 2
    if (value.positionWgs84 != null) present += 3
    if (value.geometricAltitudeFeet != null) present += 4
    if (value.extendedMode1Code != null) present += 5
    if (value.timeOffsetSeconds != null) present += 6
    if (value.xPulsePresence != null) present += 7
    writeCompoundIndicator(buffer, present)

    value.summary?.let {
        val octet = (if (it.m5) 0x80 else 0) or (if (it.id) 0x40 else 0) or (if (it.da) 0x20 else 0) or
            (if (it.m1) 0x10 else 0) or (if (it.m2) 0x08 else 0) or (if (it.m3) 0x04 else 0) or
            (if (it.mc) 0x02 else 0) or (if (it.x) 0x01 else 0)
        buffer.put(octet.toByte())
    }
    value.pinNationalOriginMission?.let {
        require(it.pin in 0..0x3FFF) { "mode5DataReports.pinNationalOriginMission.pin out of range: ${it.pin}" }
        require(it.nationalOrigin in 0..0x1F) { "mode5DataReports.pinNationalOriginMission.nationalOrigin out of range: ${it.nationalOrigin}" }
        require(it.missionCode in 0..0x3F) { "mode5DataReports.pinNationalOriginMission.missionCode out of range: ${it.missionCode}" }
        val raw = ((it.pin and 0x3FFF) shl 17) or ((it.nationalOrigin and 0x1F) shl 9) or (it.missionCode and 0x3F)
        buffer.putInt(raw)
    }
    value.positionWgs84?.let { writeWgs84Position(buffer, it) }
    value.geometricAltitudeFeet?.let { buffer.putSignedShort((it / 25.0).roundToInt(), "mode5DataReports.geometricAltitudeFeet") }
    value.extendedMode1Code?.let {
        require(it.code in 0..0x0FFF) { "mode5DataReports.extendedMode1Code.code out of range: ${it.code}" }
        buffer.putUnsignedShort(it.code, "mode5DataReports.extendedMode1Code")
    }
    value.timeOffsetSeconds?.let { buffer.putSignedByte((it * 128.0).roundToInt(), "mode5DataReports.timeOffsetSeconds") }
    value.xPulsePresence?.let {
        val octet = (if (it.x5) 0x10 else 0) or (if (it.xc) 0x08 else 0) or (if (it.x3) 0x04 else 0) or
            (if (it.x2) 0x02 else 0) or (if (it.x1) 0x01 else 0)
        buffer.put(octet.toByte())
    }
}

internal fun Cat062CodecSupport.readEstimatedAccuracies(buffer: ByteBuffer): EstimatedAccuracies {
    val indicator = readCompoundIndicator(buffer)
    var positionCartesian: CartesianAccuracy? = null
    var xyCovarianceMeters: Double? = null
    var positionWgs84: Wgs84Accuracy? = null
    var geometricAltitudeFeet: Double? = null
    var barometricAltitudeFeet: Double? = null
    var trackVelocity: CartesianVelocity? = null
    var trackAcceleration: CartesianAcceleration? = null
    var rateOfClimbDescentFeetPerMinute: Double? = null

    if (isCompoundSubfieldPresent(indicator, 1)) {
        positionCartesian = CartesianAccuracy(
            xMeters = buffer.short.toUnsignedInt() * 0.5,
            yMeters = buffer.short.toUnsignedInt() * 0.5,
        )
    }
    if (isCompoundSubfieldPresent(indicator, 2)) xyCovarianceMeters = buffer.short.toDouble() * 0.5
    if (isCompoundSubfieldPresent(indicator, 3)) {
        positionWgs84 = Wgs84Accuracy(
            latitudeDegrees = buffer.short.toUnsignedInt() * WGS84_RESOLUTION,
            longitudeDegrees = buffer.short.toUnsignedInt() * WGS84_RESOLUTION,
        )
    }
    if (isCompoundSubfieldPresent(indicator, 4)) geometricAltitudeFeet = buffer.get().toUnsignedInt() * 6.25
    if (isCompoundSubfieldPresent(indicator, 5)) barometricAltitudeFeet = buffer.get().toUnsignedInt() * 25.0
    if (isCompoundSubfieldPresent(indicator, 6)) {
        trackVelocity = CartesianVelocity(
            xMetersPerSecond = buffer.get().toUnsignedInt() * 0.25,
            yMetersPerSecond = buffer.get().toUnsignedInt() * 0.25,
        )
    }
    if (isCompoundSubfieldPresent(indicator, 7)) {
        trackAcceleration = CartesianAcceleration(
            xMetersPerSecondSquared = buffer.get().toUnsignedInt() * 0.25,
            yMetersPerSecondSquared = buffer.get().toUnsignedInt() * 0.25,
        )
    }
    if (isCompoundSubfieldPresent(indicator, 8)) rateOfClimbDescentFeetPerMinute = buffer.get().toUnsignedInt() * 6.25

    return EstimatedAccuracies(
        positionCartesian = positionCartesian,
        xyCovarianceMeters = xyCovarianceMeters,
        positionWgs84 = positionWgs84,
        geometricAltitudeFeet = geometricAltitudeFeet,
        barometricAltitudeFeet = barometricAltitudeFeet,
        trackVelocity = trackVelocity,
        trackAcceleration = trackAcceleration,
        rateOfClimbDescentFeetPerMinute = rateOfClimbDescentFeetPerMinute,
    )
}

internal fun Cat062CodecSupport.writeEstimatedAccuracies(buffer: ByteBuffer, value: EstimatedAccuracies) {
    val present = mutableSetOf<Int>()
    if (value.positionCartesian != null) present += 1
    if (value.xyCovarianceMeters != null) present += 2
    if (value.positionWgs84 != null) present += 3
    if (value.geometricAltitudeFeet != null) present += 4
    if (value.barometricAltitudeFeet != null) present += 5
    if (value.trackVelocity != null) present += 6
    if (value.trackAcceleration != null) present += 7
    if (value.rateOfClimbDescentFeetPerMinute != null) present += 8
    writeCompoundIndicator(buffer, present)

    value.positionCartesian?.let {
        buffer.putUnsignedShort((it.xMeters / 0.5).roundToInt(), "estimatedAccuracies.positionCartesian.xMeters")
        buffer.putUnsignedShort((it.yMeters / 0.5).roundToInt(), "estimatedAccuracies.positionCartesian.yMeters")
    }
    value.xyCovarianceMeters?.let { buffer.putSignedShort((it / 0.5).roundToInt(), "estimatedAccuracies.xyCovarianceMeters") }
    value.positionWgs84?.let {
        buffer.putUnsignedShort((it.latitudeDegrees / WGS84_RESOLUTION).roundToInt(), "estimatedAccuracies.positionWgs84.latitudeDegrees")
        buffer.putUnsignedShort((it.longitudeDegrees / WGS84_RESOLUTION).roundToInt(), "estimatedAccuracies.positionWgs84.longitudeDegrees")
    }
    value.geometricAltitudeFeet?.let { buffer.putUnsignedByte((it / 6.25).roundToInt(), "estimatedAccuracies.geometricAltitudeFeet") }
    value.barometricAltitudeFeet?.let { buffer.putUnsignedByte((it / 25.0).roundToInt(), "estimatedAccuracies.barometricAltitudeFeet") }
    value.trackVelocity?.let {
        buffer.putUnsignedByte((it.xMetersPerSecond / 0.25).roundToInt(), "estimatedAccuracies.trackVelocity.xMetersPerSecond")
        buffer.putUnsignedByte((it.yMetersPerSecond / 0.25).roundToInt(), "estimatedAccuracies.trackVelocity.yMetersPerSecond")
    }
    value.trackAcceleration?.let {
        buffer.putUnsignedByte((it.xMetersPerSecondSquared / 0.25).roundToInt(), "estimatedAccuracies.trackAcceleration.xMetersPerSecondSquared")
        buffer.putUnsignedByte((it.yMetersPerSecondSquared / 0.25).roundToInt(), "estimatedAccuracies.trackAcceleration.yMetersPerSecondSquared")
    }
    value.rateOfClimbDescentFeetPerMinute?.let { buffer.putUnsignedByte((it / 6.25).roundToInt(), "estimatedAccuracies.rateOfClimbDescentFeetPerMinute") }
}

internal fun Cat062CodecSupport.readMeasuredInformation(buffer: ByteBuffer): MeasuredInformation {
    val indicator = readCompoundIndicator(buffer)
    var sensorIdentification: DataSourceIdentifier? = null
    var position: PolarPosition? = null
    var heightFeet: Double? = null
    var lastMeasuredModeCCode: MeasuredModeCCode? = null
    var lastMeasuredMode3aCode: MeasuredMode3ACode? = null
    var reportType: ReportType? = null

    if (isCompoundSubfieldPresent(indicator, 1)) sensorIdentification = readDataSourceIdentifier(buffer)
    if (isCompoundSubfieldPresent(indicator, 2)) {
        position = PolarPosition(
            rangeNm = buffer.short.toUnsignedInt() * (1.0 / 256.0),
            azimuthDegrees = buffer.short.toUnsignedInt() * (360.0 / 65536.0),
        )
    }
    if (isCompoundSubfieldPresent(indicator, 3)) heightFeet = buffer.short.toDouble() * 25.0
    if (isCompoundSubfieldPresent(indicator, 4)) {
        val raw = buffer.short.toUnsignedInt()
        lastMeasuredModeCCode = MeasuredModeCCode(
            validated = (raw and 0x8000) == 0,
            garbled = (raw and 0x4000) != 0,
            flightLevel = signExtend(raw and 0x3FFF, 14) * 0.25,
        )
    }
    if (isCompoundSubfieldPresent(indicator, 5)) {
        val raw = buffer.short.toUnsignedInt()
        lastMeasuredMode3aCode = MeasuredMode3ACode(
            code = raw and 0x0FFF,
            validated = (raw and 0x8000) == 0,
            garbled = (raw and 0x4000) != 0,
            smoothed = (raw and 0x2000) != 0,
        )
    }
    if (isCompoundSubfieldPresent(indicator, 6)) {
        val octet = buffer.get().toUnsignedInt()
        reportType = ReportType(
            typ = octet ushr 5,
            simulated = (octet and 0x10) != 0,
            rab = (octet and 0x08) != 0,
            testTarget = (octet and 0x04) != 0,
        )
    }

    return MeasuredInformation(
        sensorIdentification = sensorIdentification,
        position = position,
        heightFeet = heightFeet,
        lastMeasuredModeCCode = lastMeasuredModeCCode,
        lastMeasuredMode3aCode = lastMeasuredMode3aCode,
        reportType = reportType,
    )
}

internal fun Cat062CodecSupport.writeMeasuredInformation(buffer: ByteBuffer, value: MeasuredInformation) {
    val present = mutableSetOf<Int>()
    if (value.sensorIdentification != null) present += 1
    if (value.position != null) present += 2
    if (value.heightFeet != null) present += 3
    if (value.lastMeasuredModeCCode != null) present += 4
    if (value.lastMeasuredMode3aCode != null) present += 5
    if (value.reportType != null) present += 6
    writeCompoundIndicator(buffer, present)

    value.sensorIdentification?.let { writeDataSourceIdentifier(buffer, it) }
    value.position?.let {
        buffer.putUnsignedShort((it.rangeNm / (1.0 / 256.0)).roundToInt(), "measuredInformation.position.rangeNm")
        buffer.putUnsignedShort((it.azimuthDegrees / (360.0 / 65536.0)).roundToInt(), "measuredInformation.position.azimuthDegrees")
    }
    value.heightFeet?.let { buffer.putSignedShort((it / 25.0).roundToInt(), "measuredInformation.heightFeet") }
    value.lastMeasuredModeCCode?.let {
        var raw = encodeSignedBits((it.flightLevel / 0.25).roundToInt(), 14, "measuredInformation.lastMeasuredModeCCode.flightLevel")
        if (!it.validated) raw = raw or 0x8000
        if (it.garbled) raw = raw or 0x4000
        buffer.putShort(raw.toShort())
    }
    value.lastMeasuredMode3aCode?.let {
        require(it.code in 0..0x0FFF) { "measuredInformation.lastMeasuredMode3aCode.code out of range: ${it.code}" }
        var raw = it.code and 0x0FFF
        if (!it.validated) raw = raw or 0x8000
        if (it.garbled) raw = raw or 0x4000
        if (it.smoothed) raw = raw or 0x2000
        buffer.putUnsignedShort(raw, "measuredInformation.lastMeasuredMode3aCode")
    }
    value.reportType?.let {
        require(it.typ in 0..0x07) { "measuredInformation.detectedTargetType.typ out of range: ${it.typ}" }
        val octet = ((it.typ and 0x07) shl 5) or (if (it.simulated) 0x10 else 0) or
            (if (it.rab) 0x08 else 0) or (if (it.testTarget) 0x04 else 0)
        buffer.put(octet.toByte())
    }
}

internal fun Cat062CodecSupport.readMeteorologicalData(buffer: ByteBuffer): MeteorologicalData {
    val indicator = readCompoundIndicator(buffer)
    var windSpeedKnots: Int? = null
    var windDirectionDegrees: Double? = null
    var temperatureCelsius: Double? = null
    var turbulenceCode: Int? = null
    if (isCompoundSubfieldPresent(indicator, 1)) windSpeedKnots = buffer.short.toUnsignedInt()
    if (isCompoundSubfieldPresent(indicator, 2)) windDirectionDegrees = buffer.short.toUnsignedInt() * (360.0 / 65536.0)
    if (isCompoundSubfieldPresent(indicator, 3)) temperatureCelsius = buffer.short.toDouble() * 0.25
    if (isCompoundSubfieldPresent(indicator, 4)) turbulenceCode = buffer.get().toUnsignedInt()
    return MeteorologicalData(windSpeedKnots, windDirectionDegrees, temperatureCelsius, turbulenceCode)
}

internal fun Cat062CodecSupport.writeMeteorologicalData(buffer: ByteBuffer, value: MeteorologicalData) {
    val present = mutableSetOf<Int>()
    if (value.windSpeedKnots != null) present += 1
    if (value.windDirectionDegrees != null) present += 2
    if (value.temperatureCelsius != null) present += 3
    if (value.turbulenceCode != null) present += 4
    writeCompoundIndicator(buffer, present)
    value.windSpeedKnots?.let { buffer.putUnsignedShort(it, "meteorologicalData.windSpeedKnots") }
    value.windDirectionDegrees?.let { buffer.putUnsignedShort((it / (360.0 / 65536.0)).roundToInt(), "meteorologicalData.windDirectionDegrees") }
    value.temperatureCelsius?.let { buffer.putSignedShort((it / 0.25).roundToInt(), "meteorologicalData.temperatureCelsius") }
    value.turbulenceCode?.let { buffer.putUnsignedByte(it, "meteorologicalData.turbulenceCode") }
}
