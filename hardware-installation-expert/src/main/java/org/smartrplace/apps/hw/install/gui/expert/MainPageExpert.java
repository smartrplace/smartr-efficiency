package org.smartrplace.apps.hw.install.gui.expert;

import java.util.Arrays;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.externalviewer.extensions.DefaultScheduleViewerConfigurationProviderExtended;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.timeseries.eval.simple.mon3.std.StandardEvalAccess;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;
import org.smartrplace.widget.extensions.GUIUtilHelper;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.WidgetStyle;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.label.LabelData;

@SuppressWarnings("serial")
public class MainPageExpert extends MainPage {

	private static final String LOG_ALL = "Log All";
	private static final String LOG_NONE = "Log None";
	private static final String DELETE = "Delete";
	private static final String RESET = "Reset";
	private static final String TRASH = "Mark as Trash";
	private static final String TRASH_RESET = "Back from Trash";
	private static final String MAKE_TEMPLATE = "Make Template";
	private static final String APPLY_TEMPLATE = "Apply Template To Devices";
	private static final String APPLY_DEFAULT_ALARM = "Apply Default Alarm Settings";
	private static final List<String> ACTIONS = Arrays.asList(new String[] {LOG_ALL, LOG_NONE, DELETE, RESET, TRASH, MAKE_TEMPLATE, APPLY_DEFAULT_ALARM});
	private static final List<String> ACTIONS_TEMPLATE = Arrays.asList(new String[] {LOG_ALL, LOG_NONE, DELETE, RESET, TRASH, APPLY_TEMPLATE, APPLY_DEFAULT_ALARM});
	private static final List<String> ACTIONS_TRASH = Arrays.asList(new String[] {LOG_ALL, LOG_NONE, DELETE, RESET, TRASH_RESET, MAKE_TEMPLATE, APPLY_DEFAULT_ALARM});
	private static final String GAPS_LABEL = "Qual4d/perTs/SetpReact/perTs";

	protected final boolean isTrashPage;
	protected final ShowModeHw showMode;
	
	@Override
	public String getHeader() {return "Device Setup and Configuration Expert "+((showMode==ShowModeHw.STANDARD)?"(Locations)":"(Known Issues/Gaps)");}

	public MainPageExpert(WidgetPage<?> page, final HardwareInstallController controller, ShowModeHw showModeHw) {
		this(page, controller, false, showModeHw);
	}
	public MainPageExpert(WidgetPage<?> page, final HardwareInstallController controller, boolean isTrashPage,
			ShowModeHw showModeHw) {
		super(page, controller, true);
		StandardEvalAccess.init(controller.appManPlus);
		this.isTrashPage = isTrashPage;
		this.showMode = showModeHw;
		
		Button updateDatapoints = new Button(page, "updateDatapoints", "Update Datapoints") {
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				for(InstallAppDevice iad: controller.appConfigData.knownDevices().getAllElements()) {
					DeviceHandlerProvider<?> tableProvider = controller.getDeviceHandler(iad);
					if(tableProvider != null)
						controller.updateDatapoints(tableProvider, iad);
				}
			}
		};
		topTable.setContent(0, 4, updateDatapoints);
	}
	
	@Override
	protected void finishConstructor() {
		StaticTable secondTable = new StaticTable(1, 6);
		RedirectButton stdCharts = new RedirectButton(page, "stdCharts", "Chart Config", "/org/sp/app/srcmonexpert/rssioverview.html");
		RedirectButton homeScreen = new RedirectButton(page, "homeScreen", "Other Apps", "/org/smartrplace/apps/apps-overview/index.html") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				String user = GUIUtilHelper.getUserLoggedIn(req);
				if(user.equals("master"))
					setUrl("/ogema/index.html", req);
			}
			
		};
		RedirectButton alarming = new RedirectButton(page, "alarming", "Alarming Configuration", "/org/smartrplace/alarmingconfig/templateconfig.html") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				String user = GUIUtilHelper.getUserLoggedIn(req);
				if(user.equals("master"))
					setUrl("/org/smartrplace/alarmingsuper/index.html", req);
			}
		};
		RedirectButton alarmingExpert = new RedirectButton(page, "alarmExpert", "Alarming Expert", "/org/smartrplace/alarmingexpert/deviceknownfaults.html");
		
		boolean isIndex = page.getUrl().startsWith("index");
		RedirectButton otherMainPageBut = new RedirectButton(page, "otherMainPageBut",
				(showMode==ShowModeHw.STANDARD)?"KnI/Gap Eval":"Locations",
				isIndex?"/org/smartrplace/hardwareinstall/expert/mainExpert2.html":"/org/smartrplace/hardwareinstall/expert/index.html");

		/*showModeFilter = new SingleFilteringDirect<ShowModeHw>(page, "showModeFilter", 
				OptionSavingMode.PER_USER, 5000, false) {

			@Override
			protected List<GenericFilterOption<ShowModeHw>> getOptionsDynamic(OgemaHttpRequest req) {
				List<GenericFilterOption<ShowModeHw>> result = new ArrayList<>();
				result.add(new GenericFilterFixed<ShowModeHw>(ShowModeHw.STANDARD, "Locations"));
				result.add(new GenericFilterFixed<ShowModeHw>(ShowModeHw.KNI, "Issue and Gap Eval"));
				return result ;
			}

			@Override
			protected long getFrameworkTime() {
				return appMan.getFrameworkTime();
			}
		};
		showModeFilter.setDefaultPreSelectionGeneral("Locations");*/
		
		secondTable.setContent(0, 0, stdCharts).setContent(0, 1, homeScreen).setContent(0,  2, alarming).setContent(0,  3, alarmingExpert)
				.setContent(0,  5, otherMainPageBut);
		page.append(secondTable);
		super.finishConstructor();
	}
	
	@Override
	protected DefaultScheduleViewerConfigurationProviderExtended getScheduleViewerExtended() {
		return ScheduleViewerConfigProvHWI.getInstance();
	}
	
	@Override
	public void addWidgetsExpert(DeviceHandlerProvider<?> tableProvider, final InstallAppDevice object,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan) {
		if(tableProvider != null)
			tableProvider.addMoreWidgetsExpert(object, vh, id, req, row, appMan);
		
		if(showMode == ShowModeHw.STANDARD) {
			vh.stringLabel("IAD", id, object.getName(), row);
			vh.stringLabel("ResLoc", id, object.device().getLocation(), row);
		} else if(req == null) {
			vh.registerHeaderEntry("KniStatus");
			vh.registerHeaderEntry(GAPS_LABEL);
		} else {
			IntegerResource kniStatus = object.knownFault().assigned();
			KniStatus kniStat = getStatus(kniStatus);
			Label kniLabel = vh.stringLabel("KniStatus", id, kniStat.text, row);
			if(kniStat.style != null)
				kniLabel.addStyle(kniStat.style, req);
			float[] gapData = StandardEvalAccess.getQualityValuesPerDeviceStandard(object, appMan, controller.appManPlus);
			String gapText = gapData[0]+"/"+gapData[1];
			float[] setpReactData = StandardEvalAccess.getSetpReactValuesPerDeviceStandard(object, appMan, controller.appManPlus);
			if(!Float.isNaN(setpReactData[0]))
				gapText += "/"+setpReactData[0]+"/"+setpReactData[1];
			Label gapLabel = vh.stringLabel(GAPS_LABEL, id, gapText, row);
			if(gapData[0] < 0.9f)
				gapLabel.addStyle(LabelData.BOOTSTRAP_RED, req);
		}
		if(req == null) {
			vh.registerHeaderEntry(DATAPOINT_INFO_HEADER);
			vh.registerHeaderEntry("Plot");
			vh.registerHeaderEntry("Action");
			vh.registerHeaderEntry("Perform");
			return;
		}

		DeviceHandlerProvider<?> devHandForTrash = null;
		if(isTrashPage)
			devHandForTrash = controller.getDeviceHandlerForTrash(object);
		final GetPlotButtonResult logResult = getPlotButton(id, object, controller, true, vh, row, req, devHandForTrash);
		if(logResult.devHand != null) {
			row.addCell("Plot", logResult.plotButton);
			
			final boolean isTemplate = DeviceTableRaw.isTemplate(object, logResult.devHand);
			final TemplateDropdown<String> actionDrop = new TemplateDropdown<String>(vh.getParent(), "actionDrop"+id, req) {
				@Override
				public void onGET(OgemaHttpRequest req) {
					if(isTrashPage)
						update(ACTIONS_TRASH, req);
					else if(isTemplate) {
						update(ACTIONS_TEMPLATE, req);
					} else {
						update(ACTIONS, req);
					}
				}
			};
			actionDrop.setDefaultItems(ACTIONS);
			row.addCell("Action", actionDrop);
			ButtonConfirm performButton = new ButtonConfirm(vh.getParent(), WidgetHelper.getValidWidgetId("delBut"+id), req) {
				@Override
				public void onGET(OgemaHttpRequest req) {
					String sel = actionDrop.getSelectedItem(req);
					switch(sel) {
					case LOG_ALL:
						setConfirmMsg("Really log all datapoints for "+object.device().getLocation()+" ?", req);
						break;
					case LOG_NONE:
						setConfirmMsg("Really log no datapoints for "+object.device().getLocation()+" ?", req);
						break;
					case DELETE:
						setConfirmMsg("Really delete "+object.device().getLocation()+" ?", req);
						break;
					case RESET:
						setConfirmMsg("Really delete installation&setup configuration for "+object.device().getLocation()+
								" ? Search for new devices to recreate clean configuration.", req);
						break;
					case TRASH:
					case TRASH_RESET:
						setConfirmMsg(getTrashConfirmation(object), req);
						break;
					case MAKE_TEMPLATE:
						setConfirmMsg("Really move template status to "+object.device().getLocation()+" ?", req);
						break;
					case APPLY_TEMPLATE:
						setConfirmMsg("Really apply settings of "+object.device().getLocation()+
								" to all devices of the same type? Note that all settings will be overwritten without further confirmation!", req);
						break;
					case APPLY_DEFAULT_ALARM:
						setConfirmMsg("Really overwrite settings of " + object.device().getLocation() +
									"with default alarming settings?", req);
						break;
					}
					setText(sel, req);
				}
				
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					String sel = getText(req);
					switch(sel) {
					case LOG_ALL:
						controller.activateLogging(logResult.devHand, object, false, false);
						break;
					case LOG_NONE:
						controller.activateLogging(logResult.devHand, object, false, true);
						break;
					case DELETE:
						object.device().getLocationResource().delete();
						object.delete();
						break;
					case RESET:
						object.delete();
						break;
					case TRASH_RESET:
					case TRASH:
						performTrashOperation(object, logResult.devHand);
						break;
					case MAKE_TEMPLATE:
						InstallAppDevice currentTemplate = controller.getTemplateDevice(logResult.devHand);
						if(currentTemplate != null)
							DeviceTableRaw.setTemplateStatus(currentTemplate, null, false);
							//currentTemplate.isTemplate().deactivate(false);
						DeviceTableRaw.setTemplateStatus(object, logResult.devHand, true);
						//ValueResourceHelper.setCreate(object.isTemplate(), logResult.devHand.id());
						//if(!object.isTemplate().isActive())
						//	object.isTemplate().activate(false);
						break;
					case APPLY_TEMPLATE:
						for(InstallAppDevice dev: controller.getDevices(logResult.devHand)) {
							if(dev.equalsLocation(object))
								continue;
							AlarmingConfigUtil.copySettings(object, dev, controller.appMan);
						}
						break;
					case APPLY_DEFAULT_ALARM:
						DeviceHandlerProvider<?> tableProvider = controller.getDeviceHandler(object);
						if(tableProvider != null)
							tableProvider.initAlarmingForDevice(object, controller.appConfigData);
						break;
					}
				}
			};
			row.addCell("Perform", performButton);
			actionDrop.registerDependentWidget(performButton, req);
			
		}
		
	}
	
	protected String getTrashConfirmation(InstallAppDevice object) {
		return "Really mark "+object.device().getLocation()+" as trash?";
	}

	public void performTrashOperation(InstallAppDevice object, final DeviceHandlerProvider<?> devHand) {
		//deactivate logging
		if(devHand != null)
			controller.activateLogging(devHand, object, false, true);
		//remove all alarming
		for(AlarmConfiguration alarm: object.alarms().getAllElements()) {
			IntegerResource status = AlarmingConfigUtil.getAlarmStatus(alarm.sensorVal(), false);
			if(status.exists())
				status.delete();
			alarm.delete();
		}
		object.device().getLocationResource().deactivate(true);
		ValueResourceHelper.setCreate(object.isTrash(), true);		
	}
	
	public static class KniStatus {
		public KniStatus(WidgetStyle<Label> style, String text) {
			this.style = style;
			this.text = text;
		}
		WidgetStyle<Label> style;
		String text;
	}
	public static KniStatus getStatus(IntegerResource kniStatus) {
		if(!kniStatus.isActive())
			return new KniStatus(LabelData.BOOTSTRAP_GREEN, "No Open");
		int kniVal = kniStatus.getValue();
		switch(kniVal) {
		case 0:
			return new KniStatus(LabelData.BOOTSTRAP_RED, "Unassigned");
		case 2100:
			return new KniStatus(LabelData.BOOTSTRAP_ORANGE, "Battery");
		case 2150:
			return new KniStatus(LabelData.BOOTSTRAP_LIGHT_BLUE, "NoReach");
		case 2200:
			return new KniStatus(LabelData.BOOTSTRAP_BLUE, "Signal Strength");
		case 2000:
			return new KniStatus(LabelData.BOOTSTRAP_BLUE, "Operation Other");
		case 2500:
			return new KniStatus(LabelData.BOOTSTRAP_BLUE, "Operation (Ext)");
		case 4000:
			return new KniStatus(LabelData.BOOTSTRAP_LIGHT_BLUE, "Customer");
		case 3000:
			return new KniStatus(null, "Development");
		case 3500:
			return new KniStatus(LabelData.BOOTSTRAP_GREY, "Dev (Ext)");
		case 7000:
			return new KniStatus(LabelData.BOOTSTRAP_GREY, "Special");
		case 1000:
		case 5000:
		case 6000:
			return new KniStatus(LabelData.BOOTSTRAP_GREY, "Other");
		default:
			throw new IllegalStateException("Unknown KniStatus:"+kniVal+" for "+kniStatus.getLocation());
		}
	}
}
