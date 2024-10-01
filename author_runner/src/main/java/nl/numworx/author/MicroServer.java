package nl.numworx.author;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.service.provisioning.ProvisioningService;
import org.osgi.util.tracker.ServiceTracker;

import it.sauronsoftware.junique.AlreadyLockedException;
import it.sauronsoftware.junique.JUnique;
import it.sauronsoftware.junique.MessageHandler;

public class MicroServer {

	private static Framework framework;
	private static BundleContext context;
	// Whether Windows/Mac
	static boolean isWindows = (System.getProperty("os.name").indexOf("Windows") >= 0);
	static boolean isMac = (System.getProperty("os.name").indexOf("Mac OS X") >= 0);

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void main(String[] args) throws Exception {
	    System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");
	    System.setProperty("java.security.policy","all.policy");
	    
		Preferences pref = Preferences.userRoot().node("fi/microserver");
		String uuid = pref.get("uuid", null);
		String clean = pref.get(Constants.FRAMEWORK_STORAGE_CLEAN, "");
		if(uuid == null) {
			uuid = UUID.randomUUID().toString();
			pref.put("uuid", uuid);
			try {
				pref.flush(); // Jammer als niet werkt, maar niet een showstopper
			} catch (Exception e) {
			}
		}
		List<String> arglist = new ArrayList<String>(Arrays.asList(args));
		FrameworkFactory factory = ServiceLoader.load(FrameworkFactory.class).iterator().next();
		Map<String, String> map = new HashMap<String,String>();
	    if(arglist.remove("-clean"))
	    	clean = Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT;
	    if (clean != null)
	      map.put(Constants.FRAMEWORK_STORAGE_CLEAN,clean);
	    map.put("fi.dwo.console", Boolean.valueOf(arglist.remove("-console")).toString());
	    map.put("fi.dwo.uuid", uuid);
	    String dir = System.getProperty("user.home");
	    if(isWindows) dir += File.separator + "AppData" + File.separator + "Local";
	    else if(isMac) dir += File.separator + "Library" + File.separator + "Application Support";

		String system = "javafx.application,javafx.beans.property,javafx.beans.value,javafx.collections,javafx.concurrent,javafx.embed.swing,javafx.event,javafx.scene,javafx.scene.control,javafx.scene.web,javafx.util,javax.swing,javax.swing.border,netscape.javascript," + 
						"com.apple.eawt,";
		String version = System.getProperty("java.version", "0.0.0");
		
		int javaVersion = Integer.parseInt(version.split("\\.")[0]);
		if ( javaVersion >= 11)
			system = "sun.misc,";
		else
		if ( javaVersion >= 9)
			system = "netscape.javascript,"; // vanaf 9 geen netscape automatisch (felix 5.6.10 defaults.properties)
		Properties props = new Properties();
		InputStream in = MicroServer.class.getResourceAsStream("resources/DWO.properties");
		props.load(in);
		in.close();
		
		
		map.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, 
// Java 8,9 en 10, 11 alleen met een eigen java
		  system
				+ "org.osgi.service.provisioning;version=\"1.2.0\""
				+ ",it.sauronsoftware.junique;version=\"1.0.4\""
			);
		map.putAll((Map)props);
		String target = props.getProperty("fi.dwo.target", "DWO-docent");

		try {
          JUnique.acquireLock(target, MicroServer::handleMessage);
        } catch (AlreadyLockedException e1) {
          JUnique.sendMessage(target, Arrays.toString(args));
          throw e1;
        }
		
		map.put(Constants.FRAMEWORK_STORAGE, dir + File.separator + target + "-cache");
		
		if("true".equals(props.getProperty("fi.dwo.properties")))
			map.put("fi.dwo.documentbase", MicroServer.class.getResource("resources/").toExternalForm());
		else 
			map.remove("fi.dwo.properties");
		URL resource = MicroServer.class.getResource("/null.zip");
		String null_zip = resource == null ? "https://cdn.dwo.nl/bundles/null.zip" : resource.toExternalForm();
        map.put(ProvisioningService.PROVISIONING_REFERENCE, null_zip);
		if (!arglist.isEmpty()) {
		  File f = new File(arglist.get(0));
		  if ( f.isFile()) {
		    map.put("fi.dwo.provisioning", f.toURI().toString());
		  }
		}
		
		cleanOnExit(true);
		FelixLogger logger = new FelixLogger();

		Map m = map;
		m.put(FelixConstants.LOG_LOGGER_PROP, logger); // Must be Felix

		framework = factory.newFramework(map);
		framework.init();
		context = framework.getBundleContext();
		ServiceTracker tracker = new LoggerTracker(context,logger);
		tracker.open();
		try {
		    Provisioning.install(context);
			int type;
			do { 
				framework.start();
				FrameworkEvent event = framework.waitForStop(0);
				type  = event.getType();
			} while (type != FrameworkEvent.STOPPED);
			cleanOnExit(false);
		} catch (InterruptedException e) {
		} catch (Throwable t) {
			displayException(t);			
		}		
		finally {
			System.exit(0);
		}	
	}

  private static void displayException(final Throwable t) {
		try {
			Runnable run = 
			new Runnable() {
				public void run() {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					t.printStackTrace(pw);
					pw.close();
					JTextArea area = new JTextArea(sw.toString());
					JOptionPane.showMessageDialog(null, new JScrollPane(area));
				}
			};
			if(SwingUtilities.isEventDispatchThread()) {
			  run.run();
			} else {
			  SwingUtilities.invokeAndWait(run);
			}
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void cleanOnExit(boolean on) {
		try {
			Preferences pref = Preferences.userRoot().node("fi/microserver");
			if(on)
				pref.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
			else
				pref.remove(Constants.FRAMEWORK_STORAGE_CLEAN);
			pref.flush();
		} catch (BackingStoreException e) {
		}
		
	}

	private static String handleMessage(String message) {
	  if (context != null) {
	    ServiceReference<MessageHandler> ref = context.getServiceReference(MessageHandler.class);
	    if (ref != null) {
	      MessageHandler handler = context.getService(ref);
	      if (handler != null) {
	        try { 
	          message = handler.handle(message);
	        } catch(Throwable t) {}
	      }
          context.ungetService(ref);
	    }
	  }
	  return message;
	}
	
//	static public Set<String> findPackageNames(String prefix) {
//		
//     // DOES NOT WORK!		
//		Package[] packages = Package.getPackages();
//	 // DOES NOT WORK either
//		packages = MicroServer.class.getClassLoader().getDefinedPackages();
//	 // same 10 names	
//		Object[] names = MicroServer.class.getClassLoader().getUnnamedModule().getPackages().toArray();
//		return Arrays.asList(packages).stream()
//	        .map(Package::getName)
//	        .filter(
//	        		n -> 
//	        		n.startsWith(prefix) && !n.contains("internal")
//	        )
//	        .collect(Collectors.toCollection(TreeSet::new));
//	}
}
