package org.smartrplace.driverhandler.more;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.prototypes.Configuration;
import org.ogema.simulation.shared.api.RoomInsideSimulationBase;
import org.ogema.simulation.shared.api.SingleRoomSimulationBase;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gateway.device.GatewayDevice;
import org.smartrplace.monitoring.vnstat.resources.NetworkTrafficData;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;

//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class GatewayDeviceHandler extends DeviceHandlerBase<GatewayDevice> {

	private final ApplicationManagerPlus appMan;

	public GatewayDeviceHandler(ApplicationManagerPlus appMan) {
		this.appMan = appMan;
		appMan.getLogger().info("{} created :)", this.getClass().getSimpleName());
	}
	
	@Override
	public DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert, InstalledAppsSelector appSelector) {
		return new DeviceTableBase(page, appMan, alert, appSelector, this) {

			@Override
			public void addWidgets(InstallAppDevice object,
					ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req,
					Row row, ApplicationManager appMan) {
				id = id + "_DeviceHandlerGwLocal";  // avoid duplicates for now
				addWidgetsInternal(object, vh, id, req, row, appMan);
				appSelector.addWidgetsExpert(null, object, vh, id, req, row, appMan);
			}

			@Override
			protected Class<? extends Resource> getResourceType() {
				return GatewayDeviceHandler.this.getResourceType();
			}

			@Override
			protected String id() {
				return GatewayDeviceHandler.this.id();
			}

			@Override
			protected String getTableTitle() {
				return "Gateway Device";
			}
			
			public GatewayDevice addWidgetsInternal(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
					OgemaHttpRequest req, Row row, ApplicationManager appMan) {
				//if(!(object.device() instanceof Thermostat) && (req != null)) return null;
				final GatewayDevice device;
				if(req == null)
					device = ResourceHelper.getSampleResource(GatewayDevice.class);
				else
					device = (GatewayDevice) object.device();
				//if(!(object.device() instanceof Thermostat)) return;
				final String name;
				name = ResourceUtils.getHumanReadableShortName(device);
				vh.stringLabel("Name", id, name, row);
				vh.stringLabel("ID", id, object.deviceId().getValue(), row);
				
				Label lastGitLabel = vh.intLabel("Git Update", id, device.gitUpdateStatus(), row, 0);
				if(req == null) {
					vh.registerHeaderEntry("Last Git");
				} else {
					if(device.gitUpdateStatus().exists()) {
						Label lastContact = addLastContact("Last Git", vh, id, req, row, device.gitUpdateStatus());
						lastGitLabel.setPollingInterval(DEFAULT_POLL_RATE, req);
						lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
					}
				}
				
				Label lastRestartLabel = vh.intLabel("Restart", id, device.systemRestart(), row, 0);
				if(req == null) {
					vh.registerHeaderEntry("Last Restart");
				} else {
					if(device.systemRestart().exists()) {
						Label lastContact = addLastContact("Last Restart", vh, id, req, row, device.systemRestart());
						lastRestartLabel.setPollingInterval(DEFAULT_POLL_RATE, req);
						lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
					}
				}

				addComment(object, vh, id, req, row);
				
				return device;
			}
			
		};
	}

	@Override
	public Class<GatewayDevice> getResourceType() {
		return GatewayDevice.class;
	}

	@Override
	public Collection<Datapoint> getDatapoints(InstallAppDevice installDeviceRes, DatapointService dpService) {
		List<Datapoint> result = new ArrayList<>();
		GatewayDevice device = (GatewayDevice) installDeviceRes.device();
		result.add(dpService.getDataPointStandard(device.gitUpdateStatus()));
		result.add(dpService.getDataPointStandard(device.systemRestart()));
		result.add(dpService.getDataPointStandard(device.datapointsInAlarmState()));
		result.add(dpService.getDataPointStandard(device.heartBeatDelay()));
		result.add(dpService.getDataPointStandard(device.apiMethodAccess()));

		List<NetworkTrafficData> ifacs = device.networkTrafficData().getAllElements();
		for(NetworkTrafficData ifac: ifacs) {
			addDatapoint(ifac.monthlyTotalKiB().reading(), result, ifac.getName(), dpService);			
		}

		return result;
	}

	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		appDevice.alarms().create();
		GatewayDevice device = (GatewayDevice) appDevice.device();
		AlarmingUtiH.setTemplateValues(appDevice, device.gitUpdateStatus(),
				0, 1000, 30, 120);
		//TODO: We need alarming if values occur too often
		AlarmingUtiH.setTemplateValues(appDevice, device.systemRestart(),
				200, 200, 10, TimeProcUtil.YEAR_MILLIS/(60000));
	}

	@Override
	public List<RoomInsideSimulationBase> startSupportingLogicForDevice(InstallAppDevice device,
			GatewayDevice deviceResource, SingleRoomSimulationBase roomSimulation, DatapointService dpService) {
		if(!deviceResource.networkTrafficData().exists()) {
			Resource networkTraffic = appMan.appMan().getResourceAccess().getResource("NetworkTrafficData");
			if(networkTraffic != null && networkTraffic.exists() && (networkTraffic instanceof ResourceList)) {
				if(((ResourceList<?>)networkTraffic).getElementType() == null) {
					((ResourceList<?>)networkTraffic).setElementType(NetworkTrafficData.class);
				}
				deviceResource.networkTrafficData().setAsReference(networkTraffic);
			}
		}
		
		Configuration rundirUpdateStatus = ResourceHelper.getTopLevelResource("RundirUpdateStatus", Configuration.class, appMan.getResourceAccess());
		if(rundirUpdateStatus == null)
			return super.startSupportingLogicForDevice(device, deviceResource, roomSimulation, dpService);
		BooleanResource failed = rundirUpdateStatus.getSubResource("failed", BooleanResource.class);
		if(failed.isActive()) {
			ResourceValueListener<BooleanResource> failedListener = new ResourceValueListener<BooleanResource>() {

				@Override
				public void resourceChanged(BooleanResource resource) {
					ValueResourceHelper.setCreate(deviceResource.gitUpdateStatus(), resource.getValue()?-1:0);
					
				}
			};
			failed.addValueListener(failedListener, true);
		}
		StringResource result = rundirUpdateStatus.getSubResource("result", StringResource.class);
		if(result.isActive()) {
			ResourceValueListener<StringResource> resultListener = new ResourceValueListener<StringResource>() {

				@Override
				public void resourceChanged(StringResource resource) {
					String val = resource.getValue();
					ValueResourceHelper.setCreate(deviceResource.gitUpdateStatus(), val.startsWith("no update")?1:2);
					
				}
			};
			result.addValueListener(resultListener, true);
		}
		StringResource systemBootTime = rundirUpdateStatus.getSubResource("systemBootTime", StringResource.class);
		//the resource might have been written before this starts up, so we also have to check outside the listener
		checkSystemBootTime(systemBootTime, deviceResource);
		if(systemBootTime.isActive()) {
			ResourceValueListener<StringResource> bootListener = new ResourceValueListener<StringResource>() {
				@Override
				public void resourceChanged(StringResource resource) {
					checkSystemBootTime(systemBootTime, deviceResource);					
				}
			};
			systemBootTime.addValueListener(bootListener, true);
		}
		
		if(deviceResource.publicAddress().isActive()) {
			ResourceValueListener<StringResource> addrListener = new ResourceValueListener<StringResource>() {
				@Override
				public void resourceChanged(StringResource resource) {
					String val = resource.getValue();
					int lastIdx = val.lastIndexOf('.');
					int last = -100;
					if(lastIdx > 0 && val.length() > (lastIdx+1)) {
						try {
							last = Integer.parseInt(val.substring(lastIdx+1));
						} catch(NumberFormatException e) {
							last = -200;
						}
					}
					if(Boolean.getBoolean("org.smartrplace.driverhandler.more.publicaddress.onvalueupdate")) {
						ValueResourceHelper.setCreate(deviceResource.foundPublicAddressLastPart(), last);
					} else {
						if(deviceResource.foundPublicAddressLastPart().getValue() != last) {
							ValueResourceHelper.setCreate(deviceResource.foundPublicAddressLastPart(), last);
						}
					}
				}
			};
			deviceResource.publicAddress().addValueListener(addrListener, true);
		}
		
		return super.startSupportingLogicForDevice(device, deviceResource, roomSimulation, dpService);
	}
	
	protected void checkSystemBootTime(StringResource systemBootTime, GatewayDevice deviceResource) {
		String bootTimeStr = systemBootTime.getValue();
		ZonedDateTime joda = null;
		try {
			joda = ZonedDateTime.parse(bootTimeStr);
			//joda = new DateTime(bootTimeStr);
			long bootTime = joda.toEpochSecond()*1000;
			long now = appMan.getFrameworkTime();
			if(now - bootTime < TimeProcUtil.HOUR_MILLIS) {
				ValueResourceHelper.setCreate(deviceResource.systemRestart(), 1);			
			}
		} catch(Exception e) {
			System.out.println("Warning: System boot time could not be parsed! Exception printStakTrace follows...");
			e.printStackTrace();
		}
	}
	
	@Override
	protected Class<? extends ResourcePattern<GatewayDevice>> getPatternClass() {
		return GatewayDevicePattern.class;
	}

	@Override
	protected ResourcePatternAccess advAcc() {
		return appMan.getResourcePatternAccess();
	}
}
