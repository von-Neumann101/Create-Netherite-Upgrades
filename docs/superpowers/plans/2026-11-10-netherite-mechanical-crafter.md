# Netherite Mechanical Crafter Implementation Plan

**Goal:** Each Netherite Mechanical Crafter holds one normal item stack and crafts the maximum complete batch per native crafting cycle.

**Architecture:** Reuse Create's connected grids, recipe validation, animations and output retries. Before a fully Netherite chain starts, reserve the minimum nonempty slot count from every occupied cell; leave excess in the original inventories. Assemble each recipe copy with count-one inputs, merge and split products and crafting containers into legal stacks, then let native output handling deliver them. Mixed chains retain one-recipe cycles.

**Tech stack:** Minecraft 1.21.1, NeoForge 21.1.219, Create 6.0.10, Java 21, MixinExtras.

**Constraints:** Native stress/speed, covers, redstone start, wrench layout and automated inputs remain available. Ordinary crafters remain unchanged. Fireproof/blast-resistant item/block; sheet upgrade and conversion recipes, JEI entry, native Shift summary. Version 0.3.26; no client launch. Art chat supplies new PNGs only.

- [x] Add real server tests for stack limits, 64 recipes per cycle, limiting ingredients with preserved excess, native mechanical recipes beyond 3x3, container returns, blocked outputs, persistence and ordinary/mixed grids. Run a failing baseline.
- [x] Add block/entity and narrow native identity/batch hooks, register capabilities, stress and arm interaction; integrate models, connected textures, renderer, recipes and JEI.
- [x] Run headless build and server tests, review feature paths and fix concrete findings. Update README/technical docs and packaged version.

Validation: 205 existing regression cases; 148 server GameTests with JEI and144 without, including12 new crafter cases. Logs: `build/crafter-release-with-jei.log` and `build/crafter-release-without-jei.log`. Review identified the missing native cutout render layer; corrected. Fresh-instance reload testing exposed native initialization calling INSERTING retries before inventory discovery; the upgrade now discovers its outlet before superclass initialization. Both final suites pass. No client launch.

Delivery: commit and push only this task after these checks.
