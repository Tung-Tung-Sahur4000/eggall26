import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.bukkit.Bukkit;
import org.bukkit.Server;

/**
 * Reproduces the class load that failed on the server: Bukkit's PluginClassLoader
 * calls Class.forName on the plugin main class, which runs SimplePlugin's static
 * initializer and, through it, MinecraftVersion's.
 *
 * On the unpatched jar this dies with ExceptionInInitializerError caused by
 * "Foundation cannot read Bukkit version".
 *
 * Usage: java LoadTest "<bukkit version>"
 */
public class LoadTest {

	private static final String MAIN_CLASS = "dev.shadmage.eggemall2.EggEmAllPlugin";

	public static void main(String[] args) {
		final String bukkitVersion = args[0];

		Bukkit.setServer(mockServer(bukkitVersion));

		// Foundation swaps System.out for its own filter while initializing
		final java.io.PrintStream out = System.out;

		try {
			Class.forName(MAIN_CLASS, true, LoadTest.class.getClassLoader());

			System.setOut(out);
			out.println("PASS  " + MAIN_CLASS + " initialized on Bukkit " + bukkitVersion);

		} catch (final Throwable t) {
			System.setOut(out);
			out.println("FAIL  " + MAIN_CLASS + " failed to initialize on Bukkit " + bukkitVersion);

			Throwable cause = t;

			while (cause != null) {
				out.println("      " + cause);

				cause = cause.getCause();
			}

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

		return (Server) Proxy.newProxyInstance(LoadTest.class.getClassLoader(), new Class[] { Server.class }, handler);
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

		return Proxy.newProxyInstance(LoadTest.class.getClassLoader(), new Class[] { org.bukkit.plugin.PluginManager.class }, handler);
	}
}
