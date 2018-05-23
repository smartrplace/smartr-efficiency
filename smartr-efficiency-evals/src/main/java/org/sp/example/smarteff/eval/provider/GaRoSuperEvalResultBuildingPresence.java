package org.sp.example.smarteff.eval.provider;

import de.iwes.timeseries.eval.garo.api.base.GaRoSuperEvalResult;

@Deprecated //not used
public class GaRoSuperEvalResultBuildingPresence extends GaRoSuperEvalResult<BuildingPresenceMultiResult> {
	//constructor for de-serialization
	public GaRoSuperEvalResultBuildingPresence() {
		super(null, 0, null);
	}

}
