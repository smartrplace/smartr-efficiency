package org.smartrplace.apps.hw.install.gui.alarm;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.devicefinder.util.DpGroupUtil;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.apps.alarmingconfig.gui.MainPage;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.autoconfig.api.InitialConfig;
import org.smartrplace.hwinstall.basetable.BooleanResourceButton;
import org.smartrplace.hwinstall.basetable.HardwareTablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;

@SuppressWarnings("serial")
public class DeviceAlarmingPage extends HardwareTablePage {
	protected final Button commitButton;
	
	@Override
	protected String getHeader() {
		return "2. Device Alarming Overview";
	}
	
	public DeviceAlarmingPage(WidgetPage<?> page, AlarmingConfigAppController controller) {
		super(page, controller.appManPlus, controller.accessAdminApp, controller.hwTableData);
		
		StaticTable alarmingTopTable = new StaticTable(1, 7, new int[] {2, 2, 1, 2, 1, 2, 2});
		
		commitButton = new Button(page, "commitButton", "Commit") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				if(resData.appConfigData.isAlarmingActive().getValue()) {
					enable(req);
					if(MainPage.hasOpenCommits)
						setStyle(ButtonData.BOOTSTRAP_RED, req);
					else
						setStyle(ButtonData.BOOTSTRAP_DEFAULT, req);
				} else {
					disable(req);
					setStyle(ButtonData.BOOTSTRAP_DEFAULT, req);
				}
				super.onGET(req);
			}
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				if(MainPage.alarmingUpdater != null) {
					MainPage.alarmingUpdater.updateAlarming();
					MainPage.hasOpenCommits = false;
					alert.showAlert("Updated and restarted alarming", true, req);
				} else
					alert.showAlert("Could not find alarmingManagement for update", false, req);				
			}
		};
		commitButton.registerDependentWidget(alert);
		commitButton.registerDependentWidget(commitButton);

		BooleanResourceButton alarmingGeneralButton = new BooleanResourceButton(page, "alarmingGeneralButton",
				"Alarming", resData.appConfigData.isAlarmingActive(), ButtonData.BOOTSTRAP_GREEN, ButtonData.BOOTSTRAP_RED) {
			@Override
			public void onGET(OgemaHttpRequest req) {
				if(!res.getValue()) {
					setText("System alarming inactive", req);
				} else {
					setText("System alarming active", req);					
				}
				super.onGET(req);
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				super.onPOSTComplete(data, req);
				if(MainPage.alarmingUpdater != null) {
					//MainPage.alarmingUpdater.updateAlarming();
					MainPage.hasOpenCommits = false;
					alert.showAlert("Updated and restarted alarming", true, req);
				} else
					alert.showAlert("Could not find alarmingManagement for update", false, req);				
			}
		};
		
		alarmingTopTable.setContent(0, 1, alarmingGeneralButton);
		alarmingTopTable.setContent(0, 3, commitButton);
		
		page.append(alarmingTopTable);
	}

	@Override
	public void updateTables() {
		synchronized(tableProvidersDone) {
		if(devHandAcc != null) for(DeviceHandlerProvider<?> pe: devHandAcc.getTableProviders().values()) {
			//if(isObjectsInTableEmpty(pe))
			//	continue;
			String id = pe.id();
			if(tableProvidersDone.contains(id))
				continue;
			tableProvidersDone.add(id);
			DeviceTableBase tableLoc = getDeviceTable(page, alert, this, pe);
			tableLoc.triggerPageBuild();
			installFilterDrop.registerDependentWidget(tableLoc.getMainTable());
			roomsDrop.registerDependentWidget(tableLoc.getMainTable());
			roomsDrop.getFirstDropdown().registerDependentWidget(tableLoc.getMainTable());
			subTables.add(new SubTableData(pe, tableLoc));
			
		}
		}
	}

	protected DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert, InstalledAppsSelector appSelector,
			final DeviceHandlerProvider<?> pe) {
		final String pageTitle;
		DatapointGroup grp = DpGroupUtil.getDeviceTypeGroup(pe, appManPlus.dpService(), false);
		if(grp != null)
			pageTitle = "Devices of type "+ grp.label(null);
		else
			pageTitle = "Devices of type "+ pe.label(null);
		return new DeviceTableBase(page, appManPlus, alert, appSelector, pe) {
			@Override
			protected String pid() {
				return WidgetHelper.getValidWidgetId(pe.id());
			}
			
			@Override
			public void addWidgets(InstallAppDevice object,
					ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req,
					Row row, ApplicationManager appMan) {
				id = id + pid();  // avoid duplicates for now
				addWidgetsInternal(object, vh, id, req, row, appMan);
				//appSelector.addWidgetsExpert(object, vh, id, req, row, appMan);
			}

			@Override
			protected Class<? extends Resource> getResourceType() {
				return pe.getResourceType();
			}

			@Override
			protected String id() {
				return pe.id();
			}

			@Override
			protected String getTableTitle() {
				return pageTitle;
			}

			public PhysicalElement addWidgetsInternal(final InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
					OgemaHttpRequest req, Row row, final ApplicationManager appMan) {

				addNameWidget(object, vh, id, req, row, appMan);
				PhysicalElement device = object.device();
				final InstallAppDevice template;
				if(req == null) {
					vh.registerHeaderEntry("Active Alarms");
					vh.registerHeaderEntry("Alarm Control");
					template = null;
				} else {
					int[] alNum = AlarmingConfigUtil.getActiveAlarms(object);
					/*int alNum = 0;
					int alStatusNum = 0;
					for(AlarmConfiguration ac: object.alarms().getAllElements()) {
						if(ac.sendAlarm().getValue()) {
							alNum++;
							IntegerResource status = AlarmingConfigUtil.getAlarmStatus(ac.sensorVal().getLocationResource());
							if(status != null && status.getValue() > 0)
								alStatusNum++;
						}
					}
					vh.stringLabel("Active Alarms", id, String.format("%d / %d", alStatusNum, alNum), row);*/
					vh.stringLabel("Active Alarms", id, String.format("%d / %d", alNum[0], alNum[1]), row);

					template = AlarmingConfigUtil.getTemplate(object, appManPlus);
					if(template != null) {
						Boolean templateStatus = AlarmingConfigUtil.getAlarmingStatus(template, template);
						if(templateStatus == null)
							throw new IllegalStateException("Template status cannot be null!");
						Button perm = new Button(mainTable, WidgetHelper.getValidWidgetId("perm_"+id), "", req) {
							private static final long serialVersionUID = 1L;
							@Override
							public void onGET(OgemaHttpRequest req) {
								Boolean status = AlarmingConfigUtil.getAlarmingStatus(template, object);
								if(object.equalsLocation(template)) {
									if(status == null)
										throw new IllegalStateException("Template status cannot be null!");
									setText(status ? "✓ Template": "✕ Templ.inactive", req);
									if (status) {
										setStyle(ButtonData.BOOTSTRAP_GREEN, req);
										disable(req);
									} else {
										setStyle(ButtonData.BOOTSTRAP_RED, req);
										enable(req);
									}
									setToolTip("Template is " + (status ? "active" : "inactive") + ".", req);
									return;
								}
								if(templateStatus)
									enable(req);
								else
									disable(req);
								if (status == null) {
									setStyle(ButtonData.BOOTSTRAP_DEFAULT, req);
									setText("(✓ special)", req);
									setToolTip("Alarming is " + "special.", req);
									
								} else {
									//setGlyphicon(status ? Glyphicons.CHECK : Glyphicons.OFF, req);
									setText(status ? "✓ active": "✕ inactive", req);
									if (status) {
										setStyle(ButtonData.BOOTSTRAP_GREEN, req);
									} else {
										setStyle(ButtonData.BOOTSTRAP_RED, req);
									}
									setToolTip("Alarming is " + (status ? "active" : "inactive") + ".", req);
								}
							}
							
							@Override
							public void onPOSTComplete(String data, OgemaHttpRequest req) {
								Boolean status = AlarmingConfigUtil.getAlarmingStatus(template, object);
								MainPage.hasOpenCommits = true;
								if(status == null || status)
									AlarmingConfigUtil.deactivateAlarms(object);
								else
									AlarmingConfigUtil.copySettings(template, object, appManPlus.appMan(), false);
							}
						};
						row.addCell(WidgetHelper.getValidWidgetId("Alarm Control"), perm);
						perm.registerDependentWidget(perm);
						perm.registerDependentWidget(commitButton);
					}
				}					
				Room deviceRoom = device.location().room();
				addRoomWidget(vh, id, req, row, appMan, deviceRoom);
				addSubLocation(object, vh, id, req, row);
				if(req == null) {
					vh.registerHeaderEntry("Select Template");
					return device;
				}
				
				ButtonConfirm selectTemplButton = new ButtonConfirm(vh.getParent(), WidgetHelper.getValidWidgetId("selectTemplBut"+id), req) {
					@Override
					public void onGET(OgemaHttpRequest req) {
						if(template != null && object.equalsLocation(template)) {
							setText("is Template", req);
							disable(req);
						} else {
							setText("Select as Template", req);
							enable(req);							
						}
					}
					
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						if(template != null)
							DeviceTableRaw.setTemplateStatus(template, null, false);
						DeviceTableRaw.setTemplateStatus(object, pe, true);
						//if(template != null)
						//	template.isTemplate().deactivate(false);
						//ValueResourceHelper.setCreate(object.isTemplate(), pe.id());
						//if(!object.isTemplate().isActive())
						//	object.isTemplate().activate(false);
					}
				};
				selectTemplButton.setDefaultConfirmMsg("Really select as template "+object.device().getLocation()+" ?");
				selectTemplButton.setDefaultText("Select as Template");
				row.addCell(WidgetHelper.getValidWidgetId("Select Template"), selectTemplButton);

				/*addInstallationStatus(object, vh, id, req, row);
				//addComment(object, vh, id, req, row);
				if(req != null) {
					String text = getHomematicCCUId(object.device().getLocation());
					vh.stringLabel("RT", id, text, row);
				} else
					vh.registerHeaderEntry("RT");*/
				
				return device;
			}
			public PhysicalElement addNameWidget(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
					OgemaHttpRequest req, Row row, ApplicationManager appMan) {
				final PhysicalElement device;
				if(req == null)
					device = ResourceHelper.getSampleResource(PhysicalElement.class);
				else
					device = object.device().getLocationResource();
				DatapointGroup devGrp = DpGroupUtil.getDeviceGroup(device.getLocation(), appManPlus.dpService(), false);
				String name;
				if(devGrp != null)
					name = devGrp.label(null);
				else
					name = ResourceUtils.getHumanReadableShortName(device);
				if(!InitialConfig.isInitDone(object.deviceId().getValue()+pe.getInitVersion(), resData.appConfigData.initDoneStatus()))
					name += "*";
				vh.stringLabel("Name", id, name, row);
				vh.stringLabel("ID", id, object.deviceId().getValue(), row);
				return device;
			}	
		};
		
	}
}
