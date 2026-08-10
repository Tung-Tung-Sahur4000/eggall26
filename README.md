# eggall26

EggEmAll2 2.1.1 ([SpigotMC](https://www.spigotmc.org/resources/eggemall.122056/)) patched to load on
Minecraft 26.1.

| File | What it is |
| --- | --- |
| `EggEmAll2-2.1.1.jar` | The original release, unchanged |
| `EggEmAll2-2.1.1-mc26-fix.jar` | **Use this one.** Same jar with the version detection fixed |
| `patch/` | The patched sources, the build script and the checks |

## Installing

Drop `EggEmAll2-2.1.1-mc26-fix.jar` into `plugins/` and **delete the old
`EggEmAll2-2.1.1.jar`** — two copies of the same plugin will not load. Configs and
`data.db` are untouched, so nothing needs migrating.

## What was wrong

The plugin never loaded. From the server log:

```
[18:34:51] [Server thread/ERROR]: [ModernPluginLoadingStrategy] Could not load plugin 'EggEmAll2-2.1.1.jar' in folder 'plugins'
org.bukkit.plugin.InvalidPluginException: java.lang.ExceptionInInitializerError
Caused by: dev.shadmage.eggemall.lib.exception.FoException: Report: Foundation cannot read Bukkit version:
    26.1.2.build.72, expected 2 or 3 parts separated by dots, got 5 parts
	at dev.shadmage.eggemall.lib.MinecraftVersion.<clinit>(MinecraftVersion.java:176)
	at dev.shadmage.eggemall.lib.plugin.SimplePlugin.<clinit>(SimplePlugin.java:192)
```

EggEmAll2 bundles the Foundation library, whose `MinecraftVersion` class reads
`Bukkit.getBukkitVersion()`, strips everything after the first `-`, and insists the rest
splits into exactly 2 or 3 dot separated parts:

```java
String versionString = bukkitVersion.split("\\-")[0];
String[] versions = versionString.split("\\.");
Valid.checkBoolean(versions.length == 2 || versions.length == 3, "Foundation cannot read Bukkit version: ...");
int version = Integer.parseInt(versions[1]);
current = version < 3 ? V.v1_3_AND_BELOW : V.parse(version);
```

That held for `1.21.4-R0.1-SNAPSHOT` → `1.21.4` → 3 parts. Minecraft 26.1 reports
`26.1.2.build.72-stable` → `26.1.2.build.72` → **5 parts**, so the check throws. It throws
from a static initializer that runs while Bukkit is loading the plugin's main class, which
turns into `ExceptionInInitializerError` and the plugin is dropped. Nothing else in the log
mentions EggEmAll2 again — the server simply ran without it.

Two further problems sat behind that one:

1. **The version scheme itself changed.** Even reading `26.1.2` successfully, the old code
   takes `versions[1]` — the `1` — as the Minecraft version, giving `V.v1_3_AND_BELOW`. Every
   `MinecraftVersion.atLeast(...)` gate would then pick the pre-1.3 code path.
2. **The failure was noisy.** `Valid.checkBoolean` builds an `FoException`, whose constructor
   calls into `Common`/`Remain`, which call back into `MinecraftVersion` while its initializer
   is still running. `current` is still `null`, so `compareWith` throws an NPE and prints it.
   That loop produced ~6,700 lines of `Cannot read field "minorVersionNumber" because "x0" is null`
   in the log before the real error appeared.

## The fix

Two classes are recompiled and replaced. Everything else in the jar is byte for byte the original.

### `dev/shadmage/eggemall/lib/MinecraftVersion`

- Reads the **leading numeric parts** of the version and stops at the first non-number, so
  `26.1.2.build.72` gives `26, 1, 2` and `1.21.4` still gives `1, 21, 4`.
- Understands both schemes: `1.MINOR.PATCH` keeps its old meaning, anything else is read as
  the calendar `YEAR.MAJOR.PATCH`. A new `V.v26_1` constant sorts above every `1.x` release,
  so all the `atLeast(v1_13)` style gates resolve to the modern code paths.
- **Never throws.** An unreadable version, or a release newer than this build knows, falls
  back to the newest known version with a one line warning. A future `27.1` will not
  re-break the plugin the way `26.1` did.
- Does not touch `Common` or `Valid` during initialization, which is what caused the NPE
  storm. It logs through `Bukkit.getLogger()` instead.
- `getFullVersion()` now reports what the server actually said (`26.1.2`) rather than
  rebuilding it from the enum.

### `dev/shadmage/eggemall/lib/remain/nbt/MinecraftVersion`

The bundled NBT-API keeps its own lookup table of exact version strings, ending at 1.21.7.
`26.1.2` is not in it, so it resolved to `UNKNOWN`, whose sentinel id of `Integer.MAX_VALUE`
disables every NBT method the API knows only up to a given revision and falls back to the
outdated 1.20.2 Mojang mappings. The plugin's menus hit this on every click
(`Menu` → `ItemUtil.isSimilar` → `NBTItem`).

An unknown version now resolves to the newest revision the API actually has mappings for
(`MC1_21_R5`) with a warning, instead of `UNKNOWN`. The enum constants and their order are
unchanged, so the `switch` in `MojangToMapping` still lines up.

## Rebuilding

```bash
./patch/build.sh          # writes EggEmAll2-2.1.1-mc26-fix.jar
./patch/test/run.sh       # checks the patched jar
```

`build.sh` compiles the two patched classes to Java 8 bytecode, matching the rest of the jar,
and swaps them into a copy of the original. It downloads a Bukkit API jar to compile against
on first run.

`run.sh` feeds a range of version strings through the detection in a fresh JVM each time and
checks what comes out, including the exact class load that failed on the server. Run it against
the original jar to see the failure reproduced:

```bash
./patch/test/run.sh EggEmAll2-2.1.1.jar
```

## Known limits

- The NBT-API in this build has mappings up to Minecraft 1.21.7. It now runs against 26.1 using
  those, which is the best this jar can do — if 26.1 moved the NBT internals, individual NBT
  calls can still fail and will say so in the console. A proper fix needs an upstream
  NBT-API release that supports 26.1.
- Foundation's NMS reflection already failed gracefully before this change ("Mojang mappings
  detected, failing NMS gracefully" in the log) and still does. The plugin makes no direct NMS
  calls, so this only affects Foundation internals that have non-NMS fallbacks.
- These are patches to a third party binary. The real fix is the author shipping a build with
  an updated Foundation and NBT-API.
