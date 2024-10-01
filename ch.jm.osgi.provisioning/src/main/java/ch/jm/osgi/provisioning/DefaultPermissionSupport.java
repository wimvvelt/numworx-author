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

/* $Id: DefaultPermissionSupport.java 1683 2011-09-27 14:16:23Z jeremias $ */

package ch.jm.osgi.provisioning;

import java.security.AllPermission;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.permissionadmin.PermissionInfo;

/**
 * {@link PermissionSupport} subclass that actually uses {@link PermissionAdmin} if it's
 * available in the service registry.
 */
public class DefaultPermissionSupport extends PermissionSupport {

    /** {@inheritDoc} */
    public void setAllPermission(BundleContext context, Bundle targetBundle) {
        ServiceReference ref = context.getServiceReference(
                PermissionAdmin.class.getName());
        if (ref != null) {
            PermissionAdmin permAdmin = (PermissionAdmin)context.getService(ref);
            if (permAdmin != null) {
                try {
                    permAdmin.setPermissions(targetBundle.getLocation(), new PermissionInfo[] {
                        new PermissionInfo(AllPermission.class.getName(), "", "")
                    });
                } finally {
                    context.ungetService(ref);
                }
            }
        }
    }

}
