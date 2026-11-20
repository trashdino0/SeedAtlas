package dev.seedatlas.cli;

import dev.seedatlas.core.io.ProjectIO;
import dev.seedatlas.core.model.Project;
import dev.seedatlas.core.validation.ProjectValidator;
import dev.seedatlas.core.validation.ValidationResult;
import dev.seedatlas.ingest.ImportResult;
import dev.seedatlas.ingest.SeedCrackerImporter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
        name = "ingest",
        description = "Import evidence from another tool (currently SeedCrackerX)."
)
public final class IngestCommand implements Callable<Integer> {

    @Spec
    private CommandSpec spec;

    @Parameters(index = "0", paramLabel = "FILE",
            description = "SeedCrackerX saved-structures file, or a world folder for future formats.")
    private Path input;

    @Option(names = "--format", defaultValue = "seedcracker",
            description = "Input format. Default: ${DEFAULT-VALUE}.")
    private String format;

    @Option(names = {"-v", "--version"}, defaultValue = "1.21.4",
            description = "Minecraft Java version the data was collected on. Default: ${DEFAULT-VALUE}.")
    private String versionText;

    @Option(names = {"-o", "--out"}, required = true, paramLabel = "PROJECT_JSON",
            description = "Where to write the imported evidence bundle.")
    private Path out;

    @Override
    public Integer call() {
        if (!"seedcracker".equalsIgnoreCase(format)) {
            spec.commandLine().getErr().println("error: unsupported format '" + format + "'.");
            spec.commandLine().getErr().println("hint: supported formats: seedcracker");
            return 1;
        }

        ImportResult result;
        try {
            result = SeedCrackerImporter.read(input, versionText);
        } catch (IOException e) {
            spec.commandLine().getErr().println(
                    "error: could not read " + input + ": " + e.getMessage());
            return 1;
        }

        Project project = result.project();
        ValidationResult validation = ProjectValidator.validate(project);
        if (!validation.valid()) {
            CliSupport.printValidation(spec.commandLine().getOut(), spec.commandLine().getErr(), validation);
            return 1;
        }

        try {
            ProjectIO.write(out, project);
        } catch (IOException e) {
            spec.commandLine().getErr().println(
                    "error: could not write " + out + ": " + e.getMessage());
            return 1;
        }

        var outStream = spec.commandLine().getOut();
        outStream.println("Imported " + result.importedStructures()
                + " structure sighting(s) from " + input);
        if (!result.unknownStructures().isEmpty()) {
            outStream.println("Skipped " + result.unknownStructures().size() + " unrecognized line(s):");
            result.unknownStructures().stream().limit(20)
                    .forEach(line -> outStream.println("  - " + line));
            if (result.unknownStructures().size() > 20) {
                outStream.println("  ...");
            }
        }
        outStream.println("Wrote " + out.toAbsolutePath());
        return 0;
    }
}
