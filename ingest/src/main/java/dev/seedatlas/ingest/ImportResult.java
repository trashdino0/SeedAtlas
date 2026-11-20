package dev.seedatlas.ingest;

import dev.seedatlas.core.model.Project;

import java.util.List;

/**
 * @param project            imported bundle
 * @param importedStructures number of structure lines accepted
 * @param unknownStructures  structure names that could not be mapped
 */
public record ImportResult(
        Project project,
        int importedStructures,
        List<String> unknownStructures
) {
    public ImportResult {
        unknownStructures = unknownStructures == null ? List.of() : List.copyOf(unknownStructures);
    }
}
