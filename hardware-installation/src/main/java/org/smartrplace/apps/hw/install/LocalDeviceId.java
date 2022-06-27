package org.smartrplace.apps.hw.install;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.drivers.homematic.xmlrpc.hl.types.HmDevice;
import org.ogema.model.prototypes.PhysicalElement;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.config.PreKnownDeviceData;

import de.iwes.util.logconfig.LogHelper;
import de.iwes.util.logconfig.LogHelper.MustFitLevel;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;

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

	public static <T extends PhysicalElement> String generateDeviceId(InstallAppDevice dev, HardwareInstallConfig cfg,
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

		PreKnownDeviceData pre = getPreDeviceData(dev.device(), cfg, tableProvider.id());
		if(pre != null) {
			String deviceId = getAndPrepareConflictFreeDeviceId(dev.device(), pre, typeId, cfg);
			if(pre.room().isReference(false) && (!dev.device().location().room().exists()))
				dev.device().location().room().setAsReference(pre.room().getLocationResource());
			if(pre.installationLocation().exists() && (!pre.installationLocation().getValue().isEmpty()) &&
					((dev.installationLocation() == null) || dev.installationLocation().getValue().isEmpty()))
				ValueResourceHelper.setCreate(dev.installationLocation(), pre.installationLocation().getValue());
			return String.format("%s-%s", typeId, deviceId);
		}
		
        int maxSerial = 0;
        for(InstallAppDevice d : cfg.knownDevices().getAllElements()) {
            if(d.deviceId().getValue().startsWith(typeId+"-")) {
        	//if (d.device().getClass() == dev.device().getClass()) {
                /*String id = d.deviceId().getValue();
                Matcher m = ID_PATTERN.matcher(id);
                if (!m.find()) continue;
                int serial = Integer.parseInt(m.group(1));*/
                int serial = getDeviceIdNumericalPart(d);
                if(serial < 0)
                	continue;
                if (maxSerial < serial)
                    maxSerial = serial;
            }
        }
        return String.format("%s-%04d", typeId, maxSerial + 1);
    }
	
	public static int getDeviceIdNumericalPart(InstallAppDevice d) {
        String id = d.deviceId().getValue();
        Matcher m = ID_PATTERN.matcher(id);
        if (!m.find()) return -1;
        int serial = Integer.parseInt(m.group(1));
        return serial;
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

	public static PreKnownDeviceData getPreDeviceData(PhysicalElement device, HardwareInstallConfig cfg,
			String devHandId) {
		PreKnownDeviceData forDevHand = getPreDeviceData(device, cfg, devHandId, true);
		if(forDevHand != null)
			return forDevHand;
		return getPreDeviceData(device, cfg, devHandId, false);
	}
	public static PreKnownDeviceData getPreDeviceData(PhysicalElement device, HardwareInstallConfig cfg,
			String devHandId, boolean mustFitDeviceHandler) {
		String hmId = LogHelper.getDeviceId(device);
		HmDevice hmDevice = (HmDevice) ResourceHelper.getFirstParentOfType(device, "HmDevice");
		for(PreKnownDeviceData pre : cfg.preKnownDevices().getAllElements()) {
			if(LogHelper.doesDeviceFitPreKnownData(hmDevice, pre, devHandId,
					mustFitDeviceHandler?MustFitLevel.MUST_FIT:MustFitLevel.ANY_TYPE_ALLOWED,
					hmId))
				return pre;
			/*if(mustFitDeviceHandler && 
					((!pre.deviceHandlerId().isActive()) || (pre.deviceHandlerId().getValue().isEmpty())))
				continue;
			if(pre.deviceHandlerId().isActive()) {
				String val = pre.deviceHandlerId().getValue();
				if((!val.isEmpty()) && (!val.equals(devHandId)))
					continue;
			}
			String endCode = pre.deviceEndCode().getValue();
			String hmIdLoc;
			if(endCode.length() != 4)
				hmIdLoc = LogHelper.getDeviceId(device, endCode.length());
			else
				hmIdLoc = hmId;
			if(hmIdLoc.equals(endCode))
				return pre;*/
		}
		return null;
	}

	public static String getAndPrepareConflictFreeDeviceId(PhysicalElement device, PreKnownDeviceData pre, String typeIdIn,
			HardwareInstallConfig cfg) {
        String preDevId = pre.deviceIdNumber().getValue();
		Set<String> usedSuffixes = new HashSet<>();
        boolean hasConflict = false;
        String typeId = typeIdIn+"-";
		for(InstallAppDevice d : cfg.knownDevices().getAllElements()) {
        	if(!d.deviceId().getValue().startsWith(typeId))
            	continue;
            if(d.deviceId().getValue().length() <= typeId.length())
            	continue;
            String numPart = d.deviceId().getValue().substring(typeId.length());
            if(preDevId.equals(numPart))
            	hasConflict = true;
            else if(numPart.startsWith(preDevId)) {
            	String suffix = numPart.substring(preDevId.length());
            	usedSuffixes.add(suffix);
            }
        }
        if(hasConflict) for(InstallAppDevice d : cfg.knownDevices().getAllElements()) {
        	if(!d.deviceId().getValue().startsWith(typeId))
            	continue;
            if(d.deviceId().getValue().length() <= typeId.length())
            	continue;
            String numPart = d.deviceId().getValue().substring(typeId.length());
            if(preDevId.equals(numPart)) {
            	//conflict
            	if(d.isTrash().getValue()) {
            		String newTrash = String.format("%s-%s", typeIdIn, preDevId+getUniqueSuffix(true, usedSuffixes));
            		d.deviceId().setValue(newTrash);
            	} else {
            		preDevId = preDevId+getUniqueSuffix(false, usedSuffixes);
            		break;
            	}
            }
        }
		return preDevId;
	}
	
	public static String getUniqueSuffix(boolean isTrash, Set<String> usedSuffixes) {
		String result = isTrash?"X":"_A";
		int idx = 0;
		while(usedSuffixes.contains(result)) {
			if(isTrash)
				result += "X";
			else {
				idx++;
				if(idx <= 58)
					result = "_"+String.valueOf((char)(idx + 65));
				else
					result += "Y";
			}
		}
		usedSuffixes.add(result);
		return result;
	}
}
