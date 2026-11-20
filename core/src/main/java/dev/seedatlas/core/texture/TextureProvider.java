package dev.seedatlas.core.texture;

/**
 * Replicates the Minecraft/Sodium texture-rotation hash for a block coordinate.
 *
 * <p>The rotation value is deterministic and seed-independent. It is used for
 * localization, not for seed filtering.</p>
 */
public interface TextureProvider {

    long multiplier = 0x5DEECE66DL;
    long mask = (1L << 48) - 1;

    /**
     * Raw coordinate hash used by Minecraft's block-rotation logic.
     */
    static long getCoordRandom(int x, int y, int z) {
        long l = (long) (x * 3129871) ^ (long) z * 116129781L ^ (long) y;
        l = l * l * 42317861L + l * 11L;
        return l;
    }

    static int getCoordinateRandomLegacy(int x, int y, int z) {
        return (int) getCoordRandom(x, y, z) >> 16;
    }

    static long getCoordinateRandom(int x, int y, int z) {
        return getCoordRandom(x, y, z) >> 16;
    }

    /**
     * @param x   block x
     * @param y   block y
     * @param z   block z
     * @param mod number of rotation variants (4 for top/bottom, 2 for sides)
     * @return rotation index in {@code [0, mod)}
     */
    int getTexture(int x, int y, int z, int mod);
}
