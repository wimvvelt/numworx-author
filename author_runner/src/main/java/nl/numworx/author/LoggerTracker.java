package nl.numworx.author;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

class LoggerTracker extends ServiceTracker<Object, Object> {

	private FelixLogger logger;
	@SuppressWarnings("rawtypes")
	private ServiceReference current;
	private int level;

	public LoggerTracker(BundleContext context, FelixLogger logger) {
		super(context, "org.osgi.service.log.LogService", null);
		this.logger = logger;
		String level = context.getProperty("felix.log.level");
		this.level = Integer.parseInt(level);
	}

	@Override
	public Object addingService(ServiceReference<Object> reference) {
		Object service = super.addingService(reference);
		if (current == null) {
			current = reference;
			logger.setLogger(service);
			logger.setLogLevel(level);
			logger.flush();
		} else {
			// implement ranking
		}		
		return service;
	}

	@Override
	public void removedService(ServiceReference<Object> reference, Object service) {
		super.removedService(reference, service);
		logger.setLogger(getService());
		logger.setLogLevel(level);
		logger.flush();
	}


}
