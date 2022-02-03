package org.smartrplace.driverhandler.devices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DocumentationLinkProvider;
import org.ogema.devicefinder.api.DriverHandlerProvider;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
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
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.html.form.textfield.ValueInputField;
import de.iwes.widgets.template.DefaultDisplayTemplate;

public class DriverHandlerJMBus implements DriverHandlerProvider {
	
	public static class DriverConfigJMBus {
		public String port = "";
		// use alternatively to #port
		public String hardwareIdentifier = "";
		// AMBER, IMST, RADIO_CRAFTS
		public String manufacturer = "IMST";
		public String mode = "T"; // modes: "S", "T", "C"
		public Long[] keyDeviceIds;
		public String[] keysHexa;

		public DriverConfigJMBus(ConfigurationAdmin configAdmin) {
	        try {
	        	//TODO: Check if this can be automated not only for reading on startup in activate method, but reading the current
	        	//configuration any time
	        	Configuration config = configAdmin.getConfiguration("org.smartrplace.drivers.JmbusTest");
	        	if(config == null)
	        		return;
	        	Dictionary<String, Object> dict = config.getProperties();
	        	if(dict == null) {
	        		config = null;
	        		return;
	        	}
	        	port = getString("port", dict);
	        	hardwareIdentifier = getString("hardwareIdentifier", dict);
	        	manufacturer = getString("manufacturer", dict);
	        	mode = getString("mode", dict);
	        	keyDeviceIds = getLongArray("keyDeviceIds", dict);
	        	keysHexa = getStringArray("keysHexa", dict);
	 		} catch (IOException e) {
	 			e.printStackTrace();
	 			throw new IllegalStateException(e);
	 		}
		}
		
		//TODO: The concept of holding this object and writing back later on is not fully thread-safe
		//If the config page is opened, then configuration is edited in the Felix Admin, then written on this config page
		//the result may be non-consistent or changes in Felix Admin will be overwritten.
		public boolean writeConfigMain(ConfigurationAdmin configAdmin) {
			try {
				Configuration configPrev = configAdmin.getConfiguration("org.smartrplace.drivers.JmbusTest");
	        	Dictionary<String, Object> dict = configPrev.getProperties();
	        	if(dict == null) {
	        		dict = new Hashtable<String, Object>();
	        	}
	        	putDict(dict, "port", port);
	        	putDict(dict, "hardwareIdentifier", hardwareIdentifier);
	        	putDict(dict, "manufacturer", manufacturer);
	        	putDict(dict, "mode", mode);
				configPrev.update(dict);
				return true;
			} catch (IOException e) {
				e.printStackTrace();
	 			throw new IllegalStateException(e);
			}
		}
		public boolean writeConfigDeviceData(ConfigurationAdmin configAdmin) {
			try {
				Configuration configPrev = configAdmin.getConfiguration("org.smartrplace.drivers.JmbusTest");
	        	Dictionary<String, Object> dict = configPrev.getProperties();
	        	putDict(dict, "keyDeviceIds", keyDeviceIds);
	        	putDict(dict, "keysHexa", keysHexa);
				configPrev.update(dict);
				return true;
			} catch (IOException e) {
				e.printStackTrace();
	 			throw new IllegalStateException(e);
			}
		}
	}
	
	public static boolean putDict(Dictionary<String, Object> dict, String key, Object value) {
		if(value == null)
			return false;
		dict.put(key, value);
		return true;
	}
	
	public static String getString(String key, Dictionary<String, Object> dict) {
		Object obj = dict.get(key);
		if(!(obj instanceof String))
			return null;
		return (String) obj;
	}
	public static String[] getStringArray(String key, Dictionary<String, Object> dict) {
		Object obj = dict.get(key);
		if(!(obj instanceof String[]))
			return null;
		return (String[]) obj;
	}
	public static Long[] getLongArray(String key, Dictionary<String, Object> dict) {
		Object obj = dict.get(key);
		if(!(obj instanceof Long[]))
			return null;
		return (Long[]) obj;
	}
	
	/** See */
	public static class DriverDeviceConfigJMBus implements DriverDeviceConfig {
		public SensorDevice deviceRes;
		public InstallAppDevice installAppDevice = null;
		
		//set to null if deleted
		private DriverConfigJMBus mainConfig;
		
		//public long keyDeviceIds;
		//public String keysHexa;
		/** Index within the device arrays of the mainConfig*/
		public int deviceIdx;
		
		@Override
		public Resource getInstallResource() {
			return deviceRes;
		}
		
	}
	
	public DriverHandlerJMBus(ApplicationManagerPlus appMan, ConfigurationAdmin configAdmin) {
		this.appMan = appMan;
		this.configAdmin = configAdmin;
		this.deviceHandlerSensDev = new DeviceHandlerWMBus_SensorDevice(appMan);
	}

	private final ApplicationManagerPlus appMan;
	private final ConfigurationAdmin configAdmin;
	private final DeviceHandlerWMBus_SensorDevice deviceHandlerSensDev;

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
		DriverConfigJMBus config = new DriverConfigJMBus(configAdmin);
		if(config.keyDeviceIds == null)
			return Collections.emptyList();

		List<DriverDeviceConfig> result = new ArrayList<>();
		int idx = 0;
		for(long devID: config.keyDeviceIds) {
			DriverDeviceConfigJMBus devConfig = new DriverDeviceConfigJMBus();
			devConfig.mainConfig = config;
			devConfig.deviceIdx = idx;
			//devConfig.keyDeviceIds = devID;
			//if(config.keysHexa != null && config.keysHexa.length > idx)
			//	devConfig.keysHexa = config.keysHexa[idx];
			result.add(devConfig);
			idx++;
			
			ResourceList<SensorDevice> jMBUS_BASE = appMan.getResourceAccess().getResource("JMBUS_BASE");
			if(jMBUS_BASE == null)
				continue;
			SensorDevice res = jMBUS_BASE.getSubResource("_"+devID, SensorDevice.class);
			if(res.exists())
				devConfig.deviceRes = res;
		}
		return result;
	}

	@Override
	public List<DeviceHandlerProvider<?>> getDeviceHandlerProviders(boolean registerByFramework) {
		return Collections.emptyList();
	}

	/*@Override
	public List<DeviceHandlerProvider<?>> getDeviceHandlerProviders(boolean registerByFramework) {
		// TODO: Add device handler providers for energy cam,...
		return Arrays.asList(new DeviceHandlerProvider<?>[] {deviceHandlerSensDev});
	}*/

	@Override
	public DeviceTableRaw<DriverHandlerProvider, Resource> getDriverInitTable(WidgetPage<?> page, Alert alert) {
		return new DeviceTableRaw<DriverHandlerProvider, Resource>(page, appMan, alert, this) {
			DriverConfigJMBus config = new DriverConfigJMBus(configAdmin);
			
			@Override
			public void addWidgets(DriverHandlerProvider object,
					ObjectResourceGUIHelper<DriverHandlerProvider, Resource> vh, String id, OgemaHttpRequest req,
					Row row, ApplicationManager appMan) {

				vh.stringLabel("Name", id, "wmbus", row);
				if(req == null) {
					vh.registerHeaderEntry("USB Port (if no HW-ID)");
					vh.registerHeaderEntry("Hardware Identifier of USB stick");
					vh.registerHeaderEntry("Manufacturer ID");
					vh.registerHeaderEntry("Mode");
					return;
				}
				TextField portText = new TextField(mainTable, "portText"+id, req) {
					private static final long serialVersionUID = 1L;
					@Override
					public void onGET(OgemaHttpRequest req) {
						setValue(config.port, req);
					}
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						String val = getValue(req);
						if(val == null)
							alert.showAlert("Value could not be processed for device ID!", false, req);
						else {
							config.port = val;
							config.writeConfigMain(configAdmin);
							initDeviceData(config, configAdmin);
						}
					}
				};
				row.addCell(WidgetHelper.getValidWidgetId("USB Port (if no HW-ID)"), portText);

				//vh.stringEdit("USB Port (if no HW-ID)", id, config.port, row);
				//vh.stringEdit("Hardware Identifier of USB stick", id, config.hardwareIdentifier, row);
				
				TextField hwIDText = new TextField(mainTable, "hwIDText"+id, req) {
					private static final long serialVersionUID = 1L;
					@Override
					public void onGET(OgemaHttpRequest req) {
						setValue(config.hardwareIdentifier, req);
					}
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						String val = getValue(req);
						if(val == null)
							alert.showAlert("Value could not be processed for device ID!", false, req);
						else {
							config.hardwareIdentifier = val;
							config.writeConfigMain(configAdmin);
							initDeviceData(config, configAdmin);
						}
					}
				};
				row.addCell(WidgetHelper.getValidWidgetId("Hardware Identifier of USB stick"), hwIDText);

				//vh.stringLabel("Manufacturer ID", id, config.manufacturer, row);
				TemplateDropdown<String> manufacturerDrop = new TemplateDropdown<String>(mainTable, "manufacturerDrop"+id, req) {
					private static final long serialVersionUID = 1L;

					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						config.manufacturer = getSelectedItem(req);
						config.writeConfigMain(configAdmin);
					}
				};
				manufacturerDrop.setTemplate(new DefaultDisplayTemplate<String>() {
					@Override
					public String getLabel(String object, OgemaLocale locale) {
						switch(object) {
						case "AMBER": return "Amber";
						case "IMST": return "IMST GmbH";
						case "RADIO_CRAFTS": return "Radio Crafts";
						default: return "Unknown:"+object;
						}
					}
				});
				manufacturerDrop.setDefaultItems(Arrays.asList(new String[] {"AMBER", "IMST", "RADIO_CRAFTS"}));
				manufacturerDrop.selectDefaultItem(config.manufacturer);
				row.addCell(WidgetHelper.getValidWidgetId("Manufacturer ID"), manufacturerDrop);
	
				TemplateDropdown<String> modeDrop = new TemplateDropdown<String>(mainTable, "modeDrop"+id, req) {
					private static final long serialVersionUID = 1L;

					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						config.mode = getSelectedItem(req);
						config.writeConfigMain(configAdmin);
					}
				};
				modeDrop.setTemplate(new DefaultDisplayTemplate<String>());
				modeDrop.setDefaultItems(Arrays.asList(new String[] {"S", "T", "C"}));
				modeDrop.selectDefaultItem(config.mode);
				row.addCell("Mode", modeDrop);
			}
			
			@Override
			protected String id() {
				return DriverHandlerJMBus.this.id();
			}

			@Override
			public String getTableTitleRaw() {
				return "wMBus Configuration";
			}

			@Override
			protected DocumentationLinkProvider getDocLinkProvider() {
				return DriverHandlerJMBus.this;
			}
			
			@Override
			public Collection<DriverHandlerProvider> getObjectsInTable(OgemaHttpRequest req) {
				//if(config.keyDeviceIds == null)
				//	return Collections.emptyList();
				return Arrays.asList(new DriverHandlerProvider[] {DriverHandlerJMBus.this});
			}
		};
	}

	private static void initDeviceData(DriverConfigJMBus config, ConfigurationAdmin configAdmin) {
		if(config.keyDeviceIds == null || config.keyDeviceIds.length == 0) {
			config.keyDeviceIds = new Long[] {12345678l};
			config.keysHexa = new String[] {""};
			config.writeConfigDeviceData(configAdmin);
		}		
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
					vh.registerHeaderEntry("DeviceID");
					vh.registerHeaderEntry("Device Encryption Key (Hex)");
					vh.registerHeaderEntry("Delete");
					vh.registerHeaderEntry("Copy");
					vh.registerHeaderEntry("Name");
					vh.registerHeaderEntry("Room");
					vh.registerHeaderEntry("Location");
					vh.registerHeaderEntry("Status");
					vh.registerHeaderEntry("Comment");
					return;
				}
				DriverDeviceConfigJMBus object = (DriverDeviceConfigJMBus) objectIn;
				ValueInputField<Long> devIdText = new ValueInputField<Long>(mainTable, "devIdText"+id, Long.class, req) {
					private static final long serialVersionUID = 1L;
					@Override
					public void onGET(OgemaHttpRequest req) {
						if(object.mainConfig == null)
							return;
						setNumericalValue(object.mainConfig.keyDeviceIds[object.deviceIdx], req);
					}
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						Long val = getNumericalValue(req);
						if(val == null)
							alert.showAlert("Value "+getValue(req)+" could not be processed for device ID!", false, req);
						else {
							object.mainConfig.keyDeviceIds[object.deviceIdx] = val;
							object.mainConfig.writeConfigDeviceData(configAdmin);
						}
					}
				};
				row.addCell("DeviceID", devIdText);
				
				TextField keyHexaText = new TextField(mainTable, "keyHexaText"+id, req) {
					private static final long serialVersionUID = 1L;
					@Override
					public void onGET(OgemaHttpRequest req) {
						if(object.mainConfig == null)
							return;
						setValue(""+object.mainConfig.keysHexa[object.deviceIdx], req);
					}
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						String val = getValue(req);
						if(val == null)
							alert.showAlert("Value could not be processed for device ID!", false, req);
						else {
							object.mainConfig.keysHexa[object.deviceIdx] = val;
							object.mainConfig.writeConfigDeviceData(configAdmin);
						}
					}
				};
				row.addCell(WidgetHelper.getValidWidgetId("Device Encryption Key (Hex)"), keyHexaText);

				Button resultCopy = new Button(mainTable, "copy"+id, "Copy", req) {
					private static final long serialVersionUID = 1L;
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						long devId = object.mainConfig.keyDeviceIds[object.deviceIdx];
						object.mainConfig.keyDeviceIds = ArrayUtils.add(object.mainConfig.keyDeviceIds, devId);
						String kexHex = object.mainConfig.keysHexa[object.deviceIdx];
						object.mainConfig.keysHexa = ArrayUtils.add(object.mainConfig.keysHexa, kexHex);
						object.mainConfig.writeConfigDeviceData(configAdmin);
					}
				};

				Button resultDel = new Button(mainTable, "delete"+id, "Delete", req) {
					private static final long serialVersionUID = 1L;
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						object.mainConfig.keyDeviceIds = ArrayUtils.remove(object.mainConfig.keyDeviceIds, object.deviceIdx);
						object.mainConfig.keysHexa = ArrayUtils.remove(object.mainConfig.keysHexa, object.deviceIdx);
						disable(req);
						resultCopy.disable(req);
						devIdText.disable(req);
						keyHexaText.disable(req);
						object.mainConfig.writeConfigDeviceData(configAdmin);
						if(object.deviceRes != null)
							alert.showAlert("Note that device information int the OGEMA database is not deleted by this Action. Check the Hardware Installation page for this!", false, req);
						object.mainConfig = null;
					}
				};
				row.addCell(WidgetHelper.getValidWidgetId("Delete"), resultDel);
				row.addCell(WidgetHelper.getValidWidgetId("Copy"), resultCopy);
				
				resultDel.registerDependentWidget(resultDel);
				resultDel.registerDependentWidget(resultCopy);
				resultDel.registerDependentWidget(devIdText);
				resultDel.registerDependentWidget(keyHexaText);
				
				if(object.installAppDevice == null)
					return;
				PhysicalElement device = (PhysicalElement) addNameWidgetRaw(object.installAppDevice, vh, id, req, row, appMan);
				Room deviceRoom = device.location().room();
				addRoomWidget(vh, id, req, row, appMan, deviceRoom);
				addSubLocation(object.installAppDevice, vh, id, req, row);
				addInstallationStatus(object.installAppDevice, vh, id, req, row);
				addComment(object.installAppDevice, vh, id, req, row);
			}
			
			@Override
			protected String id() {
				return DriverHandlerJMBus.this.id()+"_devices";
			}

			@Override
			public String getTableTitleRaw() {
				return "wMBus Device Configuration";
			}
			
			@Override
			public InstallAppDevice getResource(DriverDeviceConfig object, OgemaHttpRequest req) {
				return ((DriverDeviceConfigJMBus)object).installAppDevice;
			}

			@Override
			public Collection<DriverDeviceConfig> getObjectsInTable(OgemaHttpRequest req) {
				List<DriverDeviceConfig> all = getDeviceConfigs();
				Collection<DriverDeviceConfig> result = new ArrayList<>();
				for(DriverDeviceConfig devIn: all) {
					DriverDeviceConfigJMBus dev = (DriverDeviceConfigJMBus) devIn;
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
		};
	}

	@Override
	public String getDriverDocumentationPageURL(boolean publicVersion) {
		if(publicVersion)
			return "https://www.openmuc.org/m-bus/";
		else
			return "https://gitlab.com/smartrplace/smartrplace-main/-/wikis/Hardware/wMBus";
	}

}
