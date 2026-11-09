# Netherite NBT filter

> Implement inline with superpowers:executing-plans; one focused review at completion.

**User requirements:** General-purpose Netherite Filter installs in original Create filter slots. Read a reference item's NBT/components, edit field values, support numeric equality, greater-than, less-than and inclusive ranges. User explicitly chose editable values/ranges. Restore crafting/creative access. Keep fire/blast protection, bump0.3.25, auto commit/push, no client launch.

**Design:** Preserve old NetheriteFilterData/menu for legacy saved FanRules. New typed reference uses CONTAINER slot36; old0..35 preserved. CUSTOM_DATA.NbtFilter stores SameItem(defaulttrue), Blacklist(defaultfalse), Conditions (Path string-segment list, Op0EQ/1GT/2LT/3RANGE, First/Second strings, Kind numeric0 or NBT type); serialized Reference is accepted as a manual-config fallback. Encode effective persistent DataComponentMap to NBT, including defaults; path segments handle compound keys/list indexes without lossy dotted parsing. All selected conditions AND together; whitelist/blacklist invert the overall match. Unconfigured filters reject everything. Numeric comparisons use BigDecimal, nonnumeric equality exact, empty-structure edits use native SNBT parser. Missing fields fail a condition.

**UI:** New menu/screen keeps the held filter locked, copies one server-owned reference sample without consuming it, and applies validated field changes only to that menu/filter. Payload binds container ID and current reference snapshot hash. Tree list5 rows with scrolling, checkbox toggles, operator and two text fields+Apply. Screen316x236, reference slot(18,31), player slots(77,153), hotbarY211. Background art requested from user-designated chat01a1859a-feb8-7880-b7c0-e0c1ac5f5a42; new netherite_nbt_filter.png only, old76 PNG immutable. Read/send completed; await asset.

**Integration:** Existing FilterItem wrapper dispatches new config, preserves legacy wrapper fallback; native copying and nested filters remain supported. New creative item and shapeless Create filter+sheet recipe. The fan's generic filter slots may accept this new general filter, no processing modes in the filter itself. Native original Create filtering interfaces handle funnels, chutes, basin, saw, deployer etc.

- [x] Server test fails on missing NBT matching.
- [x] Data model, matching and tests (selected fields, precision, missing fields, defaults, lists, arbitrary keys, strict item matching, inversion, copy/persistence/legacy).
- [x] Menu/payload/UI, server validation, actual native filter installation/removal and sample nonconsumption.
- [x] Recipe/creative/fan compatibility, summaries, localization, docs/version0.3.25.
- [x] Artwork arrival/inspection, headless build+GameTests, optional JEI content coverage, one review, JAR audit. Auto commit/push follows repository policy.

## Progress

- Baseline25e144878b883b2cc543e8343c35a8803cd5f618; only old crash report untracked.
- Source inspected: existing filter is a FilterItem but retired, legacy menu groups by fan mode, plain samples lose NBT. Native FilterItem/FilteringBehaviour permits general filters and persists CUSTOM_DATA.
- Final storage decision: typed full reference uses CONTAINER slot36; old nested slots0..35 retained. CUSTOM_DATA.NbtFilter stores SameItem/Blacklist and Conditions with explicit Kind (numeric0 or NBT tag type), preventing numeric-looking strings from matching numeric equality. Serialized Reference remains a fallback for manually authored config. Only scalar/empty-structure leaves appear in the editor.
- `build/nbt-filter-red.log`: first selected-numeric-field test fails on absent support. `build/nbt-filter-core-validation.log`: first125 tests pass. `build/nbt-filter-expanded-validation.log`:131 tests pass, including7 new feature tests and original native filter tests.
- Independent review found newly crafted/creative blanks rejected by native copying eligibility. Actual recipe test RED in `build/nbt-filter-copy-red.log`; explicit modern canCopyFrom/canCopyTo retains legacy rules and fixes targets.
- Review's resize finding was regraded as important because Apply could overwrite a string with empty text. `init` now preserves existing edit values across resize. Actual client path is source-reviewed, not graphically exercised.
- `build/nbt-filter-release-with-jei.log`:205 regressions and136 real server GameTests pass, including8 feature tests +4 JEI tests. No re-review needed; fixes covered by actual recipe test and the complete suite.
- Art chat delivered316x236 opaque RGBA, SHA256703ab992eb0ee0622e8579fe62a21229ab1287918d31a9777cdaa02530c28185. Root viewed it and independently verified37 slot frames and all76 original PNGs unchanged.
- Final no-JEI build:205 regression cases and132 GameTests passed (`build/nbt-filter-release-without-jei.log`).
- Package audit:275 source resources and217 compiled classes byte-match;194 JSON;224 locale keys each with16 new/7 intentionally changed old keys; all45 original recipes normalized-line-ending-identical; all76 oldPNG byte-identical. NewGUI37 frames independently verified. No JEI/test resources bundled, version0.3.25.
- Final JAR SHA256 `f4cc9784ed7e200598c6b62b4ba9e48caf91803bb6f84cac55a30649b3118432`. No client launch, UI interaction/resize visual QA stays with the user.
