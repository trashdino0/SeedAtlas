package dev.seedatlas.core.texture;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextureProviderTest {

    @Test
    void vanilla12MatchesKnownSamples() {
        TextureProvider p = new Vanilla12Textures();
        assertRotation(p, 0, 4, 0, 4, 3);
        assertRotation(p, 0, 4, 1, 4, 2);
        assertRotation(p, 0, 4, 2, 4, 3);
        assertRotation(p, 0, 4, 3, 4, 3);
        assertRotation(p, 0, 4, 4, 4, 3);
    }

    @Test
    void vanilla21_1MatchesKnownSamples() {
        TextureProvider p = new Vanilla21_1Textures();

        assertRotation(p, -1, 0, -1, 4, 0);
        assertRotation(p, 0, 0, -1, 4, 1);
        assertRotation(p, 1, 0, -1, 4, 0);
        assertRotation(p, -2, 0, 0, 4, 2);
        assertRotation(p, -1, 0, 0, 4, 0);
        assertRotation(p, 0, 0, 0, 4, 0);
        assertRotation(p, 1, 0, 0, 4, 1);
        assertRotation(p, -1, 0, 1, 4, 0);
        assertRotation(p, 0, 0, 1, 4, 3);
        assertRotation(p, 1, 0, 1, 4, 0);

        assertRotation(p, -6, 0, 0, 2, 1);
        assertRotation(p, -5, 0, 0, 2, 0);
        assertRotation(p, -4, 0, 0, 2, 1);
        assertRotation(p, -3, 0, 0, 2, 1);
        assertRotation(p, -2, 0, 0, 2, 0);
        assertRotation(p, -1, 0, 0, 2, 0);
        assertRotation(p, 0, 0, 0, 2, 0);
        assertRotation(p, 1, 0, 0, 2, 1);
        assertRotation(p, 2, 0, 0, 2, 0);
        assertRotation(p, 3, 0, 0, 2, 0);
        assertRotation(p, 4, 0, 0, 2, 1);
        assertRotation(p, 5, 0, 0, 2, 1);
        assertRotation(p, 6, 0, 0, 2, 0);
        assertRotation(p, 7, 0, 0, 2, 1);
        assertRotation(p, 8, 0, 0, 2, 0);
    }

    @Test
    void vanilla21_2MatchesKnownSamples() {
        TextureProvider p = new VanillaTextures();

        assertRotation(p, -1, 0, -1, 4, 0);
        assertRotation(p, 0, 0, -1, 4, 3);
        assertRotation(p, 1, 0, -1, 4, 3);
        assertRotation(p, -2, 0, 0, 4, 3);
        assertRotation(p, -1, 0, 0, 4, 0);
        assertRotation(p, 0, 0, 0, 4, 2);
        assertRotation(p, 1, 0, 0, 4, 0);
        assertRotation(p, -1, 0, 1, 4, 3);
        assertRotation(p, 0, 0, 1, 4, 1);
        assertRotation(p, 1, 0, 1, 4, 0);

        assertRotation(p, -538, 67, -575, 2, 1);
        assertRotation(p, -537, 67, -575, 2, 1);
        assertRotation(p, -536, 67, -575, 2, 0);
        assertRotation(p, -535, 67, -575, 2, 0);
        assertRotation(p, -534, 67, -575, 2, 1);
        assertRotation(p, -533, 67, -575, 2, 0);
        assertRotation(p, -532, 67, -575, 2, 0);
        assertRotation(p, -531, 67, -575, 2, 1);
        assertRotation(p, -527, 67, -575, 2, 1);
    }

    @Test
    void sodium19MatchesKnownSamples() {
        TextureProvider p = new Sodium19Textures();
        assertRotation(p, 0, -61, 0, 4, 1);
        assertRotation(p, 1, -61, 0, 4, 1);
        assertRotation(p, 2, -61, 0, 4, 0);
        assertRotation(p, 3, -61, 0, 4, 2);
        assertRotation(p, 4, -61, 0, 4, 0);
        assertRotation(p, 5, -61, 0, 4, 3);
        assertRotation(p, 6, -61, 0, 4, 1);
        assertRotation(p, 7, -61, 0, 4, 2);
        assertRotation(p, 8, -61, 0, 4, 2);
        assertRotation(p, 9, -61, 0, 4, 1);
    }

    @Test
    void sodiumMatchesKnownSamples() {
        TextureProvider p = new SodiumTextures();

        assertRotation(p, 0, -56, 0, 4, 3);
        assertRotation(p, 1, -56, 0, 4, 3);
        assertRotation(p, 2, -56, 0, 4, 3);
        assertRotation(p, 3, -56, 0, 4, 3);
        assertRotation(p, 4, -56, 0, 4, 1);
        assertRotation(p, 5, -56, 0, 4, 3);
        assertRotation(p, 6, -56, 0, 4, 0);
        assertRotation(p, 7, -56, 0, 4, 2);
        assertRotation(p, 8, -56, 0, 4, 0);
        assertRotation(p, 9, -56, 0, 4, 3);
        assertRotation(p, 10, -56, 0, 4, 1);
        assertRotation(p, 11, -56, 0, 4, 1);
        assertRotation(p, 12, -56, 0, 4, 3);
        assertRotation(p, 13, -56, 0, 4, 3);

        assertRotation(p, 0, -56, 1, 2, 1);
        assertRotation(p, 1, -56, 1, 2, 1);
        assertRotation(p, 2, -56, 1, 2, 1);
        assertRotation(p, 3, -56, 1, 2, 0);
        assertRotation(p, 4, -56, 1, 2, 0);
        assertRotation(p, 5, -56, 1, 2, 0);
        assertRotation(p, 6, -56, 1, 2, 1);
        assertRotation(p, 7, -56, 1, 2, 1);
        assertRotation(p, 8, -56, 1, 2, 1);
        assertRotation(p, 9, -56, 1, 2, 0);
        assertRotation(p, 10, -56, 1, 2, 1);
        assertRotation(p, 11, -56, 1, 2, 1);
        assertRotation(p, 12, -56, 1, 2, 1);
        assertRotation(p, 13, -56, 1, 2, 1);
    }

    private static void assertRotation(TextureProvider provider, int x, int y, int z, int mod, int expected) {
        assertEquals(expected, provider.getTexture(x, y, z, mod),
                () -> "rotation at (" + x + ", " + y + ", " + z + ") mod " + mod);
    }
}
