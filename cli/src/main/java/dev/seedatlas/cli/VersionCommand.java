package dev.seedatlas.cli;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
        name = "version",
        description = "Print the SeedAtlas version."
)
public final class VersionCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("seedatlas 0.1.0-SNAPSHOT");
        return 0;
    }
}
