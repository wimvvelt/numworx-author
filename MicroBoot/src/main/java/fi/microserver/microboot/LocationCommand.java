package fi.microserver.microboot;

import java.io.PrintStream;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.Stack;

import org.eclipse.concierge.shell.commands.ShellCommandGroup;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;

public class LocationCommand implements ShellCommandGroup {

	private final BundleContext context;
	private final PrintStream out;

	public LocationCommand(BundleContext context, PrintStream out) {
		this.context = context;
		this.out = out;
	}

	@Override
	public String getHelp() {
		return "numworx.location -- display location of bundles\n"
				+ "numworx.log [n] -- display log, [n] entries";
	}

	@Override
	public String getGroup() {
		return "numworx";
	}

	@Override
	public void handleCommand(String command, String[] args) throws Exception {
		if ("log".equals(command)) {
			handleLog(args); return;
		}
		handleLocation();
		
	}

	private void handleLocation() {
		Bundle[] bundles = context.getBundles();
		out.println("Locations:");
		for (Bundle b: bundles) {
			out.println(b.getBundleId() + ": " + b.getSymbolicName() + "/" + b.getVersion() + " " + b.getLocation() + " " + Instant.ofEpochMilli(b.getLastModified()));
		}
		out.println();
	}
	
	private void handleLog(String[] args) {
		int cnt = Short.MAX_VALUE;
		if (args != null && args.length > 0) {
			try {
				cnt = Integer.parseInt(args[0]);
			} catch(Exception oops) {}
		}
		ServiceReference<LogReaderService> ref = context.getServiceReference(LogReaderService.class);
		if (ref != null) {
			LogReaderService service = context.getService(ref);
			if (service != null) {
				Enumeration<LogEntry> list = service.getLog();
				context.ungetService(ref);
				if (list.hasMoreElements()) out.println("Log:");
				Stack<LogEntry> stack = new Stack<>();
				while (list.hasMoreElements()) {
					LogEntry logEntry = (LogEntry) list.nextElement();
					if (!logEntry.getLoggerName().startsWith("Events."))
					{
						stack.add(logEntry);	
						if (cnt -- <= 1) break;
					}
				}
				while(!stack.isEmpty()) println(stack.pop());
			}
		}
	}

	private void println(LogEntry entry) {
		//out.println(entry);
		Instant instant  = Instant.ofEpochMilli(entry.getTime());
		LocalTime time = LocalTime.ofInstant(instant, ZoneId.systemDefault());
		out.print((time));
		out.print(" ");
		out.print(entry.getLogLevel().toString());
		out.print(" [");
		out.print(entry.getBundle().getBundleId());
		out.print("] ");
		out.print(entry.getLocation().getClassName());
		out.print(":");
		out.print(entry.getLocation().getLineNumber());
		out.print(" ");
		out.println(entry.getMessage());
		Throwable t = entry.getException();
		if (t != null) t.printStackTrace(out);		
	}

}
