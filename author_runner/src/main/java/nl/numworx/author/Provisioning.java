package nl.numworx.author;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.zip.ZipInputStream;


import org.osgi.framework.*;
import org.osgi.service.provisioning.*;

import ch.jm.osgi.provisioning.ProvisioningServiceImpl;

public class Provisioning {
  static String DWOv1 = "#DWOv1";
  static String DWOv2 = "PK";

  @SuppressWarnings({"deprecation"})
  static ProvisioningService install(BundleContext context) {
    ProvisioningServiceImpl s = new ProvisioningServiceImpl(context);
    s.start();
    Object v = s.getInformation().get("fi.microserver.version");
    String version = context.getProperty("fi.microserver.version");
    if (!version.equals(v)) {
      try {
        String zip = context.getProperty(ProvisioningService.PROVISIONING_REFERENCE);
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new URL(zip).openStream()));
        s.addInformation(zis);
        zis.close();
      } catch (IOException e) {
      }
    }
    
    String uri = context.getProperty("fi.dwo.provisioning");
    v = s.getInformation().get("fi.dwo.provisioning");
    Object vv = s.getInformation().get("fi.dwo.provisioning.version");
    if (uri != null ) {
      try {
        URL url = new URL(uri);
        URLConnection uc = url.openConnection();
        long l = uc.getLastModified();
        String ll = String.valueOf(l);
        if (ll.equals(vv) && uri.equals(v))
        {
          return s;
        }
        
        BufferedInputStream in = new BufferedInputStream(uc.getInputStream());
        in.mark(6);
        byte[] buf = new byte[6];
        in.read(buf);
        String string = new String(buf, 0);
        if (DWOv1.equals(string)) {
          in.reset();
          Properties props = new Properties();
          props.load(in);
          s.addInformation(props);
        } else if (DWOv2.equals(string.substring(0, 2))) {
          in.reset();
          s.addInformation(new ZipInputStream(in));
        }
        in.close();
        Hashtable<String, String> info = new Hashtable<>();
        info.put("fi.dwo.provisioning", uri);
        info.put("fi.dwo.provisioning.version", ll);
        s.addInformation(info);
      } catch (Exception oops) {

      }
    } else {
// Reset to context 
    	Hashtable<String, Object> reset = new Hashtable<>();
    	Dictionary info = s.getInformation();
    	Enumeration keys = info.keys();
    	while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			String value = context.getProperty(key);
			Object old = info.get(key);
			if (old instanceof String && ! old.equals(value) && value != null) {
				reset.put(key, value);
			}
		}
    	if (!reset.isEmpty()) {
            reset.put("fi.dwo.provisioning", "null"); // reset to null
    		s.addInformation(reset);
    	}
    	
    }
    return s;
  }


}
