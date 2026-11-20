# SeedAtlas

Offline Minecraft Java seed-investigation workbench. It fuses texture-rotation
localization, biome samples, structure sightings, stronghold geometry, End pillar
heights, and spawn estimates into ranked candidate seeds.

This repository is at **M1 (foundation) + M0 (native bridge)**: evidence schema,
validation, version profiles, texture-rotation providers, difficulty estimation,
a CLI, and a working Cubiomes JNI bridge on Windows.

The full roadmap is in [PLAN.md](PLAN.md).

## Requirements

- Java 21+
- Windows, Linux, or macOS
- Optional, for the native bridge: Visual Studio 2022 C++ tools on Windows.
  The pure-Java commands (`init`, `validate`, `info`, `localize`) work without it.

## Build

```powershell
.\gradlew.bat build
```

The native bridge is a separate step because it needs a C toolchain:

```powershell
native\build-cubiomes.bat
native\build-jni.bat
.\gradlew.bat build
```

The resulting `native\build\seedatlas.dll` is bundled into the
`nativebridge` JAR and extracted at runtime. See [native/README.md](native/README.md).

## CLI

```powershell
.\gradlew.bat :cli:run --args="version"
.\gradlew.bat :cli:run --args="init my-project --name LostBase --version 1.21.4"
.\gradlew.bat :cli:run --args="validate my-project/project.json"
.\gradlew.bat :cli:run --args="info my-project/project.json"
```

Or install a launcher:

```powershell
.\gradlew.bat :cli:installDist
.\cli\build\install\seedatlas\bin\seedatlas.bat version
```

### Commands

| Command | Purpose |
| --- | --- |
| `init` | Create a new evidence bundle. |
| `validate` | Check a bundle for errors and weak evidence. |
| `info` | Show the bundle summary and estimated information content. |
| `localize` | Resolve local anchors from texture-rotation evidence. |
| `ingest` | Import a SeedCrackerX saved-structures file. |
| `derive` | Generate a synthetic evidence bundle from a known seed via Cubiomes. |
| `solve` | Solver entry point (currently a dry run). |
| `native` | Show Cubiomes native-bridge diagnostics. |

Examples:

```powershell
seedatlas localize examples\localize\project.json --threads 4
seedatlas ingest "%APPDATA%\.minecraft\config\SeedCrackerX saved structures\MyWorld.txt" --version 1.21.4 --out my-project\seedcracker-import.json
seedatlas derive --seed 3055141959546 --version 1.21.4 --regions 1 --biomes 1 --out examples\derived\project.json
seedatlas info examples\derived\project.json
seedatlas native --version 1.21.4
```

## Status

- [x] Repo scaffold and Gradle build
- [x] Evidence schema v1
- [x] Validation
- [x] Version profiles
- [x] Texture-rotation providers (Vanilla 1.12 / 1.13-1.21.1 / 1.21.2+, Sodium)
- [x] Difficulty estimator
- [x] CLI: `init`, `validate`, `info`, `localize`, `ingest`, `derive`, `solve`, `native`, `version`
- [x] Cubiomes native build (M0, Windows/MSVC)
- [x] Synthetic evidence generator (`derive`)
- [x] SeedCrackerX saved-structures importer (`ingest`)
- [ ] Structure lifting / pillar solver (M2)
- [ ] Localization runner for real screenshots (M3)
- [ ] Web UI (M4)

## License

MIT. See `NOTICE` for third-party attributions.
