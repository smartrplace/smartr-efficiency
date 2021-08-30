package org.smartrplace.apps.hw.install.gui.alarm;

import java.util.Collection;

import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.extended.alarming.DevelopmentTask;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.apps.hw.install.config.InstallAppDeviceBase;
import org.smartrplace.gui.tablepages.ObjectGUITablePageNamed;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.RedirectButton;

@SuppressWarnings("serial")
public class DevelopmentTaskPage extends ObjectGUITablePageNamed<DevelopmentTask, DevelopmentTask> {
	protected final AlarmingConfigAppController controller;
	
	public DevelopmentTaskPage(WidgetPage<?> page, AlarmingConfigAppController controller) {
		super(page, controller.appMan, ResourceHelper.getSampleResource(DevelopmentTask.class));
		this.controller = controller;
		triggerPageBuild();
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return "Development Special Settings";
	}
	
	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		StaticTable topTable = new StaticTable(1, 4);
		
		Button addButton = new Button(page, "addButton", "Add") {
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				controller.hwTableData.appConfigData.knownDevelopmentTasks().add();
			}
		};
		addButton.registerDependentWidget(mainTable);
		
		RedirectButton alarmingSettings = new RedirectButton(page, "alarmingSettings", "Development Task Alarming Settings", "/org/smartrplace/alarmingsuper/devicedevtask.html");
		topTable.setContent(0, 0, addButton).setContent(0, 3, alarmingSettings);
		page.append(topTable);
	}
	
	@Override
	public void addWidgets(DevelopmentTask object, ObjectResourceGUIHelper<DevelopmentTask, DevelopmentTask> vh,
			String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		addNameLabel(object, vh, id, row, req);
		vh.stringEdit("Name", id, object.name(), row, alert);
		vh.stringEdit("Comment", id, object.comment(), row, alert);
		if(req == null) {
			vh.registerHeaderEntry("Task Tracking");
			vh.registerHeaderEntry("Edit TT");
			vh.registerHeaderEntry("Types");
			vh.registerHeaderEntry("Delete");
			vh.registerHeaderEntry("Template Reset");
			return;
		}
		if(!object.linkToTaskTracking().getValue().isEmpty()) {
			RedirectButton taskLink = new RedirectButton(mainTable, "taskLink"+id, "Task Tracking",
					object.linkToTaskTracking().getValue(), req);
			row.addCell(WidgetHelper.getValidWidgetId("Task Tracking"), taskLink);
		}
		vh.stringEdit("Edit TT",  id, object.linkToTaskTracking(), row, alert);
		
		String types;
		if(object.templates().size() == 0)
			types = "--";
		else {
			types = null;
			for(InstallAppDeviceBase iad: object.templates().getAllElements()) {
				String dhid = AlarmingConfigUtil.getDeviceHandlerShortId(iad);
				if(!iad.alarms().exists())
					dhid = "("+dhid+"*)";
				if(types == null)
					types = dhid;
				else
					types += ", "+dhid;
			}
		}
		vh.stringLabel("Types", id, types, row);
		
		ButtonConfirm releaseBut = new ButtonConfirm(mainTable, "releaseBut"+id, req) {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				object.delete();
			}
		};
		releaseBut.setConfirmMsg("Really delete developmentTask "+ResourceUtils.getHumanReadableShortName(object)+"?", req);
		releaseBut.setText("Delete", req);
		
		vh.booleanEdit("Template Reset", id, object.overWriteTemplateRequest(), row, 1);
	}

	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "ResName";
	}

	@Override
	protected String getLabel(DevelopmentTask obj, OgemaHttpRequest req) {
		return obj.getName();
	}

	@Override
	public Collection<DevelopmentTask> getObjectsInTable(OgemaHttpRequest req) {
		return controller.hwTableData.appConfigData.knownDevelopmentTasks().getAllElements();
	}

}
