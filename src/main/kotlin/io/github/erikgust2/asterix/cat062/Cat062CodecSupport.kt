package io.github.erikgust2.asterix.cat062

import java.nio.ByteBuffer
internal class Cat062CodecSupport

internal fun Cat062CodecSupport.readRecord(buffer: ByteBuffer): Cat062Record {
    val presentFrns = readFspec(buffer)
    var record = Cat062Record()
    presentFrns.forEach { frn ->
        record = when (frn) {
            1 -> record.copy(dataSourceIdentifier = readDataSourceIdentifier(buffer))
            3 -> record.copy(serviceIdentification = buffer.get().toUnsignedInt())
            4 -> record.copy(timeOfTrackInformationSeconds = readUnsignedInt24(buffer) / 128.0)
            5 -> record.copy(calculatedTrackPositionWgs84 = readWgs84Position(buffer))
            6 -> record.copy(calculatedTrackPositionCartesian = readCartesianPosition(buffer))
            7 -> record.copy(calculatedTrackVelocityCartesian = readCartesianVelocity(buffer))
            8 -> record.copy(calculatedAccelerationCartesian = readCartesianAcceleration(buffer))
            9 -> record.copy(trackMode3aCode = readMode3ACode(buffer))
            10 -> record.copy(targetIdentification = readTargetIdentification(buffer))
            11 -> record.copy(aircraftDerivedData = readAircraftDerivedData(buffer))
            12 -> record.copy(trackNumber = buffer.short.toUnsignedInt())
            13 -> record.copy(trackStatus = readTrackStatus(buffer))
            14 -> record.copy(systemTrackUpdateAges = readSystemTrackUpdateAges(buffer))
            15 -> record.copy(modeOfMovement = readModeOfMovement(buffer))
            16 -> record.copy(trackDataAges = readTrackDataAges(buffer))
            17 -> record.copy(measuredFlightLevel = readFlightLevelMeasurement(buffer))
            18 -> record.copy(calculatedTrackGeometricAltitudeFeet = buffer.short.toDouble() * 6.25)
            19 -> record.copy(calculatedTrackBarometricAltitude = readBarometricAltitude(buffer))
            20 -> record.copy(rateOfClimbDescentFeetPerMinute = buffer.short.toDouble() * 6.25)
            21 -> record.copy(flightPlanRelatedData = readFlightPlanRelatedData(buffer))
            22 -> record.copy(targetSizeAndOrientation = readTargetSizeAndOrientation(buffer))
            23 -> record.copy(vehicleFleetIdentification = readVehicleFleetIdentification(buffer))
            24 -> record.copy(mode5DataReports = readMode5DataReports(buffer))
            25 -> record.copy(trackMode2Code = readMode2Code(buffer))
            26 -> record.copy(composedTrackNumber = readComposedTrackNumber(buffer))
            27 -> record.copy(estimatedAccuracies = readEstimatedAccuracies(buffer))
            28 -> record.copy(measuredInformation = readMeasuredInformation(buffer))
            34 -> record.copy(reservedExpansionField = readLengthPrefixedField(buffer))
            35 -> record.copy(specialPurposeField = readLengthPrefixedField(buffer))
            else -> error("Unsupported FRN $frn in CAT062")
        }
    }
    return record
}

internal fun Cat062CodecSupport.writeRecord(buffer: ByteBuffer, record: Cat062Record) {
    requireMandatoryItems(record)
    val frns = presentFrns(record)
    writeFspec(buffer, frns)
    frns.forEach { frn ->
        when (frn) {
            1 -> writeDataSourceIdentifier(buffer, record.dataSourceIdentifier!!)
            3 -> buffer.putUnsignedByte(record.serviceIdentification!!, "serviceIdentification")
            4 -> writeUnsignedInt24(buffer, quantize(record.timeOfTrackInformationSeconds!!, 1.0 / 128.0, "timeOfTrackInformationSeconds"))
            5 -> writeWgs84Position(buffer, record.calculatedTrackPositionWgs84!!)
            6 -> writeCartesianPosition(buffer, record.calculatedTrackPositionCartesian!!)
            7 -> writeCartesianVelocity(buffer, record.calculatedTrackVelocityCartesian!!)
            8 -> writeCartesianAcceleration(buffer, record.calculatedAccelerationCartesian!!)
            9 -> writeMode3ACode(buffer, record.trackMode3aCode!!)
            10 -> writeTargetIdentification(buffer, record.targetIdentification!!)
            11 -> writeAircraftDerivedData(buffer, record.aircraftDerivedData!!)
            12 -> buffer.putUnsignedShort(record.trackNumber!!, "trackNumber")
            13 -> writeTrackStatus(buffer, record.trackStatus!!)
            14 -> writeSystemTrackUpdateAges(buffer, record.systemTrackUpdateAges!!)
            15 -> writeModeOfMovement(buffer, record.modeOfMovement!!)
            16 -> writeTrackDataAges(buffer, record.trackDataAges!!)
            17 -> writeFlightLevelMeasurement(buffer, record.measuredFlightLevel!!)
            18 -> buffer.putSignedShort(quantize(record.calculatedTrackGeometricAltitudeFeet!!, 6.25, "calculatedTrackGeometricAltitudeFeet"), "calculatedTrackGeometricAltitudeFeet")
            19 -> writeBarometricAltitude(buffer, record.calculatedTrackBarometricAltitude!!)
            20 -> buffer.putSignedShort(quantize(record.rateOfClimbDescentFeetPerMinute!!, 6.25, "rateOfClimbDescentFeetPerMinute"), "rateOfClimbDescentFeetPerMinute")
            21 -> writeFlightPlanRelatedData(buffer, record.flightPlanRelatedData!!)
            22 -> writeTargetSizeAndOrientation(buffer, record.targetSizeAndOrientation!!)
            23 -> buffer.putUnsignedByte(record.vehicleFleetIdentification!!.code, "vehicleFleetIdentification.code")
            24 -> writeMode5DataReports(buffer, record.mode5DataReports!!)
            25 -> writeMode2Code(buffer, record.trackMode2Code!!)
            26 -> writeComposedTrackNumber(buffer, record.composedTrackNumber!!)
            27 -> writeEstimatedAccuracies(buffer, record.estimatedAccuracies!!)
            28 -> writeMeasuredInformation(buffer, record.measuredInformation!!)
            34 -> writeLengthPrefixedField(buffer, record.reservedExpansionField!!)
            35 -> writeLengthPrefixedField(buffer, record.specialPurposeField!!)
        }
    }
}

private fun requireMandatoryItems(record: Cat062Record) {
    require(record.dataSourceIdentifier != null) { "CAT062 record missing mandatory I062/010 dataSourceIdentifier" }
    require(record.trackNumber != null) { "CAT062 record missing mandatory I062/040 trackNumber" }
    require(record.timeOfTrackInformationSeconds != null) { "CAT062 record missing mandatory I062/070 timeOfTrackInformationSeconds" }
    require(record.trackStatus != null) { "CAT062 record missing mandatory I062/080 trackStatus" }
}

private fun Cat062CodecSupport.presentFrns(record: Cat062Record): List<Int> = buildList {
    if (record.dataSourceIdentifier != null) add(1)
    if (record.serviceIdentification != null) add(3)
    if (record.timeOfTrackInformationSeconds != null) add(4)
    if (record.calculatedTrackPositionWgs84 != null) add(5)
    if (record.calculatedTrackPositionCartesian != null) add(6)
    if (record.calculatedTrackVelocityCartesian != null) add(7)
    if (record.calculatedAccelerationCartesian != null) add(8)
    if (record.trackMode3aCode != null) add(9)
    if (record.targetIdentification != null) add(10)
    if (record.aircraftDerivedData != null) add(11)
    if (record.trackNumber != null) add(12)
    if (record.trackStatus != null) add(13)
    if (record.systemTrackUpdateAges != null) add(14)
    if (record.modeOfMovement != null) add(15)
    if (record.trackDataAges != null) add(16)
    if (record.measuredFlightLevel != null) add(17)
    if (record.calculatedTrackGeometricAltitudeFeet != null) add(18)
    if (record.calculatedTrackBarometricAltitude != null) add(19)
    if (record.rateOfClimbDescentFeetPerMinute != null) add(20)
    if (record.flightPlanRelatedData != null) add(21)
    if (record.targetSizeAndOrientation != null) add(22)
    if (record.vehicleFleetIdentification != null) add(23)
    if (record.mode5DataReports != null) add(24)
    if (record.trackMode2Code != null) add(25)
    if (record.composedTrackNumber != null) add(26)
    if (record.estimatedAccuracies != null) add(27)
    if (record.measuredInformation != null) add(28)
    if (record.reservedExpansionField != null) add(34)
    if (record.specialPurposeField != null) add(35)
}

private fun Cat062CodecSupport.readFspec(buffer: ByteBuffer): List<Int> {
    val result = mutableListOf<Int>()
    var frn = 1
    var octet: Int
    do {
        octet = buffer.get().toUnsignedInt()
        for (bit in 7 downTo 1) {
            if ((octet and (1 shl bit)) != 0) {
                result += frn
            }
            frn += 1
        }
    } while ((octet and 0x01) != 0)
    return result
}

private fun Cat062CodecSupport.writeFspec(buffer: ByteBuffer, frns: List<Int>) {
    val highestFrn = frns.maxOrNull() ?: return
    val octetCount = ((highestFrn - 1) / 7) + 1
    repeat(octetCount) { index ->
        var octet = 0
        for (bit in 0 until 7) {
            val frn = index * 7 + bit + 1
            if (frn in frns) {
                octet = octet or (1 shl (7 - bit))
            }
        }
        if (index < octetCount - 1) {
            octet = octet or 0x01
        }
        buffer.put(octet.toByte())
    }
}
