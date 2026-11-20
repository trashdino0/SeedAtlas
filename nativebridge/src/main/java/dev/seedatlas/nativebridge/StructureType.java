package dev.seedatlas.nativebridge;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Cubiomes structure types plus tolerant name aliases used in evidence bundles.
 */
public enum StructureType {

    FEATURE(0, "feature"),
    DESERT_PYRAMID(1, "desert_pyramid"),
    JUNGLE_TEMPLE(2, "jungle_temple"),
    SWAMP_HUT(3, "swamp_hut"),
    IGLOO(4, "igloo"),
    VILLAGE(5, "village"),
    OCEAN_RUIN(6, "ocean_ruin"),
    SHIPWRECK(7, "shipwreck"),
    MONUMENT(8, "monument"),
    MANSION(9, "mansion"),
    OUTPOST(10, "outpost"),
    RUINED_PORTAL(11, "ruined_portal"),
    RUINED_PORTAL_N(12, "ruined_portal_nether"),
    ANCIENT_CITY(13, "ancient_city"),
    TREASURE(14, "buried_treasure"),
    MINESHAFT(15, "mineshaft"),
    DESERT_WELL(16, "desert_well"),
    GEODE(17, "geode"),
    FORTRESS(18, "fortress"),
    BASTION(19, "bastion"),
    END_CITY(20, "end_city"),
    END_GATEWAY(21, "end_gateway"),
    END_ISLAND(22, "end_island"),
    TRAIL_RUINS(23, "trail_ruins"),
    TRIAL_CHAMBERS(24, "trial_chambers");

    private static final Map<String, StructureType> BY_NAME = new LinkedHashMap<>();

    static {
        for (StructureType type : values()) {
            BY_NAME.put(type.canonicalName, type);
        }
        alias("village_plains", VILLAGE);
        alias("village_desert", VILLAGE);
        alias("village_savanna", VILLAGE);
        alias("village_snowy", VILLAGE);
        alias("village_taiga", VILLAGE);
        alias("pillager_outpost", OUTPOST);
        alias("ocean_monument", MONUMENT);
        alias("woodland_mansion", MANSION);
        alias("nether_fortress", FORTRESS);
        alias("trial_chamber", TRIAL_CHAMBERS);
        alias("ruined_portal_standard", RUINED_PORTAL);
        alias("buried_treasure", TREASURE);
        alias("endcity", END_CITY);
        alias("jungle_pyramid", JUNGLE_TEMPLE);
        alias("bastion_remnant", BASTION);
        alias("nether_fossil", FEATURE);
    }

    private final int nativeId;
    private final String canonicalName;

    StructureType(int nativeId, String canonicalName) {
        this.nativeId = nativeId;
        this.canonicalName = canonicalName;
    }

    public int nativeId() {
        return nativeId;
    }

    public String canonicalName() {
        return canonicalName;
    }

    public static Optional<StructureType> fromName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String normalized = name.trim()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        return Optional.ofNullable(BY_NAME.get(normalized));
    }

    private static void alias(String name, StructureType type) {
        BY_NAME.put(name, type);
    }
}
