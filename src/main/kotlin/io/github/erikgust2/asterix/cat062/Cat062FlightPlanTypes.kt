package io.github.erikgust2.asterix.cat062

data class IfpsFlightId(
    val typeCode: IfpsFlightIdType,
    val number: Int,
)

data class FlightCategory(
    val gatOatCode: GatOatType,
    val flightRulesCode: FlightRulesType,
    val rvsmStatus: RvsmStatus,
    val hpr: Boolean,
)

enum class IfpsFlightIdType(
    val code: Int,
) {
    PLAN_NUMBER(0),
    UNIT_1_INTERNAL_FLIGHT_NUMBER(1),
    UNIT_2_INTERNAL_FLIGHT_NUMBER(2),
    UNIT_3_INTERNAL_FLIGHT_NUMBER(3),
    ;

    companion object {
        fun fromCode(code: Int): IfpsFlightIdType = entries.first { it.code == code }
    }
}

enum class GatOatType(
    val code: Int,
) {
    UNKNOWN(0),
    GENERAL_AIR_TRAFFIC(1),
    OPERATIONAL_AIR_TRAFFIC(2),
    NOT_APPLICABLE(3),
    ;

    companion object {
        fun fromCode(code: Int): GatOatType = entries.first { it.code == code }
    }
}

enum class FlightRulesType(
    val code: Int,
) {
    INSTRUMENT_FLIGHT_RULES(0),
    VISUAL_FLIGHT_RULES(1),
    NOT_APPLICABLE(2),
    CONTROLLED_VISUAL_FLIGHT_RULES(3),
    ;

    companion object {
        fun fromCode(code: Int): FlightRulesType = entries.first { it.code == code }
    }
}

enum class RvsmStatus(
    val code: Int,
) {
    UNKNOWN(0),
    APPROVED(1),
    EXEMPT(2),
    NOT_APPROVED(3),
    ;

    companion object {
        fun fromCode(code: Int): RvsmStatus = entries.first { it.code == code }
    }
}

data class ControlPosition(
    val centre: Int,
    val position: Int,
)

enum class RelativeDay(
    val code: Int,
) {
    TODAY(0),
    YESTERDAY(1),
    TOMORROW(2),
    INVALID(3),
    ;

    companion object {
        fun fromCode(code: Int): RelativeDay = entries.first { it.code == code }
    }
}

/**
 * Structured CAT062 I062/390 time of departure / arrival entry.
 *
 * `typeCode` preserves the spec's 5-bit code directly. `second == null`
 * represents "seconds not available" on the wire.
 */
data class TimeOfDepartureArrival(
    val typeCode: Int,
    val day: RelativeDay,
    val hour: Int,
    val minute: Int,
    val second: Int? = null,
)

data class StandStatus(
    val emp: StandOccupancyStatus,
    val avl: StandAvailabilityStatus,
)

enum class StandOccupancyStatus(
    val code: Int,
) {
    EMPTY(0),
    OCCUPIED(1),
    UNKNOWN(2),
    INVALID(3),
    ;

    companion object {
        fun fromCode(code: Int): StandOccupancyStatus = entries.first { it.code == code }
    }
}

enum class StandAvailabilityStatus(
    val code: Int,
) {
    AVAILABLE(0),
    NOT_AVAILABLE(1),
    UNKNOWN(2),
    INVALID(3),
    ;

    companion object {
        fun fromCode(code: Int): StandAvailabilityStatus = entries.first { it.code == code }
    }
}

data class PreEmergencyMode3a(
    val valid: Boolean,
    val code: Int,
)

data class FlightPlanRelatedData(
    val tag: DataSourceIdentifier? = null,
    val callsign: String? = null,
    val ifpsFlightId: IfpsFlightId? = null,
    val flightCategory: FlightCategory? = null,
    val aircraftType: String? = null,
    val wakeTurbulenceCategory: String? = null,
    val departureAerodrome: String? = null,
    val destinationAerodrome: String? = null,
    val runwayDesignation: String? = null,
    val currentClearedFlightLevel: Double? = null,
    val currentControlPosition: ControlPosition? = null,
    val timesOfDepartureArrival: List<TimeOfDepartureArrival>? = null,
    val aircraftStand: String? = null,
    val standStatus: StandStatus? = null,
    val standardInstrumentDeparture: String? = null,
    val standardInstrumentArrival: String? = null,
    val preEmergencyMode3a: PreEmergencyMode3a? = null,
    val preEmergencyCallsign: String? = null,
)
