# patch

Sources for the two classes replaced inside `EggEmAll2-2.1.1.jar`. See the
[root README](../README.md) for the diagnosis.

```
src/dev/shadmage/eggemall/lib/MinecraftVersion.java             Foundation version detection
src/dev/shadmage/eggemall/lib/remain/nbt/MinecraftVersion.java  bundled NBT-API version table
build.sh                                                        recompile and repack the jar
test/run.sh                                                     check the result
```

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
