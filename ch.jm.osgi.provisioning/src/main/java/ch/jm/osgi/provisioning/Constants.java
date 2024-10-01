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

/* $Id: Constants.java 1693 2011-09-28 15:05:33Z jeremias $ */

package ch.jm.osgi.provisioning;

import org.osgi.service.provisioning.ProvisioningService;

/**
 * This interface defines various constants used by the provisioning service.
 */
public interface Constants {

    /** The property prefix used by proprietary configuration properties. */
    String SYS_PROPERTY_PREFIX = "ch.jm.osgi.provisioning.";

    /**
     * This property controls whether debug-level logging can occur on the system console.
     * Valid values: "true", "false". Default: "false"
     */
    String SYS_PROPERTY_DEBUG = SYS_PROPERTY_PREFIX + "debug";

    /**
     * This property references a text file (Java Properties syntax) with properties to be loaded
     * for the configuration of the initial provisioning service. The value must be a valid URL
     * that can be loaded by the available URL handlers.
     */
    String SYS_PROPERTY_CONFIG = SYS_PROPERTY_PREFIX + "config";

    /**
     * This property is used for specifying the initial request URL. The value must be a valid URL
     * that can be loaded by the available URL handlers and refers to a ZIP file as described
     * in the OSGi Initial Provisioning specification.
     */
    String SYS_PROPERTY_URL = ProvisioningService.PROVISIONING_REFERENCE;

    /**
     * This property defines the service platform identifier if it is not determined automatically
     * by the management agent.
     */
    String SYS_PROPERTY_SPID = ProvisioningService.PROVISIONING_SPID;

    /** The property prefix used by properties for the ProvisioningService. */
    String PROVISIONING_PREFIX = "provisioning.";

    /**
     * This property indicates an error condition encountered during initial provisioning that
     * some client can inspect.
     */
    String PROVISIONING_ERROR = PROVISIONING_PREFIX + "error";

    /** Error code for recoverable errors (only used internally for logging) */
    int ERROR_RECOVERABLE = -1;
    /** Error code: Unknown error */
    int ERROR_UNKNOWN = 0;
    /** Error code: Couldn't load or save provisioning information */
    int ERROR_LOAD_SAVE = 1;
    /** Error code: MalformedURLException */
    int ERROR_URL = 2;
    /** Error code: IOException when retrieving document of a URL */
    int ERROR_DOWNLOAD = 3;
    /** Error code: Corrupted ZipInputStream */
    int ERROR_CORRUPT_ZIP = 4;

}
