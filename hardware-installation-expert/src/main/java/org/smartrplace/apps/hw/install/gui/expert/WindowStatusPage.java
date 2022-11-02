package org.smartrplace.apps.hw.install.gui.expert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.devicefinder.api.PropType;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.label.Label;

@SuppressWarnings("serial")
@Deprecated
public class WindowStatusPage extends BatteryPage {
	public static final long DEFAULT_POLL_RATE = 5000;

	DeviceTableBase devTable;
	
	@Override
	public String getHeader() {return "Thermostat Window Overview";}

	public WindowStatusPage(WidgetPage<?> page, HardwareInstallController controller) {
		super(page, controller);
		//finishConstructor();
		
		/*Button triggerUpdate = new Button(page, "triggerUpdateBut", "Update all") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				lastUpds = lastHmParamUpdate(hmDevice));
			}
		};
		topTable.setContent(0, 4, triggerUpdate);*/
	}

	protected Label addParamLabel(PhysicalElement device, PropType type, String colName,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
			String id, OgemaHttpRequest req, Row row) {
		if(req == null) {
			vh.registerHeaderEntry(colName);
			return null;
		}
		final SingleValueResource sres = PropType.getHmParam(device, type, true);
		Label result = new Label(vh.getParent(), WidgetHelper.getValidWidgetId("paramLabel"+type.toString()+id), req) {
			@Override
			public void onGET(OgemaHttpRequest req) {
				String text = ValueResourceUtils.getValue(sres);
				setText(text, req);
			}
		};
		result.setPollingInterval(DEFAULT_POLL_RATE, req);
		row.addCell(WidgetHelper.getValidWidgetId(colName), result);
		return result;
	}
	
	@Override
	protected void finishConstructor() {
		devTable = new DeviceTableBase(page, controller.appManPlus, alert, this, null) {
			//@Override
			//public String getLineId(InstallAppDevice object) {
			//	return object.getName()+"_BAT";
			//}
			
			@Override
			public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {
				final PhysicalElement device2 = addNameWidget(object, vh, id, req, row, appMan).getLocationResource();
				
				addParamLabel(device2, PropType.THERMOSTAT_WINDOWOPEN_MODE, "Mode", vh, id, req, row);
				addParamLabel(device2, PropType.THERMOSTAT_WINDOWOPEN_TEMPERATURE, "Setpoint", vh, id, req, row);
				addParamLabel(device2, PropType.THERMOSTAT_WINDOWOPEN_MINUTES, "Minutes", vh, id, req, row);
				addParamLabel(device2, PropType.THERMOSTAT_VALVE_MAXPOSITION, "ValveMax", vh, id, req, row);

				if(req == null) {
					vh.registerHeaderEntry("Last Status");
				} else {
					Button triggerUpdate = new Button(vh.getParent(), "triggerUpdateBut"+id, "Update", req) {
						@Override
						public void onGET(OgemaHttpRequest req) {
							long[] lastUpds = PropType.lastHmParamUpdate(device2);
							String text;
							if(lastUpds != null)
								text = "Upd("+StringFormatHelper.getFormattedAgoValue(controller.appMan, lastUpds[0])+
									"/"+StringFormatHelper.getFormattedAgoValue(controller.appMan, lastUpds[0])+")";
							else
								text = "Update";
							setText(text, req);
						}
						
						@Override
						public void onPOSTComplete(String data, OgemaHttpRequest req) {
							PropType.triggerHmUpdate(device2);
						}
					};
					triggerUpdate.setPollingInterval(DEFAULT_POLL_RATE, req);
					row.addCell(WidgetHelper.getValidWidgetId("Last Status"), triggerUpdate);
				}
				
				Room deviceRoom = device2.location().room();
				addRoomWidget(vh, id, req, row, appMan, deviceRoom);
			 	addSubLocation(object, vh, id, req, row);
			}
			
			@Override
			public PhysicalElement addNameWidget(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
					OgemaHttpRequest req, Row row, ApplicationManager appMan) {
				return addNameWidgetStatic(object, vh, id, req, row, appManPlus);
			}	

			@Override
			protected String id() {
				return "ThermostatWindowOverview";
			}
			
			@Override
			public String getTableTitleRaw() {
				return "Thermostat Property Overview";
			}
			
			@Override
			protected Class<? extends Resource> getResourceType() {
				return PhysicalElement.class;
			}
			
			@Override
			public List<InstallAppDevice> getObjectsInTable(OgemaHttpRequest req) {
				Collection<InstallAppDevice> result = controller.dpService.managedDeviceResoures(Thermostat.class);
				/*List<InstallAppDevice> all = MainPage.getDevicesSelectedDefault(null, controller, roomsDrop, typeFilterDrop, req);
				//List<InstallAppDevice> all = appSelector.getDevicesSelected();
				List<InstallAppDevice> result = new ArrayList<InstallAppDevice>();
				for(InstallAppDevice dev: all) {
					PhysicalElement device2 = dev.device().getLocationResource();
					if((device2 instanceof Thermostat))
						result.add(dev);
				}*/
				return new ArrayList<InstallAppDevice>(result);
			}
		};
		devTable.triggerPageBuild();
		typeFilterDrop.registerDependentWidget(devTable.getMainTable());
		
	}
	
}
