package io.github.erikgust2.asterix.cat062

import java.nio.ByteBuffer

internal fun Cat062CodecSupport.readFlightPlanRelatedData(buffer: ByteBuffer): FlightPlanRelatedData =
    decodeCat062Item("I062/390", "flightPlanRelatedData") {
        val indicator = decodeCat062Item("I062/390", "flightPlanRelatedData.indicator") { readCompoundIndicator(buffer) }
        var tag: DataSourceIdentifier? = null
        var callsign: String? = null
        var ifpsFlightId: IfpsFlightId? = null
        var flightCategory: FlightCategory? = null
        var aircraftType: String? = null
        var wakeTurbulenceCategory: String? = null
        var departureAerodrome: String? = null
        var destinationAerodrome: String? = null
        var runwayDesignation: String? = null
        var currentClearedFlightLevel: Double? = null
        var currentControlPosition: ControlPosition? = null
        var timesOfDepartureArrival: List<TimeOfDepartureArrival>? = null
        var aircraftStand: String? = null
        var standStatus: StandStatus? = null
        var standardInstrumentDeparture: String? = null
        var standardInstrumentArrival: String? = null
        var preEmergencyMode3a: PreEmergencyMode3a? = null
        var preEmergencyCallsign: String? = null

        if (isCompoundSubfieldPresent(indicator, 1)) {
            tag =
                decodeCat062Item("I062/390", "flightPlanRelatedData.tag") { readDataSourceIdentifier(buffer) }
        }
        if (isCompoundSubfieldPresent(indicator, 2)) {
            callsign =
                decodeCat062Item("I062/390", "flightPlanRelatedData.callsign") { readAscii(buffer, 7).trim() }
        }
        if (isCompoundSubfieldPresent(indicator, 3)) {
            val raw = decodeCat062Item("I062/390", "flightPlanRelatedData.ifpsFlightId") { buffer.int }
            ifpsFlightId = IfpsFlightId(IfpsFlightIdType.fromCode(raw ushr 30), raw and 0x07FFFFFF)
        }
        if (isCompoundSubfieldPresent(indicator, 4)) {
            val octet = decodeCat062Item("I062/390", "flightPlanRelatedData.flightCategory") { buffer.get().toUnsignedInt() }
            flightCategory =
                FlightCategory(
                    gatOatCode = GatOatType.fromCode(octet ushr 6),
                    flightRulesCode = FlightRulesType.fromCode((octet ushr 4) and 0x03),
                    rvsmStatus = RvsmStatus.fromCode((octet ushr 2) and 0x03),
                    hpr = (octet and 0x02) != 0,
                )
        }
        if (isCompoundSubfieldPresent(indicator, 5)) {
            aircraftType =
                decodeCat062Item("I062/390", "flightPlanRelatedData.aircraftType") { readAscii(buffer, 4).trim() }
        }
        if (isCompoundSubfieldPresent(indicator, 6)) {
            wakeTurbulenceCategory =
                decodeCat062Item("I062/390", "flightPlanRelatedData.wakeTurbulenceCategory") { readAscii(buffer, 1) }
        }
        if (isCompoundSubfieldPresent(indicator, 7)) {
            departureAerodrome =
                decodeCat062Item("I062/390", "flightPlanRelatedData.departureAerodrome") { readAscii(buffer, 4).trim() }
        }
        if (isCompoundSubfieldPresent(indicator, 8)) {
            destinationAerodrome =
                decodeCat062Item("I062/390", "flightPlanRelatedData.destinationAerodrome") { readAscii(buffer, 4).trim() }
        }
        if (isCompoundSubfieldPresent(indicator, 9)) {
            runwayDesignation =
                decodeCat062Item("I062/390", "flightPlanRelatedData.runwayDesignation") { readAscii(buffer, 3).trim() }
        }
        if (isCompoundSubfieldPresent(indicator, 10)) {
            currentClearedFlightLevel =
                decodeCat062Item("I062/390", "flightPlanRelatedData.currentClearedFlightLevel") {
                    signExtend(buffer.short.toUnsignedInt(), 16) * 0.25
                }
        }
        if (isCompoundSubfieldPresent(indicator, 11)) {
            currentControlPosition =
                decodeCat062Item("I062/390", "flightPlanRelatedData.currentControlPosition") {
                    ControlPosition(buffer.get().toUnsignedInt(), buffer.get().toUnsignedInt())
                }
        }
        if (isCompoundSubfieldPresent(indicator, 12)) {
            val rep = decodeCat062Item("I062/390", "flightPlanRelatedData.timesOfDepartureArrival.size") { buffer.get().toUnsignedInt() }
            timesOfDepartureArrival =
                List(rep) { index ->
                    decodeCat062Item("I062/390", "flightPlanRelatedData.timesOfDepartureArrival[$index]") {
                        val b1 = buffer.get().toUnsignedInt()
                        val b2 = buffer.get().toUnsignedInt()
                        val b3 = buffer.get().toUnsignedInt()
                        val b4 = buffer.get().toUnsignedInt()
                        TimeOfDepartureArrival(
                            typeCode = b1 ushr 3,
                            day = RelativeDay.fromCode((b1 ushr 1) and 0x03),
                            hour = b2 and 0x1F,
                            minute = b3 and 0x3F,
                            second = if ((b4 and 0x80) != 0) null else b4 and 0x3F,
                        )
                    }
                }
        }
        if (isCompoundSubfieldPresent(indicator, 13)) {
            aircraftStand =
                decodeCat062Item("I062/390", "flightPlanRelatedData.aircraftStand") { readAscii(buffer, 6).trim() }
        }
        if (isCompoundSubfieldPresent(indicator, 14)) {
            val octet = decodeCat062Item("I062/390", "flightPlanRelatedData.standStatus") { buffer.get().toUnsignedInt() }
            standStatus =
                StandStatus(
                    StandOccupancyStatus.fromCode((octet ushr 6) and 0x03),
                    StandAvailabilityStatus.fromCode((octet ushr 4) and 0x03),
                )
        }
        if (isCompoundSubfieldPresent(indicator, 15)) {
            standardInstrumentDeparture =
                decodeCat062Item("I062/390", "flightPlanRelatedData.standardInstrumentDeparture") { readAscii(buffer, 7).trim() }
        }
        if (isCompoundSubfieldPresent(indicator, 16)) {
            standardInstrumentArrival =
                decodeCat062Item("I062/390", "flightPlanRelatedData.standardInstrumentArrival") { readAscii(buffer, 7).trim() }
        }
        if (isCompoundSubfieldPresent(indicator, 17)) {
            val raw = decodeCat062Item("I062/390", "flightPlanRelatedData.preEmergencyMode3a") { buffer.short.toUnsignedInt() }
            preEmergencyMode3a = PreEmergencyMode3a(valid = (raw and 0x1000) != 0, code = raw and 0x0FFF)
        }
        if (isCompoundSubfieldPresent(indicator, 18)) {
            preEmergencyCallsign =
                decodeCat062Item("I062/390", "flightPlanRelatedData.preEmergencyCallsign") { readAscii(buffer, 7).trim() }
        }

        FlightPlanRelatedData(
            tag = tag,
            callsign = callsign,
            ifpsFlightId = ifpsFlightId,
            flightCategory = flightCategory,
            aircraftType = aircraftType,
            wakeTurbulenceCategory = wakeTurbulenceCategory,
            departureAerodrome = departureAerodrome,
            destinationAerodrome = destinationAerodrome,
            runwayDesignation = runwayDesignation,
            currentClearedFlightLevel = currentClearedFlightLevel,
            currentControlPosition = currentControlPosition,
            timesOfDepartureArrival = timesOfDepartureArrival,
            aircraftStand = aircraftStand,
            standStatus = standStatus,
            standardInstrumentDeparture = standardInstrumentDeparture,
            standardInstrumentArrival = standardInstrumentArrival,
            preEmergencyMode3a = preEmergencyMode3a,
            preEmergencyCallsign = preEmergencyCallsign,
        )
    }

internal fun Cat062CodecSupport.writeFlightPlanRelatedData(
    buffer: ByteBuffer,
    value: FlightPlanRelatedData,
) {
    encodeCat062Item("I062/390") {
        val present = mutableSetOf<Int>()
        if (value.tag != null) present += 1
        if (value.callsign != null) present += 2
        if (value.ifpsFlightId != null) present += 3
        if (value.flightCategory != null) present += 4
        if (value.aircraftType != null) present += 5
        if (value.wakeTurbulenceCategory != null) present += 6
        if (value.departureAerodrome != null) present += 7
        if (value.destinationAerodrome != null) present += 8
        if (value.runwayDesignation != null) present += 9
        if (value.currentClearedFlightLevel != null) present += 10
        if (value.currentControlPosition != null) present += 11
        if (value.timesOfDepartureArrival != null) present += 12
        if (value.aircraftStand != null) present += 13
        if (value.standStatus != null) present += 14
        if (value.standardInstrumentDeparture != null) present += 15
        if (value.standardInstrumentArrival != null) present += 16
        if (value.preEmergencyMode3a != null) present += 17
        if (value.preEmergencyCallsign != null) present += 18
        writeCompoundIndicator(buffer, present)

        value.tag?.let { writeDataSourceIdentifier(buffer, it) }
        value.callsign?.let { writeAscii(buffer, it, 7) }
        value.ifpsFlightId?.let {
            require(it.number in 0..0x07FFFFFF) { "flightPlanRelatedData.ifpsFlightId.number out of range: ${it.number}" }
            buffer.putInt(((it.typeCode.code and 0x03) shl 30) or (it.number and 0x07FFFFFF))
        }
        value.flightCategory?.let {
            val octet =
                ((it.gatOatCode.code and 0x03) shl 6) or ((it.flightRulesCode.code and 0x03) shl 4) or
                    ((it.rvsmStatus.code and 0x03) shl 2) or (if (it.hpr) 0x02 else 0)
            buffer.put(octet.toByte())
        }
        value.aircraftType?.let { writeAscii(buffer, it, 4) }
        value.wakeTurbulenceCategory?.let { writeAscii(buffer, it, 1) }
        value.departureAerodrome?.let { writeAscii(buffer, it, 4) }
        value.destinationAerodrome?.let { writeAscii(buffer, it, 4) }
        value.runwayDesignation?.let { writeAscii(buffer, it, 3) }
        value.currentClearedFlightLevel?.let {
            buffer.putSignedShort(
                quantize(it, 0.25, "flightPlanRelatedData.currentClearedFlightLevel"),
                "flightPlanRelatedData.currentClearedFlightLevel",
            )
        }
        value.currentControlPosition?.let {
            buffer.putUnsignedByte(it.centre, "flightPlanRelatedData.currentControlPosition.centre")
            buffer.putUnsignedByte(it.position, "flightPlanRelatedData.currentControlPosition.position")
        }
        value.timesOfDepartureArrival?.let {
            buffer.putUnsignedByte(it.size, "flightPlanRelatedData.timesOfDepartureArrival.size")
            it.forEach { entry ->
                require(entry.typeCode in 0..0x1F) {
                    "flightPlanRelatedData.timesOfDepartureArrival.typeCode out of range: ${entry.typeCode}"
                }
                require(entry.hour in 0..23) {
                    "flightPlanRelatedData.timesOfDepartureArrival.hour out of range: ${entry.hour}"
                }
                require(entry.minute in 0..59) {
                    "flightPlanRelatedData.timesOfDepartureArrival.minute out of range: ${entry.minute}"
                }
                entry.second?.let { second ->
                    require(second in 0..59) {
                        "flightPlanRelatedData.timesOfDepartureArrival.second out of range: $second"
                    }
                }
                buffer.put((((entry.typeCode and 0x1F) shl 3) or ((entry.day.code and 0x03) shl 1)).toByte())
                buffer.put((entry.hour and 0x1F).toByte())
                buffer.put((entry.minute and 0x3F).toByte())
                val secondsOctet = (if (entry.second == null) 0x80 else 0) or ((entry.second ?: 0) and 0x3F)
                buffer.put(secondsOctet.toByte())
            }
        }
        value.aircraftStand?.let { writeAscii(buffer, it, 6) }
        value.standStatus?.let {
            buffer.put((((it.emp.code and 0x03) shl 6) or ((it.avl.code and 0x03) shl 4)).toByte())
        }
        value.standardInstrumentDeparture?.let { writeAscii(buffer, it, 7) }
        value.standardInstrumentArrival?.let { writeAscii(buffer, it, 7) }
        value.preEmergencyMode3a?.let {
            require(it.code in 0..0x0FFF) { "flightPlanRelatedData.preEmergencyMode3a.code out of range: ${it.code}" }
            val raw = (if (it.valid) 0x1000 else 0) or (it.code and 0x0FFF)
            buffer.putUnsignedShort(raw, "flightPlanRelatedData.preEmergencyMode3a")
        }
        value.preEmergencyCallsign?.let { writeAscii(buffer, it, 7) }
    }
}
