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

import java.util.Arrays;

import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.IntegerResource;
import org.smartrplace.apps.hw.install.HardwareInstallController;

import de.iwes.util.collectionother.IPNetworkHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.resource.widget.dropdown.ValueResourceDropdown;
import de.iwes.widgets.resource.widget.textfield.BooleanResourceCheckbox;

public class ConfigurationPageHWInstall {
	//private final WidgetPage<?> page;
	private final HardwareInstallController controller;

	public ConfigurationPageHWInstall(final WidgetPage<?> page, final HardwareInstallController app) {
		
		//this.page = page;
		this.controller = app;
		
		ValueResourceDropdown<IntegerResource> loggingAutoActivation = new ValueResourceDropdown<IntegerResource>(page, "loggingAutoMode",
				app.appConfigData.autoLoggingActivation(), Arrays.asList(new String[] {"do not activate logging automatically",
						"activate logging for all datapoints of new devices",
						"activate all logging configured for all devices found on each startup"}));
		
		BooleanResourceCheckbox autoTransfer = new BooleanResourceCheckbox(page, "autoTransfer",
				"", app.appConfigData.autoTransferActivation()) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				BooleanResource sel = getSelectedItem(req);
				if(!sel.isActive()) {
					sel.create();
					sel.activate(false);
				}
			}
		};
		
		BooleanResourceCheckbox autoApplyTemplate = new BooleanResourceCheckbox(page, "autoApplyTemplate",
				"", app.appConfigData.autoConfigureNewDevicesBasedOnTemplate()) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				BooleanResource sel = getSelectedItem(req);
				if(!sel.isActive()) {
					sel.create();
					sel.activate(false);
				}
			}
		};

		BooleanResourceCheckbox includeInactiveDevices = new BooleanResourceCheckbox(page, "includeInactiveDevices",
				"", app.appConfigData.includeInactiveDevices()) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				BooleanResource sel = getSelectedItem(req);
				if(!sel.isActive()) {
					sel.create();
					sel.activate(false);
				}
			}
		};

		String ipText = IPNetworkHelper.getLocalIPAddress();
		Label localIP = new Label(page, "localIP", ipText);
		
		StaticTable configTable = new StaticTable(5, 2);
		int i = 0;
		configTable.setContent(i, 0, "Auto-logging activation for new and existing devices").
		setContent(i, 1, loggingAutoActivation);
		i++;
		configTable.setContent(i, 0, "Activate data transfer to backup server when logging is activated").
				setContent(i, 1, autoTransfer);
		i++;
		configTable.setContent(i, 0, "Apply template device configuration (mostly alarming) to any new device").
		setContent(i, 1, autoApplyTemplate);
		i++;
		configTable.setContent(i, 0, "Include inactive devices: This may lead to showing devices in more than one DeviceHandlerProvider").
		setContent(i, 1, includeInactiveDevices);
		i++;
		configTable.setContent(i, 0, "Local IP Address:").
		setContent(i, 1, localIP);
		i++;
		
		page.append(configTable);
	}
}
