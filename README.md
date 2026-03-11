# asterix-cat062

Kotlin/JVM implementation of an ASTERIX CAT062 codec.

The project currently targets the CAT062 v1.15 system track format and provides
data classes plus binary read/write support for CAT062 data blocks and records.

## Requirements

- Java 21
- Maven 3.x

## Build

```bash
mvn test
```

## Usage

The main entry point is `Cat062Codec` in
`src/main/kotlin/com/erik/asterix/cat062/Cat062Codec.kt`.

Example shape:

```kotlin
import com.erik.asterix.cat062.Cat062Codec
import com.erik.asterix.cat062.Cat062Record
import java.nio.ByteBuffer

val buffer = ByteBuffer.allocate(1024)

Cat062Codec.writeRecord(
    buffer,
    Cat062Record(serviceIdentification = 4),
)

buffer.flip()
val decoded = Cat062Codec.readRecord(buffer)
```

## Project Layout

- `src/main/kotlin/com/erik/asterix/cat062`: codec and model types
- `src/test/kotlin/com/erik/asterix/cat062`: codec tests
- `cat062-asterix-system-track-data-part9-v1.15-20110901.pdf`: local reference spec

## Notes

The Maven artifact is `com.erik.asterix:asterix-cat062`.
