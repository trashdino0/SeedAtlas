# Examples

- `localize/project.json` — texture-rotation evidence for a 7x7x1 search area.
  The only candidate is `(3, 64, -2)`. Run:
  `seedatlas localize examples\localize\project.json --threads 4`
- `derived/project.json` — a synthetic bundle derived from seed
  `3055141959546` on 1.21.4 using Cubiomes. It contains 33 viable structures,
  3 strongholds, a spawn estimate, and 9 biome samples. Regenerate with:
  `seedatlas derive --seed 3055141959546 --version 1.21.4 --out examples\derived\project.json`
- `demo/project.json` — a hand-written schema and difficulty-estimator demo.
  The evidence does not describe a real world.

The `derive` bundles are our golden-test material: a fixture is generated from
a known seed, then the solver must recover that seed from the evidence alone.
Use `--include-seed` only when you want the bundle to be self-describing.

`seedatlas ingest` converts a SeedCrackerX saved-structures file
(`<name>;<chunkX>;<chunkZ>` per line) into an evidence bundle. This is the
quickest path when the mod has already collected data for a world.
