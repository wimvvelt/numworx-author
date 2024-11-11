package fi.microserver.microboot;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
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
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogService;
import org.osgi.service.log.admin.LoggerAdmin;
import org.osgi.service.log.admin.LoggerContext;
import org.osgi.service.provisioning.ProvisioningService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator, ServiceTrackerCustomizer<ConditionalPermissionAdmin, ConditionalPermissionAdmin> {

  
  private final Logger LOG = Logger.getLogger(getClass().getName());
  ServiceTracker<ConditionalPermissionAdmin, ConditionalPermissionAdmin> tracker;
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
    tracker = new ServiceTracker<ConditionalPermissionAdmin, ConditionalPermissionAdmin>(context, ConditionalPermissionAdmin.class, this);
    tracker.open();

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

  private boolean getProperty(final String key, final boolean defaultVal) {
      final String val = context.getProperty(key);
      return val != null ? Boolean.valueOf(val).booleanValue() : defaultVal;
}

  private void startLog() {
	  ProvisioningService ps = null;
	  ServiceReference<ProvisioningService> ref = context.getServiceReference(ProvisioningService.class);
	  if (ref != null) {
		  ps = context.getService(ref);
	  }
      int LOG_BUFFER_SIZE = getProperty("org.eclipse.concierge.log.buffersize",
              10, ps);
      int LOG_LEVEL = getProperty("org.eclipse.concierge.log.level",
              LogService.LOG_DEBUG, ps);
      if (ref != null) {
    	  ps = null;
    	  context.ungetService(ref);
      }
      boolean QUIET = !getProperty("fi.dwo.console", false);
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
			// TODO Auto-generated method stub
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
//      LogServiceImpl impl = new LogServiceImpl(LOG_BUFFER_SIZE, LOG_LEVEL, QUIET);
//      context.registerService(LogReaderService.class, impl, null);
//      context.registerService(LogService.class, impl.factory, null);
}

@Override
  public void stop(BundleContext context) throws Exception {
    if(this.context == null) return;
    boot.stop();
    tracker.close();
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

  
  
  @Override
  public ConditionalPermissionAdmin addingService(
      ServiceReference<ConditionalPermissionAdmin> reference) {
    ConditionalPermissionAdmin admin = context.getService(reference);
    if (admin == null) return null;
    LOG.info("installing security policy");
    List<String> lines = Collections.emptyList();
    try {
      lines = readPolicyFile(getClass().getResourceAsStream("/info.txt"));
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "read profile", e);
    }
    String line = 
     "ALLOW {"                                          
        + "[org.osgi.service.condpermadmin.BundleLocationCondition \""
        + context.getBundle().getLocation() + "\"]"
        + "(java.security.AllPermission \"*\" \"*\")"
        + "} \"Management Agent Policy\"";
    
    ConditionalPermissionUpdate update = admin.newConditionalPermissionUpdate();
    List<ConditionalPermissionInfo> list = update.getConditionalPermissionInfos();
    list.clear();

    ConditionalPermissionInfo info = admin.newConditionalPermissionInfo(line);
    list.add(info);
    for(String l: lines) {
      info = admin.newConditionalPermissionInfo(l);
      list.add(info);
    }
    if (!update.commit())
      LOG.warning("Commit failed");
      ;
    return admin;
  }

  @Override
  public void modifiedService(ServiceReference<ConditionalPermissionAdmin> reference,
      ConditionalPermissionAdmin service) {
  }

  @Override
  public void removedService(ServiceReference<ConditionalPermissionAdmin> reference,
      ConditionalPermissionAdmin service) {
    ConditionalPermissionUpdate update = service.newConditionalPermissionUpdate();
    List<ConditionalPermissionInfo> list = update.getConditionalPermissionInfos();
    list.clear();
    update.commit();
    context.ungetService(reference);
    LOG.info("removed security policy");
  }


  private List<String> readPolicyFile(InputStream policyFile) throws Exception {
    BufferedReader policyReader = null;
    Exception org = null;
    try
    {
        policyReader = new BufferedReader(new InputStreamReader(policyFile, "UTF-8"));
        List policy = new ArrayList();
        StringBuffer buffer = new StringBuffer();
        for (String input = policyReader.readLine(); input != null; input = policyReader.readLine()) {
            if (!input.trim().startsWith("#")) {
              buffer.append(input);
              if (input.contains("}")) {
                policy.add(buffer.toString());
                buffer = new StringBuffer();
              }
            }
        }
        return policy;
    }
    catch (Exception ex) {
        org = ex;
        throw ex;
    }
    finally {
        if (policyReader != null) {
            try
            {
                policyReader.close();
            }
            catch (Exception ex) {
                if (org == null) {
                    throw ex;
                }
            }
        }
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
