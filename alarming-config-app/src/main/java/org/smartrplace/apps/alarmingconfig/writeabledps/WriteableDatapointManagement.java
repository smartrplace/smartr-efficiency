package org.smartrplace.apps.alarmingconfig.writeabledps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.resourcemanager.AccessPriority;
import org.ogema.core.resourcemanager.pattern.PatternListener;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DatapointImpl;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.spapi.model.WriteableDatapoint;
import org.smartrplace.util.frontend.servlet.UserServletUtil;

import de.iwes.util.resource.ValueResourceHelper;

public class WriteableDatapointManagement implements PatternListener<WriteableDatapointPattern> {
	
	private final ApplicationManagerPlus appMan;
	private final DatapointService dpService;
	
	public WriteableDatapointManagement(ApplicationManagerPlus appMan) {
		this.appMan = appMan;
		this.dpService = appMan.dpService();
		
		Collection<InstallAppDevice> iadsToClean = dpService.managedDeviceResoures("org.smartrplace.app.drivermonservice.devicehandler.WriteableDatapointHandler", false, true);
		for(InstallAppDevice iad: iadsToClean) {
			iad.delete();
		}
		
		appMan.getResourcePatternAccess().addPatternDemand(WriteableDatapointPattern.class, this, AccessPriority.PRIO_LOWEST);
	}
	
	
	@Deprecated
	public SingleValueResource getMainSensorValue(WriteableDatapoint device,
			InstallAppDevice deviceConfiguration) {
		return device.resource();
	}

	@Deprecated
	protected Collection<Datapoint> getDatapoints(WriteableDatapoint device,
			InstallAppDevice deviceConfiguration) {
		List<Datapoint> result = new ArrayList<>();
		String loc = device.datapointLocation().getValue();
		Datapoint dp = dpService.getDataPointStandard(loc);
		RecordedData recData = ValueResourceHelper.getRecordedData(device.resource());
		if(recData != null)
			dp.setTimeSeries(recData);
		if(dp instanceof DatapointImpl) {
			((DatapointImpl)dp).setResourceExperimental(device.resource());
		}
		result.add(dp);
		
		return result;
	}

	@Deprecated
	public boolean relevantForDefaultLogging(Datapoint dp, InstallAppDevice iad) {
		WriteableDatapoint device = (WriteableDatapoint) iad.device();
		return !device.disableLogging().getValue();
	}



	@Override
	public void patternAvailable(WriteableDatapointPattern pattern) {
		WriteableDatapoint wdp = pattern.model;
		
		checkDatapoint(wdp);
	}

	public void checkDatapoint(WriteableDatapoint wdp) {
		//TODO: This criteria may be adapted in the future
		if(wdp.deviceAssigned().isReference(false) && (!Boolean.getBoolean("org.smartrplace.apps.alarmingconfig.writeabledps.cleanup")))
			return;
		
		String name = "Unknown"+wdp.datapointLocation().getValue();
		//Find room or device
		int i = wdp.datapointLocation().getValue().lastIndexOf('/');
		if(i < 0) {
			ValueResourceHelper.setCreate(wdp.name(), name);		
			return;
		}
		String id = wdp.datapointLocation().getValue().substring(0, i);
		String alarmType = wdp.datapointLocation().getValue().substring(i+1);
		Room room = null;
		PhysicalElement device = null;
		if(AlarmingUtiH.roomAlarms.contains(alarmType)) {
			room = UserServletUtil.getRoomById(id, appMan.getResourceAccess(), dpService);
			if(room != null) {
				name = ResourceUtils.getHumanReadableShortName(room)+"::"+alarmType;
				wdp.deviceAssigned().setAsReference(room);
				room.getSubResource(alarmType, WriteableDatapoint.class).setAsReference(wdp);
				//ValueResourceHelper.setCreate(wdp.alarmType(), alarmType);
			} else {
				name += "(Room)";
			}
		} else if(AlarmingUtiH.deviceAlarms.contains(alarmType)) {
			device = UserServletUtil.getDeviceById(id, appMan.getResourceAccess(), dpService);
			if(device != null) {
				if(device instanceof Room) {
					wdp.delete();
					return;
				}
				InstallAppDevice iad = dpService.getMangedDeviceResource(device);
				if(iad != null)
					name = iad.deviceId().getValue()+"::"+alarmType;
				else
					name = ResourceUtils.getHumanReadableName(device)+"::"+alarmType;
				wdp.deviceAssigned().setAsReference(device);
				device.getSubResource(alarmType, WriteableDatapoint.class).setAsReference(wdp);
				//ValueResourceHelper.setCreate(wdp.alarmType(), alarmType);
			} else {
				name +="(Device)";
			}
		} else if(Boolean.getBoolean("org.smartrplace.apps.alarmingconfig.writeabledps.cleanup"))
			wdp.delete();
		
		//Set name
		ValueResourceHelper.setCreate(wdp.name(), name);		
	}


	@Override
	public void patternUnavailable(WriteableDatapointPattern pattern) {
		// Nothing to do for now
		
		//TODO: Clean up for lost datapoints, but then it needs to be deleted from remote
		
	}
}
