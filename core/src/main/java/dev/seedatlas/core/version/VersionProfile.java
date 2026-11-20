package dev.seedatlas.core.version;

/**
 * A supported Minecraft Java version and its worldgen/texture defaults.
 */
public record VersionProfile(
        String id,
        String displayName,
        TextureMode textureMode
) {
}
