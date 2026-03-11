package com.erik.asterix.cat062

data class IfpsFlightId(
    val typeCode: Int,
    val number: Int,
)

data class FlightCategory(
    val gatOatCode: Int,
    val flightRulesCode: Int,
    val rvsmStatus: Int,
    val hpr: Boolean,
)

data class ControlPosition(
    val centre: Int,
    val position: Int,
)

data class TimeOfDepartureArrival(
    val raw: RawBytes,
) {
    constructor(raw: ByteArray) : this(raw.toRawBytes())
}

data class StandStatus(
    val emp: Int,
    val avl: Int,
)

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
