package org.smartrplace.apps.alarmingconfig.release;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.resourcemanager.transaction.ResourceTransaction;
import org.ogema.core.resourcemanager.transaction.WriteConfiguration;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.extended.alarming.AlarmGroupDataMajor;
import org.smartrplace.apps.alarmingconfig.sync.SuperiorIssuesSyncUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import com.google.common.base.Objects;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.emptywidget.EmptyData;
import de.iwes.widgets.html.emptywidget.EmptyWidget;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.dropdown.Dropdown;
import de.iwes.widgets.html.form.dropdown.DropdownOption;
import de.iwes.widgets.html.form.dropdown.EnumDropdown;
import de.iwes.widgets.html.html5.Flexbox;
import de.iwes.widgets.html.html5.flexbox.JustifyContent;
import de.iwes.widgets.html.popup.Popup;

public class ReleasePopup {

	private final Popup popup;
	private final EmptyWidget issueContainer;
	private final Dropdown releaseModeSelector;
	private final EnumDropdown<FinalAnalysis> analysisSelector;
	private final Button cancelButton;
	private final ButtonConfirm submitButton;
	
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
		this.analysisSelector = new EnumDropdown<FinalAnalysis>(page, baseId + "_finalanalysis", FinalAnalysis.class) {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final String selectedMode = releaseModeSelector.getSelectedValue(req);
				setWidgetVisibility("finalanalysis".equals(selectedMode), req);
			}
			
		};
		analysisSelector.setComparator((o1,o2) -> {
			final String id1 = o1.id();
			final String id2 = o2.id();
			if (Objects.equal(id1, id2))
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
		this.cancelButton = new Button(page, baseId + "_cancel", "Cancel");
		this.submitButton = new ButtonConfirm(page, baseId + "submit") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final String selectedMode = releaseModeSelector.getSelectedValue(req);
				switch(selectedMode) {
				case "finalanalysis":
					setConfirmMsg("Do you really want to release the issue with final analysis?", req);
					setConfirmBtnMsg("Release", req);
					break;
				case "trash":
					setConfirmMsg("Do you really want to move the issue to the trash?", req);
					setConfirmBtnMsg("To trash", req);
					break;
				case "delete":
					setConfirmMsg("Do you really want to delete the issue permanently?", req);
					setConfirmBtnMsg("Delete", req);
					break;
				default:
					setConfirmBtnMsg("?", req);
					setConfirmMsg("", req);
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
		submitButton.setDefaultCancelBtnMsg("Cancel");
		submitButton.setDefaultConfirmPopupTitle("Confirm release");
		submitButton.setDefaultText("Release");
		
		
		final Flexbox footerFlex = new Flexbox(page, baseId + "_footer", true);
		footerFlex.addItem(submitButton, null).addItem(cancelButton, null);
		footerFlex.setJustifyContent(JustifyContent.FLEX_RIGHT, null);
		footerFlex.addCssItem(">div", Map.of("column-gap", "1em"), null);
		
		
		
		final Flexbox bodyFlex = new Flexbox(page, baseId + "_body", true);
		bodyFlex.addItem(releaseModeSelector, null).addItem(analysisSelector, null);
		bodyFlex.setJustifyContent(JustifyContent.FLEX_LEFT, null);
		bodyFlex.addCssItem(">div", Collections.singletonMap("column-gap", "1em"), null);
		
		popup.setTitle("Release issue", null);
		popup.setBody(bodyFlex, null);
		popup.setFooter(footerFlex, null);
		popup.addCssItem(">div>div>div>div.modal-body", Collections.singletonMap("min-height", "10em"), null);
		
		
		releaseModeSelector.triggerAction(analysisSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
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
		((IssueContainer) this.issueContainer.getData(req)).issue = issue;
		final boolean isMajor = issue instanceof AlarmGroupDataMajor;
		releaseModeSelector.setWidgetVisibility(isMajor, req);
		releaseModeSelector.selectSingleOption(isMajor ? "finalanalysis" : "delete", req);
	}
	
	public void trigger(OgemaWidget externalWidget) {
		externalWidget.triggerAction(releaseModeSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		externalWidget.triggerAction(analysisSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, 1);
		externalWidget.triggerAction(submitButton, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST, 1);
		externalWidget.triggerAction(popup, TriggeringAction.POST_REQUEST, TriggeredAction.SHOW_WIDGET, 1);
	}
	
	public void append(WidgetPage<?> page) {
		page.append(popup);
	}
	
	public AlarmGroupData getSelectedIssue(OgemaHttpRequest req) {
		return ((IssueContainer) this.issueContainer.getData(req)).issue; 
	}
	
	static class IssueContainer extends EmptyData {
		
		AlarmGroupData issue;

		public IssueContainer(EmptyWidget empty) {
			super(empty);
		}
		
	}
	
	
}
