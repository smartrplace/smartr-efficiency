package org.smartrplace.extensionservice;

/**
 * Helper functions for dealing with SmartEff2DMaps
 * @author jruckel
 *
 */
public class SmartEffMapHelper {

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
		
		return (float) (nearestLower + (nearestHigher - nearestLower) * factor);
		
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
