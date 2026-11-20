package dev.seedatlas.core.model;

public record SpawnEvidence(
        String id,
        int x,
        int z,
        int tolerance,
        String source,
        double confidence
) implements Evidence {
    @Override
    public String type() {
        return "spawn";
    }
}
