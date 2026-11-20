package dev.seedatlas.core.model;

/**
 * An estimated stronghold position. Ring 1 is the initial ring.
 */
public record StrongholdEvidence(
        String id,
        int ring,
        int x,
        int z,
        int tolerance,
        String source,
        double confidence
) implements Evidence {
    @Override
    public String type() {
        return "stronghold";
    }
}
