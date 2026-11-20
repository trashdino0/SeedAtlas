package dev.seedatlas.core.texture;

/**
 * Texture rotations for Minecraft 1.13 - 1.21.1.
 */
public class Vanilla21_1Textures implements TextureProvider {

    @Override
    public int getTexture(int x, int y, int z, int mod) {
        int rand = random(TextureProvider.getCoordinateRandom(x, y, z));
        return Math.abs(rand) % mod;
    }

    protected int random(long seed) {
        seed = (seed ^ multiplier) & mask;
        // nextLong() combine two 32-bit halves
        return (int) ((seed * 0xBB20B4600A69L + 0x40942DE6BAL) >>> 16);
    }
}
