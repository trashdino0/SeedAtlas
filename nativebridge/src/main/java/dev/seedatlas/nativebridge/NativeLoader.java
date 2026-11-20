package dev.seedatlas.nativebridge;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Loads the platform-native SeedAtlas bridge.
 *
 * <p>Development builds put the library on {@code java.library.path}. Packaged
 * builds embed it under {@code /native/<os>-<arch>/} and extract it to a temp
 * file because the JVM cannot load a shared library directly from a JAR.</p>
 */
public final class NativeLoader {

    private static volatile boolean loaded;
    private static volatile String error = "not attempted";

    private NativeLoader() {
    }

    public static synchronized boolean load() {
        if (loaded) {
            return true;
        }

        try {
            System.loadLibrary("seedatlas");
            loaded = true;
            error = null;
            return true;
        } catch (UnsatisfiedLinkError ignored) {
            // Fall through to the embedded resource.
        }

        try {
            loadFromResource();
            loaded = true;
            error = null;
            return true;
        } catch (IOException | UnsatisfiedLinkError e) {
            error = e.getMessage();
            return false;
        }
    }

    private static void loadFromResource() throws IOException {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);

        String platform;
        String library;
        if (os.contains("win")) {
            platform = "windows-" + (arch.contains("64") ? "x86_64" : "x86");
            library = "seedatlas.dll";
        } else if (os.contains("mac")) {
            platform = "macos-" + (arch.contains("aarch64") ? "aarch64" : "x86_64");
            library = "libseedatlas.dylib";
        } else {
            platform = "linux-" + (arch.contains("64") ? "x86_64" : "x86");
            library = "libseedatlas.so";
        }

        String resource = "/native/" + platform + "/" + library;
        InputStream input = NativeLoader.class.getResourceAsStream(resource);
        if (input == null) {
            throw new IOException("Native library resource not found: " + resource);
        }

        String suffix = library.substring(library.lastIndexOf('.'));
        Path temp = Files.createTempFile("seedatlas-native-", suffix);
        temp.toFile().deleteOnExit();
        try (input) {
            Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
        }
        System.load(temp.toAbsolutePath().toString());
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static String error() {
        return error;
    }
}
