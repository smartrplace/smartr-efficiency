package org.smartrplace.mqtt.devicetable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.model.devices.connectiondevices.ElectricityConnectionBox;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.ElectricCurrentSensor;
import org.ogema.model.sensors.ElectricEnergySensor;
import org.ogema.model.sensors.ElectricVoltageSensor;
import org.ogema.model.sensors.PowerSensor;
import org.ogema.model.sensors.ReactivePowerAngleSensor;
import org.ogema.model.sensors.Sensor;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;


//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class DeviceHandlerMQTT_ElecConnBox extends DeviceHandlerBase<ElectricityConnectionBox> {
	private final ApplicationManagerPlus appMan;
	
	public DeviceHandlerMQTT_ElecConnBox(ApplicationManagerPlus appMan) {
		this.appMan = appMan;
	}
	
	@Override
	public Class<ElectricityConnectionBox> getResourceType() {
		return ElectricityConnectionBox.class;
	}

	@Override
	public DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert,
			InstalledAppsSelector appSelector) {
		return new DeviceTableBase(page, appMan, alert, appSelector, this) {
			
			@Override
			public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {

				final ElectricityConnectionBox box =
						(ElectricityConnectionBox) addNameWidget(object, vh, id, req, row, appMan);

				Room deviceRoom = box.location().room();
				//Label lastContact = null;

				ElectricityConnection cc = box.connection();
				if(cc.powerSensor().isActive() || req == null)
					addPowerEnergySensor(cc, vh, id, req, row);
				else for (ElectricityConnection c : box.getSubResources(ElectricityConnection.class, true)) {
					if(c.powerSensor().isActive()) {
						addPowerEnergySensor(c, vh, id, req, row);
						break;
					}
				}

				addRoomWidget(vh, id, req, row, appMan, deviceRoom);
				addSubLocation(object, vh, id, req, row);
				addInstallationStatus(object, vh, id, req, row);
				addComment(object, vh, id, req, row);

				appSelector.addWidgetsExpert(null, object, vh, id, req, row, appMan);
			}
			
			@Override
			protected Class<? extends Resource> getResourceType() {
				return DeviceHandlerMQTT_ElecConnBox.this.getResourceType();
			}
			
			@Override
			protected String id() {
				return DeviceHandlerMQTT_ElecConnBox.this.id();
			}

			@Override
			protected String getTableTitle() {
				return "Electricity Meters";
			}
			
			protected void addPowerEnergySensor(ElectricityConnection c, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row) {   
				Label voltage = vh.floatLabel("Voltage", // (" + ResourceUtils.getHumanReadableShortName(c) + ")",
						id, c.voltageSensor().reading(), row, "%.1f");
				Label power = vh.floatLabel("Power", // (" + ResourceUtils.getHumanReadableShortName(c) + ")",
						id, c.powerSensor().reading(), row, "%.1f");
				Label lastContact = addLastContact(vh, id, req, row,
							c.powerSensor().reading());
				if (req != null) {
					voltage.setPollingInterval(DEFAULT_POLL_RATE, req);
					power.setPollingInterval(DEFAULT_POLL_RATE, req);
					lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
				}

			}
		};
	}
	

	@Override
	protected Class<? extends ResourcePattern<ElectricityConnectionBox>> getPatternClass() {
		return ElectricityConnectionBoxPattern.class;
	}

	@Override
	protected ResourcePatternAccess advAcc() {
		return appMan.getResourcePatternAccess();
	}

	@Override
	public Collection<Datapoint> getDatapoints(InstallAppDevice installDeviceRes, DatapointService dpService) {
		ElectricityConnectionBox dev = (ElectricityConnectionBox) installDeviceRes.device();
		List<Datapoint> result = new ArrayList<>();
		addConnDatapoints(result, dev.connection(), null, dpService);
		for(ElectricityConnection subConn: dev.connection().subPhaseConnections().getAllElements()) {
			addConnDatapoints(result, subConn, subConn.getName(), dpService);			
		}
		
		return result;
	}
	
	protected void addConnDatapoints(List<Datapoint> result, ElectricityConnection conn, String phase, DatapointService dpService) {
		addDatapoint(conn.voltageSensor().reading(), result, phase, dpService);
		addDatapoint(conn.powerSensor().reading(), result, phase, dpService);
		addDatapoint(conn.reactivePowerSensor().reading(), result, phase, dpService);
		addDatapoint(conn.reactiveAngleSensor().reading(), result, phase, dpService);
		addDatapoint(conn.energySensor().reading(), result, phase, dpService);
		addDatapoint(conn.currentSensor().reading(), result, phase, dpService);
		addDatapoint(conn.frequencySensor().reading(), result, phase, dpService);
		addDecoratorDatapoints(conn.getSubResources(ElectricVoltageSensor.class, false), result, phase, dpService);
		addDecoratorDatapoints(conn.getSubResources(ElectricCurrentSensor.class, false), result, phase, dpService);
		addDecoratorDatapoints(conn.getSubResources(ElectricEnergySensor.class, false), result, phase, dpService);
		addDecoratorDatapoints(conn.getSubResources(PowerSensor.class, false), result, phase, dpService);
		addDecoratorDatapoints(conn.getSubResources(ReactivePowerAngleSensor.class, false), result, phase, dpService);
	}
	
	protected <T extends Sensor> void addDecoratorDatapoints(List<T> valRess, List<Datapoint> result, String phase, DatapointService dpService) {
		for(T sens: valRess) {
			if(!sens.isDecorator())
				continue;
			ValueResource valRes = sens.reading();
			if(valRes instanceof SingleValueResource) {
				final String phaseToUse;
				if(sens instanceof ElectricVoltageSensor) {
					String name = sens.getName();
					if(name.startsWith("voltageSensor") && name.length() > "voltageSensor".length()) {
						String subName = name.substring("voltageSensor".length());
						if(phase == null)
							phaseToUse = subName;
						else phaseToUse = phase+"_"+subName;
					} else
						phaseToUse = phase;
				} else
					phaseToUse = phase;
				addDatapoint((SingleValueResource) sens.reading(), result, phaseToUse, dpService);
			}
		}
	}
	
	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		appDevice.alarms().create();
		ElectricityConnectionBox device = (ElectricityConnectionBox) appDevice.device();
		AlarmingUtiH.setTemplateValues(appDevice, device.connection().powerSensor().reading(),
				0.0f, 4000.0f, 10, 20);
		AlarmingUtiH.setTemplateValues(appDevice, device.connection().voltageSensor().reading(),
				200f, 245f, 10, 20);
		AlarmingUtiH.setTemplateValues(appDevice, device.connection().frequencySensor().reading(),
				49.8f, 50.2f, 1, 20);
		AlarmingUtiH.addAlarmingMQTT(device, appDevice);
	}

}
