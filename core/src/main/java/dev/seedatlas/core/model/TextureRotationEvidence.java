package dev.seedatlas.core.model;

/**
 * A block whose texture rotation was observed. Coordinates are relative to the
 * named anchor. Texture rotations are seed-independent: this is localization
 * evidence, not seed evidence.
 */
public record TextureRotationEvidence(
        String id,
        String anchor,
        int x,
        int y,
        int z,
        int rotation,
        boolean side,
        String source,
        double confidence
) implements Evidence {
    @Override
    public String type() {
        return "texture_rotation";
    }
}
