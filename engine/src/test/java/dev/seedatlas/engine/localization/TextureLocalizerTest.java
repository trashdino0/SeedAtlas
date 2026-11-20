package dev.seedatlas.engine.localization;

import dev.seedatlas.core.model.Anchor;
import dev.seedatlas.core.model.Evidence;
import dev.seedatlas.core.model.Position;
import dev.seedatlas.core.model.Project;
import dev.seedatlas.core.model.ProjectInfo;
import dev.seedatlas.core.model.SearchArea;
import dev.seedatlas.core.model.TextureRotationEvidence;
import dev.seedatlas.core.model.WorldDescriptor;
import dev.seedatlas.core.texture.TextureProvider;
import dev.seedatlas.core.texture.VanillaTextures;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextureLocalizerTest {

    private static final int[][] FORMATION = {
            {0, 0, 0},
            {1, 0, 0},
            {0, 0, 1},
            {1, 0, 1},
            {2, 0, 0},
            {0, 1, 0},
            {1, 1, 0},
            {0, 0, 2}
    };

    @Test
    void findsKnownOriginFromTopRotations() {
        Position origin = new Position(3, 64, -2);
        SearchArea area = new SearchArea(-10, 10, -10, 10, 60, 70);
        Project project = projectWithFormation(origin, area, false, 2);

        LocalizationResult result = TextureLocalizer.localize(project, project.anchors().get(0), 2);

        assertEquals(1, result.matches().size(), () -> "matches: " + result.matches());
        assertEquals(origin, result.matches().get(0).position());
    }

    @Test
    void findsKnownOriginWithSideRotations() {
        Position origin = new Position(-4, 62, 5);
        SearchArea area = new SearchArea(-9, 9, -9, 9, 58, 68);
        Project project = projectWithFormation(origin, area, true, 4);

        LocalizationResult result = TextureLocalizer.localize(project, project.anchors().get(0), 2);

        assertEquals(1, result.matches().size(), () -> "matches: " + result.matches());
        assertEquals(origin, result.matches().get(0).position());
    }

    @Test
    void verifiesAlreadyResolvedAnchor() {
        Position origin = new Position(7, 70, 1);
        SearchArea area = new SearchArea(-2, 2, -2, 2, 68, 72);
        Project project = projectWithFormation(origin, area, false, 2);
        Anchor resolved = new Anchor("base", "world", origin, area);
        Project resolvedProject = new Project(
                project.schemaVersion(),
                project.project(),
                project.world(),
                List.of(resolved),
                project.evidence(),
                project.constraints()
        );

        LocalizationResult result = TextureLocalizer.localize(
                resolvedProject,
                resolvedProject.anchors().get(0),
                2
        );

        assertEquals(1, result.matches().size());
        assertEquals(origin, result.matches().get(0).position());
        assertEquals(1L, result.checkedPositions());
    }

    private static Project projectWithFormation(
            Position origin,
            SearchArea area,
            boolean includeSide,
            int sideCount
    ) {
        TextureProvider provider = new VanillaTextures();
        List<Evidence> evidence = new ArrayList<>();
        int index = 0;

        for (int[] offset : FORMATION) {
            int x = origin.x() + offset[0];
            int y = origin.y() + offset[1];
            int z = origin.z() + offset[2];
            int rotation = provider.getTexture(x, y, z, 4);
            evidence.add(new TextureRotationEvidence(
                    "rot-" + index++,
                    "base",
                    offset[0],
                    offset[1],
                    offset[2],
                    rotation,
                    false,
                    "synthetic",
                    1.0
            ));
        }

        if (includeSide) {
            int[][] sideOffsets = {
                    {0, 0, 0},
                    {1, 0, 0},
                    {0, 0, 1},
                    {1, 0, 1},
                    {0, 1, 0},
                    {1, 1, 0}
            };
            for (int i = 0; i < Math.min(sideCount, sideOffsets.length); i++) {
                int[] offset = sideOffsets[i];
                int x = origin.x() + offset[0];
                int y = origin.y() + offset[1];
                int z = origin.z() + offset[2];
                int rotation = provider.getTexture(x, y, z, 2);
                evidence.add(new TextureRotationEvidence(
                        "side-" + i,
                        "base",
                        offset[0],
                        offset[1],
                        offset[2],
                        rotation,
                        true,
                        "synthetic",
                        1.0
                ));
            }
        }

        return new Project(
                Project.CURRENT_SCHEMA_VERSION,
                new ProjectInfo("Localizer test", ""),
                new WorldDescriptor("java", "1.21.4", "overworld", false),
                List.of(new Anchor("base", "local", null, area)),
                evidence,
                null
        );
    }
}
