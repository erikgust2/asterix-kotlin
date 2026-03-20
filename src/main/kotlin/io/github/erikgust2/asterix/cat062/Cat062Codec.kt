package io.github.erikgust2.asterix.cat062

import java.nio.BufferOverflowException
import java.nio.ByteBuffer

object Cat062Codec {
    const val CATEGORY: Int = 62

    private val support = Cat062CodecSupport()
    private const val INITIAL_RECORD_CAPACITY = 256
    private const val INITIAL_BLOCK_CAPACITY = 512

    /**
     * Reads a full CAT062 ASTERIX data block from [buffer].
     *
     * This is the lowest-allocation public decode path because the caller owns
     * the buffer and can reuse it across operations.
     */
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
            val recordNumber = records.size + 1
            try {
                records += readRecord(buffer)
            } catch (error: IllegalArgumentException) {
                throw IllegalArgumentException("CAT062 data block record $recordNumber: ${error.message}", error)
            } catch (error: IllegalStateException) {
                throw IllegalStateException("CAT062 data block record $recordNumber: ${error.message}", error)
            }
        }
        require(buffer.position() == blockEnd) { "CAT062 block parsing ended at ${buffer.position()} instead of $blockEnd" }
        return Cat062DataBlock(records)
    }

    /**
     * Reads a full CAT062 ASTERIX data block from [bytes].
     *
     * This convenience overload wraps the array in a temporary [ByteBuffer].
     * It avoids copying the payload bytes, but it still allocates the wrapper
     * object. Prefer [readDataBlock] with a caller-managed [ByteBuffer] in
     * allocation-sensitive code.
     */
    fun readDataBlock(bytes: ByteArray): Cat062DataBlock = readDataBlock(ByteBuffer.wrap(bytes))

    /**
     * Writes a full CAT062 ASTERIX data block into [buffer].
     *
     * This is the lowest-allocation public encode path because the caller owns
     * the destination buffer and can size or reuse it explicitly.
     */
    fun writeDataBlock(
        buffer: ByteBuffer,
        block: Cat062DataBlock,
    ) {
        val start = buffer.position()
        buffer.put(CATEGORY.toByte())
        buffer.putShort(0)
        block.records.forEach { writeRecord(buffer, it) }
        val length = buffer.position() - start
        require(length in 3..0xFFFF) { "ASTERIX block length out of range: $length" }
        buffer.putShort(start + 1, length.toShort())
    }

    /**
     * Writes a full CAT062 ASTERIX data block and returns the encoded bytes.
     *
     * This overload is easier to call, but it allocates an internal
     * [ByteBuffer] and a result [ByteArray]. Large payloads may also trigger
     * internal buffer growth retries. Prefer [writeDataBlock] with a
     * caller-managed [ByteBuffer] in performance-sensitive code.
     */
    fun writeDataBlock(block: Cat062DataBlock): ByteArray = writeToByteArray(INITIAL_BLOCK_CAPACITY) { writeDataBlock(it, block) }

    /**
     * Reads a single CAT062 record from [buffer].
     *
     * This is the lowest-allocation public decode path because the caller owns
     * the buffer and can reuse it across operations.
     */
    fun readRecord(buffer: ByteBuffer): Cat062Record = support.readRecord(buffer)

    /**
     * Reads a single CAT062 record from [bytes].
     *
     * This convenience overload wraps the array in a temporary [ByteBuffer].
     * It avoids copying the payload bytes, but it still allocates the wrapper
     * object. Prefer [readRecord] with a caller-managed [ByteBuffer] in
     * allocation-sensitive code.
     */
    fun readRecord(bytes: ByteArray): Cat062Record = readRecord(ByteBuffer.wrap(bytes))

    /**
     * Writes a single CAT062 record into [buffer].
     *
     * This is the lowest-allocation public encode path because the caller owns
     * the destination buffer and can size or reuse it explicitly.
     */
    fun writeRecord(
        buffer: ByteBuffer,
        record: Cat062Record,
    ) = support.writeRecord(buffer, record)

    /**
     * Writes a single CAT062 record and returns the encoded bytes.
     *
     * This overload is easier to call, but it allocates an internal
     * [ByteBuffer] and a result [ByteArray]. Large payloads may also trigger
     * internal buffer growth retries. Prefer [writeRecord] with a
     * caller-managed [ByteBuffer] in performance-sensitive code.
     */
    fun writeRecord(record: Cat062Record): ByteArray = writeToByteArray(INITIAL_RECORD_CAPACITY) { writeRecord(it, record) }

    private inline fun writeToByteArray(
        initialCapacity: Int,
        write: (ByteBuffer) -> Unit,
    ): ByteArray {
        var capacity = initialCapacity
        while (true) {
            val buffer = ByteBuffer.allocate(capacity)
            try {
                write(buffer)
                return buffer.array().copyOf(buffer.position())
            } catch (_: BufferOverflowException) {
                capacity = nextCapacity(capacity)
            }
        }
    }

    private fun nextCapacity(capacity: Int): Int {
        require(capacity <= Int.MAX_VALUE / 2) { "Encoded CAT062 payload exceeded supported buffer growth limit" }
        return capacity * 2
    }
}
