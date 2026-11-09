# JEI recipe visibility

> **For agentic workers:** Implement inline using superpowers:executing-plans.

**Goal:** Make every current obtainable item and both custom recipe types discoverable in JEI, including Nether Essence and the advanced wrench.

**Spec:** User request, 2026-11-09. Existing production data has 25 crafting recipes, one pressing recipe, 18 conversion recipes and one exclusive mixing recipe. Preserve those rules and the retired legacy filter exclusion.

**Architecture:** Optional JEI plugin with two categories, using Create's recipe layout helpers and the live recipe manager. Exclusive mixing lists only the Netherite Mixer and basin. Conversion shows actual input/output, 250mB essence, per-recipe value cost, fixed 100 ticks and 2048SU, with point sources explained separately. Register native processing category catalysts for upgraded machines and distinguish empty/filled Netherite burners. Native Create filling provides the bucket route.

**Tech Stack:** MC1.21.1, NeoForge21.1.219, Create6.0.10-280, JEI19.21.0.247 API (compile-only), Java21. Full JEI development runtime opt-in via `-PwithJei=true`.

## Constraints

- No new gameplay costs, alternate crafting shortcuts, textures or client launch.
- JEI remains optional; API and test fixtures are excluded from the release JAR.
- Release0.3.23; synchronize README, technical documentation, translations and metadata; auto commit/push.

## Task

- [x] Headless registration test fails against the missing plugin.
- [x] Implement categories, registration, point-source descriptions, burner subtypes and machine catalysts.
- [x] Check actual registered recipes/ingredients and all current item acquisition routes, including bucket filling.
- [x] Full headless build/GameTests, independent review, packaged resource and optional-dependency checks.
- [x] Record evidence; automatic commit/push follows repository policy.

## Evidence

- Initial `build/jei-recipes-red.log` is an invalid API-only test setup: API jars do not register as loadable mods in the game module layer. Retained separately.
- `build/jei-recipes-red-with-jei.log`: with real JEI installed, the new registration test fails as intended on the missing plugin.
- `build/jei-recipes-validation.log`: 127/128 pass; a test proxy for client-facing IIngredientManager resolves Rect2i on the dedicated server. Replace that test with actual native filling; source-review native JEI bucket recipe generation.
- `build/jei-recipes-with-jei.log`: full build, 205 regressions and all128 GameTests pass, including4 JEI registration/layout/acquisition checks.
- `build/jei-recipes-without-jei.log`: no JEI installed, all124 original GameTests pass. JEI-only test classes are conditionally registered to keep this run free of JEI API class loading.
- Independent read-only review found no remaining critical, important or minor issue. Actual client rendering remains for the user to check.
- Package: 273 resource files and208 production classes byte-match;193 valid JSON;208 keys per language, all201 old values retained. All76 PNG byte-identical and45 gameplay recipes unchanged after Git line-ending normalization. Optional CLIENT metadata, no bundled JEI or test classes/resources.
- SHA-256: `b4d3bc205a639408abfe834f4851448262db2dfda2d7458a2812aa0f53d3da3a`.
