package fi.microserver.microboot;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.knopflerfish.service.repository.XmlBackedRepositoryFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.provisioning.ProvisioningService;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RequirementBuilder;
import org.osgi.util.tracker.ServiceTracker;

class StartBoot {

  final BundleContext context;
  private ServiceReference<Repository> reposRegistration;
  
  StartBoot(BundleContext context) {
    this.context = context;
  }

  @SuppressWarnings("rawtypes")
  class LocationFactory implements ServiceFactory {

	@Override
	public Object getService(Bundle bundle, ServiceRegistration registration) {
		return new LocationCommand(context, System.out);
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
	}

  }
  
  private String getProperty( String key, ProvisioningService ps) {
    Object value = ps.getInformation().get(key);
    if (value != null) return value.toString();
    return context.getProperty(key);
  }

  static final String BOOTLOADER  = "fi.dwo.BootLoader";

  private  Bundle installBoot(String BOOT) throws BundleException {
    Repository repos = context.getService(reposRegistration);
    RequirementBuilder builder;
    builder = repos.newRequirementBuilder("osgi.identity");
    builder.addDirective("filter", "(osgi.identity="+ BOOTLOADER +")");
    Requirement r = builder.build();
    try {
        if (BOOT == null) {
            Map<Requirement, Collection<Capability>> providers = repos.findProviders(Collections.singleton(r));
            Collection<Capability> caps = providers.get(r);
            context.ungetService(reposRegistration);repos = null;
            for(Capability c: caps) {
                Resource res = c.getResource();
                List<Capability> list = res.getCapabilities("osgi.content");
                for(Capability cc: list)
                    BOOT = cc.getAttributes().get("url").toString();
            }
        } 
        return context.installBundle(BOOT);
    } catch (BundleException e) {
        int type = e.getType();
        if (type == BundleException.DUPLICATE_BUNDLE_ERROR) { // uninstall asap
            FrameworkWiring fw = context.getBundle(0).adapt(FrameworkWiring.class);
            Collection<BundleCapability> res = fw.findProviders(r);
            for (BundleCapability bc:res) {
                bc.getRevision().getBundle().uninstall();
                return installBoot(BOOT); // recurse
            }
        }
        throw e;
    }
}

  void start() throws Exception {
    ServiceReference<ProvisioningService> ref = context.getServiceReference(ProvisioningService.class);
    if (ref == null) return;
    
    context.registerService("org.eclipse.concierge.shell.commands.ShellCommandGroup", new LocationFactory(), null);
    
    ServiceTracker<XmlBackedRepositoryFactory, XmlBackedRepositoryFactory> factory = new ServiceTracker<>(context, XmlBackedRepositoryFactory.class, null);
    factory.open();

    ProvisioningService ps = context.getService(ref);
    XmlBackedRepositoryFactory service = factory.waitForService(2000);
    String u = getProperty("fi.dwo.repository", ps);

 	reposRegistration = service.create(u, null, this);

 	String BOOT = getProperty("fi.dwo.boot", ps);
    context.ungetService(ref);
    Bundle boot = installBoot(BOOT);
    boot.start(Bundle.START_TRANSIENT);
  }
  
  
  void stop() {
  }
}
