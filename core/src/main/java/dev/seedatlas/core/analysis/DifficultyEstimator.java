package dev.seedatlas.core.analysis;

import dev.seedatlas.core.model.BiomeEvidence;
import dev.seedatlas.core.model.EndPillarEvidence;
import dev.seedatlas.core.model.Evidence;
import dev.seedatlas.core.model.Project;
import dev.seedatlas.core.model.SeedHintEvidence;
import dev.seedatlas.core.model.SpawnEvidence;
import dev.seedatlas.core.model.StrongholdEvidence;
import dev.seedatlas.core.model.StructureEvidence;
import dev.seedatlas.core.model.TextureRotationEvidence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fast, approximate estimate of evidence information content.
 *
 * <p>These numbers are intentionally conservative and will be calibrated against
 * Cubiomes in M2. They exist to stop a user from launching a hopeless search.</p>
 */
public final class DifficultyEstimator {

    private static final Map<String, Double> BIOME_BITS = Map.ofEntries(
            Map.entry("ocean", 0.5),
            Map.entry("deep_ocean", 1.0),
            Map.entry("frozen_ocean", 1.0),
            Map.entry("warm_ocean", 1.0),
            Map.entry("lukewarm_ocean", 1.0),
            Map.entry("cold_ocean", 1.0),
            Map.entry("deep_cold_ocean", 1.5),
            Map.entry("deep_frozen_ocean", 1.5),
            Map.entry("deep_lukewarm_ocean", 1.5),
            Map.entry("river", 2.0),
            Map.entry("frozen_river", 3.0),
            Map.entry("beach", 2.0),
            Map.entry("stony_shore", 2.5),
            Map.entry("plains", 1.0),
            Map.entry("sunflower_plains", 4.4),
            Map.entry("forest", 1.2),
            Map.entry("flower_forest", 4.4),
            Map.entry("birch_forest", 2.0),
            Map.entry("dark_forest", 2.5),
            Map.entry("taiga", 1.5),
            Map.entry("snowy_taiga", 2.0),
            Map.entry("old_growth_pine_taiga", 3.0),
            Map.entry("old_growth_spruce_taiga", 3.0),
            Map.entry("giant_tree_taiga", 3.0),
            Map.entry("savanna", 1.5),
            Map.entry("savanna_plateau", 2.0),
            Map.entry("desert", 1.5),
            Map.entry("badlands", 3.0),
            Map.entry("wooded_badlands", 3.5),
            Map.entry("jungle", 2.0),
            Map.entry("sparse_jungle", 3.0),
            Map.entry("bamboo_jungle", 2.5),
            Map.entry("swamp", 3.0),
            Map.entry("mangrove_swamp", 3.0),
            Map.entry("meadow", 4.4),
            Map.entry("mushroom_fields", 6.0),
            Map.entry("snowy_plains", 1.5),
            Map.entry("snowy_tundra", 1.5),
            Map.entry("ice_spikes", 5.0),
            Map.entry("cherry_grove", 4.0),
            Map.entry("grove", 3.5),
            Map.entry("snowy_slopes", 3.0),
            Map.entry("jagged_peaks", 4.0),
            Map.entry("frozen_peaks", 4.0),
            Map.entry("stony_peaks", 3.5),
            Map.entry("windswept_hills", 3.0),
            Map.entry("windswept_forest", 3.5),
            Map.entry("windswept_gravelly_hills", 3.5)
    );

    private DifficultyEstimator() {
    }

    public static DifficultyReport estimate(Project project) {
        double structureBits = 0.0;
        double biomeBits = 0.0;
        double strongholdBits = 0.0;
        double otherBits = 0.0;

        int structureCount = 0;
        int biomeCount = 0;
        int strongholdCount = 0;
        boolean hasEndPillar = false;

        for (Evidence evidence : project.evidence()) {
            switch (evidence) {
                case StructureEvidence s -> {
                    structureBits += structureBits(s);
                    structureCount++;
                }
                case BiomeEvidence b -> {
                    biomeBits += biomeBits(b);
                    biomeCount++;
                }
                case StrongholdEvidence s -> {
                    strongholdBits += strongholdBits(s);
                    strongholdCount++;
                }
                case SpawnEvidence ignored -> otherBits += 4.0;
                case EndPillarEvidence ignored -> {
                    otherBits += 16.0;
                    hasEndPillar = true;
                }
                case SeedHintEvidence h -> otherBits += seedHintBits(h);
                case TextureRotationEvidence ignored -> {
                    // Localization only: no seed bits.
                }
            }
        }

        // Evidence items are correlated through the seed, so the naive sum
        // overcounts. A full seed never contains more than 64 bits.
        double rawTotal = structureBits + biomeBits + strongholdBits + otherBits;
        double total = Math.min(64.0, rawTotal);
        String tier = tier(total);
        double lower48 = total >= 48.0 ? 1.0 : Math.pow(2.0, 48.0 - total);
        double upperChecks = Math.min(Math.pow(2.0, 64.0), lower48 * 65_536.0);

        List<String> notes = new ArrayList<>();
        if (rawTotal > 64.0) {
            notes.add("Evidence sums to more than a full 64-bit seed; the total is saturated.");
        }
        notes.add("Texture rotations localize the build but contribute 0 seed bits.");
        notes.add("Bit estimates are approximate; Cubiomes-backed calibration arrives in M2.");
        if (total < 24.0) {
            notes.add("Fewer than ~24 bits is usually insufficient; add structure or biome evidence.");
        }
        if (structureCount == 0 && !hasEndPillar) {
            notes.add("No structure or End pillar evidence: practical search routes are limited.");
        }
        if (biomeCount > 0) {
            notes.add("Biome checks recover the upper 16 bits after structure-seed recovery.");
        }
        if (project.evidence().stream().anyMatch(TextureRotationEvidence.class::isInstance) && total < 1.0) {
            notes.add("This bundle is localization-only right now; no seed candidate can be returned yet.");
        }

        return new DifficultyReport(
                structureBits,
                biomeBits,
                strongholdBits,
                otherBits,
                total,
                tier,
                lower48,
                upperChecks,
                structureCount,
                biomeCount,
                strongholdCount,
                notes
        );
    }

    private static double structureBits(StructureEvidence s) {
        // Roughly 8-9 bits per structure. Tolerance erodes the information.
        double reduction = Math.log(1.0 + Math.max(0, s.tolerance()) / 16.0) / Math.log(2.0);
        double bits = Math.max(0.0, Math.min(9.0, 9.0 - reduction));
        return bits * clampConfidence(s.confidence());
    }

    private static double biomeBits(BiomeEvidence b) {
        double base = BIOME_BITS.getOrDefault(normalize(b.biome()), 2.5);
        return base * clampConfidence(b.confidence());
    }

    private static double strongholdBits(StrongholdEvidence s) {
        double base = switch (s.ring()) {
            case 1 -> 7.0;
            case 2 -> 6.0;
            default -> 5.0;
        };
        return base * clampConfidence(s.confidence());
    }

    private static double seedHintBits(SeedHintEvidence h) {
        if (h.knownSeed() != null) {
            return 64.0;
        }
        if (h.hashedSeed() != null) {
            return 32.0;
        }
        if (h.seedPrefix() != null) {
            return 16.0;
        }
        return 0.0;
    }

    private static double clampConfidence(double confidence) {
        if (!Double.isFinite(confidence)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, confidence));
    }

    private static String normalize(String biome) {
        if (biome == null) {
            return "";
        }
        return biome.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private static String tier(double bits) {
        if (bits < 24.0) {
            return "INSUFFICIENT";
        }
        if (bits < 32.0) {
            return "MARGINAL";
        }
        if (bits < 40.0) {
            return "VIABLE";
        }
        return "STRONG";
    }
}
