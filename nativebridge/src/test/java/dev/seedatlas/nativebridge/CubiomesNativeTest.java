package dev.seedatlas.nativebridge;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CubiomesNativeTest {

    @Test
    void loadsNativeLibrary() {
        assertTrue(Cubiomes.isAvailable(), () -> "native load error: " + NativeLoader.error());
    }

    @Test
    void readsKnownBiome() {
        int biome = Cubiomes.biomeAt(CubiomesVersion.MC_1_18_2, 0L, 0, 64, 0, 1);
        assertTrue(biome >= 0, "expected a valid biome id");
        assertEquals("plains", Cubiomes.biomeName(CubiomesVersion.MC_1_18_2, 1));
    }

    @Test
    void buildsBiomeRegistry() {
        assertTrue(BiomeRegistry.id(CubiomesVersion.MC_1_18_2, "plains").isPresent());
        assertEquals(1, BiomeRegistry.id(CubiomesVersion.MC_1_18_2, "plains").getAsInt());
        assertTrue(BiomeRegistry.id(CubiomesVersion.MC_1_18_2, "flower_forest").isPresent());
    }

    @Test
    void readsStructureConfig() {
        StructureConfigInfo config = Cubiomes.structureConfig(
                StructureType.VILLAGE,
                CubiomesVersion.MC_1_18_2
        ).orElseThrow();

        assertEquals(34, config.regionSize());
        assertEquals(26, config.chunkRange());
    }

    @Test
    void computesStructurePosition() {
        StructurePos pos = Cubiomes.structurePos(
                StructureType.VILLAGE,
                CubiomesVersion.MC_1_18_2,
                0L,
                0,
                0
        ).orElseThrow();

        // getStructurePos returns block coordinates for the generation attempt.
        assertTrue(pos.x() >= 0 && pos.x() < 34 * 16, "x=" + pos.x());
        assertTrue(pos.z() >= 0 && pos.z() < 34 * 16, "z=" + pos.z());
        assertEquals(0, pos.x() % 16, "x should be a chunk origin");
        assertEquals(0, pos.z() % 16, "z should be a chunk origin");
    }

    @Test
    void computesStrongholds() {
        List<StructurePos> strongholds = Cubiomes.strongholds(
                CubiomesVersion.MC_1_18_2,
                3055141959546L,
                3
        );

        assertFalse(strongholds.isEmpty());
        assertTrue(strongholds.size() <= 3);
    }

    @Test
    void findsViableStructuresInRegionBox() {
        List<StructurePos> villages = Cubiomes.findStructures(
                StructureType.VILLAGE,
                CubiomesVersion.MC_1_18_2,
                3055141959546L,
                -2, -2, 2, 2
        );

        assertFalse(villages.isEmpty());
        for (StructurePos pos : villages) {
            assertTrue(Cubiomes.isViableStructurePos(
                    StructureType.VILLAGE,
                    CubiomesVersion.MC_1_18_2,
                    3055141959546L,
                    pos.x(),
                    pos.z()
            ), "not viable: " + pos);
        }
    }

    @Test
    void batchesBiomeLookups() {
        int[] points = {
                0, 320, 0,
                256, 320, 256,
                -1024, 320, 512
        };

        int[] biomes = Cubiomes.biomesAt(
                CubiomesVersion.MC_1_18_2,
                3055141959546L,
                1,
                points
        );

        assertEquals(3, biomes.length);
        for (int biome : biomes) {
            assertTrue(biome >= 0, "invalid biome");
        }
    }

    @Test
    void computesSpawn() {
        StructurePos spawn = Cubiomes.spawn(
                CubiomesVersion.MC_1_18_2,
                3055141959546L,
                true
        ).orElseThrow();

        assertTrue(Math.abs(spawn.x()) < 100_000);
        assertTrue(Math.abs(spawn.z()) < 100_000);
    }
}
