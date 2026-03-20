package io.github.erikgust2.asterix.cat062

import java.nio.BufferUnderflowException
import java.nio.ByteBuffer

internal class Cat062CodecSupport

internal fun Cat062CodecSupport.readRecord(buffer: ByteBuffer): Cat062Record {
    val presentFrns =
        try {
            readFspec(buffer)
        } catch (error: BufferUnderflowException) {
            throw IllegalArgumentException("Truncated CAT062 FSPEC payload", error)
        }
    var record = Cat062Record()
    presentFrns.forEach { frn ->
        record =
            when (frn) {
                1 ->
                    decodeCat062Item(
                        "I062/010",
                        "dataSourceIdentifier",
                    ) { record.copy(dataSourceIdentifier = readDataSourceIdentifier(buffer)) }
                3 ->
                    decodeCat062Item(
                        "I062/015",
                        "serviceIdentification",
                    ) { record.copy(serviceIdentification = buffer.get().toUnsignedInt()) }
                4 ->
                    decodeCat062Item("I062/070", "timeOfTrackInformationSeconds") {
                        record.copy(timeOfTrackInformationSeconds = readUnsignedInt24(buffer) / 128.0)
                    }
                5 ->
                    decodeCat062Item(
                        "I062/105",
                        "calculatedTrackPositionWgs84",
                    ) { record.copy(calculatedTrackPositionWgs84 = readWgs84Position(buffer)) }
                6 ->
                    decodeCat062Item("I062/100", "calculatedTrackPositionCartesian") {
                        record.copy(calculatedTrackPositionCartesian = readCartesianPosition(buffer))
                    }
                7 ->
                    decodeCat062Item("I062/185", "calculatedTrackVelocityCartesian") {
                        record.copy(calculatedTrackVelocityCartesian = readCartesianVelocity(buffer))
                    }
                8 ->
                    decodeCat062Item("I062/210", "calculatedAccelerationCartesian") {
                        record.copy(calculatedAccelerationCartesian = readCartesianAcceleration(buffer))
                    }
                9 -> decodeCat062Item("I062/060", "trackMode3aCode") { record.copy(trackMode3aCode = readMode3ACode(buffer)) }
                10 ->
                    decodeCat062Item(
                        "I062/245",
                        "targetIdentification",
                    ) { record.copy(targetIdentification = readTargetIdentification(buffer)) }
                11 ->
                    decodeCat062Item(
                        "I062/380",
                        "aircraftDerivedData",
                    ) { record.copy(aircraftDerivedData = readAircraftDerivedData(buffer)) }
                12 -> decodeCat062Item("I062/040", "trackNumber") { record.copy(trackNumber = buffer.short.toUnsignedInt()) }
                13 -> decodeCat062Item("I062/080", "trackStatus") { record.copy(trackStatus = readTrackStatus(buffer)) }
                14 ->
                    decodeCat062Item(
                        "I062/290",
                        "systemTrackUpdateAges",
                    ) { record.copy(systemTrackUpdateAges = readSystemTrackUpdateAges(buffer)) }
                15 -> decodeCat062Item("I062/200", "modeOfMovement") { record.copy(modeOfMovement = readModeOfMovement(buffer)) }
                16 -> decodeCat062Item("I062/295", "trackDataAges") { record.copy(trackDataAges = readTrackDataAges(buffer)) }
                17 ->
                    decodeCat062Item(
                        "I062/136",
                        "measuredFlightLevel",
                    ) { record.copy(measuredFlightLevel = readFlightLevelMeasurement(buffer)) }
                18 ->
                    decodeCat062Item("I062/130", "calculatedTrackGeometricAltitudeFeet") {
                        record.copy(calculatedTrackGeometricAltitudeFeet = buffer.short.toDouble() * 6.25)
                    }
                19 ->
                    decodeCat062Item("I062/135", "calculatedTrackBarometricAltitude") {
                        record.copy(calculatedTrackBarometricAltitude = readBarometricAltitude(buffer))
                    }
                20 ->
                    decodeCat062Item("I062/220", "rateOfClimbDescentFeetPerMinute") {
                        record.copy(rateOfClimbDescentFeetPerMinute = buffer.short.toDouble() * 6.25)
                    }
                21 ->
                    decodeCat062Item(
                        "I062/390",
                        "flightPlanRelatedData",
                    ) { record.copy(flightPlanRelatedData = readFlightPlanRelatedData(buffer)) }
                22 ->
                    decodeCat062Item("I062/270", "targetSizeAndOrientation") {
                        record.copy(targetSizeAndOrientation = readTargetSizeAndOrientation(buffer))
                    }
                23 ->
                    decodeCat062Item("I062/300", "vehicleFleetIdentification") {
                        record.copy(vehicleFleetIdentification = readVehicleFleetIdentification(buffer))
                    }
                24 -> decodeCat062Item("I062/110", "mode5DataReports") { record.copy(mode5DataReports = readMode5DataReports(buffer)) }
                25 -> decodeCat062Item("I062/120", "trackMode2Code") { record.copy(trackMode2Code = readMode2Code(buffer)) }
                26 ->
                    decodeCat062Item(
                        "I062/510",
                        "composedTrackNumber",
                    ) { record.copy(composedTrackNumber = readComposedTrackNumber(buffer)) }
                27 ->
                    decodeCat062Item(
                        "I062/500",
                        "estimatedAccuracies",
                    ) { record.copy(estimatedAccuracies = readEstimatedAccuracies(buffer)) }
                28 ->
                    decodeCat062Item(
                        "I062/340",
                        "measuredInformation",
                    ) { record.copy(measuredInformation = readMeasuredInformation(buffer)) }
                34 ->
                    decodeCat062Item(
                        null,
                        "reservedExpansionField",
                    ) { record.copy(reservedExpansionField = readLengthPrefixedField(buffer)) }
                35 -> decodeCat062Item(null, "specialPurposeField") { record.copy(specialPurposeField = readLengthPrefixedField(buffer)) }
                else -> error("Unsupported FRN $frn in CAT062")
            }
    }
    return record
}

internal fun Cat062CodecSupport.writeRecord(
    buffer: ByteBuffer,
    record: Cat062Record,
) {
    requireMandatoryItems(record)
    val frns = presentFrns(record)
    writeFspec(buffer, frns)
    frns.forEach { frn ->
        when (frn) {
            1 -> encodeCat062Item("I062/010") { writeDataSourceIdentifier(buffer, record.dataSourceIdentifier!!) }
            3 -> encodeCat062Item("I062/015") { buffer.putUnsignedByte(record.serviceIdentification!!, "serviceIdentification") }
            4 ->
                encodeCat062Item("I062/070") {
                    writeUnsignedInt24(
                        buffer,
                        quantize(record.timeOfTrackInformationSeconds!!, 1.0 / 128.0, "timeOfTrackInformationSeconds"),
                        "timeOfTrackInformationSeconds",
                    )
                }
            5 -> encodeCat062Item("I062/105") { writeWgs84Position(buffer, record.calculatedTrackPositionWgs84!!) }
            6 -> encodeCat062Item("I062/100") { writeCartesianPosition(buffer, record.calculatedTrackPositionCartesian!!) }
            7 -> encodeCat062Item("I062/185") { writeCartesianVelocity(buffer, record.calculatedTrackVelocityCartesian!!) }
            8 -> encodeCat062Item("I062/210") { writeCartesianAcceleration(buffer, record.calculatedAccelerationCartesian!!) }
            9 -> encodeCat062Item("I062/060") { writeMode3ACode(buffer, record.trackMode3aCode!!) }
            10 -> encodeCat062Item("I062/245") { writeTargetIdentification(buffer, record.targetIdentification!!) }
            11 -> encodeCat062Item("I062/380") { writeAircraftDerivedData(buffer, record.aircraftDerivedData!!) }
            12 -> encodeCat062Item("I062/040") { buffer.putUnsignedShort(record.trackNumber!!, "trackNumber") }
            13 -> encodeCat062Item("I062/080") { writeTrackStatus(buffer, record.trackStatus!!) }
            14 -> encodeCat062Item("I062/290") { writeSystemTrackUpdateAges(buffer, record.systemTrackUpdateAges!!) }
            15 -> encodeCat062Item("I062/200") { writeModeOfMovement(buffer, record.modeOfMovement!!) }
            16 -> encodeCat062Item("I062/295") { writeTrackDataAges(buffer, record.trackDataAges!!) }
            17 -> encodeCat062Item("I062/136") { writeFlightLevelMeasurement(buffer, record.measuredFlightLevel!!) }
            18 ->
                encodeCat062Item("I062/130") {
                    buffer.putSignedShort(
                        quantize(record.calculatedTrackGeometricAltitudeFeet!!, 6.25, "calculatedTrackGeometricAltitudeFeet"),
                        "calculatedTrackGeometricAltitudeFeet",
                    )
                }
            19 -> encodeCat062Item("I062/135") { writeBarometricAltitude(buffer, record.calculatedTrackBarometricAltitude!!) }
            20 ->
                encodeCat062Item("I062/220") {
                    buffer.putSignedShort(
                        quantize(record.rateOfClimbDescentFeetPerMinute!!, 6.25, "rateOfClimbDescentFeetPerMinute"),
                        "rateOfClimbDescentFeetPerMinute",
                    )
                }
            21 -> encodeCat062Item("I062/390") { writeFlightPlanRelatedData(buffer, record.flightPlanRelatedData!!) }
            22 -> encodeCat062Item("I062/270") { writeTargetSizeAndOrientation(buffer, record.targetSizeAndOrientation!!) }
            23 ->
                encodeCat062Item(
                    "I062/300",
                ) { buffer.putUnsignedByte(record.vehicleFleetIdentification!!.code, "vehicleFleetIdentification.code") }
            24 -> encodeCat062Item("I062/110") { writeMode5DataReports(buffer, record.mode5DataReports!!) }
            25 -> encodeCat062Item("I062/120") { writeMode2Code(buffer, record.trackMode2Code!!) }
            26 -> encodeCat062Item("I062/510") { writeComposedTrackNumber(buffer, record.composedTrackNumber!!) }
            27 -> encodeCat062Item("I062/500") { writeEstimatedAccuracies(buffer, record.estimatedAccuracies!!) }
            28 -> encodeCat062Item("I062/340") { writeMeasuredInformation(buffer, record.measuredInformation!!) }
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

private fun Cat062CodecSupport.presentFrns(record: Cat062Record): List<Int> =
    buildList {
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

private fun Cat062CodecSupport.writeFspec(
    buffer: ByteBuffer,
    frns: List<Int>,
) {
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

internal inline fun <T> decodeCat062Item(
    itemReference: String?,
    fieldPath: String,
    block: () -> T,
): T =
    try {
        block()
    } catch (error: BufferUnderflowException) {
        throw IllegalArgumentException(truncatedCat062Message(itemReference, fieldPath), error)
    } catch (error: IllegalArgumentException) {
        throw prefixCat062Exception(error, itemReference)
    } catch (error: IllegalStateException) {
        throw prefixCat062Exception(error, itemReference)
    }

internal inline fun <T> encodeCat062Item(
    itemReference: String,
    block: () -> T,
): T =
    try {
        block()
    } catch (error: IllegalArgumentException) {
        throw prefixCat062Exception(error, itemReference)
    } catch (error: IllegalStateException) {
        throw prefixCat062Exception(error, itemReference)
    }

private fun truncatedCat062Message(
    itemReference: String?,
    fieldPath: String,
): String =
    buildString {
        append("Truncated ")
        if (itemReference != null) {
            append(itemReference)
            append(' ')
        }
        append(fieldPath)
        append(" payload")
    }

private fun prefixCat062Exception(
    error: RuntimeException,
    itemReference: String?,
): RuntimeException {
    val message = error.message ?: return error
    if (itemReference == null || message.contains(itemReference)) return error
    val prefixedMessage = "$itemReference $message"
    return when (error) {
        is IllegalStateException -> IllegalStateException(prefixedMessage, error)
        else -> IllegalArgumentException(prefixedMessage, error)
    }
}
