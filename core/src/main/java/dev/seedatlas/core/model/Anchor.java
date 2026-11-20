package dev.seedatlas.core.model;

/**
 * A coordinate frame referenced by local evidence.
 *
 * <p>An anchor may already have a resolved world {@code origin}, or it may need
 * localization. In the latter case {@code searchArea} bounds the localization search.</p>
 */
public record Anchor(
        String id,
        String frame,
        Position origin,
        SearchArea searchArea
) {
    public Anchor {
        frame = frame == null || frame.isBlank() ? "local" : frame;
    }
}
