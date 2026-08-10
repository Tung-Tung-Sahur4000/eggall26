# eggall26

EggEmAll2 2.1.1 ([SpigotMC](https://www.spigotmc.org/resources/eggemall.122056/)) patched to load on
Minecraft 26.1.

| File | What it is |
| --- | --- |
| `EggEmAll2-2.1.1.jar` | The original release, unchanged |
| `EggEmAll2-2.1.1-mc26-fix.jar` | The 26.1 compatibility fix |
| `EggEmAll2-2.1.1-mc26-admin.jar` | **Use this one.** The fix plus an administrator-only permission set |
| `patch/` | The patched sources, the build script and the checks |

## Installing

Drop one jar into `plugins/` and **delete the old `EggEmAll2-2.1.1.jar`** — two copies of
the same plugin will not load. Configs and `data.db` are untouched, so nothing needs
migrating.

## Administrator build

`EggEmAll2-2.1.1-mc26-admin.jar` is the compatibility jar with a rewritten `plugin.yml`.
Every node is declared explicitly as `default: op`, so nothing is available to a normal
player until you grant it:

| Permission | Grants |
| --- | --- |
| `eggemall.admin` | Everything — all commands and all mob categories |
| `eggemall.all` | All four catch categories |
| `eggemall.villagers` | Villagers and wandering traders |
| `eggemall.aggressive` | Hostile mobs |
| `eggemall.passive` | Passive creatures |
| `eggemall.unknown` | Mobs in no other category |
| `eggemall.command.gui` | `/eggemall menu` |
| `eggemall.command.reload` | `/eggemall reload` |

The stock jar declared only `eggemall.all` and left the rest undeclared. Those still
resolved to op through Bukkit's fallback for unregistered permissions, so **this does not
change who can do what today** — it makes the nodes visible to LuckPerms and other
permission plugins instead of invisible, and adds `eggemall.admin` as a single node to hand
to a staff group:

```
/lp group admin permission set eggemall.admin true
```

Permissions still work normally, so you can grant narrower access without op — for example
`/lp group trusted permission set eggemall.passive true`. If you want a lock that no
permission plugin can override, that needs a code level `isOp()` check, which this build
deliberately does not do.

### Clone mode

Hitting a mob gives you the captured spawn egg and **leaves the original standing where it
is**, like pick block. The stock plugin removed the mob on capture.

```yaml
Restrictions:
  KeepOriginalOnCatch: true    # false restores the stock behaviour
```

The flag defaults to `true` when the key is absent, so an existing `settings.yml` written by
an older build picks the new behaviour up without being edited.

It gates two things in `EggListener`. `targetEntity.remove()` is skipped, and so is the
inventory dump that `DeleteInventoryOnCatch: false` would otherwise do — dropping a mob's
inventory on the floor while the mob is still holding it would duplicate the items. The egg
still drops either way.

Worth knowing: a captured mob can now be copied without limit. Every throw at the same
librarian yields another egg carrying its trades. That is what pick block behaviour means,
but it is worth thinking about before handing the permission to anyone but staff.

### Permission checks are enforced

In the stock plugin, `Restrictions.RequirePermissions: false` in `settings.yml` bypasses the
permission system entirely and lets **every** player catch mobs — the check is
`if (REQUIRE_PERMISSIONS && !hasPermission(group) && !hasPermission(mobSpecific))`, so
turning it off short circuits the whole thing.

In this build that switch is disabled. `Settings.Restrictions.init()` still reads the key,
so it stays in your `settings.yml` and the config updater leaves it alone, but the value is
discarded and the field is always `true`:

```
60: ldc           // String RequirePermissions
62: invokestatic  // Settings.getBoolean(String)
65: pop                                             <- value read, then thrown away
66: getstatic     // Boolean.TRUE
69: putstatic     // REQUIRE_PERMISSIONS
```

No branch, so there is no config, reload or permission plugin path that turns permission
checks off. This is the only behavioural change; every other restriction in `settings.yml`
still works exactly as before, since those are gameplay choices rather than access control.

The bundled `settings.yml` is byte identical to the original apart from the comment above
that key. That file is only the template for a fresh install; an existing
`plugins/EggEmAll2/settings.yml` is left alone — which is precisely why the enforcement is
in code rather than in the config.

One node cannot be declared because it is built per mob: `eggemall.catchmob.<entity>`, i.e.
`eggemall.catchmob.zombie`. Bukkit lowercases permission checks, so grant it in lowercase.
It is undeclared and therefore also op only unless granted, and it is checked *in addition*
to the category node — either one passing is enough.

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

Three classes are recompiled and replaced. Everything else in the jar is byte for byte the
original. All three are the same bug in different places: code that assumes a Minecraft
version looks like `1.X`.

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

### `dev/shadmage/eggemall/lib/remain/CompMaterial$Data`

With the load failure fixed the plugin started, then broke at runtime with a flood of
`NoClassDefFoundError: Could not initialize class CompMaterial`. Every one of those was a
cascade from a single root cause:

```
Caused by: java.lang.NumberFormatException: For input string: ".1"
	at dev.shadmage.eggemall.lib.remain.CompMaterial$Data.<clinit>(CompMaterial.java:2905)
```

CompMaterial's version holder read the server version and stripped the leading `1.` with a
fixed offset:

```java
VERSION = Integer.parseInt(getMajorVersion(Bukkit.getVersion()).substring(2));
```

`getMajorVersion` returns `1.21` on the old scheme, and `substring(2)` leaves `21`. On 26.1 it
returns `26.1`, and `substring(2)` leaves `.1`. Because that dies inside a static
initializer, the JVM marks CompMaterial permanently unusable — every later touch throws
`NoClassDefFoundError` for the rest of the server's life. CompMaterial backs the menus, the
item builder and the spawn egg handling, so the plugin was dead on arrival even though it
had enabled cleanly.

The holder now reads both parts of the version and applies the same encoding as
`MinecraftVersion.V`: `1.21` gives 21, `26.1` gives 2601. An unreadable version returns the
modern default rather than throwing, since throwing here is what bricked the class.

Only `CompMaterial$Data` is replaced. It is compiled as a top level class whose name contains
a `$`, so CompMaterial itself — including its table of roughly 1500 material constants — is
left exactly as the author shipped it.

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
./patch/build.sh          # writes both the -mc26-fix and -mc26-admin jars
./patch/test/run.sh       # checks them
```

`build.sh` compiles the two patched classes to Java 8 bytecode, matching the rest of the jar,
and swaps them into a copy of the original to make the fix jar. It then copies that and
swaps in `patch/admin/plugin.yml` and `patch/admin/settings.yml` to make the admin jar, which
is otherwise identical. It downloads a Bukkit API jar to compile against on first run.

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
