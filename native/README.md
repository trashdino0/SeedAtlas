# Native bridge

JNI wrapper around [Cubiomes](https://github.com/Cubitect/cubiomes), used for
biome lookup, structure placement/viability, strongholds, and spawn estimates.

## Layout

- `cubiomes/` — vendored Cubiomes source (`master`, MC versions through 1.21).
- `src/seedatlas_jni.c` — JNI entry points consumed by `nativebridge`.
- `build-cubiomes.bat` — compiles Cubiomes into `build/cubiomes.lib`.
- `build-jni.bat` — compiles and links `build/seedatlas.dll`.

`build/` is ignored by git; the DLL is bundled into the `nativebridge` JAR at
build time when it exists.

## Build

Requires Visual Studio 2022 with the C++ workload and a JDK 21 at
`C:\Program Files\Java\jdk-21` (override `JAVA_HOME` in `build-jni.bat` if
yours differs).

```powershell
.\build-cubiomes.bat
.\build-jni.bat
```

Both scripts call `vcvars64.bat` themselves and are safe to run from any
directory.

## Exported functions

| Java method | Purpose |
| --- | --- |
| `getBiomeAt` | One biome lookup at scale 1/4/16/64/256. |
| `biomesAt` | Batch biome lookup with one generator. |
| `getStructurePos` | Raw structure generation attempt for a region. |
| `getStructureConfig` | Salt, region size, and range for a structure type. |
| `isViableStructurePos` | Biome/terrain viability check. |
| `findStructures` | All viable attempts in a region box, sharing one generator. |
| `strongholds` | First N strongholds. |
| `spawn` | Exact (`getSpawn`) or estimated spawn. |
| `biomeName` | Resource-id biome name. |
| `newestVersion` | Cubiomes `MC_NEWEST` id. |

## Licensing

Cubiomes is MIT. Its license text is in `cubiomes/LICENSE` and attribution is
recorded in the repository `NOTICE`.
