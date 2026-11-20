package dev.seedatlas.core.texture;

/**
 * Texture rotations for Minecraft 1.12.2 and earlier.
 */
public final class Vanilla12Textures implements TextureProvider {

    @Override
    public int getTexture(int x, int y, int z, int mod) {
        int rand = TextureProvider.getCoordinateRandomLegacy(x, y, z);
        return Math.abs(rand) % mod;
    }
}
