package dev.seedatlas.cli;

import dev.seedatlas.core.io.ProjectIO;
import dev.seedatlas.core.model.BiomeEvidence;
import dev.seedatlas.core.model.Project;
import dev.seedatlas.core.model.SpawnEvidence;
import dev.seedatlas.core.model.StrongholdEvidence;
import dev.seedatlas.core.model.StructureEvidence;
import dev.seedatlas.nativebridge.Cubiomes;
import dev.seedatlas.nativebridge.CubiomesVersion;
import dev.seedatlas.nativebridge.NativeLoader;
import dev.seedatlas.testkit.SeedEvidenceGenerator;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Builds a synthetic evidence bundle from a known seed. This is the fixture
 * generator used for golden tests and a quick way to sanity-check Cubiomes.
 */
@Command(
        name = "derive",
        description = "Generate an evidence bundle from a known seed via Cubiomes."
)
public final class DeriveCommand implements Callable<Integer> {

    @Spec
    private CommandSpec spec;

    @Option(names = {"-s", "--seed"}, required = true,
            description = "Known 64-bit world seed (decimal, may be negative).")
    private long seed;

    @Option(names = {"-v", "--version"}, defaultValue = "1.21.4",
            description = "Minecraft Java version. Default: ${DEFAULT-VALUE}.")
    private String versionText;

    @Option(names = {"-r", "--regions"}, defaultValue = "1",
            description = "Structure search radius in regions. Default: ${DEFAULT-VALUE}.")
    private int regionRadius;

    @Option(names = {"-b", "--biomes"}, defaultValue = "1",
            description = "Biome sample radius in 256-block steps. Default: ${DEFAULT-VALUE}.")
    private int biomeRadius;

    @Option(names = "--include-seed",
            description = "Embed the known seed as a seed_hint evidence item.")
    private boolean includeSeed;

    @Option(names = {"-o", "--out"}, paramLabel = "PROJECT_JSON",
            description = "Write the generated bundle to this path.")
    private Path out;

    @Override
    public Integer call() {
        if (!Cubiomes.isAvailable()) {
            spec.commandLine().getErr().println(
                    "error: Cubiomes native library is unavailable: " + NativeLoader.error());
            spec.commandLine().getErr().println(
                    "hint: run native\\build-cubiomes.bat and native\\build-jni.bat, then retry.");
            return 1;
        }

        CubiomesVersion version = CubiomesVersion.fromId(versionText).orElse(null);
        if (version == null) {
            spec.commandLine().getErr().println(
                    "error: unsupported version '" + versionText + "'.");
            spec.commandLine().getErr().println(
                    "hint: try 1.18.2, 1.19.4, 1.20.6, 1.21.1, 1.21.3, or 1.21.4.");
            return 1;
        }

        Project project;
        try {
            project = SeedEvidenceGenerator.derive(
                    seed,
                    versionText,
                    version,
                    regionRadius,
                    biomeRadius,
                    includeSeed
            );
        } catch (RuntimeException e) {
            spec.commandLine().getErr().println("error: evidence generation failed: " + e.getMessage());
            return 1;
        }

        long structures = project.evidence().stream()
                .filter(StructureEvidence.class::isInstance)
                .count();
        long biomes = project.evidence().stream()
                .filter(BiomeEvidence.class::isInstance)
                .count();
        long strongholds = project.evidence().stream()
                .filter(StrongholdEvidence.class::isInstance)
                .count();
        boolean hasSpawn = project.evidence().stream()
                .anyMatch(SpawnEvidence.class::isInstance);

        var outStream = spec.commandLine().getOut();
        outStream.println("Derived fixture for seed " + seed + " (" + versionText + ")");
        outStream.println("  structures:  " + structures);
        outStream.println("  strongholds: " + strongholds);
        outStream.println("  biomes:      " + biomes);
        outStream.println("  spawn:       " + (hasSpawn ? "estimated" : "unavailable"));
        outStream.println("  evidence:    " + project.evidence().size() + " item(s)");

        if (out != null) {
            try {
                ProjectIO.write(out, project);
                outStream.println("Wrote " + out.toAbsolutePath());
            } catch (IOException e) {
                spec.commandLine().getErr().println(
                        "error: could not write " + out + ": " + e.getMessage());
                return 1;
            }
        }

        return 0;
    }
}
