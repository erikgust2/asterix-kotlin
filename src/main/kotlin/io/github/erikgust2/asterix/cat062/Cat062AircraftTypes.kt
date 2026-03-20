package io.github.erikgust2.asterix.cat062

data class SelectedAltitude(
    val sourceAvailable: Boolean,
    val sourceCode: SelectedAltitudeSource,
    val flightLevel: Double,
)

enum class SelectedAltitudeSource(
    val code: Int,
) {
    UNKNOWN(0),
    AIRCRAFT_ALTITUDE(1),
    FCU_MCP_SELECTED_ALTITUDE(2),
    FMS_SELECTED_ALTITUDE(3),
    ;

    companion object {
        fun fromCode(code: Int): SelectedAltitudeSource = entries.first { it.code == code }
    }
}

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
    val tcpNumberAvailable: Boolean,
    val tcpNonCompliance: Boolean,
    val tcpNumber: Int,
    val altitudeFeet: Double,
    val positionWgs84: Wgs84Position,
    val pointType: TrajectoryIntentPointType,
    val turnDirectionCode: TurnDirection,
    val turnRadiusAvailable: Boolean,
    val timeOverPointAvailable: Boolean,
    val timeOverPointSeconds: Int,
    val turnRadiusNm: Double,
)

enum class TrajectoryIntentPointType(
    val code: Int,
) {
    UNKNOWN(0),
    FLY_BY_WAYPOINT(1),
    FLY_OVER_WAYPOINT(2),
    HOLD_PATTERN(3),
    PROCEDURE_HOLD(4),
    PROCEDURE_TURN(5),
    RF_LEG(6),
    TOP_OF_CLIMB(7),
    TOP_OF_DESCENT(8),
    START_OF_LEVEL(9),
    CROSS_OVER_ALTITUDE(10),
    TRANSITION_ALTITUDE(11),
    ;

    companion object {
        fun fromCode(code: Int): TrajectoryIntentPointType = entries.first { it.code == code }
    }
}

enum class TurnDirection(
    val code: Int,
) {
    NOT_APPLICABLE(0),
    RIGHT(1),
    LEFT(2),
    NO_TURN(3),
    ;

    companion object {
        fun fromCode(code: Int): TurnDirection = entries.first { it.code == code }
    }
}

data class CommunicationsCapabilities(
    val comCode: ModeSCommunicationsCapability,
    val statCode: ModeSFlightStatus,
    val ssc: Boolean,
    val arcCode: Boolean,
    val aic: Boolean,
    val b1a: Int,
    val b1b: Int,
)

sealed interface ModeSCommunicationsCapability {
    val code: Int

    enum class Known(
        override val code: Int,
    ) : ModeSCommunicationsCapability {
        NO_COMMUNICATIONS_CAPABILITY(0),
        COMM_A_AND_COMM_B_CAPABILITY(1),
        COMM_A_COMM_B_AND_UPLINK_ELM(2),
        COMM_A_COMM_B_UPLINK_ELM_AND_DOWNLINK_ELM(3),
        LEVEL_5_TRANSPONDER_CAPABILITY(4),
    }

    data class Unknown(
        override val code: Int,
    ) : ModeSCommunicationsCapability

    companion object {
        fun fromCode(code: Int): ModeSCommunicationsCapability = Known.entries.firstOrNull { it.code == code } ?: Unknown(code)
    }
}

sealed interface ModeSFlightStatus {
    val code: Int

    enum class Known(
        override val code: Int,
    ) : ModeSFlightStatus {
        NO_ALERT_NO_SPI_AIRBORNE(0),
        NO_ALERT_NO_SPI_ON_GROUND(1),
        ALERT_NO_SPI_AIRBORNE(2),
        ALERT_NO_SPI_ON_GROUND(3),
        ALERT_SPI_AIRBORNE_OR_ON_GROUND(4),
        NO_ALERT_SPI_AIRBORNE_OR_ON_GROUND(5),
    }

    data class Unknown(
        override val code: Int,
    ) : ModeSFlightStatus

    companion object {
        fun fromCode(code: Int): ModeSFlightStatus = Known.entries.firstOrNull { it.code == code } ?: Unknown(code)
    }
}

data class AdsbStatus(
    val ac: AdsbAcasStatus,
    val mn: MultipleNavigationalAidStatus,
    val dc: DifferentialCorrectionStatus,
    val gbs: Boolean,
    val stat: AdsbEmergencyStatus,
)

enum class AdsbAcasStatus(
    val code: Int,
) {
    UNKNOWN(0),
    ACAS_NOT_OPERATIONAL(1),
    ACAS_OPERATIONAL(2),
    INVALID(3),
    ;

    companion object {
        fun fromCode(code: Int): AdsbAcasStatus = entries.first { it.code == code }
    }
}

enum class MultipleNavigationalAidStatus(
    val code: Int,
) {
    UNKNOWN(0),
    MULTIPLE_NAVIGATIONAL_AIDS_NOT_OPERATING(1),
    MULTIPLE_NAVIGATIONAL_AIDS_OPERATING(2),
    INVALID(3),
    ;

    companion object {
        fun fromCode(code: Int): MultipleNavigationalAidStatus = entries.first { it.code == code }
    }
}

enum class DifferentialCorrectionStatus(
    val code: Int,
) {
    UNKNOWN(0),
    DIFFERENTIAL_CORRECTION(1),
    NO_DIFFERENTIAL_CORRECTION(2),
    INVALID(3),
    ;

    companion object {
        fun fromCode(code: Int): DifferentialCorrectionStatus = entries.first { it.code == code }
    }
}

enum class AdsbEmergencyStatus(
    val code: Int,
) {
    NO_EMERGENCY(0),
    GENERAL_EMERGENCY(1),
    LIFEGUARD_MEDICAL(2),
    MINIMUM_FUEL(3),
    NO_COMMUNICATIONS(4),
    UNLAWFUL_INTERFERENCE(5),
    DOWNED_AIRCRAFT(6),
    UNKNOWN(7),
    ;

    companion object {
        fun fromCode(code: Int): AdsbEmergencyStatus = entries.first { it.code == code }
    }
}

data class AcasResolutionAdvisory(
    val raw: Long,
)

data class MeteorologicalData(
    val windSpeedKnots: Int? = null,
    val windDirectionDegrees: Double? = null,
    val temperatureCelsius: Double? = null,
    val turbulenceCode: TurbulenceLevel? = null,
)

enum class TurbulenceLevel(
    val code: Int,
) {
    LEVEL_0(0),
    LEVEL_1(1),
    LEVEL_2(2),
    LEVEL_3(3),
    LEVEL_4(4),
    LEVEL_5(5),
    LEVEL_6(6),
    LEVEL_7(7),
    LEVEL_8(8),
    LEVEL_9(9),
    LEVEL_10(10),
    LEVEL_11(11),
    LEVEL_12(12),
    LEVEL_13(13),
    LEVEL_14(14),
    LEVEL_15(15),
    ;

    companion object {
        fun fromCode(code: Int): TurbulenceLevel = entries.first { it.code == code }
    }
}

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
    val velocityUncertaintyCategory: VelocityUncertaintyCategory? = null,
    val meteorologicalData: MeteorologicalData? = null,
    val emitterCategory: AircraftEmitterCategory? = null,
    val positionWgs84: Wgs84Position? = null,
    val geometricAltitudeFeet: Double? = null,
    val positionUncertaintyCode: PositionUncertaintyCategory? = null,
    val modeSMessages: List<ModeSMessage>? = null,
    val indicatedAirspeedKnots: Int? = null,
    val machNumber: Double? = null,
    val barometricPressureSettingHpa: Double? = null,
)

enum class VelocityUncertaintyCategory(
    val code: Int,
) {
    CATEGORY_0(0),
    CATEGORY_1(1),
    CATEGORY_2(2),
    CATEGORY_3(3),
    CATEGORY_4(4),
    CATEGORY_5(5),
    CATEGORY_6(6),
    CATEGORY_7(7),
    ;

    companion object {
        fun fromCode(code: Int): VelocityUncertaintyCategory = entries.first { it.code == code }
    }
}

sealed interface AircraftEmitterCategory {
    val code: Int

    enum class Known(
        override val code: Int,
    ) : AircraftEmitterCategory {
        LIGHT_AIRCRAFT(1),
        MEDIUM_AIRCRAFT(3),
        HEAVY_AIRCRAFT(5),
        HIGHLY_MANOEUVRABLE_AND_HIGH_SPEED(6),
        ROTOCRAFT(10),
        GLIDER_SAILPLANE(11),
        LIGHTER_THAN_AIR(12),
        UNMANNED_AERIAL_VEHICLE(13),
        SPACE_TRANSATMOSPHERIC_VEHICLE(14),
        ULTRALIGHT_HANDGLIDER_PARAGLIDER(15),
        PARACHUTIST_SKYDIVER(16),
        SURFACE_EMERGENCY_VEHICLE(20),
        SURFACE_SERVICE_VEHICLE(21),
        FIXED_GROUND_OR_TETHERED_OBSTRUCTION(22),
    }

    data class Unknown(
        override val code: Int,
    ) : AircraftEmitterCategory

    companion object {
        fun fromCode(code: Int): AircraftEmitterCategory = Known.entries.firstOrNull { it.code == code } ?: Unknown(code)
    }
}

enum class PositionUncertaintyCategory(
    val code: Int,
) {
    CATEGORY_0(0),
    CATEGORY_1(1),
    CATEGORY_2(2),
    CATEGORY_3(3),
    CATEGORY_4(4),
    CATEGORY_5(5),
    CATEGORY_6(6),
    CATEGORY_7(7),
    CATEGORY_8(8),
    CATEGORY_9(9),
    CATEGORY_10(10),
    CATEGORY_11(11),
    CATEGORY_12(12),
    CATEGORY_13(13),
    CATEGORY_14(14),
    CATEGORY_15(15),
    ;

    companion object {
        fun fromCode(code: Int): PositionUncertaintyCategory = entries.first { it.code == code }
    }
}
