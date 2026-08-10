package dev.shadmage.eggemall.lib.remain;

import org.bukkit.Bukkit;

/**
 * Replacement for the version holder nested inside CompMaterial.
 *
 * The original read the server version and stripped the leading "1." with a fixed
 * substring(2):
 *
 *     VERSION = Integer.parseInt(getMajorVersion(Bukkit.getVersion()).substring(2));
 *
 * On Minecraft 26.1 that leaves ".1", so the initializer died with
 * NumberFormatException. Because it dies in a static initializer, CompMaterial is
 * left permanently unusable and every later touch throws
 * "NoClassDefFoundError: Could not initialize class CompMaterial" - which takes the
 * whole plugin down, since CompMaterial backs the menus, the item builder and the
 * spawn egg handling.
 *
 * This is compiled as a top level class with a '$' in its name so that it replaces
 * CompMaterial$Data on its own. CompMaterial itself, and in particular its table of
 * roughly 1500 material constants, is left exactly as the author shipped it.
 * CompMaterial reaches this class only through access$000 and access$100, so those
 * two methods and their signatures have to stay as they are.
 */
final class CompMaterial$Data {

	/**
	 * How the running version compares to the "1.MINOR" releases this library was
	 * written against: 21 for 1.21, and for the calendar versions from 26.1 onwards
	 * YEAR * 100 + MAJOR, which ranks above every 1.x release. The same encoding as
	 * MinecraftVersion.V - keep the two in step.
	 */
	private static final int VERSION;

	/**
	 * Whether the server uses the flattened material names, i.e. 1.13 and up.
	 */
	private static final boolean ISFLAT;

	private CompMaterial$Data() {
	}

	static boolean access$000() {
		return ISFLAT;
	}

	static int access$100() {
		return VERSION;
	}

	static {
		VERSION = readVersion();
		ISFLAT = VERSION >= 13;
	}

	private static int readVersion() {
		try {
			final String major = majorVersion(Bukkit.getVersion());
			final int dot = major.indexOf('.');

			if (dot > 0) {
				final int nextDot = major.indexOf('.', dot + 1);

				final int first = Integer.parseInt(major.substring(0, dot));
				final int second = Integer.parseInt(nextDot == -1 ? major.substring(dot + 1) : major.substring(dot + 1, nextDot));

				return first == 1 ? second : first * 100 + second;
			}

		} catch (final Throwable t) {
			// Fall through to the assumption below
		}

		// Nothing may be thrown from here: this runs in a static initializer, and an
		// exception leaves CompMaterial unusable for the rest of the server's life.
		// A version we cannot read is far more likely to be newer than older.
		return 2601;
	}

	/**
	 * Trims the server version down to its major part, i.e. "1.21" or "26.1". Kept
	 * identical to CompMaterial's own getMajorVersion, which cannot be called from
	 * here because it is only reachable through a synthetic accessor.
	 */
	private static String majorVersion(String version) {
		int index = version.lastIndexOf("MC:");

		if (index != -1)
			version = version.substring(index + 4, version.length() - 1);

		else if (version.endsWith("SNAPSHOT")) {
			index = version.indexOf('-');
			version = version.substring(0, index);
		}

		final int lastDot = version.lastIndexOf('.');

		if (version.indexOf('.') != lastDot)
			version = version.substring(0, lastDot);

		return version;
	}
}
