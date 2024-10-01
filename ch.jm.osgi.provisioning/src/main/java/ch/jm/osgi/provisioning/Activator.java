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

/* $Id: Activator.java 1684 2011-09-27 15:35:57Z jeremias $ */

package ch.jm.osgi.provisioning;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * The {@link BundleActivator} for the provisioning service.
 */
public class Activator implements BundleActivator {

    private ProvisioningServiceImpl service;

    /** {@inheritDoc} */
    public void start(BundleContext context) throws Exception {
        this.service = new ProvisioningServiceImpl(context);
        this.service.start();
    }

    /** {@inheritDoc} */
    public void stop(BundleContext context) throws Exception {
        //Service will be unregistered automatically
        this.service = null;
    }

}
