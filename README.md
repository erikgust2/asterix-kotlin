# asterix-kotlin

`asterix-kotlin` is a Kotlin/JVM codec for ASTERIX CAT062 System Track Data.

The repository is intentionally narrow:

- one ASTERIX category: CAT062
- one target revision: CAT062 v1.15
- one public entry point: `Cat062Codec`
- one package namespace: `io.github.erikgust2.asterix.cat062`

The CAT062 v1.15 PDF in the repository root,
`cat062-asterix-system-track-data-part9-v1.15-20110901.pdf`, is the wire
layout source of truth for item structure, scaling, extents, and UAP behavior.

## What The Library Provides

- `ByteBuffer`-oriented read and write APIs for CAT062 records and full
  ASTERIX data blocks
- convenience `ByteArray` overloads for the same operations
- Kotlin model types for the implemented CAT062 items and compound structures
- typed enums and sealed `Known` / `Unknown(code)` models for many CAT062 code
  tables
- write-side validation for mandatory CAT062 items and item-specific wire
  constraints
- a spec-driven regression suite with golden vectors, round-trips, and
  malformed-input coverage

## Documentation Map

- [`docs/HOW_CAT062_WORKS.md`](docs/HOW_CAT062_WORKS.md): practical CAT062 and
  ASTERIX explainer, including FSPEC, FRNs, UAP order, and a worked example
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md): how the codec is structured
  internally and where to make changes
- [`docs/TESTING_PLAN.md`](docs/TESTING_PLAN.md): implemented CAT062 coverage
  map and verification expectations

## Requirements

- Java 21
- Maven 3.8.7 or newer
- Kotlin 2.1.20 via the Maven build

## Build And Verify

Run the standard local checks:

```bash
mvn test
mvn verify
mvn spotless:apply
```

To produce the jar:

```bash
mvn package
```

If you want a clean local Maven repository for repeatable runs, the test plan
documents the `/tmp/m2` commands used in local verification.

## Public API

`Cat062Codec` exposes eight operations:

- `readDataBlock(buffer: ByteBuffer): Cat062DataBlock`
- `readDataBlock(bytes: ByteArray): Cat062DataBlock`
- `writeDataBlock(buffer: ByteBuffer, block: Cat062DataBlock)`
- `writeDataBlock(block: Cat062DataBlock): ByteArray`
- `readRecord(buffer: ByteBuffer): Cat062Record`
- `readRecord(bytes: ByteArray): Cat062Record`
- `writeRecord(buffer: ByteBuffer, record: Cat062Record)`
- `writeRecord(record: Cat062Record): ByteArray`

The `ByteBuffer` methods are the core, lower-allocation path. The `ByteArray`
overloads wrap or allocate buffers for convenience.

Decode and validation failures include CAT062 item references such as
`I062/080` where the codec can identify the failing item. Nested failures while
reading a full data block also include the one-based record ordinal.

## Minimal Example

`writeRecord` enforces the CAT062 mandatory items from the v1.15 UAP:
`I062/010`, `I062/040`, `I062/070`, and `I062/080`.

```kotlin
import io.github.erikgust2.asterix.cat062.Cat062Codec
import io.github.erikgust2.asterix.cat062.Cat062Record
import io.github.erikgust2.asterix.cat062.DataSourceIdentifier
import io.github.erikgust2.asterix.cat062.TrackSource
import io.github.erikgust2.asterix.cat062.TrackStatus

val record = Cat062Record(
    dataSourceIdentifier = DataSourceIdentifier(1, 2),
    timeOfTrackInformationSeconds = 12_345.0,
    trackNumber = 42,
    trackStatus = TrackStatus(
        mon = true,
        spi = false,
        mrh = false,
        src = TrackSource.THREE_D_RADAR,
        cnf = true,
    ),
)

val bytes = Cat062Codec.writeRecord(record)
val decoded = Cat062Codec.readRecord(bytes)
```

For higher-throughput paths, manage the destination buffer directly:

```kotlin
import io.github.erikgust2.asterix.cat062.Cat062Codec
import io.github.erikgust2.asterix.cat062.Cat062DataBlock
import java.nio.ByteBuffer

val buffer = ByteBuffer.allocate(1024)
Cat062Codec.writeDataBlock(buffer, Cat062DataBlock(listOf(record)))
buffer.flip()

val decodedBlock = Cat062Codec.readDataBlock(buffer)
```

## CAT062 Model Notes

`Cat062Record` is a sparse aggregate model. Most fields are nullable, and
`null` generally means the corresponding CAT062 item is absent from the record.

Important semantics to know:

- `TrackStatus` models CAT062 `I062/080` extents explicitly. Octet 1 is always
  required when the item is present, and later extents must be complete rather
  than partially null.
- `TargetSizeAndOrientation` models `I062/270` extent presence directly:
  `orientationDegrees == null` means length only, and `widthMeters` requires
  `orientationDegrees`.
- `RE` and `SP` are preserved as raw length-prefixed payloads through
  `RawBytes`. They round-trip byte-for-byte, but the library does not yet
  expose richer spec-level models for their contents.
- Several code tables use sealed `Known` / `Unknown(code)` models so unknown
  wire values can still be decoded and re-encoded losslessly.

## Current Support Boundary

The codec currently implements the non-spare UAP items represented in
`Cat062Record` and its related type files. The test coverage map in
[`docs/TESTING_PLAN.md`](docs/TESTING_PLAN.md) is the authoritative list of
implemented FRNs and their current regression coverage.

Current explicit limitations:

- spare FRNs `2`, `29`, `30`, `31`, `32`, and `33` are unsupported and rejected
  on decode
- `RE` and `SP` are intentionally opaque pass-through payloads
- this repository is CAT062-only, not a general multi-category ASTERIX
  framework

## Project Layout

- `src/main/kotlin/io/github/erikgust2/asterix/cat062`: codec and model types
- `src/test/kotlin/io/github/erikgust2/asterix/cat062`: spec-driven tests
- `docs/ARCHITECTURE.md`: internal structure and extension guidance
- `docs/TESTING_PLAN.md`: active coverage map
- `docs/HOW_CAT062_WORKS.md`: CAT062 explainer and worked wire example
- `cat062-asterix-system-track-data-part9-v1.15-20110901.pdf`: local reference
  specification

## Coordinates

- Group ID: `io.github.erikgust2`
- Artifact ID: `asterix-kotlin`
- Version: `0.1.0-SNAPSHOT`
