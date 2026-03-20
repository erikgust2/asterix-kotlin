# Architecture

This document describes how the current `asterix-kotlin` codebase is organized,
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
- `Cat062CodecTrackState.kt`
  - codecs for track status, mode of movement, system track update ages, and
    track data ages
- `Cat062CodecAircraftDerivedData.kt`
  - compound codec for `I062/380` aircraft derived data
- `Cat062CodecFlightPlan.kt`
  - compound codec for `I062/390` flight plan related data
- `Cat062CodecMode5.kt`
  - codec for `I062/110` mode 5 data reports and related fields
- `Cat062CodecEstimatedAccuracies.kt`
  - codec for `I062/500` estimated accuracies
- `Cat062CodecMeasuredInformation.kt`
  - codec for `I062/340` measured information
- `Cat062Record.kt`
  - top-level `Cat062Record` and `Cat062DataBlock` models
- `Cat062*Types.kt`
  - model types grouped by domain
  - common, track, aircraft-derived, measured, flight plan, mode 5, etc.
- `Cat062ByteArraySupport.kt`
  - `ByteArray` helpers such as `RawBytes` for value semantics

## Public API

The public entry point is `Cat062Codec`.

It exposes eight operations:

- `readDataBlock(buffer: ByteBuffer): Cat062DataBlock`
- `readDataBlock(bytes: ByteArray): Cat062DataBlock`
- `writeDataBlock(buffer: ByteBuffer, block: Cat062DataBlock)`
- `writeDataBlock(block: Cat062DataBlock): ByteArray`
- `readRecord(buffer: ByteBuffer): Cat062Record`
- `readRecord(bytes: ByteArray): Cat062Record`
- `writeRecord(buffer: ByteBuffer, record: Cat062Record)`
- `writeRecord(record: Cat062Record): ByteArray`

The API is still `ByteBuffer`-oriented at its core. The `ByteArray` overloads
are convenience wrappers around that core path. Read-side wrappers allocate a
temporary `ByteBuffer` object but do not copy the input bytes; write-side
wrappers allocate an internal `ByteBuffer` and the returned `ByteArray`, and
may retry with a larger buffer for bigger payloads. Performance-sensitive code
should continue using the explicit `ByteBuffer` methods. The library does not
currently expose stream adapters or higher-level builders. It does provide
`RawBytes` for length-prefixed binary payloads that need value semantics.

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
fields are nullable. Null usually means "item absent from the record".

`I062/080` Track Status is the main exception to treat carefully: null is only
used for absent extents, not for absent individual bits inside a present
extent. Octet 1 is always required, and if any later extent is present then all
fields in that extent, and every earlier implied extent, must be specified.
Write-side validation rejects partially specified extents so the model does not
silently collapse null into spec-default zero bits.

`I062/270` Target Size & Orientation also has dependent extent semantics on
write. `orientationDegrees == null` means only the first octet with length is
present, `orientationDegrees != null && widthMeters == null` means the first
extent is present without the second, and `widthMeters != null` requires
`orientationDegrees != null` because width only exists in the second extent.

The supporting type files group related models by domain:

- `Cat062CommonTypes.kt`
- `Cat062TrackTypes.kt`
- `Cat062AircraftTypes.kt`
- `Cat062MeasuredTypes.kt`
- `Cat062FlightPlanTypes.kt`
- `Cat062Mode5Types.kt`

The code uses plain Kotlin data classes heavily. This keeps the model simple,
but it also means some spec-coded values are still raw `Int`s instead of
stronger enum-like abstractions.

`RawBytes` is a small wrapper around `ByteArray` used where binary payloads need
value semantics instead of reference equality.

Some CAT062 substructures are intentionally still opaque pass-through payloads
rather than fully decoded models. The remaining examples are the `RE` and `SP`
length-prefixed payloads.

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

These live in the focused domain codec files and handle:

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

If you need to work on a complex spec item, start in the codec file named for
that domain.

## Internal Conventions

The codebase follows a few important conventions:

- FRNs are encoded and decoded through FSPEC helper logic in
  `Cat062CodecSupport.kt`.
- Compound subfield presence is encoded and decoded through helper functions in
  `Cat062CodecWire.kt`, then composed by the focused domain codec files.
- Field-specific validation is usually enforced at write time with `require(...)`.
- Reads are mostly permissive about default values and absent optional fields.
- Writes are generally driven by nullability: non-null fields become present
  items or subfields.

## Testing Strategy

The test suite is split by codec area:

- `Cat062CodecDataBlockTest`
- `Cat062CodecSupportTest`
- `Cat062CodecWireFixedItemsTest`
- `Cat062CodecTrackStateTest`
- `Cat062CodecAircraftDerivedDataTest`
- `Cat062CodecFlightPlanTest`
- `Cat062CodecMode5Test`
- `Cat062CodecEstimatedAccuraciesTest`
- `Cat062CodecMeasuredInformationTest`
- `Cat062TestSupport`

The test suite covers:

- public record and data-block round trips through both `ByteBuffer` and `ByteArray` entry points
- FSPEC bit layout and FRN dispatch behavior
- spec-layout assertions for fixed and compound items
- malformed-input and truncation behavior
- mandatory item enforcement on write

The local CAT062 v1.15 PDF is the source of truth for spec alignment.
`docs/TESTING_PLAN.md` is the active coverage map and should stay aligned with
the current suite layout.

## Current Architectural Strengths

- Small public API
- Clear separation between public API, record orchestration, wire helpers, and
  model types
- Focused domain codec files for the more complex CAT062 items
- Strong use of value types
- Spec-driven write validation in several places
- Fast local test cycle

## Current Architectural Constraints

- The API is low-level and buffer-oriented
- Some spec-coded fields are still modeled as raw integers
- Several CAT062 compound items remain inherently complex even after file
  splitting
- The project is category-specific, so cross-category ASTERIX reuse is limited

## How To Extend The Codec

When adding or fixing a CAT062 item:

1. Confirm the wire layout in the v1.15 PDF.
2. Update or add model fields in the relevant `Cat062*Types.kt` file.
3. Add read and write logic in the correct codec file:
   - fixed/simple item: usually `Cat062CodecWire.kt`
   - track state and ages: `Cat062CodecTrackState.kt`
   - aircraft-derived data: `Cat062CodecAircraftDerivedData.kt`
   - flight plan data: `Cat062CodecFlightPlan.kt`
   - mode 5 data: `Cat062CodecMode5.kt`
   - estimated accuracies: `Cat062CodecEstimatedAccuracies.kt`
   - measured information: `Cat062CodecMeasuredInformation.kt`
4. Wire the item into FRN dispatch in `Cat062CodecSupport.kt`.
5. Add or update the focused test file for that codec area.
6. Update `README.md`, `docs/TESTING_PLAN.md`, and this document if the public
   API, coverage map, or architecture description changed.

## What Future Refactors Should Preserve

If you refactor this codebase later, preserve these properties:

- `Cat062Codec` remains the obvious public entry point
- FSPEC handling stays centralized
- item-specific wire logic stays isolated from model definitions
- tests remain spec-oriented, not just happy-path object equality checks
- mandatory CAT062 write constraints remain enforced
