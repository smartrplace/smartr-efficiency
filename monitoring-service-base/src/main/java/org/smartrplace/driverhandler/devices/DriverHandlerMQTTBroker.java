package org.smartrplace.driverhandler.devices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DriverHandlerProvider;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.model.prototypes.PhysicalElement;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.textfield.TextField;

public class DriverHandlerMQTTBroker implements DriverHandlerProvider {
	
	public static final String BASE_CONFIG_ID = "com.smartrplace.mqtt.PahoClientService";

	protected static int maxIdx = 0;
	public static class DriverConfigMQTTBroker implements DriverDeviceConfig {
		
		public InstallAppDevice installAppDevice = null;
		public PhysicalElement deviceRes;

		public String url = "";
		public String username = "";
		public String password = "";
		public String clientId = "";
		public String brokerConfigId;

		@Override
		public Resource getInstallResource() {
			if(installAppDevice != null)
				return installAppDevice.device();
			return null;
		}
		
		//TODO: The concept of holding this object and writing back later on is not fully thread-safe
		//If the config page is opened, then configuration is edited in the Felix Admin, then written on this config page
		//the result may be non-consistent or changes in Felix Admin will be overwritten.
		public boolean writeConfigDeviceData(ConfigurationAdmin configAdmin) {
			//DriverConfigMQTTBroker bd = brokerData.get(brokerConfigId);
			Configuration configPrev;
			try {
				configPrev = configAdmin.getConfiguration(BASE_CONFIG_ID+"~"+brokerConfigId);
				putAllData(configPrev);
				return true;
			} catch (IOException e) {
				e.printStackTrace();
	 			throw new IllegalStateException(e);
			}
		}

		protected void putAllData(Configuration configPrev) {
			try {
        	Dictionary<String, Object> dict = configPrev.getProperties();
        	DriverHandlerJMBus.putDict(dict, "url", url);
        	DriverHandlerJMBus.putDict(dict, "username", username);
        	DriverHandlerJMBus.putDict(dict, ".password", password);
        	DriverHandlerJMBus.putDict(dict, "username", clientId);
			configPrev.update(dict);			
			} catch (IOException e) {
				e.printStackTrace();
	 			throw new IllegalStateException(e);
			}
		}
		
		public boolean deleteConfigDeviceData(ConfigurationAdmin configAdmin) {
			Configuration configPrev;
			try {
				configPrev = configAdmin.getConfiguration(BASE_CONFIG_ID+"~"+brokerConfigId);
				configPrev.delete();
				return true;
			} catch (IOException e) {
				e.printStackTrace();
	 			throw new IllegalStateException(e);
			}
		}
	}

	public static class DriverConfigMQTT {
		public Map<String, DriverConfigMQTTBroker> brokerData = new HashMap<>();

		public DriverConfigMQTT(ConfigurationAdmin configAdmin) {
	        try {
				//TODO: Check if this can be automated not only for reading on startup in activate method, but reading the current
	        	//configuration any time
	        	String filter = "(service.pid=com.smartrplace.mqtt.PahoClientService*)";
	        	Configuration[] configs = configAdmin.listConfigurations(filter);
	        	if(configs != null) for(Configuration config: configs) {
	        		//TODO
	        		DriverConfigMQTTBroker data = new DriverConfigMQTTBroker();
	        		data.brokerConfigId = config.getFactoryPid();
	        		data.brokerConfigId = config.getPid();
	        		if(data.brokerConfigId.startsWith("b")) {
	        			try {
	        				int numId = Integer.parseInt(data.brokerConfigId.substring(1));
	        				if(numId > maxIdx)
	        					maxIdx = numId;
	        			} catch(NumberFormatException e) {
	        				//do nothing
	        			}
	        		}
		        	Dictionary<String, Object> dict = config.getProperties();
		        	if(dict == null) {
		        		config = null;
		        		return;
		        	}
		        	data.url = DriverHandlerJMBus.getString("url", dict);
		        	data.username = DriverHandlerJMBus.getString("username", dict);
		        	data.password = DriverHandlerJMBus.getString(".password", dict);
		        	data.clientId = DriverHandlerJMBus.getString("clientId", dict);
	        		brokerData.put(data.brokerConfigId, data);
	        	}
	        	
	 		} catch (InvalidSyntaxException e) {
	 			e.printStackTrace();
	 			throw new IllegalStateException(e);
	 		} catch (IOException e) {
	 			e.printStackTrace();
	 			throw new IllegalStateException(e);
	 		}
		}
	}
		
	public DriverHandlerMQTTBroker(ApplicationManager appMan, ConfigurationAdmin configAdmin) {
		this.appMan = appMan;
		this.configAdmin = configAdmin;
	}

	private final ApplicationManager appMan;
	private final ConfigurationAdmin configAdmin;

	@Override
	public String id() {
		return this.getClass().getName();
	}

	@Override
	public String label(OgemaLocale locale) {
		return id();
	}

	@Override
	public List<DriverDeviceConfig> getDeviceConfigs() {
		DriverConfigMQTT config = new DriverConfigMQTT(configAdmin);

		List<DriverDeviceConfig> result = new ArrayList<>();
		for(DriverConfigMQTTBroker devConfig: config.brokerData.values()) {
			result.add(devConfig);
		}
		return result;
	}

	@Override
	public List<DeviceHandlerProvider<?>> getDeviceHandlerProviders(boolean registerByFramework) {
		// TODO: Add device handler providers for energy cam,...
		return Collections.emptyList();
		//return Arrays.asList(new DeviceHandlerProvider<?>[] {deviceHandlerSensDev});
	}

	@Override
	public DeviceTableRaw<DriverHandlerProvider, Resource> getDriverInitTable(WidgetPage<?> page, Alert alert) {
		return null;
		/*return new DeviceTableRaw<DriverHandlerProvider, Resource>(page, appMan, alert, this) {

			@Override
			public void addWidgets(DriverHandlerProvider object,
					ObjectResourceGUIHelper<DriverHandlerProvider, Resource> vh, String id, OgemaHttpRequest req,
					Row row, ApplicationManager appMan) {}
			
			@Override
			protected String id() {
				return DriverHandlerMQTTBroker.this.id();
			}

			@Override
			protected String getTableTitle() {
				return "MQTT Doc";
			}

			@Override
			protected DocumentationLinkProvider getDocLinkProvider() {
				return DriverHandlerMQTTBroker.this;
			}
			
			@Override
			public Collection<DriverHandlerProvider> getObjectsInTable(OgemaHttpRequest req) {
				return Collections.emptyList();
			}
		};*/
	}

	private static void initDeviceData(DriverConfigMQTT config, ConfigurationAdmin configAdmin) {
		if(config.brokerData == null || config.brokerData.isEmpty()) {
			DriverConfigMQTTBroker bd = newDeviceData();
			config.brokerData.put(bd.brokerConfigId, bd);
			bd.writeConfigDeviceData(configAdmin);
		}		
	}
	private static DriverConfigMQTTBroker newDeviceData() {
		DriverConfigMQTTBroker bd = new DriverConfigMQTTBroker();
		maxIdx++;
		bd.brokerConfigId = "b"+maxIdx;
		return bd;
	}
	
	@Override
	public DeviceTableRaw<DriverDeviceConfig, InstallAppDevice> getDriverPerDeviceConfigurationTable(WidgetPage<?> page,
			Alert alert, InstalledAppsSelector selector, boolean addUnfoundDevices) {
		return new DeviceTableRaw<DriverDeviceConfig, InstallAppDevice>(page, appMan, alert, null) {

			@Override
			public void addWidgets(DriverDeviceConfig objectIn,
					ObjectResourceGUIHelper<DriverDeviceConfig, InstallAppDevice> vh, String id, OgemaHttpRequest req,
					Row row, ApplicationManager appMan) {
				if(req == null) {
					vh.registerHeaderEntry("URL");
					vh.registerHeaderEntry("User name");
					vh.registerHeaderEntry("Password");
					vh.registerHeaderEntry("Client ID");

					vh.registerHeaderEntry("Delete");
					vh.registerHeaderEntry("Copy");
					
					//vh.registerHeaderEntry("Name");
					//vh.registerHeaderEntry("Room");
					//vh.registerHeaderEntry("Location");
					//vh.registerHeaderEntry("Status");
					//vh.registerHeaderEntry("Comment");
					return;
				}
				DriverConfigMQTTBroker object = (DriverConfigMQTTBroker) objectIn;
				
				TextField urlText = new TextField(mainTable, "urlText"+id, req) {
					private static final long serialVersionUID = 1L;
					@Override
					public void onGET(OgemaHttpRequest req) {
						if(object.brokerConfigId == null)
							return;
						setValue(""+object.url, req);
					}
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						String val = getValue(req);
						if(val == null)
							alert.showAlert("Value could not be processed for device ID!", false, req);
						else {
							object.url = val;
							object.writeConfigDeviceData(configAdmin);
						}
					}
				};
				row.addCell(WidgetHelper.getValidWidgetId("URL"), urlText);

				TextField usernameText = new TextField(mainTable, "usernameText"+id, req) {
					private static final long serialVersionUID = 1L;
					@Override
					public void onGET(OgemaHttpRequest req) {
						if(object.brokerConfigId == null)
							return;
						setValue(""+object.username, req);
					}
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						String val = getValue(req);
						if(val == null)
							alert.showAlert("Value could not be processed for device ID!", false, req);
						else {
							object.username = val;
							object.writeConfigDeviceData(configAdmin);
						}
					}
				};
				row.addCell(WidgetHelper.getValidWidgetId("User name"), usernameText);

				TextField passwordText = new TextField(mainTable, "passwordText"+id, req) {
					private static final long serialVersionUID = 1L;
					@Override
					public void onGET(OgemaHttpRequest req) {
						if(object.brokerConfigId == null)
							return;
						setValue(""+object.password, req);
					}
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						String val = getValue(req);
						if(val == null)
							alert.showAlert("Value could not be processed for device ID!", false, req);
						else {
							object.password = val;
							object.writeConfigDeviceData(configAdmin);
						}
					}
				};
				row.addCell(WidgetHelper.getValidWidgetId("Password"), passwordText);

				TextField clientidText = new TextField(mainTable, "clientidText"+id, req) {
					private static final long serialVersionUID = 1L;
					@Override
					public void onGET(OgemaHttpRequest req) {
						if(object.brokerConfigId == null)
							return;
						setValue(""+object.clientId, req);
					}
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						String val = getValue(req);
						if(val == null)
							alert.showAlert("Value could not be processed for device ID!", false, req);
						else {
							object.clientId = val;
							object.writeConfigDeviceData(configAdmin);
						}
					}
				};
				row.addCell(WidgetHelper.getValidWidgetId("Client ID"), clientidText);

				Button resultCopy = new Button(mainTable, "copy"+id, "Copy", req) {
					private static final long serialVersionUID = 1L;
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						DriverConfigMQTTBroker bd = newDeviceData();
						bd.url = object.url;
						bd.username = object.username;
						bd.password = object.password;
						bd.clientId = object.clientId;
						bd.writeConfigDeviceData(configAdmin);
					}
				};

				Button resultDel = new Button(mainTable, "delete"+id, "Delete", req) {
					private static final long serialVersionUID = 1L;
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						disable(req);
						resultCopy.disable(req);
						object.deleteConfigDeviceData(configAdmin);
						if(object.installAppDevice != null)
							alert.showAlert("Note that device information int the OGEMA database is not deleted by this Action. Check the Hardware Installation page for this!", false, req);
						object.brokerConfigId = null;
					}
				};
				row.addCell(WidgetHelper.getValidWidgetId("Delete"), resultDel);
				row.addCell(WidgetHelper.getValidWidgetId("Copy"), resultCopy);
				
				resultDel.registerDependentWidget(resultDel);
				resultDel.registerDependentWidget(resultCopy);
				resultDel.registerDependentWidget(urlText);
				resultDel.registerDependentWidget(usernameText);
				resultDel.registerDependentWidget(passwordText);
				resultDel.registerDependentWidget(clientidText);
				
				/*if(object.installAppDevice == null)
					return;
				PhysicalElement device = (PhysicalElement) addNameWidgetRaw((InstallAppDevice) object.installAppDevice, vh, id, req, row, appMan);
				Room deviceRoom = device.location().room();
				addRoomWidget((InstallAppDevice) object.installAppDevice, vh, id, req, row, appMan, deviceRoom);
				addSubLocation((InstallAppDevice) object.installAppDevice, vh, id, req, row, appMan, deviceRoom);
				addInstallationStatus((InstallAppDevice) object.installAppDevice, vh, id, req, row, appMan, deviceRoom);
				addComment((InstallAppDevice) object.installAppDevice, vh, id, req, row, appMan, deviceRoom);*/
			}
			
			@Override
			protected String id() {
				return DriverHandlerMQTTBroker.this.id()+"_devices";
			}

			@Override
			protected String getTableTitle() {
				return "MQTT Broker Configuration";
			}
			
			@Override
			public InstallAppDevice getResource(DriverDeviceConfig object, OgemaHttpRequest req) {
				return ((DriverConfigMQTTBroker)object).installAppDevice;
			}

			//TODO: Brokers do not have device or InstallApp resources !!
			@Override
			public Collection<DriverDeviceConfig> getObjectsInTable(OgemaHttpRequest req) {
				List<DriverDeviceConfig> all = getDeviceConfigs();
				Collection<DriverDeviceConfig> result = new ArrayList<>();
				for(DriverDeviceConfig devIn: all) {
					DriverConfigMQTTBroker dev = (DriverConfigMQTTBroker) devIn;
					boolean found = false;
					for(InstallAppDevice inst: selector.getDevicesSelected()) {
						if(inst.device().equalsLocation(dev.deviceRes)) {
							dev.installAppDevice = inst;
							result.add(dev);
							found = true;
							break;
						}
					}
					if(!found && addUnfoundDevices) {
						result.add(dev);
					}
				}
				return result ;
			}
		};
	}

	@Override
	public String getDriverDocumentationPageURL(boolean publicVersion) {
		if(publicVersion)
			return "https://en.wikipedia.org/wiki/MQTT";
		else
			return "https://gitlab.com/smartrplace/smartrplace-main/-/wikis/WiFi-Socket-MQTT-Protocol-(Tasmota-firmware)";
	}

}
