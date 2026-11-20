package dev.seedatlas.core.model;

import java.util.List;

/**
 * Root of a SeedAtlas evidence bundle.
 *
 * @param schemaVersion bundle schema version; currently {@value #CURRENT_SCHEMA_VERSION}
 * @param project       human-facing project metadata
 * @param world         Minecraft edition/version/dimension
 * @param anchors       local coordinate frames that may need localization
 * @param evidence      all observations, both localization and seed evidence
 * @param constraints   optional seed-space restrictions
 */
public record Project(
        int schemaVersion,
        ProjectInfo project,
        WorldDescriptor world,
        List<Anchor> anchors,
        List<Evidence> evidence,
        Constraints constraints
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public Project {
        anchors = anchors == null ? List.of() : List.copyOf(anchors);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        constraints = constraints == null ? Constraints.empty() : constraints;
    }

    public static Project empty(String name, WorldDescriptor world) {
        return new Project(
                CURRENT_SCHEMA_VERSION,
                new ProjectInfo(name, ""),
                world,
                List.of(),
                List.of(),
                Constraints.empty()
        );
    }
}
