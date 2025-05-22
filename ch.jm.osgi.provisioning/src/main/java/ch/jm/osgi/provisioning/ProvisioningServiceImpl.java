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

/* $Id: ProvisioningServiceImpl.java 1692 2011-09-28 15:05:15Z jeremias $ */

package ch.jm.osgi.provisioning;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.provisioning.ProvisioningService;

/**
 * This class is the actual implementation of the {@link ProvisioningService}. It is responsible
 * for loading and processing provisioning ZIP files and the management of the service platform
 * information.
 * <p>
 * The following properties (OSGi framework or system properties) can be used to configure
 * the provisioning service:
 * <p>
 * <ul>
 * <li>{@link ch.jm.osgi.provisioning.Constants#SYS_PROPERTY_URL}:
 *           initial request URL (required to make the service operable)</li>
 * <li>{@link ch.jm.osgi.provisioning.Constants#SYS_PROPERTY_SPID}:
 *           the service platform identifier</li>
 * <li>{@link ch.jm.osgi.provisioning.Constants#SYS_PROPERTY_DEBUG}:
 *           set this to "true" to enable debug-level logging to the console
 *           when no log service is available</li>
 * <li>{@link ch.jm.osgi.provisioning.Constants#SYS_PROPERTY_CONFIG}:
 *           references a text file (Java Properties syntax) that contains one or more of the
 *           properties defined above. The value must be a valid URL that can be loaded
 *           by one of the available URL handlers. Note: specifying the above properties directly
 *           overrides any value loaded from an external config file!</li>
 * </ul>
 */
public class ProvisioningServiceImpl implements ProvisioningService,
            ch.jm.osgi.provisioning.Constants {

    //TODO Guard against endless recursion if the same reference references itself

    private static final String LOCATION_PREFIX = "provisioning:";

    private static final String MIME_UNKNOWN = "content/unknown";
    private static final String MIME_ZIP = "application/zip";

    private static final String PARAM_SPID = "service_platform_id";

    private static final String TYPE_TEXT = "text";
    private static final String TYPE_BINARY = "binary";
    private static final String TYPE_BUNDLE = "bundle";
    private static final String TYPE_BUNDLE_URL = "bundle-url";

    private static final Map VALID_TYPES = new java.util.HashMap();

    static {
        VALID_TYPES.put(MIME_STRING, TYPE_TEXT);
        VALID_TYPES.put(MIME_BYTE_ARRAY, TYPE_BINARY);
        VALID_TYPES.put(MIME_BUNDLE, TYPE_BUNDLE);
        VALID_TYPES.put(MIME_BUNDLE_ALT, TYPE_BUNDLE);
        VALID_TYPES.put(MIME_BUNDLE_URL, TYPE_BUNDLE_URL);
        //The following are not quite standard but let's allow them
        VALID_TYPES.put(TYPE_TEXT, TYPE_TEXT);
        VALID_TYPES.put(TYPE_BINARY, TYPE_BINARY);
        VALID_TYPES.put(TYPE_BUNDLE, TYPE_BUNDLE);
        VALID_TYPES.put(TYPE_BUNDLE_URL, TYPE_BUNDLE_URL);
    }

    private final Config config;
    private final BundleContext bundleContext;
    private Dictionary information = new Hashtable();
    private ServiceRegistration reg;

    /**
     * Creates a new provisioning service.
     * @param bundleContext the bundle context (may be null for testing purposes)
     */
    public ProvisioningServiceImpl(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.config = new Config(bundleContext);
    }

    /**
     * Starts the provisioning service and initiates the provisioning process if an initial
     * request URL is available.
     */
    public void start() {
        String url = config.getInitialRequestURL();
        if (url != null) {
            boolean needBootstrap = true;
            if (getInfoFile().isFile()) {
                //initial provisioning has already been performed
                needBootstrap = false;
                loadDictionary();
                String previousError = (String)this.information.get(PROVISIONING_ERROR);
                if (previousError != null) {
                    error(ERROR_RECOVERABLE,
                            "Previous initial provisioning failed. Reason: " + previousError);
                }
            }
            makeOperational();
            if (needBootstrap) {
                //We have to start from the beginning and bootstrap
                bootstrap(url);
            }
        } else {
            info("Initial provisioning inactive. Reason: no initial request URL available."
                    + " Supply the '" + ch.jm.osgi.provisioning.Constants.SYS_PROPERTY_URL
                    + "' property to enable initial provisioning.");
        }
    }

    /**
     * Starts the provisioning process.
     * @param initialProvisioningURL the initial request URL
     */
    protected void bootstrap(String initialProvisioningURL) {
        this.information.put(PROVISIONING_SPID, config.getServicePlatformIdentifier());
        incrementUpdateCount();
        handleReference(initialProvisioningURL);
    }

    /**
     * Returns the file to use for storing the service platform information. This
     * can be overwritten for testing purposes.
     * @return the file
     */
    protected File getInfoFile() {
        return this.bundleContext.getDataFile("information.dat");
    }

    /**
     * Makes the provisioning service operational by registering it in the OSGi service
     * registry.
     */
    protected void makeOperational() {
        if (this.reg == null) {
            Dictionary props = new java.util.Hashtable();
            props.put(Constants.SERVICE_DESCRIPTION,
                    "This service handles initial provisioning of a management agent.");
            this.reg = this.bundleContext.registerService(
                    ProvisioningService.class.getName(),
                    this, props);
        }
    }

    private boolean loadDictionary() {
        File infoFile = getInfoFile();
        try {
            InputStream in = new java.io.FileInputStream(infoFile);
            ObjectInputStream oin = new ObjectInputStream(in);
            try {
                this.information = (Dictionary)oin.readObject();
            } finally {
                Util.closeQuietly(oin);
            }
            return true;
        } catch (Exception e) {
            error(ERROR_LOAD_SAVE,
                    "Error loading service platform information: " + e.getMessage(), e);
            return false;
        }
    }

    private void saveDictionary(boolean ignoreErrors) {
        File infoFile = getInfoFile();
        try {
            OutputStream out = new java.io.FileOutputStream(infoFile);
            out = new java.io.BufferedOutputStream(out);
            ObjectOutputStream oout = new ObjectOutputStream(out);
            try {
                oout.writeObject(this.information);
            } finally {
                oout.close();
            }
        } catch (Exception e) {
            if (!ignoreErrors) {
                error(ERROR_LOAD_SAVE,
                        "Error saving service platform information: " + e.getMessage(), e);
            }
        }
    }

    private void incrementUpdateCount() {
        incrementUpdateCount(true);
    }

    private synchronized void incrementUpdateCount(boolean save) {
        Integer count = (Integer)this.information.get(PROVISIONING_UPDATE_COUNT);
        if (count != null) {
            count = new Integer(count.intValue() + 1);
        } else {
            count = new Integer(1);
        }
        this.information.put(PROVISIONING_UPDATE_COUNT, count);
        if (save) {
            saveDictionary(false);
        }
    }

    /** {@inheritDoc} */
    public void addInformation(Dictionary info) {
        addInformation(info, true);
    }

    private synchronized void addInformation(Dictionary info, boolean reactToReference) {
        boolean hasRef = false;
        Enumeration en = info.keys();
        while (en.hasMoreElements()) {
            Object k = en.nextElement();
            if (k instanceof String) {
                String key = (String)k;
                Object value = info.get(key);
                debug(key + ": " + value);
                if (PROVISIONING_UPDATE_COUNT.equals(key)) {
                    //ignore, handled internally, treat as read-only to the outside
                } else {
                    if (PROVISIONING_REFERENCE.equals(key)) {
                        hasRef = true;
                    }
                    if (value instanceof String || value instanceof byte[]) {
                        this.information.put(key, value);
                    } //silently ignore anything that is not (String|byte[])
                }
            } else {
                //silently ignore invalid key types (need String)
            }
        }
        incrementUpdateCount();

        if (reactToReference && hasRef) {
            handleReference(this.information);
        }
    }

    /** {@inheritDoc} */
    public void addInformation(ZipInputStream zis) throws IOException {
        Map bundleBytes = new java.util.HashMap();
        Dictionary dict = new Hashtable();
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.isDirectory()) {
                continue; //skip directories
            }
            String name = entry.getName();
            if ("META-INF/MANIFEST.MF".equalsIgnoreCase(name)) {
                //TODO load manifest
                continue;
            }

            String key = name;
            if (key.startsWith("/")) {
                //Key may not start with "/"
                key = key.substring(1);
            }

            //Determine content type
            String type = determineContentType(entry);
            if (type == null) {
                type = determineContentTypeFromExtension(name);
                if (!TYPE_BINARY.equals(type)) {
                    //TODO Uncertainty: the spec is not quite clear if extensions are mandatory
                    //and if the text after the last dot (the extension) needs to be removed.
                    //The chapter "Example With File Scheme" seems to suggest that the
                    //extensions are only used if the "extra" field isn't used.
                    //We're currently removing the extension only if the type is not (txt|url|jar)
                    int dot = key.lastIndexOf('.');
                    if (dot >= 0) {
                        //Remove extension
                        key = key.substring(0, dot);
                    }
                }
            }

            //Handle entries
            if (TYPE_TEXT.equals(type)) {
                String value = getText(zis);
                dict.put(key, value);
            } else if (TYPE_BUNDLE.equals(type)) {
                bundleBytes.put(key, getBinary(zis, entry.getSize()));
            } else if (TYPE_BUNDLE_URL.equals(type)) {
                String u = getText(zis);
                bundleBytes.put(key, getBinary(download(new URL(u)), -1));
            } else {
                byte[] value = getBinary(zis, entry.getSize());
                dict.put(key, value);
            }
        }

        //Add new information
        addInformation(dict, false);

        //Now that the ZIP has been fully read and no errors occurred, we can install the bundles.
        installBundles(bundleBytes);

        //Now start the main bundle if necessary
        startBundle(dict);

        //Finally, process reference
        handleReference(dict);
    }

    private String determineContentType(ZipEntry entry)
            throws UnsupportedEncodingException {
        //TODO Step 1: Use Manifest
        //Step 2: extra field
        byte[] extraBytes = entry.getExtra();
        if (extraBytes != null) {
            String extra = new String(extraBytes, "UTF-8").toLowerCase();
            String type = (String)VALID_TYPES.get(extra);
            if (type != null) {
                return type;
            } else {
                //No valid type
                //don't look further (says the spec)
                return TYPE_BINARY;
            }
        }
        return null;
    }

    private String determineContentTypeFromExtension(String name) {
        //Step 3: use extension
        int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            String ext = name.substring(lastDot + 1);
            if ("txt".equalsIgnoreCase(ext)) {
                return TYPE_TEXT;
            } else if ("jar".equalsIgnoreCase(ext)) {
                return TYPE_BUNDLE;
            } else if ("url".equalsIgnoreCase(ext)) {
                return TYPE_BUNDLE_URL;
            }
        }

        return TYPE_BINARY; //Step 4: fall back to binary
    }

    private void installBundles(Map bundleBytes) {
        Iterator iter = bundleBytes.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry e = (Map.Entry)iter.next();
            String name = (String)e.getKey();
            InputStream in = new ByteArrayInputStream((byte[])e.getValue());
            try {
                installBundle(name, in);
            } catch (BundleException be) {
                error(ERROR_UNKNOWN,
                        "Error while installing/updating a bundle as part of initial provisioning: "
                            + be.getMessage(), be);
                //continue in the hope that it helps fix the problem.
            }
        }
    }

    private Bundle installBundle(String name, InputStream in) throws BundleException {
        String location = LOCATION_PREFIX + name;
        debug("Installing bundle with location " + location);
        Bundle bundle = findByLocation(location);
        if (bundle != null) {
            bundle.update(in);
        } else {
            bundle = this.bundleContext.installBundle(location, in);
        }
        return bundle;
    }

    private Bundle findByLocation(String location) {
        Bundle[] bundles = this.bundleContext.getBundles();
        for (int i = 0, c = bundles.length; i < c; i++) {
            Bundle b = bundles[i];
            if (b.getLocation().equalsIgnoreCase(location)) {
                return b;
            }
        }
        return null;
    }

    private void startBundle(Dictionary dict) {
        Object o = dict.get(PROVISIONING_START_BUNDLE);
        if (o != null) {
            if (o instanceof String) {
                String startBundle = LOCATION_PREFIX + (String)o;
                Bundle b = findByLocation(startBundle);
                if (b != null) {
                    startBundle(b);
                } else {
                    error(ERROR_UNKNOWN,
                            "Bundle to be started during initial provisioning wasn't found: "
                            + startBundle);
                }
            } else {
                error(ERROR_UNKNOWN,
                        "The dictionary entry '" + PROVISIONING_START_BUNDLE + "' was no String!");
            }
        }
    }

    private void startBundle(Bundle b) {
        //Give AllPermission as per the spec, never used in numworx author
        //PermissionSupport.newInstance().setAllPermission(this.bundleContext, b);

        try {
            b.start();
        } catch (BundleException be) {
            error(ERROR_UNKNOWN, "Error starting bundle " + b.getLocation()
                    + " for initial provisioning: "
                    + be.getMessage(), be);
        }
    }

    private void handleReference(Dictionary dict) {
        String ref = (String)dict.get(PROVISIONING_REFERENCE);
        handleReference(ref);
    }

    private void handleReference(String ref) {
        if (ref != null && ref.length() > 0) {
            URL url;
            try {
                url = new URL(ref.trim());
            } catch (MalformedURLException mfue) {
                error(ERROR_URL, mfue.getMessage(), mfue);
                return;
            }
            handleReference(url);
        }
    }

    private void handleReference(URL url) {
        try {
            InputStream in = download(url);
            in = new java.io.BufferedInputStream(in);
            ZipInputStream zis = new ZipInputStream(in);
            try {
                addInformation(zis);
            } finally {
                Util.closeQuietly(zis);
            }
        } catch (ZipException ze) {
            error(ERROR_CORRUPT_ZIP, ze.getMessage(), ze);
        } catch (FileNotFoundException fnfe) {
            error(ERROR_DOWNLOAD,
                    "Initial provisioning reference not found: " + url, fnfe);
        } catch (IOException ioe) {
            error(ERROR_DOWNLOAD,
                    "I/O error while handling initial provisioning reference: " + url, ioe);
        }
    }

    private String getText(InputStream in) throws IOException {
        InputStreamReader reader = new InputStreamReader(in, "UTF-8");
        StringWriter writer = new StringWriter();
        Util.copy(reader, writer);
        return writer.toString();
    }

    private byte[] getBinary(InputStream in, long sizeHint) throws IOException {
        ByteArrayOutputStream baout;
        if (sizeHint > 0 && sizeHint <= Integer.MAX_VALUE) {
            baout = new ByteArrayOutputStream((int)sizeHint);
        } else {
            baout = new ByteArrayOutputStream();
        }
        Util.copy(in, baout);
        return baout.toByteArray();
    }

    /** {@inheritDoc} */
    public Dictionary getInformation() {
        return Util.unmodifiableDictionary(this.information);
    }

    /** {@inheritDoc} */
    public void setInformation(Dictionary info) {
        Enumeration en = info.keys();
        while (en.hasMoreElements()) {
            String key = (String)en.nextElement();
            if (PROVISIONING_UPDATE_COUNT.equals(key)) {
                //preserve
            } else {
                this.information.remove(key);
            }
        }
        addInformation(info);
    }

    private URL updateURL(URL url) throws MalformedURLException {
        String protocol = url.getProtocol();
        if ("file".equals(protocol)||"jar".equals(protocol)) {
            return url; //Don't append SPID
        }
        String query = url.getQuery();
        String params = PARAM_SPID + "=" + config.getServicePlatformIdentifier();
        if (query != null) {
            query = query + "&" + params;
        } else {
            query = params;
        }
        try {
            URI mod = new URI(protocol, url.getUserInfo(), url.getHost(), url.getPort(),
                    url.getPath(), query, url.getRef());
            return mod.toURL();
        } catch (URISyntaxException use) {
            //Just turn it to a URL exception since we're dealing with URLs (URI was just a helper)
            throw new MalformedURLException(use.getMessage());
        }
    }

    private InputStream download(URL url) throws IOException {
        url = updateURL(url);
        //TODO improve, handle HTTPS certificates etc.
        URLConnection conn = url.openConnection();
        conn.setAllowUserInteraction(false);
        conn.connect();
        String contentType = conn.getContentType();
        if (contentType != null
                && !MIME_UNKNOWN.equals(contentType)
                && !MIME_ZIP.equals(contentType)) {
            Util.closeQuietly(conn.getInputStream());
            throw new IOException("Expected a ZIP file (" + MIME_ZIP + ") but got " + contentType);
        }
        return conn.getInputStream();
    }

    // ==================================== Logging

    private void debug(String message) {
      LOG.fine(message);
    }

    Logger LOG = Logger.getLogger(getClass().getName());
    private void info(String message) {
            LOG.info(message);
    }

    private void error(int errorCode, String message) {
        error(errorCode, message, null);
    }

    @SuppressWarnings("unchecked")
    private void error(int errorCode, String message, Throwable t) {
        if (errorCode >= 0) {
            this.information.put(PROVISIONING_ERROR, errorCode + " " + message);
            incrementUpdateCount(false);
            saveDictionary(true);
        }
        LOG.log(Level.SEVERE, message, t);
    }
}
