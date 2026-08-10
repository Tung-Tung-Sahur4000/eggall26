import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.bukkit.Bukkit;
import org.bukkit.Server;

import dev.shadmage.eggemall.lib.MinecraftVersion;
import dev.shadmage.eggemall.lib.MinecraftVersion.V;

/**
 * Feeds one Bukkit version string into MinecraftVersion and prints what it
 * detected. MinecraftVersion reads the version in a static initializer, so this
 * runs in a fresh JVM per case.
 *
 * Usage: java VersionTest "<bukkit version>" "<expected V>" <expected subversion> "<expected full version>"
 */
public class VersionTest {

	public static void main(String[] args) {
		final String bukkitVersion = args[0];

		Bukkit.setServer(mockServer(bukkitVersion));

		final String actual = MinecraftVersion.getCurrent().name()
				+ " | sub=" + MinecraftVersion.getSubversion()
				+ " | full=" + MinecraftVersion.getFullVersion();

		final String expected = args[1] + " | sub=" + args[2] + " | full=" + args[3];

		// The version gates the plugin actually relies on
		final String gates = "atLeast(1.13)=" + MinecraftVersion.atLeast(V.v1_13)
				+ " atLeast(1.16)=" + MinecraftVersion.atLeast(V.v1_16)
				+ " atLeast(1.17)=" + MinecraftVersion.atLeast(V.v1_17)
				+ " olderThan(1.9)=" + MinecraftVersion.olderThan(V.v1_9);

		final boolean ok = expected.equals(actual);

		System.out.println((ok ? "PASS" : "FAIL") + "  " + pad(bukkitVersion) + " -> " + actual + "   " + gates);

		if (!ok) {
			System.out.println("      expected: " + expected);

			System.exit(1);
		}
	}

	private static String pad(String text) {
		final StringBuilder builder = new StringBuilder(text);

		while (builder.length() < 26)
			builder.append(' ');

		return builder.toString();
	}

	private static Server mockServer(String bukkitVersion) {
		final InvocationHandler handler = new InvocationHandler() {

			@Override
			public Object invoke(Object proxy, Method method, Object[] methodArgs) {
				switch (method.getName()) {
					case "getBukkitVersion":
						return bukkitVersion;
					case "getName":
						return "Paper";
					case "getVersion":
						return bukkitVersion;
					case "getLogger":
						return java.util.logging.Logger.getLogger("Minecraft");
					case "toString":
						return "MockServer";
					case "hashCode":
						return 0;
					case "equals":
						return proxy == methodArgs[0];
				}

				return null;
			}
		};

		return (Server) Proxy.newProxyInstance(VersionTest.class.getClassLoader(), new Class[] { Server.class }, handler);
	}
}
