# Press and mixer recipe filters

> **For agentic workers:** Implement inline using superpowers:executing-plans. Track the checks below.

**Goal:** Let the Netherite Press and Mixer use original Create filters to avoid unwanted recipe outputs during bulk processing.

**Spec:** User request and confirmed original blacklist/whitelist semantics, 2026-11-09. Filter recipe outputs, not ingredients. A blacklist containing lapis blocks must leave lapis available for other recipes.

**Architecture:** Attach native `FilteringBehaviour` to each machine, retaining native installation, removal, saving, synchronization and rendering. Check the machine filter during recipe matching and before application; retain the basin's independent filter. Pressing on belts, depots and in-world items uses the same output rule. Follow native basin semantics for the primary item output, or first fluid output for fluid-only recipes.

**Tech Stack:** Minecraft 1.21.1, NeoForge 21.1.219, Create 6.0.10-280, Java 21.

## Decisions and constraints

- Use native side slots instead of a separate GUI or inventory. Press slots avoid shaft faces; mixer slots use its horizontal sides.
- Reuse native sample-item and filter interactions, including the original filter's blacklist/whitelist and component settings.
- Empty machine slot preserves existing recipes and batching. Machine and basin filters both apply.
- No client launch: the user handles visual checks. No texture changes or dependency updates.
- Release 0.3.22; update README, Shift summaries and technical notes; commit and push after checks.

## Task 1: Recipe filtering

- [x] Add real server tests for blocked lapis compression and permitted bulk mixing; run against current code and observe missing-filter failure.
- [x] Add shared native recipe filter and wire Press/Mixer matching, direct pressing and in-flight application.
- [x] Verify blacklist, whitelist, empty slot, native filter interaction/persistence, basin intersection, fluid-only mixing and changes during a running cycle.
- [x] Run full headless build and GameTests, inspect release metadata and resources.
- [x] Review the change and record evidence; automatic commit/push follows the repository policy.

## Evidence

- `build/processing-filter-red.log`: both new initial tests fail because the native machine filter slot is missing.
- `build/processing-filter-validation.log`: 123/124 pass; interaction expectation incorrectly retained default enchantment/attribute components stripped by Create. Compare the native canonical filter stack and explicitly use survival mode.
- `build/processing-filter-release.log`: 205 regression cases and all 124 GameTests pass, including seven new processing-filter tests.
- Independent read-only review found missing filter-item rendering in native Press/Mixer renderers. Add Netherite-only HEAD hooks plus `neverSkipVanillaRender` for both visualizers. No outstanding findings.
- `build/processing-filter-final-build.log`: final client hooks compile, build succeeds and all 205 regression cases pass; no client launch.
- Package: 273 resources and 205 compiled classes byte-match; 193 valid production JSON; 201 keys per locale, all previous 197 values preserved; all 76 existing PNG unchanged; QA content excluded; metadata 0.3.22.
- SHA-256: `48f9a344ba0fc1f9f29bb651ada9b3619e0bb5e70bbcc1d07b103ea7aded8602`.
