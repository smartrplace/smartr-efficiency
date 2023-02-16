/**
 * ﻿Copyright 2020-2022 Smartrplace GmbH
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
import java.util.Collection;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.externalviewer.extensions.DefaultScheduleViewerConfigurationProviderExtended;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButton;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.model.devices.connectiondevices.ElectricityConnectionBox;
import org.ogema.model.gateway.LocalGatewayInformation;
import org.ogema.model.metering.ElectricityMeter;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.util.extended.eval.widget.IntegerMultiButton;
import org.ogema.util.extended.eval.widget.IntegerResourceMultiButton;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.expert.plottest.ScheduleViewerTest;
import org.smartrplace.apps.hw.install.gui.ScheduleViewerConfigProvHW;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatUtil;
import org.smartrplace.device.testing.ThermostatTestingConfig;
import org.smartrplace.heartbeat.gw.gui.OpenRequestTable;
import org.smartrplace.iotawatt.ogema.resources.IotaWattElectricityConnection;
import org.smartrplace.tissue.util.logconfig.VirtualSensorKPIMgmt;
import org.smartrplace.util.virtualdevice.HmSetpCtrlManager;
import org.smartrplace.util.virtualdevice.HmSetpCtrlManagerTHSetp;
import org.smatrplace.apps.hw.install.gui.mainexpert.MainPageExpert;

import de.iwes.util.collectionother.IPNetworkHelper;
import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.extended.util.UserLocaleUtil;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.resource.widget.calendar.DatepickerTimeResource;
import de.iwes.widgets.resource.widget.dropdown.ResourceDropdown;
import de.iwes.widgets.resource.widget.dropdown.ValueResourceDropdown;
import de.iwes.widgets.resource.widget.textfield.BooleanResourceCheckbox;
import de.iwes.widgets.resource.widget.textfield.ResourceTextField;
import de.iwes.widgets.resource.widget.textfield.TimeResourceTextField;
import de.iwes.widgets.resource.widget.textfield.TimeResourceTextField.Interval;
import de.iwes.widgets.resource.widget.textfield.ValueResourceTextField;
import de.iwes.widgets.template.DefaultDisplayTemplate;

@SuppressWarnings("serial")
public class ConfigurationPageHWInstall {
	public static final Float DEFAULT_TEACHIN_DURATION = 30f;
	//private final WidgetPage<?> page;
	private final HardwareInstallController controller;
	protected final boolean baseVersion;

	public ConfigurationPageHWInstall(final WidgetPage<?> page, final HardwareInstallController app,
			boolean baseVersion) {
		
		//this.page = page;
		this.controller = app;
		this.baseVersion = baseVersion;
		if(app.appConfigData.mainMeter().exists()) {
		//	CompletableFuture.runAsync(() -> {
				VirtualSensorKPIMgmt.waitForCollectingGatewayServerInit(app.appMan.getResourceAccess());
				setIotawattAsMainMeter(app.appConfigData.mainMeter());
		//	});
		}
		
		final Alert alert = new Alert(page, "alert", "");
		page.append(alert);
		
		final LocalGatewayInformation gwInfo = ResourceHelper.getLocalGwInfo(controller.appMan);
		TemplateDropdown<OgemaLocale> languageDrop = new TemplateDropdown<OgemaLocale>(page,
				"languageDrop") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				String sel = gwInfo.systemLocale().getValue();
				if(sel == null || sel.isEmpty())
					sel = "de"; //TODO
				OgemaLocale l = OgemaLocale.getLocale(sel);
				selectItem(l, req);
			}
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				OgemaLocale l = OgemaLocale.getLocale(getSelectedValue(req));
				if (l == null) return;
				ValueResourceHelper.setCreate(gwInfo.systemLocale(), l.getLanguage());
				UserLocaleUtil.setSystemDefaultLocale(l.getLanguage(), null);
			}
		};
		languageDrop.setTemplate(new DefaultDisplayTemplate<OgemaLocale>() {
			@Override
			public String getLabel(OgemaLocale object, OgemaLocale locale) {
				return object.getLocale().getDisplayLanguage();
			}
		});
		languageDrop.setDefaultItems(OgemaLocale.getAllLocales());
		
		Label frameworkTimeLabel = new Label(page, "frameworkTimeLabel") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				String text = StringFormatHelper.getFullTimeDateInLocalTimeZone(controller.dpService.getFrameworkTime());
				setText(text, req);
			}
		};
		frameworkTimeLabel.setDefaultPollingInterval(OpenRequestTable.DEFAULT_POLL_RATE);

		Label localIP = new Label(page, "localIP") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				String ipText = IPNetworkHelper.getLocalIPAddress();
				setText(ipText, req);
			}
		};

		Button manualDeviceIdBut = new Button(page, "manualDeviceIdBut") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				long status = app.appConfigData.deviceIdManipulationUntil().getValue();
				if(status == 0)
					setText("Enable for 1 hour", req);
				else {
					long now = app.appMan.getFrameworkTime();
					if(status < now) {
						app.appConfigData.deviceIdManipulationUntil().setValue(0);
						setText("Enable for 1 hour", req);
					} else
						setText("Disable remaining "+StringFormatHelper.getFormattedFutureValue(app.appMan, status), req);
				}
			}
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				long status = app.appConfigData.deviceIdManipulationUntil().getValue();
				if(status == 0)
					ValueResourceHelper.setCreate(app.appConfigData.deviceIdManipulationUntil(),
							app.appMan.getFrameworkTime()+TimeProcUtil.HOUR_MILLIS);
				else
					app.appConfigData.deviceIdManipulationUntil().setValue(0);
			}
		};
		ButtonConfirm autoResetDeviceIds = new ButtonConfirm(page, "autoResetDeviceIds") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				boolean status = app.appConfigData.blockAutoResetOfDeviceIds().getValue();
				if(status) {
					setText("Auto-reset thermostats blocked", req);
					disable(req);
					return;
				} else {
					setText("Auto-reset thermostats", req);
				}
				long manUntil = app.appConfigData.deviceIdManipulationUntil().getValue();
				if(manUntil == 0)
					disable(req);
				else {
					long now = app.appMan.getFrameworkTime();
					if(manUntil < now) {
						manUntil = 0;
						disable(req);
					} else
						enable(req);					
				}
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				boolean status = app.appConfigData.blockAutoResetOfDeviceIds().getValue() ||
						(app.appConfigData.deviceIdManipulationUntil().getValue()==0);
				if(status)
					return;
				for(InstallAppDevice iad: app.appConfigData.knownDevices().getAllElements()) {
					if(iad.devHandlerInfo().getValue().endsWith("DeviceHandlerThermostat")) {
						iad.delete();
					}
				}
				//LocalDeviceId.resetDeviceIds(app.appConfigData.knownDevices().getAllElements(), app.dpService);
				ValueResourceHelper.setCreate(app.appConfigData.blockAutoResetOfDeviceIds(), true);
			}
		};
		autoResetDeviceIds.setDefaultConfirmMsg("Really delete all thermostat knownDevice entries? Please stop and start Search Devices afterwards.");

		Button disableActionConfirmationBut = new Button(page, "disableActionConfirmationBut") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				long status = app.appConfigData.disableConfirmationUntil().getValue();
				if(status == 0)
					setText("Enable perform action without confirmation for 1 hour, select default action below:", req);
				else {
					long now = app.appMan.getFrameworkTime();
					if(status < now) {
						app.appConfigData.disableConfirmationUntil().setValue(0);
						setText("Enable perform action without confirmation for 1 hour, select default action below:", req);
					} else
						setText("Skip remaining "+StringFormatHelper.getFormattedFutureValue(app.appMan, status), req);
				}
			}
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				long status = app.appConfigData.disableConfirmationUntil().getValue();
				if(status == 0)
					ValueResourceHelper.setCreate(app.appConfigData.disableConfirmationUntil(),
							app.appMan.getFrameworkTime()+TimeProcUtil.HOUR_MILLIS);
				else
					app.appConfigData.disableConfirmationUntil().setValue(0);
			}
		};
		final TemplateDropdown<String> actionDefaultDrop = new TemplateDropdown<String>(page, "actionDrop") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				update(MainPageExpert.ACTIONS, req);
			}
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				String action = getSelectedItem(req);
				MainPageExpert.defaultActionAfterReload = action;
			}
		};
		actionDefaultDrop.setDefaultItems(MainPageExpert.ACTIONS);
		final TemplateDropdown<String> actionTrashDefaultDrop = new TemplateDropdown<String>(page, "actionTrashDrop") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				update(MainPageExpert.ACTIONS_TRASH, req);
			}
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				String action = getSelectedItem(req);
				MainPageExpert.defaultTrashActionAfterReload = action;
			}
		};
		actionTrashDefaultDrop.setDefaultItems(MainPageExpert.ACTIONS_TRASH);
		
		final StaticTable configTable;
		if(baseVersion)
			configTable = new StaticTable(14, 2);			
		else
			configTable = new StaticTable(35, 2);
		int i = 0;

		if(gwInfo != null) {
			StringResource baseUrlRes = gwInfo.gatewayBaseUrl();
			ResourceTextField<StringResource> baseUrl = new ValueResourceTextField<StringResource>(page,
					"baseUrlEdit", baseUrlRes);
			StringResource databaseUrlRes = gwInfo.gatewayOperationDatabaseUrl();
			ResourceTextField<StringResource> databaseUrl = new ValueResourceTextField<StringResource>(page,
					"databaseUrlEdit", databaseUrlRes);
			StringResource linkOverviewUrlRes = gwInfo.gatewayLinkOverviewUrl();
			ResourceTextField<StringResource> linkOverviewUrl = new ValueResourceTextField<StringResource>(page,
					"linkOverviewUrlEdit", linkOverviewUrlRes);
						
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
			configTable.setContent(i, 0, "Operation Documenation URL for access via internet:").
			setContent(i, 1, databaseUrl);
			i++;
			configTable.setContent(i, 0, "Additional overview on gateway documentation data in the database and elsewhere:").
			setContent(i, 1, linkOverviewUrl);
			i++;
			configTable.setContent(i, 0, "GatewayId:").
			setContent(i, 1, gwIdLabel);
			i++;
			
			//update config
			if(linkOverviewUrlRes.exists()) {
				String curLink = linkOverviewUrlRes.getValue();
				if(curLink.contains("gitlab.com/smartrplace/smartrplace-main/-/wikis/")) {
					String newLink = curLink.replace("gitlab.com/smartrplace/smartrplace-main/-/wikis/",
							"gitlab.smartrplace.de/i1/smartrplace/smartrplace-main/-/wikis/");
					linkOverviewUrlRes.setValue(newLink);
				}
			}
		}
		configTable.setContent(i, 0, "System default language").
		setContent(i, 1, languageDrop);
		i++;
		configTable.setContent(i, 0, "Local IP Address:").
		setContent(i, 1, localIP);
		i++;		
		configTable.setContent(i, 0, "Framework Time:").
		setContent(i, 1, frameworkTimeLabel);
		i++;

		if(!baseVersion) {
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

			DatepickerTimeResource defaultRefTime = new DatepickerTimeResource(page, "defaultRefTime") {
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					super.onPOSTComplete(data, req);
					controller.dpService.virtualScheduleService().resetAll();
				}
			};
			TimeResource refRes = TimeProcUtil.getDefaultMeteringReferenceResource(controller.appMan.getResourceAccess());
			defaultRefTime.selectDefaultItem(refRes);

			Button updateViaHeartbeat = new Button(page, "updateViaHeartbeat", "Update Datapoints for Heartbeat Transfer") {
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					ViaHeartbeatUtil.updateAllTransferRegistrations(controller.dpService, controller.appMan.getResourceAccess(),
							Boolean.getBoolean("org.smartrplace.app.srcmon.isgateway"));
				};
			};
			
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
						setIotawattAsMainMeter(selected.elConn());
					} else if(app.appConfigData.mainMeter().exists()) {
						IotaWattElectricityConnection selected1 = ResourceHelper.getFirstParentOfType(app.appConfigData.mainMeter().getLocationResource(), IotaWattElectricityConnection.class);
						if(selected1 != null)
							app.appConfigData.mainMeter().delete();					
					}
				}
			};
			mainMeterDrop.setDefaultAddEmptyOption(true, "No main meter selected");
	
			ResourceDropdown<ElectricityConnectionBox> mainMeterOtherDrop = new ResourceDropdown<ElectricityConnectionBox>(page, "mainMeterOtherDrop",
					false, ElectricityConnectionBox.class, null, controller.appMan.getResourceAccess()) {
				@Override
				public void onGET(OgemaHttpRequest req) {
					ElectricityConnectionBox selected = null;
					if(app.appConfigData.mainMeter().isReference(false)) {
						IotaWattElectricityConnection selected1 = ResourceHelper.getFirstParentOfType(app.appConfigData.mainMeter().getLocationResource(), IotaWattElectricityConnection.class);
						if(selected1 == null) {
							selected = ResourceHelper.getFirstParentOfType(app.appConfigData.mainMeter().getLocationResource(), ElectricityConnectionBox.class);
						}
					}
					if(selected != null && selected.exists())
						selectItem(selected, req);
					else
						selectItem(null, req);
				}
			
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					ElectricityConnectionBox selected = getSelectedItem(req);
					if(selected != null) {
						app.appConfigData.mainMeter().setAsReference(selected.connection());
						//setIotawattAsMainMeter(selected.elConn());
					} else if(app.appConfigData.mainMeter().exists()) {
						IotaWattElectricityConnection selected1 = ResourceHelper.getFirstParentOfType(app.appConfigData.mainMeter().getLocationResource(), IotaWattElectricityConnection.class);
						if(selected1 == null)
							app.appConfigData.mainMeter().delete();
					}
				}
			};
			mainMeterOtherDrop.setDefaultAddEmptyOption(true, "No main meter selected");
	
			ResourceDropdown<ElectricityMeter> mainMeterIECDrop = new ResourceDropdown<ElectricityMeter>(page, "mainMeterIECDrop",
					false, ElectricityMeter.class, null, controller.appMan.getResourceAccess()) {
				@Override
				public void onGET(OgemaHttpRequest req) {
					ElectricityMeter selected = null;
					if(app.appConfigData.mainMeterAsElMeter().isReference(false)) {
						selected = app.appConfigData.mainMeterAsElMeter();
					}
					if(selected != null && selected.exists())
						selectItem(selected, req);
					else
						selectItem(null, req);
				}
			
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					ElectricityMeter selected = getSelectedItem(req);
					if(selected != null) {
						app.appConfigData.mainMeterAsElMeter().setAsReference(selected);
						//setIotawattAsMainMeter(selected.elConn());
					} else if(app.appConfigData.mainMeterAsElMeter().exists()) {
						app.appConfigData.mainMeterAsElMeter().delete();
					}
				}
			};
			mainMeterIECDrop.setDefaultAddEmptyOption(true, "No main meter selected");
	
			//mainMeterDrop.setTemplate(template);
			
			ValueResourceDropdown<IntegerResource> extendedViewModeDrop =
					new ValueResourceDropdown<IntegerResource>(page, "extendedViewModeDrop", app.appConfigData.extendedViewMode(),
					Arrays.asList(new String[] {"No extended view", "master only (no extended pages)", "All users based on permissions"}));
			
			ValueResourceDropdown<IntegerResource> showPageOrderDrop =
					new ValueResourceDropdown<IntegerResource>(page, "showPageOrderDrop", app.appConfigData.showModePageOrder(),
					Arrays.asList(new String[] {"Locations, Kni/Gap Eval", "Kni/Gap Eval, Locations"}));
	
			Datapoint dp = controller.dpService.getDataPointStandard("Gateway_Device/systemRestart");
			DefaultScheduleViewerConfigurationProviderExtended schedViewProv = ScheduleViewerConfigProvHW.getInstance();
			final ScheduleViewerOpenButton testPlot = ScheduleViewerTest.getPlotButton(page, "testPlot",
					controller.dpService, controller.appMan, Arrays.asList(new Datapoint[] {dp}), schedViewProv);
			
			ValueResourceTextField<StringResource> co2singleUserEdit = new ValueResourceTextField<StringResource>(page, "co2singleUserEdit",
					app.appConfigData.singleCO2AlarmingUser());
			
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
			
			Button testDevHand = new Button(page, "testDevHand", "Test DeviceHandler on console") {
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					String path = "hardwareInstallConfig/knownDevices/knownDevices_117";
					System.out.println("Testing "+path+"...");
					InstallAppDevice dev = controller.appMan.getResourceAccess().getResource(path);
					if(dev == null)
						System.out.println("Resouce not found!");
					DeviceHandlerProviderDP<Resource> devHand = controller.dpService.getDeviceHandlerProvider(dev);
					Collection<Datapoint> dps = devHand.getDatapoints(dev, controller.dpService);
					System.out.println("Found dps:"+dps.size());
				}
			};
			
			autoResetDeviceIds.setDefaultConfirmMsg("!!! REALLY RESET ALL DEVICEIDs ?  Note that this must NEVER be done when the "
					+ "hardware devices already have been labelled !!!");
			
			configTable.setContent(i, 0, "Extended view mode:").
			setContent(i, 1, extendedViewModeDrop);
			i++;
			configTable.setContent(i, 0, "Expert page(s) version order:").
			setContent(i, 1, showPageOrderDrop);
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
			
			configTable.setContent(i, 0, "Main Meter (Others). Note that selecting a main meter here overrides a Iotawatt main meter and vice-versa. "
					+ "Note that this has to be supported in the respective device handler and no Non-Iotawatt meters support this currently.").
			setContent(i, 1, mainMeterOtherDrop);
			i++;

			configTable.setContent(i, 0, "Main Meter (HM-IEC). Note that selecting a meter here can be done in addition to a Iotawatt or Other, but "
					+ " may lead to an unpredictable mainMeterConsumption datapoint alias").
			setContent(i, 1, mainMeterIECDrop);
			i++;

			//configTable.setContent(i, 0, "Update generalBackup.zip file").
			//setContent(i, 1, generalBackupUpdateButton);
			//i++;

			configTable.setContent(i, 0, "Interval for alarming evaluation (days behind now)").
			setContent(i, 1, alarmEvalIntervalEdit);
			i++;
			
			ThermostatTestingConfig thTest = ResourceHelper.getEvalCollection(controller.appMan).getSubResource(
					"thermostatTestingConfig", ThermostatTestingConfig.class);
			TimeResourceTextField testSwitchingEdit =
					new TimeResourceTextField(page, "testSwitchingEdit", Interval.minutes, thTest.testSwitchingInterval());
			configTable.setContent(i, 0, "Test switching interval (min) - started only after system relaunch. Set zero to disable:").setContent(i, 1, testSwitchingEdit);
			i++;
			
			/*FloatResource maxDutyCycle = ResourceHelper.getEvalCollection(controller.appMan).getSubResource(
					HmCentralManager.paramMaxDutyCycle, FloatResource.class);
			ValueResourceTextField<FloatResource> maxDutyCycleEdit =
					new ValueResourceTextField<FloatResource>(page, "maxDutyCycleEdit", maxDutyCycle);
			configTable.setContent(i, 0, "Maximum Duty Cycle before traffic limit (0..1.0):").setContent(i, 1, maxDutyCycleEdit);
			i++;*/

			FloatResource maxWritePerCCUperHour = ResourceHelper.getEvalCollection(controller.appMan).getSubResource(
					HmSetpCtrlManagerTHSetp.paramMaxWritePerCCUperHour, FloatResource.class);
			ValueResourceTextField<FloatResource> maxWritePerCCUperHourEdit =
					new ValueResourceTextField<FloatResource>(page, "maxWritePerCCUperHourEdit", maxWritePerCCUperHour,
					HmSetpCtrlManager.DEFAULT_MAX_WRITE_PER_HOUR);
			configTable.setContent(i, 0, "Maximum Setpoint writes per hour checked in addition to CCU-spefic duty cycle limit. This value is regarded equivalent to DutyCycle=100%:").setContent(i, 1, maxWritePerCCUperHourEdit);
			i++;
			configTable.setContent(i, 0, "Test Plot:").setContent(i, 1, testPlot);
			i++;
			configTable.setContent(i, 0, "If this NOT empty then only to a user with the userName given by this "
					+ "value CO2 air quality alarming messages will be sent via firebase:").setContent(i, 1, co2singleUserEdit);
			i++;
			
			configTable.setContent(i, 0, "Test Button (result on console only):").setContent(i, 1, testDevHand);
			i++;
			
			if(Boolean.getBoolean("org.smartrplace.apps.hw.install.blockinitdatapoints")) {
				ButtonConfirm initDevHandsButton = new ButtonConfirm(page, "initDevHandsButton", "Start Init DeviceHandlers") {
					boolean initDone = false;
					@Override
					public void onGET(OgemaHttpRequest req) {
						if(initDone)
							setText("Ongoing or done", req);
					}
					
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						initDone = true;
						List<InstallAppDevice> allDev = controller.appConfigData.knownDevices().getAllElements();
						int count = 0;
						for(InstallAppDevice iad: allDev) {
							DeviceHandlerProviderDP<Resource> devHand = controller.dpService.getDeviceHandlerProvider(iad);
							count++;
							if(devHand == null || (!(devHand instanceof DeviceHandlerProvider)))
								continue;
							System.out.println("  Perform init "+count+"/"+allDev.size()+" of IAD:"+iad.getName()+"  with device:"+iad.device().getLocation());
							startSim((DeviceHandlerProvider<?>) devHand, iad);
						}
					}
				};
				initDevHandsButton.setDefaultConfirmMsg("Really start init of device handlers?");
				configTable.setContent(i, 0, "Perform retarded init of datapoints via DeviceHandlers:").setContent(i, 1, initDevHandsButton);
				i++;				
			}

		} else {
		}
		
		
		
		
		Button cleanUpIADsBut = new Button(page, "cleanUpIADsBut", "Clean up device data") {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				controller.cleanupOnStart();
			}
		};
		ButtonConfirm cleanUpIADsButExtended = new ButtonConfirm(page, "cleanUpIADsExt", "Clean up device data Extended") {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				controller.cleanupOnStart(true);
			}
		};
		cleanUpIADsButExtended.setDefaultConfirmMsg("Really clean up all entries for missing device handlers? Note: This should only be done when system startup is fully finished!");
		
		IntegerMultiButton allowAllInTablePages = new IntegerResourceMultiButton(page, "allowAllInTablePages",
				controller.appConfigData.allowAllDevicesInTablePagesMode()) {
			
			@Override
			protected String getText(int state, OgemaHttpRequest req) {
				switch(state) {
				case 0:
					return "Page default";
				case 1:
					return "Allow ALL";
				case 2:
					return "Deny ALL";
				default:
					throw new IllegalStateException("Unknown allowAllDeviceInTablePagesMode state:"+state);
				}
			}
		};
		
		ValueResourceTextField<FloatResource> techInModeDuration = new ValueResourceTextField<>(page, "techInDuration",
				controller.appConfigData.techInModeDuration(), DEFAULT_TEACHIN_DURATION);
		
		configTable.setContent(i, 0, "Cleanup device data:").setContent(i, 1, cleanUpIADsBut);
		i++;
		configTable.setContent(i, 0, "Cleanup device data Extended (!Be careful!):").setContent(i, 1, cleanUpIADsButExtended);
		i++;
		configTable.setContent(i, 0, "TeachInMode Duration (minutes):").setContent(i, 1, techInModeDuration);
		i++;
		configTable.setContent(i, 0, "Allow All Devices in Table Pages:").setContent(i, 1, allowAllInTablePages);
		i++;
		configTable.setContent(i, 0, "Enable manual editing of deviceIds:").setContent(i, 1, manualDeviceIdBut).setContent(i, 1, autoResetDeviceIds);
		i++;
		configTable.setContent(i, 0, "Select action for all devices and disable confirmation:").
			setContent(i, 1, disableActionConfirmationBut).setContent(i, 1, actionDefaultDrop);
		i++;
		configTable.setContent(i, 0, "Select default action for trash device page:").
		setContent(i, 1, actionTrashDefaultDrop);
	i++;

		page.append(configTable);
	}

	private <T extends PhysicalElement> void startSim(DeviceHandlerProvider<?> tableProvider, InstallAppDevice appDevice) {
		controller.startSimulationForced(tableProvider, appDevice);						
	}

	public void setIotawattAsMainMeter(ElectricityConnection conn) {
		VirtualSensorKPIMgmt.registerEnergySumDatapointOverSubPhases(conn, AggregationMode.Meter2Meter, controller.util, controller.dpService,
				TimeProcUtil.SUM_PER_DAY_EVAL, true);
	}
}
