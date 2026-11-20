package dev.seedatlas.ingest;

import dev.seedatlas.core.model.StructureEvidence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedCrackerImporterTest {

    @TempDir
    Path tempDir;

    @Test
    void importsChunkCoordinatesAsBlocks() throws Exception {
        Path file = tempDir.resolve("world.txt");
        Files.writeString(file, """
                village;10;-4
                jungle_pyramid;0;0
                not_a_structure;1;1
                malformed line
                """);

        ImportResult result = SeedCrackerImporter.read(file, "1.21.4");

        assertEquals(2, result.importedStructures());
        assertEquals(2, result.unknownStructures().size());

        List<StructureEvidence> structures = result.project().evidence().stream()
                .filter(StructureEvidence.class::isInstance)
                .map(StructureEvidence.class::cast)
                .toList();

        assertEquals("village", structures.get(0).structure());
        assertEquals(160, structures.get(0).x());
        assertEquals(-64, structures.get(0).z());
        assertEquals("jungle_temple", structures.get(1).structure());
        assertEquals("1.21.4", result.project().world().version());
        assertTrue(result.project().evidence().stream()
                .allMatch(e -> e.source().startsWith("seedcracker:")));
    }
}
