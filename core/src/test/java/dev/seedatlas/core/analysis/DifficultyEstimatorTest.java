package dev.seedatlas.core.analysis;

import dev.seedatlas.core.model.BiomeEvidence;
import dev.seedatlas.core.model.EndPillarEvidence;
import dev.seedatlas.core.model.Evidence;
import dev.seedatlas.core.model.Project;
import dev.seedatlas.core.model.ProjectInfo;
import dev.seedatlas.core.model.StructureEvidence;
import dev.seedatlas.core.model.TextureRotationEvidence;
import dev.seedatlas.core.model.WorldDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DifficultyEstimatorTest {

    @Test
    void textureRotationsContributeNoSeedBits() {
        Project project = project(List.of(
                new TextureRotationEvidence("rot", "base", 0, 64, 0, 1, false, "a.png", 1.0)
        ));

        DifficultyReport report = DifficultyEstimator.estimate(project);
        assertEquals(0.0, report.totalBits(), 0.0001);
        assertEquals("INSUFFICIENT", report.tier());
        assertTrue(report.notes().stream().anyMatch(n -> n.contains("0 seed bits")));
    }

    @Test
    void fourStructuresGiveViableTier() {
        Project project = project(List.of(
                structure("s1", "village_plains", 0, 0),
                structure("s2", "desert_pyramid", 1000, 0),
                structure("s3", "jungle_temple", 0, 1000),
                structure("s4", "shipwreck", 1000, 1000)
        ));

        DifficultyReport report = DifficultyEstimator.estimate(project);
        assertEquals(36.0, report.totalBits(), 0.0001);
        assertEquals("VIABLE", report.tier());
        assertEquals(4096.0, report.estimatedLower48Candidates(), 0.5);
    }

    @Test
    void evidenceCannotExceedFullSeed() {
        List<Evidence> evidence = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) {
            evidence.add(structure("s" + i, "village_plains", i * 1000, 0));
        }

        DifficultyReport report = DifficultyEstimator.estimate(project(evidence));
        assertEquals(64.0, report.totalBits(), 0.0001);
        assertEquals(1.0, report.estimatedLower48Candidates(), 0.0001);
        assertTrue(report.notes().stream().anyMatch(n -> n.contains("saturated")));
    }

    @Test
    void endPillarsAddSixteenBits() {
        Project project = project(List.of(
                new EndPillarEvidence("pillars", List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), "end.png", 1.0)
        ));

        DifficultyReport report = DifficultyEstimator.estimate(project);
        assertEquals(16.0, report.totalBits(), 0.0001);
    }

    @Test
    void rareBiomesAddInformation() {
        Project project = project(List.of(
                new BiomeEvidence("b1", "world", 0, 64, 0, "flower_forest", "a.png", 1.0),
                new BiomeEvidence("b2", "world", 100, 64, 100, "meadow", "b.png", 1.0)
        ));

        DifficultyReport report = DifficultyEstimator.estimate(project);
        assertEquals(8.8, report.totalBits(), 0.0001);
    }

    private static StructureEvidence structure(String id, String type, int x, int z) {
        return new StructureEvidence(id, "world", type, x, z, 0, "map.png", 1.0);
    }

    private static Project project(List<Evidence> evidence) {
        return new Project(
                Project.CURRENT_SCHEMA_VERSION,
                new ProjectInfo("Test", ""),
                new WorldDescriptor("java", "1.21.4", "overworld", false),
                List.of(),
                evidence,
                null
        );
    }
}
