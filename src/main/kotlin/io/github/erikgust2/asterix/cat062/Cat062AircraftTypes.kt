package io.github.erikgust2.asterix.cat062

data class SelectedAltitude(
    val sourceAvailable: Boolean,
    val sourceCode: Int,
    val flightLevel: Double,
)

data class FinalStateSelectedAltitude(
    val managedVerticalModeActive: Boolean,
    val altitudeHoldActive: Boolean,
    val approachModeActive: Boolean,
    val flightLevel: Double,
)

data class TrajectoryIntentStatus(
    val available: Boolean,
    val valid: Boolean,
)

data class TrajectoryIntentPoint(
    val raw: RawBytes,
) {
    constructor(raw: ByteArray) : this(raw.toRawBytes())
}

data class CommunicationsCapabilities(
    val comCode: Int,
    val statCode: Int,
    val ssc: Boolean,
    val arcCode: Boolean,
    val aic: Boolean,
    val b1a: Int,
    val b1b: Int,
)

data class AdsbStatus(
    val ac: Int,
    val mn: Int,
    val dc: Int,
    val gbs: Boolean,
    val stat: Int,
)

data class AcasResolutionAdvisory(
    val raw: Long,
)

data class MeteorologicalData(
    val windSpeedKnots: Int? = null,
    val windDirectionDegrees: Double? = null,
    val temperatureCelsius: Double? = null,
    val turbulenceCode: Int? = null,
)

data class ModeSMessage(
    val message: RawBytes,
    val bds1: Int,
    val bds2: Int,
) {
    constructor(message: ByteArray, bds1: Int, bds2: Int) : this(message.toRawBytes(), bds1, bds2)
}

data class AircraftDerivedData(
    val targetAddress: Int? = null,
    val targetIdentification: String? = null,
    val magneticHeadingDegrees: Double? = null,
    val indicatedAirspeed: Airspeed? = null,
    val trueAirspeedKnots: Int? = null,
    val selectedAltitude: SelectedAltitude? = null,
    val finalStateSelectedAltitude: FinalStateSelectedAltitude? = null,
    val trajectoryIntentStatus: TrajectoryIntentStatus? = null,
    val trajectoryIntentData: List<TrajectoryIntentPoint>? = null,
    val communicationsCapabilities: CommunicationsCapabilities? = null,
    val adsbStatus: AdsbStatus? = null,
    val acasResolutionAdvisoryReport: AcasResolutionAdvisory? = null,
    val barometricVerticalRateFeetPerMinute: Double? = null,
    val geometricVerticalRateFeetPerMinute: Double? = null,
    val rollAngleDegrees: Double? = null,
    val trackAngleRateDegreesPerSecond: Double? = null,
    val trackAngleDegrees: Double? = null,
    val groundSpeedKnots: Double? = null,
    val velocityUncertaintyCategory: Int? = null,
    val meteorologicalData: MeteorologicalData? = null,
    val emitterCategory: Int? = null,
    val positionWgs84: Wgs84Position? = null,
    val geometricAltitudeFeet: Double? = null,
    val positionUncertaintyCode: Int? = null,
    val modeSMessages: List<ModeSMessage>? = null,
    val indicatedAirspeedKnots: Int? = null,
    val machNumber: Double? = null,
    val barometricPressureSettingHpa: Double? = null,
)
