package io.github.erikgust2.asterix.cat062

data class Cat062DataBlock(
    val records: List<Cat062Record>,
)

data class Cat062Record(
    val dataSourceIdentifier: DataSourceIdentifier? = null,
    val serviceIdentification: Int? = null,
    val timeOfTrackInformationSeconds: Double? = null,
    val calculatedTrackPositionWgs84: Wgs84Position? = null,
    val calculatedTrackPositionCartesian: CartesianPosition? = null,
    val calculatedTrackVelocityCartesian: CartesianVelocity? = null,
    val calculatedAccelerationCartesian: CartesianAcceleration? = null,
    val trackMode3aCode: Mode3ACode? = null,
    val targetIdentification: TargetIdentification? = null,
    val aircraftDerivedData: AircraftDerivedData? = null,
    val trackNumber: Int? = null,
    val trackStatus: TrackStatus? = null,
    val systemTrackUpdateAges: SystemTrackUpdateAges? = null,
    val modeOfMovement: ModeOfMovement? = null,
    val trackDataAges: TrackDataAges? = null,
    val measuredFlightLevel: FlightLevelMeasurement? = null,
    val calculatedTrackGeometricAltitudeFeet: Double? = null,
    val calculatedTrackBarometricAltitude: BarometricAltitude? = null,
    val rateOfClimbDescentFeetPerMinute: Double? = null,
    val flightPlanRelatedData: FlightPlanRelatedData? = null,
    val targetSizeAndOrientation: TargetSizeAndOrientation? = null,
    val vehicleFleetIdentification: VehicleFleetIdentification? = null,
    val mode5DataReports: Mode5DataReports? = null,
    val trackMode2Code: Mode2Code? = null,
    val composedTrackNumber: ComposedTrackNumber? = null,
    val estimatedAccuracies: EstimatedAccuracies? = null,
    val measuredInformation: MeasuredInformation? = null,
    val reservedExpansionField: RawBytes? = null,
    val specialPurposeField: RawBytes? = null,
)
