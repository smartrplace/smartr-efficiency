package org.smartrplace.smarteff.defaultservice;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.externalviewer.extensions.DefaultScheduleViewerConfigurationProviderExtended;

import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationProvider;

@Service(ScheduleViewerConfigurationProvider.class)
@Component
public class ScheduleViewerConfigProvTSMan extends DefaultScheduleViewerConfigurationProviderExtended {
	public static final String PROVIDER_ID = "SmartrEffTSMan";

	protected static volatile Map<String, SessionConfiguration> configs = new ConcurrentHashMap<>();
	protected static int lastConfig = 0;

	@Override
	protected Map<String, SessionConfiguration> configs() {
		return configs;
	}
	
	@Override
	public String getConfigurationProviderId() {
		return PROVIDER_ID;
	}

	@Override
	protected String getNextId() {
		lastConfig = super.getNextId(lastConfig, MAX_ID);
		return ""+lastConfig;
	}
	
	public static DefaultScheduleViewerConfigurationProviderExtended getInstance() {
		if(instance == null) instance = new ScheduleViewerConfigProvTSMan();
		return instance;
	}

	protected static volatile ScheduleViewerConfigProvTSMan instance = null;
	@Override
	protected DefaultScheduleViewerConfigurationProviderExtended getInstanceObj() {
		return instance;
	}
	@Override
	protected void setInstance(DefaultScheduleViewerConfigurationProviderExtended instanceIn) {
		instance = (ScheduleViewerConfigProvTSMan) instanceIn;
	}
}
