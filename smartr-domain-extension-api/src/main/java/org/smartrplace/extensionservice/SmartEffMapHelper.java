package org.smartrplace.extensionservice;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Helper functions for dealing with SmartEff2DMaps.
 * @author jruckel
 *
 */
public class SmartEffMapHelper {
	
	/**
	 * Get an interpolated value for any x and y from a map.
	 * Bilinear interpolation. No extrapolation is performed.
	 * @param x
	 * @param y
	 * @param map
	 * @return
	 */
	public static double getInterpolated(double x, double y, SmartEff2DMap map) {
		Keys keys = getKeys(map);
		
		Nearest nX = getNearest(x, keys.x);
		Nearest nY = getNearest(y, keys.y);
		
		// Four nearest points
		float q11 = getValue(nX.lo, nY.lo, map);
		float q12 = getValue(nX.lo, nY.hi, map);
		float q21 = getValue(nX.hi, nY.lo, map);
		float q22 = getValue(nX.hi, nY.hi, map);
		
		// Interpolate along X axis to yield two points.
		double r1;
		double r2;

		if (nX.exact) {
			r1 = q11;
			r2 = q22;
		}
		else {
			r1 = ((nX.hi - x) / (nX.hi - nX.lo)) * q11  +  ((x - nX.lo) / (nX.hi - nX.lo)) * q21;
			r2 = ((nX.hi - x) / (nX.hi - nX.lo)) * q12  +  ((x - nX.lo) / (nX.hi - nX.lo)) * q22;
		}
		
		// Interpolate along Y axis to yield final value.
		double val;

		if (nY.exact) {
			val = r1;
		}
		else {
			val = ((nY.hi - y) / (nY.hi - nY.lo)) * r1  +  ((y - nY.lo) / (nY.hi - nY.lo)) * r2;
		}
		
		return val;
	}
	

	/** Nearest keys */
	private static class Nearest {
		/** Value of nearest lower key */
		double lo;
		/** Index of nearest lower key */
		int loIdx;
		/** Value of higher lower key */
		double hi;
		/** Index of nearest higher key */
		int hiIdx;
		/** True if the exact key is in the set of keys. Lower and higher key will be equal. */
		boolean exact = false;
	}
	/** 
	 * Get the nearest keys for a given arbitrary value in a known set of keys.
	 * @param givenVal
	 * @param keys
	 * @return
	 */
	private static Nearest getNearest(double givenVal, Float[] keys) {

		Nearest nearest = new Nearest();
		Arrays.sort(keys);
		
		for (int i = 0; i < keys.length; i++) {
			float knownVal = keys[i];
			if (givenVal == knownVal || givenVal < keys[0] || givenVal > keys[keys.length - 1]) {
				nearest.loIdx = i;
				nearest.hiIdx = i;
				nearest.exact = true;
				break;
			}
			else if (givenVal < knownVal) {
				nearest.loIdx = i - 1;
				nearest.hiIdx = i;
				break;
			}
		}

		nearest.lo = keys[nearest.loIdx];
		nearest.hi = keys[nearest.hiIdx];
		return nearest;
	}

	
	/**
	 * Get a value from a map by indexes.
	 * @param xIdx
	 * @param yIdx
	 * @param map
	 * @return
	 */
	public static float getValue(int xIdx, int yIdx, SmartEff2DMap map) {
		
		Keys keys = getKeys(map);
		
		if (xIdx <= -1 || yIdx <= -1) {
			return Float.NaN;
		}
		else if (xIdx >= keys.x.length || yIdx >= keys.y.length) {
			return Float.NaN;
		}
		
		for (SmartEff2DMapPrimaryValue primaryVal : map.characteristics().getAllElements()) {
			if (primaryVal.index().getValue() == xIdx)
				return primaryVal.val().getElementValue(yIdx);
		}
		
		return Float.NaN;
	}
	
	/**
	 * Get a value from a map by x and y values.
	 * Note that both x and y must be in the respective key lists.
	 * To get a value for an arbitrary x and y, use {@link #getInterpolated(double, double, SmartEff2DMap)}.
	 * @param x
	 * @param y
	 * @param map
	 * @return
	 */
	static float getValue(double x, double y, SmartEff2DMap map) {

		Keys keys = getKeys(map);
		int xIdx = Arrays.asList(keys.x).indexOf((float) x);
		int yIdx = Arrays.asList(keys.y).indexOf((float) y);
		return getValue(xIdx, yIdx, map);
	}
	

	/** Primary and secondary keys of a map */
	public static class Keys {
		/** Primary keys */
		Float[] x;
		/** Secondary keys */
		Float[] y;
	}

	/**
	 * Extract x (primary) and y (secondary) keys from the map.
	 * @param map
	 * @return
	 */
	public static Keys getKeys(SmartEff2DMap map) {
		
		Keys keys = new Keys();
		keys.x = ArrayUtils.toObject(map.primaryKeys().getValues());
		keys.y = ArrayUtils.toObject(map.secondaryKeys().getValues());
		return keys;
	}
}
