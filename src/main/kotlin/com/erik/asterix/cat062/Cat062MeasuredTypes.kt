package com.erik.asterix.cat062

data class PolarPosition(
    val rangeNm: Double,
    val azimuthDegrees: Double,
)

data class MeasuredModeCCode(
    val validated: Boolean,
    val garbled: Boolean,
    val flightLevel: Double,
)

data class MeasuredMode3ACode(
    val code: Int,
    val validated: Boolean,
    val garbled: Boolean,
    val smoothed: Boolean,
)

data class ReportType(
    val typ: Int,
    val simulated: Boolean,
    val rab: Boolean,
    val testTarget: Boolean,
)

data class MeasuredInformation(
    val sensorIdentification: DataSourceIdentifier? = null,
    val position: PolarPosition? = null,
    val heightFeet: Double? = null,
    val lastMeasuredModeCCode: MeasuredModeCCode? = null,
    val lastMeasuredMode3aCode: MeasuredMode3ACode? = null,
    val reportType: ReportType? = null,
)
