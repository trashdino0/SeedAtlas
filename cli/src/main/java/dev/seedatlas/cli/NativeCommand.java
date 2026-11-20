package dev.seedatlas.cli;

import dev.seedatlas.nativebridge.BiomeRegistry;
import dev.seedatlas.nativebridge.Cubiomes;
import dev.seedatlas.nativebridge.CubiomesVersion;
import dev.seedatlas.nativebridge.NativeLoader;
import dev.seedatlas.nativebridge.StructureConfigInfo;
import dev.seedatlas.nativebridge.StructureType;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(
        name = "native",
        description = "Show Cubiomes native-bridge diagnostics."
)
public final class NativeCommand implements Callable<Integer> {

    @Option(names = {"-v", "--version"}, defaultValue = "1.21.4",
            description = "Minecraft Java version. Default: ${DEFAULT-VALUE}.")
    private String versionText;

    @Override
    public Integer call() {
        if (!Cubiomes.isAvailable()) {
            System.out.println("Cubiomes native: UNAVAILABLE");
            System.out.println("  reason: " + NativeLoader.error());
            System.out.println("  hint:   run native\\build-cubiomes.bat and native\\build-jni.bat.");
            return 1;
        }

        CubiomesVersion version = CubiomesVersion.fromId(versionText).orElse(null);
        if (version == null) {
            System.err.println("error: unsupported version '" + versionText + "'.");
            return 1;
        }

        System.out.println("Cubiomes native: loaded");
        System.out.println("  newest native version id: " + Cubiomes.newestVersion());
        System.out.println("  requested version:        " + versionText + " (native id " + version.nativeId() + ")");
        System.out.println("  known biome names:        " + BiomeRegistry.byName(version).size());

        for (StructureType type : new StructureType[]{
                StructureType.VILLAGE,
                StructureType.MONUMENT,
                StructureType.ANCIENT_CITY,
                StructureType.TRIAL_CHAMBERS
        }) {
            StructureConfigInfo config = Cubiomes.structureConfig(type, version).orElse(null);
            if (config == null) {
                System.out.println("  " + type.canonicalName() + ": unavailable");
            } else {
                System.out.println("  " + type.canonicalName() + ": salt=" + config.salt()
                        + " region=" + config.regionSize()
                        + " range=" + config.chunkRange());
            }
        }

        return 0;
    }
}
