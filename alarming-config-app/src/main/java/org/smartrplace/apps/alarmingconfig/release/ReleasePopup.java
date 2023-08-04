package org.smartrplace.apps.alarmingconfig.release;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.resourcemanager.transaction.ResourceTransaction;
import org.ogema.core.resourcemanager.transaction.WriteConfiguration;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.extended.alarming.AlarmGroupDataMajor;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.alarmconfig.util.AlarmResourceUtil;
import org.smartrplace.apps.alarmingconfig.sync.SuperiorIssuesSyncUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.emptywidget.EmptyData;
import de.iwes.widgets.html.emptywidget.EmptyWidget;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.dropdown.Dropdown;
import de.iwes.widgets.html.form.dropdown.DropdownOption;
import de.iwes.widgets.html.form.dropdown.EnumDropdown;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.label.Link;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.html.html5.Flexbox;
import de.iwes.widgets.html.html5.flexbox.FlexDirection;
import de.iwes.widgets.html.html5.flexbox.JustifyContent;
import de.iwes.widgets.html.popup.Popup;

public class ReleasePopup {

	private final Popup popup;
	private final EmptyWidget issueContainer;
	private final Dropdown releaseModeSelector;
	private final Label finalAnalysisLabel;
	private final Label device;
	private final Link analysisExplanation;
	private final Label commentLabel;
	private final TextField analysisComment;
	private final Label devLabel;
	private final TextField devComment;
	private final EnumDropdown<FinalAnalysis> analysisSelector;
	
	private final Button cancelButton;
	private final Button submitButton;
	
	@SuppressWarnings("serial")
	public ReleasePopup(WidgetPage<?> page, String baseId, ApplicationManager appMan, Alert alert) {
		this.popup = new Popup(page, baseId + "_popup", true);
		this.issueContainer = new EmptyWidget(page, baseId + "_issuecontainer") {
			
			@Override
			public EmptyData createNewSession() {
				return new IssueContainer(this);
			}
			
		};
		this.releaseModeSelector = new Dropdown(page, baseId + "_releasemode");
		releaseModeSelector.setDefaultOptions(Arrays.asList(
			new DropdownOption("finalanalysis", "Final Analysis", false),
			new DropdownOption("trash", "Trash", false),
			new DropdownOption("delete", "Direct delete", false)
		));
		releaseModeSelector.setDefaultToolTip("Select the release mode. Either provide a final analysis, move the issue to the trash (only for assigned issues), or delete it directly.");
		releaseModeSelector.setDefaultMinWidth("9em");
		this.analysisSelector = new EnumDropdown<FinalAnalysis>(page, baseId + "_finalanalysis", FinalAnalysis.class) {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final String selectedMode = releaseModeSelector.getSelectedValue(req);
				setWidgetVisibility("finalanalysis".equals(selectedMode), req);
				final AlarmGroupData issue = getSelectedIssue(req);
				if (issue != null && issue instanceof AlarmGroupDataMajor && ((AlarmGroupDataMajor) issue).finalDiagnosis().isActive()) {
					try {
						final FinalAnalysis selected = FinalAnalysis.valueOf(((AlarmGroupDataMajor) issue).finalDiagnosis().getValue());
						selectItem(selected, req);
					} catch (IllegalArgumentException e) {} // ?
				}
			}
			
		};
		analysisSelector.setComparator((o1,o2) -> {
			final String id1 = o1.id();
			final String id2 = o2.id();
			if (Objects.equals(id1, id2))
				return 0;
			if ("finalanalysis".equals(id1))
				return -1;
			if ("finalanalysis".equals(id2))
				return 1;
			if ("trash".equals(id1))
				return -1;
			if ("trash".equals(id2))
				return 1;
			return id1.compareTo(id2);
		});
		this.analysisComment = new HideableTextField(page, baseId + "_analysisComment", "finalAnalysisComment");
		this.devComment = new HideableTextField(page, baseId + "_devComment", "featureUnderDevelopment");
		
		this.cancelButton = new Button(page, baseId + "_cancel", "Cancel");
		this.submitButton = new Button(page, baseId + "submit") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final String selectedMode = releaseModeSelector.getSelectedValue(req);
				switch(selectedMode) {
				case "finalanalysis":
					//setConfirmMsg("Do you really want to release the issue with final analysis?", req);
					//setConfirmBtnMsg("Release", req);
					setText("Release issue", req);
					break;
				case "trash":
					//setConfirmMsg("Do you really want to move the issue to the trash?", req);
					//setConfirmBtnMsg("To trash", req);
					setText("Issue to trash", req);
					break;
				case "delete":
					//setConfirmMsg("Do you really want to delete the issue permanently?", req);
					//setConfirmBtnMsg("Delete", req);
					setText("Delete issue", req);
					break;
				default:
					//setConfirmBtnMsg("?", req);
					//setConfirmMsg("", req);
				}
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				final AlarmGroupData issue = getSelectedIssue(req);
				if (issue == null)
					return;
				final boolean isMajorIssue = issue instanceof AlarmGroupDataMajor;
				AlarmGroupDataMajor major = isMajorIssue ? (AlarmGroupDataMajor) issue.getLocationResource() : null;
				final ResourceTransaction trans = isMajorIssue ? basicRelease(major, appMan) : null;
				final String selectedMode = releaseModeSelector.getSelectedValue(req);
				switch(selectedMode) {
				case "finalanalysis":
					//TODO: Move to major first
					if (!isMajorIssue) {
						major = SuperiorIssuesSyncUtils.syncIssueToSuperior(issue, appMan); 
						//return;
					}
					final FinalAnalysis analysis = analysisSelector.getSelectedItem(req);
					if (analysis == null) 
						return;
					trans.setString(major.finalDiagnosis(), analysis.name(), WriteConfiguration.CREATE_AND_ACTIVATE);
					final String comment = analysisComment.getValue(req).trim();
					if (!comment.isEmpty()) {
						StringResource comRes = issue.getSubResource("finalAnalysisComment", StringResource.class);
						trans.create(comRes);
						trans.setString(comRes, comment, WriteConfiguration.CREATE_AND_ACTIVATE);
					}
					final String developmentComment = devComment.getValue(req).trim();
					if (!developmentComment.isEmpty()) {
						StringResource comRes = issue.getSubResource("featureUnderDevelopment", StringResource.class);
						trans.create(comRes);
						trans.setString(comRes, developmentComment, WriteConfiguration.CREATE_AND_ACTIVATE);
					}
					break;
				case "trash":
					//TODO: Move to major first
					if (!isMajorIssue) {
						major = SuperiorIssuesSyncUtils.syncIssueToSuperior(issue, appMan); 
						//return;
					}
					trans.setTime(major.keepAsTrashUntil(), appMan.getFrameworkTime() + 30 * 24 * 3_600_00);  // 30d hardcoded
					break;
				case "delete":
					issue.delete();
					if (alert != null)
						alert.showAlert("Issue deleted successfully", true, req);
					break;
				default:
					alert.showAlert("Something went wrong, unexpected mode " + selectedMode, false, req);	
					return;
				}
				if (trans != null) {
					trans.commit();
					if (alert != null)
						alert.showAlert("Released", true, req);
				}
				
			}
			
		};
		
		
		
		//submitButton.setDefaultCancelBtnMsg("Cancel");
		//submitButton.setDefaultConfirmPopupTitle("Confirm release");
		submitButton.setDefaultText("Release");
		
		
		final Flexbox footerFlex = new Flexbox(page, baseId + "_footer", true);
		footerFlex.addItem(submitButton, null).addItem(cancelButton, null);
		footerFlex.setJustifyContent(JustifyContent.FLEX_RIGHT, null);
		footerFlex.addCssItem(">div", Map.of("column-gap", "1em"), null);
		
		final PageSnippet bodySnippet = new PageSnippet(page, baseId + "_body", true);
		final Label deviceLabel = new Label(page, baseId + "_deviceLabelInRelease", "Device");
		this.device = new Label(page, baseId + "_deviceInRelease") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final AlarmGroupData alarm = getSelectedIssue(req);
				final InstallAppDevice device = alarm != null ? AlarmResourceUtil.getDeviceForKnownFault(alarm) : null;
				if (device == null) {
					setText("", req);
					return;
				}
				final String id = device.deviceId().isActive() ? device.deviceId().getValue() :
					device.device().isActive() ? ResourceUtils.getHumanReadableName(device.device().getLocationResource()) :
					device.getPath();
				setText(id, req);
			}
			
		};
		final Label releaseModeLabel = new Label(page, baseId + "_releaseModeLabel", "Release mode");
		this.finalAnalysisLabel = new HideableLabel(page, baseId + "_finalAnalysisLabel", "Final analysis", releaseModeSelector);
		this.commentLabel = new HideableLabel(page, baseId + "_commentLabel", "Final analysis comment", releaseModeSelector);
		this.devLabel = new HideableLabel(page, baseId + "_devLabel", "Under development", releaseModeSelector);
		this.devLabel.setDefaultToolTip("Is the issue linked to a feature under developmen/a special development task?");
		
		this.analysisExplanation = new Link(page, baseId + "_releaseModeCodesLink") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final boolean visible = "finalanalysis".equals(releaseModeSelector.getSelectedValue(req));
				setWidgetVisibility(visible, req);
			}
			
		};
		
		
		analysisExplanation.setDefaultText("See https://gitlab.smartrplace.de/i1/smartrplace/smartrplace-main/-/wikis/Operation/Incidents_Operation");
		analysisExplanation.setDefaultNewTab(true);
		analysisExplanation.setDefaultUrl("https://gitlab.smartrplace.de/i1/smartrplace/smartrplace-main/-/wikis/Operation/Incidents_Operation");
		
		final Flexbox analysisFlex = new Flexbox(page, baseId + "_analysisFlex", true);
		analysisFlex.setDefaultFlexDirection(FlexDirection.COLUMN);
		analysisFlex.addItem(analysisSelector, null);
		analysisFlex.addItem(analysisExplanation, null);
		
		final StaticTable bodyTable = new StaticTable(5, 2, new int[] {3,9})
				.setContent(0, 0, deviceLabel).setContent(0, 1, device)
				.setContent(1, 0, releaseModeLabel).setContent(1, 1, releaseModeSelector)
				.setContent(2, 0, finalAnalysisLabel).setContent(2, 1, analysisFlex)
				.setContent(3, 0, commentLabel).setContent(3, 1, analysisComment)
				.setContent(4, 0, devLabel).setContent(4, 1, devComment);
		bodySnippet.append(bodyTable, null);
		
		popup.setTitle("Release issue", null);
		popup.setBody(bodySnippet, null);
		popup.setFooter(footerFlex, null);
		popup.addCssItem(">div>div>div>div.modal-body", Collections.singletonMap("min-height", "10em"), null);
		page.append(issueContainer);
		
		releaseModeSelector.triggerAction(finalAnalysisLabel, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		releaseModeSelector.triggerAction(analysisSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		releaseModeSelector.triggerAction(analysisExplanation, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		releaseModeSelector.triggerAction(commentLabel, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		releaseModeSelector.triggerAction(analysisComment, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		releaseModeSelector.triggerAction(devLabel, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		releaseModeSelector.triggerAction(devComment, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		releaseModeSelector.triggerAction(submitButton, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		cancelButton.triggerAction(popup, TriggeringAction.POST_REQUEST, TriggeredAction.HIDE_WIDGET);
		submitButton.triggerAction(popup, TriggeringAction.POST_REQUEST, TriggeredAction.HIDE_WIDGET);
		if (alert != null)
			submitButton.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}
	
	// here major must be the location resource
	private static ResourceTransaction basicRelease(AlarmGroupDataMajor major, ApplicationManager appMan) {
		final ResourceTransaction trans = appMan.getResourceAccess().createResourceTransaction();
		if (!major.releaseTime().isActive() || major.releaseTime().getValue() <= 0)
			trans.setTime(major.releaseTime(), appMan.getFrameworkTime(), WriteConfiguration.CREATE_AND_ACTIVATE);
		major.getReferencingNodes(false).stream().filter(r -> r.getParent() instanceof InstallAppDevice).forEach(trans::delete);
		return trans;
	}
	
	public void selectIssue(AlarmGroupData issue, OgemaHttpRequest req) {
		selectIssue(issue, Collections.emptyList(), req);
	}
	
	public void selectIssue(AlarmGroupData issue, Collection<OgemaWidget> feedbackWidgets, OgemaHttpRequest req) {
		((IssueContainer) this.issueContainer.getData(req)).issue = issue;
		final boolean isMajor = issue instanceof AlarmGroupDataMajor;
		releaseModeSelector.setWidgetVisibility(isMajor, req);
		releaseModeSelector.selectSingleOption(isMajor ? "finalanalysis" : "delete", req);
		final Collection<OgemaWidget> oldFeedbackWidgets = ((IssueContainer) this.issueContainer.getData(req)).feedbackWidgets;
		final List<OgemaWidget> forRemoval = oldFeedbackWidgets.stream().filter(w -> !feedbackWidgets.contains(w)).collect(Collectors.toList());
		final List<OgemaWidget> forAddition = feedbackWidgets.stream().filter(w -> !oldFeedbackWidgets.contains(w)).collect(Collectors.toList());
		forRemoval.forEach(w -> {
			submitButton.removeTriggerAction(w, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
			oldFeedbackWidgets.remove(w);
		});
		forAddition.forEach(w -> {
			submitButton.triggerAction(w, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, req);
			oldFeedbackWidgets.add(w);
		});
	}
	
	public void trigger(OgemaWidget externalWidget) {
		trigger(externalWidget, true);
	}
	
	public void trigger(OgemaWidget externalWidget, boolean showPopup) {
		externalWidget.triggerAction(device, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		externalWidget.triggerAction(releaseModeSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		externalWidget.triggerAction(finalAnalysisLabel, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, 1);
		externalWidget.triggerAction(analysisSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, 1);
		externalWidget.triggerAction(analysisExplanation, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, 1);
		externalWidget.triggerAction(commentLabel, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, 1);
		externalWidget.triggerAction(analysisComment, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, 1);
		externalWidget.triggerAction(devLabel, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, 1);
		externalWidget.triggerAction(devComment, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, 1);
		externalWidget.triggerAction(submitButton, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, 1);
		if (showPopup)
			externalWidget.triggerAction(popup, TriggeringAction.ON_CLICK, TriggeredAction.SHOW_WIDGET, 1);
	}
	
	public void append(WidgetPage<?> page) {
		page.append(popup);
	}
	
	public AlarmGroupData getSelectedIssue(OgemaHttpRequest req) {
		return ((IssueContainer) this.issueContainer.getData(req)).issue; 
	}
	
	public Popup popupWidget() {
		return this.popup;
	}
	
	static class IssueContainer extends EmptyData {
		
		AlarmGroupData issue;
		final Collection<OgemaWidget> feedbackWidgets = new ArrayList<>(4);
		
		public IssueContainer(EmptyWidget empty) {
			super(empty);
		}
		
	}
	
	@SuppressWarnings("serial")
	static class HideableLabel extends Label {
		
		private final Dropdown releaseModeSelector;
		
		public HideableLabel(WidgetPage<?> page, String id, String text, Dropdown releaseModeSelector) {
			super(page, id, text);
			this.releaseModeSelector = releaseModeSelector;
		}

		@Override
		public void onGET(OgemaHttpRequest req) {
			final boolean visible = "finalanalysis".equals(releaseModeSelector.getSelectedValue(req));
			setWidgetVisibility(visible, req);
		}
		
	}
	
	class HideableTextField extends TextField {
		
		private final String subresourceName;
		
		public HideableTextField(WidgetPage<?> page, String id, String subresourceName) {
			super(page, id);
			this.subresourceName = subresourceName;
		}
			
		@Override
		public void onGET(OgemaHttpRequest req) {
			final String selectedMode = releaseModeSelector.getSelectedValue(req);
			final boolean visible = "finalanalysis".equals(selectedMode);
			setWidgetVisibility(visible, req);
			if (visible) {
				final AlarmGroupData issue = getSelectedIssue(req);
				if (issue == null)
					return;
				final Resource analysisComment = issue.getSubResource(subresourceName);
				if (analysisComment instanceof StringResource && analysisComment.isActive())
					setValue(((StringResource) analysisComment).getValue(), req);
				else
					setValue("", req);
			}
			
		}
			
		
	}
	
	
}
