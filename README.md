# asterix-kotlin

Kotlin/JVM codec for ASTERIX CAT062 System Track Data.

The project targets CAT062 v1.15 and provides:

- Kotlin model types for CAT062 records and compound items
- Typed enums and sealed `Known`/`Unknown(code)` models for spec-coded CAT062 fields
- `ByteBuffer`-based read/write support for CAT062 records and complete data blocks
- Round-trip tests for the current wire encoding

## Requirements

- Java 21
- Maven 3.x

## Build And Test

```bash
mvn test
```

To build the jar as well:

```bash
mvn package
```

To run the full local verification path used by CI:

```bash
mvn verify
```

To auto-format the Kotlin sources before committing:

```bash
mvn spotless:apply
```

## API Overview

The main entry point is `Cat062Codec`.

- `Cat062Codec.readDataBlock(buffer)` reads a full ASTERIX CAT062 data block
- `Cat062Codec.readDataBlock(bytes)` reads a full ASTERIX CAT062 data block from a `ByteArray`
- `Cat062Codec.writeDataBlock(buffer, block)` writes a full ASTERIX CAT062 data block
- `Cat062Codec.writeDataBlock(block)` writes a full ASTERIX CAT062 data block to a new `ByteArray`
- `Cat062Codec.readRecord(buffer)` reads a single CAT062 record
- `Cat062Codec.readRecord(bytes)` reads a single CAT062 record from a `ByteArray`
- `Cat062Codec.writeRecord(buffer, record)` writes a single CAT062 record
- `Cat062Codec.writeRecord(record)` writes a single CAT062 record to a new `ByteArray`

The writer uses `ByteBuffer` directly, so the caller is responsible for
allocating a large enough buffer and flipping it before reading.

The `ByteArray` overloads are convenience helpers. They wrap decode input in a
temporary `ByteBuffer`, and encode paths allocate an internal `ByteBuffer` plus
the returned `ByteArray`. For hot paths or buffer reuse, prefer the explicit
`ByteBuffer` methods.

## CAT062 Coverage Status

Supported CAT062 item coverage is tracked in `docs/TESTING_PLAN.md`.

Current explicit limitations:

- `RE` and `SP` are preserved as raw length-prefixed payloads
- spare FRNs `2`, `29`, `30`, `31`, `32`, and `33` are unsupported and rejected on decode

## Minimal Example

```kotlin
import io.github.erikgust2.asterix.cat062.Cat062DataBlock
import io.github.erikgust2.asterix.cat062.Cat062Codec
import io.github.erikgust2.asterix.cat062.Cat062Record
import io.github.erikgust2.asterix.cat062.DataSourceIdentifier
import io.github.erikgust2.asterix.cat062.TrackSource
import io.github.erikgust2.asterix.cat062.TrackStatus
import java.nio.ByteBuffer

val buffer = ByteBuffer.allocate(1024)
val record = Cat062Record(
    dataSourceIdentifier = DataSourceIdentifier(1, 2),
    serviceIdentification = 4,
    trackNumber = 42,
    timeOfTrackInformationSeconds = 12_345.0,
    trackStatus = TrackStatus(
        mon = true,
        spi = false,
        mrh = false,
        src = TrackSource.THREE_D_RADAR,
        cnf = true,
    ),
)

Cat062Codec.writeDataBlock(
    buffer,
    Cat062DataBlock(listOf(record)),
)

buffer.flip()
val decoded = Cat062Codec.readDataBlock(buffer)
```

## Convenience Example

```kotlin
import io.github.erikgust2.asterix.cat062.Cat062Codec
import io.github.erikgust2.asterix.cat062.Cat062Record
import io.github.erikgust2.asterix.cat062.DataSourceIdentifier
import io.github.erikgust2.asterix.cat062.TrackSource
import io.github.erikgust2.asterix.cat062.TrackStatus

val record = Cat062Record(
    dataSourceIdentifier = DataSourceIdentifier(1, 2),
    trackNumber = 42,
    timeOfTrackInformationSeconds = 12_345.0,
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

`writeRecord` enforces the CAT062 mandatory items from the v1.15 UAP:
`I062/010`, `I062/040`, `I062/070`, and `I062/080`.

For `I062/270` Target Size & Orientation, the model follows the wire extents:
`orientationDegrees = null` means length-only, `orientationDegrees != null`
with `widthMeters = null` means orientation is present without width, and
`widthMeters` requires `orientationDegrees`.

For semantic CAT062 code tables such as track source, report type, flight
category, selected-altitude source, and ADS-B / Mode-S status fields, the
public model now uses typed enums or sealed `Known` / `Unknown(code)` values
instead of raw integers.

Sparse code tables that may grow in future revisions, such as trajectory
intent point type in `I062/380`, use sealed `Known` / `Unknown(code)` models
so unknown decode values can still round-trip losslessly.

## Scope

This repository currently only supports CAT062 System Track Data, and is not yet a general ASTERIX
framework for multiple categories.

## Project Layout

- `src/main/kotlin/io/github/erikgust2/asterix/cat062`: codec and model types
- `src/test/kotlin/io/github/erikgust2/asterix/cat062`: codec tests
- `docs/ARCHITECTURE.md`: how the current codec is structured internally
- `cat062-asterix-system-track-data-part9-v1.15-20110901.pdf`: local reference spec

## Coordinates

- Group ID: `io.github.erikgust2`
- Artifact ID: `asterix-kotlin`
- Version: `0.1.0-SNAPSHOT`
