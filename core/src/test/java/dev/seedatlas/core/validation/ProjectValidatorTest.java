package dev.seedatlas.core.validation;

import dev.seedatlas.core.model.Anchor;
import dev.seedatlas.core.model.BiomeEvidence;
import dev.seedatlas.core.model.EndPillarEvidence;
import dev.seedatlas.core.model.Evidence;
import dev.seedatlas.core.model.Project;
import dev.seedatlas.core.model.ProjectInfo;
import dev.seedatlas.core.model.SearchArea;
import dev.seedatlas.core.model.StructureEvidence;
import dev.seedatlas.core.model.TextureRotationEvidence;
import dev.seedatlas.core.model.WorldDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectValidatorTest {

    @Test
    void acceptsMinimalValidProject() {
        Project project = project(
                "1.21.4",
                List.of(),
                List.of()
        );

        ValidationResult result = ProjectValidator.validate(project);
        assertTrue(result.valid(), () -> result.errors().toString());
    }

    @Test
    void rejectsUnsupportedVersion() {
        Project project = project("1.4.7", List.of(), List.of());

        ValidationResult result = ProjectValidator.validate(project);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("Unsupported Minecraft version")));
    }

    @Test
    void rejectsDuplicateEvidenceIds() {
        Evidence a = new BiomeEvidence("same", "world", 0, 64, 0, "plains", "a.png", 1.0);
        Evidence b = new BiomeEvidence("same", "world", 1, 64, 1, "forest", "b.png", 1.0);
        Project project = project("1.21.4", List.of(), List.of(a, b));

        ValidationResult result = ProjectValidator.validate(project);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("Duplicate evidence id")));
    }

    @Test
    void rejectsTextureReferencingUnknownAnchor() {
        Evidence texture = new TextureRotationEvidence("rot", "missing", 0, 64, 0, 1, false, "a.png", 1.0);
        Project project = project("1.21.4", List.of(), List.of(texture));

        ValidationResult result = ProjectValidator.validate(project);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("unknown anchor")));
    }

    @Test
    void rejectsUnknownFrame() {
        Evidence biome = new BiomeEvidence("biome", "elsewhere", 0, 64, 0, "plains", "a.png", 1.0);
        Project project = project("1.21.4", List.of(), List.of(biome));

        ValidationResult result = ProjectValidator.validate(project);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("unknown frame")));
    }

    @Test
    void rejectsWrongEndPillarCount() {
        Evidence pillars = new EndPillarEvidence("pillars", List.of(1, 2, 3), "end.png", 1.0);
        Project project = project("1.21.4", List.of(), List.of(pillars));

        ValidationResult result = ProjectValidator.validate(project);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("exactly 10 heights")));
    }

    @Test
    void rejectsInvalidConfidence() {
        Evidence biome = new BiomeEvidence("biome", "world", 0, 64, 0, "plains", "a.png", 1.5);
        Project project = project("1.21.4", List.of(), List.of(biome));

        ValidationResult result = ProjectValidator.validate(project);
        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("confidence must be between 0 and 1")));
    }

    @Test
    void warnsWhenOnlyTextureEvidenceExists() {
        Evidence texture = new TextureRotationEvidence("rot", "base", 0, 64, 0, 1, false, "a.png", 1.0);
        Project project = project(
                "1.21.4",
                List.of(new Anchor("base", "local", null,
                        new SearchArea(-100, 100, -100, 100, 10, 60))),
                List.of(texture)
        );

        ValidationResult result = ProjectValidator.validate(project);
        assertTrue(result.valid(), () -> result.errors().toString());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("do not constrain the seed")));
    }

    private static Project project(String version, List<Anchor> anchors, List<Evidence> evidence) {
        return new Project(
                Project.CURRENT_SCHEMA_VERSION,
                new ProjectInfo("Test", ""),
                new WorldDescriptor("java", version, "overworld", false),
                anchors,
                evidence,
                null
        );
    }
}
