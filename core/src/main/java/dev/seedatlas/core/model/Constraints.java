package dev.seedatlas.core.model;

import java.util.List;

/**
 * Optional restrictions on the seed search space.
 *
 * @param seedRange       inclusive range to search; {@code null} means full 64-bit space
 * @param knownSeedPrefix optional known upper bits of the seed, if any
 * @param excludeSeeds    explicit seeds to skip (for example, already-checked negatives)
 */
public record Constraints(
        SeedRange seedRange,
        Long knownSeedPrefix,
        List<Long> excludeSeeds
) {
    public Constraints {
        excludeSeeds = excludeSeeds == null ? List.of() : List.copyOf(excludeSeeds);
    }

    public static Constraints empty() {
        return new Constraints(null, null, List.of());
    }
}
