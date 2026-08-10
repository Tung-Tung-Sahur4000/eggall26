package dev.shadmage.eggemall.lib.remain.nbt;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.bukkit.Bukkit;
import org.bukkit.Server;

/**
 * Checks which revision the bundled NBT-API resolves a Bukkit version to. Lives
 * in the NBT-API package because that enum is package private.
 *
 * Usage: java dev.shadmage.eggemall.lib.remain.nbt.NbtVersionTest "<bukkit version>" "<expected revision>"
 */
public class NbtVersionTest {

	public static void main(String[] args) {
		final String bukkitVersion = args[0];

		Bukkit.setServer(mockServer(bukkitVersion));

		final String actual = MinecraftVersion.getVersion().name();
		final boolean ok = args[1].equals(actual);

		System.out.println((ok ? "PASS" : "FAIL") + "  " + bukkitVersion + " -> " + actual
				+ " | mojangMapping=" + MinecraftVersion.getVersion().isMojangMapping()
				+ " | mappings=" + (MojangToMapping.getMapping() != null ? "resolved" : "null"));

		if (!ok) {
			System.out.println("      expected: " + args[1]);

			System.exit(1);
		}
	}

	private static Server mockServer(String bukkitVersion) {
		final InvocationHandler handler = new InvocationHandler() {

			@Override
			public Object invoke(Object proxy, Method method, Object[] methodArgs) {
				switch (method.getName()) {
					case "getBukkitVersion":
					case "getVersion":
						return bukkitVersion;
					case "getName":
						return "Paper";
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

		return (Server) Proxy.newProxyInstance(NbtVersionTest.class.getClassLoader(), new Class[] { Server.class }, handler);
	}
}
