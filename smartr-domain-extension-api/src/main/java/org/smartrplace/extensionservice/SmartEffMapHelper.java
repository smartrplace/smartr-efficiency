package org.smartrplace.extensionservice;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Helper functions for dealing with SmartEff2DMaps
 * @author jruckel
 *
 */
public class SmartEffMapHelper {
	
	public static class Point {
		double x;
		double y;
		double val;

		public Point(double x, double y) {
			this.x = x;
			this.y = y;
		}
		public Point(double x, double y, double val) {
			this.x = x;
			this.y = y;
			this.val = val;
		}
		
		public double setVal(SmartEff2DMap map) {
			Float[] knownX = ArrayUtils.toObject(map.primaryKeys().getValues());
			Float[] knownY = ArrayUtils.toObject(map.secondaryKeys().getValues());
			
			int xIdx = Arrays.asList(knownX).indexOf((float) this.x);
			int yIdx = Arrays.asList(knownY).indexOf((float) this.y);

			this.val = map.characteristics().getAllElements()
			.get(map.characteristics().size() - 1 - xIdx)
			.getValues()[yIdx];
			
			return this.val;
		}
	}
	
	public static float bilinearInterp(SmartEff2DMap map, float x, float y) {
		float[] knownX = map.primaryKeys().getValues();
		float[] knownY = map.secondaryKeys().getValues();
		
		Point p = new Point(x, y);
		
		/** Find nearest known values for x */
		Nearest nX = getNearest(x, knownX);
		Nearest nY = getNearest(y, knownY);
		Point q11 = new Point(nX.lower, nY.lower);
		q11.setVal(map);
		Point q12 = new Point(nX.lower, nY.higher);
		q12.setVal(map);
		Point q21 = new Point(nX.higher, nY.lower);
		q21.setVal(map);
		Point q22 = new Point(nX.higher, nY.higher);
		q22.setVal(map);
		
		double dx1 = (q22.x - x) / (q22.x - q11.x);
		double dx2 = (x - q11.x) / (q22.x - q11.x);
		
		Point r1 = new Point((q11.x + q21.x) / 2, q11.y, dx1*q11.val + dx2*q21.val);
		Point r2 = new Point((q12.x + q22.x) / 2, q22.y, dx1*q12.val + dx2*q22.val);
		
		double dy1 = (q22.y - y) / (q22.y - q11.y);
		double dy2 = (y - q11.y) / (q22.y - q11.y);
		
		p.val = dy1 * r1.val + dy2 * r2.val;
		
		return (float) p.val;
	}

	public static float getInterpolated(SmartEff2DMap map, float x, float y) {
		float[] knownX = map.primaryKeys().getValues();
		float[] knownY = map.secondaryKeys().getValues();
		
		/** Find nearest known values for x */
		Nearest nearestX = getNearest(x, knownX);
		Nearest nearestY = getNearest(y, knownY);
		
		// Nearest value for lower x and lower y
		float nearestLower =
				// Get list of values for all primary keys
				map.characteristics().getAllElements()
				// Get list of values for nearest lower primary key.  Note that the ResourceList is indexed backwards,
				// i.e. the FloatArrayResource that was added last (which is supposed to be for the highest primary key
				// will be indexed at zero.
				.get(map.characteristics().size() - 1 - nearestX.lowerIdx)
				.getValues()[nearestY.lowerIdx];
		// Nearest value for higher x and higher y
		float nearestHigher =
				map.characteristics().getAllElements()
				.get(map.characteristics().size() - 1 - nearestX.higherIdx)
				.getValues()[nearestY.higherIdx];
		
		double factor = (nearestX.delta * nearestX.delta + nearestY.delta * nearestY.delta) / Math.sqrt(2);
		
		float val = (float) (nearestLower + (nearestHigher - nearestLower) * factor);
		
		float val_bilin = bilinearInterp(map, x, y);
		
		if (!Float.isNaN(val_bilin)) // TODO: Properly handle out of bounds values
			return val_bilin;
		
		return val;
		
	}
	
	public static class Nearest {
		public float delta;
		public float lower;
		public int lowerIdx;
		public float higher;
		public int higherIdx;
	}
	
	public static Nearest getNearest(float val, float[] known) {
		Nearest nearest = new Nearest();
		if (val <= known[0]) {
			nearest.lowerIdx = 0;
			nearest.higherIdx = 0;
			nearest.delta = 0;
		}
		else if(val >= known[known.length - 1]) {
			nearest.lowerIdx = known.length - 1;
			nearest.higherIdx = known.length - 1;
			nearest.delta = 0;
		}
		else {
			for(int i = 0; i < known.length; i++) {
				if(val < known[i]) {
					nearest.lowerIdx = i - 1;
					nearest.higherIdx = i;

					nearest.lower = known[nearest.lowerIdx];
					nearest.higher = known[nearest.higherIdx];

					nearest.delta = (val - nearest.lower) / (nearest.higher - nearest.lower);

					return nearest;
				}
			}
		}
		
		nearest.lower = known[nearest.lowerIdx];
		nearest.higher = known[nearest.higherIdx];
		
		return nearest;
	}
	
}
