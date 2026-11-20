package dev.seedatlas.core.model;

public record WorldDescriptor(
        String edition,
        String version,
        String dimension,
        boolean largeBiomes
) {
    public WorldDescriptor {
        edition = edition == null || edition.isBlank() ? "java" : edition;
        dimension = dimension == null || dimension.isBlank() ? "overworld" : dimension;
    }
}
