package org.smartrplace.apps.hw.install.gui.alarm;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONObject;
import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.extended.alarming.AlarmGroupDataMajor;
import org.ogema.model.user.NaturalPerson;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.apps.alarmingconfig.release.FinalAnalysis;
import org.smartrplace.apps.alarmingconfig.release.ReleasePopup;
import org.smartrplace.apps.alarmingconfig.sync.SuperiorIssuesSyncUtils;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.ThermostatPage;
import org.smartrplace.gateway.device.GatewaySuperiorData;
import org.smartrplace.util.directobjectgui.ObjectGUIHelperBase.ValueResourceDropdownFlex;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.directresourcegui.GUIHelperExtension;
import org.smartrplace.util.format.WidgetHelper;
import org.smartrplace.util.virtualdevice.ChartsUtil;
import org.smartrplace.util.virtualdevice.ChartsUtil.GetPlotButtonResult;

import com.google.common.base.Supplier;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.extended.mode.UpdateMode;
import de.iwes.widgets.api.extended.resource.DefaultResourceTemplate;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.dropdown.Dropdown;
import de.iwes.widgets.html.form.dropdown.DropdownData;
import de.iwes.widgets.html.form.dropdown.DropdownOption;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.label.LabelData;
import de.iwes.widgets.html.html5.Flexbox;
import de.iwes.widgets.html.html5.flexbox.AlignItems;
import de.iwes.widgets.resource.widget.dropdown.ResourceDropdown;
import de.iwes.widgets.resource.widget.textfield.ValueResourceTextField;

@SuppressWarnings("serial")
public class MajorKnownFaultsPage extends ObjectGUITablePage<AlarmGroupDataMajor, AlarmGroupDataMajor> {
	
	private static final String GW_URL_PARAM = "gw";
	private static final String RELEASE_URL_PARAM = "release";
	private static final String ASSIGNED_URL_PARAM = "assigned";
	private static final String TRASH_URL_PARAM = "trash";
	private static final List<DropdownOption> RELEASE_FILTER_OPTIONS = Arrays.asList(
			new DropdownOption("all", "All", true),
			new DropdownOption("released", "Released", false),
			new DropdownOption("nonreleased", "Not released", false)
	);
	private static final List<DropdownOption> ASSIGNMENT_FILTER_OPTIONS = AlarmingConfigUtil.ASSIGNEMENT_ROLES.entrySet().stream()
			.map(entry -> new DropdownOption(entry.getKey(), entry.getValue(), false))
			.collect(Collectors.toList());
	private static final List<DropdownOption> TRASH_FILTER_OPTIONS = Arrays.asList(
			new DropdownOption("nontrash", "Active", true),
			new DropdownOption("all", "All", false),
			new DropdownOption("trash", "Trash", false)
	); 
	
	private final ApplicationManagerPlus appManPlus;
	private final AlarmingConfigAppController controller;
	private final HardwareInstallConfig hwInstallConfig;
	private final boolean isSuperior;
	private final TemplateDropdown<GatewaySuperiorData> gatewaySelector; // may be null
	private final Dropdown releaseStatusFilter;
	private final Dropdown assignmentStatusFilter;
	private final Dropdown trashStatusFilter;

	private IssueDetailsPopup lastMessagePopup;
	private final ReleasePopup releasePopup;

	public MajorKnownFaultsPage(WidgetPage<?> page, AlarmingConfigAppController controller) {
		super(page, controller.appMan, AlarmGroupDataMajor.class, false);
		this.appManPlus = controller.appManPlus;
		this.controller = controller;
		hwInstallConfig = appMan.getResourceAccess().getResource("hardwareInstallConfig");
		this.isSuperior = Boolean.getBoolean("org.smartrplace.app.srcmon.server.issuperior");
		if (isSuperior) {
			gatewaySelector = new ResourceDropdown<GatewaySuperiorData>(page, "gatewaySelector", false, 
					GatewaySuperiorData.class, UpdateMode.AUTO_ON_GET, appMan.getResourceAccess());
			gatewaySelector.setTemplate(new DefaultResourceTemplate<GatewaySuperiorData>() {
				
				@Override
				public String getLabel(GatewaySuperiorData res, OgemaLocale req) {
					return SuperiorIssuesSyncUtils.findGatewayId(res, isSuperior);
				}
				
			});
			gatewaySelector.setDefaultAddEmptyOption(true, "All");
			gatewaySelector.setDefaultSelectByUrlParam(GW_URL_PARAM);
		} else {
			gatewaySelector = null;
		}
		this.releaseStatusFilter = new Dropdown(page, "releaseStatusFilter") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final List<DropdownOption> opts = getDropdownOptions(req); 
				if (opts == null || opts.isEmpty()) {
					setOptions(RELEASE_FILTER_OPTIONS, req);
					final String[] initialStatus = getPage().getPageParameters(req).get(RELEASE_URL_PARAM);
					if (initialStatus != null && initialStatus.length > 0)
						selectSingleOption(initialStatus[0], req);
				}
			} 
			
		};
		releaseStatusFilter.setDefaultSelectByUrlParam(RELEASE_URL_PARAM);
		this.assignmentStatusFilter = new Dropdown(page, "assignmentStatusFilter") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final List<DropdownOption> opts = getDropdownOptions(req); 
				if (opts == null || opts.isEmpty()) {
					setOptions(ASSIGNMENT_FILTER_OPTIONS, req);
					final String[] initialStatus = getPage().getPageParameters(req).get(ASSIGNED_URL_PARAM);
					if (initialStatus != null && initialStatus.length > 0)
						selectSingleOption(initialStatus[0], req);
				}
			} 
			
		};
		assignmentStatusFilter.setDefaultAddEmptyOption(true);
		assignmentStatusFilter.setDefaultSelectByUrlParam(ASSIGNED_URL_PARAM);
		this.trashStatusFilter = new Dropdown(page, "trashStatusFilter") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final List<DropdownOption> opts = getDropdownOptions(req); 
				if (opts == null || opts.isEmpty()) {
					setOptions(TRASH_FILTER_OPTIONS, req);
					final String[] initialStatus = getPage().getPageParameters(req).get(TRASH_URL_PARAM);
					if (initialStatus != null && initialStatus.length > 0)
						selectSingleOption(initialStatus[0], req);
				}
			}
			
		};
		trashStatusFilter.setDefaultSelectByUrlParam(TRASH_URL_PARAM);
		
		this.lastMessagePopup = new IssueDetailsPopup(page);
		this.releasePopup = new ReleasePopup(page, "releasePop", controller.appMan, alert, controller);
		releasePopup.append(page);
		
		triggerPageBuild();
		if(gatewaySelector != null)
			gatewaySelector.triggerAction(mainTable, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		releaseStatusFilter.triggerAction(mainTable, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		assignmentStatusFilter.triggerAction(mainTable, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		trashStatusFilter.triggerAction(mainTable, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		this.mainTable.postponeLoading();
		this.mainTable.setComposite();
		page.showOverlay(true);
	}

	@Override
	public void addWidgetsAboveTable() {
		page.append(new Header(page, "header", "8b. Major Device Issues"));	
		
		final Map<String, String> subFlexCss = new HashMap<>(4);
		subFlexCss.put("column-gap", "1em");
		subFlexCss.put("flex-wrap", "nowrap");
		final Map<String, String> subFlexFirstCss = new HashMap<>(4);
		//subFlexFirstCss.put("color", "darkblue");
		subFlexFirstCss.put("font-weight", "bold");
		final Map<String, String> filterFlexCss = new HashMap<>(8);
		filterFlexCss.put("column-gap", "3em");
		filterFlexCss.put("row-gap", "1em");
		filterFlexCss.put("flex-wrap", "wrap");
		filterFlexCss.put("padding", "0.5em");
		filterFlexCss.put("background-color", "lightgray");
		
		final Flexbox filterFlex = new Flexbox(page, "filterflex", true);
		filterFlex.addCssItem(">div", filterFlexCss, null);
		filterFlex.setAlignItems(AlignItems.CENTER, null);
		
		final AtomicInteger subCnt = new AtomicInteger();
		final Supplier<Flexbox> subFlexSupplier = () -> {
			final Flexbox sub  = new Flexbox(page, "filterflex_sub" + subCnt.getAndIncrement(), true);
			sub.addCssItem(">div", subFlexCss, null);
			sub.addCssItem(">div>div:first-child", subFlexFirstCss, null);
			sub.setAlignItems(AlignItems.CENTER, null);
			filterFlex.addItem(sub, null);
			return sub;
		};
		if (isSuperior) {
			subFlexSupplier.get().addItem(new Label(page, "gatewaySelectLabel", "Select gateway"), null)
				.addItem(gatewaySelector, null);
		}
		subFlexSupplier.get().addItem(new Label(page, "releasedFilterLabel", "Release status"), null)
			.addItem(releaseStatusFilter, null);
		subFlexSupplier.get().addItem(new Label(page, "assignmentFilterLabel", "Assignment"), null)
			.addItem(assignmentStatusFilter, null);
		subFlexSupplier.get().addItem(new Label(page, "trashFilterLabel", "Trash status"), null)
			.addItem(trashStatusFilter, null);
		final Header filterHeader = new Header(page, "filterHeader", "Filters");
		filterHeader.setDefaultHeaderType(3);
		//filterHeader.setDefaultColor("darkblue");
		final Header alarmsHeader = new Header(page, "alarmsHeader", "Device alarms");
		alarmsHeader.setDefaultHeaderType(3);
		//alarmsHeader.setDefaultColor("darkblue");
		page.append(filterHeader).append(filterFlex).append(alarmsHeader);
	}
	
	@Override
	public void addWidgets(AlarmGroupDataMajor res,
			ObjectResourceGUIHelper<AlarmGroupDataMajor, AlarmGroupDataMajor> vh, String id, OgemaHttpRequest req,
			Row row, ApplicationManager appMan) {
		if(req == null) {
			if (isSuperior)
				vh.registerHeaderEntry("Gateway");
			//vh.registerHeaderEntry("Main value");
			vh.registerHeaderEntry("Name");
			vh.registerHeaderEntry("ID");
			vh.registerHeaderEntry("Started");
			vh.registerHeaderEntry("Release");
			vh.registerHeaderEntry("Diagnosis");
			vh.registerHeaderEntry("Details");
			vh.registerHeaderEntry("Alarms");
			vh.registerHeaderEntry("Comment_Analysis");
			vh.registerHeaderEntry("Analysis_Assigned");
			vh.registerHeaderEntry("Task Tracking");
			vh.registerHeaderEntry("Priority");
			vh.registerHeaderEntry("Responsible");
			vh.registerHeaderEntry("Follow-up");
			vh.registerHeaderEntry("TH-Plot");
			vh.registerHeaderEntry("Plot");
			vh.registerHeaderEntry("Delete");
			return;
		}

		final InstallAppDevice object; // may be null
		if(res.parentForOngoingIssues().isActive()) {
			object = res.parentForOngoingIssues();
			if(!res.devicesRelated().isActive()) {
				//should be there already, but legacy issues may not have it
				ValueResourceHelper.setCreate(res.devicesRelated(), new String[] {object.deviceId().getValue()});
			}
		} else if (res.devicesRelated().isActive()) { // FIXME on superior, only if this is not a gateway related issue 
			String[] vals = res.devicesRelated().getValues();
			//if(vals.length == 0)
			//	return;
			object = vals.length > 0 ? appManPlus.dpService().getMangedDeviceResource(vals[0]) : null;
		} else {
			object = null;
		}
		if (isSuperior) {
			final String gwId = SuperiorIssuesSyncUtils.findGatewayId(res, isSuperior);
			if (gwId != null)
				vh.stringLabel("Gateway", id, gwId, row);
		}
		
		final DeviceHandlerProviderDP<?> pe = object != null ? appManPlus.dpService().getDeviceHandlerProvider(object) : null;
		
		if (object != null) {
			AlarmingDeviceTableBase.addNameWidgetStatic(object, vh, id, req, row, appManPlus.dpService(), pe, hwInstallConfig);
		} else if (res.devicesRelated().isActive()) {
			final String[] devices = res.devicesRelated().getValues();
			if (devices.length > 0) {
				String devId = Arrays.stream(devices).collect(Collectors.joining(", "));
				vh.stringLabel("Name", id, devId, row);
				vh.stringLabel("ID", id, devId, row);
			}
		} // TODO show something else as id, name?
		
		//vh.stringLabel("ID", id, object.deviceId().getValue(), row);
		
		//vh.stringLabel("Finished", id, ""+res.isFinished().getValue(), row);
		// some special sensor values... priority for display: battery voltage; mainSensorValue, rssi, eq3state
		//final ValueData valueData = getValueData(object, appMan);
		/*
		final Label valueField = new Label(mainTable, "valueField" + id, req);
		AlarmMessageUtil.configureAlarmValueLabel(object, appMan, valueField, req, Locale.ENGLISH);
		//valueField.setText(valueData.message, req);
		row.addCell("value", valueField);
		*/
		//if (valueData.responsibleResource != null)
		//	valueField.setToolTip("Value resource: " + valueData.responsibleResource.getLocationResource(), req);
		
		vh.timeLabel("Started", id, res.ongoingAlarmStartTime(), row, 0);
		final Label timeLabel = vh.timeLabel("Release", id, res.releaseTime(), row, 0);
		// the Label is adapted such that it supports POST operations, and  
		// a click on the label triggers a POST, in the onPOST method it sets the alarm resource of ReleasePopup, 
		// then it triggers an update of the ReleasePopup widgets
		final Label diag = new Label(mainTable, "diagLabel" + id, req) {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final boolean isTrash = res.keepAsTrashUntil().isActive() && res.keepAsTrashUntil().getValue() > 0;
				if (!res.finalDiagnosis().isActive()) {
					setText(isTrash ? "Trash" : "--", req);
					setToolTip("Click to open release menu", req);
				} else {
					String v = res.finalDiagnosis().getValue();
					try {
						final String descriptionSuffix =  ": " + FinalAnalysis.valueOf(v).description();
						v += descriptionSuffix;
					} catch (Exception e) {}
					final String tooltip = v;
					if (v.length() > 30)
						v = v.substring(0, 30) + " ...";
					if (isTrash)
						v += " (Trash)";
					setText(v, req);
					setToolTip(tooltip + "\nClick to edit", req);
				}
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				releasePopup.selectIssue(res, Arrays.asList(this, timeLabel), req);
			}
			
			@Override
			public LabelData createNewSession() {
				return new PostableLabelData(this);
			}
			
			class PostableLabelData extends LabelData {
				
				public PostableLabelData(Label l) {
					super(l);
				}
			
				@Override
				public JSONObject onPOST(String data, OgemaHttpRequest req) {
					return new JSONObject();
				}
			}
			
		};
		diag.addCssItem(":hover", Collections.singletonMap("cursor", "pointer"), req);
		diag.triggerAction(diag, TriggeringAction.ON_CLICK, TriggeredAction.POST_REQUEST);
		diag.setDefaultMinWidth("5em;");
		releasePopup.trigger(diag);
		row.addCell("Diagnosis", diag);
		
		final Dropdown followupemail = new FollowUpDropdown(mainTable, "followup" + id, req, appMan, alert, object, res, controller);
		
		final Button showMsg = new Button(mainTable, "msg" + id, req) {
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				// TODO handle case of null object, supply gateway id instead?
				lastMessagePopup.setValues(res, object, object.device().getLocationResource(), followupemail, req);
			}
			
		};
		showMsg.setDefaultText("Details");
		showMsg.setDefaultToolTip("Show the last alarm message sent for this device and othr details about the source of the alarm.");
		lastMessagePopup.setTriggers(showMsg);
		row.addCell("Details", showMsg);
		
		if (object != null) {
			final RedirectButton detailsRedirect = new RedirectButton(mainTable, "details" + id, "Redirect", 
					"/org/smartrplace/alarmingexpert/ongoingbase.html?device=" + object.deviceId().getValue(), req);
			detailsRedirect.setToolTip("View alarm details in new tab", req);
			row.addCell("Alarms", detailsRedirect);
		}
		if(res.exists()) {
			row.addCell("Comment_Analysis", new CommentEditTextArea(mainTable, id, req, res));
			//vh.stringEdit("Comment_Analysis",  id, res.comment(), row, alert, res.comment());
			ValueResourceDropdownFlex<IntegerResource> widgetPlus = new ValueResourceDropdownFlex<IntegerResource>(
					"Analysis_Assigned"+id, vh, AlarmingConfigUtil.ASSIGNEMENT_ROLES) {
				public void onGET(OgemaHttpRequest req) {
					myDrop.selectItem(res.assigned(), req);
					final String role = AlarmingConfigUtil.ASSIGNEMENT_ROLES.get(String.valueOf(res.assigned().getValue()));
					if (role != null)
						myDrop.setToolTip(role, req);
				}
				@Override
				public void onPrePOST(String data, OgemaHttpRequest req) {
					IntegerResource source = res.assigned();
					if(!source.exists()) {
						source.create();
						source.activate(true);
					}
				}
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					int val = res.assigned().getValue();
					if(val >= 7000 && val < 8000) {
						//Non-Blocking
						ValueResourceHelper.setCreate(res.minimumTimeBetweenAlarms(), 0);
					} else {
						//Blocking
						ValueResourceHelper.setCreate(res.minimumTimeBetweenAlarms(), -1);
					}
				}
			};
			row.addCell(WidgetHelper.getValidWidgetId("Analysis_Assigned"), widgetPlus.myDrop);
			
			if(!res.linkToTaskTracking().getValue().isEmpty()) {
				RedirectButton taskLink = new RedirectButton(mainTable, "taskLink"+id, "Task Tracking",
						res.linkToTaskTracking().getValue(), req);
				row.addCell(WidgetHelper.getValidWidgetId("Task Tracking"), taskLink);
			}
			final ValueResourceTextField<FloatResource> prioField 
				= new ValueResourceTextField<FloatResource>(mainTable, "prio" + id, res.getSubResource("processingOrder", FloatResource.class), req);
			prioField.setDefaultToolTip("Alarm priority, e.g. 10, 20, 30, ...");
			prioField.setDefaultWidth("4em");
			prioField.triggerAction(prioField, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
			row.addCell("Priority", prioField);
			
			final Dropdown responsibleDropdown = new Dropdown(mainTable, "responsible"+id, req) {
				
				@Override
				public void onGET(OgemaHttpRequest req) {
					final GatewaySuperiorData supData = SuperiorIssuesSyncUtils.getSuperiorData(res, appMan);
					if (supData == null || !supData.responsibilityContacts().isActive())
						return;
					final StringResource responsibility = res.responsibility();
					final String email = responsibility.isActive() ? responsibility.getValue() : "";
					final NaturalPerson selected = email.isEmpty() ? null : supData.responsibilityContacts().getAllElements().stream()
						.filter(c -> email.equals(c.getSubResource("emailAddress", StringResource.class).getValue()))
						.findAny().orElse(null);
					final List<DropdownOption> options = supData.responsibilityContacts().getAllElements().stream()
						.map(contact -> new DropdownOption(
								contact.getName(), contact.userRole().isActive() ? contact.userRole().getValue() :
								contact.firstName().getValue() + " " + contact.lastName().getValue(), 
								contact.equalsLocation(selected)
						))
						.collect(Collectors.toList());
					setOptions(options, req);
					if (selected != null) {
						final String id = selected.userRole().isActive() ? selected.userRole().getValue() : selected.firstName().getValue() + " " + selected.lastName().getValue();
						setToolTip(id + ": " + email, req);
					} else {
						setToolTip(email.isEmpty() ? "Select responsible" :  email, req);
					}
				}
				
				@Override
				public void onPOSTComplete(String arg0, OgemaHttpRequest req) {
					final GatewaySuperiorData supData = SuperiorIssuesSyncUtils.getSuperiorData(res, appMan);
					if (supData == null || !supData.responsibilityContacts().isActive())
						return;
					final String currentSelected = getSelectedValue(req);
					final StringResource responsibility = res.responsibility();
					if (currentSelected == null || currentSelected.isEmpty() || currentSelected.equals(DropdownData.EMPTY_OPT_ID) 
								|| supData.responsibilityContacts().getSubResource(currentSelected) == null) {
						responsibility.delete();
						return;
					}
					final NaturalPerson selected = supData.responsibilityContacts().getSubResource(currentSelected); 
					final StringResource emailRes = selected.getSubResource("emailAddress");
					final String email = emailRes.isActive() ? emailRes.getValue() : "";
					if (email.isEmpty()) { // ?
						return;
					}
					responsibility.<StringResource> create().setValue(email);
					responsibility.activate(false);
				}
				
				
			};
			responsibleDropdown.setDefaultAddEmptyOption(true);
			responsibleDropdown.setDefaultMinWidth("8em");
			responsibleDropdown.setComparator((o1, o2) ->  { // show default roles supervision and terminvereinbarung first
				if (Objects.equals(o1, o2))
					return 0;
				final boolean comp1 = o1.id().indexOf('_') > 0;
				final boolean comp2 = o2.id().indexOf('_') > 0;
				if (comp1 == comp2)
					return o1.id().compareTo(o2.id());
				return comp1 ? 1 : -1;
			});
			responsibleDropdown.triggerAction(responsibleDropdown, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
			row.addCell("Responsible", responsibleDropdown);
			
		}
		row.addCell(WidgetHelper.getValidWidgetId("Follow-up"), followupemail);
		
		if(object != null && object.device() instanceof Thermostat) {
			Thermostat dev = (Thermostat)object.device();
			final GetPlotButtonResult logResultSpecial = ThermostatPage.getThermostatPlotButton(dev, appManPlus, vh, id, row, req, ScheduleViewerConfigProvAlarm.getInstance());
			row.addCell(WidgetHelper.getValidWidgetId("TH-Plot"), logResultSpecial.plotButton);
		}
		
		// TODO object == null ?
		final GetPlotButtonResult logResult = ChartsUtil.getPlotButton(id, object, appManPlus.dpService(), appMan, false, vh, row, req, pe,
				ScheduleViewerConfigProvAlarm.getInstance(), null);
		row.addCell("Plot", logResult.plotButton);
		
		GUIHelperExtension.addDeleteButton(null, res, mainTable, id, alert, "Delete", row, vh, req);
	}

	@Override
	public AlarmGroupDataMajor getResource(AlarmGroupDataMajor object, OgemaHttpRequest req) {
		return object;
	}

	@Override
	public Collection<AlarmGroupDataMajor> getObjectsInTable(OgemaHttpRequest req) {
		Stream<AlarmGroupDataMajor> alarms = getAlarms(req);
		final String statusFilter = releaseStatusFilter.getSelectedValue(req);
		switch(statusFilter) {
		case "released":
			alarms = alarms.filter(MajorKnownFaultsPage::isReleased);
			break;
		case "nonreleased":
			alarms = alarms.filter(a -> !isReleased(a));
			break;
		default:
			break;
		}
		final String assignmentFilter = assignmentStatusFilter.getSelectedValue(req);
		if (assignmentFilter != null && assignmentFilter != DropdownData.EMPTY_OPT_ID)
			alarms = alarms.filter(a -> a.assigned().isActive() && assignmentFilter.equals(String.valueOf(a.assigned().getValue())));
		final String trashFilter = trashStatusFilter.getSelectedValue(req);
		switch (trashFilter) {
		case "trash":
			alarms = alarms.filter(a -> a.keepAsTrashUntil().isActive() && a.keepAsTrashUntil().getValue() > 0);
			break;
		case "nontrash":
			alarms = alarms.filter(a -> !a.keepAsTrashUntil().isActive() || a.keepAsTrashUntil().getValue() <= 0);
			break;
		}
		return alarms.collect(Collectors.toList());
	}
	
	private static boolean isReleased(AlarmGroupDataMajor alarm) {
		return alarm.releaseTime().isActive() && alarm.releaseTime().getValue() > 0;
	}
	
	private Stream<AlarmGroupDataMajor> getAlarms(OgemaHttpRequest req) {
		if (this.gatewaySelector != null) {
			final GatewaySuperiorData data = gatewaySelector.getSelectedItem(req);
			if (data != null)
				return data.majorKnownIssues().getAllElements().stream();
		}
 		return appMan.getResourceAccess().getResources(AlarmGroupDataMajor.class).stream();
	}
	

}
