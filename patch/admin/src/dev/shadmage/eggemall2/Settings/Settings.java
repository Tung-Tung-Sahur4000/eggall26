package dev.shadmage.eggemall2.Settings;

import dev.shadmage.eggemall.lib.Common;
import dev.shadmage.eggemall.lib.settings.SimpleSettings;
import java.util.List;
import org.bukkit.entity.EntityType;

public final class Settings
extends SimpleSettings {
    public static String LOG_PREFIX;
    public static String CHAT_PREFIX;

    @Override
    protected boolean saveComments() {
        return true;
    }

    private static void init() {
        Settings.setPathPrefix(null);
        LOG_PREFIX = Settings.getString("LogPrefix");
        CHAT_PREFIX = Settings.getString("ChatPrefix");
    }

    @Override
    protected int getConfigVersion() {
        Settings.setPathPrefix("");
        return Settings.getInteger("Version");
    }

    public static class Messages {
        public static String NO_PERMISSION;
        public static String CATCH_SUCCESS;
        public static String CATCH_FAILED_CHANCE;
        public static String NO_BABIES;
        public static String NO_TAMED;
        public static String NO_SHEARED_SHEEP;
        public static String NO_NAMED_ENTITIES;
        public static String BLACKLISTED_WORLD;
        public static String NOT_CATCHABLE;

        private static void init() {
            Settings.setPathPrefix("Messages");
            NO_PERMISSION = Settings.getString("NoPermissions");
            CATCH_SUCCESS = Settings.getString("CatchSuccess");
            CATCH_FAILED_CHANCE = Settings.getString("CatchFailedChance");
            NO_BABIES = Settings.getString("NoBabies");
            NO_TAMED = Settings.getString("NoTamed");
            NO_SHEARED_SHEEP = Settings.getString("NoShearedSheep");
            NO_NAMED_ENTITIES = Settings.getString("NoNamedEntities");
            BLACKLISTED_WORLD = Settings.getString("BlacklistedWorld");
            NOT_CATCHABLE = Settings.getString("NotCatchable");
        }
    }

    public static class GUI {
        public static String MAIN_TITLE;
        public static String CATCHABLE_ENTITIES_TITLE;
        public static String BLACKLISTED_ENTITIES_TITLE;

        private static void init() {
            Settings.setPathPrefix("GUI");
            MAIN_TITLE = Settings.getString("Title");
            CATCHABLE_ENTITIES_TITLE = Settings.getString("CatchableEntitiesTitle");
            BLACKLISTED_ENTITIES_TITLE = Settings.getString("BlacklistedEntitiesTitle");
        }
    }

    public static class General {
        public static Boolean STARTUP_CONSOLE_STATS;

        private static void init() {
            Settings.setPathPrefix("General");
            STARTUP_CONSOLE_STATS = Settings.getBoolean("StartupConsoleStats");
        }
    }

    public static class EntityInventories {
        public static Boolean ERASE_ENTITY_INVENTORY;

        private static void init() {
            Settings.setPathPrefix("EntityInventories");
            ERASE_ENTITY_INVENTORY = Settings.getBoolean("DeleteInventoryOnCatch");
        }
    }

    public static class Restrictions {
        public static Boolean ONLY_ALLOW_PLAYER_THROWN_EGGS;
        public static Boolean PREVENT_CATCHING_BABIES;
        public static Boolean PREVENT_CATCHING_TAMED;
        public static Boolean PREVENT_CATCHING_SHEARED_SHEEP;
        public static Boolean PREVENT_CATCHING_NAMED_ENTITIES;
        public static Boolean REQUIRE_PERMISSIONS;
        public static Boolean KEEP_ORIGINAL_ON_CATCH;
        public static List<EntityType> BLACKLISTED_ENTITIES;

        private static void init() {
            Settings.setPathPrefix("Restrictions");
            ONLY_ALLOW_PLAYER_THROWN_EGGS = Settings.getBoolean("OnlyAllowPlayerThrownEggs");
            PREVENT_CATCHING_BABIES = Settings.getBoolean("PreventCatchingBabyEntities");
            PREVENT_CATCHING_TAMED = Settings.getBoolean("PreventCatchingTamedEntities");
            PREVENT_CATCHING_SHEARED_SHEEP = Settings.getBoolean("PreventCatchingShearedSheep");
            PREVENT_CATCHING_NAMED_ENTITIES = Settings.getBoolean("PreventCatchingNamedEntities");
            // Administrator build: permission checks are always on. The key is still read so
            // that it stays in settings.yml and the config updater leaves it alone, but the
            // value is discarded - setting it to false cannot open catching up to everyone.
            Settings.getBoolean("RequirePermissions");
            REQUIRE_PERMISSIONS = Boolean.TRUE;
            // Clone mode. Defaults to on when the key is absent, so an existing
            // settings.yml written by an older build still gets the new behaviour.
            KEEP_ORIGINAL_ON_CATCH = Common.getOrDefault(Settings.getBoolean("KeepOriginalOnCatch"), Boolean.TRUE);
            BLACKLISTED_ENTITIES = Settings.getList("EntityBlacklist", EntityType.class);
        }
    }

    public static class NBT {
        public static Boolean MAINTAIN_ENTITY_DATA;

        private static void init() {
            Settings.setPathPrefix("NBT");
            MAINTAIN_ENTITY_DATA = Settings.getBoolean("MaintainEntityDataValues");
        }
    }

    public static class CatchChance {
        public static Integer CHANCE_PERCENTAGE;
        public static Boolean SPAWN_CHICKEN_ON_FAIL;
        public static Boolean REMOVE_ENTITY_ON_FAIL_CHANCE;
        public static Boolean ADD_LORE_TO_EGG;
        public static List<String> LORE_LINES;

        private static void init() {
            Settings.setPathPrefix("CatchChance");
            CHANCE_PERCENTAGE = Common.getOrDefault(Settings.getInteger("ChancePercentage"), 100);
            SPAWN_CHICKEN_ON_FAIL = Settings.getBoolean("SpawnChickenOnFail");
            REMOVE_ENTITY_ON_FAIL_CHANCE = Settings.getBoolean("RemoveEntityOnFail");
            ADD_LORE_TO_EGG = Settings.getBoolean("AddLoreToSpawnEgg");
            LORE_LINES = Settings.getList("Lore_Lines", String.class);
        }
    }

    public static class Particles {
        public static Boolean EGG_TRAILS;
        public static Boolean PLAYER_THROW_ONLY;
        public static Boolean EXPLOSION_ON_SUCCESS;
        public static Boolean SMOKE_ON_ESCAPE;

        private static void init() {
            Settings.setPathPrefix("Particles");
            EGG_TRAILS = Settings.getBoolean("EggTrails");
            PLAYER_THROW_ONLY = Settings.getBoolean("PlayerThrowOnly");
            EXPLOSION_ON_SUCCESS = Settings.getBoolean("ExplosionOnSuccess");
            SMOKE_ON_ESCAPE = Settings.getBoolean("SmokeOnEscape");
        }
    }

    public static class BlacklistWorlds {
        public static Boolean AS_WHITELIST;
        public static List<String> WORLDS;

        private static void init() {
            Settings.setPathPrefix("BlacklistWorlds");
            AS_WHITELIST = Settings.getBoolean("AsWhitelist");
            WORLDS = Settings.getList("Worlds", String.class);
        }
    }
}

