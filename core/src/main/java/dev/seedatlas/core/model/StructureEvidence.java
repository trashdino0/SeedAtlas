package dev.seedatlas.core.model;

/**
 * A structure sighting. Coordinates are block coordinates; {@code tolerance} is
 * the uncertainty in blocks around the reported position.
 */
public record StructureEvidence(
        String id,
        String frame,
        String structure,
        int x,
        int z,
        int tolerance,
        String source,
        double confidence
) implements Evidence {
    @Override
    public String type() {
        return "structure";
    }
}
