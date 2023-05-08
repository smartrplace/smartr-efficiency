package org.smartrplace.apps.alarmingconfig.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gui.filtering.GenericFilterFixedSingle;
import org.smartrplace.gui.filtering.GenericFilterOption;
import org.smartrplace.gui.filtering.SingleFiltering;
import org.smartrplace.gui.filtering.SingleFiltering.OptionSavingMode;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.dropdown.DropdownOption;
import de.iwes.widgets.html.multiselect.Multiselect;

@SuppressWarnings("serial")
public class OngoingBaseAlarmsPage extends MainPage {
	
	private SingleFiltering<String, AlarmConfiguration> typeDrop;
	private Multiselect deviceSelector;
	
	public OngoingBaseAlarmsPage(WidgetPage<?> page, ApplicationManagerPlus appManPlus) {
		super(page, appManPlus);
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return "4. Active Alarms";
	}
	
	@Override
	public Collection<AlarmConfiguration> getObjectsInTable(OgemaHttpRequest req) {
		Collection<AlarmConfiguration> all = getObjectsInTableUnfiltered(req);
		if (deviceSelector != null) {
			final Collection<String> ids = deviceSelector.getSelectedValues(req);
			if (ids != null && !ids.isEmpty()) {
				all = all.stream()
					.filter(ac -> {
						final InstallAppDevice iad = ResourceHelper.getFirstParentOfType(ac, InstallAppDevice.class);
						return iad != null && ids.contains(iad.deviceId().getValue().toLowerCase());
					})
					.collect(Collectors.toList());
			}
		}
		return typeDrop.getFiltered(all, req);
	}
	public Collection<AlarmConfiguration> getObjectsInTableUnfiltered(OgemaHttpRequest arg0) {
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
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		
		typeDrop = new SingleFiltering<String, AlarmConfiguration>(page, "typeDrop", OptionSavingMode.GENERAL,
				10000, true) {

			@Override
			protected boolean isAttributeSinglePerDestinationObject() {
				return true;
			}

			@Override
			protected long getFrameworkTime() {
				return appMan.getFrameworkTime();
			}

			@Override
			protected String getAttribute(AlarmConfiguration object) {
				InstallAppDevice parent = ResourceHelper.getFirstParentOfType(object, InstallAppDevice.class);
				if(parent == null)
					return "NULL";
				return parent.device().getResourceType().getSimpleName();
			}
			
			@Override
			protected List<GenericFilterOption<String>> getOptionsDynamic(OgemaHttpRequest req) {
				Collection<AlarmConfiguration> all = getObjectsInTableUnfiltered(req);
				List<GenericFilterOption<String>> result = new ArrayList<>();
				Set<String> done = new HashSet<String>();
				for(AlarmConfiguration res: all) {
					InstallAppDevice parent = ResourceHelper.getFirstParentOfType(res, InstallAppDevice.class);
					if(parent == null)
						continue;
					String name = parent.device().getResourceType().getSimpleName();
					if(done.contains(name))
						continue;
					done.add(name);	
					result.add(new GenericFilterFixedSingle<String>(name, name));
				}
				return result;
			}
		};
		typeDrop.registerDependentWidget(mainTable);
		topTable.setContent(0, 1, typeDrop);
		
		deviceSelector = new Multiselect(page, "deviceIdSelector") {
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				final Set<DropdownOption> options = OngoingBaseAlarmsPage.super.getObjectsInTable(req).stream()
						.filter(alarm -> {
							final IntegerResource status = AlarmingConfigUtil.getAlarmStatus(alarm.sensorVal().getLocationResource());
							return status != null && status.getValue() > 0;
						})
						.map(ac -> ResourceHelper.getFirstParentOfType(ac, InstallAppDevice.class))
						.filter(Objects::nonNull)
						.map(iad -> iad.deviceId().getValue().trim())
						.map(id -> new DropdownOption(id.toLowerCase(), id, false))
						.collect(Collectors.toSet());
				final List<DropdownOption> options2 = new ArrayList<>(options);
				Collections.sort(options2, (o1, o2) ->  o1.id().compareTo(o2.id()));
				setOptions( options2, req);
				
				final Map<String, String[]> params = getPage().getPageParameters(req);
				final List<String> deviceIds = params == null || !params.containsKey("device") ? null 
						: Arrays.stream(params.get("device")).map(dev -> dev.trim().toLowerCase()).filter(dev -> !dev.isEmpty()).collect(Collectors.toList());
				if (deviceIds != null && !deviceIds.isEmpty())
					selectMultipleOptions(deviceIds, req);
			}
			
		};
		deviceSelector.setDefaultSelectByUrlParam("device");
		deviceSelector.triggerAction(mainTable, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		
		final PageSnippet snip = new PageSnippet(page, "deviceIdSnippet", true);
		final StaticTable devTable = new StaticTable(1, 2);
		devTable.setContent(0, 0, "Devices:").setContent(0, 1, deviceSelector);
		snip.append(devTable, null);
		topTable.setContent(0, 3, snip);
		
		
		
	}
	
	@Override
	public void addWidgets(AlarmConfiguration object,
			ObjectResourceGUIHelper<AlarmConfiguration, AlarmConfiguration> vh, String id, OgemaHttpRequest req,
			Row row, ApplicationManager appMan) {
		super.addWidgets(object, vh, id, req, row, appMan);
		if(req == null) {
			vh.registerHeaderEntry("DevId");
			return;
		}
		InstallAppDevice iad = ResourceHelper.getFirstParentOfType(object, InstallAppDevice.class);
		if(iad != null) {
			String devId = iad.deviceId().getValue();
			vh.stringLabel("DevId", id, devId, row);
		}
		//vh.stringLabel("Res.Location", id, object.getLocation(), row);
	}

	@Override
	protected void addAdditionalWidgets(AlarmConfiguration sr,
			ObjectResourceGUIHelper<AlarmConfiguration, AlarmConfiguration> vh, String id, OgemaHttpRequest req,
			Row row, ApplicationManager appMan) {
		if(req == null) {
			vh.registerHeaderEntry("Last Status");
			vh.registerHeaderEntry("Last Val");
		} else {
			ValueResource res = sr.sensorVal().getLocationResource();
			long lastUpdRes = res.getLastUpdateTime();
			vh.timeLabel("Last Val", id, lastUpdRes, row, 2);

			IntegerResource statusRes = AlarmingConfigUtil.getAlarmStatus(res);
			if(statusRes == null)
				return;
			long lastUpd = statusRes.getLastUpdateTime();
			vh.timeLabel("Last Status", id, lastUpd, row, 2);
		}
		
	}
	
	@Override
	public String getLineId(AlarmConfiguration object) {
		InstallAppDevice iad = ResourceHelper.getFirstParentOfType(object, InstallAppDevice.class);
		if(iad != null) {
			String devId = iad.deviceId().getValue();
			return devId+super.getLineId(object);
		}
		return super.getLineId(object);
	}
}
