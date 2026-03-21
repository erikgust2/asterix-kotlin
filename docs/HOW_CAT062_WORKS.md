# How CAT062 Works

This document is a practical introduction to ASTERIX CAT062 as implemented by
`asterix-kotlin`.

It is not a substitute for the CAT062 v1.15 PDF in the repository root. The
PDF remains the wire-layout source of truth. The goal here is to explain the
shape of the format, the terms used in the codebase, and how the library maps
CAT062 records into Kotlin types.

## Start With ASTERIX

ASTERIX messages are organized as category-specific data blocks. For CAT062,
the block layout is:

1. category byte `62`
2. two-byte block length
3. one or more CAT062 records

`Cat062Codec.readDataBlock(...)` and `writeDataBlock(...)` handle that outer
framing.

Inside the block, CAT062 is record-oriented. Each record represents one system
track and contains only the items declared present by its FSPEC.

## What FSPEC Means

FSPEC stands for Field Specification.

It is a compact bitmask at the start of every CAT062 record. Each bit selects a
Field Reference Number, or FRN. The FRN identifies one UAP entry such as:

- `I062/010` Data Source Identifier
- `I062/040` Track Number
- `I062/070` Time Of Track Information
- `I062/080` Track Status

Important FSPEC rules:

- bits 8 down to 2 in each FSPEC octet represent seven FRNs
- bit 1 is the FX bit
- FX set to `1` means another FSPEC octet follows
- FX set to `0` means the FSPEC ends there

The codec expands the FSPEC into a list of FRNs, then decodes item payloads in
UAP order.

## What The UAP Does

CAT062 defines a User Application Profile, or UAP. The UAP assigns each FRN to
one CAT062 item and fixes the order in which present item payloads appear in
the record.

That order matters.

CAT062 records do not carry per-item length tags for every item. After the
FSPEC, the decoder must already know which items are present and in which order
to parse the following bytes correctly.

In this repository, that mapping lives in `Cat062CodecSupport.kt`:

- `readRecord(...)` reads the FSPEC and dispatches FRNs to item readers
- `writeRecord(...)` computes present FRNs from the non-null model fields and
  writes payloads in UAP order

## The Minimal Record In This Library

The writer enforces the CAT062 mandatory items used by this project:

- `I062/010` Data Source Identifier
- `I062/040` Track Number
- `I062/070` Time Of Track Information
- `I062/080` Track Status

That means the smallest writable `Cat062Record` in this library still needs
those four items.

Example:

```kotlin
val record = Cat062Record(
    dataSourceIdentifier = DataSourceIdentifier(1, 2),
    timeOfTrackInformationSeconds = 128.0,
    trackNumber = 42,
    trackStatus = TrackStatus(
        mon = true,
        spi = false,
        mrh = false,
        src = TrackSource.TRIANGULATION,
        cnf = true,
    ),
)
```

The golden-vector test pins the encoded bytes for that record:

```text
91 0C 01 02 00 40 00 00 2A 8E
```

Breakdown:

```text
91 0C    FSPEC
01 02    I062/010 Data Source Identifier
00 40 00 I062/070 Time Of Track Information
00 2A    I062/040 Track Number
8E       I062/080 Track Status
```

FSPEC details:

- `0x91` = `1001 0001`
  - FRN 1 present
  - FRN 4 present
  - FX set, so a second FSPEC octet follows
- `0x0C` = `0000 1100`
  - FRN 12 present
  - FRN 13 present
  - FX clear, so FSPEC ends

So the decoder knows that the record contains FRNs `1`, `4`, `12`, and `13`,
in that order.

## How Items Differ

CAT062 items are not all shaped the same way. In practice, you will see four
main patterns.

### 1. Fixed-width items

These are simple items with a fixed number of bytes and direct scaling rules.

Examples:

- `I062/010` Data Source Identifier
- `I062/040` Track Number
- `I062/070` Time Of Track Information
- `I062/105` WGS-84 position
- `I062/100` Cartesian position

These mostly live in `Cat062CodecWire.kt`.

### 2. Items with bit fields

These still have fixed width, but their contents are split into flags and small
code tables.

Examples:

- `I062/060` Track Mode 3/A Code
- `I062/120` Track Mode 2 Code
- `I062/135` Barometric altitude
- `I062/200` Mode Of Movement

The library usually maps stable code tables to enums instead of leaving them as
raw integers.

### 3. Compound items

These items contain their own presence indicator and then a variable set of
subfields.

Examples:

- `I062/380` Aircraft Derived Data
- `I062/390` Flight Plan Related Data
- `I062/500` Estimated Accuracies
- `I062/340` Measured Information

These use dedicated codec files because the logic is too dense to keep inside
the generic record dispatcher.

### 4. Extent-based items

These items grow octet by octet, where a low bit indicates that another extent
follows.

Examples:

- `I062/080` Track Status
- `I062/270` Target Size & Orientation

These are easy to mis-model if you treat every nullable field as an optional
bit. The library models their extent rules explicitly.

## How The Kotlin Model Maps To CAT062

`Cat062Record` is the aggregate model for one record.

Most properties are nullable:

- non-null means the item is present and must be written
- null usually means the item is absent from the FSPEC

This makes the mapping from model presence to FRN presence straightforward.

Important exceptions and special cases:

- `TrackStatus` must respect CAT062 extent rules. Later extents cannot be
  partially specified.
- `TargetSizeAndOrientation` encodes extent presence directly:
  - `orientationDegrees == null` means length only
  - `orientationDegrees != null && widthMeters == null` means orientation
    present, width absent
  - `widthMeters != null` requires `orientationDegrees != null`
- `RE` and `SP` are modeled as `RawBytes`
  - the library preserves their bytes and their length-prefix framing
  - it does not yet decode their internal structure into richer models

## Why Some Fields Use `Known` / `Unknown(code)`

Some CAT062 code tables are stable and closed enough for enums.

Other tables are sparse or may gain future values. For those, the library uses
sealed `Known` / `Unknown(code)` models. That gives two useful properties:

- known values decode to readable named variants
- unknown values can still round-trip without being discarded or normalized

This is important for forward-compatible byte preservation.

## How Reading Works In The Codec

`Cat062Codec.readRecord(...)` does not try to parse a record by guessing.

The sequence is:

1. read the FSPEC
2. expand it to FRNs
3. dispatch each FRN to the correct item reader
4. update the `Cat062Record`

`readDataBlock(...)` adds the ASTERIX framing around that:

1. verify category `62`
2. read the block length
3. decode records until the block boundary
4. include the record ordinal if a nested decode fails

## How Writing Works In The Codec

Writing goes in the opposite direction:

1. validate mandatory items
2. collect the FRNs implied by non-null model fields
3. write the FSPEC
4. write payloads in UAP order

This means the Kotlin model is not just a convenient data holder. It directly
controls the encoded CAT062 record shape.

## Opaque Areas And Current Limits

The codec is intentionally not a full decoder for every conceptual area of
CAT062.

Current important limits:

- spare FRNs `2`, `29`, `30`, `31`, `32`, and `33` are unsupported and rejected
- `RE` and `SP` are preserved but not semantically decoded
- the repository is CAT062-only

Those limits are deliberate. They keep the project narrow and keep the
implemented part of the format well tested.

## Where To Go Next

- Read [`ARCHITECTURE.md`](ARCHITECTURE.md) if you want to change the codec.
- Read [`TESTING_PLAN.md`](TESTING_PLAN.md) if you want to understand which FRNs
  are already covered and how regressions are pinned.
- Read the CAT062 v1.15 PDF in the repository root before changing any wire
  behavior.
