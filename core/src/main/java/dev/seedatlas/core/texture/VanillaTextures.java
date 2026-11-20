package dev.seedatlas.core.texture;

/**
 * Texture rotations for Minecraft 1.21.2+.
 */
public final class VanillaTextures implements TextureProvider {

    @Override
    public int getTexture(int x, int y, int z, int mod) {
        long seed = TextureProvider.getCoordinateRandom(x, y, z);
        seed = seed ^ multiplier;
        // nextInt(mod)
        seed = seed * multiplier + 11L & mask;
        int next = (int) (seed >> 17);
        // assumes there are always 4 variants for a block
        return (int) ((4 * (long) next) >> 31) % mod;
    }
}
