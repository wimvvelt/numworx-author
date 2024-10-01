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

/* $Id: PermissionSupport.java 1683 2011-09-27 14:16:23Z jeremias $ */

package ch.jm.osgi.provisioning;

import java.security.AllPermission;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Abstract base class for setting the {@link AllPermission} on the management agent when it is
 * started. If the <code>PermissionAdmin</code> class is not available (i.e. compendium APIs not
 * present) a fallback implementation is returned that does nothing.
 */
public abstract class PermissionSupport {

    /**
     * Creates a new instance of {@link PermissionSupport}.
     * @return the new instance
     */
    public static PermissionSupport newInstance() {
        try {
            //Try to load
            String pkg = PermissionSupport.class.getPackage().getName();
            String className = pkg + ".DefaultPermissionHandler";
            Class clazz = Class.forName(className, true, PermissionSupport.class.getClassLoader());
            return (PermissionSupport)clazz.newInstance();
        } catch (Exception e) {
            return new PermissionSupport() {
                public void setAllPermission(BundleContext context, Bundle targetBundle) {
                    //do nothing, no PermissionAdmin available
                }
            };
        }
    }

    /**
     * Sets the {@link AllPermission} for the given {@link Bundle}.
     * @param context the bundle context
     * @param targetBundle the target bundle for which to set the permission
     */
    public abstract void setAllPermission(BundleContext context, Bundle targetBundle);

}
