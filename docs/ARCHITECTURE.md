# Architecture

This document describes how the current `asterix-cat062` codebase is organized,
how data flows through it, and where to make changes when extending the codec.

## Scope

The repository is a Kotlin/JVM codec for ASTERIX CAT062 System Track Data,
currently aligned to the local CAT062 v1.15 reference PDF in the repository
root.

It is intentionally narrow in scope:

- one ASTERIX category: CAT062
- one Maven module
- one public entry point: `Cat062Codec`
- one model namespace: `io.github.erikgust2.asterix.cat062`

It is not yet a general multi-category ASTERIX framework.

## High-Level Structure

The important code lives under:

- `src/main/kotlin/io/github/erikgust2/asterix/cat062`
- `src/test/kotlin/io/github/erikgust2/asterix/cat062`

The main files and responsibilities are:

- `Cat062Codec.kt`
  - public API
  - reads and writes whole CAT062 data blocks and single records
- `Cat062CodecSupport.kt`
  - record-level orchestration
  - FSPEC parsing and writing
  - FRN-to-field dispatch
  - mandatory field enforcement on write
- `Cat062CodecWire.kt`
  - low-level wire primitives
  - fixed-length field codecs
  - integer packing helpers
  - length-prefixed field handling
- `Cat062CodecCompounds.kt`
  - compound and variable-length item codecs
  - currently the largest and most complex file in the repository
- `Cat062Record.kt`
  - top-level `Cat062Record` and `Cat062DataBlock` models
- `Cat062*Types.kt`
  - model types grouped by domain
  - common, track, aircraft-derived, measured, flight plan, mode 5, etc.
- `Cat062CodecTest.kt`
  - focused codec tests and regression coverage

## Public API

The public entry point is `Cat062Codec`.

It exposes four operations:

- `readDataBlock(buffer: ByteBuffer): Cat062DataBlock`
- `writeDataBlock(buffer: ByteBuffer, block: Cat062DataBlock)`
- `readRecord(buffer: ByteBuffer): Cat062Record`
- `writeRecord(buffer: ByteBuffer, record: Cat062Record)`

The API is `ByteBuffer`-oriented. The library does not currently expose
`ByteArray` convenience wrappers, stream adapters, or builders.

## Data Flow

### Reading

Reading a full CAT062 block works like this:

1. `Cat062Codec.readDataBlock` verifies the category byte is `62`.
2. It reads the ASTERIX block length.
3. It repeatedly calls `readRecord` until the block boundary is reached.
4. `Cat062CodecSupport.readRecord` reads the FSPEC.
5. The FSPEC is converted into a list of FRNs.
6. Each FRN dispatches to the item-specific reader for that field.
7. The record is assembled incrementally with `copy(...)` calls on
   `Cat062Record`.

This means the record decoder is table-driven by FRN order, not by hand-written
 per-record parsing logic.

### Writing

Writing runs in the opposite direction:

1. `Cat062Codec.writeDataBlock` writes the category byte and reserves space for
   the block length.
2. Each record is written by `Cat062CodecSupport.writeRecord`.
3. `writeRecord` first validates CAT062 mandatory items.
4. It computes the list of present FRNs from the non-null fields of
   `Cat062Record`.
5. It writes the FSPEC for those FRNs.
6. It writes item payloads in FRN order.
7. `writeDataBlock` backfills the final ASTERIX block length.

The current mandatory items enforced on write are:

- `I062/010` Data Source Identifier
- `I062/040` Track Number
- `I062/070` Time Of Track Information
- `I062/080` Track Status

## Model Layer

`Cat062Record` is the aggregate model for a single CAT062 record. Nearly all
fields are nullable. Null usually means “item absent from the record”.

The supporting type files group related models by domain:

- `Cat062CommonTypes.kt`
- `Cat062TrackTypes.kt`
- `Cat062AircraftTypes.kt`
- `Cat062MeasuredTypes.kt`
- `Cat062FlightPlanTypes.kt`
- `Cat062Mode5Types.kt`

The code uses plain Kotlin data classes heavily. This keeps the model simple and
serializable in spirit, but it also means some spec-coded values are still raw
`Int`s instead of stronger enum-like abstractions.

`RawBytes` is a small wrapper around `ByteArray` used where binary payloads need
value semantics instead of reference equality.

## Fixed vs Compound Items

There are two broad codec styles in the repository.

### Fixed-Length and Primitive Items

These mostly live in `Cat062CodecWire.kt` and handle:

- scalar numeric conversions
- fixed-width integer packing
- signed and unsigned interpretation
- simple multi-octet structures

Examples:

- positions
- velocities
- mode 2 / mode 3 codes
- barometric altitude
- length-prefixed reserved fields

### Compound and Variable-Length Items

These live mostly in `Cat062CodecCompounds.kt` and handle:

- compound presence indicators
- variable extents
- repeated subfields
- items whose shape depends on flags

Examples:

- aircraft derived data
- track status
- system track update ages
- track data ages
- flight plan related data
- estimated accuracies
- measured information

If you need to work on a complex spec item, this is the first place to inspect.

## Internal Conventions

The codebase follows a few important conventions:

- FRNs are encoded and decoded through FSPEC helper logic in
  `Cat062CodecSupport.kt`.
- Compound subfield presence is encoded and decoded through helper functions in
  `Cat062CodecWire.kt`.
- Field-specific validation is usually enforced at write time with `require(...)`.
- Reads are mostly permissive about default values and absent optional fields.
- Writes are generally driven by nullability: non-null fields become present
  items or subfields.

## Testing Strategy

The repository currently uses one focused test file:

- `src/test/kotlin/io/github/erikgust2/asterix/cat062/Cat062CodecTest.kt`

The test suite covers:

- FSPEC bit layout checks
- round-trip checks for selected fixed and compound items
- regression coverage for Track Status
- mandatory item enforcement on write

The local CAT062 v1.15 PDF is the source of truth for spec alignment.

## Current Architectural Strengths

- Small public API
- Clear separation between public API, record orchestration, wire helpers, and
  model types
- Strong use of value types
- Spec-driven write validation in several places
- Fast local test cycle

## Current Architectural Constraints

- `Cat062CodecCompounds.kt` is large and carries a lot of complexity in one file
- The API is low-level and buffer-oriented
- There is only one main test file
- Some spec-coded fields are still modeled as raw integers
- The project is category-specific, so cross-category ASTERIX reuse is limited

## How To Extend The Codec

When adding or fixing a CAT062 item:

1. Confirm the wire layout in the v1.15 PDF.
2. Update or add model fields in the relevant `Cat062*Types.kt` file.
3. Add read and write logic in the correct codec file:
   - fixed/simple item: usually `Cat062CodecWire.kt`
   - compound/extent-based item: usually `Cat062CodecCompounds.kt`
4. Wire the item into FRN dispatch in `Cat062CodecSupport.kt`.
5. Add round-trip and byte-layout tests in `Cat062CodecTest.kt`.
6. Update `README.md` if the public API or supported scope changed.

## What Future Refactors Should Preserve

If you refactor this codebase later, preserve these properties:

- `Cat062Codec` remains the obvious public entry point
- FSPEC handling stays centralized
- item-specific wire logic stays isolated from model definitions
- tests remain spec-oriented, not just happy-path object equality checks
- mandatory CAT062 write constraints remain enforced
