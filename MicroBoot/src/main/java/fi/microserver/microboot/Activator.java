package fi.microserver.microboot;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogService;
import org.osgi.service.log.admin.LoggerAdmin;
import org.osgi.service.log.admin.LoggerContext;
import org.osgi.service.provisioning.ProvisioningService;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

  
  private final Logger LOG = Logger.getLogger(getClass().getName());
  BundleContext context;
  private StartBoot boot;
  private JULHandler jul;
  private static final String LOCATION_PREFIX = "provisioning:";

  @Override
  public void start(BundleContext context) throws Exception {
// VERSION CHECK
    String v = context.getProperty("fi.microserver.version");
    Version v1 = new Version(v);
    Version v2 = new Version("0.0.7");
    if (v1.compareTo(v2) < 0)
      return; 
      
    this.context = context;
    startLog();

    jul = JULHandler.install(context);
    installEvent();
// start all "provision" bundles except me.
    Bundle[] bundles = context.getBundles();
    boot = new StartBoot(context);
    Runnable go = (new Runnable() {
      public void run() {
        for (int i = 1; i < bundles.length; i++) {
          Bundle b = bundles[i];
          if (b == context.getBundle()) continue;
          String loc = b.getLocation();
          int state = b.getState();
          String fragment = b.getHeaders().get(Constants.FRAGMENT_HOST);
          if (loc.startsWith(LOCATION_PREFIX) && fragment == null
              && (state == Bundle.INSTALLED || state == Bundle.RESOLVED))
            try {
              b.start();
            } catch (Exception e) {
            LOG.log(Level.WARNING, "starting " + loc, e);
          }
        }
        try {
          boot.start();
        } catch (Exception e) {
          LOG.log(Level.SEVERE, "starting bootloader", e);
          displayException(e);
          System.exit(3);
        }

      }
    });
    Executors.newSingleThreadExecutor().execute(go);

  }

  private int getProperty(final String key, final int defaultVal, ProvisioningService ps) {
      String val = null;
      if (ps != null)
    	  val = (String) ps.getInformation().get(key);
      if (val == null)
    	  val = context.getProperty(key);
      return val != null ? Integer.parseInt(val) : defaultVal;
  }

  @SuppressWarnings("deprecation")
  private void startLog() {
	  ProvisioningService ps = null;
	  ServiceReference<ProvisioningService> ref = context.getServiceReference(ProvisioningService.class);
	  if (ref != null) {
		  ps = context.getService(ref);
	  }
      int LOG_LEVEL = getProperty("org.eclipse.concierge.log.level",
              LogService.LOG_DEBUG, ps);
      if (ref != null) {
    	  ps = null;
    	  context.ungetService(ref);
      }
      LogLevel level = LogLevel.WARN;
      switch(LOG_LEVEL) {
      case LogService.LOG_DEBUG: level = LogLevel.DEBUG; break;
      case LogService.LOG_ERROR: level = LogLevel.ERROR; break;
      case LogService.LOG_INFO: level = LogLevel.INFO; break;
      }
      final LogLevel LEVEL = level;
      ServiceTracker<LoggerAdmin, LoggerAdmin> admintracker;
      admintracker = new ServiceTracker<LoggerAdmin, LoggerAdmin>(context, LoggerAdmin.class, null) {

		@Override
		public LoggerAdmin addingService(ServiceReference<LoggerAdmin> reference) {
			LoggerAdmin admin = super.addingService(reference);
			LoggerContext lc = admin.getLoggerContext(null);
			Map<String, LogLevel> map = lc.getLogLevels();
			if (map == null) map = new HashMap<>();
			map.put(org.osgi.service.log.Logger.ROOT_LOGGER_NAME, LEVEL);
			lc.setLogLevels(map);
			return admin;
		}
    	  
      };
      admintracker.open();
}

@Override
  public void stop(BundleContext context) throws Exception {
    if(this.context == null) return;
    boot.stop();
    jul.uninstall();
    jul.close();
  }

  private void displayException(final Throwable t) {
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
        LOG.log(Level.SEVERE, "displayException fails", e);
    } catch (InterruptedException e) {
      LOG.log(Level.SEVERE, "displayException fails", e);
    }
}

  
  




  private ServiceRegistration<?> registration;
  private static final String STOP_EVENT = "fi/microserver/MicroServer/STOP";

  private void installEvent() {
    EventHandler service;
    Dictionary<String, Object> properties = new Hashtable<String, Object>();
    properties.put(Constants.SERVICE_RANKING, Integer.MIN_VALUE);
    properties.put(Constants.SERVICE_VENDOR, "fi.microserver.MicroServer");
    properties.put(EventConstants.EVENT_TOPIC, new String[] { STOP_EVENT } );
    service = new EventHandler() {

        public void handleEvent(Event event) {
            if(STOP_EVENT.equals(event.getTopic()))
            {
                Throwable t = (Throwable) event.getProperty(EventConstants.EXCEPTION);
                if(t != null) {
                  displayException(t);
                  System.exit(1); // Harde stop
                }
                stop();
            }
        } };
    registration = 
    context.registerService(EventHandler.class, service, properties);
    
}

  void stop() {
    if (registration != null) { 
    	registration.unregister(); 
    	registration = null;
    }
    SwingUtilities.invokeLater(
    new Runnable() {

        public void run() {
            try {
                context.getBundle(0L).stop();
            } catch (Exception e) {
                displayException(e);
                System.exit(2);
            }
        }
    });
}

}
