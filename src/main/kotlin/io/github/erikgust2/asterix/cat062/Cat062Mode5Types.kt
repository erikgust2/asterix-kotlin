package io.github.erikgust2.asterix.cat062

data class Mode5Summary(
    val m5: Boolean,
    val id: Boolean,
    val da: Boolean,
    val m1: Boolean,
    val m2: Boolean,
    val m3: Boolean,
    val mc: Boolean,
    val x: Boolean,
)

data class PinNationalOriginMission(
    val pin: Int,
    val nationalOrigin: Int,
    val missionCode: Int,
)

data class ExtendedMode1Code(
    val code: Int,
)

data class Mode5XPulsePresence(
    val x5: Boolean,
    val xc: Boolean,
    val x3: Boolean,
    val x2: Boolean,
    val x1: Boolean,
)

data class Mode5DataReports(
    val summary: Mode5Summary? = null,
    val pinNationalOriginMission: PinNationalOriginMission? = null,
    val positionWgs84: Wgs84Position? = null,
    val geometricAltitudeFeet: Double? = null,
    val extendedMode1Code: ExtendedMode1Code? = null,
    val timeOffsetSeconds: Double? = null,
    val xPulsePresence: Mode5XPulsePresence? = null,
)
