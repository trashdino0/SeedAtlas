package dev.seedatlas.testkit;

import dev.seedatlas.core.model.BiomeEvidence;
import dev.seedatlas.core.model.Constraints;
import dev.seedatlas.core.model.Evidence;
import dev.seedatlas.core.model.Project;
import dev.seedatlas.core.model.ProjectInfo;
import dev.seedatlas.core.model.SeedHintEvidence;
import dev.seedatlas.core.model.SpawnEvidence;
import dev.seedatlas.core.model.StrongholdEvidence;
import dev.seedatlas.core.model.StructureEvidence;
import dev.seedatlas.core.model.WorldDescriptor;
import dev.seedatlas.nativebridge.BiomeRegistry;
import dev.seedatlas.nativebridge.Cubiomes;
import dev.seedatlas.nativebridge.CubiomesVersion;
import dev.seedatlas.nativebridge.StructurePos;
import dev.seedatlas.nativebridge.StructureType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Derives a synthetic evidence bundle from a known seed using Cubiomes.
 *
 * <p>This is the inverse of the solver and the backbone of golden tests: a
 * fixture is generated from seed S, then the solver must recover S from the
 * evidence alone. Set {@code includeSeedHint} to false for a real test and
 * true when the bundle should be self-describing.</p>
 */
public final class SeedEvidenceGenerator {

    /**
     * Structures whose positions Cubiomes can place and validate reliably for
     * the supported versions. Structures are skipped when the version has no
     * config for them (for example trial chambers before 1.21).
     */
    public static final Set<StructureType> DEFAULT_STRUCTURES = EnumSet.of(
            StructureType.VILLAGE,
            StructureType.DESERT_PYRAMID,
            StructureType.JUNGLE_TEMPLE,
            StructureType.SWAMP_HUT,
            StructureType.IGLOO,
            StructureType.SHIPWRECK,
            StructureType.MONUMENT,
            StructureType.MANSION,
            StructureType.OUTPOST,
            StructureType.ANCIENT_CITY,
            StructureType.RUINED_PORTAL,
            StructureType.TRIAL_CHAMBERS
    );

    private static final int SURFACE_Y = 320;
    private static final int BIOME_GRID_SPACING = 256;
    private static final int STRONGHOLD_COUNT = 3;

    private SeedEvidenceGenerator() {
    }

    /**
     * @param seed             known 64-bit world seed
     * @param requestedVersion version string to record in the bundle
     * @param version          Cubiomes version used for generation
     * @param regionRadius     region box is {@code [-radius, +radius]} on both axes
     * @param biomeRadius      biome grid is {@code [-radius, +radius]} samples
     * @param includeSeedHint  whether to embed the true seed as evidence
     */
    public static Project derive(
            long seed,
            String requestedVersion,
            CubiomesVersion version,
            int regionRadius,
            int biomeRadius,
            boolean includeSeedHint
    ) {
        if (regionRadius < 0) {
            throw new IllegalArgumentException("regionRadius must be >= 0");
        }
        if (biomeRadius < 0) {
            throw new IllegalArgumentException("biomeRadius must be >= 0");
        }

        List<Evidence> evidence = new ArrayList<>();

        if (includeSeedHint) {
            evidence.add(new SeedHintEvidence(
                    "seed",
                    seed,
                    null,
                    null,
                    "fixture: known seed",
                    1.0
            ));
        }

        int structureId = 0;
        for (StructureType type : DEFAULT_STRUCTURES) {
            if (Cubiomes.structureConfig(type, version).isEmpty()) {
                continue;
            }
            List<StructurePos> positions = Cubiomes.findStructures(
                    type,
                    version,
                    seed,
                    -regionRadius,
                    -regionRadius,
                    regionRadius,
                    regionRadius
            );
            for (StructurePos pos : positions) {
                evidence.add(new StructureEvidence(
                        "struct-" + (++structureId),
                        "world",
                        type.canonicalName(),
                        pos.x(),
                        pos.z(),
                        8,
                        "fixture:cubiomes",
                        1.0
                ));
            }
        }

        for (StrongholdEvidence stronghold : strongholds(seed, version)) {
            evidence.add(stronghold);
        }

        Cubiomes.spawn(version, seed, true).ifPresent(spawn -> evidence.add(
                new SpawnEvidence(
                        "spawn-1",
                        spawn.x(),
                        spawn.z(),
                        64,
                        "fixture:cubiomes:estimateSpawn",
                        0.6
                )
        ));

        evidence.addAll(biomes(seed, version, biomeRadius));

        return new Project(
                Project.CURRENT_SCHEMA_VERSION,
                new ProjectInfo(
                        "Derived fixture",
                        "Generated by SeedAtlas from a known seed. Not a real player world."
                ),
                new WorldDescriptor("java", requestedVersion, "overworld", false),
                List.of(),
                evidence,
                Constraints.empty()
        );
    }

    private static List<StrongholdEvidence> strongholds(long seed, CubiomesVersion version) {
        List<dev.seedatlas.nativebridge.StructurePos> positions =
                Cubiomes.strongholds(version, seed, STRONGHOLD_COUNT);
        List<StrongholdEvidence> evidence = new ArrayList<>(positions.size());
        for (int i = 0; i < positions.size(); i++) {
            var pos = positions.get(i);
            evidence.add(new StrongholdEvidence(
                    "sh-" + (i + 1),
                    i + 1,
                    pos.x(),
                    pos.z(),
                    16,
                    "fixture:cubiomes",
                    1.0
            ));
        }
        return evidence;
    }

    private static List<BiomeEvidence> biomes(
            long seed,
            CubiomesVersion version,
            int radius
    ) {
        List<int[]> grid = new ArrayList<>();
        for (int gx = -radius; gx <= radius; gx++) {
            for (int gz = -radius; gz <= radius; gz++) {
                grid.add(new int[]{
                        gx * BIOME_GRID_SPACING,
                        SURFACE_Y,
                        gz * BIOME_GRID_SPACING
                });
            }
        }

        int[] points = new int[grid.size() * 3];
        for (int i = 0; i < grid.size(); i++) {
            int[] point = grid.get(i);
            points[i * 3] = point[0];
            points[i * 3 + 1] = point[1];
            points[i * 3 + 2] = point[2];
        }

        int[] biomeIds = Cubiomes.biomesAt(version, seed, 1, points);
        List<BiomeEvidence> evidence = new ArrayList<>(biomeIds.length);
        for (int i = 0; i < biomeIds.length; i++) {
            int[] point = grid.get(i);
            String name = BiomeRegistry.name(version, biomeIds[i]);
            evidence.add(new BiomeEvidence(
                    "biome-" + (i + 1),
                    "world",
                    point[0],
                    point[1],
                    point[2],
                    name,
                    "fixture:cubiomes",
                    1.0
            ));
        }
        return evidence;
    }
}
