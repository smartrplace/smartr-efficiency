package org.smartrplace.app.monbase.servlet;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.util.DatapointImpl;
import org.ogema.devicefinder.util.DpGroupUtil;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.app.monbase.MonitoringController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.frontend.servlet.ServletNumProvider;
import org.smartrplace.util.frontend.servlet.ServletStringProvider;
import org.smartrplace.util.frontend.servlet.UserServlet;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletPageProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;
import org.smartrplace.util.frontend.servlet.UserServletUtil;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** Implementation of servlet on /org/sp/app/monappserv/userdata */
public class DevicesServlet implements ServletPageProvider<InstallAppDevice> {
	//List<Room> knownRooms = null;
	
	/** Hash location or other ID -> Timeseries*/
	//final Map<String, TimeSeriesDataImpl> knownTimeseries = UserServlet.knownTS;
	final MonitoringController controller;
	final DatapointService dpService;
	
	public DevicesServlet(MonitoringController controller) {
		this.controller = controller;
		this.dpService = controller.dpService;
		if(dpService == null)
			throw new IllegalStateException("Datapoint Service required for Datapoint Servlet!");
	}

	@Override
	public Map<String, ServletValueProvider> getProviders(InstallAppDevice object, String user, Map<String, String[]> parameters) {
		Map<String, ServletValueProvider> result = new HashMap<>();
		
		PhysicalElement dev = object.device().getLocationResource();
		Room room = ResourceUtils.getDeviceRoom(dev);
		int roomIdInt = -3;
		if(room != null) {
			roomIdInt = room.getLocation().hashCode();
		}
		
		DeviceHandlerProviderDP<Resource> devHand = dpService.getDeviceHandlerProvider(object);
		String typeIdStr = null;
		if(devHand != null) {
			typeIdStr = devHand.getDeviceTypeShortId(dpService);
		}

		
		if(!UserServletUtil.isPOST(parameters)) {
			//perform filtering
			String roomFilter = UserServlet.getParameter("room", parameters);
			if(roomFilter != null) {
				if(room == null)
					return null;
				int roomFilterId;
				try {
					roomFilterId = Integer.parseInt(roomFilter);
				} catch(NumberFormatException e) {
					roomFilterId = roomFilter.hashCode();
				}
				if(roomIdInt != roomFilterId)
					return null;
			}
			
			String deviceTypeFilter = UserServlet.getParameter("deviceType", parameters);
			if(deviceTypeFilter != null) {
				if(devHand == null)
					return null;
				if(!deviceTypeFilter.equals(typeIdStr))
					return null;
			}
			
			String inactiveFilter = UserServlet.getParameter("includeInactive", parameters);
			if(inactiveFilter == null || (!inactiveFilter.toLowerCase().equals("true"))) {
				if(object.isTrash().getValue())
					return null;
			}
		}
		
		String locationStr = dev.getLocation();
		ServletStringProvider location = new ServletStringProvider(locationStr);
		result.put("locationDevice", location);
		ServletStringProvider locationConfig = new ServletStringProvider(object.getLocation());
		result.put("locationConfig", locationConfig);
		ServletNumProvider id = new ServletNumProvider(ServletPageProvider.getNumericalId(object.getLocation()));
		result.put("id", id);

		String label = DatapointImpl.getDeviceLabel(object, null, dpService, devHand);
		ServletStringProvider labelStd = new ServletStringProvider(label);
		result.put("labelStd", labelStd);
		OgemaLocale locale = UserServlet.getLocale(parameters);
		if(locale != null) {
			String labelLoc = DatapointImpl.getDeviceLabel(object, locale, dpService, devHand);
			ServletStringProvider labelLocale = new ServletStringProvider(labelLoc);
			result.put("labelLocale", labelLocale);
		}
		if(devHand != null) {
			ServletStringProvider typeId = new ServletStringProvider(typeIdStr);
			result.put("typeId", typeId);
			ServletStringProvider devHandLabel = new ServletStringProvider(devHand.label(null));
			result.put("type", devHandLabel);
		} else {
			ServletStringProvider type = new ServletStringProvider(dev.getResourceType().getSimpleName());
			result.put("type", type);
		}
		if(room != null) {
			ServletStringProvider roomName = new ServletStringProvider(ResourceUtils.getHumanReadableName(room));
			result.put("roomName", roomName);
			ServletNumProvider roomId = new ServletNumProvider(roomIdInt);
			result.put("roomId", roomId);
		}
		String subloc = object.installationLocation().getValue();
		if(subloc != null) {
			ServletStringProvider subLocation = new ServletStringProvider(subloc);
			result.put("subLocation", subLocation);
		}
		ServletNumProvider isActive = new ServletNumProvider(!object.isTrash().getValue());
		result.put("isActive", isActive);
		
		return result;
	}

	@Override
	public Collection<InstallAppDevice> getAllObjects(String user) {
		return DpGroupUtil.getAllDevices(controller.appMan.getResourceAccess());
	}

	@Override
	public InstallAppDevice getObject(String objectId, String user) {
		return UserServletUtil.getObject(objectId, getAllObjects(null));
	}
	
	@Override
	public String getObjectId(InstallAppDevice obj) {
		return obj.getLocation();
	}

	@Override
	public String getObjectName() {
		return "device";
	}
}
