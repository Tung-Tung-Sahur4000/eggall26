# patch

Sources for the two classes replaced inside `EggEmAll2-2.1.1.jar`. See the
[root README](../README.md) for the diagnosis.

```
src/dev/shadmage/eggemall/lib/MinecraftVersion.java             Foundation version detection
src/dev/shadmage/eggemall/lib/remain/CompMaterial$Data.java     CompMaterial's version holder
src/dev/shadmage/eggemall/lib/remain/nbt/MinecraftVersion.java  bundled NBT-API version table
admin/plugin.yml                                                every permission declared default: op
admin/settings.yml                                              stock config plus a RequirePermissions note
admin/src/dev/shadmage/eggemall2/Settings/Settings.java         forces RequirePermissions on
build.sh                                                        recompile and repack both jars
test/run.sh                                                     check the result
```

`build.sh` produces two jars. `-mc26-fix` is the compatibility fix alone, compiled from
`src/`. `-mc26-admin` is a copy of that with `admin/plugin.yml`, `admin/settings.yml` and the
classes compiled from `admin/src/` swapped in — nothing else differs, which `test/run.sh`
and a `diff -rq` of the extracted jars both confirm.

`admin/src/.../Settings.java` is the decompiled original with one change: the value read from
`RequirePermissions` is discarded and the field is set to `Boolean.TRUE`. The `getBoolean`
call is kept so the key stays in `settings.yml`. `test/run.sh` asserts the resulting bytecode
has no branch between reading the key and the assignment, and that the compatibility jar
still reads it normally.

The plugin ships without sources, so both files were recovered by decompiling the jar
(CFR 0.152) and edited from there. That is why they read like decompiler output in places —
the goal was to change as little as possible, so everything except the version handling is
left exactly as it was recovered.

Both classes are compiled with `--release 8` to match the Java 8 bytecode in the rest of the
jar, and `build.sh` only replaces those class files inside a copy of the original archive.
The jar is unsigned, so replacing entries does not invalidate anything.

## Constraints worth knowing before editing

- **Do not reorder or add constants in the NBT-API's `MinecraftVersion` enum.**
  `MojangToMapping.getMapping()` switches on it, and the compiled switch in the jar is indexed
  by ordinal. `test/run.sh` would catch a mismatch, but it is easy to miss.
- **`MinecraftVersion` must not call `Common` or `Valid` from its static initializer.**
  Those initialize `Remain`, which calls straight back into `MinecraftVersion` before it has
  finished setting `current`. That is what produced the NPE flood in the original log. Log
  through `Bukkit.getLogger()` instead.
- **Nothing in the static initializer may throw.** It runs while Bukkit loads the plugin main
  class, so any exception becomes an `ExceptionInInitializerError` and the plugin never loads.
- Adding constants to Foundation's `V` enum is safe: nothing outside the class uses
  `V.values()`, `ordinal()` or `valueOf()`.
- `CompMaterial$Data.java` is deliberately a top level class with a `$` in its name, which
  javac accepts, so that it replaces the nested `CompMaterial$Data` on its own and the 1500
  constant material table in `CompMaterial` is never recompiled. It must keep
  `static boolean access$000()` and `static int access$100()` exactly as they are —
  CompMaterial calls the holder only through those two synthetic accessors. It also cannot
  call `CompMaterial.getMajorVersion`, which is reachable only through a synthetic accessor
  of its own, so that logic is duplicated there and has to stay in step.
- Version parsing lives in three places now, each with its own encoding rules. When a new
  Minecraft scheme appears, grep for `Bukkit.getVersion()`, `getBukkitVersion()` and
  `getPackage().getName()` across the decompiled jar rather than fixing one at a time.
- `admin/plugin.yml` must keep `name`, `main`, `version` and `api-version` exactly as the
  original had them. `test/run.sh` parses it with Bukkit's own `PluginDescriptionFile`, the
  same reader the server uses, and fails if any declared permission is not `default: op`.
- `admin/settings.yml` is CRLF, like the file it came from. Only comments were added; no
  value was changed.
- Recompiling `Settings.java` renumbers the synthetic `access$NNNN` bridge methods javac
  generates for the nested config classes. That is safe here because those bridges are only
  ever called from within `Settings.java` itself — verified by disassembling every class in
  the jar that references `Settings` and finding no external `Settings.access$` call. Redo
  that check if the override ever grows beyond this file.
