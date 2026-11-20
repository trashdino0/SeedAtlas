package dev.seedatlas.cli;

import dev.seedatlas.core.analysis.DifficultyEstimator;
import dev.seedatlas.core.analysis.DifficultyReport;
import dev.seedatlas.core.io.ProjectIO;
import dev.seedatlas.core.model.Project;
import dev.seedatlas.core.validation.ProjectValidator;
import dev.seedatlas.core.validation.ValidationResult;
import dev.seedatlas.core.version.VersionProfiles;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
        name = "info",
        description = "Show a project summary and difficulty estimate."
)
public final class InfoCommand implements Callable<Integer> {

    @Spec
    private CommandSpec spec;

    @Parameters(index = "0", paramLabel = "PROJECT_JSON", description = "Path to project.json.")
    private Path projectFile;

    @Override
    public Integer call() {
        Project project;
        try {
            project = ProjectIO.read(projectFile);
        } catch (IOException e) {
            spec.commandLine().getErr().println("error: could not read " + projectFile + ": " + e.getMessage());
            return 1;
        }

        ValidationResult validation = ProjectValidator.validate(project);
        if (!validation.valid()) {
            CliSupport.printValidation(spec.commandLine().getOut(), spec.commandLine().getErr(), validation);
            return 1;
        }

        var out = spec.commandLine().getOut();
        out.println("Project: " + project.project().name());
        out.println("  world:     " + project.world().edition() + " " + project.world().version()
                + " / " + project.world().dimension()
                + (project.world().largeBiomes() ? " (large biomes)" : ""));
        VersionProfiles.find(project.world().version())
                .ifPresent(profile -> out.println("  profile:   " + profile.displayName()
                        + " (" + profile.textureMode() + " textures)"));
        out.println("  anchors:   " + project.anchors().size());
        out.println("  evidence:  " + project.evidence().size());
        out.println();

        DifficultyReport report = DifficultyEstimator.estimate(project);
        out.print(report.summary());
        out.flush();

        if (!validation.warnings().isEmpty()) {
            out.println();
            out.println("Warnings:");
            for (String warning : validation.warnings()) {
                out.println("  - " + warning);
            }
        }
        return 0;
    }
}
