package dev.seedatlas.cli;

import dev.seedatlas.core.analysis.DifficultyReport;
import dev.seedatlas.core.io.ProjectIO;
import dev.seedatlas.core.model.Anchor;
import dev.seedatlas.core.model.Position;
import dev.seedatlas.core.model.Project;
import dev.seedatlas.core.model.TextureRotationEvidence;
import dev.seedatlas.core.validation.ProjectValidator;
import dev.seedatlas.core.validation.ValidationResult;
import dev.seedatlas.engine.localization.LocalizationMatch;
import dev.seedatlas.engine.localization.LocalizationResult;
import dev.seedatlas.engine.localization.TextureLocalizer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "localize",
        description = "Locate local anchors from texture-rotation evidence."
)
public final class LocalizeCommand implements Callable<Integer> {

    @Spec
    private CommandSpec spec;

    @Parameters(index = "0", paramLabel = "PROJECT_JSON", description = "Path to project.json.")
    private Path projectFile;

    @Option(names = {"-a", "--anchor"}, description = "Only localize this anchor id.")
    private String anchorId;

    @Option(names = {"-t", "--threads"}, defaultValue = "0",
            description = "Worker threads; 0 uses all available processors.")
    private int threads;

    @Option(names = "--write", description = "Write a uniquely resolved anchor origin back to project.json.")
    private boolean write;

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

        List<Anchor> selected = project.anchors().stream()
                .filter(a -> anchorId == null || anchorId.equals(a.id()))
                .toList();

        if (selected.isEmpty()) {
            spec.commandLine().getErr().println("error: no anchor matched"
                    + (anchorId == null ? "." : " '" + anchorId + "'."));
            spec.commandLine().getErr().println("anchors: "
                    + project.anchors().stream().map(Anchor::id).toList());
            return 1;
        }

        var out = spec.commandLine().getOut();
        List<Anchor> resolvedAnchors = new ArrayList<>(project.anchors());
        boolean attempted = false;
        boolean wrote = false;

        for (Anchor anchor : selected) {
            boolean hasTexture = project.evidence().stream()
                    .anyMatch(e -> e instanceof TextureRotationEvidence t && anchor.id().equals(t.anchor()));
            if (!hasTexture) {
                out.println("Anchor '" + anchor.id() + "' has no texture-rotation evidence; skipping.");
                continue;
            }

            attempted = true;
            out.println("Localizing anchor '" + anchor.id() + "' ...");
            LocalizationResult result = TextureLocalizer.localize(project, anchor, threads);
            out.printf("  checked %s positions in %.1f s; %d candidate(s)%n",
                    DifficultyReport.formatCount(result.checkedPositions()),
                    result.elapsedMillis() / 1000.0,
                    result.matches().size());

            for (LocalizationMatch match : result.matches()) {
                Position p = match.position();
                out.printf("  (%d, %d, %d)%n", p.x(), p.y(), p.z());
            }

            if (write) {
                if (result.matches().size() == 1) {
                    Position origin = result.matches().get(0).position();
                    Anchor updated = new Anchor(anchor.id(), "world", origin, anchor.searchArea());
                    for (int i = 0; i < resolvedAnchors.size(); i++) {
                        if (resolvedAnchors.get(i).id().equals(anchor.id())) {
                            resolvedAnchors.set(i, updated);
                        }
                    }
                    wrote = true;
                    out.println("  resolved origin written: ("
                            + origin.x() + ", " + origin.y() + ", " + origin.z() + ")");
                } else {
                    out.println("  not writing: expected exactly one candidate, found "
                            + result.matches().size() + ".");
                }
            }
        }

        if (!attempted) {
            spec.commandLine().getErr().println("error: none of the selected anchors have texture-rotation evidence.");
            return 1;
        }

        if (write && wrote) {
            Project updated = new Project(
                    project.schemaVersion(),
                    project.project(),
                    project.world(),
                    resolvedAnchors,
                    project.evidence(),
                    project.constraints()
            );
            try {
                ProjectIO.write(projectFile, updated);
                out.println("Updated " + projectFile.toAbsolutePath());
            } catch (IOException e) {
                spec.commandLine().getErr().println("error: could not write " + projectFile + ": " + e.getMessage());
                return 1;
            }
        }

        return 0;
    }
}
