package org.sp.calculator.hpadapt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.array.FloatArrayResource;
import org.ogema.core.model.schedule.AbsoluteSchedule;
import org.slf4j.LoggerFactory;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.SmartEff2DMap;
import org.smartrplace.extensionservice.SmartEffMapHelper;
import org.smartrplace.extensionservice.proposal.ProjectProposal;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.MyParam;
import org.smartrplace.smarteff.util.ProjectProviderBase;
import org.sp.example.smartrheating.util.BaseInits;
import org.sp.example.smartrheating.util.BasicCalculations;
import org.sp.example.smartrheating.util.BasicCalculations.YearlyConsumption;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.api.common.BuildingUnit;
import extensionmodel.smarteff.api.common.HeatCostBillingInfo;
import extensionmodel.smarteff.api.common.HeatRadiator;
import extensionmodel.smarteff.api.common.Window;
import extensionmodel.smarteff.defaultproposal.DefaultProviderParams;
import extensionmodel.smarteff.hpadapt.HPAdaptData;
import extensionmodel.smarteff.hpadapt.HPAdaptParams;
import extensionmodel.smarteff.hpadapt.HPAdaptResult;


public class HPAdaptEval extends ProjectProviderBase<HPAdaptData> {
	
	@Override
	public String label(OgemaLocale locale) {
		return "Bivalent heat pump refurbishment";
	}

	@Override
	protected void calculateProposal(HPAdaptData hpData, ProjectProposal resultProposal,
			ExtensionResourceAccessInitData data) {
		
		if (!(resultProposal instanceof HPAdaptResult)) {
			LoggerFactory.getLogger(HPAdaptEval.class).error("Wrong Result type. Can't evaluate.");
			return;
		}

		HPAdaptResult result = (HPAdaptResult) resultProposal;
		calculateHPAdapt(hpData, result, resultProposal, data);

	}
	

	public static final float ABSOLUTE_ZERO = -273.15f;
	/* Range of typical temperatures to evaluate for */
	public static final int LOWEST_TEMP = -20;
	public static final int HIGHEST_TEMP = 40;

	/**
	 * Calculate HPAdapt-specific result. Main evaluation function.
	 * Note: variables named_with_underscores indicate values that have yet to be added to their respective data models.
	 * @param hpData
	 * @param result
	 * @param resultProposal
	 * @param data
	 */
	protected void calculateHPAdapt(HPAdaptData hpData, HPAdaptResult result, ProjectProposal resultProposal,
			ExtensionResourceAccessInitData data) {
		
		/* SETUP */

		MyParam<HPAdaptParams> hpParamHelper =
				CapabilityHelper.getMyParams(HPAdaptParams.class, data.userData(), appManExt);
		HPAdaptParams hpParams = hpParamHelper.get();
		
		MyParam<DefaultProviderParams> defParamHelper =
				CapabilityHelper.getMyParams(DefaultProviderParams.class, data.userData(), appManExt);
		DefaultProviderParams defParams = defParamHelper.get();
		
		BuildingData building = hpData.getParent();
		

		/* CALCULATION */

		/* Calculate heating days, Heating degree days on daily and hourly basis as well as temperature shares. */
		Map<Integer, Integer> temperatureShares = calcHeatingDaysAndTempShares(result, hpData, hpParams);

		/* Calculate window-related data */
		List<Window> allWindows = building.getSubResources(Window.class, true);
		if (allWindows.isEmpty())
			LoggerFactory.getLogger(HPAdaptEval.class).warn("No windows configured!");
		calcWindows(result, allWindows);
		
		/* Find most critical room for each temperature */
		List<BuildingUnit> rooms = building.getSubResources(BuildingUnit.class, true);
		Map<Integer, Float> badRoomCops = calcBadRoomCOP(result, hpData, hpParams, rooms);
		
		/* Perform calculations of TODO for all price levels */
		for (int priceTypeIdx = 0; priceTypeIdx < HPAdaptData.PRICE_TYPE_NAMES_EN.length; priceTypeIdx++) {
			calcPriceLevel(result, resultProposal, hpData, hpParams, temperatureShares, badRoomCops, priceTypeIdx);
		}

		/* Calculate remaining results */
		calcRemaining(result, hpData);
		
		/* Cleanup */
		hpParamHelper.close();
		defParamHelper.close();
	}



	/* * * * * * * * * * * * * * * * * * * * * * *
	 *   AUXILIARY FUNCTIONS FOR CALCULATION     *
	 * * * * * * * * * * * * * * * * * * * * * * */

	private static final long MS_IN_A_DAY = (long) 24 * 60 * 60 * 1000;
	private static final int DAYS_IN_A_YEAR = 365;
	/**
	 * Calculate heating days, Heating degree days on daily and hourly basis as well as temperature shares.
	 * @param result Sets the following result resources:
	 * heatingDegreeDays, numberOfHeatingDays
	 * @param hpData
	 * @param hpParams
	 * @return temperatureShares: Over a range of possible outside temperatures, the number of days with that outside
	 * temperature. Used for further evaluation.
	 */
	private Map<Integer, Integer> calcHeatingDaysAndTempShares(
			HPAdaptResult result, HPAdaptData hpData, HPAdaptParams hpParams) {
		
		Map<Integer, Integer> temperatureShares = new HashMap<>();
		float heatingDegreeDays = 0;
		int numberOfHeatingDays = 0;
		float heatingLimitTemp = hpData.heatingLimitTemp().getCelsius();

		float temperatureOffset = hpData.outsideTempOffset().getValue();
		AbsoluteSchedule temperatureHistory = hpParams.temperatureHistory().recordedDataParent().program();
		
		if (temperatureHistory.size() != DAYS_IN_A_YEAR) {
			LoggerFactory.getLogger(HPAdaptEval.class).warn("Temperature history has {} data points. Expected {}",
					temperatureHistory.size(), DAYS_IN_A_YEAR);
		}
		
		Iterator<SampledValue> iter = temperatureHistory.iterator();
		long prevTimestamp = 0;
		/** Number of data points that are not offset by 24 hours */
		int wrongDataPoints = 0;
		while (iter.hasNext()) {
			SampledValue val = iter.next();
			/* Basic sanity checks of the data we've got */
			long timestamp = val.getTimestamp();
			if (prevTimestamp != 0 && Math.round((timestamp - prevTimestamp) / 1000) * 1000 != MS_IN_A_DAY)
				wrongDataPoints++;
			
			
			float meanOutsideDaytimeTemperature = val.getValue().getFloatValue() + temperatureOffset;
			if (meanOutsideDaytimeTemperature < heatingLimitTemp) {
				numberOfHeatingDays += 1;
				heatingDegreeDays += heatingLimitTemp - meanOutsideDaytimeTemperature;
			}

			int tempRounded = Math.round(meanOutsideDaytimeTemperature);
			int n = 1;
			if(temperatureShares.containsKey(tempRounded))
				n = temperatureShares.get(tempRounded) + 1;
			temperatureShares.put(tempRounded, n);
			prevTimestamp = timestamp;
		}
		ValueResourceHelper.setCreate(result.heatingDegreeDays(), heatingDegreeDays);
		ValueResourceHelper.setCreate(result.numberOfHeatingDays(), numberOfHeatingDays);

		if(wrongDataPoints > 10)
			LoggerFactory.getLogger(HPAdaptEval.class).warn("{} data points in temperature history are not daily!",
					wrongDataPoints);
		
		/* TODO?: Allow user to upload hourly data
		float heatingDegreeDaysHourly = 0;
		int numberOfHeatingDaysHourly = 0;
		
		// For each recorded hour of the year {
			float outside_temperature = 0f;
				if (outside_temperature < heatingLimitTemp) {
					numberOfHeatingDaysHourly += 1;
					heatingDegreeDaysHourly += heatingLimitTemp - outside_temperature;
				}
		// }
		ValueResourceHelper.setCreate(result.heatingDegreeDaysHourly(), heatingDegreeDaysHourly);
		ValueResourceHelper.setCreate(result.numberOfHeatingDaysHourly(), numberOfHeatingDaysHourly);
		*/
		
		/* Perform calculations on temperature Shares */
		for(int i = LOWEST_TEMP; i <= HIGHEST_TEMP; i++) {
			// TODO?
		}
		
		return temperatureShares;
	}


	private final static float DEFAULT_WINDOW_UVALUE = 1.8f;
	private final static float DEFAULT_WINDOW_HEIGHT = 1.05f;
	private final static float DEFAULT_WINDOW_WIDTH = 1.5f;
	/**
	 * Calculate window-related data.
	 * If parts of the window data are missing, it will assume defaults:
	 * U-Value: {@value #DEFAULT_WINDOW_UVALUE} W/(m²*K),
	 * Height: {@value #DEFAULT_WINDOW_HEIGHT} m,
	 * Width: {@value #DEFAULT_WINDOW_WIDTH} m
	 * @param result Sets the following result resources:
	 * windowArea, pLossWindow
	 * @param allWindows List of all windows in the building
	 */
	private void calcWindows(HPAdaptResult result, List<Window> allWindows) {
		float totalWindowArea = 0.0f; // m²
		float totalPLossWindow = 0.0f; // W/K
		
		for(Window window : allWindows) {
			float uValue = window.type().uValue().getValue(); // W/(m²*K)
			if (Float.isNaN(uValue)) uValue = DEFAULT_WINDOW_UVALUE;
			float height = window.type().height().getValue(); // m
			if (Float.isNaN(height)) height = DEFAULT_WINDOW_HEIGHT;
			float width = window.width().getValue(); // m
			if (Float.isNaN(height)) width = DEFAULT_WINDOW_WIDTH;
			float area = height * width; // m²
			totalWindowArea += area;
			totalPLossWindow += uValue * area;
		}
		
		ValueResourceHelper.setCreate(result.windowArea(), totalWindowArea);
		ValueResourceHelper.setCreate(result.pLossWindow(), totalPLossWindow);
	}


	/**
	 * Find most critical room for each temperature / RoomEval2.
	 * Over a range of outside temperatures, determine the room losing the most energy ("Bad Room") at that temperature
	 * and store its COP value.
	 * @param result Sets the following result resources:
	 * roofArea, basementArea, facadeWallArea, numberOfRoomsFacingOutside,
	 * uValueFacade (NOTE: Currently assumes 1.0 as a starting point, later calculations will overwrite this),
	 * @param hpData
	 * @param hpParams
	 * @param rooms List of all rooms in the building.
	 * @return Outside Temperature --> COP of most critical room
	 */
	private Map<Integer, Float> calcBadRoomCOP(HPAdaptResult result,
			HPAdaptData hpData, HPAdaptParams hpParams, List<BuildingUnit> rooms) {
		
		float minTemp = hpData.outsideDesignTemp().getCelsius();
		float heatingLimitTemp = hpData.heatingLimitTemp().getCelsius();
		
		if (rooms.isEmpty())
			LoggerFactory.getLogger(HPAdaptEval.class).error("No rooms found!");

		/* Values needed for RoomEval3 */
		float totalRoofArea = 0f;
		float totalBasementArea = 0f;
		float totalFacadeWallArea = 0f;
		int numberOfRoomsFacingOutside = 0;
		for (BuildingUnit room : rooms) {
			float wall_height = 2.8f; // TODO add to BuildingUnit
			float roof_share = 1.0f; // TODO add to BuildingUnit
			float basement_share = 1.0f; // TODO add to BuildingUnit
			totalRoofArea += room.groundArea().getValue() * roof_share;
			totalBasementArea += room.groundArea().getValue() * basement_share;
			totalFacadeWallArea += room.totalOutsideWallArea().getValue() 
					+ wall_height * hpData.innerWallThickness().getValue() * 0.01
					- room.outsideWindowArea().getValue();
			if (room.window().size() > 0) numberOfRoomsFacingOutside++;
		}
		ValueResourceHelper.setCreate(result.roofArea(), totalRoofArea);
		ValueResourceHelper.setCreate(result.basementArea(), totalBasementArea);
		ValueResourceHelper.setCreate(result.facadeWallArea(), totalFacadeWallArea);
		ValueResourceHelper.setCreate(result.numberOfRoomsFacingOutside(), numberOfRoomsFacingOutside);
		

		/** Temperature -> COP for worst room at that temperature. */
		Map<Integer, Float> badRoomCops = new HashMap<>();

		for (int temp = Math.round(heatingLimitTemp); temp >= Math.round(minTemp); temp--) {
			
			float vLMax = - Float.MAX_VALUE;
			float pMax = - Float.MAX_VALUE;

			BuildingUnit badRoom = null; // Most critical room for temperature
			float deltaT = heatingLimitTemp - temp;
			
			for (BuildingUnit room : rooms) {
				/* Get radiator data */
				List<HeatRadiator> radiators = room.heatRadiator().getAllElements();
				float radPower = 0;
				if(radiators.size() > 0) {
					Iterator<HeatRadiator> iter = radiators.iterator();
					while(iter.hasNext()) {
						HeatRadiator rad = iter.next();
						float nominalPower = rad.radiatorLength().getValue() * rad.type().powerPerLength().getValue();
						float nominalDeltaT = rad.type().nominalTemperatureDifference().getValue();
						radPower += nominalPower / nominalDeltaT;
					}
				}
				/* Window loss */
				List<Window> windows = room.getSubResources(Window.class, true);
				float windowLoss = 0.0f;
				float windowArea = 0.0f;
				for (Window window : windows) {
					float uValue = window.type().uValue().getValue(); // W/(m²*K)
					float height = window.type().height().getValue(); // m
					float width = window.width().getValue(); // m
					float area = height * width; // m²
					
					windowArea += area;
					windowLoss += area * uValue;
				}
				
				/* Wall, ceiling and basement loss */
				float uValueFacade = 1.0f;
				//float uValueFacade = (totalPowerLoss - totalPLossWindow) / weightedExtSurfaceAreaExclWindows;
				ValueResourceHelper.setCreate(result.uValueFacade(), uValueFacade);
				float wallLoss = (room.totalOutsideWallArea().getValue() - windowArea) * uValueFacade;

				float ceil_share = 1.0f; // TODO add to BuildingUnit
				float uValueRoofFacade = hpData.uValueRoofFacade().getValue();
				float ceilLoss = (room.groundArea().getValue() * ceil_share) * uValueRoofFacade * uValueFacade;
				
				float pLoc = (windowLoss + wallLoss + ceilLoss) * deltaT;
				
				float basement_share = 1.0f; // TODO add to BuildingUnit
				float uValueBasementFacade = hpData.uValueBasementFacade().getValue();
				float basementLoss = room.groundArea().getValue() * basement_share
						* uValueBasementFacade * uValueFacade;
				pLoc += basementLoss * (heatingLimitTemp - hpData.basementTempHeatingSeason().getCelsius());
				
				float comfortTemp = hpData.comfortTemp().getCelsius();
				
				float vLLoc = pLoc / radPower + comfortTemp;
				
				if (vLLoc > vLMax && !Float.isInfinite(vLLoc)) {
					vLMax = vLLoc;
					pMax = pLoc;
					badRoom = room;
				}
			}
				
			if (badRoom == null) {
				LoggerFactory.getLogger(HPAdaptEval.class).warn("No BadRoom was determined!");
				continue;
			}

			// TODO store these?
			// temp
			// String badRoomName = badRoom.name().getValue();
			// pMax
			// vLMax

			/* TODO from VBA: This is a oversimplied interpolation, but should work for now */
			float cop = SmartEffMapHelper.getInterpolated(hpParams.copCharacteristics(), (float) temp, vLMax);
			badRoomCops.put(temp, cop);
			
		}
		
	
		return badRoomCops;
	}


	/**
	 * Calculate remaining simple results that depend on other results.
	 * @param result Sets the following result resources:
	 * powerLossAtFreezing, powerLossAtOutsideDesignTemp.
	 * Assumes that all but these resources have already been calculated.
	 * @param hpData
	 */
	private void calcRemaining(HPAdaptResult result, HPAdaptData hpData) {

		float heatingLimitTemp = hpData.heatingLimitTemp().getCelsius();
		float otherPowerLoss = result.otherPowerLoss().getValue();
		float outsideDesignTemp = hpData.outsideDesignTemp().getCelsius();
		float powerLossBasementHeating = result.powerLossBasementHeating().getValue();

		float powerLossAtFreezing = powerLossBasementHeating + otherPowerLoss * heatingLimitTemp;
		ValueResourceHelper.setCreate(result.powerLossAtFreezing(), powerLossAtFreezing);
		
		float powerLossAtOutsideDesignTemp = powerLossBasementHeating
				+ otherPowerLoss * (heatingLimitTemp - outsideDesignTemp);
		ValueResourceHelper.setCreate(result.powerLossAtOutsideDesignTemp(), powerLossAtOutsideDesignTemp);
		
	}

	
	/**
	 * Perform COP evaluation / RoomEval3.
	 * TODO: Parts of these calculations are independent of the price level and don't need to be re-calculated each
	 * time.
	 * @param result Sets the following result resources:
	 * meanHeatingOutsideTemp, activePowerWhileHeating, totalPowerLoss, weightedExtSurfaceAreaExclWindows,
	 * uValueFacade, powerLossBasementHeating, powerLossBasementHeating, powerLossBasementHeating,
	 * maxPowerHPfromBadRoom
	 * @param resultProposal Sets the following resultProposal resources: TODO
	 * @param hpData
	 * @param hpParams
	 * @param temperatureShares calculated by {@link #calcHeatingDaysAndTempShares}
	 * @param badRoomCops calculated by {@link #calcBadRoomCOP}
	 * @param priceType defined in HPAdaptData
	 */
	private void calcPriceLevel(HPAdaptResult result, ProjectProposal resultProposal, HPAdaptData hpData,
			HPAdaptParams hpParams, Map<Integer, Integer> temperatureShares, Map<Integer, Float> badRoomCops,
			int priceType) {
		// TODO Auto-generated method stub
		float heatingLimitTemp = hpData.heatingLimitTemp().getCelsius();

		float heatingDegreeDays = result.heatingDegreeDays().getValue();
		float numberOfHeatingDays = result.numberOfHeatingDays().getValue();
		float facadeWallArea = result.facadeWallArea().getValue();
		float roofArea = result.roofArea().getValue();
		float basementArea = result.basementArea().getValue();
		float pLossWindow = result.pLossWindow().getValue();
		
		/* Get copMin */
		float priceVarHeatPower;
		float priceVarGas;
		/**
		 * The following result resources are only set in calcPriceLevel runs evaluating the user-defined price type
		 * ("dimensioning for price type"):
		 * maxPowerHPfromBadRoom
		 * TODO: All other result resources set by calcPriceLevel are independent of the price type, which means there
		 * is no need to re-calculate them for each price level. In the future, calcPriceLevel should be split up.
		 */
		boolean usingUserDefinedPriceType = false;
		if(priceType == hpData.dimensioningForPriceType().getValue())
			usingUserDefinedPriceType = true;

		if(priceType == HPAdaptData.USE_USER_DEFINED_PRICE_TYPE) {
			priceType = hpData.dimensioningForPriceType().getValue();
		}
		if (priceType == HPAdaptData.PRICE_TYPE_100EE) {
			priceVarHeatPower = hpParams.electrictiyPriceHeat100EEPerkWh().getValue();
			priceVarGas = hpParams.gasPrice100EEPerkWh().getValue();
		}
		else if (priceType == HPAdaptData.PRICE_TYPE_CO2_NEUTRAL) {
			priceVarHeatPower = hpParams.electrictiyPriceHeatCO2neutralPerkWh().getValue();
			priceVarGas = hpParams.gasPriceCO2neutralPerkWh().getValue();
		}
		else { // Assume conventional
			priceVarHeatPower = hpParams.electrictiyPriceHeatPerkWh().getValue();
			priceVarGas = 0.0532f; // TODO use globally defined parameter
		}
		float copMin = priceVarHeatPower / priceVarGas;

		// float heatingLimitTemp = hpData.heatingLimitTemp().getCelsius();
		float sumHp = 0;
		float sumBurn = 0;
		float maxPowerHp = 0;

		/** Calculate powerLossBasementHeating and otherPowerLoss */
		for (int temp = Math.round(heatingLimitTemp); temp >= Math.round(LOWEST_TEMP); temp--) {
			
			/* Calculate required values */

			float cop = badRoomCops.getOrDefault(temp, 0.0f);
			int nShares = temperatureShares.getOrDefault(temp, 0);
			
			float meanHeatingOutsideTemp = heatingLimitTemp - heatingDegreeDays / numberOfHeatingDays;
			result.meanHeatingOutsideTemp().create();
			result.meanHeatingOutsideTemp().setCelsius(meanHeatingOutsideTemp);

			BuildingData building = hpData.getParent();
			ResourceList<HeatCostBillingInfo> heatCostBillingInfo = building.heatCostBillingInfo();
			YearlyConsumption yearlyConsumption =
					BasicCalculations.getYearlyConsumption(heatCostBillingInfo, 3);
			float yearlyHeatingEnergyConsumption = yearlyConsumption.avKWh;
			float b_is_LT_to_CD = 1f; // TODO
			float boilerPowerReductionLTtoCD = hpParams.boilerPowerReductionLTtoCD().getValue() * 0.01f;
			float activePowerWhileHeating =
					yearlyHeatingEnergyConsumption * (1 - b_is_LT_to_CD * boilerPowerReductionLTtoCD)
					/ numberOfHeatingDays / 24 * 1000;
			ValueResourceHelper.setCreate(result.activePowerWhileHeating(), activePowerWhileHeating);
			
			float totalPowerloss = activePowerWhileHeating / (heatingLimitTemp - meanHeatingOutsideTemp);
			ValueResourceHelper.setCreate(result.totalPowerLoss(), totalPowerloss);

			float basementTempHeatingSeason = hpData.basementTempHeatingSeason().getCelsius();

			float weightedExtSurfaceAreaExclWindows =
					facadeWallArea
					+ roofArea * hpData.uValueRoofFacade().getValue()
					+ basementArea * hpData.uValueBasementFacade().getValue()
					* (heatingLimitTemp - basementTempHeatingSeason) / (heatingLimitTemp - meanHeatingOutsideTemp);
			ValueResourceHelper.setCreate(result.weightedExtSurfaceAreaExclWindows(),
					weightedExtSurfaceAreaExclWindows);
			
			float uValueFacade = (totalPowerloss - pLossWindow) / weightedExtSurfaceAreaExclWindows;
			ValueResourceHelper.setCreate(result.uValueFacade(), uValueFacade);
			
			float powerLossBasementHeating = uValueFacade * hpData.uValueBasementFacade().getValue()
					* (heatingLimitTemp - hpData.basementTempHeatingSeason().getCelsius()) * basementArea;
			ValueResourceHelper.setCreate(result.powerLossBasementHeating(), powerLossBasementHeating);
			
			float otherPowerLoss = pLossWindow
					+ facadeWallArea * uValueFacade
					+ roofArea * uValueFacade * hpData.uValueRoofFacade().getValue();
			ValueResourceHelper.setCreate(result.otherPowerLoss(), otherPowerLoss);
			
			/* * */

			float p = powerLossBasementHeating + otherPowerLoss * (heatingLimitTemp - temp);
			// TODO save these?
			float pTotperkW = p / 1000f;
			float pLoc;
			float wElPerkWh;
			float wGas;
			if (cop >= copMin) {
				pLoc = nShares * p * 24f / 1000f / cop;
				sumHp += pLoc;
				wElPerkWh = pLoc;
				wGas = 0;
				if(maxPowerHp < p)
					maxPowerHp = p;
			}
			else {
				pLoc = nShares * p * 24f / 1000f;
				sumBurn += pLoc;
				wElPerkWh = 0;
				wGas = pLoc;
			}
		}
		
		/* variable Heizkosten EUR */
		// TODO save these?
		/** maxPowerHPfromBadRoom / maxPowerHp */
		float maxPowerHPfromBadRoom = maxPowerHp;
		if (usingUserDefinedPriceType)
			ValueResourceHelper.setCreate(result.maxPowerHPfromBadRoom(), maxPowerHPfromBadRoom);
		
		// TODO save these to ProjectProposal100EE?
		// sumHp
		// sumBurn
		System.out.println("Price Type is " + HPAdaptData.PRICE_TYPE_NAMES_EN[priceType] + " (" + priceType + ")");
		System.out.println("sumHp is " + sumHp);
		System.out.println("sumBurn is " + sumBurn);
		System.out.println("maxPowerHPfromBadRoom is " + maxPowerHPfromBadRoom);
		System.out.println("copMin is " + copMin);

		
		/* END RoomEval3 */
	}
	


	/* * * * * * * * * * * * * * * * * * * * * * *
	 *   PROJECT PROVIDER FUNCTIONS              *
	 * * * * * * * * * * * * * * * * * * * * * * */

	@Override
	protected Class<? extends ProjectProposal> getResultType() {
		return HPAdaptResult.class;
	}
	@Override
	protected Class<HPAdaptData> typeClass() {
		return HPAdaptData.class;
	}
	@Override
	public Class<? extends SmartEffResource> getParamType() {
		return HPAdaptParams.class;
	}
	
	@Override
	protected boolean initParams(SmartEffResource paramsIn) {
		BaseInits.initSmartrEffPriceData(appManExt, this.getClass().getName());

		HPAdaptParams params = (HPAdaptParams) paramsIn;
		
		if (!params.exists() || !params.isActive())
			params.create();

		if (!params.copCharacteristics().exists() || !params.copCharacteristics().isActive()) {
			SmartEff2DMap cop = params.copCharacteristics().create();
			FloatArrayResource outsideTemp = cop.primaryKeys().create();
			FloatArrayResource supplyTemp = cop.secondaryKeys().create();
			outsideTemp.setValues(new float[] {-20f, -15f, -10f, -5f, 0f, 5f, 10f, 15f, 20f, 25f, 30f});
			supplyTemp.setValues(new float[] {35, 50, 65});
			cop.characteristics().create();
			final float[][] copField = {
					/* out/supply	 35°C	50°C	65°C	*/
					/* -20°C */ {	1.90f,	1.90f,	0.00f,	},
					/* -15°C */ {	2.50f,	2.20f,	0.00f,	},
					/* -10°C */ {	2.60f,	2.25f,	1.90f,	},
					/* - 5°C */ {	2.80f,	2.35f,	1.90f,	},
					/*   0°C */ {	3.15f,	2.50f,	2.00f,	},
					/*   5°C */ {	3.50f,	2.65f,	2.10f,	},
					/*  10°C */ {	3.90f,	2.95f,	2.40f,	},
					/*  15°C */ {	4.25f,	3.40f,	2.70f,	},
					/*  20°C */ {	4.60f,	3.60f,	2.80f,	},
					/*  25°C */ {	4.75f,	3.80f,	2.90f,	},
					/*  30°C */ {	4.85f,	4.00f,	3.10f,	}
			};
			for (int i = 0; i < copField.length; i++) {
				FloatArrayResource f = cop.characteristics().add();
				f.setValues(copField[i]);
			}
			cop.activate(true);
		}
		
		// Perhaps move to properties?
		if(
				ValueResourceHelper.setIfNew(params.electrictiyPriceCO2neutralPerkWh(), 0.259f) |
				ValueResourceHelper.setIfNew(params.electrictiyPrice100EEPerkWh(), 0.249f) |
				ValueResourceHelper.setIfNew(params.electrictiyPriceHeatBase(), 112) |
				ValueResourceHelper.setIfNew(params.electrictiyPriceHeatPerkWh(), 0.191f) |
				ValueResourceHelper.setIfNew(params.electrictiyPriceHeatCO2neutralPerkWh(), 0.201f) |
				ValueResourceHelper.setIfNew(params.electrictiyPriceHeat100EEPerkWh(), 0.201f) |
				ValueResourceHelper.setIfNew(params.gasPriceCO2neutralPerkWh(), 0.099f) |
				ValueResourceHelper.setIfNew(params.gasPrice100EEPerkWh(), 0.15f) |
				ValueResourceHelper.setIfNew(params.boilerChangeCDtoCD(), 4000) |
				ValueResourceHelper.setIfNew(params.boilerChangeLTtoCD(), 7000) |
				ValueResourceHelper.setIfNew(params.boilerChangeCDtoCDAdditionalPerkW(), 100) |
				ValueResourceHelper.setIfNew(params.boilerChangeLTtoCDAdditionalPerkW(), 200) |
				ValueResourceHelper.setIfNew(params.additionalBivalentHPBase(), 5000) |
				ValueResourceHelper.setIfNew(params.additionalBivalentHPPerkW(), 100) |
				ValueResourceHelper.setIfNew(params.boilerPowerReductionLTtoCD(), 10) |
				ValueResourceHelper.setIfNew(params.wwSupplyTemp(), (8.0f - ABSOLUTE_ZERO))
				
		) {
			return true;
		}
		return false;
	}
	
	public HPAdaptEval(ApplicationManagerSPExt appManExt) {
		super(appManExt);
	}
	
	@Override
	public String userName() {
		return "master";
	}

}
