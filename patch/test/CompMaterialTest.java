import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;

import dev.shadmage.eggemall.lib.remain.CompMaterial;

/**
 * Initializes CompMaterial against a given server version.
 *
 * This is the failure from latest24.log: CompMaterial's version holder did
 * Integer.parseInt(major.substring(2)), which on "26.1" leaves ".1". The
 * NumberFormatException escaped a static initializer, so CompMaterial stayed
 * permanently unusable and every later use threw NoClassDefFoundError.
 *
 * Initializing CompMaterial pulls in Remain, which prints a
 * "Could not find class: org.bukkit.craftbukkit.proxy1.entity.CraftPlayer" trace
 * here. That is this harness, not a fault: the mock server is a java.lang.reflect
 * proxy living in package jdk.proxy1, so Foundation derives "proxy1" where a real
 * server has no package version at all. Remain handles it the same way it handles
 * Mojang mappings on a real server - it logs and carries on.
 *
 * Usage: java CompMaterialTest "<Bukkit.getVersion()>" "<getBukkitVersion()>" <expected VERSION>
 */
public class CompMaterialTest {

	public static void main(String[] args) {
		final String serverVersion = args[0];
		final String bukkitVersion = args[1];
		final int expectedVersion = Integer.parseInt(args[2]);

		Bukkit.setServer(mockServer(serverVersion, bukkitVersion));

		final java.io.PrintStream out = System.out;

		try {
			// The class load that used to die
			Class.forName("dev.shadmage.eggemall.lib.remain.CompMaterial", true, CompMaterialTest.class.getClassLoader());

			final int version = readInt("VERSION");
			final boolean isFlat = readBoolean("ISFLAT");

			// What EggListener actually calls when a player right clicks with an egg
			final boolean spawnEgg = CompMaterial.isMonsterEgg(Material.VILLAGER_SPAWN_EGG);
			final boolean notSpawnEgg = CompMaterial.isMonsterEgg(Material.STONE);
			final CompMaterial egg = CompMaterial.fromMaterial(Material.EGG);

			System.setOut(out);

			final boolean ok = version == expectedVersion && isFlat && spawnEgg && !notSpawnEgg && egg == CompMaterial.EGG;

			out.println((ok ? "PASS" : "FAIL") + "  " + pad(serverVersion)
					+ " VERSION=" + version + " ISFLAT=" + isFlat
					+ " isMonsterEgg(VILLAGER_SPAWN_EGG)=" + spawnEgg
					+ " isMonsterEgg(STONE)=" + notSpawnEgg
					+ " fromMaterial(EGG)=" + egg);

			if (!ok) {
				out.println("      expected VERSION=" + expectedVersion + ", ISFLAT=true, spawn egg true, stone false, EGG");

				System.exit(1);
			}

		} catch (final Throwable t) {
			System.setOut(out);
			out.println("FAIL  CompMaterial unusable on " + serverVersion);

			Throwable cause = t;

			while (cause != null) {
				out.println("      " + cause);

				cause = cause.getCause();
			}

			System.exit(1);
		}
	}

	private static int readInt(String field) throws Exception {
		return (Integer) lookup(field).get(null);
	}

	private static boolean readBoolean(String field) throws Exception {
		return (Boolean) lookup(field).get(null);
	}

	private static Field lookup(String name) throws Exception {
		final Class<?> data = Class.forName("dev.shadmage.eggemall.lib.remain.CompMaterial$Data", true,
				CompMaterialTest.class.getClassLoader());

		final Field field = data.getDeclaredField(name);
		field.setAccessible(true);

		return field;
	}

	private static String pad(String text) {
		final StringBuilder builder = new StringBuilder(text);

		while (builder.length() < 34)
			builder.append(' ');

		return builder.toString();
	}

	private static Server mockServer(String serverVersion, String bukkitVersion) {
		final InvocationHandler handler = new InvocationHandler() {

			@Override
			public Object invoke(Object proxy, Method method, Object[] methodArgs) {
				switch (method.getName()) {
					case "getVersion":
						return serverVersion;
					case "getBukkitVersion":
						return bukkitVersion;
					case "getName":
						return "Paper";
					case "getLogger":
						return java.util.logging.Logger.getLogger("Minecraft");
					case "getPluginManager":
						return mockPluginManager();
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

		return (Server) Proxy.newProxyInstance(CompMaterialTest.class.getClassLoader(), new Class[] { Server.class }, handler);
	}

	private static Object mockPluginManager() {
		final InvocationHandler handler = new InvocationHandler() {

			@Override
			public Object invoke(Object proxy, Method method, Object[] methodArgs) {
				if ("getPlugins".equals(method.getName()))
					return new org.bukkit.plugin.Plugin[0];

				if ("hashCode".equals(method.getName()))
					return 0;

				if ("equals".equals(method.getName()))
					return proxy == methodArgs[0];

				if ("toString".equals(method.getName()))
					return "MockPluginManager";

				return null;
			}
		};

		return Proxy.newProxyInstance(CompMaterialTest.class.getClassLoader(),
				new Class[] { org.bukkit.plugin.PluginManager.class }, handler);
	}
}
