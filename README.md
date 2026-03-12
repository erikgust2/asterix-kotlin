# asterix-kotlin

Kotlin/JVM codec for ASTERIX CAT062 System Track Data.

The project targets CAT062 v1.15 and provides:

- Kotlin model types for CAT062 records and compound items
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

## API Overview

The main entry point is `Cat062Codec`.

- `Cat062Codec.readDataBlock(buffer)` reads a full ASTERIX CAT062 data block
- `Cat062Codec.writeDataBlock(buffer, block)` writes a full ASTERIX CAT062 data block
- `Cat062Codec.readRecord(buffer)` reads a single CAT062 record
- `Cat062Codec.writeRecord(buffer, record)` writes a single CAT062 record

The writer uses `ByteBuffer` directly, so the caller is responsible for
allocating a large enough buffer and flipping it before reading.

## Minimal Example

```kotlin
import io.github.erikgust2.asterix.cat062.Cat062DataBlock
import io.github.erikgust2.asterix.cat062.Cat062Codec
import io.github.erikgust2.asterix.cat062.Cat062Record
import io.github.erikgust2.asterix.cat062.DataSourceIdentifier
import io.github.erikgust2.asterix.cat062.TrackStatus
import java.nio.ByteBuffer

val buffer = ByteBuffer.allocate(1024)
val record = Cat062Record(
    dataSourceIdentifier = DataSourceIdentifier(1, 2),
    serviceIdentification = 4,
    trackNumber = 42,
    timeOfTrackInformationSeconds = 12_345.0,
    trackStatus = TrackStatus(),
)

Cat062Codec.writeDataBlock(
    buffer,
    Cat062DataBlock(listOf(record)),
)

buffer.flip()
val decoded = Cat062Codec.readDataBlock(buffer)
```

`writeRecord` enforces the CAT062 mandatory items from the v1.15 UAP:
`I062/010`, `I062/040`, `I062/070`, and `I062/080`.

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
