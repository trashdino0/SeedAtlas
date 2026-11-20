package dev.seedatlas.core.version;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of versions SeedAtlas can validate and (eventually) solve.
 */
public final class VersionProfiles {

    private static final Map<String, VersionProfile> PROFILES = new LinkedHashMap<>();

    static {
        // Legacy / pre-1.13 rotation algorithm.
        register("1.12.2", "Minecraft 1.12.2", TextureMode.VANILLA_12);

        // 1.13 - 1.21.1 rotation algorithm.
        register("1.16.5", "Minecraft 1.16.5", TextureMode.VANILLA_21_1);
        register("1.17.1", "Minecraft 1.17.1", TextureMode.VANILLA_21_1);
        register("1.18.2", "Minecraft 1.18.2", TextureMode.VANILLA_21_1);
        register("1.19.2", "Minecraft 1.19.2", TextureMode.VANILLA_21_1);
        register("1.19.4", "Minecraft 1.19.4", TextureMode.VANILLA_21_1);
        register("1.20.1", "Minecraft 1.20.1", TextureMode.VANILLA_21_1);
        register("1.20.6", "Minecraft 1.20.6", TextureMode.VANILLA_21_1);
        register("1.21.1", "Minecraft 1.21.1", TextureMode.VANILLA_21_1);

        // 1.21.2+ rotation algorithm.
        register("1.21.3", "Minecraft 1.21.3", TextureMode.VANILLA_21_2);
        register("1.21.4", "Minecraft 1.21.4", TextureMode.VANILLA_21_2);
        register("1.21.5", "Minecraft 1.21.5", TextureMode.VANILLA_21_2);
        register("1.21.6", "Minecraft 1.21.6", TextureMode.VANILLA_21_2);
        register("1.21.7", "Minecraft 1.21.7", TextureMode.VANILLA_21_2);
        register("1.21.8", "Minecraft 1.21.8", TextureMode.VANILLA_21_2);
    }

    private VersionProfiles() {
    }

    private static void register(String id, String displayName, TextureMode mode) {
        PROFILES.put(id, new VersionProfile(id, displayName, mode));
    }

    public static Optional<VersionProfile> find(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(PROFILES.get(id.trim()));
    }

    public static boolean isSupported(String id) {
        return find(id).isPresent();
    }

    public static Collection<VersionProfile> all() {
        return PROFILES.values();
    }

    public static String supportedIds() {
        return String.join(", ", PROFILES.keySet());
    }
}
