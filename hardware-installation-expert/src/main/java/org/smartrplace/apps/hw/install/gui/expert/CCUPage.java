package org.smartrplace.apps.hw.install.gui.expert;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.units.PercentageResource;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.drivers.homematic.xmlrpc.hl.types.HmInterfaceInfo;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.eval.hardware.HmCCUPageUtils;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.virtualdevice.ChartsUtil;
import org.smartrplace.util.virtualdevice.HmSetpCtrlManagerTHSetp;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;

@SuppressWarnings("serial")
public class CCUPage extends MainPage {

	private static final int CCU_MAX_FOR_ALL = 50;
	DeviceTableBase devTable;
	HardwareInstallConfig hwConfig;
	
	@Override
	protected String getHeader() {return "CCU Page";}

	static Boolean isAllAllowed = null;
	@Override
	protected boolean isAllOptionAllowedSuper(OgemaHttpRequest req) {
		if(isAllAllowed == null) {
			int thermNum = appMan.getResourceAccess().getResources(HmInterfaceInfo.class).size();
			isAllAllowed = (thermNum <= CCU_MAX_FOR_ALL);
		}
		return isAllAllowed;
	}
	
	public CCUPage(WidgetPage<?> page, HardwareInstallController controller) {
		super(page, controller, false);
		hwConfig = appMan.getResourceAccess().getResource("hardwareInstallConfig");
		finishConstructor();
	}

	@Override
	protected void finishConstructor() {
		devTable = new DeviceTableBase(page, controller.appManPlus, alert, this, null) {
			
			@Override
			public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {
				final HmInterfaceInfo device;
				if(req == null)
					device = ResourceHelper.getSampleResource(HmInterfaceInfo.class);
				else
					device = (HmInterfaceInfo) object.device().getLocationResource();
				final String name;
				if(device.getLocation().toLowerCase().contains("homematic")) {
					name = "CCU HM:"+ScheduleViewerOpenButtonEval.getDeviceShortId(device.getLocation());
				} else
					name = ResourceUtils.getHumanReadableShortName(device);
				vh.stringLabel("Name", id, name, row);
				vh.stringLabel("ID", id, object.deviceId().getValue(), row);
				
				if(req == null) {
					vh.registerHeaderEntry("DutyCcl");
					vh.registerHeaderEntry("Last Contact");
					vh.registerHeaderEntry("yellow");
					vh.registerHeaderEntry("red");
					vh.registerHeaderEntry("TeachIn");
					vh.registerHeaderEntry("Location");
					vh.registerHeaderEntry("Comment");
					vh.registerHeaderEntry("Plot");
					return;
				}
				Label dutyCycleLb = ChartsUtil.getDutyCycleLabel(device, object, vh, id);
				row.addCell("DutyCcl", dutyCycleLb);
				Label lastContact = addLastContact(vh, id, req, row, device.dutyCycle().reading());
				vh.floatEdit("yellow", id, object.getSubResource(HmSetpCtrlManagerTHSetp.dutyCycleYellowMin, PercentageResource.class),
						row, alert, 0, 1.0f, "Only 0 to 100% allowed");
				vh.floatEdit("red", id, object.getSubResource(HmSetpCtrlManagerTHSetp.dutyCycleRedMin, PercentageResource.class),
						row, alert, 0, 1.0f, "Only 0 to 100% allowed");
				
				HmCCUPageUtils.addTechInModeButton(object, device, vh, id, req, row, appMan, hwConfig);
				vh.stringLabel("Location", id, object.installationLocation(), row);
				vh.stringLabel("Comment", id, object.installationComment(), row);

				dutyCycleLb.setPollingInterval(DEFAULT_POLL_RATE, req);
				lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
				
				DeviceHandlerProviderDP<Resource> pe = controller.dpService.getDeviceHandlerProvider(object);
				final GetPlotButtonResult logResult = MainPage.getPlotButton(id, object, appManPlus.dpService(), appMan, false, vh, row, req, pe,
						ScheduleViewerConfigProvCCUDebug.getInstance(), null);
				row.addCell("Plot", logResult.plotButton);
			}
			
			@Override
			protected String id() {
				return "CCUDetails";
			}
			
			@Override
			public String getTableTitleRaw() {
				return "CCU Detail Data";
			}
			
			@Override
			protected Class<? extends Resource> getResourceType() {
				return HmInterfaceInfo.class;
			}
			
			@Override
			public List<InstallAppDevice> getObjectsInTable(OgemaHttpRequest req) {
				List<InstallAppDevice> all = MainPage.getDevicesSelectedDefault(null, controller, roomsDrop, typeFilterDrop, req);
				List<InstallAppDevice> result = new ArrayList<InstallAppDevice>();
				for(InstallAppDevice dev: all) {
					if(dev.device() instanceof HmInterfaceInfo) {
						result.add(dev);
					}
				}
				return result;
			}
		};
		devTable.triggerPageBuild();
		typeFilterDrop.registerDependentWidget(devTable.getMainTable());
		
	}
	
}
