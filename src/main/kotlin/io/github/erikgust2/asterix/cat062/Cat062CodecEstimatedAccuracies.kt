package io.github.erikgust2.asterix.cat062

import java.nio.ByteBuffer

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
        positionCartesian =
            CartesianAccuracy(
                xMeters = buffer.short.toUnsignedInt() * 0.5,
                yMeters = buffer.short.toUnsignedInt() * 0.5,
            )
    }
    if (isCompoundSubfieldPresent(indicator, 2)) xyCovarianceMeters = buffer.short.toDouble() * 0.5
    if (isCompoundSubfieldPresent(indicator, 3)) {
        positionWgs84 =
            Wgs84Accuracy(
                latitudeDegrees = buffer.short.toUnsignedInt() * WGS84_RESOLUTION,
                longitudeDegrees = buffer.short.toUnsignedInt() * WGS84_RESOLUTION,
            )
    }
    if (isCompoundSubfieldPresent(indicator, 4)) geometricAltitudeFeet = buffer.get().toUnsignedInt() * 6.25
    if (isCompoundSubfieldPresent(indicator, 5)) barometricAltitudeFeet = buffer.get().toUnsignedInt() * 25.0
    if (isCompoundSubfieldPresent(indicator, 6)) {
        trackVelocity =
            CartesianVelocity(
                xMetersPerSecond = buffer.get().toUnsignedInt() * 0.25,
                yMetersPerSecond = buffer.get().toUnsignedInt() * 0.25,
            )
    }
    if (isCompoundSubfieldPresent(indicator, 7)) {
        trackAcceleration =
            CartesianAcceleration(
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

internal fun Cat062CodecSupport.writeEstimatedAccuracies(
    buffer: ByteBuffer,
    value: EstimatedAccuracies,
) {
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
        buffer.putUnsignedShort(
            quantize(it.xMeters, 0.5, "estimatedAccuracies.positionCartesian.xMeters"),
            "estimatedAccuracies.positionCartesian.xMeters",
        )
        buffer.putUnsignedShort(
            quantize(it.yMeters, 0.5, "estimatedAccuracies.positionCartesian.yMeters"),
            "estimatedAccuracies.positionCartesian.yMeters",
        )
    }
    value.xyCovarianceMeters?.let {
        buffer.putSignedShort(quantize(it, 0.5, "estimatedAccuracies.xyCovarianceMeters"), "estimatedAccuracies.xyCovarianceMeters")
    }
    value.positionWgs84?.let {
        buffer.putUnsignedShort(
            quantize(it.latitudeDegrees, WGS84_RESOLUTION, "estimatedAccuracies.positionWgs84.latitudeDegrees"),
            "estimatedAccuracies.positionWgs84.latitudeDegrees",
        )
        buffer.putUnsignedShort(
            quantize(it.longitudeDegrees, WGS84_RESOLUTION, "estimatedAccuracies.positionWgs84.longitudeDegrees"),
            "estimatedAccuracies.positionWgs84.longitudeDegrees",
        )
    }
    value.geometricAltitudeFeet?.let {
        buffer.putUnsignedByte(quantize(it, 6.25, "estimatedAccuracies.geometricAltitudeFeet"), "estimatedAccuracies.geometricAltitudeFeet")
    }
    value.barometricAltitudeFeet?.let {
        buffer.putUnsignedByte(
            quantize(it, 25.0, "estimatedAccuracies.barometricAltitudeFeet"),
            "estimatedAccuracies.barometricAltitudeFeet",
        )
    }
    value.trackVelocity?.let {
        buffer.putUnsignedByte(
            quantize(it.xMetersPerSecond, 0.25, "estimatedAccuracies.trackVelocity.xMetersPerSecond"),
            "estimatedAccuracies.trackVelocity.xMetersPerSecond",
        )
        buffer.putUnsignedByte(
            quantize(it.yMetersPerSecond, 0.25, "estimatedAccuracies.trackVelocity.yMetersPerSecond"),
            "estimatedAccuracies.trackVelocity.yMetersPerSecond",
        )
    }
    value.trackAcceleration?.let {
        buffer.putUnsignedByte(
            quantize(it.xMetersPerSecondSquared, 0.25, "estimatedAccuracies.trackAcceleration.xMetersPerSecondSquared"),
            "estimatedAccuracies.trackAcceleration.xMetersPerSecondSquared",
        )
        buffer.putUnsignedByte(
            quantize(it.yMetersPerSecondSquared, 0.25, "estimatedAccuracies.trackAcceleration.yMetersPerSecondSquared"),
            "estimatedAccuracies.trackAcceleration.yMetersPerSecondSquared",
        )
    }
    value.rateOfClimbDescentFeetPerMinute?.let {
        buffer.putUnsignedByte(
            quantize(it, 6.25, "estimatedAccuracies.rateOfClimbDescentFeetPerMinute"),
            "estimatedAccuracies.rateOfClimbDescentFeetPerMinute",
        )
    }
}
