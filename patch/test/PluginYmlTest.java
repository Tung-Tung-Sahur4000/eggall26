import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginDescriptionFile;

/**
 * Parses plugin.yml straight out of a jar with Bukkit's own reader, the same one
 * the server uses at load time, and checks that every declared permission
 * defaults to op.
 *
 * Usage: java PluginYmlTest <jar> [expected permission ...]
 */
public class PluginYmlTest {

	public static void main(String[] args) throws Exception {
		final String jarPath = args[0];

		final PluginDescriptionFile description;

		try (JarFile jar = new JarFile(jarPath); InputStream in = jar.getInputStream(jar.getEntry("plugin.yml"))) {
			description = new PluginDescriptionFile(in);
		}

		final List<String> declared = new ArrayList<>();
		final List<String> notOp = new ArrayList<>();

		for (final Permission permission : description.getPermissions()) {
			declared.add(permission.getName());

			if (permission.getDefault() != PermissionDefault.OP)
				notOp.add(permission.getName() + "=" + permission.getDefault());
		}

		System.out.println("      " + description.getName() + " v" + description.getVersion()
				+ ", api-version " + description.getAPIVersion()
				+ ", main " + description.getMain());

		for (final Permission permission : description.getPermissions())
			System.out.println("      " + pad(permission.getName()) + " default=" + permission.getDefault()
					+ (permission.getChildren().isEmpty() ? "" : " children=" + permission.getChildren().keySet()));

		final List<String> missing = new ArrayList<>();

		for (int i = 1; i < args.length; i++)
			if (!declared.contains(args[i]))
				missing.add(args[i]);

		if (!notOp.isEmpty()) {
			System.out.println("FAIL  permissions not defaulting to op: " + notOp);

			System.exit(1);
		}

		if (!missing.isEmpty()) {
			System.out.println("FAIL  permissions missing from plugin.yml: " + missing);

			System.exit(1);
		}

		System.out.println("PASS  " + declared.size() + " permissions declared, all default=OP");
	}

	private static String pad(String text) {
		final StringBuilder builder = new StringBuilder(text);

		while (builder.length() < 26)
			builder.append(' ');

		return builder.toString();
	}
}
