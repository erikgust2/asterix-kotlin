package io.github.erikgust2.asterix.cat062

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
    val typ: MeasuredReportType,
    val simulated: Boolean,
    val rab: Boolean,
    val testTarget: Boolean,
)

enum class MeasuredReportType(
    val code: Int,
) {
    NO_DETECTION(0),
    SINGLE_PSR_DETECTION(1),
    SINGLE_SSR_DETECTION(2),
    SSR_PSR_DETECTION(3),
    SINGLE_MODES_ALL_CALL(4),
    SINGLE_MODES_ROLL_CALL(5),
    MODES_ALL_CALL_PSR(6),
    MODES_ROLL_CALL_PSR(7),
    ;

    companion object {
        fun fromCode(code: Int): MeasuredReportType = entries.first { it.code == code }
    }
}

data class MeasuredInformation(
    val sensorIdentification: DataSourceIdentifier? = null,
    val position: PolarPosition? = null,
    val heightFeet: Double? = null,
    val lastMeasuredModeCCode: MeasuredModeCCode? = null,
    val lastMeasuredMode3aCode: MeasuredMode3ACode? = null,
    val reportType: ReportType? = null,
)
