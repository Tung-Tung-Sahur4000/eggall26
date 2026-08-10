#!/usr/bin/env bash
#
# Rebuilds EggEmAll2 with the patched MinecraftVersion class.
#
# The plugin ships as a compiled jar without sources, so the fix is applied by
# recompiling the single patched class against the jar itself and replacing it
# inside a copy of the jar. Everything else in the jar is untouched.
#
set -euo pipefail

readonly PATCH_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly ROOT_DIR="$(dirname "$PATCH_DIR")"

readonly INPUT_JAR="${1:-$ROOT_DIR/EggEmAll2-2.1.1.jar}"
readonly OUTPUT_JAR="${2:-$ROOT_DIR/EggEmAll2-2.1.1-mc26-fix.jar}"

# Second output: the same jar with every permission declared default: op
readonly ADMIN_JAR="${3:-$ROOT_DIR/EggEmAll2-2.1.1-mc26-admin.jar}"

readonly BUILD_DIR="$PATCH_DIR/.build"
readonly CLASSES_DIR="$BUILD_DIR/classes"

# We only compile against org.bukkit.Bukkit, so any modern Bukkit API works.
# Spigot rather than Paper because Paper's Server interface pulls in Adventure.
readonly API_JAR="$BUILD_DIR/spigot-api.jar"
readonly API_BASE="https://hub.spigotmc.org/nexus/content/repositories/snapshots/org/spigotmc/spigot-api/1.21.4-R0.1-SNAPSHOT"

mkdir -p "$CLASSES_DIR"

if [ ! -f "$API_JAR" ]; then
	echo "==> Downloading Bukkit API to compile against"

	snapshot="$(curl -sSf "$API_BASE/maven-metadata.xml" | grep -o '<value>[^<]*</value>' | head -1 | sed 's/<[^>]*>//g')"
	curl -sSf -o "$API_JAR" "$API_BASE/spigot-api-${snapshot}.jar"
fi

echo "==> Compiling patched classes"
rm -rf "$CLASSES_DIR"
mkdir -p "$CLASSES_DIR"

# Java 8 bytecode to match the rest of the jar
find "$PATCH_DIR/src" -name '*.java' -print0 | xargs -0 \
	javac -nowarn -encoding UTF-8 --release 8 \
		-cp "$INPUT_JAR:$API_JAR" \
		-d "$CLASSES_DIR"

echo "==> Writing $OUTPUT_JAR"
cp "$INPUT_JAR" "$OUTPUT_JAR"

# Replace every recompiled class, including the synthetic inner ones
(cd "$CLASSES_DIR" && find . -name '*.class' -printf '%P\n' | sort) > "$BUILD_DIR/classlist.txt"
jar uf "$OUTPUT_JAR" -C "$CLASSES_DIR" @"$BUILD_DIR/classlist.txt" 2>/dev/null ||
	while read -r entry; do
		jar uf "$OUTPUT_JAR" -C "$CLASSES_DIR" "$entry"
	done < "$BUILD_DIR/classlist.txt"

echo "==> Replaced:"
sed 's/^/    /' "$BUILD_DIR/classlist.txt"

echo "==> Compiling administrator overrides"
readonly ADMIN_CLASSES_DIR="$BUILD_DIR/admin-classes"
rm -rf "$ADMIN_CLASSES_DIR"
mkdir -p "$ADMIN_CLASSES_DIR"

# Compiled against the fix jar so the overrides see the already patched classes
find "$PATCH_DIR/admin/src" -name '*.java' -print0 | xargs -0 \
	javac -nowarn -encoding UTF-8 --release 8 \
		-cp "$OUTPUT_JAR:$API_JAR" \
		-d "$ADMIN_CLASSES_DIR"

echo "==> Writing $ADMIN_JAR"
cp "$OUTPUT_JAR" "$ADMIN_JAR"
jar uf "$ADMIN_JAR" -C "$PATCH_DIR/admin" plugin.yml
jar uf "$ADMIN_JAR" -C "$PATCH_DIR/admin" settings.yml

(cd "$ADMIN_CLASSES_DIR" && find . -name '*.class' -printf '%P\n' | sort) > "$BUILD_DIR/admin-classlist.txt"
while read -r entry; do
	jar uf "$ADMIN_JAR" -C "$ADMIN_CLASSES_DIR" "$entry"
done < "$BUILD_DIR/admin-classlist.txt"

echo "==> Replaced:"
echo "    plugin.yml"
echo "    settings.yml"
sed 's/^/    /' "$BUILD_DIR/admin-classlist.txt"
