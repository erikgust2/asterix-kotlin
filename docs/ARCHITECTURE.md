# Architecture

This document explains how the current `asterix-kotlin` codebase is organized,
how CAT062 data flows through it, and where to make changes without losing the
spec-driven structure of the codec.

For a practical walkthrough of ASTERIX blocks, FSPEC, FRNs, and CAT062 item
semantics, read [`HOW_CAT062_WORKS.md`](HOW_CAT062_WORKS.md) first.

## Scope

The repository is a Kotlin/JVM codec for ASTERIX CAT062 System Track Data,
aligned to the CAT062 v1.15 PDF in the repository root.

It is intentionally narrow in scope:

- one ASTERIX category: CAT062
- one Maven module
- one public entry point: `Cat062Codec`
- one model namespace: `io.github.erikgust2.asterix.cat062`

It is not a general multi-category ASTERIX framework.

## High-Level Layout

The important code lives under:

- `src/main/kotlin/io/github/erikgust2/asterix/cat062`
- `src/test/kotlin/io/github/erikgust2/asterix/cat062`

Key production files:

- `Cat062Codec.kt`
  - public API for record and data-block read/write
  - `ByteBuffer` core path plus convenience `ByteArray` overloads
  - ASTERIX category and block-length framing for full data blocks
- `Cat062CodecSupport.kt`
  - CAT062 record orchestration
  - FSPEC decoding and encoding
  - FRN-to-field dispatch
  - write-side mandatory-item enforcement
  - public error-context enrichment with CAT062 item references
- `Cat062CodecWire.kt`
  - low-level fixed-width wire helpers
  - signed and unsigned packing helpers
  - quantization and range enforcement
  - length-prefixed field handling
  - compound-indicator helpers reused by complex items
- `Cat062CodecTrackState.kt`
  - `I062/080`, `I062/200`, `I062/290`, and `I062/295`
- `Cat062CodecAircraftDerivedData.kt`
  - `I062/380`
- `Cat062CodecFlightPlan.kt`
  - `I062/390`
- `Cat062CodecMode5.kt`
  - `I062/110`
- `Cat062CodecEstimatedAccuracies.kt`
  - `I062/500`
- `Cat062CodecMeasuredInformation.kt`
  - `I062/340`
- `Cat062Record.kt`
  - `Cat062Record` and `Cat062DataBlock`
- `Cat062*Types.kt`
  - typed model layer grouped by domain
- `Cat062ByteArraySupport.kt`
  - `RawBytes` and byte-array helpers with value semantics

## Wire Model

At the ASTERIX layer, a CAT062 data block is:

1. category byte `62`
2. two-byte block length
3. one or more CAT062 records

Each CAT062 record is:

1. an FSPEC of one or more octets
2. item payloads for the FRNs selected by that FSPEC
3. payloads written in UAP order, not arbitrary field order

The codec keeps these layers separate:

- `Cat062Codec.kt` owns ASTERIX block framing
- `Cat062CodecSupport.kt` owns CAT062 record orchestration
- item-specific functions own the payload details

## Public API

`Cat062Codec` exposes:

- `readDataBlock(buffer: ByteBuffer): Cat062DataBlock`
- `readDataBlock(bytes: ByteArray): Cat062DataBlock`
- `writeDataBlock(buffer: ByteBuffer, block: Cat062DataBlock)`
- `writeDataBlock(block: Cat062DataBlock): ByteArray`
- `readRecord(buffer: ByteBuffer): Cat062Record`
- `readRecord(bytes: ByteArray): Cat062Record`
- `writeRecord(buffer: ByteBuffer, record: Cat062Record)`
- `writeRecord(record: Cat062Record): ByteArray`

Design choices worth preserving:

- `ByteBuffer` is the primary abstraction, not streams or builders
- `ByteArray` overloads are convenience wrappers, not a second code path
- full data-block APIs add ASTERIX framing while single-record APIs operate on
  raw CAT062 records

## Read Path

Reading a full data block works like this:

1. `Cat062Codec.readDataBlock` verifies category `62`.
2. It reads the ASTERIX block length and computes the block boundary.
3. It repeatedly calls `readRecord` until the boundary is reached.
4. Nested record failures are wrapped with the one-based record ordinal.

Reading a single record works like this:

1. `Cat062CodecSupport.readRecord` reads the FSPEC.
2. The FSPEC is expanded into present FRNs.
3. FRNs dispatch through a table-driven `when` block.
4. Each FRN reader updates a `Cat062Record` via `copy(...)`.
5. Item-level failures are wrapped so public errors include the relevant
   CAT062 item reference where known.

This is intentionally UAP-driven. The decoder is controlled by FRN presence and
order, not by ad hoc per-record parsing logic.

## Write Path

Writing a single record works like this:

1. `Cat062CodecSupport.writeRecord` checks mandatory items.
2. It computes the present FRN list from non-null `Cat062Record` fields.
3. It writes the FSPEC for those FRNs.
4. It writes item payloads in FRN order.
5. Item-specific writers enforce range and extent rules with `require(...)`.

Writing a full data block adds ASTERIX framing:

1. `Cat062Codec.writeDataBlock` writes category `62`.
2. It reserves two bytes for the block length.
3. It writes each record through the normal record writer.
4. It backfills the final length.

Mandatory items currently enforced on write:

- `I062/010` Data Source Identifier
- `I062/040` Track Number
- `I062/070` Time Of Track Information
- `I062/080` Track Status

## Model Layer

`Cat062Record` is the aggregate model for one CAT062 record. Most fields are
nullable and `null` normally means that item is absent from the FSPEC.

The model is split by domain:

- `Cat062CommonTypes.kt`: shared scalar and fixed-width structures
- `Cat062TrackTypes.kt`: track state, track ages, movement, accuracies, fleet
  identification
- `Cat062AircraftTypes.kt`: aircraft-derived data and related code tables
- `Cat062FlightPlanTypes.kt`: flight-plan related structures
- `Cat062Mode5Types.kt`: mode 5 data
- `Cat062MeasuredTypes.kt`: measured-information structures

Important modeling conventions:

- fixed and simple items use plain Kotlin data classes and enums where the
  code table is closed and stable
- sparse or potentially extensible code tables use sealed `Known` /
  `Unknown(code)` models so unknown values can still round-trip unchanged
- opaque binary payloads use `RawBytes` to get value semantics instead of raw
  `ByteArray` reference equality

## Extent And Presence Semantics

Some CAT062 items need more than plain nullability.

`I062/080` Track Status:

- octet 1 is always required when the item is present
- later extents are optional
- if a later extent is present, all fields in that extent must be specified
- the writer rejects partially specified extents rather than silently encoding
  zero/default bits

`I062/270` Target Size & Orientation:

- `orientationDegrees == null` means only the first octet with length is
  present
- `orientationDegrees != null && widthMeters == null` means the first extent is
  present but the second is absent
- `widthMeters != null` requires `orientationDegrees != null`

Compound items such as `I062/380`, `I062/390`, `I062/500`, and `I062/340` use
their own presence indicators and subfield-specific rules inside the dedicated
codec files.

## Error Model

The public API uses runtime exceptions for invalid input and invalid write-side
state.

Current behavior:

- truncation normally surfaces as `IllegalArgumentException`
- record and item context is added close to the failure site
- item references such as `I062/390` are preserved in public messages where the
  codec can identify the failing item
- full data-block decode failures add the one-based record ordinal

This context enrichment is part of the public usability of the codec and should
be preserved during refactors.

## Testing Structure

The regression suite is organized by codec area:

- `Cat062CodecDataBlockTest`
- `Cat062CodecGoldenVectorTest`
- `Cat062CodecSupportTest`
- `Cat062CodecWireFixedItemsTest`
- `Cat062CodecTrackStateTest`
- `Cat062CodecAircraftDerivedDataTest`
- `Cat062CodecFlightPlanTest`
- `Cat062CodecMode5Test`
- `Cat062CodecEstimatedAccuraciesTest`
- `Cat062CodecMeasuredInformationTest`
- `Cat062TestSupport`

`docs/TESTING_PLAN.md` is the active coverage map and should stay aligned with
the implemented item set and suite layout.

## How To Extend The Codec

When adding or fixing a CAT062 item:

1. Confirm the wire layout in the CAT062 v1.15 PDF.
2. Decide whether the model belongs in an existing `Cat062*Types.kt` file or a
   new one.
3. Add read and write logic in the correct codec file:
   - fixed/simple item: usually `Cat062CodecWire.kt`
   - track-state and age items: `Cat062CodecTrackState.kt`
   - aircraft-derived data: `Cat062CodecAircraftDerivedData.kt`
   - flight-plan data: `Cat062CodecFlightPlan.kt`
   - mode 5 data: `Cat062CodecMode5.kt`
   - estimated accuracies: `Cat062CodecEstimatedAccuracies.kt`
   - measured information: `Cat062CodecMeasuredInformation.kt`
4. Wire the item into the FRN dispatch and present-FRN calculation in
   `Cat062CodecSupport.kt`.
5. Add or update focused tests for round-trip, spec-layout, and malformed-input
   behavior.
6. Update `README.md`, `docs/HOW_CAT062_WORKS.md`, `docs/TESTING_PLAN.md`, and
   this document if the support boundary or architecture changed.

## Architectural Strengths

- small and obvious public API
- clear separation between ASTERIX framing, record orchestration, wire helpers,
  and domain-specific compound codecs
- model types that keep spec semantics visible in Kotlin
- strong write-side validation for mandatory items and wire ranges
- regression tests that pin exact bytes for representative records and data
  blocks

## Architectural Constraints

- the public API is intentionally low-level and `ByteBuffer`-oriented
- some CAT062 structures remain inherently dense and spec-heavy
- `RE` and `SP` are still opaque payloads rather than decoded sub-models
- the repository is category-specific, so cross-category ASTERIX reuse is
  intentionally limited

## Refactors Should Preserve

- `Cat062Codec` as the obvious public entry point
- centralized FSPEC handling
- centralized FRN ordering and dispatch
- separation between model definitions and wire logic
- spec-driven tests, not only object-equality round-trips
- mandatory CAT062 write constraints
