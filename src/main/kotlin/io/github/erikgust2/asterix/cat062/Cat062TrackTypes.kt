package io.github.erikgust2.asterix.cat062

data class TrackStatus(
    val mon: Boolean? = null,
    val spi: Boolean? = null,
    val mrh: Boolean? = null,
    val src: Int? = null,
    val cnf: Boolean? = null,
    val sim: Boolean? = null,
    val tse: Boolean? = null,
    val tsb: Boolean? = null,
    val fpc: Boolean? = null,
    val aff: Boolean? = null,
    val stp: Boolean? = null,
    val kos: Boolean? = null,
    val ama: Boolean? = null,
    val md4: Int? = null,
    val me: Boolean? = null,
    val mi: Boolean? = null,
    val md5: Int? = null,
    val cst: Boolean? = null,
    val psr: Boolean? = null,
    val ssr: Boolean? = null,
    val mds: Boolean? = null,
    val ads: Boolean? = null,
    val suc: Boolean? = null,
    val aac: Boolean? = null,
    val sds: Int? = null,
    val ems: Int? = null,
)

enum class SystemTrackAgeType {
    TRACK,
    PSR,
    SSR,
    MDS,
    ADS_C,
    ADS_ES,
    VDL,
    UAT,
    LOP,
    MLT,
}

data class SystemTrackUpdateAges(
    val agesSeconds: Map<SystemTrackAgeType, Double>,
)

enum class MovementAccelerationClass {
    CONSTANT_GROUND_SPEED,
    INCREASING_GROUND_SPEED,
    DECREASING_GROUND_SPEED,
    UNDETERMINED,
}

enum class VerticalMovementClass {
    LEVEL,
    CLIMB,
    DESCENT,
    UNDETERMINED,
}

enum class TransversalAccelerationClass {
    CONSTANT_COURSE,
    RIGHT_TURN,
    LEFT_TURN,
    UNDETERMINED,
}

data class ModeOfMovement(
    val transversalAccelerationClass: TransversalAccelerationClass,
    val longitudinalAccelerationClass: MovementAccelerationClass,
    val verticalMovementClass: VerticalMovementClass,
    val altitudeDiscrepancyFlag: Boolean,
)

enum class TrackDataAgeType {
    MFL,
    MD1,
    MD2,
    MDA,
    MD4,
    MD5,
    MHG,
    IAS,
    TAS,
    SAL,
    FSS,
    TID,
    COM,
    SAB,
    ACS,
    BVR,
    GVR,
    RAN,
    TAR,
    TAN,
    GSP,
    VUN,
    MET,
    EMC,
    POS,
    GAL,
    PUN,
    MB,
    IAR,
    MAC,
    BPS,
}

data class TrackDataAges(
    val agesSeconds: Map<TrackDataAgeType, Double>,
)

data class TargetSizeAndOrientation(
    val lengthMeters: Int,
    val orientationDegrees: Double? = null,
    val widthMeters: Int? = null,
)

data class CartesianAccuracy(
    val xMeters: Double,
    val yMeters: Double,
)

data class Wgs84Accuracy(
    val latitudeDegrees: Double,
    val longitudeDegrees: Double,
)

sealed interface VehicleFleetIdentification {
    val code: Int

    enum class Known(
        override val code: Int,
    ) : VehicleFleetIdentification {
        UNKNOWN(0),
        ATC_EQUIPMENT_MAINTENANCE(1),
        AIRPORT_MAINTENANCE(2),
        FIRE(3),
        BIRD_SCARER(4),
        SNOW_PLOUGH(5),
        RUNWAY_SWEEPER(6),
        EMERGENCY(7),
        POLICE(8),
        BUS(9),
        TUG_PUSHER(10),
        GRASS_CUTTER(11),
        FUEL(12),
        BAGGAGE(13),
        CATERING(14),
        AIRCRAFT_MAINTENANCE(15),
        FOLLOW_ME(16),
    }

    data class Unknown(
        override val code: Int,
    ) : VehicleFleetIdentification

    companion object {
        val UNKNOWN: VehicleFleetIdentification = Known.UNKNOWN
        val ATC_EQUIPMENT_MAINTENANCE: VehicleFleetIdentification = Known.ATC_EQUIPMENT_MAINTENANCE
        val AIRPORT_MAINTENANCE: VehicleFleetIdentification = Known.AIRPORT_MAINTENANCE
        val FIRE: VehicleFleetIdentification = Known.FIRE
        val BIRD_SCARER: VehicleFleetIdentification = Known.BIRD_SCARER
        val SNOW_PLOUGH: VehicleFleetIdentification = Known.SNOW_PLOUGH
        val RUNWAY_SWEEPER: VehicleFleetIdentification = Known.RUNWAY_SWEEPER
        val EMERGENCY: VehicleFleetIdentification = Known.EMERGENCY
        val POLICE: VehicleFleetIdentification = Known.POLICE
        val BUS: VehicleFleetIdentification = Known.BUS
        val TUG_PUSHER: VehicleFleetIdentification = Known.TUG_PUSHER
        val GRASS_CUTTER: VehicleFleetIdentification = Known.GRASS_CUTTER
        val FUEL: VehicleFleetIdentification = Known.FUEL
        val BAGGAGE: VehicleFleetIdentification = Known.BAGGAGE
        val CATERING: VehicleFleetIdentification = Known.CATERING
        val AIRCRAFT_MAINTENANCE: VehicleFleetIdentification = Known.AIRCRAFT_MAINTENANCE
        val FOLLOW_ME: VehicleFleetIdentification = Known.FOLLOW_ME

        fun fromCode(code: Int): VehicleFleetIdentification = Known.entries.firstOrNull { it.code == code } ?: Unknown(code)
    }
}

data class EstimatedAccuracies(
    val positionCartesian: CartesianAccuracy? = null,
    val xyCovarianceMeters: Double? = null,
    val positionWgs84: Wgs84Accuracy? = null,
    val geometricAltitudeFeet: Double? = null,
    val barometricAltitudeFeet: Double? = null,
    val trackVelocity: CartesianVelocity? = null,
    val trackAcceleration: CartesianAcceleration? = null,
    val rateOfClimbDescentFeetPerMinute: Double? = null,
)
