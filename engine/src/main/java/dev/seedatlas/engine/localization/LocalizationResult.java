package dev.seedatlas.engine.localization;

import java.util.List;

public record LocalizationResult(
        String anchorId,
        List<LocalizationMatch> matches,
        long checkedPositions,
        long elapsedMillis
) {
    public LocalizationResult {
        matches = matches == null ? List.of() : List.copyOf(matches);
    }
}
