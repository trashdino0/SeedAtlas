package dev.seedatlas.cli;

import dev.seedatlas.core.analysis.DifficultyEstimator;
import dev.seedatlas.core.analysis.DifficultyReport;
import dev.seedatlas.core.io.ProjectIO;
import dev.seedatlas.core.model.Project;
import dev.seedatlas.core.validation.ProjectValidator;
import dev.seedatlas.core.validation.ValidationResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
        name = "solve",
        description = "Plan a solve. The cracking engine is not implemented until M2."
)
public final class SolveCommand implements Callable<Integer> {

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
        out.println("Solve plan (dry run)");
        out.println();
        DifficultyReport report = DifficultyEstimator.estimate(project);
        out.print(report.summary());
        out.flush();
        out.println();
        out.println("The Cubiomes-backed solver and lifting routes arrive in M2.");
        out.println("This command currently validates the bundle and estimates the search space.");
        return 0;
    }
}
