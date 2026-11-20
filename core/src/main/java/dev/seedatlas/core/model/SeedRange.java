package dev.seedatlas.core.model;

/**
 * Inclusive signed 64-bit seed range. Minecraft world seeds are Java {@code long}s.
 */
public record SeedRange(long min, long max) {
}
