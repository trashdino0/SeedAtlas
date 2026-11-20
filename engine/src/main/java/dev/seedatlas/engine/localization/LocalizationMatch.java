package dev.seedatlas.engine.localization;

import dev.seedatlas.core.model.Position;

/**
 * A candidate world position for a local anchor.
 *
 * @param position    resolved anchor origin
 * @param matched     number of rotation evidence entries that matched
 * @param total       number of rotation evidence entries checked
 */
public record LocalizationMatch(Position position, int matched, int total) {
}
