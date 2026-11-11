# Netherite Basin

User-approved design:27 input slots and27 output slots, each retaining normal item stack limits. Identical items may occupy several slots. Ordinary stackable input capacity rises576→1728; fluid tanks, processing speed, heat and filters remain native. Add the established sheet upgrade and converter routes, fire/blast protection, native Shift summary, JEI and art-chat PNGs. Release0.3.27; no client launch; automatic commit/push after checks.

- [x] Add real server tests and record the failing native-capacity baseline.
- [x] Wrap the native BasinInventory construction only for the Netherite subclass; create27-slot inventories that accept repeated item stacks while honoring insertion locks. Keep the native private inventory list and capability wrappers bound to the same instances.
- [x] Register block/entity/item, capabilities, renderer, unpacking integration, recipes, translations, models and new art.
- [x] Verify full storage, simulation, output locks, stack limits, filter/processing compatibility, save/reload, removal and ordinary-basin preservation. Update current version/docs and package checks.

Review found a supported repeated-product capacity defect: inherited simulation independently promises the same remaining space to both outputs. A real BasinRecipe producing2×64 diamonds with only65 free positions reproduces it in `build/basin-multiple-products-red.log`; a collective-output preflight now prevents consumption until all products fit. Test harness fixes use mutable package item lists and account for the native wrench toggling an already-selected outlet off.

Final validation:205 regression cases and157 server GameTests with JEI (`build/basin-release-with-jei.log`);205 and153 without JEI (`build/basin-release-without-jei.log`). Nine feature tests include all four real motor-driven operators, native filter and side outlet, package input, moving storage, full buffers, limits, persistence, removal and blocked multiple products. Both builds pass.298 production resource entries,232 classes and212 valid JSON;228 keys per locale; metadata0.3.27; no QA or JEI bundled. No client launch. Deliver through the authorized scoped commit/push.
