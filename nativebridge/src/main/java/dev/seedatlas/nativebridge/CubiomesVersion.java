package dev.seedatlas.nativebridge;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Mapping from Minecraft version strings to Cubiomes' {@code MCVersion} enum.
 *
 * <p>Keep this in sync with {@code native/cubiomes/biomes.h}.</p>
 */
public enum CubiomesVersion {

    MC_1_12_2(15, "1.12.2"),
    MC_1_16_5(20, "1.16.5"),
    MC_1_17_1(21, "1.17.1"),
    MC_1_18_2(22, "1.18.2"),
    MC_1_19_2(23, "1.19.2"),
    MC_1_19_4(24, "1.19.4"),
    MC_1_20_6(25, "1.20.6"),
    MC_1_21_1(26, "1.21.1"),
    MC_1_21_3(27, "1.21.3"),
    MC_1_21_WD(28, "1.21.4");

    private static final Map<String, CubiomesVersion> BY_ID = new LinkedHashMap<>();

    static {
        for (CubiomesVersion version : values()) {
            BY_ID.put(version.id, version);
        }
        // Cubiomes' MC_1_21_WD covers the winter-drop family.
        BY_ID.put("1.21.5", MC_1_21_WD);
        BY_ID.put("1.21.6", MC_1_21_WD);
        BY_ID.put("1.21.7", MC_1_21_WD);
        BY_ID.put("1.21.8", MC_1_21_WD);
        BY_ID.put("1.21.2", MC_1_21_3);
    }

    private final int nativeId;
    private final String id;

    CubiomesVersion(int nativeId, String id) {
        this.nativeId = nativeId;
        this.id = id;
    }

    public int nativeId() {
        return nativeId;
    }

    public String id() {
        return id;
    }

    public static Optional<CubiomesVersion> fromId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_ID.get(id.trim().toLowerCase(Locale.ROOT)));
    }
}
