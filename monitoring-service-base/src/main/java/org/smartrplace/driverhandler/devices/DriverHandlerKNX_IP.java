package org.smartrplace.driverhandler.devices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DocumentationLinkProvider;
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

public class DriverHandlerKNX_IP implements DriverHandlerProvider {
	
	public static final String BASE_CONFIG_ID = "com.smartrplace.knx.baseservice";

	protected static int maxIdx = 0;
	public static class DriverConfigKNX_IP_IF implements DriverDeviceConfig {
		
		public InstallAppDevice installAppDevice = null;
		public PhysicalElement deviceRes;

		public String url = "";
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
        	if(dict == null)
        		dict = new Hashtable<>();
        	DriverHandlerJMBus.putDict(dict, "url", url);
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

	public static class DriverConfigKNX {
		public Map<String, DriverConfigKNX_IP_IF> brokerData = new HashMap<>();

		public DriverConfigKNX(ConfigurationAdmin configAdmin) {
	        try {
				//TODO: Check if this can be automated not only for reading on startup in activate method, but reading the current
	        	//configuration any time
	        	String filter = "(service.pid="+BASE_CONFIG_ID+"*)";
	        	Configuration[] configs = configAdmin.listConfigurations(filter);
	        	if(configs != null) for(Configuration config: configs) {
	        		//TODO
	        		DriverConfigKNX_IP_IF data = new DriverConfigKNX_IP_IF();
	        		String fullPid = config.getPid();
	        		int index = fullPid.indexOf('~');
	        		if((index >= 0) && (fullPid.length() > (index+1))) {
	        			data.brokerConfigId = fullPid.substring(index+1);
	        		} else
	        			data.brokerConfigId = fullPid;
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
		
	public DriverHandlerKNX_IP(ApplicationManagerPlus appMan, ConfigurationAdmin configAdmin) {
		this.appMan = appMan;
		this.configAdmin = configAdmin;
	}

	private final ApplicationManagerPlus appMan;
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
		DriverConfigKNX config = new DriverConfigKNX(configAdmin);

		List<DriverDeviceConfig> result = new ArrayList<>();
		for(DriverConfigKNX_IP_IF devConfig: config.brokerData.values()) {
			result.add(devConfig);
		}
		return result;
	}

	@Override
	public List<DeviceHandlerProvider<?>> getDeviceHandlerProviders(boolean registerByFramework) {
		return Collections.emptyList();
	}

	@Override
	public DeviceTableRaw<DriverHandlerProvider, Resource> getDriverInitTable(WidgetPage<?> page, Alert alert) {
		return null;
	}

	private static DriverConfigKNX_IP_IF newDeviceData() {
		DriverConfigKNX_IP_IF bd = new DriverConfigKNX_IP_IF();
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
					//vh.registerHeaderEntry("Copy");
					
					return;
				}
				DriverConfigKNX_IP_IF object = (DriverConfigKNX_IP_IF) objectIn;
				
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

				Button resultDel = new Button(mainTable, "delete"+id, "Delete", req) {
					private static final long serialVersionUID = 1L;
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						disable(req);
						object.deleteConfigDeviceData(configAdmin);
						if(object.installAppDevice != null)
							alert.showAlert("Note that device information int the OGEMA database is not deleted by this Action. Check the Hardware Installation page for this!", false, req);
						object.brokerConfigId = null;
					}
				};
				row.addCell(WidgetHelper.getValidWidgetId("Delete"), resultDel);
				
				resultDel.registerDependentWidget(resultDel);
				resultDel.registerDependentWidget(urlText);
			}
			
			@Override
			protected String id() {
				return DriverHandlerKNX_IP.this.id()+"_devices";
			}

			@Override
			protected String getTableTitle() {
				return "KNX IP Configuration";
			}
			
			@Override
			public InstallAppDevice getResource(DriverDeviceConfig object, OgemaHttpRequest req) {
				return ((DriverConfigKNX_IP_IF)object).installAppDevice;
			}

			//TODO: Brokers do not have device or InstallApp resources !!
			@Override
			public Collection<DriverDeviceConfig> getObjectsInTable(OgemaHttpRequest req) {
				List<DriverDeviceConfig> all = getDeviceConfigs();
				Collection<DriverDeviceConfig> result = new ArrayList<>();
				for(DriverDeviceConfig devIn: all) {
					DriverConfigKNX_IP_IF dev = (DriverConfigKNX_IP_IF) devIn;
					boolean found = false;
					for(InstallAppDevice inst: selector.getDevicesSelected(null, req)) {
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

			@Override
			protected DocumentationLinkProvider getDocLinkProvider() {
				return DriverHandlerKNX_IP.this;
			}
			
			@Override
			protected Button getAddButton() {
				Button result = new Button(page, WidgetHelper.getValidWidgetId("addButton"+id()), "Add") {
					private static final long serialVersionUID = 1L;
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						DriverConfigKNX_IP_IF bd = newDeviceData();
						bd.url = "enter URL";
						bd.writeConfigDeviceData(configAdmin);
					}
				};
				return result;
			}
		};
	}

	@Override
	public String getDriverDocumentationPageURL(boolean publicVersion) {
		return null;
	}

}
