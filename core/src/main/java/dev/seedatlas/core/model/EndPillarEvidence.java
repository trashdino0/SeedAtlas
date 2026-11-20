package dev.seedatlas.core.model;

import java.util.List;

/**
 * The ten End pillar heights. These yield a 16-bit pillar seed.
 */
public record EndPillarEvidence(
        String id,
        List<Integer> heights,
        String source,
        double confidence
) implements Evidence {
    public EndPillarEvidence {
        heights = heights == null ? List.of() : List.copyOf(heights);
    }

    @Override
    public String type() {
        return "end_pillar";
    }
}
