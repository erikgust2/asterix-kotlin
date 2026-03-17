package io.github.erikgust2.asterix.cat062

import java.nio.ByteBuffer

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
        summary =
            Mode5Summary(
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
        pinNationalOriginMission =
            PinNationalOriginMission(
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
        xPulsePresence =
            Mode5XPulsePresence(
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

internal fun Cat062CodecSupport.writeMode5DataReports(
    buffer: ByteBuffer,
    value: Mode5DataReports,
) {
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
        val octet =
            (if (it.m5) 0x80 else 0) or (if (it.id) 0x40 else 0) or (if (it.da) 0x20 else 0) or
                (if (it.m1) 0x10 else 0) or (if (it.m2) 0x08 else 0) or (if (it.m3) 0x04 else 0) or
                (if (it.mc) 0x02 else 0) or (if (it.x) 0x01 else 0)
        buffer.put(octet.toByte())
    }
    value.pinNationalOriginMission?.let {
        require(it.pin in 0..0x3FFF) { "mode5DataReports.pinNationalOriginMission.pin out of range: ${it.pin}" }
        require(
            it.nationalOrigin in 0..0x1F,
        ) { "mode5DataReports.pinNationalOriginMission.nationalOrigin out of range: ${it.nationalOrigin}" }
        require(it.missionCode in 0..0x3F) { "mode5DataReports.pinNationalOriginMission.missionCode out of range: ${it.missionCode}" }
        val raw = ((it.pin and 0x3FFF) shl 17) or ((it.nationalOrigin and 0x1F) shl 9) or (it.missionCode and 0x3F)
        buffer.putInt(raw)
    }
    value.positionWgs84?.let { writeWgs84Position(buffer, it) }
    value.geometricAltitudeFeet?.let {
        buffer.putSignedShort(quantize(it, 25.0, "mode5DataReports.geometricAltitudeFeet"), "mode5DataReports.geometricAltitudeFeet")
    }
    value.extendedMode1Code?.let {
        require(it.code in 0..0x0FFF) { "mode5DataReports.extendedMode1Code.code out of range: ${it.code}" }
        buffer.putUnsignedShort(it.code, "mode5DataReports.extendedMode1Code")
    }
    value.timeOffsetSeconds?.let {
        buffer.putSignedByte(quantize(it, 1.0 / 128.0, "mode5DataReports.timeOffsetSeconds"), "mode5DataReports.timeOffsetSeconds")
    }
    value.xPulsePresence?.let {
        val octet =
            (if (it.x5) 0x10 else 0) or (if (it.xc) 0x08 else 0) or (if (it.x3) 0x04 else 0) or
                (if (it.x2) 0x02 else 0) or (if (it.x1) 0x01 else 0)
        buffer.put(octet.toByte())
    }
}
