package dev.seedatlas.core.analysis;

import java.util.List;

/**
 * Advisory estimate of how much information a bundle contains and how large the
 * remaining search space is likely to be.
 */
public record DifficultyReport(
        double structureBits,
        double biomeBits,
        double strongholdBits,
        double otherBits,
        double totalBits,
        String tier,
        double estimatedLower48Candidates,
        double estimatedUpper16Checks,
        int structureCount,
        int biomeCount,
        int strongholdCount,
        List<String> notes
) {
    public DifficultyReport {
        notes = notes == null ? List.of() : List.copyOf(notes);
    }

    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Estimated information: ").append(String.format("%.1f", totalBits)).append(" bits")
                .append(" (").append(tier).append(")").append(System.lineSeparator());
        sb.append("  structure bits : ").append(String.format("%.1f", structureBits))
                .append(" from ").append(structureCount).append(" sighting(s)").append(System.lineSeparator());
        sb.append("  biome bits     : ").append(String.format("%.1f", biomeBits))
                .append(" from ").append(biomeCount).append(" sample(s)").append(System.lineSeparator());
        sb.append("  stronghold bits: ").append(String.format("%.1f", strongholdBits))
                .append(" from ").append(strongholdCount).append(" estimate(s)").append(System.lineSeparator());
        sb.append("  other bits     : ").append(String.format("%.1f", otherBits)).append(System.lineSeparator());
        sb.append("  lower-48 candidates (approx.): ").append(formatCount(estimatedLower48Candidates))
                .append(System.lineSeparator());
        sb.append("  upper-16 checks (approx.)   : ").append(formatCount(estimatedUpper16Checks))
                .append(System.lineSeparator());
        if (!notes.isEmpty()) {
            sb.append("Notes:").append(System.lineSeparator());
            for (String note : notes) {
                sb.append("  - ").append(note).append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    public static String formatCount(double value) {
        if (!Double.isFinite(value)) {
            return "unbounded";
        }
        if (value >= 1.0e15) {
            return String.format("%.3e", value);
        }
        return String.format("%,.0f", value);
    }
}
