package dev.seedatlas.core.io;

import dev.seedatlas.core.model.Anchor;
import dev.seedatlas.core.model.BiomeEvidence;
import dev.seedatlas.core.model.Constraints;
import dev.seedatlas.core.model.EndPillarEvidence;
import dev.seedatlas.core.model.Evidence;
import dev.seedatlas.core.model.Position;
import dev.seedatlas.core.model.Project;
import dev.seedatlas.core.model.ProjectInfo;
import dev.seedatlas.core.model.SearchArea;
import dev.seedatlas.core.model.SeedHintEvidence;
import dev.seedatlas.core.model.SeedRange;
import dev.seedatlas.core.model.SpawnEvidence;
import dev.seedatlas.core.model.StrongholdEvidence;
import dev.seedatlas.core.model.StructureEvidence;
import dev.seedatlas.core.model.TextureRotationEvidence;
import dev.seedatlas.core.model.WorldDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectRoundTripTest {

    @Test
    void roundTripsAllEvidenceTypes() throws Exception {
        List<Evidence> evidence = List.of(
                new TextureRotationEvidence("rot-1", "base", 0, 64, 0, 1, false, "frame_0012.png", 1.0),
                new BiomeEvidence("biome-1", "world", -1621, 200, 913, "flower_forest", "aerial_03.png", 0.95),
                new StructureEvidence("struct-1", "world", "village_plains", 1234, -567, 16, "map.png", 0.9),
                new StrongholdEvidence("sh-1", 1, -2663, -655, 112, "eye_throw.png", 0.8),
                new SpawnEvidence("spawn-1", 0, 0, 32, "compass.png", 0.7),
                new EndPillarEvidence("pillars-1", List.of(10, 20, 30, 40, 50, 60, 70, 80, 90, 100), "end.png", 0.9),
                new SeedHintEvidence("hint-1", null, null, 1234L, "server.log", 0.5)
        );

        Project project = new Project(
                Project.CURRENT_SCHEMA_VERSION,
                new ProjectInfo("Round trip", "all evidence types"),
                new WorldDescriptor("java", "1.21.4", "overworld", false),
                List.of(
                        new Anchor("base", "local", null,
                                new SearchArea(-1000, 1000, -1000, 1000, 10, 60)),
                        new Anchor("spawn", "world", new Position(0, 64, 0), null)
                ),
                evidence,
                new Constraints(new SeedRange(-100L, 100L), 1234L, List.of(42L, 43L))
        );

        String json = ProjectIO.toJson(project);
        assertTrue(json.contains("\"type\" : \"texture_rotation\""), json);
        assertTrue(json.contains("\"type\" : \"end_pillar\""), json);

        Project restored = ProjectIO.fromJson(json);
        assertEquals(project, restored);
    }
}
