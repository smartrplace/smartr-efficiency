package org.smartrplace.apps.alarmingconfig.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

public class OngoingBaseAlarmsPage extends MainPage {

	public OngoingBaseAlarmsPage(WidgetPage<?> page, ApplicationManagerPlus appManPlus) {
		super(page, appManPlus);
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return "4. Active Alarms";
	}
	@Override
	public Collection<AlarmConfiguration> getObjectsInTable(OgemaHttpRequest arg0) {
		Collection<AlarmConfiguration> all = super.getObjectsInTable(arg0);
		List<AlarmConfiguration> result = new ArrayList<>();
		for(AlarmConfiguration ac: all) {
			IntegerResource status = AlarmingConfigUtil.getAlarmStatus(ac.sensorVal().getLocationResource());
			if(status == null)
				continue;
			if(status.getValue() > 0)
				result.add(ac);
		}
		return result;
	}
	
	@Override
	protected void addAdditionalWidgets(AlarmConfiguration sr,
			ObjectResourceGUIHelper<AlarmConfiguration, AlarmConfiguration> vh, String id, OgemaHttpRequest req,
			Row row, ApplicationManager appMan) {
		if(req == null)
			vh.registerHeaderEntry("Last Update");
		else {
			ValueResource res = sr.sensorVal().getLocationResource();
			IntegerResource statusRes = AlarmingConfigUtil.getAlarmStatus(res);
			if(statusRes == null)
				return;
			long lastUpd = statusRes.getLastUpdateTime();
			vh.timeLabel("Last Update", id, lastUpd, row, 2);
		}
		
	}
}
