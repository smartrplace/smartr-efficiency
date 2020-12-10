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
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.gateway.LocalGatewayInformation;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatUtil;

import de.iwes.util.collectionother.IPNetworkHelper;
import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.resource.widget.calendar.DatepickerTimeResource;
import de.iwes.widgets.resource.widget.dropdown.ValueResourceDropdown;
import de.iwes.widgets.resource.widget.textfield.BooleanResourceCheckbox;
import de.iwes.widgets.resource.widget.textfield.ResourceTextField;
import de.iwes.widgets.resource.widget.textfield.TimeResourceTextField;
import de.iwes.widgets.resource.widget.textfield.TimeResourceTextField.Interval;
import de.iwes.widgets.resource.widget.textfield.ValueResourceTextField;

public class ConfigurationPageHWInstall {
	//private final WidgetPage<?> page;
	private final HardwareInstallController controller;

	@SuppressWarnings("serial")
	public ConfigurationPageHWInstall(final WidgetPage<?> page, final HardwareInstallController app) {
		
		//this.page = page;
		this.controller = app;
		
		ValueResourceDropdown<IntegerResource> loggingAutoActivation = new ValueResourceDropdown<IntegerResource>(page, "loggingAutoMode",
				app.appConfigData.autoLoggingActivation(), Arrays.asList(new String[] {"do not activate logging automatically",
						"activate logging for all datapoints of new devices",
						"activate all logging configured for all devices found on each startup"}));
		
		BooleanResourceCheckbox autoTransfer = new BooleanResourceCheckbox(page, "autoTransfer",
				"", app.appConfigData.autoTransferActivation()) {

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

			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				BooleanResource sel = getSelectedItem(req);
				if(!sel.isActive()) {
					sel.create();
					sel.activate(false);
				}
			}
		};

		Label frameworkTimeLabel = new Label(page, "frameworkTimeLabel") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				String text = StringFormatHelper.getFullTimeDateInLocalTimeZone(controller.dpService.getFrameworkTime());
				setText(text, req);
			}
		};

		DatepickerTimeResource defaultRefTime = new DatepickerTimeResource(page, "defaultRefTime");
		TimeResource refRes = TimeProcUtil.getDefaultMeteringReferenceResource(controller.appMan.getResourceAccess());
		defaultRefTime.selectDefaultItem(refRes);
		
		Label localIP = new Label(page, "localIP") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				String ipText = IPNetworkHelper.getLocalIPAddress();
				setText(ipText, req);
			}
		};

		Button updateViaHeartbeat = new Button(page, "updateViaHeartbeat", "Update Datapoints") {
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				ViaHeartbeatUtil.updateAllTransferRegistrations(controller.dpService, controller.appMan.getResourceAccess(),
						Boolean.getBoolean("org.smartrplace.app.srcmon.isgateway"));
			};
		};
		
		ValueResourceTextField<IntegerResource> bulkNumEdit = new ValueResourceTextField<IntegerResource>(page, "bulkEditNum",
				controller.appConfigData.maxMessageNumBeforeBulk());
		ValueResourceTextField<TimeResource> bulkIntervalEdit = new TimeResourceTextField(page, "bulkIntervalEdit", Interval.minutes);
		bulkIntervalEdit.selectDefaultItem(controller.appConfigData.bulkMessageIntervalDuration());
		
		StaticTable configTable = new StaticTable(12, 2);
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
		configTable.setContent(i, 0, "Bulk Alarm Messages: Maximum alarming messages to be sent within duration before\n" + 
				"messages are aggregated into a single bulk message").
		setContent(i, 1, bulkNumEdit);
		i++;
		configTable.setContent(i, 0, "Duration of bulk message aggregation before bulk messages are sent (minutes)").
		setContent(i, 1, bulkIntervalEdit);
		i++;
		
		final LocalGatewayInformation gwInfo = ResourceHelper.getLocalGwInfo(controller.appMan.getResourceAccess());
		if(gwInfo != null) {
			StringResource baseUrlRes = gwInfo.gatewayBaseUrl();
			ResourceTextField<StringResource> baseUrl = new ValueResourceTextField<StringResource>(page,
					"baseUrlEdit", baseUrlRes);
						
			Label gwIdLabel = new Label(page, "gwIdLabel") {
				@Override
				public void onGET(OgemaHttpRequest req) {
					String gwId = gwInfo.id().getValue();
					setText(gwId, req);
				}
			};

			configTable.setContent(i, 0, "Base URL for access via internet, e.g. https://customer.manufacturer.de:2000").
			setContent(i, 1, baseUrl);
			i++;
			configTable.setContent(i, 0, "GatewayId:").
			setContent(i, 1, gwIdLabel);
			i++;
		}
		configTable.setContent(i, 0, "Local IP Address:").
		setContent(i, 1, localIP);
		i++;		
		configTable.setContent(i, 0, "Framework Time:").
		setContent(i, 1, frameworkTimeLabel);
		i++;
		configTable.setContent(i, 0, "Standard Referenzzeit für virtuelle Zähler/Auswertungen:").
		setContent(i, 1, defaultRefTime);
		i++;
		
		configTable.setContent(i, 0, "Update datapoints for transfer via heartbeat from datapoint groups").
		setContent(i, 1, updateViaHeartbeat);
		i++;
		
		page.append(configTable);
	}
}
