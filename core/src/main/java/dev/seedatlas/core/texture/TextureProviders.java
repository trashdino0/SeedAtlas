package dev.seedatlas.core.texture;

import dev.seedatlas.core.version.TextureMode;

public final class TextureProviders {

    private TextureProviders() {
    }

    public static TextureProvider forMode(TextureMode mode) {
        return switch (mode) {
            case VANILLA_12 -> new Vanilla12Textures();
            case VANILLA_21_1 -> new Vanilla21_1Textures();
            case VANILLA_21_2 -> new VanillaTextures();
            case SODIUM_19 -> new Sodium19Textures();
            case SODIUM -> new SodiumTextures();
        };
    }
}
