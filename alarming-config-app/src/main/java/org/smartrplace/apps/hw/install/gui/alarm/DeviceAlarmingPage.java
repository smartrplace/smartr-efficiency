package org.smartrplace.apps.hw.install.gui.alarm;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.devicefinder.util.DpGroupUtil;
import org.ogema.model.extended.alarming.DevelopmentTask;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.util.extended.eval.widget.BooleanResourceButton;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.apps.alarmingconfig.gui.MainPage;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.hwinstall.basetable.HardwareTablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.dropdown.Dropdown;
import de.iwes.widgets.html.form.dropdown.DropdownOption;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;

@SuppressWarnings("serial")
public class DeviceAlarmingPage extends HardwareTablePage {
	protected Button commitButton;
	protected final AlarmingConfigAppController controller;
	
	protected int getTopTableLines() {
		return 2;
	}
	
	@Override
	protected boolean offerAddRoomButton() {
		return false;
	}
	
	@Override
	protected String getHeader() {
		return "1. Device Alarming Overview";
	}
	
	public DeviceAlarmingPage(WidgetPage<?> page, AlarmingConfigAppController controller) {
		this(page, controller, true);
	}
	public DeviceAlarmingPage(WidgetPage<?> page, AlarmingConfigAppController controller, boolean triggerFinishConstructorAutomatically) {
		super(page, controller.appManPlus, controller.accessAdminApp, controller.hwTableData, triggerFinishConstructorAutomatically, FilterMode.KNOWN_FAULTS);
		this.controller = controller;
		
		getCommitButton(); //make sure it is initialized

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
		
		topTable.setContent(1, 0, alarmingGeneralButton);
	}

	@Override
	protected OgemaWidget getCommitButton() {
		if(commitButton == null) {
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
			
		}
		return commitButton;
	}
	
	@Override
	public void updateTables() {
		synchronized(tableProvidersDone) {
		Collection<DeviceHandlerProvider<?>> providers = devHandAcc.getTableProviders().values();
		if(devHandAcc != null) for(DeviceHandlerProvider<?> pe: providers) {
			//if(isObjectsInTableEmpty(pe))
			//	continue;
			String id = pe.id();
			if(tableProvidersDone.contains(id))
				continue;
			tableProvidersDone.add(id);
			DeviceTableBase tableLoc = getDeviceTable(page, alert, this, pe);
			tableLoc.triggerPageBuild();
			typeFilterDrop.registerDependentWidget(tableLoc.getMainTable());
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
		return new AlarmingDeviceTableBase(page, appManPlus, alert, pageTitle, resData, commitButton, appSelector, pe) {
			protected void addAdditionalWidgets(final InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
					OgemaHttpRequest req, Row row, final ApplicationManager appMan,
					PhysicalElement device, final InstallAppDevice template) {
				if(req == null) {
					vh.registerHeaderEntry("Select Template");
					vh.registerHeaderEntry("Special Settings(Dev)");
					vh.registerHeaderEntry("Alarming delay");
					return;
				}
				
				ButtonConfirm selectTemplButton = new ButtonConfirm(vh.getParent(), WidgetHelper.getValidWidgetId("selectTemplBut"+id), req) {
					@Override
					public void onGET(OgemaHttpRequest req) {
						if(template != null && object.equalsLocation(template)) {
							setText("is Template", req);
							disable(req);
						} else {
							if(DeviceTableRaw.isTemplate(object, null)) {
								if(Boolean.getBoolean("org.smartrplace.apps.hw.install.gui.alarm.cleanup.brokentemplate")) {
									object.isTemplate().delete();
									setText("Select as Template (**--)", req);
								} else
									setText("Select as Template (**)", req);
							} else
								setText("Select as Template", req);
							enable(req);							
						}
					}
					
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						if(template != null)
							DeviceTableRaw.setTemplateStatus(template, false);
						DeviceTableRaw.setTemplateStatus(object, true);
					}
				};
				selectTemplButton.setDefaultConfirmMsg("Really select as template "+object.device().getLocation()+" ?");
				selectTemplButton.setDefaultText("Select as Template");
				row.addCell(WidgetHelper.getValidWidgetId("Select Template"), selectTemplButton);
				
				TemplateDropdown<DevelopmentTask> devTaskDrop = new DevelopmentTaskDropdown(object, resData, appMan, controller,
						vh.getParent(), "devTaskDrop"+id, req);
				row.addCell(WidgetHelper.getValidWidgetId("Special Settings(Dev)"), devTaskDrop);
				final Dropdown alarmingDelayDrop = new Dropdown(mainTable, "alarmingDelayDrop"+id, req) {
					
					@Override
					public void onGET(OgemaHttpRequest req) {
						final int delayHours = (int)(object.minimumIntervalBetweenNewValues().getValue()/60);
						selectSingleOption(String.valueOf(delayHours), req); // find closest option matching the delay?
					}
					
					@Override
					public void onPOSTComplete(String arg0, OgemaHttpRequest req) {
						final String selected = getSelectedValue(req);
						try {
							final int delay = Integer.parseInt(selected);
							if (delay == 0) {
								object.minimumIntervalBetweenNewValues().setValue(-1);
							} else {
								ValueResourceHelper.setCreate(object.minimumIntervalBetweenNewValues(), delay*60);
							}
						} catch (NumberFormatException ignore) {}
					}
					
				};
				alarmingDelayDrop.setDefaultOptions(IntStream.builder().add(0).add(6).add(12).add(24).add(48).add(72).add(168).build()
					.mapToObj(i -> new DropdownOption(i > 0 ? String.valueOf(i) : "", i > 0 ? i + "h" : "--", i == 0))
					.collect(Collectors.toList()));
				row.addCell(WidgetHelper.getValidWidgetId("Alarming delay"), alarmingDelayDrop);
				
			}			
		};
		
	}
}
