package org.smartrplace.apps.hw.install.gui.prop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.DriverPropertySuccessHandler;
import org.ogema.devicefinder.api.OGEMADriverPropertyService;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class AggregatedDevicePropertyService implements OGEMADriverPropertyService<Resource> {
	protected final Collection<OGEMADriverPropertyService<?>> services;
	protected final String id;
	//protected final OgemaLogger log;
	
	public AggregatedDevicePropertyService(Collection<OGEMADriverPropertyService<?>> services, String id,
			OgemaLogger log) {
		this.services = services;
		this.id = id;
		//this.log = log;
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public String label(OgemaLocale locale) {
		return id;
	}

	protected static class OverallSuccessHandling {
		List<DriverPropertySuccessHandler<?>> sucHandlers = new ArrayList<>();
		int finished = 0;
		final int serviceNum;
		boolean successAll = true;
		String messageAll;
		public OverallSuccessHandling(int serviceNum) {
			this.serviceNum = serviceNum;
		}
		public void addMessage(String message) {
			if(message == null)
				return;
			if(messageAll == null)
				messageAll = message;
			else
				messageAll += ", "+message;
		}
	}
	
	protected class SinglePropertySuccessHandler implements DriverPropertySuccessHandler<Resource> {
		public final OverallSuccessHandling osh;
		protected final DriverPropertySuccessHandler<Resource> successHandler;
		
		public SinglePropertySuccessHandler(OverallSuccessHandling osh,
				DriverPropertySuccessHandler<Resource> successHandler) {
			this.osh = osh;
			this.successHandler = successHandler;
		}

		@Override
		public void operationFinished(Resource anchorResource, String propertyId, boolean success,
				String message) {
			if(!success)
				osh.successAll = false;
			osh.addMessage(message);
			osh.finished++;
			if(osh.finished == osh.serviceNum)
				successHandler.operationFinished(anchorResource, propertyId,
						osh.successAll, osh.messageAll);
		}
		
	}
	
	@Override
	public void updateProperty(Resource dataPointResource, String propertyId, OgemaLogger logger,
			final DriverPropertySuccessHandler<Resource> successHandler) {
		final OverallSuccessHandling osh = new OverallSuccessHandling(services.size());
		for(OGEMADriverPropertyService<?> serv: services) {
			AccessAvailability accLoc = getReadWriteType(dataPointResource, propertyId, serv);
			if(accLoc == AccessAvailability.READ || accLoc == AccessAvailability.WRITE) {
				SinglePropertySuccessHandler sucHand = null;
				if(successHandler != null) {
					sucHand = new SinglePropertySuccessHandler(osh, successHandler);
					osh.sucHandlers.add(sucHand);
				}
				updatePropertyLoc(dataPointResource, propertyId, logger, sucHand, serv);
			}
		}		
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void updatePropertyLoc(Resource dataPointResource, String propertyId, OgemaLogger logger,
			DriverPropertySuccessHandler successHandler,
			OGEMADriverPropertyService serv) {
		serv.updateProperty(dataPointResource, propertyId, logger, successHandler);		
	}

	@Override
	public void writeProperty(Resource dataPointResource, String propertyId, OgemaLogger logger, String value,
			final DriverPropertySuccessHandler<Resource> successHandler) {
		final OverallSuccessHandling osh = new OverallSuccessHandling(services.size());
		for(OGEMADriverPropertyService<?> serv: services) {
			AccessAvailability accLoc = getReadWriteType(dataPointResource, propertyId, serv);
			if(accLoc == AccessAvailability.WRITE_ONLY || accLoc == AccessAvailability.WRITE) {
				SinglePropertySuccessHandler sucHand = null;
				if(successHandler != null) {
					sucHand = new SinglePropertySuccessHandler(osh, successHandler);
					osh.sucHandlers.add(sucHand);
				}
				writePropertyLoc(dataPointResource, propertyId, logger, value, sucHand, serv);
			}
		}		
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void writePropertyLoc(Resource dataPointResource, String propertyId, OgemaLogger logger,
			String value,
			DriverPropertySuccessHandler successHandler,
			OGEMADriverPropertyService serv) {
		serv.writeProperty(dataPointResource, propertyId, logger, value, successHandler);		
	}

	@Override
	public void updateProperties(Resource dataPointResource, OgemaLogger logger,
			final DriverPropertySuccessHandler<Resource> successHandler) {
		final OverallSuccessHandling osh = new OverallSuccessHandling(services.size());
		for(OGEMADriverPropertyService<?> serv: services) {
			SinglePropertySuccessHandler sucHand = null;
			if(successHandler != null) {
				sucHand = new SinglePropertySuccessHandler(osh, successHandler);
				osh.sucHandlers.add(sucHand);
			}
			updatePropertiesLoc(dataPointResource, logger, sucHand, serv);
		}		
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void updatePropertiesLoc(Resource dataPointResource, OgemaLogger logger,
			DriverPropertySuccessHandler successHandler,
			OGEMADriverPropertyService serv) {
		serv.updateProperties(dataPointResource, logger, successHandler);		
	}

	@Override
	public Class<Resource> getDataPointResourceType() {
		return Resource.class;
	}

	@Override
	public AccessAvailability getReadWriteType(Resource dataPointResource, String propertyId) {
		AccessAvailability resultMax = AccessAvailability.UNKNOWN;
		for(OGEMADriverPropertyService<?> serv: services) {
			AccessAvailability accLoc = getReadWriteType(dataPointResource, propertyId, serv);
			switch(accLoc) {
			case WRITE:
				return AccessAvailability.WRITE;
			case READ:
				if(resultMax == AccessAvailability.WRITE || resultMax == AccessAvailability.WRITE_ONLY)
					return AccessAvailability.WRITE;
				else
					resultMax = AccessAvailability.READ;
				break;
			case WRITE_ONLY:
				if(resultMax == AccessAvailability.WRITE || resultMax == AccessAvailability.READ)
					return AccessAvailability.WRITE;
				else
					resultMax = AccessAvailability.WRITE_ONLY;
				break;
			default:
				break;				
			}
		}		
		return resultMax;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected AccessAvailability getReadWriteType(Resource dataPointResource, String propertyId,
			OGEMADriverPropertyService serv) {
		return serv.getReadWriteType(dataPointResource, propertyId);		
	}
}
