package dev.shadmage.eggemall.lib;

import java.util.Arrays;

import org.bukkit.Bukkit;

/**
 * Detects the Minecraft version this server runs on.
 *
 * Understands both the classic "1.MINOR.PATCH" versions and the calendar based
 * "YEAR.MAJOR.PATCH" scheme Minecraft moved to with 26.1, where the Bukkit
 * version reads i.e. "26.1.2.build.72-stable".
 *
 * Nothing in here throws. This class is initialized from the static initializer
 * of SimplePlugin, so any exception escaping it turns into an
 * ExceptionInInitializerError that stops the whole plugin from loading. A
 * version we cannot read, or a release newer than any we know about, therefore
 * falls back to the newest known version instead.
 */
public final class MinecraftVersion {

	/**
	 * The CraftBukkit package version such as "v1_20_R3", empty on servers that no
	 * longer put the version into the package name.
	 */
	private static String serverVersion;

	/**
	 * The detected version.
	 */
	private static V current;

	/**
	 * The patch number, i.e. 4 in "1.21.4" or 2 in "26.1.2".
	 */
	private static int subversion;

	/**
	 * The version the server reports, i.e. "1.21.4" or "26.1.2".
	 */
	private static String fullVersion;

	public static boolean equals(V version) {
		return compareWith(version) == 0;
	}

	public static boolean olderThan(V version) {
		return compareWith(version) < 0;
	}

	public static boolean newerThan(V version) {
		return compareWith(version) > 0;
	}

	public static boolean atLeast(V version) {
		return equals(version) || newerThan(version);
	}

	private static int compareWith(V version) {
		return (current != null ? current : V.newest()).minorVersionNumber - version.minorVersionNumber;
	}

	public static String getFullVersion() {
		return fullVersion;
	}

	@Deprecated
	public static String getServerVersion() {
		return serverVersion.equals("craftbukkit") ? "" : serverVersion;
	}

	public static V getCurrent() {
		return current;
	}

	public static int getSubversion() {
		return subversion;
	}

	/**
	 * Reads the leading numeric, dot separated parts of the given version.
	 *
	 * "1.21.4" gives 1, 21, 4 and "26.1.2.build.72" gives 26, 1, 2 since we stop at
	 * the first part that is not a number.
	 */
	private static int[] readVersionNumbers(String versionString) {
		final String[] parts = versionString.split("\\.");
		final int[] numbers = new int[parts.length];
		int count = 0;

		for (final String part : parts) {
			if (part.isEmpty())
				break;

			boolean numeric = true;

			for (int i = 0; i < part.length(); i++)
				if (part.charAt(i) < '0' || part.charAt(i) > '9') {
					numeric = false;

					break;
				}

			if (!numeric)
				break;

			try {
				numbers[count] = Integer.parseInt(part);

			} catch (final NumberFormatException ex) {
				break;
			}

			count++;
		}

		return Arrays.copyOf(numbers, count);
	}

	static {
		String packageName = "";
		String bukkitVersion = "";

		try {
			if (Bukkit.getServer() != null) {
				packageName = Bukkit.getServer().getClass().getPackage().getName();
				bukkitVersion = Bukkit.getServer().getBukkitVersion();
			}

		} catch (final Throwable t) {
			// Not running on a server, fall back to the newest version below
		}

		final String packageVersion = packageName.substring(packageName.lastIndexOf('.') + 1);
		serverVersion = !"craftbukkit".equals(packageVersion) && !"".equals(packageName) ? packageVersion : "";

		// "1.21.4-R0.1-SNAPSHOT" gives "1.21.4", "26.1.2.build.72-stable" gives "26.1.2.build.72"
		final String versionString = bukkitVersion.split("\\-")[0];
		final int[] numbers = readVersionNumbers(versionString);

		if (numbers.length < 2) {
			current = V.newest();
			subversion = 0;
			fullVersion = versionString.isEmpty() ? current.toString() : versionString;

			warn("Foundation cannot read Bukkit version '" + bukkitVersion + "', assuming Minecraft " + current + ". Report this if the plugin misbehaves.");

		} else {
			final int major = numbers[0];
			final int minor = numbers[1];
			final int patch = numbers.length > 2 ? numbers[2] : 0;

			// Minecraft 26.1 replaced the "1.MINOR" scheme with "YEAR.MAJOR"
			current = major == 1 ? minor < 3 ? V.v1_3_AND_BELOW : V.parse(minor) : V.parse(V.calendarNumber(major, minor));
			subversion = patch;
			fullVersion = major + "." + minor + (patch > 0 ? "." + patch : "");
		}
	}

	/**
	 * Logs through Bukkit directly. We must not touch Common or Valid here since
	 * they initialize Remain, which reads this class right back while we are still
	 * inside our own static initializer.
	 */
	private static void warn(String message) {
		try {
			Bukkit.getLogger().warning(message);

		} catch (final Throwable t) {
			System.out.println(message);
		}
	}

	public static enum V {
		v26_1(V.calendarNumber(26, 1), "26.1"),
		v1_22(22),
		v1_21(21),
		v1_20(20),
		v1_19(19),
		v1_18(18),
		v1_17(17),
		v1_16(16),
		v1_15(15),
		v1_14(14),
		v1_13(13),
		v1_12(12),
		v1_11(11),
		v1_10(10),
		v1_9(9),
		v1_8(8),
		v1_7(7),
		v1_6(6),
		v1_5(5),
		v1_4(4),
		v1_3_AND_BELOW(3);

		/**
		 * How this version sorts against the others. For "1.MINOR" releases this is
		 * simply the minor number, calendar releases are encoded by
		 * {@link #calendarNumber(int, int)} so that they rank above every "1.MINOR" one.
		 */
		private final int minorVersionNumber;

		private final String displayName;

		private V(int version) {
			this(version, "1." + version);
		}

		private V(int version, String displayName) {
			this.minorVersionNumber = version;
			this.displayName = displayName;
		}

		/**
		 * Encodes a calendar version such as 26.1 into a number that sorts above every
		 * classic "1.MINOR" release.
		 */
		static int calendarNumber(int major, int minor) {
			return major * 100 + minor;
		}

		/**
		 * Returns the version with the given number, or the newest older one when we do
		 * not know it yet, so that a Minecraft release published after this build is
		 * treated as the newest version we know rather than failing.
		 */
		protected static V parse(int number) {
			V closest = null;

			for (final V version : values()) {
				if (version.minorVersionNumber == number)
					return version;

				if (version.minorVersionNumber < number && (closest == null || version.minorVersionNumber > closest.minorVersionNumber))
					closest = version;
			}

			return closest != null ? closest : v1_3_AND_BELOW;
		}

		/**
		 * Returns the newest version known to this build.
		 */
		static V newest() {
			V newest = v1_3_AND_BELOW;

			for (final V version : values())
				if (version.minorVersionNumber > newest.minorVersionNumber)
					newest = version;

			return newest;
		}

		@Override
		public String toString() {
			return this.displayName;
		}
	}
}
