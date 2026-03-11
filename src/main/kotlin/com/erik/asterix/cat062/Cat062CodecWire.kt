package com.erik.asterix.cat062

import java.nio.ByteBuffer
import kotlin.math.roundToInt

internal fun Cat062CodecSupport.readDataSourceIdentifier(buffer: ByteBuffer): DataSourceIdentifier =
    DataSourceIdentifier(buffer.get().toUnsignedInt(), buffer.get().toUnsignedInt())

internal fun Cat062CodecSupport.writeDataSourceIdentifier(buffer: ByteBuffer, value: DataSourceIdentifier) {
    buffer.putUnsignedByte(value.sac, "dataSourceIdentifier.sac")
    buffer.putUnsignedByte(value.sic, "dataSourceIdentifier.sic")
}

internal fun Cat062CodecSupport.readWgs84Position(buffer: ByteBuffer): Wgs84Position =
    Wgs84Position(
        latitudeDegrees = readSignedInt32(buffer) * WGS84_RESOLUTION,
        longitudeDegrees = readSignedInt32(buffer) * WGS84_RESOLUTION,
    )

internal fun Cat062CodecSupport.writeWgs84Position(buffer: ByteBuffer, value: Wgs84Position) {
    buffer.putInt((value.latitudeDegrees / WGS84_RESOLUTION).roundToInt())
    buffer.putInt((value.longitudeDegrees / WGS84_RESOLUTION).roundToInt())
}

internal fun Cat062CodecSupport.readCartesianPosition(buffer: ByteBuffer): CartesianPosition =
    CartesianPosition(
        xMeters = readSignedInt24(buffer) * 0.5,
        yMeters = readSignedInt24(buffer) * 0.5,
    )

internal fun Cat062CodecSupport.writeCartesianPosition(buffer: ByteBuffer, value: CartesianPosition) {
    writeSignedInt24(buffer, (value.xMeters / 0.5).roundToInt())
    writeSignedInt24(buffer, (value.yMeters / 0.5).roundToInt())
}

internal fun Cat062CodecSupport.readCartesianVelocity(buffer: ByteBuffer): CartesianVelocity =
    CartesianVelocity(
        xMetersPerSecond = buffer.short.toDouble() * 0.25,
        yMetersPerSecond = buffer.short.toDouble() * 0.25,
    )

internal fun Cat062CodecSupport.writeCartesianVelocity(buffer: ByteBuffer, value: CartesianVelocity) {
    buffer.putSignedShort((value.xMetersPerSecond / 0.25).roundToInt(), "calculatedTrackVelocityCartesian.xMetersPerSecond")
    buffer.putSignedShort((value.yMetersPerSecond / 0.25).roundToInt(), "calculatedTrackVelocityCartesian.yMetersPerSecond")
}

internal fun Cat062CodecSupport.readCartesianAcceleration(buffer: ByteBuffer): CartesianAcceleration =
    CartesianAcceleration(
        xMetersPerSecondSquared = buffer.get().toDouble() * 0.25,
        yMetersPerSecondSquared = buffer.get().toDouble() * 0.25,
    )

internal fun Cat062CodecSupport.writeCartesianAcceleration(buffer: ByteBuffer, value: CartesianAcceleration) {
    buffer.putSignedByte((value.xMetersPerSecondSquared / 0.25).roundToInt(), "calculatedAccelerationCartesian.xMetersPerSecondSquared")
    buffer.putSignedByte((value.yMetersPerSecondSquared / 0.25).roundToInt(), "calculatedAccelerationCartesian.yMetersPerSecondSquared")
}

internal fun Cat062CodecSupport.readMode3ACode(buffer: ByteBuffer): Mode3ACode {
    val b1 = buffer.get().toUnsignedInt()
    val b2 = buffer.get().toUnsignedInt()
    return Mode3ACode(
        code = ((b1 and 0x0F) shl 8) or b2,
        codeChanged = (b1 and 0x20) != 0,
    )
}

internal fun Cat062CodecSupport.writeMode3ACode(buffer: ByteBuffer, value: Mode3ACode) {
    require(value.code in 0..0x0FFF) { "trackMode3aCode.code out of range: ${value.code}" }
    var b1 = (value.code shr 8) and 0x0F
    if (value.codeChanged) b1 = b1 or 0x20
    buffer.put(b1.toByte())
    buffer.put((value.code and 0xFF).toByte())
}

internal fun Cat062CodecSupport.readTargetIdentification(buffer: ByteBuffer): TargetIdentification {
    val first = buffer.get().toUnsignedInt()
    val raw = LongArray(8)
    raw[0] = (first and 0x3F).toLong()
    val rest = ByteArray(5)
    buffer.get(rest)
    var bits = 0L
    rest.forEach { bits = (bits shl 8) or it.toUnsignedInt().toLong() }
    for (index in 1 until 8) {
        raw[index] = (bits shr ((7 - index) * 6)) and 0x3F
    }
    return TargetIdentification(
        source = TargetIdentificationSource.entries.first { it.code == (first ushr 6) },
        value = raw.joinToString(separator = "") { sixBitChar(it.toInt()).toString() }.trim(),
    )
}

internal fun Cat062CodecSupport.writeTargetIdentification(buffer: ByteBuffer, value: TargetIdentification) {
    val chars = normaliseCallsign(value.value, 8)
    var first = value.source.code shl 6
    first = first or encodeSixBitChar(chars[0])
    buffer.put(first.toByte())
    var packed = 0L
    for (index in 1 until 8) {
        packed = (packed shl 6) or encodeSixBitChar(chars[index]).toLong()
    }
    for (shift in 32 downTo 0 step 8) {
        buffer.put(((packed shr shift) and 0xFF).toByte())
    }
}

internal fun Cat062CodecSupport.readFlightLevelMeasurement(buffer: ByteBuffer): FlightLevelMeasurement {
    val raw = buffer.short.toInt()
    return FlightLevelMeasurement(
        flightLevel = raw * 0.25,
    )
}

internal fun Cat062CodecSupport.writeFlightLevelMeasurement(buffer: ByteBuffer, value: FlightLevelMeasurement) {
    buffer.putSignedShort((value.flightLevel / 0.25).roundToInt(), "measuredFlightLevel.flightLevel")
}

internal fun Cat062CodecSupport.readBarometricAltitude(buffer: ByteBuffer): BarometricAltitude {
    val raw = buffer.short.toUnsignedInt()
    return BarometricAltitude(
        qnhCorrectionApplied = (raw and 0x8000) != 0,
        altitudeFeet = signExtend(raw and 0x7FFF, 15) * 25.0,
    )
}

internal fun Cat062CodecSupport.writeBarometricAltitude(buffer: ByteBuffer, value: BarometricAltitude) {
    var raw = encodeSignedBits((value.altitudeFeet / 25.0).roundToInt(), 15, "calculatedTrackBarometricAltitude.altitudeFeet")
    if (value.qnhCorrectionApplied) raw = raw or 0x8000
    buffer.putShort(raw.toShort())
}

internal fun Cat062CodecSupport.readTargetSizeAndOrientation(buffer: ByteBuffer): TargetSizeAndOrientation {
    val octet1 = buffer.get().toUnsignedInt()
    val lengthMeters = octet1 ushr 1
    var orientationDegrees: Double? = null
    var widthMeters: Int? = null
    if ((octet1 and 0x01) != 0) {
        val octet2 = buffer.get().toUnsignedInt()
        orientationDegrees = (octet2 ushr 1) * (360.0 / 128.0)
        if ((octet2 and 0x01) != 0) {
            widthMeters = buffer.get().toUnsignedInt() ushr 1
        }
    }
    return TargetSizeAndOrientation(lengthMeters, orientationDegrees, widthMeters)
}

internal fun Cat062CodecSupport.writeTargetSizeAndOrientation(buffer: ByteBuffer, value: TargetSizeAndOrientation) {
    require(value.lengthMeters in 0..0x7F) { "targetSizeAndOrientation.lengthMeters out of range: ${value.lengthMeters}" }
    var octet1 = (value.lengthMeters and 0x7F) shl 1
    if (value.orientationDegrees != null) octet1 = octet1 or 0x01
    buffer.put(octet1.toByte())
    value.orientationDegrees?.let {
        var octet2 = (((it / (360.0 / 128.0)).roundToInt()) and 0x7F) shl 1
        if (value.widthMeters != null) octet2 = octet2 or 0x01
        buffer.put(octet2.toByte())
        value.widthMeters?.let { width ->
            require(width in 0..0x7F) { "targetSizeAndOrientation.widthMeters out of range: $width" }
            buffer.put(((width and 0x7F) shl 1).toByte())
        }
    }
}

internal fun Cat062CodecSupport.readVehicleFleetIdentification(buffer: ByteBuffer): VehicleFleetIdentification {
    val code = buffer.get().toUnsignedInt()
    return VehicleFleetIdentification.fromCode(code)
}

internal fun Cat062CodecSupport.readMode2Code(buffer: ByteBuffer): Mode2Code {
    val b1 = buffer.get().toUnsignedInt()
    val b2 = buffer.get().toUnsignedInt()
    return Mode2Code(
        code = ((b1 and 0x0F) shl 8) or b2,
    )
}

internal fun Cat062CodecSupport.writeMode2Code(buffer: ByteBuffer, value: Mode2Code) {
    require(value.code in 0..0x0FFF) { "trackMode2Code.code out of range: ${value.code}" }
    val b1 = (value.code shr 8) and 0x0F
    buffer.put(b1.toByte())
    buffer.put((value.code and 0xFF).toByte())
}

internal fun Cat062CodecSupport.readComposedTrackNumber(buffer: ByteBuffer): ComposedTrackNumber =
    ComposedTrackNumber(
        systemUnitIdentification = buffer.get().toUnsignedInt(),
        trackNumber = buffer.short.toUnsignedInt(),
    )

internal fun Cat062CodecSupport.writeComposedTrackNumber(buffer: ByteBuffer, value: ComposedTrackNumber) {
    buffer.putUnsignedByte(value.systemUnitIdentification, "composedTrackNumber.systemUnitIdentification")
    buffer.putUnsignedShort(value.trackNumber, "composedTrackNumber.trackNumber")
}

internal fun Cat062CodecSupport.readCompoundIndicator(buffer: ByteBuffer): IntArray {
    val bytes = mutableListOf<Int>()
    var octet: Int
    do {
        octet = buffer.get().toUnsignedInt()
        bytes += octet
    } while ((octet and 0x01) != 0)
    return bytes.toIntArray()
}

internal fun Cat062CodecSupport.writeCompoundIndicator(buffer: ByteBuffer, present: Set<Int>) {
    if (present.isEmpty()) {
        buffer.put(0.toByte())
        return
    }
    val octetCount = ((present.maxOrNull()!! - 1) / 7) + 1
    repeat(octetCount) { index ->
        var octet = 0
        for (bit in 0 until 7) {
            val subfield = index * 7 + bit + 1
            if (subfield in present) {
                octet = octet or (1 shl (7 - bit))
            }
        }
        if (index < octetCount - 1) {
            octet = octet or 0x01
        }
        buffer.put(octet.toByte())
    }
}

internal fun Cat062CodecSupport.isCompoundSubfieldPresent(indicator: IntArray, subfield: Int): Boolean {
    val octetIndex = (subfield - 1) / 7
    if (octetIndex >= indicator.size) return false
    val bitIndex = (subfield - 1) % 7
    return (indicator[octetIndex] and (1 shl (7 - bitIndex))) != 0
}

internal fun Cat062CodecSupport.readLengthPrefixedField(buffer: ByteBuffer): RawBytes {
    val totalLength = buffer.get().toUnsignedInt()
    require(totalLength >= 1) { "Invalid length-prefixed field length $totalLength" }
    return readBytes(buffer, totalLength - 1).toRawBytes()
}

internal fun Cat062CodecSupport.writeLengthPrefixedField(buffer: ByteBuffer, value: RawBytes) {
    require(value.size <= 254) { "Length-prefixed field too large: ${value.size}" }
    buffer.put((value.size + 1).toByte())
    buffer.put(value.unsafeBytes())
}

internal fun Cat062CodecSupport.readAscii(buffer: ByteBuffer, length: Int): String =
    buildString(length) { repeat(length) { append(buffer.get().toUnsignedInt().toChar()) } }

internal fun Cat062CodecSupport.writeAscii(buffer: ByteBuffer, value: String, length: Int) {
    val padded = value.padEnd(length, ' ').take(length)
    padded.forEach { buffer.put(it.code.toByte()) }
}

internal fun Cat062CodecSupport.encodePackedCallsign(buffer: ByteBuffer, value: String) {
    val chars = normaliseCallsign(value, 8)
    var packed = 0L
    chars.forEach { packed = (packed shl 6) or encodeSixBitChar(it).toLong() }
    for (shift in 40 downTo 0 step 8) {
        buffer.put(((packed shr shift) and 0xFF).toByte())
    }
}

internal fun Cat062CodecSupport.decodePackedCallsign(bytes: ByteArray): String {
    var packed = 0L
    bytes.forEach { packed = (packed shl 8) or it.toUnsignedInt().toLong() }
    return buildString(8) {
        for (shift in 42 downTo 0 step 6) {
            append(sixBitChar(((packed shr shift) and 0x3F).toInt()))
        }
    }.trim()
}

private fun normaliseCallsign(value: String, length: Int): String =
    value.uppercase().padEnd(length, ' ').take(length)

private fun sixBitChar(code: Int): Char = when (code) {
    0 -> ' '
    in 1..26 -> ('A'.code + code - 1).toChar()
    in 48..57 -> ('0'.code + code - 48).toChar()
    else -> ' '
}

private fun encodeSixBitChar(char: Char): Int = when (char) {
    ' ' -> 0
    in 'A'..'Z' -> char.code - 'A'.code + 1
    in '0'..'9' -> char.code - '0'.code + 48
    else -> 0
}

internal fun Cat062CodecSupport.readBytes(buffer: ByteBuffer, length: Int): ByteArray =
    ByteArray(length).also { buffer.get(it) }

internal fun Cat062CodecSupport.readUnsignedInt24(buffer: ByteBuffer): Int =
    (buffer.get().toUnsignedInt() shl 16) or (buffer.get().toUnsignedInt() shl 8) or buffer.get().toUnsignedInt()

private fun Cat062CodecSupport.readSignedInt24(buffer: ByteBuffer): Int = signExtend(readUnsignedInt24(buffer), 24)

internal fun Cat062CodecSupport.writeUnsignedInt24(buffer: ByteBuffer, value: Int) {
    require(value in 0..0xFFFFFF) { "Unsigned 24-bit value out of range: $value" }
    buffer.put(((value ushr 16) and 0xFF).toByte())
    buffer.put(((value ushr 8) and 0xFF).toByte())
    buffer.put((value and 0xFF).toByte())
}

private fun Cat062CodecSupport.writeSignedInt24(buffer: ByteBuffer, value: Int) = writeUnsignedInt24(buffer, value and 0xFFFFFF)

private fun Cat062CodecSupport.readSignedInt32(buffer: ByteBuffer): Int = buffer.int

internal fun Cat062CodecSupport.readUnsignedInt56(buffer: ByteBuffer): Long {
    var value = 0L
    repeat(7) {
        value = (value shl 8) or buffer.get().toUnsignedInt().toLong()
    }
    return value
}

internal fun Cat062CodecSupport.writeUnsignedInt56(buffer: ByteBuffer, value: Long) {
    val remaining = value and 0x00FFFFFFFFFFFFFFL
    for (shift in 48 downTo 0 step 8) {
        buffer.put(((remaining shr shift) and 0xFF).toByte())
    }
}

internal fun signExtend(value: Int, bitWidth: Int): Int {
    val shift = 32 - bitWidth
    return (value shl shift) shr shift
}

internal fun Byte.toUnsignedInt(): Int = toInt() and 0xFF
internal fun Short.toUnsignedInt(): Int = toInt() and 0xFFFF

internal fun ByteBuffer.putUnsignedByte(value: Int, fieldName: String) {
    require(value in 0..0xFF) { "$fieldName out of range: $value" }
    put(value.toByte())
}

internal fun ByteBuffer.putSignedByte(value: Int, fieldName: String) {
    require(value in Byte.MIN_VALUE..Byte.MAX_VALUE) { "$fieldName out of range: $value" }
    put(value.toByte())
}

internal fun ByteBuffer.putUnsignedShort(value: Int, fieldName: String) {
    require(value in 0..0xFFFF) { "$fieldName out of range: $value" }
    putShort(value.toShort())
}

internal fun ByteBuffer.putSignedShort(value: Int, fieldName: String) {
    require(value in Short.MIN_VALUE..Short.MAX_VALUE) { "$fieldName out of range: $value" }
    putShort(value.toShort())
}

internal fun encodeSignedBits(value: Int, bitWidth: Int, fieldName: String): Int {
    val min = -(1 shl (bitWidth - 1))
    val max = (1 shl (bitWidth - 1)) - 1
    require(value in min..max) { "$fieldName out of range: $value" }
    return value and ((1 shl bitWidth) - 1)
}

internal fun requireRawLength(value: RawBytes, expectedLength: Int, fieldName: String) {
    require(value.size == expectedLength) { "$fieldName must be $expectedLength bytes but was ${value.size}" }
}

internal const val WGS84_RESOLUTION = 180.0 / 33554432.0
