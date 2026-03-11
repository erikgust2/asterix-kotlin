package com.erik.asterix.cat062

data class DataSourceIdentifier(
    val sac: Int,
    val sic: Int,
)

data class Wgs84Position(
    val latitudeDegrees: Double,
    val longitudeDegrees: Double,
)

data class CartesianPosition(
    val xMeters: Double,
    val yMeters: Double,
)

data class CartesianVelocity(
    val xMetersPerSecond: Double,
    val yMetersPerSecond: Double,
)

data class CartesianAcceleration(
    val xMetersPerSecondSquared: Double,
    val yMetersPerSecondSquared: Double,
)

data class Mode3ACode(
    val code: Int,
    val codeChanged: Boolean,
)

data class Mode2Code(
    val code: Int,
)

enum class TargetIdentificationSource(val code: Int) {
    CALLSIGN_OR_REGISTRATION_FROM_TRANSPONDER(0),
    CALLSIGN_NOT_FROM_TRANSPONDER(1),
    REGISTRATION_NOT_FROM_TRANSPONDER(2),
    INVALID(3),
}

data class TargetIdentification(
    val source: TargetIdentificationSource,
    val value: String,
)

data class Airspeed(
    val type: AirspeedType,
    val value: Double,
)

enum class AirspeedType {
    INDICATED_AIRSPEED_KNOTS,
    MACH,
}

data class FlightLevelMeasurement(
    val flightLevel: Double,
)

data class BarometricAltitude(
    val qnhCorrectionApplied: Boolean,
    val altitudeFeet: Double,
)

data class ComposedTrackNumber(
    val systemUnitIdentification: Int,
    val trackNumber: Int,
)
