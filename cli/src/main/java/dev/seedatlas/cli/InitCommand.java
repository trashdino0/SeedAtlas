package dev.seedatlas.cli;

import dev.seedatlas.core.io.ProjectIO;
import dev.seedatlas.core.model.Project;
import dev.seedatlas.core.model.WorldDescriptor;
import dev.seedatlas.core.version.VersionProfile;
import dev.seedatlas.core.version.VersionProfiles;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;

@Command(
        name = "init",
        description = "Create a new project bundle (project.json) in a directory."
)
public final class InitCommand implements Callable<Integer> {

    private static final Set<String> DIMENSIONS = Set.of("overworld", "nether", "end");

    @Spec
    private CommandSpec spec;

    @Parameters(index = "0", paramLabel = "DIR", description = "Project directory to create.")
    private Path directory;

    @Option(names = {"-n", "--name"}, defaultValue = "Untitled", description = "Project name.")
    private String name;

    @Option(names = {"-v", "--version"}, defaultValue = "1.21.4", description = "Minecraft Java version.")
    private String version;

    @Option(names = {"-d", "--dimension"}, defaultValue = "overworld", description = "overworld, nether, or end.")
    private String dimension;

    @Option(names = "--large-biomes", description = "Mark the world as using large biomes.")
    private boolean largeBiomes;

    @Option(names = "--force", description = "Overwrite an existing project.json.")
    private boolean force;

    @Override
    public Integer call() {
        VersionProfile profile = VersionProfiles.find(version).orElse(null);
        if (profile == null) {
            spec.commandLine().getErr().println("error: unsupported Minecraft version '" + version
                    + "'. Supported: " + VersionProfiles.supportedIds() + ".");
            return 1;
        }

        String normalizedDimension = dimension == null ? "" : dimension.trim().toLowerCase(Locale.ROOT);
        if (!DIMENSIONS.contains(normalizedDimension)) {
            spec.commandLine().getErr().println("error: dimension must be one of " + DIMENSIONS + ".");
            return 1;
        }

        Path projectFile = directory.toAbsolutePath().resolve("project.json");
        if (Files.exists(projectFile) && !force) {
            spec.commandLine().getErr().println("error: " + projectFile + " already exists; use --force to overwrite.");
            return 1;
        }

        Project project = Project.empty(
                name,
                new WorldDescriptor("java", profile.id(), normalizedDimension, largeBiomes)
        );

        try {
            ProjectIO.write(projectFile, project);
        } catch (IOException e) {
            spec.commandLine().getErr().println("error: could not write " + projectFile + ": " + e.getMessage());
            return 1;
        }

        spec.commandLine().getOut().println("Created " + projectFile);
        spec.commandLine().getOut().println("  version:   " + profile.displayName());
        spec.commandLine().getOut().println("  dimension: " + normalizedDimension);
        spec.commandLine().getOut().println("Next: add evidence to project.json, then run `seedatlas validate`.");
        return 0;
    }
}
