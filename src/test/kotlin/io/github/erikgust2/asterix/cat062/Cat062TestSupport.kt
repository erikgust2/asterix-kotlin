package io.github.erikgust2.asterix.cat062

import java.nio.ByteBuffer
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal fun minimalValidRecord(
    dataSourceIdentifier: DataSourceIdentifier = DataSourceIdentifier(1, 2),
    timeOfTrackInformationSeconds: Double = 128.0,
    trackNumber: Int = 42,
    trackStatus: TrackStatus = TrackStatus(mon = true, spi = false, mrh = false, src = TrackSource.TRIANGULATION, cnf = true),
): Cat062Record =
    Cat062Record(
        dataSourceIdentifier = dataSourceIdentifier,
        timeOfTrackInformationSeconds = timeOfTrackInformationSeconds,
        trackNumber = trackNumber,
        trackStatus = trackStatus,
    )

internal fun writeRecordBytes(record: Cat062Record): ByteArray {
    val buffer = ByteBuffer.allocate(1024)
    Cat062Codec.writeRecord(buffer, record)
    return buffer.usedBytes()
}

internal fun writeDataBlockBytes(vararg records: Cat062Record): ByteArray {
    val buffer = ByteBuffer.allocate(4096)
    Cat062Codec.writeDataBlock(buffer, Cat062DataBlock(records.toList()))
    return buffer.usedBytes()
}

internal fun writeFieldBytes(
    capacity: Int = 256,
    block: (ByteBuffer) -> Unit,
): ByteArray {
    val buffer = ByteBuffer.allocate(capacity)
    block(buffer)
    return buffer.usedBytes()
}

internal fun writeStandaloneRecordBytes(vararg fields: Pair<Int, ByteArray>): ByteArray {
    val buffer = ByteBuffer.allocate(512)
    val orderedFields = fields.sortedBy { it.first }
    writeFspecForTest(buffer, orderedFields.map { it.first })
    orderedFields.forEach { (_, payload) -> buffer.put(payload) }
    return buffer.usedBytes()
}

internal fun ByteBuffer.usedBytes(): ByteArray = array().copyOf(position())

internal fun byteArrayOfInts(vararg values: Int): ByteArray = ByteArray(values.size) { index -> values[index].toByte() }

internal fun bufferOf(vararg values: Int): ByteBuffer = ByteBuffer.wrap(byteArrayOfInts(*values))

internal fun truncated(
    bytes: ByteArray,
    droppedBytes: Int = 1,
): ByteArray {
    require(droppedBytes in 1 until bytes.size) { "Cannot drop $droppedBytes bytes from payload of length ${bytes.size}" }
    return bytes.copyOf(bytes.size - droppedBytes)
}

internal fun assertRangeFailure(
    expectedMessagePart: String,
    block: () -> Unit,
) {
    val error = assertFailsWith<IllegalArgumentException>(block = block)
    assertTrue(error.message?.contains(expectedMessagePart) == true, "Expected '$expectedMessagePart' in '${error.message}'")
}

private fun writeFspecForTest(
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
