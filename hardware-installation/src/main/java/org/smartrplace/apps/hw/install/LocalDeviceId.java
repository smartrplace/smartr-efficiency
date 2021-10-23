package org.smartrplace.apps.hw.install;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

public abstract class LocalDeviceId {
    public static Map<String, String> types = new HashMap<>();
    {
        types.put("DoorWindowSensor", "WS");
    }
    public static String getTypeShortId(String type) {
        String id = types.get(type);
        if (id != null) {
            System.out.println("No shorthand found for type " + type);
            return id;
        }
        return type;
    }

    private static final Pattern ID_PATTERN = Pattern.compile("\\w+-(\\d+)");

	public static <T extends Resource> String generateDeviceId(InstallAppDevice dev, HardwareInstallConfig cfg,
			DeviceHandlerProvider<T> tableProvider, DatapointService dpService) {

		final String typeId;
		if (tableProvider != null) {
			typeId = tableProvider.getDeviceTypeShortId(dpService);
		} else {
			// Fall back to default id generation.
			// Improvement:  Always provide a non-null tableProvider so we can
			// use overridden getDeviceTypeShortId for custom ids.
			typeId = "*" + dev.device().getClass().getSimpleName().replaceAll("[^A-Z]", "");
		}

        int maxSerial = 0;
        for(InstallAppDevice d : cfg.knownDevices().getAllElements()) {
            if (d.device().getClass() == dev.device().getClass()) {
                String id = d.deviceId().getValue();
                Matcher m = ID_PATTERN.matcher(id);
                if (!m.find()) continue;
                int serial = Integer.parseInt(m.group(1));
                if (maxSerial < serial)
                    maxSerial = serial;
            }
        }
        return String.format("%s-%04d", typeId, maxSerial + 1);
    }
	
	public static <T extends Resource> void resetDeviceIds(List<InstallAppDevice> devices, DatapointService dpService) {
		Map<String, Integer> maxIdPerDevice = new HashMap<>();
		resetDeviceIds(devices, dpService, false, maxIdPerDevice);
		resetDeviceIds(devices, dpService, true, maxIdPerDevice);
	}
	protected static <T extends Resource> void resetDeviceIds(List<InstallAppDevice> devices, DatapointService dpService,
			boolean processTrash, Map<String, Integer> maxIdPerDevice) {
		for(InstallAppDevice dev: devices) {
			if(dev.isTrash().getValue() != processTrash)
				continue;
			DeviceHandlerProviderDP<Resource> tableProvider = dpService.getDeviceHandlerProvider(dev);
			if (!dev.deviceId().exists()) dev.deviceId().create();
				
			final String typeId;
			if (tableProvider != null) {
				typeId = tableProvider.getDeviceTypeShortId(dpService);
			} else {
				// Fall back to default id generation.
				// Improvement:  Always provide a non-null tableProvider so we can
				// use overridden getDeviceTypeShortId for custom ids.
				typeId = "*" + dev.device().getClass().getSimpleName().replaceAll("[^A-Z]", "");
			}
	
	        Integer maxSerial = maxIdPerDevice.get(typeId);
	        if(maxSerial == null) {
	        	maxSerial = 0;
	        }
	        String result = String.format("%s-%04d", typeId, maxSerial + 1);
	        maxIdPerDevice.put(typeId, (maxSerial+1));
	        dev.deviceId().setValue(result);
			dev.activate(true);
		}
    }

}
