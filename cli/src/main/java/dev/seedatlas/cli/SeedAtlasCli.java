package dev.seedatlas.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "seedatlas",
        mixinStandardHelpOptions = true,
        version = "seedatlas 0.1.0-SNAPSHOT",
        description = "Offline Minecraft Java seed-investigation workbench.",
        subcommands = {
                InitCommand.class,
                ValidateCommand.class,
                InfoCommand.class,
                LocalizeCommand.class,
                IngestCommand.class,
                DeriveCommand.class,
                SolveCommand.class,
                NativeCommand.class,
                VersionCommand.class
        }
)
public final class SeedAtlasCli implements Runnable {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SeedAtlasCli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}
