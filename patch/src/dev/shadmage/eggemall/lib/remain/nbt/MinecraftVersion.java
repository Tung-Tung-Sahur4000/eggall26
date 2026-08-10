package dev.shadmage.eggemall.lib.remain.nbt;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;

enum MinecraftVersion {
    UNKNOWN(Integer.MAX_VALUE),
    MC1_7_R4(174),
    MC1_8_R3(183),
    MC1_9_R1(191),
    MC1_9_R2(192),
    MC1_10_R1(1101),
    MC1_11_R1(1111),
    MC1_12_R1(1121),
    MC1_13_R1(1131),
    MC1_13_R2(1132),
    MC1_14_R1(1141),
    MC1_15_R1(1151),
    MC1_16_R1(1161),
    MC1_16_R2(1162),
    MC1_16_R3(1163),
    MC1_17_R1(1171),
    MC1_18_R1(1181, true),
    MC1_18_R2(1182, true),
    MC1_19_R1(1191, true),
    MC1_19_R2(1192, true),
    MC1_19_R3(1193, true),
    MC1_20_R1(1201, true),
    MC1_20_R2(1202, true),
    MC1_20_R3(1203, true),
    MC1_20_R4(1204, true),
    MC1_21_R1(1211, true),
    MC1_21_R2(1212, true),
    MC1_21_R3(1213, true),
    MC1_21_R4(1214, true),
    MC1_21_R5(1215, true);

    private static MinecraftVersion version;
    private static Boolean isForgePresent;
    private static Boolean isNeoForgePresent;
    private static Boolean isFabricPresent;
    private static Boolean isFoliaPresent;
    private final int versionId;
    private final boolean mojangMapping;
    private static final Map<String, MinecraftVersion> VERSION_TO_REVISION;

    private MinecraftVersion(int versionId) {
        this(versionId, false);
    }

    private MinecraftVersion(int versionId, boolean mojangMapping) {
        this.versionId = versionId;
        this.mojangMapping = mojangMapping;
    }

    public int getVersionId() {
        return this.versionId;
    }

    public boolean isMojangMapping() {
        return this.mojangMapping;
    }

    public String getPackageName() {
        if (this == UNKNOWN) {
            try {
                return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return this.name().replace("MC", "v");
    }

    public static boolean isAtLeastVersion(MinecraftVersion version) {
        return MinecraftVersion.getVersion().getVersionId() >= version.getVersionId();
    }

    public static boolean isNewerThan(MinecraftVersion version) {
        return MinecraftVersion.getVersion().getVersionId() > version.getVersionId();
    }

    public static MinecraftVersion getVersion() {
        if (version != null) {
            return version;
        }
        try {
            String ver = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            version = MinecraftVersion.valueOf(ver.replace("v", "MC"));
        }
        catch (Exception ex) {
            String bukkitVersion = Bukkit.getServer().getBukkitVersion().split("-")[0];
            version = VERSION_TO_REVISION.get(bukkitVersion);

            if (version == null) {
                // A release newer than this build knows about, i.e. "26.1.2.build.72". Run as
                // the newest revision we have mappings for rather than UNKNOWN: UNKNOWN has
                // versionId Integer.MAX_VALUE, which disables every method this API knows only
                // up to a given revision and falls back to the outdated 1.20.2 Mojang mappings.
                version = MinecraftVersion.newestKnown();

                Bukkit.getLogger().warning("[NBTAPI] Unknown Minecraft version '" + bukkitVersion + "', running as " + (Object)((Object)version) + ". Report NBT related issues to the plugin author.");
            }
        }
        return version;
    }

    /**
     * Returns the newest revision this build has mappings for, ignoring UNKNOWN whose
     * version id is only a sentinel.
     */
    private static MinecraftVersion newestKnown() {
        MinecraftVersion newest = MC1_7_R4;

        for (MinecraftVersion candidate : MinecraftVersion.values()) {
            if (candidate == UNKNOWN || candidate.getVersionId() <= newest.getVersionId()) continue;
            newest = candidate;
        }

        return newest;
    }

    public static boolean isFabricPresent() {
        if (isFabricPresent != null) {
            return isFabricPresent;
        }
        try {
            Class.forName("net.fabricmc.api.ModInitializer");
            isFabricPresent = true;
        }
        catch (Exception ex) {
            isFabricPresent = false;
        }
        return isFabricPresent;
    }

    public static boolean isForgePresent() {
        if (isForgePresent != null) {
            return isForgePresent;
        }
        try {
            if (MinecraftVersion.getVersion() == MC1_7_R4) {
                Class.forName("cpw.mods.fml.common.Loader");
            } else {
                Class.forName("net.minecraftforge.fml.common.Loader");
            }
            isForgePresent = true;
        }
        catch (Exception ex) {
            isForgePresent = false;
        }
        return isForgePresent;
    }

    public static boolean isNeoForgePresent() {
        if (isNeoForgePresent != null) {
            return isNeoForgePresent;
        }
        try {
            Class.forName("net.neoforged.neoforge.common.NeoForge");
            isNeoForgePresent = true;
        }
        catch (Exception ex) {
            isNeoForgePresent = false;
        }
        return isNeoForgePresent;
    }

    public static boolean isFoliaPresent() {
        if (isFoliaPresent != null) {
            return isFoliaPresent;
        }
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFoliaPresent = true;
        }
        catch (Exception ex) {
            isFoliaPresent = false;
        }
        return isFoliaPresent;
    }

    static {
        VERSION_TO_REVISION = new HashMap<String, MinecraftVersion>(){
            {
                this.put("1.20", MC1_20_R1);
                this.put("1.20.1", MC1_20_R1);
                this.put("1.20.2", MC1_20_R2);
                this.put("1.20.3", MC1_20_R3);
                this.put("1.20.4", MC1_20_R3);
                this.put("1.20.5", MC1_20_R4);
                this.put("1.20.6", MC1_20_R4);
                this.put("1.21", MC1_21_R1);
                this.put("1.21.1", MC1_21_R1);
                this.put("1.21.2", MC1_21_R2);
                this.put("1.21.3", MC1_21_R2);
                this.put("1.21.4", MC1_21_R3);
                this.put("1.21.5", MC1_21_R4);
                this.put("1.21.6", MC1_21_R5);
                this.put("1.21.7", MC1_21_R5);
            }
        };
    }
}

