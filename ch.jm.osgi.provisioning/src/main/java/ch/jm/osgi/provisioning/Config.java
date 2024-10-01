/*
 * Copyright 2011 Jeremias Maerki, Switzerland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id: Config.java 1684 2011-09-27 15:35:57Z jeremias $ */

package ch.jm.osgi.provisioning;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Properties;

import org.osgi.framework.BundleContext;

/**
 * Provides access to the various configuration properties that the provisioning service uses.
 * It gets its properties from either the OSGi framework properties or from the system properties.
 */
class Config {

    private final BundleContext bundleContext;
    private Properties props = new Properties();

    /**
     * Creates a new Config object.
     * @param bundleContext the bundle context (may be null for testing purposes)
     */
    public Config(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        //Try to initialize SPID with some useful default
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            props.setProperty(Constants.SYS_PROPERTY_SPID, localHost.getHostName());
        } catch (UnknownHostException e) {
            props.setProperty(Constants.SYS_PROPERTY_SPID, "unknown");
        }

        //Load configuration reference first...
        String cfg = findProperty(Constants.SYS_PROPERTY_CONFIG);
        if (cfg != null) {
            try {
                URL url = new URL(cfg);
                InputStream in = url.openStream();
                try {
                    props.load(in);
                } finally {
                    Util.closeQuietly(in);
                }
            } catch (MalformedURLException mfue) {
                mfue.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        //...then override with explicit system properties
        findAndSetProperty(Constants.SYS_PROPERTY_DEBUG);
        findAndSetProperty(Constants.SYS_PROPERTY_URL);
        findAndSetProperty(Constants.SYS_PROPERTY_SPID);
    }

    private void findAndSetProperty(String key) {
        String u = findProperty(key);
        if (u != null) {
            props.setProperty(key, u);
        }
    }

    private String findProperty(String key) {
        String prop = null;
        if (this.bundleContext != null) {
            prop = this.bundleContext.getProperty(key);
        }
        if (prop == null) {
            //Also check System property if not found in framework properties
            prop = System.getProperty(key);
        }
        return prop;
    }

    /**
     * Returns the initial request URL.
     * @return the initial request URL (or null if it was not set)
     */
    public String getInitialRequestURL() {
        return props.getProperty(Constants.SYS_PROPERTY_URL);
    }

    /**
     * Returns the service platform identifier (SPID). This value is initialized to
     * the localhost's host name (or "unknown" if that could not be determined).
     * @return the SPID
     */
    public String getServicePlatformIdentifier() {
        return props.getProperty(Constants.SYS_PROPERTY_SPID);
    }

    /**
     * Indicates whether debug-level logging to the console is enabled (only applies to the case
     * where there is no log service.
     * @return true if debug-level logging is enabled
     */
    public boolean isDebugEnabled() {
        String s = props.getProperty(Constants.SYS_PROPERTY_DEBUG);
        return "true".equalsIgnoreCase(s);
    }

}
