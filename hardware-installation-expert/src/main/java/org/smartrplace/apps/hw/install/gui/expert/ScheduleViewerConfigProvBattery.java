/**
 * ﻿Copyright 2014-2018 Fraunhofer-Gesellschaft zur Förderung der angewandten Wissenschaften e.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartrplace.apps.hw.install.gui.expert;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.externalviewer.extensions.DefaultScheduleViewerConfigurationProviderExtended;

import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationProvider;

@Service(ScheduleViewerConfigurationProvider.class)
@Component
public class ScheduleViewerConfigProvBattery extends DefaultScheduleViewerConfigurationProviderExtended {
	public static final String PROVIDER_ID = "OfflineEvaluationControlBattery";

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
		if(instance == null) instance = new ScheduleViewerConfigProvBattery();
		return instance;
	}

	protected static volatile ScheduleViewerConfigProvBattery instance = null;
	@Override
	protected DefaultScheduleViewerConfigurationProviderExtended getInstanceObj() {
		return instance;
	}
	@Override
	protected void setInstance(DefaultScheduleViewerConfigurationProviderExtended instanceIn) {
		instance = (ScheduleViewerConfigProvBattery) instanceIn;
	}

}
