package org.smartrplace.driverhandler.more;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.system.guiappstore.config.GatewayData;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.RedirectButton;

//@Component(specVersion = "1.2", immediate = true)
//@Service(DeviceHandlerProvider.class)
public class GatewaySuperiorHandler extends DeviceHandlerBase<GatewayData> {

	private final ApplicationManagerPlus appMan;

	public GatewaySuperiorHandler(ApplicationManagerPlus appMan) {
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
				appSelector.addWidgetsExpert(object, vh, id, req, row, appMan);
			}

			@Override
			protected Class<? extends Resource> getResourceType() {
				return GatewaySuperiorHandler.this.getResourceType();
			}

			@Override
			protected String id() {
				return GatewaySuperiorHandler.this.id();
			}

			@Override
			protected String getTableTitle() {
				return "Gateways On Superior";
			}
			
			public GatewayData addWidgetsInternal(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
					OgemaHttpRequest req, Row row, ApplicationManager appMan) {
				//if(!(object.device() instanceof Thermostat) && (req != null)) return null;
				final GatewayData device;
				if(req == null)
					device = ResourceHelper.getSampleResource(GatewayData.class);
				else
					device = (GatewayData) object.device();
				//if(!(object.device() instanceof Thermostat)) return;
				final String name;
				name = ResourceUtils.getHumanReadableShortName(device);
				vh.stringLabel("Name", id, name, row);
				vh.stringLabel("ID", id, object.deviceId().getValue(), row);
				
				if(req == null) {
					vh.registerHeaderEntry("GUI");
				} else {
					if(device.guiLink().isActive()) {
						RedirectButton guiButton = new RedirectButton(mainTable, id+"guiButton", "GUI",
								device.guiLink().getValue()+"/ogema/index.html", req);
						row.addCell("GUI", guiButton);
					}
				}
				vh.stringEdit("Customer", id, device.customer(), row, alert);

				addSubLocation(object, vh, id, req, row);
				addInstallationStatus(object, vh, id, req, row);
				addComment(object, vh, id, req, row);
				
				return device;
			}
			
		};
	}

	@Override
	public Class<GatewayData> getResourceType() {
		return GatewayData.class;
	}

	@Override
	public Collection<Datapoint> getDatapoints(InstallAppDevice installDeviceRes, DatapointService dpService) {
		List<Datapoint> result = new ArrayList<>();
		GatewayData device = (GatewayData) installDeviceRes.device();
		result.add(dpService.getDataPointStandard(device.warningMessageInterval()));

		return result;
	}

	@Override
	public void initAlarmingForDevice(InstallAppDevice appDevice, HardwareInstallConfig appConfigData) {
		appDevice.alarms().create();
		GatewayData device = (GatewayData) appDevice.device();
		//AlarmingUtiH.setTemplateValues(appDevice, device.h.gitUpdateStatus(),
		//		0, 1000, 30, 120);
	}

	@Override
	protected Class<? extends ResourcePattern<GatewayData>> getPatternClass() {
		return GatewaySuperiorPattern.class;
	}

	@Override
	protected ResourcePatternAccess advAcc() {
		return appMan.getResourcePatternAccess();
	}
}
