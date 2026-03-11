package com.erik.asterix.cat062

class RawBytes(bytes: ByteArray) {
    private val value = bytes.copyOf()

    val size: Int
        get() = value.size

    fun toByteArray(): ByteArray = value.copyOf()

    internal fun unsafeBytes(): ByteArray = value

    override fun equals(other: Any?): Boolean =
        other is RawBytes && value.contentEquals(other.value)

    override fun hashCode(): Int = value.contentHashCode()

    override fun toString(): String = value.contentToString()
}

fun ByteArray.toRawBytes(): RawBytes = RawBytes(this)
