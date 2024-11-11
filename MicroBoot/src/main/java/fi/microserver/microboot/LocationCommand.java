package fi.microserver.microboot;

import java.io.PrintStream;
import java.util.Date;

import org.eclipse.concierge.shell.commands.ShellCommandGroup;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class LocationCommand implements ShellCommandGroup {

	private final BundleContext context;
	private final PrintStream out;

	public LocationCommand(BundleContext context, PrintStream out) {
		this.context = context;
		this.out = out;
	}

	@Override
	public String getHelp() {
		return "numworx.location -- display location of bundles";
	}

	@Override
	public String getGroup() {
		return "numworx";
	}

	@Override
	public void handleCommand(String command, String[] args) throws Exception {
		Bundle[] bundles = context.getBundles();
		out.println("Locations:");
		for (Bundle b: bundles) {
			out.println(b.getBundleId() + ": " + b.getSymbolicName() + "/" + b.getVersion() + " " + b.getLocation() + " " + new Date(b.getLastModified()));
		}
		out.println();
		
	}

}
