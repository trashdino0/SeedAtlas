package dev.seedatlas.core.model;

/**
 * A biome observed at a world coordinate. {@code frame} is {@code world} or an anchor id.
 */
public record BiomeEvidence(
        String id,
        String frame,
        int x,
        int y,
        int z,
        String biome,
        String source,
        double confidence
) implements Evidence {
    @Override
    public String type() {
        return "biome";
    }
}
