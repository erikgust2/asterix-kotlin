package io.github.erikgust2.asterix.cat062

import java.nio.ByteBuffer

object Cat062Codec {
    const val CATEGORY: Int = 62

    private val support = Cat062CodecSupport()

    fun readDataBlock(buffer: ByteBuffer): Cat062DataBlock {
        val category = buffer.get().toUnsignedInt()
        require(category == CATEGORY) { "Expected ASTERIX category $CATEGORY but found $category" }
        val blockLength = buffer.short.toUnsignedInt()
        require(blockLength >= 3) { "Invalid ASTERIX block length $blockLength" }
        val blockEnd = buffer.position() + blockLength - 3
        require(blockEnd <= buffer.limit()) {
            "CAT062 block length $blockLength exceeds remaining buffer ${buffer.remaining() + 3}"
        }
        val records = mutableListOf<Cat062Record>()
        while (buffer.position() < blockEnd) {
            records += readRecord(buffer)
        }
        require(buffer.position() == blockEnd) { "CAT062 block parsing ended at ${buffer.position()} instead of $blockEnd" }
        return Cat062DataBlock(records)
    }

    fun writeDataBlock(buffer: ByteBuffer, block: Cat062DataBlock) {
        val start = buffer.position()
        buffer.put(CATEGORY.toByte())
        buffer.putShort(0)
        block.records.forEach { writeRecord(buffer, it) }
        val length = buffer.position() - start
        require(length in 3..0xFFFF) { "ASTERIX block length out of range: $length" }
        buffer.putShort(start + 1, length.toShort())
    }

    fun readRecord(buffer: ByteBuffer): Cat062Record = support.readRecord(buffer)

    fun writeRecord(buffer: ByteBuffer, record: Cat062Record) = support.writeRecord(buffer, record)
}
