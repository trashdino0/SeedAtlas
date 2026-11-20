package dev.seedatlas.core.model;

/**
 * A direct seed hint. At least one field should be present.
 *
 * @param knownSeed    full known seed (strongest hint)
 * @param hashedSeed   server-provided hashed seed
 * @param seedPrefix   known upper bits of the seed
 */
public record SeedHintEvidence(
        String id,
        Long knownSeed,
        Long hashedSeed,
        Long seedPrefix,
        String source,
        double confidence
) implements Evidence {
    @Override
    public String type() {
        return "seed_hint";
    }
}
