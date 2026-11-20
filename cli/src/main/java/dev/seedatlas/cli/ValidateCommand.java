package dev.seedatlas.cli;

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
        name = "validate",
        description = "Validate a project bundle."
)
public final class ValidateCommand implements Callable<Integer> {

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

        ValidationResult result = ProjectValidator.validate(project);
        CliSupport.printValidation(spec.commandLine().getOut(), spec.commandLine().getErr(), result);
        return result.valid() ? 0 : 1;
    }
}
