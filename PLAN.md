# SeedAtlas — Comprehensive Project Plan

**Status:** In progress — M0 native bridge and M1 foundation are working  
**Date:** 2026-11-02  
**Working title:** SeedAtlas  
**Proposed repo:** `C:\Users\rampi\Coding\minecraft\SeedAtlas`  
**Primary language:** Java 21 + native C (Cubiomes) + TypeScript UI

---

## 0. Executive summary

SeedAtlas is an offline Minecraft Java seed-investigation workbench. It fuses heterogeneous evidence — texture rotations, biome samples, structure sightings, stronghold geometry, End pillar heights, spawn estimates, partial seed hints — into ranked candidate seeds, a confidence score, and a verifiable map/report.

The differentiating use case is **cracking from media, not from in-game exploration**: a video or screenshot set of a base/world goes in, texture rotations localize the build, biome/structure evidence constrains the seed, and SeedAtlas returns candidates. This is complementary to SeedCrackerX, which requires running around the live world.

The project is deliberately built on two existing assets:

1. `minecraft/seedcracking/TextureRotations` — deterministic, multithreaded texture-rotation localization.
2. `minecraft/seedcracking/janszoon` — Cubiomes-backed seed-cracking experiments, including JNI bindings and benchmark classes.

The plan generalizes those into a reusable engine, adds the modern structure-seed routes that SeedCrackerX proves work, and adds a map-centric UI. It is not a rewrite of the cracking math from scratch.

**Biggest risk:** the structure-seed lifting/pillar solver. It is addressed first with a time-boxed spike (M0) and a fallback route.

---

## 1. Evidence from existing projects

### 1.1 `minecraft/seedcracking/TextureRotations`

- Java, multithreaded brute-force rotation matcher.
- Searches x/z `[-10000, 10000]`, y `[10, 60]` with a hardcoded 16-block formation.
- Supports:
  - Vanilla `<= 1.12.2` (`Vanilla12Textures`)
  - Vanilla `1.13–1.21.1` (`Vanilla21_1Textures`)
  - Vanilla `1.21.2+` (`VanillaTextures`)
  - Sodium variants (`SodiumTextures`, `Sodium19Textures`)
- Current uncommitted work changes the formation and switches the default to `VanillaTextures`.
- Tests exist for each provider but are `main()`-style smoke tests, not a JUnit suite.

**Critical property:** texture rotations are a deterministic function of block coordinates and are **seed-independent**. They locate a build's coordinates, not the seed. SeedAtlas must model them as *localization evidence*, separate from *seed evidence*.

### 1.2 `minecraft/seedcracking/janszoon` (SeedCracker v1–v4)

V4 is a hardcoded **Minecraft 1.18** cracker:

- Target stronghold: `(-2663, ~-2048, -655)`, tolerance ±112.
- 16 hardcoded biome points at y=200, e.g. `(-1621, 913) = flower_forest`, `(1058, 679) = swamp`.
- Bastion treasure check at region `(0,0)`.
- Portal eyes check at chunk `(-167, -41)`, expecting 0 eyes.
- Scanner threads and checker threads; batch size 2000.
- `CubiomesJNI` loads `libseedcracker.so` from JAR resources; exports:
  - `getBiomeAt`, `checkBiome`, `checkBiomes`, `scanSeeds`
  - `checkBastionTreasure`, `countPortalEyes`
  - Native Cubiomes symbols for `getStructurePos`, `getStructureConfig`, `initFirstStronghold`, `nextStronghold`, `isViableStructurePos`, `isViableStructureTerrain`, biome parameter functions.

**Key observations:**

1. `JavaRandom` masks to 48 bits (`0xFFFFFFFFFFFF`).
2. `StrongholdFinder` constructs `new JavaRandom(seed)` and therefore depends only on the **lower 48 bits**.
3. The main loop as written scans the full 64-bit `long` range (`Long.MIN_VALUE` to `Long.MAX_VALUE`). This repeats every lower-48 seed **65,536 times** at the stronghold stage.
4. The pipeline has no explicit lower-48 / upper-16 split. Biome checks need the full seed, so the correct design is:
   - scan lower 48 with cheap filters,
   - expand upper 16 only for survivors,
   - apply expensive biome/structure verification there.
5. The whole pipeline is hardcoded to one world; there is no schema, version abstraction, UI, or general evidence model.
6. `libseedcracker.so` is Linux-only (ELF, built on Ubuntu) and there is no source in the repo.

### 1.3 Other relevant assets

- `Java Obfuscator` — strong JVM/bytecode/Gradle experience.
- `optimizjvmprime`, `optimziejvmnew`, `optimziejvmold` — benchmarking and JVM performance work.
- `REA/tools` already contains CFR (`cfr-0.152.jar`) and Vineflower (`vineflower-1.12.0.jar`), so jars can be inspected without new setup.
- `rust/chess` — engine/search/benchmark experience that transfers to solver design.

### 1.4 State of the art and licenses

- **SeedCrackerX** (Fabric mod, MIT) supports 1.16.5 through current 26.x versions. Its documented cracking phases are:
  - `PILLARS`: enumerate `2^16` pillar seeds from End pillar heights.
  - `STRUCTURES`: search `2^32` partial seeds per pillar seed using structure data, multi-threaded.
  - `LIFTING`: alternative for modern structures; filter lower bits from structure offsets; roughly 40 liftable bits.
  - `STRUCTURE_REDUCE`: filter with additional structure data.
  - `BIOMES`: recover the upper bits using biome data, hashed seed, or decorators; may scan `2^16` upper values.
- Practical thresholds: ~32 regular bits, ~40 liftable bits.
- **Cubiomes** is MIT. **SeedCrackerX** is MIT (LICENSE copyright KaptainWutax 2020). KaptainWutax's SEED libraries are MIT-compatible.
- Implication: we can reuse/adapt MIT-licensed algorithms and structure configuration data with attribution. We do not need to reverse-engineer the math blindly.

---

## 2. Product

### 2.1 Definition

SeedAtlas is a local-first desktop workbench (CLI + local web UI) that:

1. Accepts evidence bundles in a versioned JSON format.
2. Localizes unknown builds from texture-rotation formations.
3. Plans and runs a staged seed search using the best available route.
4. Ranks candidate seeds and explains which evidence passed or failed.
5. Renders results on an interactive map and exports a reproducible report.

### 2.2 Users and jobs

**Primary user:** a seed-cracking hobbyist reconstructing a world from video/screenshots — lost base, old server, lore hunt, content recreation.

**Jobs to be done:**

- “I have footage of a base but don't know where it is or what seed it is.”
- “I have a pile of biome/structure observations and want ranked seed candidates.”
- “I want to know whether my evidence is sufficient before running a search for days.”
- “I want to hand someone a reproducible evidence bundle and result.”

**Secondary users:** speedrunners verifying set seeds, server admins identifying a world from backups, map makers.

### 2.3 Workflow

1. Create a project; choose edition, version, dimension, large-biomes flag.
2. Add evidence:
   - texture-rotation grid and anchor,
   - biome points,
   - structure sightings,
   - stronghold estimates,
   - End pillar heights,
   - spawn estimate,
   - optional partial seed/hash.
3. Run localization to turn unknown anchors into candidate world coordinates.
4. Review the difficulty estimate; add evidence if insufficient.
5. Solve; watch stage progress and candidate counts.
6. Inspect candidates on the map and match matrix.
7. Verify in Minecraft / Cubiomes Viewer.
8. Export HTML report + JSON bundle.

### 2.4 Positioning

| Tool | Strength | Gap SeedAtlas fills |
|---|---|---|
| SeedCrackerX | Live in-game collection; excellent structure lifting | Cannot use footage/screenshots; no offline evidence fusion; not a workbench |
| Cubiomes Viewer | Biome/structure maps, seed exploration | No reverse search from heterogeneous evidence |
| Chunkbase | Web seed maps | Not local, not offline, no evidence fusion |
| TextureRotations | Localizes builds from rotation patterns | No seed solving; one evidence type; hardcoded workflow |
| SeedAtlas | Offline fusion, localization, ranked candidates, reproducible reports | Needs enough evidence; not a live mod |

### 2.5 Non-goals (v1)

- In-game Fabric/NeoForge mod.
- Bedrock Edition.
- Full world downloader or chunk renderer.
- Automatic screenshot computer vision (manual/assisted in v1; v2 candidate).
- Guaranteed crack of arbitrary 64-bit seeds with insufficient evidence.
- Arbitrary seed *finding* by criteria (e.g., quad witch huts) — a different product.
- Nether/End full-world reconstruction.

### 2.6 Responsible use

Seed cracking can reveal private server seeds. SeedAtlas will:

- Default to local-only projects.
- Bind the UI server to `127.0.0.1`.
- Never auto-upload evidence or results.
- Include a permission/ethics note in the README and first-run UI.
- Avoid shipping Mojang assets; texture packs are user-provided.

---

## 3. Scope and releases

### v1 (MVP)

- Java 21 CLI + local web UI.
- Manual evidence entry and JSON import/export.
- Texture-rotation localization with a formation editor.
- Evidence types: texture rotations, biomes, structures, strongholds, spawn, End pillars, seed hints.
- Solver routes: lifting, pillar, staged brute-force fallback.
- Version profiles: 1.18.2, 1.19.4, 1.20.6, 1.21.x.
- Map: biome layer, evidence pins, anchor candidates, seed candidates.
- Report export: static HTML, JSON bundle, PNG map.
- Windows x64 native; Linux native for CI.

### v1.1

- SeedCrackerX saved-data import.
- Screenshot-assisted biome picking (click a color region / pick from palette).
- Next-evidence recommendations (expected information gain).
- More structure types and Nether/End structure evidence.
- 1.16.5 / 1.17.1 profiles if Cubiomes support is straightforward.
- Candidate verification shortcuts (open in Cubiomes Viewer, copy `/execute in ...` teleport commands).

### v2

- Automatic screenshot parsing (rotation IDs and biome colors).
- GPU/SIMD solver path.
- Shareable project bundles and a results gallery.
- Optional evidence-exporter plugin/mod.
- Multi-user/collaboration (probably out of scope; evaluate demand).

### Version support matrix (target)

| MC version | v1 status | Notes |
|---|---|---|
| 1.18.2 | Supported | Primary modern baseline; matches existing V4 work |
| 1.19.4 | Supported | Structure configs differ |
| 1.20.6 | Supported | |
| 1.21.x | Supported | Pin Cubiomes commit |
| 1.16.5 / 1.17.1 | v1.1 | Legacy structure/lifting rules |
| <= 1.12.2 | v2 | Texture rotations supported; solver rules differ significantly |
| 26.x | When Cubiomes supports | SeedCrackerX already supports; monitor |

---

## 4. Domain model

### 4.1 Two evidence classes

**Localization evidence (seed-independent)**

- `texture_rotation`: anchor-relative block coordinate, rotation ID, side/top flag, source, confidence.
- `block_pattern` (future): distinctive block arrangements/silhouettes.
- Output: candidate absolute anchor positions with scores.

**Seed evidence (seed-dependent)**

- `biome`: x, z, y, biome name/ID, confidence.
- `structure`: type, x/z (chunk or block), tolerance, confidence.
- `stronghold`: ring index, x/z, tolerance, confidence.
- `end_pillar`: 10 heights (or derived 16-bit pillar seed).
- `spawn`: x/z, tolerance, confidence.
- `seed_hint`: partial seed, hashed seed, known range.

This separation is mandatory. Texture rotations do not filter seeds; they tell you where the build is. Biome/structure evidence then filters seeds at those coordinates.

### 4.2 Anchors and frames

Evidence positions are either:

- `world`: absolute Minecraft block coordinates.
- `local:<anchorId>`: relative to an unresolved anchor.

The solver resolves anchors first. Each anchor resolution produces one or more candidate world positions with scores. The solver branches on anchor assignments if necessary and merges results.

### 4.3 Bundle schema (v1)

```json
{
  "schemaVersion": 1,
  "project": {
    "name": "lost-base-2026",
    "notes": "From YouTube footage, episode 12"
  },
  "world": {
    "edition": "java",
    "version": "1.21.4",
    "dimension": "overworld",
    "largeBiomes": false
  },
  "anchors": [
    {
      "id": "base",
      "frame": "local",
      "origin": null,
      "searchArea": { "xMin": -10000, "xMax": 10000, "zMin": -10000, "zMax": 10000, "yMin": 10, "yMax": 60 }
    }
  ],
  "evidence": [
    {
      "id": "rot-1",
      "type": "texture_rotation",
      "anchor": "base",
      "x": 0, "y": 64, "z": 0,
      "rotation": 1,
      "side": false,
      "source": "frame_0012.png",
      "confidence": 1.0
    },
    {
      "id": "biome-1",
      "type": "biome",
      "frame": "world",
      "x": -1621, "y": 200, "z": 913,
      "biome": "flower_forest",
      "source": "aerial_03.png",
      "confidence": 0.95
    },
    {
      "id": "struct-1",
      "type": "structure",
      "frame": "world",
      "structure": "village_plains",
      "x": 1234, "z": -567,
      "tolerance": 16,
      "source": "map.png",
      "confidence": 0.9
    },
    {
      "id": "sh-1",
      "type": "stronghold",
      "ring": 1,
      "x": -2663, "z": -655,
      "tolerance": 112,
      "source": "eye_throw.png",
      "confidence": 0.8
    }
  ],
  "constraints": {
    "seedRange": null,
    "knownSeedPrefix": null,
    "excludeSeeds": []
  }
}
```

Schema rules:

- `schemaVersion` is required and migrated explicitly.
- Coordinates are block coordinates unless a field says `chunk`.
- Negative coordinates use floor division for region/chunk math.
- Biome names normalize per version to Cubiomes IDs.
- Structure names normalize per version to Cubiomes enum values.
- Evidence IDs are stable and unique; provenance is preserved.
- Duplicate/conflicting evidence is reported, not silently dropped.

### 4.4 Validation

- Schema validation with actionable errors.
- Version/biome/structure existence checks.
- Coordinate plausibility (e.g., y range per version).
- Tolerance sanity (too large = low information).
- Conflicting evidence detection.
- Difficulty estimate before solve.
- “Why no candidates” diagnostics after solve:
  - impossible evidence combination,
  - wrong version,
  - wrong dimension,
  - insufficient bits,
  - localization failed.

### 4.5 Difficulty estimator

Approximate information bits:

- Uniform structure: `log2(offset^2)` (SeedCrackerX's documented model).
- Triangular structure: `log2(peak^2)`.
- Biome: `-log2(p(biome))`, using empirical frequencies or a sampled estimate.
- Stronghold ring: log2 of the searched area ratio.
- End pillars: 16 bits.
- Spawn: depends on tolerance and biome.

Display:

- `< 24 bits`: insufficient; add structure/biome evidence.
- `24–32 bits`: marginal; may return many candidates.
- `32–40 bits`: normal route viable.
- `>= 40 liftable bits`: lifting route viable.
- Estimated lower-48 candidates: `2^(48 - bits)`.
- Estimated upper-16 expansion: `candidates * 65,536` biome checks.

The estimator is advisory; actual difficulty depends on evidence quality and version.

---

## 5. Solver design

### 5.1 Pipeline

1. Load + validate bundle.
2. Normalize evidence for the selected version.
3. Resolve anchors (localization).
4. Build constraint set per anchor assignment.
5. Plan: estimate selectivity and cost; order checks.
6. Recover lower-48 structure seeds (Route A/B/C).
7. Recover upper-16 world seeds (biome/hash).
8. Full verification (strongholds, spawn, structure viability, portal eyes).
9. Rank candidates; build match matrix.
10. Persist results, logs, and report.

### 5.2 Route A — Structure lifting (preferred for modern versions)

**Input:** roughly >= 40 liftable bits from structures whose offsets can be lifted (igloo, desert pyramid, jungle temple, swamp hut, shipwreck, pillager outpost).

**Steps:**

1. For each structure observation, derive region coordinates and the offset within the region.
2. Enumerate low seed bits consistent with the first structure offset using LCG inversion.
3. Extend candidates using additional structures; validate each with Cubiomes `getStructurePos`.
4. Output candidate lower-48 structure seeds.

**Implementation strategy:**

- Reuse/adapt the MIT-licensed SeedCrackerX/SEED lifting approach.
- Keep the algorithm behind a `StructureSeedSolver` interface so it can be tested independently.
- Cross-validate every candidate against Cubiomes as the source of truth.

**Expected performance:** seconds to minutes for sufficient evidence.

### 5.3 Route B — End pillars + structures

**Input:** 10 End pillar heights + roughly >= 32 regular structure bits.

**Steps:**

1. Enumerate `2^16` pillar seeds from the observed pillar heights.
2. For each pillar seed, search `2^32` partial world seeds with structure constraints.
3. Reduce candidates with additional structure data.
4. Output lower-48 structure seeds.

**Implementation strategy:**

- Multi-threaded; early exit per structure.
- Use the same `StructureSeedSolver` interface and Cubiomes validation.
- This is the route SeedCrackerX documents as taking ~1–5 minutes; target comparable performance.

### 5.4 Route C — Staged brute force (fallback)

Use when neither lifting nor pillar evidence is sufficient.

**Steps:**

1. Scan lower-48 seed space with cheapest filters first:
   - structure positions (region/offset),
   - stronghold ring geometry,
   - coarse biome checks (scale 16/64).
2. For each survivor, iterate upper 16 and run fine biome checks.
3. Apply expensive verification last.

**Guardrails:**

- Show estimated time before starting.
- Support seed ranges and partial seed hints.
- Cap candidates; if the set is too large, stop and recommend more evidence.
- Resumable checkpoints.
- GPU/SIMD path deferred to v2, but keep the engine interface ready.

### 5.5 Upper-16 recovery

For every lower-48 candidate:

1. Iterate `65,536` upper-16 values.
2. Apply:
   - known hashed seed (if available) — exact filter,
   - biome points — early-exit,
   - decorator/biome evidence,
   - optional spawn/stronghold verification.
3. Keep full 64-bit candidates.

If the lower-48 candidate set is too large, require more structure evidence or a seed range.

### 5.6 Check ordering

Generalize the `RejectRate` benchmark:

- Estimate pass probability and cost for each check.
- Sort by `information_bits / cost`.
- Use coarse-to-fine biome checks.
- Position checks before viability checks.
- Analytic stronghold ring check before full `initFirstStronghold`/`nextStronghold`.
- Spawn and portal eyes last.

### 5.7 Performance

- Native batch loop over seed ranges; avoid per-seed JNI calls.
- Separate thread pools for structure search and biome verification.
- Work-stealing / range partitioning for deterministic results.
- Progress: stage, seeds/sec, ETA, candidates found, rejection rates.
- Checkpoint candidate sets and resume.
- Benchmarks:
  - structure position check,
  - stronghold check,
  - biome check at scales 1/4/16/64,
  - full verification.
- Calibrate ETA from measured throughput on the target machine.

### 5.8 Ranking and explanation

- Score = sum of log-likelihoods.
- Exact matches weighted highest; tolerance-based matches partial; confidence scales contribution.
- Deduplicate candidates.
- Match matrix: evidence row × candidate column, pass/fail/partial.
- Show which evidence would best split remaining candidates.

### 5.9 Correctness

- Lower-48/upper-16 split must be tested explicitly.
- Cubiomes is the reference for structure positions, biome IDs, and strongholds.
- Compare lifting output against brute force on small seed ranges.
- Golden-seed regression tests across versions.
- No silent false negatives: if the known seed is in the search space, it must appear.

---

## 6. Architecture

### 6.1 Modules

```
seedatlas/
  settings.gradle.kts
  build.gradle.kts
  gradle/libs.versions.toml
  core/        # model, schema, validation, planner, difficulty estimator
  engine/      # lifting, pillar, brute force, solver interfaces
  native/      # JNI wrapper + CMake + Cubiomes submodule
  ingest/      # TextureRotations, SeedCrackerX, JSON, CSV importers
  cli/         # Picocli commands
  server/      # Javalin/Undertow + static UI hosting
  ui/          # React + TypeScript + Vite + Leaflet
  testkit/     # fixtures, synthetic evidence generator, golden tests
  docs/        # schema, architecture, version support, build guide
```

### 6.2 Tech stack

- Java 21 toolchain (installed: 21.0.8).
- Gradle Kotlin DSL.
- JUnit 5, Jackson, Picocli, SLF4J/Logback.
- Javalin or Undertow for the local server.
- React + TypeScript + Vite + Leaflet (`CRS.Simple`) for the UI.
- CMake + C for the native layer.
- Playwright for UI smoke tests (already in your webdev work).

### 6.3 Native layer

- Pin a Cubiomes commit that supports the target versions.
- Build `libcubiomes` as a static library and link into a JNI shared library:
  - `seedatlas.dll` (Windows x64)
  - `libseedatlas.so` (Linux x64)
  - optional `libseedatlas.dylib` (macOS)
- JNI functions (initial set):
  - `getBiomeAt(seed, x, y, z, scale)`
  - `checkBiomes(seed, xs[], ys[], zs[], ids[])`
  - `getStructurePos(type, version, seed48, regX, regZ)`
  - `isViableStructurePos(type, version, seed, x, z)`
  - `initFirstStronghold` / `nextStronghold`
  - `getSpawn` / `estimateSpawn`
  - `getStructureConfig(type, version)`
  - `scanStructureSeeds(range, constraints[])` — native loop for speed
- Thread safety: one `Generator` per native thread; no shared mutable state.
- Input validation at the JNI boundary; fail with error codes, not crashes.
- Load natives from JAR resources to a temp directory (as V4 does) or via `java.library.path`.
- CI matrix: Windows + Ubuntu; build native and run smoke tests.

### 6.4 Packaging

- Fat JAR + platform natives in resources.
- `jpackage` portable zip/installer for Windows.
- CLI launcher script.
- GitHub Releases with checksums.
- License notices for Cubiomes, SeedCrackerX/SEED, and other MIT dependencies.
- No Mojang assets redistributed.

### 6.5 Suggested repository layout

```
SeedAtlas/
  README.md
  LICENSE
  NOTICE
  docs/
    architecture.md
    evidence-schema.md
    version-support.md
    build.md
  core/
  engine/
  native/
    CMakeLists.txt
    cubiomes/            # git submodule or vendored copy
    src/                 # JNI wrapper
  ingest/
  cli/
  server/
  ui/
  testkit/
    fixtures/
      known-1.18/
      known-1.21/
      synthetic/
```

---

## 7. UI/UX

### Screens

1. **Project dashboard**
   - World version, dimension, evidence count.
   - Difficulty estimate and recommended next evidence.
   - Solve button; recent runs.

2. **Evidence editor**
   - Texture-rotation grid: add formation blocks, paste rotation numbers, set anchor, side/top flags.
   - Biome picker: searchable biome list, map click, confidence.
   - Structure picker: type, coordinates, tolerance.
   - Stronghold/spawn/End-pillar forms.
   - Source/provenance and notes.
   - Validation warnings inline.

3. **Map**
   - Pan/zoom Minecraft coordinates.
   - Biome layer (server-generated PNG tiles, cached).
   - Evidence pins with icons and tooltips.
   - Anchor candidates highlighted.
   - Candidate seed markers; selecting one shows details.
   - Coordinate readout and copy.

4. **Solve view**
   - Stage timeline: localization → structure seeds → upper-16 → verification.
   - Progress, seeds/sec, ETA, candidates found.
   - Candidate table: seed, score, matched/failed evidence.
   - Match matrix.
   - “Next evidence” suggestions.

5. **Report**
   - Static HTML with embedded map snapshot.
   - Evidence table with provenance.
   - Candidate seeds and match results.
   - Export JSON bundle and PNG.

6. **Settings**
   - Versions, threads, memory, tile cache, import/export.

### UX principles

- Local-first: no account, no cloud.
- Explain every failed solve.
- Never hide uncertainty: show candidate counts and confidence.
- Keyboard-friendly; color-blind-safe icons; not color-only status.
- Responsive enough for a laptop; no heavy 3D.

---

## 8. Milestones

### M0 — Spike (2–3 days)

**Goal:** prove the native + solver foundations before building product surface.

- [x] Build Cubiomes for Windows x64.
- [x] Create a minimal JNI wrapper (`getBiomeAt`, `getStructurePos`, stronghold, batch biome, structure box scan).
- [x] Verify a known seed's biome/structure/stronghold data.
- [ ] Benchmark the existing `StrongholdFinder` and `scanSeeds`.
- [ ] Prototype structure lifting on 4 structures from a known seed.
- [ ] Decide: lifting route vs staged brute-force first.

**Acceptance:**

- Windows DLL loads from Java.
- Known seed verifies against Cubiomes.
- Lifting recovers the 48-bit structure seed in < 60s on a small fixture.
- Go/no-go memo with measured throughput.

### M1 — Foundation (1 week)

- [x] Create repo, Gradle multi-module build.
- [ ] CI skeleton.
- [x] Implement evidence schema, validation, version profiles.
- [x] Implement CLI: `init`, `validate`, `info`, `localize`, `ingest`, `derive`, `native`, `solve` (dry run), `version`.
- [ ] CLI: `add`, `report`, `serve`.
- [x] Port texture-rotation providers and tests into `core`/`ingest`.
- [x] Add JSON examples and docs.

**Acceptance:**

- `seedatlas validate` accepts/rejects fixture bundles correctly.
- Existing rotation tests pass under JUnit.
- Schema docs complete.

### M2 — Solver v1 (2 weeks)

- Implement `StructureSeedSolver` interface and routes A/B/C.
- Implement upper-16 biome recovery.
- Implement planner, check ordering, parallelism, progress, resume.
- [x] Add synthetic evidence generator (`derive`).
- [x] Add difficulty estimator.
- [ ] Add golden fixtures from `derive`.

**Acceptance:**

- Known 1.18 fixture returns the known seed in top-1.
- Ten synthetic seeds across 1.18.2/1.19.4/1.20.6/1.21.4 pass.
- Deterministic results across runs.
- Throughput meets the M0 target (or documented deviation).

### M3 — Localization + ingest (1 week)

- [x] Implement texture-rotation localization command and anchor resolution.
- [ ] Import from TextureRotations output.
- [x] Import SeedCrackerX saved data/config (`ingest`).
- [x] Add fixtures for rotation formations and imports.

**Acceptance:**

- `seedatlas localize` finds the known anchor for a fixture.
- A SeedCrackerX data dump converts into a valid bundle.
- End-to-end: localize → solve → candidate.

### M4 — UI v1 (2 weeks)

- Project dashboard, evidence editor, map, solve view, report.
- Server-side tile generation + cache.
- Playwright smoke tests.

**Acceptance:**

- From a blank project, a user can add evidence, solve, see candidates on the map, and export a report in the browser.
- Smoke test passes in CI.

### M5 — Packaging + beta (1 week)

- Windows/Linux natives in release artifact.
- `jpackage` portable zip.
- Docs: build, schema, version support, troubleshooting.
- Demo project and short demo video/GIF.
- License/attribution review.

**Acceptance:**

- Release runs on a clean Windows machine with no manual DLL setup.
- CI green on Windows + Ubuntu.
- A new user can complete the sample project using only the docs.

**Total:** ~7–8 weeks part-time, with M2 as the main schedule risk.

---

## 9. Testing

### Unit

- JavaRandom vs `java.util.Random` for `nextInt`/`nextLong`.
- Structure position functions vs Cubiomes.
- Biome ID/name mapping per version.
- Texture-rotation providers (port existing tests + 1.21.2+).
- Schema validation and migration.
- Difficulty estimator math.

### Golden fixtures

- Known seed per supported version.
- Synthetic evidence generated from the seed (structures + biomes), shuffled/limited.
- Your current 1.18 world, if the seed is known.
- Negative fixtures: wrong version, impossible biome, conflicting structures.

### Integration

- CLI end-to-end on fixtures.
- UI Playwright smoke test.
- Import/export round-trip (JSON → project → JSON).
- Localization → solve pipeline.

### Performance

- JMH for planner and Java orchestration.
- Native microbenchmarks for structure/biome/stronghold checks.
- Regression tracking for seeds/sec.
- Long-running tests nightly, not on every PR.

### Manual validation

- Cross-check candidate seeds in Cubiomes Viewer and Minecraft.
- Compare structure-seed output with SeedCrackerX on the same data.
- Verify texture-rotation coordinates in-game with the resource packs.

### CI

- GitHub Actions: Windows x64 + Ubuntu x64.
- Java 21, Gradle cache, CMake, native build.
- Unit + integration tests; long performance tests nightly.
- Release workflow with checksums.

---

## 10. Risks and mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Lifting algorithm complexity | High | M0 spike; reuse MIT SeedCrackerX/SEED; fallback to staged brute force |
| Search space too large / no unique seed | High | Difficulty estimator; evidence thresholds; seed ranges; require more evidence; GPU in v2 |
| Cubiomes version drift | Medium | Pin commit; version profiles; per-version integration tests |
| Windows native build pain | Medium | CI matrix; prebuilt DLL; document MSVC/MinGW; minimal JNI surface |
| JNI crashes | Medium | Validate inputs; one generator per thread; fuzz; consider subprocess isolation later |
| Texture-rotation ambiguity | Medium | Multiple anchor candidates; confidence; user verification; coarse-to-fine matching |
| UI scope creep | Medium | CLI-first; fixed v1 screens; UI after solver works |
| Performance variance | Medium | Benchmark on target hardware; progress/ETA; resumable jobs |
| Data quality / user error | Medium | Provenance, confidence, validation warnings, “why no candidates” |
| Licensing | Low | MIT compliance; NOTICE file; no Mojang assets |
| Security | Low | Local-only server; bind 127.0.0.1; validate paths; no remote code |
| Scope drift into a live mod | Medium | Explicit non-goal; integrate via import, not Fabric |

---

## 11. Open decisions

1. **Primary use case:** media/screenshot-based cracking with SeedCrackerX import (recommended) vs in-game companion.
2. **Target versions:** 1.18.2–1.21.x (recommended); add 1.16.5/1.17.1 if cheap.
3. **Known fixture:** seed of the 1.18 world targeted by SeedCracker V4? Needed for a real acceptance test.
4. **Cubiomes source:** do we have the source and `seedcracker_CubiomesJNI.c` used to build `libseedcracker.so`?
5. **UI:** local web app (recommended) vs JavaFX desktop.
6. **Release:** public open-source MIT (recommended) vs personal tool.
7. **Hardware:** CPU cores, RAM, GPU — affects fallback route and GPU planning.
8. **Screenshot CV:** manual/assisted in v1 (recommended) vs automatic parsing in v1.
9. **Package name:** `dev.seedatlas` / `io.github.<user>.seedatlas` / other.
10. **Integration depth:** import SeedCrackerX data only, or also export evidence back to it?

---

## 12. External tools and artifacts to inspect

Priority order. Read-only inspection with REA/CFR/Vineflower.

| Priority | Artifact | Why |
|---|---|---|
| P0 | Cubiomes source + `seedcracker_CubiomesJNI.c` | Confirms exact API/version; lets us extend the wrapper instead of rebuilding blind |
| P0 | Known world bundle: seed + screenshots + coordinates | Golden test; validates localization + solver end-to-end |
| P1 | SeedCrackerX jar (latest 1.21.x) | MIT; inspect lifting/pillar implementation, structure configs, data export format |
| P1 | SeedCrackerX saved data/config from a real world | Realistic evidence format; import tests |
| P2 | Cubiomes Viewer latest | UX/feature parity; map rendering reference |
| P2 | Amidst jar | Optional map UX reference |
| P3 | Manual texture rotations + Textures to numbers resource packs | Automated rotation tests |
| P3 | Minecraft 1.21.x server jar | Only if structure configs need verification; likely not |

Suggested drop location: `C:\Users\rampi\Coding\minecraft\_refs\` (create when ready; tell me the path).

If only two things are available, prioritize:

1. Cubiomes source + JNI wrapper.
2. One known seed + evidence bundle.

---

## 13. What can proceed while waiting

Even without the missing artifacts:

- M1 foundation: schema, validation, CLI, version profiles, texture-rotation provider port.
- UI scaffold and map tile service prototype.
- Synthetic fixtures generated from Cubiomes upstream.
- Repo/build/CI setup.
- Documentation skeleton.

M0's lifting proof and M2's solver validation need the Cubiomes source/native build and at least one known fixture.

---

## 14. Definition of done (v1)

- A user can create a project, enter texture rotations + biome/structure/stronghold evidence, localize the build, solve, and get a ranked seed list.
- The known fixture seed is recovered top-1 within the documented time.
- The result is reproducible from the JSON bundle.
- The map shows biome context, evidence, and candidates.
- A shareable HTML report and JSON bundle can be exported.
- Windows release runs without manual native setup.
- Docs cover schema, build, version support, and troubleshooting.
- License/attribution is complete.
- No silent false negatives on the golden test suite.

---

## Appendix A — Key technical notes

### Lower 48 vs upper 16

- Minecraft's `JavaRandom` uses a 48-bit state. Many structure/stronghold placements depend only on the lower 48 bits of the world seed.
- Biomes (especially 1.18+) can depend on the full 64-bit seed.
- Correct pipeline: scan lower 48 with cheap deterministic filters, then expand the upper 16 only for survivors and apply biome checks.
- The existing V4 full-64-bit scan repeats lower-48 work 65,536 times; avoid that.

### Texture rotations

- Rotation value is a deterministic function of `(x, y, z)` and the version's random algorithm.
- It is seed-independent; it localizes a build, not a seed.
- Localization can be optimized with early-exit/subset matching and a restricted search area; the full 20k×20k×50 search is expensive.

### SeedCrackerX phases (public behavior)

- `PILLARS`: 2^16 pillar seeds from End pillar heights.
- `STRUCTURES`: 2^32 partial seeds per pillar seed with structure constraints.
- `LIFTING`: lower-bit filtering from liftable structure offsets (~40 bits).
- `STRUCTURE_REDUCE`: additional structure filtering.
- `BIOMES`: upper-bit recovery via biome/hash/decorator evidence.
- Thresholds: ~32 regular bits, ~40 liftable bits.

### Licenses

- Cubiomes: MIT.
- SeedCrackerX: MIT (copyright KaptainWutax 2020).
- KaptainWutax SEED libraries: MIT-compatible.
- Include a `NOTICE` file and preserve copyright headers when adapting code.

---

## Appendix B — Glossary

- **Anchor:** a local coordinate frame whose world position is unknown until localized.
- **Biome point:** a known biome at a world coordinate.
- **Lifting:** recovering structure-seed bits from structure placement offsets using LCG math.
- **Lower 48:** the 48-bit portion of the world seed used by many structure placements.
- **Pillar seed:** 16-bit seed derived from End pillar heights.
- **Structure seed:** the lower-48 seed used for structure placement.
- **Upper 16:** the remaining bits needed to form the full 64-bit world seed.
- **Viability:** whether a structure can generate at a position given the biome/terrain.
