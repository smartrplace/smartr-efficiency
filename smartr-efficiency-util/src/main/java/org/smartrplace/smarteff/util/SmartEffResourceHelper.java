package org.smartrplace.smarteff.util;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.units.TemperatureResource;

/**
 * A collection of helper functions for dealing with resources within
 * smartr-efficiency.
 * @author jruckel
 *
 */
public class SmartEffResourceHelper {
	
	/**
	 * Return the local resource unless it exists, is active and > 0 (0°C for Temperatures).  If these don't apply,
	 * return the default resource.
	 * @param local local resource, e.g. the height defined for an individual room.
	 * @param def default resource, e.g. the default room height defined for a building.
	 * @return
	 */
	public static <T extends SingleValueResource> T getOrDefault(T local, T def) {
		if (!local.exists() || !local.isActive()) return def;
		if (local instanceof TemperatureResource && ((TemperatureResource) local).getCelsius() > 0)
			return local;
		if (local instanceof FloatResource && ((FloatResource) local).getValue() > 0)
			return local;
		if (local instanceof IntegerResource && ((IntegerResource) local).getValue() > 0)
			return local;
		return def;
	}
}
