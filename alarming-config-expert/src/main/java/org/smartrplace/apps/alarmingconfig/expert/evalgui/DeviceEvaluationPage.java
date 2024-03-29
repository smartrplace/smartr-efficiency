package org.smartrplace.apps.alarmingconfig.expert.evalgui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.devicefinder.api.AlarmingExtension;
import org.ogema.devicefinder.api.AlarmingExtension.EvaluationConfigurationExtensionDevice;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.devicefinder.util.DpGroupUtil;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.apps.alarmingconfig.model.eval.EvaluationByAlarmConfig;
import org.smartrplace.apps.alarmingconfig.model.eval.EvaluationByAlarmingOption;
import org.smartrplace.apps.alarmingconfig.model.eval.EvaluationInterval;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gui.filtering.GenericFilterFixedSingle;
import org.smartrplace.gui.filtering.GenericFilterOption;
import org.smartrplace.gui.filtering.SingleFiltering.OptionSavingMode;
import org.smartrplace.gui.filtering.SingleFilteringDirect;
import org.smartrplace.hwinstall.basetable.HardwareTablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.directresourcegui.GUIHelperExtension;

import de.iwes.util.resource.OGEMAResourceCopyHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.checkbox.SimpleCheckbox;
import de.iwes.widgets.resource.widget.calendar.DatepickerTimeResource;
import de.iwes.widgets.resource.widget.textfield.BooleanResourceCheckbox;
import de.iwes.widgets.resource.widget.textfield.ValueResourceTextField;

@SuppressWarnings("serial")
public class DeviceEvaluationPage extends HardwareTablePage {
	protected Button reCalcAllButton;
	protected DatepickerTimeResource datePickerEvalStart;
	protected DatepickerTimeResource showStart;
	protected DatepickerTimeResource showEnd;
	
	protected final AlarmingConfigAppController controller;
	protected final EvaluationByAlarmConfig evalData;
	final SimpleCheckbox rawSourceValues;
	
	//Quick hack
	//TODO: In the future we have to offer the AlarmingExtensions with #getEvalConfigExtension implemented
	//in a dropdown
	final private ThermPlusEvaluation alarmingExtensionSelected;
	protected AlarmingExtension getAlarmingEvalSelected(OgemaHttpRequest req) {
		return alarmingExtensionSelected;
	}
	protected EvaluationByAlarmingOption getOptionSelected(OgemaHttpRequest req) {
		List<EvaluationByAlarmingOption> allOpts = evalData.configOptionsToTest().getAllElements();
		Class<?> type = alarmingExtensionSelected.getEvalConfigExtension().getType();
		for(EvaluationByAlarmingOption opt: allOpts) {
			if(type.isAssignableFrom(opt.getResourceType()) && opt.isSelected().getValue())
				return opt;
		}
		return null;
	}
	
	protected int getTopTableLines() {
		return 2;
	}
	
	@Override
	protected boolean offerAddRoomButton() {
		return false;
	}
	
	
	@Override
	protected String getHeader() {
		return "7. Thermostat Plus Evaluation Page";
	}
	
	public DeviceEvaluationPage(WidgetPage<?> page, AlarmingConfigAppController controller) {
		super(page, controller.appManPlus, controller.accessAdminApp, controller.hwTableData, false);
		this.controller = controller;
		evalData = controller.hwTableData.appConfigData.getSubResource("evalData", EvaluationByAlarmConfig.class);
		alarmingExtensionSelected = new ThermPlusEvaluation(controller.tsProcAl, controller.dpService, evalData.configOptionsToTest());
		finishConstructor();
		
		getCommitButton();
		
		datePickerEvalStart = new DatepickerTimeResource(page, "datePickerEvalStart");
		long now = appMan.getFrameworkTime();
		ValueResourceHelper.setIfNew(evalData.evaluationStart(), now - 30*TimeProcUtil.DAY_MILLIS);
		if(!evalData.showIntervals().exists() || evalData.showIntervals().size() < 1) {
			evalData.showIntervals().create();
			EvaluationInterval intv = evalData.showIntervals().add();
			long evalBasicInterval = controller.hwTableData.appConfigData.basicEvalInterval().getValue();
			ValueResourceHelper.setCreate(intv.start(), now-evalBasicInterval);
			ValueResourceHelper.setCreate(intv.end(), now);
			ValueResourceHelper.setCreate(intv.name(), "Start interval to edit");
			ValueResourceHelper.setCreate(intv.end(), now);
		}
		evalData.activate(true);
		
		datePickerEvalStart.selectDefaultItem(evalData.evaluationStart());
		topTable.setContent(0, 6, datePickerEvalStart);
		
		showStart = new DatepickerTimeResource(page, "showStart");
		showEnd = new DatepickerTimeResource(page, "showEnd");
		ValueResourceTextField<StringResource> editName = new ValueResourceTextField<StringResource>(page, "editName");
		
		SingleFilteringDirect<EvaluationInterval> intervalDrop = new SingleFilteringDirect<EvaluationInterval>(page, "intervalDrop",
				OptionSavingMode.GENERAL, 10000, false) {

					@Override
					protected List<GenericFilterOption<EvaluationInterval>> getOptionsDynamic(OgemaHttpRequest req) {
						List<EvaluationInterval> all = appMan.getResourceAccess().getResources(EvaluationInterval.class);
						List<GenericFilterOption<EvaluationInterval>> result = new ArrayList<>();
						for(EvaluationInterval res: all) {
							result.add(new GenericFilterFixedSingle<EvaluationInterval>(res, ResourceUtils.getHumanReadableShortName(res)));
						}
						return result;
					}

					@Override
					protected long getFrameworkTime() {
						return appMan.getFrameworkTime();
					}

					@Override
					public void updateDependentWidgets(OgemaHttpRequest req) {
						EvaluationInterval item = getSelectedItemDirect(req);
						showStart.selectItem(item.start(), req);
						showEnd.selectItem(item.end(), req);
						editName.selectItem(item.name(), req);
					}
		};
		/*ResourceDropdown<EvaluationInterval> intervalDrop = new ResourceDropdown<EvaluationInterval>(page, "intervalDrop", false,
				EvaluationInterval.class, UpdateMode.AUTO_ON_GET, appMan.getResourceAccess()) {
			@Override
			public void updateDependentWidgets(OgemaHttpRequest req) {
				EvaluationInterval item = getSelectedItem(req);
				showStart.selectItem(item.start(), req);
				showEnd.selectItem(item.end(), req);
				editName.selectItem(item.name(), req);
			}
		};*/
		topTable.setContent(1, 0, intervalDrop);
		
		intervalDrop.registerDependentWidget(showStart);
		topTable.setContent(1, 5, showStart);
		intervalDrop.registerDependentWidget(showEnd);
		topTable.setContent(1, 6, showEnd);
		intervalDrop.registerDependentWidget(editName);
		topTable.setContent(1, 3, editName);

		Button deleteButton = new Button(page, "deleteItvButton", "Delete") {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				if(evalData.showIntervals().size() < 2)
					return;
				EvaluationInterval item = intervalDrop.getSelectedItemDirect(req);
				item.delete();
			}
		};
		deleteButton.registerDependentWidget(intervalDrop);
		topTable.setContent(1, 2, deleteButton);

		Button copyButton = new Button(page, "copyItvButton", "Copy") {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				EvaluationInterval item = intervalDrop.getSelectedItemDirect(req);
				EvaluationInterval newItem = OGEMAResourceCopyHelper.copySubResourceIntoResourceList(evalData.showIntervals(), item, appMan, true);
				newItem.name().setValue(item.getName()+"(new)");
			}
		};
		copyButton.registerDependentWidget(intervalDrop);
		topTable.setContent(1, 1, copyButton);
		
		rawSourceValues = new SimpleCheckbox(page, "rawSourceValues", " plot raw values");
		topTable.setContent(0, 4, rawSourceValues);
		
		//TODO: This table must depend on the AlarmingExtension selection in the future
		//See HardwareTablePage#updateTables() for the steps required
		AlarmingExtension eval = getAlarmingEvalSelected(null);
		EvaluationConfigurationExtensionDevice<?> evalExt = eval.getEvalConfigExtension();
		
		getConfigTable(eval.id(), evalExt, evalData);
	}
	
	protected <T extends EvaluationByAlarmingOption> DeviceTableRaw<T, T> getConfigTable(String id,
			EvaluationConfigurationExtensionDevice<T> evalExt,
			EvaluationByAlarmConfig evalData) {
		DeviceTableRaw<T, T> configTable2 = new DeviceTableRaw<T, T>(page, appManPlus, alert,
				ResourceHelper.getSampleResource(evalExt.getType())) {

			@Override
			public void addWidgets(T object, ObjectResourceGUIHelper<T, T> vh, String id,
					OgemaHttpRequest req, Row row, ApplicationManager appMan) {
				if(req == null) {
					vh.registerHeaderEntry("select");
				} else {
					@SuppressWarnings("unchecked")
					Map<String, BooleanResourceCheckbox> allBoolEdit = (Map<String, BooleanResourceCheckbox>) mainTable.getData(req).getSessionDataFlex("allBoolEdit");
					if(allBoolEdit == null) {
						allBoolEdit = new HashMap<>();
						mainTable.getData(req).putSessionDataFlex("allBoolEdit", allBoolEdit);
					}
					final Map<String, BooleanResourceCheckbox> allBoolEditFinal = allBoolEdit;
					
					BooleanResourceCheckbox check = new BooleanResourceCheckbox(mainTable, "check"+id, "", req) {
						@Override
						public void onPOSTComplete(String data, OgemaHttpRequest req) {
							super.onPOSTComplete(data, req);
							boolean val = getSelectedItem(req).getValue();
							if(!val)
								return;
							for(Entry<String, BooleanResourceCheckbox> other: allBoolEditFinal.entrySet()) {
								if(other.getKey().equals(id))
									continue;
								other.getValue().getSelectedItem(req).setValue(false);
							}
						}
					};
					check.selectDefaultItem(object.isSelected());
					//.booleanEdit("select", id, object.isSelected(), row, 0);
					for(Entry<String, BooleanResourceCheckbox> other: allBoolEdit.entrySet()) {
						if(other.getKey().equals(id))
							continue;
						other.getValue().registerDependentWidget(check);
						check.registerDependentWidget(other.getValue());
					}
					allBoolEdit.put(id, check);
					row.addCell("select", check);
				}
				evalExt.addWidgetsConfigRow(object, vh, id, req, row, alert);
				
				GUIHelperExtension.addCopyButton(evalData.configOptionsToTest(), object, mainTable, id, alert, row, vh, req, appMan, evalExt.getType());
				GUIHelperExtension.addDeleteButton(evalData.configOptionsToTest(), object, mainTable, id, alert, row, vh, req);
				
			}

			@Override
			protected String id() {
				return "evalConfigTable"+id;
			}

			@Override
			public String getTableTitleRaw() {
				return "Configuration";
			}

			@SuppressWarnings("unchecked")
			@Override
			public Collection<T> getObjectsInTable(OgemaHttpRequest req) {
				Class<? extends Resource> type = evalExt.getType();
				List<T> result = new ArrayList<>();
				List<EvaluationByAlarmingOption> allOpts = evalData.configOptionsToTest().getAllElements();
				for(EvaluationByAlarmingOption opt: allOpts) {
					if(type.isAssignableFrom(opt.getResourceType()))
						result.add((T) opt);
				}
				return result ;
			}
		};
		configTable2.triggerPageBuild();
		return configTable2;
	}

	@Override
	protected OgemaWidget getCommitButton() {
		if(reCalcAllButton == null) {
			reCalcAllButton = new Button(page, "reCalcAllButton", "(Re)calculate all") {
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					//AlarmingExtension eval = getAlarmingEvalSelected(req);
					List<InstallAppDevice> devices = getDevicesSelected(null, req);
					updateEval(devices, req);
					alert.showAlert("Recalculated...", true, req);
				}
				
			};
			reCalcAllButton.registerDependentWidget(alert);
			reCalcAllButton.registerDependentWidget(reCalcAllButton);
			
		}
		return reCalcAllButton;
	}

	@SuppressWarnings("unchecked")
	public <T extends EvaluationByAlarmingOption> void updateEval(List<InstallAppDevice> devices,
			//EvaluationConfigurationExtensionDevice<T> evalExt,
			OgemaHttpRequest req) {
		AlarmingExtension eval = getAlarmingEvalSelected(req);
		EvaluationConfigurationExtensionDevice<T> evalExt = (EvaluationConfigurationExtensionDevice<T>) eval.getEvalConfigExtension();
		long start = datePickerEvalStart.getDateLong(req);
		long end = appMan.getFrameworkTime();
		EvaluationByAlarmingOption config = getOptionSelected(req);
		if(config == null) {
			alert.showAlert("No configuration selected", false, req);
			System.out.println("  ERROR in evaluation: No configuration selected");
			return;
		}
		evalExt.updateEvaluation(devices, (T)config, start, end, true);					
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
		return new EvaluationDeviceTableThermPlus(page, controller, alert, pageTitle, resData, reCalcAllButton, appSelector, pe, this) {

			@Override
			protected AlarmingExtension getAlarmingEvalSelected(OgemaHttpRequest req) {
				return DeviceEvaluationPage.this.getAlarmingEvalSelected(req);
			}

			@Override
			protected long getStart(OgemaHttpRequest req) {
				return showStart.getDateLong(req);
			}

			@Override
			protected long getEnd(OgemaHttpRequest req) {
				return showEnd.getDateLong(req);
			}
			
		};
		
	}

	@Override
	public <T extends Resource> List<InstallAppDevice> getDevices(DeviceHandlerProviderDP<?> tableProvider,
			boolean includeTrash) {
		List<InstallAppDevice> result = new ArrayList<>();
		for(InstallAppDevice install: resData.appConfigData.knownDevices().getAllElements()) {
			if((!includeTrash) && install.isTrash().getValue())
				continue;
			//if(tableProvider == null) {
			//	result.add(install);
			//	continue;
			//}
			if(!("org.smartrplace.homematic.devicetable.DeviceHandlerThermostat".equals(install.devHandlerInfo().getValue())
					|| "org.smartrplace.driverhandler.more.VirtualThermostatDeviceHandler".equals(install.devHandlerInfo().getValue())))	{
				continue;
			}
			if((tableProvider == null) || (tableProvider.id().equals(install.devHandlerInfo().getValue())))	{
				result.add(install);
			}
		}
		return result;
	}
	
	@Override
	protected boolean isObjectsInTableEmpty(DeviceHandlerProvider<?> pe, OgemaHttpRequest req) {
		if("org.smartrplace.homematic.devicetable.DeviceHandlerThermostat".equals(pe.id()))
			return false;
		if("org.smartrplace.driverhandler.more.VirtualThermostatDeviceHandler".equals(pe.id()))
			return false;
		return true;
	}
}
