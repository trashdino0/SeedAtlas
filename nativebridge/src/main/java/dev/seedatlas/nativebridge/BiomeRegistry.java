package dev.seedatlas.nativebridge;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lazily builds a biome-name to id map by querying Cubiomes for ids 0..255.
 */
public final class BiomeRegistry {

    private static final int MAX_BIOME_ID = 255;
    private static final Map<Integer, BiomeTable> TABLES = new ConcurrentHashMap<>();

    private BiomeRegistry() {
    }

    public static OptionalInt id(CubiomesVersion version, String biomeName) {
        if (biomeName == null) {
            return OptionalInt.empty();
        }
        Integer id = table(version).byName().get(normalize(biomeName));
        return id == null ? OptionalInt.empty() : OptionalInt.of(id);
    }

    public static String name(CubiomesVersion version, int biomeId) {
        String[] names = table(version).names();
        if (biomeId < 0 || biomeId >= names.length) {
            return "";
        }
        return names[biomeId] == null ? "" : names[biomeId];
    }

    public static Map<String, Integer> byName(CubiomesVersion version) {
        return table(version).byName();
    }

    private static BiomeTable table(CubiomesVersion version) {
        return TABLES.computeIfAbsent(version.nativeId(), ignored -> build(version));
    }

    private static BiomeTable build(CubiomesVersion version) {
        Map<String, Integer> byName = new LinkedHashMap<>();
        String[] names = new String[MAX_BIOME_ID + 1];

        for (int id = 0; id <= MAX_BIOME_ID; id++) {
            String name = Cubiomes.biomeName(version, id);
            if (name == null || name.isBlank()) {
                continue;
            }
            names[id] = name;
            byName.putIfAbsent(normalize(name), id);
        }

        return new BiomeTable(Map.copyOf(byName), names);
    }

    private static String normalize(String name) {
        return name.trim()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
    }

    private record BiomeTable(Map<String, Integer> byName, String[] names) {
    }
}
