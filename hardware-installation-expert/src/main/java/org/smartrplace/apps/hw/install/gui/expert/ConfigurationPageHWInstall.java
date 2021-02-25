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

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.model.devices.connectiondevices.ElectricityConnectionBox;
import org.ogema.model.gateway.LocalGatewayInformation;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatUtil;
import org.smartrplace.iotawatt.ogema.resources.IotaWattElectricityConnection;
import org.smartrplace.tissue.util.logconfig.VirtualSensorKPIMgmt;

import de.iwes.util.collectionother.IPNetworkHelper;
import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.resource.widget.calendar.DatepickerTimeResource;
import de.iwes.widgets.resource.widget.dropdown.ResourceDropdown;
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
		if(app.appConfigData.mainMeter().exists())
			setMainMeter(app.appConfigData.mainMeter());
		
		final Alert alert = new Alert(page, "alert", "");
		page.append(alert);
		
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

		/*BooleanResourceCheckbox includeInactiveDevices = new BooleanResourceCheckbox(page, "includeInactiveDevices",
				"", app.appConfigData.includeInactiveDevices()) {

			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				BooleanResource sel = getSelectedItem(req);
				if(!sel.isActive()) {
					sel.create();
					sel.activate(false);
				}
			}
		};*/

		Label frameworkTimeLabel = new Label(page, "frameworkTimeLabel") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				String text = StringFormatHelper.getFullTimeDateInLocalTimeZone(controller.dpService.getFrameworkTime());
				setText(text, req);
			}
		};

		DatepickerTimeResource defaultRefTime = new DatepickerTimeResource(page, "defaultRefTime") {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				super.onPOSTComplete(data, req);
				controller.dpService.virtualScheduleService().resetAll();
			}
		};
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
		
		/*ButtonConfirm generalBackupUpdateButton = new ButtonConfirm(page, "generalBackupUpdateButton", "Update generalBackup.zip") {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				//Create Backup
				ResourceList<?> configList = controller.appMan.getResourceAccess().getResource("resAdminConfig/configList");
				if(configList == null) {
					alert.showAlert("Could not find resAdminConfig/configList", false, req);
					return;
				}
				Resource el = ResourceListHelper.getNamedElementFlex("DailyBackupToSend", configList);
				if(el == null) {
					alert.showAlert("Could not find DailyBackupToSend", false, req);
					return;
				}
				BackupAction run = el.getSubResource("run", BackupAction.class);
				ActionHelper.performActionBlocking(run, 20000);
				
				//Create Zip (and send)
			}
		};
		generalBackupUpdateButton.setDefaultConfirmMsg("To transfer the file to the server perform /gateway-backup.sh generalBackup on the console of the gateway."
				+ " To transfer to gateway perform sync.sh in rundir-src-test on development PC.");
		*/
		Button resetEnergyServer = new Button(page, "resetEnergyServer", "Reset Energy Server") {
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				Resource ese = controller.appMan.getResourceAccess().getResource("EnergyServerReadings_ESE");
				for(ElectricityConnectionBox ecb: ese.getSubResources(ElectricityConnectionBox.class, false)) {
					StringResource nut = ecb.getSubResource("nextUpdateTime", StringResource.class);
					if(nut.exists())
						nut.delete();
				}
				alert.showAlert("Reset of EnergyServer DONE !", true, req);
			};
		};
		resetEnergyServer.registerDependentWidget(alert);
		
		ValueResourceTextField<IntegerResource> bulkNumEdit = new ValueResourceTextField<IntegerResource>(page, "bulkEditNum",
				controller.appConfigData.maxMessageNumBeforeBulk());
		ValueResourceTextField<TimeResource> bulkIntervalEdit = new TimeResourceTextField(page, "bulkIntervalEdit", Interval.minutes);
		bulkIntervalEdit.selectDefaultItem(controller.appConfigData.bulkMessageIntervalDuration());
		
		ValueResourceTextField<TimeResource> alarmEvalIntervalEdit = new TimeResourceTextField(page, "alarmEvalIntervalEdit", Interval.days);
		alarmEvalIntervalEdit.selectDefaultItem(controller.appConfigData.basicEvalInterval());

		ResourceDropdown<IotaWattElectricityConnection> mainMeterDrop = new ResourceDropdown<IotaWattElectricityConnection>(page, "mainMeterDrop",
				false, IotaWattElectricityConnection.class, null, controller.appMan.getResourceAccess()) {
			@Override
			public void onGET(OgemaHttpRequest req) {
				IotaWattElectricityConnection selected = null;
				if(app.appConfigData.mainMeter().isReference(false)) {
					selected = ResourceHelper.getFirstParentOfType(app.appConfigData.mainMeter().getLocationResource(), IotaWattElectricityConnection.class);
				}
				if(selected != null && selected.exists())
					selectItem(selected, req);
				else
					selectItem(null, req);
			}
		
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				IotaWattElectricityConnection selected = getSelectedItem(req);
				if(selected != null) {
					app.appConfigData.mainMeter().setAsReference(selected.elConn());
					setMainMeter(selected.elConn());
				} else if(app.appConfigData.mainMeter().exists())
					app.appConfigData.mainMeter().delete();
			}
		};
		mainMeterDrop.setDefaultAddEmptyOption(true, "No main meter selected");
		//mainMeterDrop.setTemplate(template);
		
		StaticTable configTable = new StaticTable(14, 2);
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
		/*configTable.setContent(i, 0, "Include inactive devices: This may lead to showing devices in more than one DeviceHandlerProvider").
		setContent(i, 1, includeInactiveDevices);
		i++;*/
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
		configTable.setContent(i, 0, "Standard Reference Time for Virtual Meter Evaluations (!Changes take time for recalculation!)").
		setContent(i, 1, defaultRefTime);
		i++;

		if(controller.appMan.getResourceAccess().getResource("EnergyServerReadings_ESE") != null) {
			configTable.setContent(i, 0, "Reset internal meter states (Energy Server)").
			setContent(i, 1, resetEnergyServer);
		}
		i++;

		configTable.setContent(i, 0, "Update datapoints for transfer via heartbeat from datapoint groups").
		setContent(i, 1, updateViaHeartbeat);
		i++;
		
		configTable.setContent(i, 0, "Main Meter (Iotawatt)").
		setContent(i, 1, mainMeterDrop);
		i++;
		
		//configTable.setContent(i, 0, "Update generalBackup.zip file").
		//setContent(i, 1, generalBackupUpdateButton);
		//i++;

		configTable.setContent(i, 0, "Interval for alarming evaluation (days behind now)").
		setContent(i, 1, alarmEvalIntervalEdit);
		i++;
		
		page.append(configTable);
	}
	
	public void setMainMeter(ElectricityConnection conn) {
		VirtualSensorKPIMgmt.registerEnergySumDatapointOverSubPhases(conn, AggregationMode.Meter2Meter, controller.util, controller.dpService,
				TimeProcUtil.SUM_PER_DAY_EVAL, true);
	}
}
