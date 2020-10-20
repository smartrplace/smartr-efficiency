package org.smartrplace.hwinstall.basetable;

import java.util.Map;

import org.ogema.devicefinder.api.DeviceHandlerProvider;

public interface DeviceHandlerAccess {
	public Map<String, DeviceHandlerProvider<?>> getTableProviders();
}
