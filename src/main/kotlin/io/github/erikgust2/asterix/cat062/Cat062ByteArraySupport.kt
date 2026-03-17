package io.github.erikgust2.asterix.cat062

/**
 * Opaque binary payload with value semantics.
 *
 * This is used for pass-through CAT062 fields where the codec preserves the
 * bytes but does not yet expose a richer spec-level model.
 */
class RawBytes(
    bytes: ByteArray,
) {
    private val value = bytes.copyOf()

    val size: Int
        get() = value.size

    fun toByteArray(): ByteArray = value.copyOf()

    internal fun unsafeBytes(): ByteArray = value

    override fun equals(other: Any?): Boolean = other is RawBytes && value.contentEquals(other.value)

    override fun hashCode(): Int = value.contentHashCode()

    override fun toString(): String = value.contentToString()
}

fun ByteArray.toRawBytes(): RawBytes = RawBytes(this)
