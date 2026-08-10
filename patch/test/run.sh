#!/usr/bin/env bash
#
# Feeds a range of Bukkit version strings through the patched version detection.
# MinecraftVersion caches its result in a static initializer, so each case needs
# its own JVM.
#
set -uo pipefail

readonly TEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly PATCH_DIR="$(dirname "$TEST_DIR")"
readonly ROOT_DIR="$(dirname "$PATCH_DIR")"

readonly JAR="${1:-$ROOT_DIR/EggEmAll2-2.1.1-mc26-fix.jar}"
readonly API_JAR="$PATCH_DIR/.build/spigot-api.jar"
readonly OUT="$PATCH_DIR/.build/test-classes"

# Bukkit expects Guava and SnakeYAML on the classpath, the server provides both at runtime
readonly GUAVA_JAR="$PATCH_DIR/.build/guava.jar"
readonly GUAVA_URL="https://repo1.maven.org/maven2/com/google/guava/guava/33.3.1-jre/guava-33.3.1-jre.jar"
readonly SNAKEYAML_JAR="$PATCH_DIR/.build/snakeyaml.jar"
readonly SNAKEYAML_URL="https://repo1.maven.org/maven2/org/yaml/snakeyaml/2.2/snakeyaml-2.2.jar"
readonly GSON_JAR="$PATCH_DIR/.build/gson.jar"
readonly GSON_URL="https://repo1.maven.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar"
readonly BUNGEE_JAR="$PATCH_DIR/.build/bungeechat.jar"
readonly BUNGEE_URL="https://repo1.maven.org/maven2/net/md-5/bungeecord-chat/1.16-R0.4/bungeecord-chat-1.16-R0.4.jar"

if [ ! -f "$JAR" ]; then
	echo "No such jar: $JAR - run patch/build.sh first" >&2
	exit 1
fi

[ -f "$GUAVA_JAR" ] || curl -sSf -o "$GUAVA_JAR" "$GUAVA_URL"
[ -f "$SNAKEYAML_JAR" ] || curl -sSf -o "$SNAKEYAML_JAR" "$SNAKEYAML_URL"
[ -f "$GSON_JAR" ] || curl -sSf -o "$GSON_JAR" "$GSON_URL"
[ -f "$BUNGEE_JAR" ] || curl -sSf -o "$BUNGEE_JAR" "$BUNGEE_URL"

mkdir -p "$OUT"
javac -nowarn -encoding UTF-8 -cp "$JAR:$API_JAR" -d "$OUT" "$TEST_DIR"/*.java || exit 1

readonly CP="$OUT:$JAR:$API_JAR:$GUAVA_JAR:$SNAKEYAML_JAR:$GSON_JAR:$BUNGEE_JAR"
failed=0

run() {
	java -cp "$CP" "$@" 2>&1 | grep -v 'JAVA_TOOL_OPTIONS' || failed=1
}

echo "== Foundation version detection =="
#                            bukkit version            expected V        sub  full
run VersionTest "26.1.2.build.72-stable"   v26_1            2    26.1.2
run VersionTest "26.1.build.4-stable"      v26_1            0    26.1
run VersionTest "27.3.1.build.5-stable"    v26_1            1    27.3.1
run VersionTest "1.21.4-R0.1-SNAPSHOT"     v1_21            4    1.21.4
run VersionTest "1.21-R0.1-SNAPSHOT"       v1_21            0    1.21
run VersionTest "1.16.5-R0.1-SNAPSHOT"     v1_16            5    1.16.5
run VersionTest "1.8.8-R0.1-SNAPSHOT"      v1_8             8    1.8.8
run VersionTest "1.2.5-R1.0"               v1_3_AND_BELOW   5    1.2.5
# An unreadable version reports itself verbatim so it shows up in logs
run VersionTest "garbage"                  v26_1            0    garbage
run VersionTest ""                         v26_1            0    26.1

echo
echo "== Plugin main class initializes (the failure from the log) =="
run LoadTest "26.1.2.build.72-stable"

echo
echo "== CompMaterial initializes (the failure from latest24.log) =="
#                  Bukkit.getVersion()                  getBukkitVersion()        expected VERSION
run CompMaterialTest "26.1.2-72-1a6b910 (MC: 26.1.2)"    "26.1.2.build.72-stable"  2601
run CompMaterialTest "4416-Spigot-2b0b4fe (MC: 1.21.4)"  "1.21.4-R0.1-SNAPSHOT"    21
run CompMaterialTest "3105-Spigot-8b0b4fe (MC: 1.16.5)"  "1.16.5-R0.1-SNAPSHOT"    16
run CompMaterialTest "unreadable nonsense"               "garbage"                 2601

echo
echo "== NBT-API revision detection =="
run dev.shadmage.eggemall.lib.remain.nbt.NbtVersionTest "26.1.2.build.72-stable" MC1_21_R5
run dev.shadmage.eggemall.lib.remain.nbt.NbtVersionTest "1.21.4-R0.1-SNAPSHOT"   MC1_21_R3
run dev.shadmage.eggemall.lib.remain.nbt.NbtVersionTest "1.20.1-R0.1-SNAPSHOT"   MC1_20_R1

readonly ADMIN_JAR="$ROOT_DIR/EggEmAll2-2.1.1-mc26-admin.jar"

if [ -f "$ADMIN_JAR" ]; then
	echo
	echo "== Administrator build: plugin.yml parsed by Bukkit's own reader =="
	run PluginYmlTest "$ADMIN_JAR" \
		eggemall.admin eggemall.all eggemall.villagers eggemall.aggressive \
		eggemall.passive eggemall.unknown eggemall.command.gui eggemall.command.reload

	echo
	echo "== Administrator build: RequirePermissions is enforced =="
	# The config key must still be read so it survives in settings.yml, but the field
	# must be assigned Boolean.TRUE with no branch in between
	seq="$(javap -c -p -cp "$ADMIN_JAR" 'dev.shadmage.eggemall2.Settings.Settings$Restrictions' 2>/dev/null |
		grep -A5 'String RequirePermissions' | grep -oE 'Boolean\.TRUE|REQUIRE_PERMISSIONS|if[a-z]+|goto' | tr '\n' ' ')"

	if [ "$seq" = "Boolean.TRUE REQUIRE_PERMISSIONS " ]; then
		echo "PASS  REQUIRE_PERMISSIONS assigned Boolean.TRUE unconditionally (no branch)"
	else
		echo "FAIL  unexpected bytecode after reading RequirePermissions: [$seq]"
		failed=1
	fi

	# and the plain compatibility jar must still honour the config
	if javap -c -p -cp "$JAR" 'dev.shadmage.eggemall2.Settings.Settings$Restrictions' 2>/dev/null |
		grep -A5 'String RequirePermissions' | grep -q 'Boolean.TRUE'; then
		echo "FAIL  the compatibility jar should read RequirePermissions from the config"
		failed=1
	else
		echo "PASS  compatibility jar still reads RequirePermissions from the config"
	fi

	echo
	echo "== Administrator build: plugin still initializes =="
	java -cp "$OUT:$ADMIN_JAR:$API_JAR:$GUAVA_JAR:$SNAKEYAML_JAR:$GSON_JAR:$BUNGEE_JAR" LoadTest "26.1.2.build.72-stable" 2>&1 |
		grep -v 'JAVA_TOOL_OPTIONS' || failed=1
fi

echo
if [ "$failed" -eq 0 ]; then
	echo "All checks passed"
else
	echo "Some checks FAILED"
fi

exit "$failed"
