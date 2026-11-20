package dev.seedatlas.nativebridge;

/**
 * Low-level JNI surface for Cubiomes. Prefer {@link Cubiomes} for typed calls.
 */
public final class CubiomesNative {

    static {
        NativeLoader.load();
    }

    private CubiomesNative() {
    }

    public static native int getBiomeAt(
            long seed,
            int x,
            int y,
            int z,
            int scale,
            int mc,
            int flags
    );

    public static native boolean getStructurePos(
            int type,
            int mc,
            long seed48,
            int regX,
            int regZ,
            int[] out
    );

    public static native boolean getStructureConfig(
            int type,
            int mc,
            long[] out
    );

    public static native boolean isViableStructurePos(
            int type,
            int mc,
            long seed,
            int x,
            int z,
            int flags
    );

    /**
     * Fills {@code out} with up to {@code count} stronghold positions as x/z pairs.
     *
     * @return number of strongholds written
     */
    public static native int strongholds(
            int mc,
            long seed,
            int count,
            int[] out
    );

    /**
     * Writes the viable structure positions for a region box as x/z pairs.
     *
     * @return number of positions written
     */
    public static native int findStructures(
            int type,
            int mc,
            long seed,
            int regMinX,
            int regMinZ,
            int regMaxX,
            int regMaxZ,
            int[] out
    );

    /**
     * Looks up many biomes with one generator, reading xyz triplets from
     * {@code coords} and writing one id per point to {@code out}.
     *
     * @return number of biomes written
     */
    public static native int biomesAt(
            long seed,
            int mc,
            int scale,
            int[] coords,
            int[] out
    );

    public static native boolean spawn(
            int mc,
            long seed,
            boolean estimate,
            int[] out
    );

    public static native String biomeName(int mc, int id);

    public static native int newestVersion();

    public static boolean isAvailable() {
        return NativeLoader.isLoaded();
    }

    public static void requireAvailable() {
        if (!isAvailable()) {
            throw new IllegalStateException(
                    "Cubiomes native library is unavailable: " + NativeLoader.error()
            );
        }
    }
}
