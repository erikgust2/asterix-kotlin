package io.github.erikgust2.asterix.cat062

import java.nio.ByteBuffer

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
        position =
            PolarPosition(
                rangeNm = buffer.short.toUnsignedInt() * (1.0 / 256.0),
                azimuthDegrees = buffer.short.toUnsignedInt() * (360.0 / 65536.0),
            )
    }
    if (isCompoundSubfieldPresent(indicator, 3)) heightFeet = buffer.short.toDouble() * 25.0
    if (isCompoundSubfieldPresent(indicator, 4)) {
        val raw = buffer.short.toUnsignedInt()
        lastMeasuredModeCCode =
            MeasuredModeCCode(
                validated = (raw and 0x8000) == 0,
                garbled = (raw and 0x4000) != 0,
                flightLevel = signExtend(raw and 0x3FFF, 14) * 0.25,
            )
    }
    if (isCompoundSubfieldPresent(indicator, 5)) {
        val raw = buffer.short.toUnsignedInt()
        lastMeasuredMode3aCode =
            MeasuredMode3ACode(
                code = raw and 0x0FFF,
                validated = (raw and 0x8000) == 0,
                garbled = (raw and 0x4000) != 0,
                smoothed = (raw and 0x2000) != 0,
            )
    }
    if (isCompoundSubfieldPresent(indicator, 6)) {
        val octet = buffer.get().toUnsignedInt()
        reportType =
            ReportType(
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

internal fun Cat062CodecSupport.writeMeasuredInformation(
    buffer: ByteBuffer,
    value: MeasuredInformation,
) {
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
        buffer.putUnsignedShort(
            quantize(it.rangeNm, 1.0 / 256.0, "measuredInformation.position.rangeNm"),
            "measuredInformation.position.rangeNm",
        )
        buffer.putUnsignedShort(
            quantize(it.azimuthDegrees, 360.0 / 65536.0, "measuredInformation.position.azimuthDegrees"),
            "measuredInformation.position.azimuthDegrees",
        )
    }
    value.heightFeet?.let { buffer.putSignedShort(quantize(it, 25.0, "measuredInformation.heightFeet"), "measuredInformation.heightFeet") }
    value.lastMeasuredModeCCode?.let {
        var raw =
            encodeSignedBits(
                quantize(it.flightLevel, 0.25, "measuredInformation.lastMeasuredModeCCode.flightLevel"),
                14,
                "measuredInformation.lastMeasuredModeCCode.flightLevel",
            )
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
        val octet =
            ((it.typ and 0x07) shl 5) or (if (it.simulated) 0x10 else 0) or
                (if (it.rab) 0x08 else 0) or (if (it.testTarget) 0x04 else 0)
        buffer.put(octet.toByte())
    }
}
