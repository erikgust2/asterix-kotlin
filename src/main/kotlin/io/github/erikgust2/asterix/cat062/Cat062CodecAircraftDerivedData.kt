package io.github.erikgust2.asterix.cat062

import java.nio.ByteBuffer
import kotlin.math.abs

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
    var positionUncertaintyCode: Int? = null
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
        indicatedAirspeed =
            if (mach) {
                Airspeed(AirspeedType.MACH, value / 1000.0)
            } else {
                Airspeed(AirspeedType.INDICATED_AIRSPEED_KNOTS, value * (1.0 / 16384.0) * 3600.0)
            }
    }
    if (isCompoundSubfieldPresent(indicator, 5)) trueAirspeedKnots = buffer.short.toUnsignedInt()
    if (isCompoundSubfieldPresent(indicator, 6)) {
        val raw = buffer.short.toUnsignedInt()
        selectedAltitude =
            SelectedAltitude(
                sourceAvailable = (raw and 0x8000) != 0,
                sourceCode = (raw ushr 13) and 0x03,
                flightLevel = signExtend(raw and 0x1FFF, 13) * 0.25,
            )
    }
    if (isCompoundSubfieldPresent(indicator, 7)) {
        val raw = buffer.short.toUnsignedInt()
        finalStateSelectedAltitude =
            FinalStateSelectedAltitude(
                managedVerticalModeActive = (raw and 0x8000) != 0,
                altitudeHoldActive = (raw and 0x4000) != 0,
                approachModeActive = (raw and 0x2000) != 0,
                flightLevel = signExtend(raw and 0x1FFF, 13) * 0.25,
            )
    }
    if (isCompoundSubfieldPresent(indicator, 8)) {
        val octet = buffer.get().toUnsignedInt()
        trajectoryIntentStatus =
            TrajectoryIntentStatus(
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
        communicationsCapabilities =
            CommunicationsCapabilities(
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
        adsbStatus =
            AdsbStatus(
                ac = (raw ushr 14) and 0x03,
                mn = (raw ushr 12) and 0x03,
                dc = (raw ushr 10) and 0x03,
                gbs = (raw and 0x0200) != 0,
                stat = raw and 0x07,
            )
    }
    if (isCompoundSubfieldPresent(indicator, 12)) acasResolutionAdvisoryReport = AcasResolutionAdvisory(readUnsignedInt56(buffer))
    if (isCompoundSubfieldPresent(indicator, 13)) {
        barometricVerticalRateFeetPerMinute =
            signExtend(buffer.short.toUnsignedInt() and 0x7FFF, 15) * 6.25
    }
    if (isCompoundSubfieldPresent(indicator, 14)) {
        geometricVerticalRateFeetPerMinute =
            signExtend(buffer.short.toUnsignedInt() and 0x7FFF, 15) * 6.25
    }
    if (isCompoundSubfieldPresent(indicator, 15)) rollAngleDegrees = buffer.short.toDouble() * 0.01
    if (isCompoundSubfieldPresent(indicator, 16)) {
        val raw = buffer.short.toUnsignedInt()
        val turnIndicator = (raw ushr 14) and 0x03
        val encodedRate = signExtend((raw ushr 1) and 0x7F, 7) * 0.25
        trackAngleRateDegreesPerSecond =
            when {
                turnIndicator == 1 && encodedRate > 0.0 -> -encodedRate
                turnIndicator == 2 && encodedRate < 0.0 -> abs(encodedRate)
                else -> encodedRate
            }
    }
    if (isCompoundSubfieldPresent(indicator, 17)) trackAngleDegrees = buffer.short.toUnsignedInt() * (360.0 / 65536.0)
    if (isCompoundSubfieldPresent(indicator, 18)) groundSpeedKnots = buffer.short.toUnsignedInt() * (1.0 / 16384.0) * 3600.0
    if (isCompoundSubfieldPresent(indicator, 19)) velocityUncertaintyCategory = buffer.get().toUnsignedInt() ushr 5
    if (isCompoundSubfieldPresent(indicator, 20)) meteorologicalData = readMeteorologicalData(buffer)
    if (isCompoundSubfieldPresent(indicator, 21)) emitterCategory = buffer.get().toUnsignedInt()
    if (isCompoundSubfieldPresent(indicator, 22)) positionWgs84 = readWgs84Position(buffer)
    if (isCompoundSubfieldPresent(indicator, 23)) geometricAltitudeFeet = buffer.short.toDouble() * 6.25
    if (isCompoundSubfieldPresent(indicator, 24)) positionUncertaintyCode = buffer.get().toUnsignedInt() and 0x0F
    if (isCompoundSubfieldPresent(indicator, 25)) {
        val rep = buffer.get().toUnsignedInt()
        modeSMessages =
            List(rep) {
                val payload = readBytes(buffer, 7)
                val bds = buffer.get().toUnsignedInt()
                ModeSMessage(payload, bds ushr 4, bds and 0x0F)
            }
    }
    if (isCompoundSubfieldPresent(indicator, 26)) indicatedAirspeedKnots = buffer.short.toUnsignedInt()
    if (isCompoundSubfieldPresent(indicator, 27)) machNumber = buffer.short.toUnsignedInt() * 0.008
    if (isCompoundSubfieldPresent(indicator, 28)) barometricPressureSettingHpa = 800.0 + ((buffer.short.toUnsignedInt() and 0x0FFF) * 0.1)

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
        positionUncertaintyCode = positionUncertaintyCode,
        modeSMessages = modeSMessages,
        indicatedAirspeedKnots = indicatedAirspeedKnots,
        machNumber = machNumber,
        barometricPressureSettingHpa = barometricPressureSettingHpa,
    )
}

internal fun Cat062CodecSupport.writeAircraftDerivedData(
    buffer: ByteBuffer,
    value: AircraftDerivedData,
) {
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
    if (value.positionUncertaintyCode != null) present += 24
    if (value.modeSMessages != null) present += 25
    if (value.indicatedAirspeedKnots != null) present += 26
    if (value.machNumber != null) present += 27
    if (value.barometricPressureSettingHpa != null) present += 28
    writeCompoundIndicator(buffer, present)

    value.targetAddress?.let { writeUnsignedInt24(buffer, it, "aircraftDerivedData.targetAddress") }
    value.targetIdentification?.let { encodePackedCallsign(buffer, it) }
    value.magneticHeadingDegrees?.let {
        buffer.putUnsignedShort(
            quantize(it, 360.0 / 65536.0, "aircraftDerivedData.magneticHeadingDegrees"),
            "aircraftDerivedData.magneticHeadingDegrees",
        )
    }
    value.indicatedAirspeed?.let {
        val raw =
            if (it.type == AirspeedType.MACH) {
                0x8000 or
                    encodeUnsignedBits(
                        quantize(it.value, 0.001, "aircraftDerivedData.indicatedAirspeed"),
                        15,
                        "aircraftDerivedData.indicatedAirspeed",
                    )
            } else {
                encodeUnsignedBits(
                    quantize(it.value, 3600.0 / 16384.0, "aircraftDerivedData.indicatedAirspeed"),
                    15,
                    "aircraftDerivedData.indicatedAirspeed",
                )
            }
        buffer.putUnsignedShort(raw, "aircraftDerivedData.indicatedAirspeed")
    }
    value.trueAirspeedKnots?.let {
        require(it in 0..2046) { "aircraftDerivedData.trueAirspeedKnots out of range: $it" }
        buffer.putUnsignedShort(it, "aircraftDerivedData.trueAirspeedKnots")
    }
    value.selectedAltitude?.let {
        require(it.sourceCode in 0..0x03) { "aircraftDerivedData.selectedAltitude.sourceCode out of range: ${it.sourceCode}" }
        require(it.flightLevel in -13.0..1000.0) { "aircraftDerivedData.selectedAltitude.flightLevel out of range: ${it.flightLevel}" }
        var raw = (if (it.sourceAvailable) 0x8000 else 0) or ((it.sourceCode and 0x03) shl 13)
        raw =
            raw or
            encodeSignedBits(
                quantize(it.flightLevel, 0.25, "aircraftDerivedData.selectedAltitude.flightLevel"),
                13,
                "aircraftDerivedData.selectedAltitude.flightLevel",
            )
        buffer.putShort(raw.toShort())
    }
    value.finalStateSelectedAltitude?.let {
        require(
            it.flightLevel in -13.0..1000.0,
        ) { "aircraftDerivedData.finalStateSelectedAltitude.flightLevel out of range: ${it.flightLevel}" }
        var raw = 0
        if (it.managedVerticalModeActive) raw = raw or 0x8000
        if (it.altitudeHoldActive) raw = raw or 0x4000
        if (it.approachModeActive) raw = raw or 0x2000
        raw =
            raw or
            encodeSignedBits(
                quantize(it.flightLevel, 0.25, "aircraftDerivedData.finalStateSelectedAltitude.flightLevel"),
                13,
                "aircraftDerivedData.finalStateSelectedAltitude.flightLevel",
            )
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
        val raw =
            ((it.ac and 0x03) shl 14) or ((it.mn and 0x03) shl 12) or ((it.dc and 0x03) shl 10) or
                (if (it.gbs) 0x0200 else 0) or (it.stat and 0x07)
        buffer.putUnsignedShort(raw, "aircraftDerivedData.adsbStatus")
    }
    value.acasResolutionAdvisoryReport?.let { writeUnsignedInt56(buffer, it.raw, "aircraftDerivedData.acasResolutionAdvisoryReport") }
    value.barometricVerticalRateFeetPerMinute?.let {
        buffer.putUnsignedShort(
            encodeSignedBits(
                quantize(it, 6.25, "aircraftDerivedData.barometricVerticalRateFeetPerMinute"),
                15,
                "aircraftDerivedData.barometricVerticalRateFeetPerMinute",
            ),
            "aircraftDerivedData.barometricVerticalRateFeetPerMinute",
        )
    }
    value.geometricVerticalRateFeetPerMinute?.let {
        buffer.putUnsignedShort(
            encodeSignedBits(
                quantize(it, 6.25, "aircraftDerivedData.geometricVerticalRateFeetPerMinute"),
                15,
                "aircraftDerivedData.geometricVerticalRateFeetPerMinute",
            ),
            "aircraftDerivedData.geometricVerticalRateFeetPerMinute",
        )
    }
    value.rollAngleDegrees?.let {
        require(it in -180.0..180.0) { "aircraftDerivedData.rollAngleDegrees out of range: $it" }
        buffer.putSignedShort(quantize(it, 0.01, "aircraftDerivedData.rollAngleDegrees"), "aircraftDerivedData.rollAngleDegrees")
    }
    value.trackAngleRateDegreesPerSecond?.let {
        require(it in -15.0..15.0) { "aircraftDerivedData.trackAngleRateDegreesPerSecond out of range: $it" }
        val quantizedRate = quantize(it, 0.25, "aircraftDerivedData.trackAngleRateDegreesPerSecond")
        val turnIndicator =
            when {
                quantizedRate < 0 -> 0x4000
                quantizedRate > 0 -> 0x8000
                else -> 0xC000
            }
        val encodedRate = encodeSignedBits(quantizedRate, 7, "aircraftDerivedData.trackAngleRateDegreesPerSecond") shl 1
        buffer.putUnsignedShort(turnIndicator or encodedRate, "aircraftDerivedData.trackAngleRateDegreesPerSecond")
    }
    value.trackAngleDegrees?.let {
        buffer.putUnsignedShort(
            quantize(it, 360.0 / 65536.0, "aircraftDerivedData.trackAngleDegrees"),
            "aircraftDerivedData.trackAngleDegrees",
        )
    }
    value.groundSpeedKnots?.let {
        buffer.putUnsignedShort(
            quantize(it, 3600.0 / 16384.0, "aircraftDerivedData.groundSpeedKnots"),
            "aircraftDerivedData.groundSpeedKnots",
        )
    }
    value.velocityUncertaintyCategory?.let {
        require(it in 0..0x07) { "aircraftDerivedData.velocityUncertaintyCategory out of range: $it" }
        buffer.put((it shl 5).toByte())
    }
    value.meteorologicalData?.let { writeMeteorologicalData(buffer, it) }
    value.emitterCategory?.let { buffer.putUnsignedByte(it, "aircraftDerivedData.emitterCategory") }
    value.positionWgs84?.let { writeWgs84Position(buffer, it) }
    value.geometricAltitudeFeet?.let {
        require(it in -1500.0..150000.0) { "aircraftDerivedData.geometricAltitudeFeet out of range: $it" }
        buffer.putSignedShort(quantize(it, 6.25, "aircraftDerivedData.geometricAltitudeFeet"), "aircraftDerivedData.geometricAltitudeFeet")
    }
    value.positionUncertaintyCode?.let {
        buffer.putUnsignedByte(
            encodeUnsignedBits(it, 4, "aircraftDerivedData.positionUncertaintyCode"),
            "aircraftDerivedData.positionUncertaintyCode",
        )
    }
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
    value.indicatedAirspeedKnots?.let {
        require(it in 0..1100) { "aircraftDerivedData.indicatedAirspeedKnots out of range: $it" }
        buffer.putUnsignedShort(it, "aircraftDerivedData.indicatedAirspeedKnots")
    }
    value.machNumber?.let {
        require(it in 0.0..4.096) { "aircraftDerivedData.machNumber out of range: $it" }
        buffer.putUnsignedShort(quantize(it, 0.008, "aircraftDerivedData.machNumber"), "aircraftDerivedData.machNumber")
    }
    value.barometricPressureSettingHpa?.let {
        require(it in 800.0..1209.5) { "aircraftDerivedData.barometricPressureSettingHpa out of range: $it" }
        buffer.putUnsignedShort(
            encodeUnsignedBits(
                quantize(it - 800.0, 0.1, "aircraftDerivedData.barometricPressureSettingHpa"),
                12,
                "aircraftDerivedData.barometricPressureSettingHpa",
            ),
            "aircraftDerivedData.barometricPressureSettingHpa",
        )
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

internal fun Cat062CodecSupport.writeMeteorologicalData(
    buffer: ByteBuffer,
    value: MeteorologicalData,
) {
    val present = mutableSetOf<Int>()
    if (value.windSpeedKnots != null) present += 1
    if (value.windDirectionDegrees != null) present += 2
    if (value.temperatureCelsius != null) present += 3
    if (value.turbulenceCode != null) present += 4
    writeCompoundIndicator(buffer, present)
    value.windSpeedKnots?.let { buffer.putUnsignedShort(it, "meteorologicalData.windSpeedKnots") }
    value.windDirectionDegrees?.let {
        buffer.putUnsignedShort(
            quantize(it, 360.0 / 65536.0, "meteorologicalData.windDirectionDegrees"),
            "meteorologicalData.windDirectionDegrees",
        )
    }
    value.temperatureCelsius?.let {
        buffer.putSignedShort(quantize(it, 0.25, "meteorologicalData.temperatureCelsius"), "meteorologicalData.temperatureCelsius")
    }
    value.turbulenceCode?.let { buffer.putUnsignedByte(it, "meteorologicalData.turbulenceCode") }
}
