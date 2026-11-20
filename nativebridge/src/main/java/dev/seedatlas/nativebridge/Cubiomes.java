package dev.seedatlas.nativebridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Typed facade over the Cubiomes JNI bridge.
 */
public final class Cubiomes {

    private Cubiomes() {
    }

    public static boolean isAvailable() {
        return CubiomesNative.isAvailable();
    }

    public static int biomeAt(
            CubiomesVersion version,
            long seed,
            int x,
            int y,
            int z,
            int scale
    ) {
        CubiomesNative.requireAvailable();
        return CubiomesNative.getBiomeAt(
                seed, x, y, z, scale, version.nativeId(), 0
        );
    }

    public static Optional<StructurePos> structurePos(
            StructureType type,
            CubiomesVersion version,
            long seed48,
            int regionX,
            int regionZ
    ) {
        CubiomesNative.requireAvailable();
        int[] out = new int[2];
        if (!CubiomesNative.getStructurePos(
                type.nativeId(), version.nativeId(), seed48, regionX, regionZ, out)) {
            return Optional.empty();
        }
        return Optional.of(new StructurePos(out[0], out[1]));
    }

    public static Optional<StructureConfigInfo> structureConfig(
            StructureType type,
            CubiomesVersion version
    ) {
        CubiomesNative.requireAvailable();
        long[] out = new long[4];
        if (!CubiomesNative.getStructureConfig(type.nativeId(), version.nativeId(), out)) {
            return Optional.empty();
        }
        return Optional.of(new StructureConfigInfo(out[0], (int) out[1], (int) out[2], (int) out[3]));
    }

    public static boolean isViableStructurePos(
            StructureType type,
            CubiomesVersion version,
            long seed,
            int blockX,
            int blockZ
    ) {
        CubiomesNative.requireAvailable();
        return CubiomesNative.isViableStructurePos(
                type.nativeId(), version.nativeId(), seed, blockX, blockZ, 0
        );
    }

    public static List<StructurePos> strongholds(
            CubiomesVersion version,
            long seed,
            int count
    ) {
        CubiomesNative.requireAvailable();
        if (count <= 0) {
            return List.of();
        }
        int[] out = new int[count * 2];
        int found = CubiomesNative.strongholds(version.nativeId(), seed, count, out);
        List<StructurePos> result = new ArrayList<>(found);
        for (int i = 0; i < found; i++) {
            result.add(new StructurePos(out[i * 2], out[i * 2 + 1]));
        }
        return List.copyOf(result);
    }

    /**
     * Finds every viable structure of {@code type} in an inclusive region box.
     */
    public static List<StructurePos> findStructures(
            StructureType type,
            CubiomesVersion version,
            long seed,
            int regMinX,
            int regMinZ,
            int regMaxX,
            int regMaxZ
    ) {
        CubiomesNative.requireAvailable();
        if (regMaxX < regMinX || regMaxZ < regMinZ) {
            return List.of();
        }

        long unsignedRegions = (long) (regMaxX - regMinX + 1) * (regMaxZ - regMinZ + 1);
        if (unsignedRegions > Integer.MAX_VALUE / 2L) {
            throw new IllegalArgumentException("Region box is too large: " + unsignedRegions);
        }

        int[] out = new int[(int) unsignedRegions * 2];
        int found = CubiomesNative.findStructures(
                type.nativeId(), version.nativeId(), seed,
                regMinX, regMinZ, regMaxX, regMaxZ, out
        );

        List<StructurePos> result = new ArrayList<>(found);
        for (int i = 0; i < found; i++) {
            result.add(new StructurePos(out[i * 2], out[i * 2 + 1]));
        }
        return List.copyOf(result);
    }

    /**
     * Batch biome lookup. {@code points} is a flat list of xyz triplets.
     */
    public static int[] biomesAt(
            CubiomesVersion version,
            long seed,
            int scale,
            int[] points
    ) {
        CubiomesNative.requireAvailable();
        if (points.length % 3 != 0) {
            throw new IllegalArgumentException("points length must be a multiple of 3");
        }
        int count = points.length / 3;
        int[] out = new int[count];
        int written = CubiomesNative.biomesAt(seed, version.nativeId(), scale, points, out);
        if (written == count) {
            return out;
        }
        int[] trimmed = new int[written];
        System.arraycopy(out, 0, trimmed, 0, written);
        return trimmed;
    }

    public static Optional<StructurePos> spawn(
            CubiomesVersion version,
            long seed,
            boolean estimate
    ) {
        CubiomesNative.requireAvailable();
        int[] out = new int[2];
        if (!CubiomesNative.spawn(version.nativeId(), seed, estimate, out)) {
            return Optional.empty();
        }
        return Optional.of(new StructurePos(out[0], out[1]));
    }

    public static String biomeName(CubiomesVersion version, int biomeId) {
        CubiomesNative.requireAvailable();
        String name = CubiomesNative.biomeName(version.nativeId(), biomeId);
        return name == null ? "" : name;
    }

    public static int newestVersion() {
        CubiomesNative.requireAvailable();
        return CubiomesNative.newestVersion();
    }
}
