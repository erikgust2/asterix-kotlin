# Testing Plan

This document is the active CAT062 test coverage map for `asterix-kotlin`.

It is aligned to the CAT062 v1.15 PDF in the repository root and tracks the
implemented User Application Profile (UAP) items, the current test suite
layout, and the remaining follow-up gaps.

## Scope

Source of truth:

- `cat062-asterix-system-track-data-part9-v1.15-20110901.pdf`
- UAP pages 129-131

In scope:

- every implemented non-spare UAP item in the codec
- public `Cat062Codec` record and data-block behavior
- FSPEC generation and parsing
- malformed-input and truncation behavior

Out of scope:

- spare FRNs `2`, `29`, `30`, `31`, `32`, `33`
- adding new production codec features or new ASTERIX categories
- a JaCoCo build gate

The spare FRNs are still covered as decode failures so unsupported-item
handling is exercised.

## Current Suite Layout

The test suite is split by codec area:

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

Shared helpers provide:

- a minimal valid CAT062 record with mandatory items populated
- byte-buffer to byte-array extraction helpers
- record and data-block encoding helpers
- truncation helpers for malformed-input tests
- common range-failure assertions

Typed-code coverage policy:

- closed code tables should have direct `fromCode(...)` coverage
- sealed `Known` / `Unknown(code)` families should prove unknown decode and
  lossless re-encode
- sparse code tables promoted from enums to sealed families should keep known
  constant aliases stable while adding explicit unknown-wire coverage
- golden vectors should stay byte-identical after model typing changes

## Coverage Matrix

| FRN | Item | Primary suite | Current coverage |
| --- | --- | --- | --- |
| 1 | `I062/010` Data Source Identifier | `Cat062CodecGoldenVectorTest`, `Cat062CodecDataBlockTest`, `Cat062CodecSupportTest` | Literal record/block vectors for mandatory and dense records, plus public round-trip coverage |
| 3 | `I062/015` Service Identification | `Cat062CodecGoldenVectorTest`, `Cat062CodecDataBlockTest`, `Cat062CodecSupportTest` | FSPEC coverage, literal dense-record bytes, and end-to-end round-trips |
| 4 | `I062/070` Time Of Track Information | `Cat062CodecGoldenVectorTest`, `Cat062CodecSupportTest`, `Cat062CodecDataBlockTest` | Literal minimal and dense record/block vectors, mandatory-item coverage, round-trip, and 24-bit overflow rejection |
| 5 | `I062/105` Calculated Track Position (WGS-84) | `Cat062CodecGoldenVectorTest`, `Cat062CodecDataBlockTest`, `Cat062CodecWireFixedItemsTest` | Dense-record literal bytes, record round-trip, and exact quantized WGS-84 wire round-trip |
| 6 | `I062/100` Calculated Track Position (Cartesian) | `Cat062CodecDataBlockTest`, `Cat062CodecWireFixedItemsTest` | Record round-trip, signed 24-bit boundary coverage, overflow rejection |
| 7 | `I062/185` Calculated Track Velocity (Cartesian) | `Cat062CodecWireFixedItemsTest` | Direct wire round-trip coverage |
| 8 | `I062/210` Calculated Acceleration (Cartesian) | `Cat062CodecWireFixedItemsTest` | Direct wire round-trip coverage |
| 9 | `I062/060` Track Mode 3/A Code | `Cat062CodecGoldenVectorTest`, `Cat062CodecWireFixedItemsTest`, `Cat062CodecSupportTest` | Dense-record literal bytes, spec-layout assertion, and public record round-trip |
| 10 | `I062/245` Target Identification | `Cat062CodecGoldenVectorTest`, `Cat062CodecWireFixedItemsTest`, `Cat062CodecDataBlockTest` | Dense-record literal bytes, all source enums, normalization, unsupported-character rejection, overlength rejection, public round-trip |
| 11 | `I062/380` Aircraft Derived Data | `Cat062CodecAircraftDerivedDataTest` | Spec-layout assertions, per-subfield round-trips across all implemented subfields, dense round-trip, repetition coverage, selected bounds and truncation coverage, structured trajectory intent point encoding/decoding, typed-code mapping checks, and unknown-value round-trip coverage for forward-compatible trajectory-intent, Mode-S, and emitter-category codes |
| 12 | `I062/040` Track Number | `Cat062CodecGoldenVectorTest`, `Cat062CodecSupportTest`, `Cat062CodecDataBlockTest` | Literal minimal and dense record/block vectors, mandatory item coverage, and public round-trips |
| 13 | `I062/080` Track Status | `Cat062CodecGoldenVectorTest`, `Cat062CodecTrackStateTest`, `Cat062CodecDataBlockTest` | Literal minimal and dense record/block vectors, minimal and full extents, explicit absent/default/non-default extent semantics, extent-completeness validation, spec byte layout, typed-code mapping coverage, and public round-trip |
| 14 | `I062/290` System Track Update Ages | `Cat062CodecTrackStateTest` | Sparse coverage, empty and dense population coverage |
| 15 | `I062/200` Mode Of Movement | `Cat062CodecTrackStateTest` | Spec byte layout and enum/ADF decode coverage |
| 16 | `I062/295` Track Data Ages | `Cat062CodecTrackStateTest` | Sparse multi-octet indicator coverage, out-of-range age rejection, truncation coverage |
| 17 | `I062/136` Measured Flight Level | `Cat062CodecWireFixedItemsTest`, `Cat062CodecDataBlockTest` | Spec byte layout and record round-trip coverage |
| 18 | `I062/130` Calculated Track Geometric Altitude | `Cat062CodecSupportTest`, `Cat062CodecEstimatedAccuraciesTest` | Dedicated public record round-trip and write-side overflow rejection, plus indirect quantization coverage via estimated-accuracy tests |
| 19 | `I062/135` Calculated Track Barometric Altitude | `Cat062CodecWireFixedItemsTest`, `Cat062CodecDataBlockTest` | Record round-trip, flag decoding, overflow rejection |
| 20 | `I062/220` Calculated Rate Of Climb/Descent | `Cat062CodecSupportTest`, `Cat062CodecEstimatedAccuraciesTest` | Dedicated public record round-trip and write-side overflow rejection, plus quantization coverage via estimated-accuracy tests |
| 21 | `I062/390` Flight Plan Related Data | `Cat062CodecGoldenVectorTest`, `Cat062CodecFlightPlanTest` | Dense-record literal bytes, spec-layout assertion, per-subfield round-trips, full round-trip, repetition coverage, structured time of departure/arrival entry coverage, typed-code mapping coverage, ASCII behavior, and truncation coverage |
| 22 | `I062/270` Target Size & Orientation | `Cat062CodecGoldenVectorTest`, `Cat062CodecWireFixedItemsTest`, `Cat062CodecSupportTest` | Dense-record literal bytes, round-trip coverage for length-only, orientation-only, and width-extension encodings; explicit rejection of width without orientation; wrapped-orientation rejection; width overflow; public record round-trip |
| 23 | `I062/300` Vehicle Fleet Identification | `Cat062CodecGoldenVectorTest`, `Cat062CodecWireFixedItemsTest`, `Cat062CodecDataBlockTest` | Dense-record literal bytes, known and unknown code decode, record round-trip |
| 24 | `I062/110` Mode 5 Data Reports & Extended Mode 1 Code | `Cat062CodecMode5Test` | Spec-layout assertion, each subfield independently, full round-trip, field-range failures, truncation coverage |
| 25 | `I062/120` Track Mode 2 Code | `Cat062CodecWireFixedItemsTest` | Spec-layout assertion and direct round-trip coverage |
| 26 | `I062/510` Composed Track Number | `Cat062CodecGoldenVectorTest`, `Cat062CodecWireFixedItemsTest`, `Cat062CodecSupportTest` | Dense-record literal bytes, direct round-trip, and public record round-trip |
| 27 | `I062/500` Estimated Accuracies | `Cat062CodecEstimatedAccuraciesTest` | Spec-layout assertion, each subfield independently, combined round-trip, overflow rejection, truncation coverage |
| 28 | `I062/340` Measured Information | `Cat062CodecMeasuredInformationTest` | Spec-layout assertion, each subfield independently, combined round-trip, report-type mapping coverage, bounds, and truncation coverage |
| 34 | `RE` Reserved Expansion Field | `Cat062CodecGoldenVectorTest`, `Cat062CodecWireFixedItemsTest`, `Cat062CodecSupportTest`, `Cat062CodecDataBlockTest` | Dense-record literal bytes, empty and maximum payloads, invalid length byte, truncation, record round-trip |
| 35 | `SP` Special Purpose Field | `Cat062CodecGoldenVectorTest`, `Cat062CodecWireFixedItemsTest`, `Cat062CodecSupportTest`, `Cat062CodecDataBlockTest` | Dense-record literal bytes, length-prefixed behavior, and public record round-trip |

Unsupported FRN coverage:

- `Cat062CodecSupportTest` verifies failure on unsupported FRN `2`
- `Cat062CodecSupportTest` verifies failure on unsupported FRNs `29`, `30`, `31`, `32`, and `33`

## Explicit Partial Support

These item areas are intentionally preserved as opaque payloads for now rather
than exposed as fully decoded spec-level models:

- `RE` and `SP` are modeled as raw pass-through payloads

This keeps byte-level round-tripping stable while making the current support
boundary explicit in the API and docs.

## Malformed Input Coverage

The current suite covers these failure categories directly:

- wrong ASTERIX category
- ASTERIX block length smaller than `3`
- ASTERIX block length larger than remaining bytes
- data block ending mid-record
- truncated FSPEC continuation
- unsupported FRNs in early and late FSPEC extents
- truncated fixed-width item payloads
- truncated compound-item payloads
- invalid length-prefixed field length byte
- overflow and range failures for write-validated fields
- direct read-side truncation for every implemented fixed-width FRN

The suite also verifies that typed semantic code fields preserve wire
compatibility: known values decode to named variants, and the modeled unknown
branches re-encode their original wire values unchanged.

Assertion policy:

- exact messages are asserted for explicit `require(...)` failures
- representative truncation paths are asserted as contextual
  `IllegalArgumentException` failures that include CAT062 item references
  and, for compound items, subfield context where available

Public API note:

- `Cat062Codec` is covered through both `ByteBuffer` and `ByteArray`
  convenience entry points for record and data-block round trips
- representative validation and decode failures are asserted through the
  `ByteArray` overloads so they stay behaviorally aligned with the
  `ByteBuffer` core path
- nested data-block decode failures include the one-based record ordinal in
  the asserted public message

## Verification

Run:

- `mvn -Dmaven.repo.local=/tmp/m2 clean test`
- `mvn -Dmaven.repo.local=/tmp/m2 verify`

Review:

- `target/site/jacoco/jacoco.csv`
- `target/surefire-reports`

The suite should be considered healthy when:

- all tests pass
- each implemented non-spare UAP item is exercised directly or through a
  stable public round-trip
- top-level record and data-block encoding is pinned by literal golden vectors
- compound items have both positive and malformed-input coverage

## Remaining Follow-Up Gaps

The suite now covers the planned public-record, compound-item, and fixed-width
read-side gaps from this iteration, including literal record/data-block vectors
for representative CAT062 payloads.

The main remaining follow-up item is:

- optional JaCoCo target-setting once the current suite shape stabilizes
