package dev.seedatlas.ingest;

import dev.seedatlas.core.model.Constraints;
import dev.seedatlas.core.model.Evidence;
import dev.seedatlas.core.model.Project;
import dev.seedatlas.core.model.ProjectInfo;
import dev.seedatlas.core.model.StructureEvidence;
import dev.seedatlas.core.model.WorldDescriptor;
import dev.seedatlas.nativebridge.StructureType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Imports {@code SeedCrackerX saved structures} files.
 *
 * <p>The format is one entry per line: {@code structure_name;chunkX;chunkZ}.
 * Chunk coordinates are converted to block coordinates by multiplying by 16.</p>
 */
public final class SeedCrackerImporter {

    private static final int CHUNK_SIZE = 16;
    private static final int TOLERANCE_BLOCKS = 8;

    private SeedCrackerImporter() {
    }

    public static ImportResult read(Path file, String versionText) throws IOException {
        List<String> lines = Files.readAllLines(file);
        List<Evidence> evidence = new ArrayList<>();
        List<String> unknown = new ArrayList<>();
        int imported = 0;
        int unknownLine = 0;

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split(";");
            if (parts.length != 3) {
                unknown.add("malformed: " + line);
                continue;
            }

            String name = parts[0].trim();
            Optional<StructureType> type = StructureType.fromName(name);
            if (type.isEmpty()) {
                unknown.add(name);
                continue;
            }

            int chunkX;
            int chunkZ;
            try {
                chunkX = Integer.parseInt(parts[1].trim());
                chunkZ = Integer.parseInt(parts[2].trim());
            } catch (NumberFormatException e) {
                unknown.add("malformed coordinates: " + line);
                continue;
            }

            evidence.add(new StructureEvidence(
                    "sc-" + (++imported),
                    "world",
                    type.get().canonicalName(),
                    chunkX * CHUNK_SIZE,
                    chunkZ * CHUNK_SIZE,
                    TOLERANCE_BLOCKS,
                    "seedcracker:" + file.getFileName(),
                    1.0
            ));
        }

        Project project = new Project(
                Project.CURRENT_SCHEMA_VERSION,
                new ProjectInfo(
                        "Imported SeedCrackerX data",
                        "Imported from " + file.toAbsolutePath()
                                + " (" + imported + " structure sighting(s))."
                ),
                new WorldDescriptor("java", versionText, "overworld", false),
                List.of(),
                evidence,
                Constraints.empty()
        );

        return new ImportResult(project, imported, unknown);
    }
}
