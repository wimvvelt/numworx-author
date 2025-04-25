package fi.microserver.microboot;

import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

public class JULHandler extends Handler {

	/*
	 * static class LogReference implements ServiceReference<Object> {
	 * 
	 * final LogRecord record; final Bundle bundle; LogReference(LogRecord record,
	 * Bundle bundle) { this.record = record; this.bundle = bundle; }
	 * 
	 * final static String OBJECT_CLASS = "objectClass"; final static String[] keys
	 * = { OBJECT_CLASS };
	 * 
	 * @Override public Object getProperty(String key) { if (
	 * OBJECT_CLASS.equals(key)) return new String[] { record.getSourceClassName()
	 * }; return null; }
	 * 
	 * @Override public String[] getPropertyKeys() { return keys; }
	 * 
	 * @Override public Bundle getBundle() { return bundle; }
	 * 
	 * @Override public Bundle[] getUsingBundles() { return null; }
	 * 
	 * @Override public boolean isAssignableTo(Bundle bundle, String className) {
	 * return false; }
	 * 
	 * @Override public int compareTo(Object reference) { // TODO Auto-generated
	 * method stub return 0; }
	 * 
	 * @Override public String toString() { return "[" + record.getSourceClassName()
	 * + "]"; }
	 * 
	 *//**
		 * @since 1.9
		 *//*
			 * public Dictionary<String,Object> getProperties() { Dictionary<String,Object>
			 * result = new Hashtable<>(); for(String key: getPropertyKeys()) {
			 * result.put(key, getProperty(key)); } return result; }
			 * 
			 * @Override public <A> A adapt(Class<A> type) { return null; } }
			 */

	private ServiceTracker<LogService, LogService> tracker;
	private SecurityManagerEx securityManager = new SecurityManagerEx();
	private Handler[] orgs; // original handlers.

	void uninstall() {
		Logger root = Logger.getLogger("");
		Handler[] old = root.getHandlers();
		if (old != null)
			for (Handler o : old)
				root.removeHandler(o);
		root.setFilter(null);
		if (orgs != null)
			for (Handler o : orgs) {
				root.addHandler(o);
			}

	}

	static JULHandler install(BundleContext context) throws SecurityException, IOException {
		// String properties =
		// "handlers= "
		// + JULHandler.class.getName()
		// + "\n" +
		// ".level= FINEST\n" +
		// "";
		// ByteArrayInputStream ins = new ByteArrayInputStream(properties.getBytes());
		// LogManager.getLogManager().readConfiguration(ins);
		Logger root = Logger.getLogger("");
		Handler[] old = root.getHandlers();
		if (old != null)
			for (Handler o : old)
				root.removeHandler(o);
		JULHandler jul = new JULHandler(context);
		root.addHandler(jul);
		jul.orgs = old;
		root.setLevel(Level.INFO); // anders NPE in wiskOpdr .... toString();
		root.setFilter(JULHandler::classFilter);
		return jul;
	}

	static boolean classFilter(LogRecord record) {
		String src = record.getSourceClassName();
		if (src == null)
			return true;
		return !(src.startsWith("java.awt") || src.startsWith("sun.") || src.startsWith("javax.swing"));
	}

	
	private Bundle root;
	JULHandler(BundleContext context) {
		root = context.getBundle();
		tracker = new ServiceTracker<LogService, LogService>(context, LogService.class, null);
		tracker.open();
		setFormatter(new SimpleFormatter());
		setFilter(JULHandler::classFilter);
	}

	@Override
	public void publish(LogRecord record) {
		if (!isLoggable(record))
			return;
		LogService service = tracker.getService();

		int level = record.getLevel().intValue();
		String message;
		message = getFormatter().formatMessage(record);
		LogLevel lvl = LogLevel.TRACE;
		if (service != null) {

			if (level >= Level.SEVERE.intValue())
				lvl = LogLevel.ERROR;
			else if (level >= Level.WARNING.intValue())
				lvl = LogLevel.WARN;
			else if (level >= Level.INFO.intValue())
				lvl = LogLevel.INFO;
			else if (level >= Level.FINE.intValue())
				lvl = LogLevel.DEBUG;
			org.osgi.service.log.Logger lg = service.getLogger(getCallerBundle(), record.getLoggerName(),
					org.osgi.service.log.Logger.class);
			Throwable exception = record.getThrown();
			switch (lvl) {
			case ERROR:
				lg.error(message, exception);
				return;
			case WARN:
				lg.warn(message, exception);
				return;
			case INFO:
				lg.info(message, exception);
				return;
			case DEBUG:
				lg.debug(message, exception);
				return;
			case TRACE:
				lg.trace(message, exception);
				return;
			case AUDIT:
				lg.audit(message, exception);
				return;
			}

		} else {
			System.err.println(message);
		}
	}

	@Override
	public void flush() {
	}

	@Override
	public void close() throws SecurityException {
		tracker.close();
	}

	private Bundle getCallerBundle() {
		Bundle ret = root;
		Class[] classCtx = securityManager.getClassContext();
		for (int i = 0; i < classCtx.length; i++) {
			Bundle bi = FrameworkUtil.getBundle(classCtx[i]);
			if (bi != null) ret = bi;
			if (!classCtx[i].getName().startsWith("fi.microserver")
					&& !classCtx[i].getName().startsWith("java.util.logging") ) {
				break;
			}
		}
		return ret; // never null
	}

	static class SecurityManagerEx extends SecurityManager {
		public Class[] getClassContext() {
			return super.getClassContext();
		}
	}

}
