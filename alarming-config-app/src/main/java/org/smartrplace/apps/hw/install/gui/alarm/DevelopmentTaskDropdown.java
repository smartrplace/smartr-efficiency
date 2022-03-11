package org.smartrplace.apps.hw.install.gui.alarm;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ResourceList;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.extended.alarming.DevelopmentTask;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.config.InstallAppDeviceBase;
import org.smartrplace.hwinstall.basetable.HardwareTableData;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.template.DefaultDisplayTemplate;

@SuppressWarnings("serial")
public class DevelopmentTaskDropdown extends TemplateDropdown<DevelopmentTask> {
	protected final InstallAppDevice object;
	protected final HardwareTableData resData;
	protected final ApplicationManager appMan;
	
	protected final AlarmingConfigAppController controller;
	
	public DevelopmentTaskDropdown(InstallAppDevice object, HardwareTableData resData,
			ApplicationManager appMan, AlarmingConfigAppController controller,
			OgemaWidget parent, String id, OgemaHttpRequest req) {
		super(parent, id, req);
		this.object = object;
		this.resData = resData;
		this.appMan = appMan;
		this.controller = controller;
		setDefaultAddEmptyOption(true, "--");
		setTemplate(new DefaultDisplayTemplate<DevelopmentTask>() {
			@Override
			public String getLabel(DevelopmentTask object, OgemaLocale locale) {
				return ResourceUtils.getHumanReadableShortName(object);
			}
		});
	}

	@Override
	public void onGET(OgemaHttpRequest req) {
		List<DevelopmentTask> items = getEffectiveKnownDevTasks(resData.appConfigData.knownDevelopmentTasks());
		DevelopmentTask select = null;
		if(object.devTask().exists())
			select = object.devTask().getLocationResource();
		update(items, select, req);
	}
	
	@Override
	public void onPOSTComplete(String data, OgemaHttpRequest req) {
		DevelopmentTask select = getSelectedItem(req);
		selectDevelopmentTask(select, object, appMan);
		controller.updateAlarmingWithRetard();
		//controller.updateAlarming(TimeProcUtil.MINUTE_MILLIS, true);
	}

	public static List<DevelopmentTask> getEffectiveKnownDevTasks(ResourceList<DevelopmentTask> knownDevelopmentTasks) {
		List<DevelopmentTask> all = knownDevelopmentTasks.getAllElements();
		List<DevelopmentTask> items = new ArrayList<>();
		for(DevelopmentTask dt: all) {
			if(!dt.name().getValue().isEmpty())
				items.add(dt);
		}
		return items;
	}
	
	public static void selectDevelopmentTask(DevelopmentTask select, InstallAppDevice object, ApplicationManager appMan) {
		if(select == null && object.devTask().isReference(false))
			object.devTask().delete();
		else {
			object.devTask().setAsReference(select);
			if(select.overWriteTemplateRequest().getValue()) {
				InstallAppDeviceBase existing = AlarmingConfigUtil.getTemplate(object, select.templates().getAllElements());
				if(existing != null)
					existing.delete();
				select.overWriteTemplateRequest().setValue(false);
			}
			AlarmingConfigUtil.getOrCreateTemplate(object, select.templates(), appMan);
		}		
	}}
