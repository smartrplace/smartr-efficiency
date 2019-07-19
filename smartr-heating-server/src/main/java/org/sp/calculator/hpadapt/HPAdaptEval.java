package org.sp.calculator.hpadapt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.array.FloatArrayResource;
import org.ogema.core.model.schedule.AbsoluteSchedule;
import org.ogema.core.model.simple.FloatResource;
import org.slf4j.LoggerFactory;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.SmartEff2DMap;
import org.smartrplace.extensionservice.SmartEff2DMapPrimaryValue;
import org.smartrplace.extensionservice.SmartEffMapHelper;
import org.smartrplace.extensionservice.proposal.ProjectProposal100EE;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.MyParam;
import org.smartrplace.smarteff.util.ProjectProviderBase100EE;
import org.smartrplace.smarteff.util.SmartEffResourceHelper;
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
import extensionmodel.smarteff.hpadapt.PriceScenarioData;


public class HPAdaptEval extends ProjectProviderBase100EE<HPAdaptData> {
	
	public static final String WIKI_LINK =
			"https://github.com/smartrplace/smartr-efficiency/blob/master/HPAdapt.md";
	/** Yearly operation cost at chosen price scenario */
	private float yearlyCostBivalent;
	/** Yearly operation cost at chosen price scenario */
	private float yearlyCostCondensing;
	
	@Override
	public String label(OgemaLocale locale) {
		return "Bivalent heat pump refurbishment";
	}

	@Override
	protected void calculateProposal(HPAdaptData hpData, ProjectProposal100EE resultProposal,
			ExtensionResourceAccessInitData data) {
		
		if (!(resultProposal instanceof HPAdaptResult))
			throw new RuntimeException("Wrong Result type. Can't evaluate.");

		HPAdaptResult result = (HPAdaptResult) resultProposal;
		
		// Completely clear result
		result.delete();
		result.create();
		result.activate(true);
		
		// Set up names of results
		result.bivalent().create();
		result.condensing().create();
		ValueResourceHelper.setCreate(result.name(),
				"Bivalent heat pump/boiler operation with fully adaptive supply temperature");
		ValueResourceHelper.setCreate(result.bivalent().name(),
				"Switching to a bivalent System w/o changing radiators");
		ValueResourceHelper.setCreate(result.condensing().name(),
				"Renew boiler to a condensing boiler");
		
		calculateHPAdapt(hpData, result, data);
		
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
	 * @param data
	 */
	protected void calculateHPAdapt(HPAdaptData hpData, HPAdaptResult result,
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
		
		/* Perform calculations for all price levels */
		calcPriceLevelPrereq(result, hpData, hpParams);
		for (int i = 0; i < HPAdaptData.PRICE_TYPE_NAMES_EN.length; i++) {
			calcPriceLevel(result, hpData, hpParams, temperatureShares, badRoomCops, i);
		}

		/* Calculate remaining results */
		calcRemaining(result, hpData, hpParams);
		
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
	 * heatingDegreeDays, numberOfHeatingDays, heatingDegreeDaysHourly, numberOfHeatingDaysHourly,
	 * meanHeatingOutsideTemp.
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
		if (temperatureHistory.size() == 0)
			throw new RuntimeException("No temperature history found!");
		
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

				
		float meanHeatingOutsideTemp = heatingLimitTemp - heatingDegreeDays / numberOfHeatingDays;
		result.meanHeatingOutsideTemp().create();
		result.meanHeatingOutsideTemp().setCelsius(meanHeatingOutsideTemp);
		
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
	 * roofArea, basementArea, facadeWallArea, numberOfRoomsFacingOutside, weightedExtSurfaceAreaExclWindows,
	 * activePowerWhileHeating, totalPowerLoss, uValueFacade.
	 * Requires the following result resources to be set:
	 * meanHeatingOutsideTemp, numberOfHeatingDays, pLossWindow.
	 * @param hpData
	 * @param hpParams
	 * @param rooms List of all rooms in the building.
	 * @return Outside Temperature --> COP of most critical room
	 */
	private Map<Integer, Float> calcBadRoomCOP(HPAdaptResult result,
			HPAdaptData hpData, HPAdaptParams hpParams, List<BuildingUnit> rooms) {
		
		float meanHeatingOutsideTemp = result.meanHeatingOutsideTemp().getCelsius();
		float numberOfHeatingDays = result.numberOfHeatingDays().getValue();
		float totalPLossWindow = result.pLossWindow().getValue();
		
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
			float roomHeight = SmartEffResourceHelper.getOrDefault(room.roomHeight(), hpData.roomHeight()).getValue();
			float ceilingShare = room.ceilingShare().getValue() / 100f;
			float basementShare = room.basementShare().getValue() / 100f;
			totalRoofArea += room.groundArea().getValue() * ceilingShare;
			totalBasementArea += room.groundArea().getValue() * basementShare;
			totalFacadeWallArea += room.totalOutsideWallArea().getValue() 
					+ roomHeight * hpData.innerWallThickness().getValue() * 0.01
					- room.outsideWindowArea().getValue();
			if (room.window().size() > 0) numberOfRoomsFacingOutside++;
		}
		ValueResourceHelper.setCreate(result.roofArea(), totalRoofArea);
		ValueResourceHelper.setCreate(result.basementArea(), totalBasementArea);
		ValueResourceHelper.setCreate(result.facadeWallArea(), totalFacadeWallArea);
		ValueResourceHelper.setCreate(result.numberOfRoomsFacingOutside(), numberOfRoomsFacingOutside);
		
		/* Wall, ceiling and basement loss */
		float basementTempHeatingSeason = hpData.basementTempHeatingSeason().getCelsius();

		float facadeWallArea = totalFacadeWallArea;
		float roofArea = totalRoofArea;
		float basementArea = totalBasementArea;

		float weightedExtSurfaceAreaExclWindows =
				facadeWallArea
				+ roofArea * hpData.uValueRoofFacade().getValue()
				+ basementArea * hpData.uValueBasementFacade().getValue()
				* (heatingLimitTemp - basementTempHeatingSeason) / (heatingLimitTemp - meanHeatingOutsideTemp);
		ValueResourceHelper.setCreate(result.weightedExtSurfaceAreaExclWindows(),
				weightedExtSurfaceAreaExclWindows);
		
		BuildingData building = hpData.getParent();
		float yearlyHeatingEnergyConsumption = getYearlyHeating(building);
		
		float b_is_LT_to_CD = building.condensingBurner().getValue() ? 0f : 1f;
		float boilerPowerReductionLTtoCD = hpParams.boilerPowerReductionLTtoCD().getValue() * 0.01f;
		float activePowerWhileHeating =
				yearlyHeatingEnergyConsumption * (1 - b_is_LT_to_CD * boilerPowerReductionLTtoCD)
				/ numberOfHeatingDays / 24 * 1000;
		ValueResourceHelper.setCreate(result.activePowerWhileHeating(), activePowerWhileHeating);
		
		float totalPowerLoss = activePowerWhileHeating / (heatingLimitTemp - meanHeatingOutsideTemp);
		ValueResourceHelper.setCreate(result.totalPowerLoss(), totalPowerLoss);
		
		float uValueFacade = (totalPowerLoss - totalPLossWindow) / weightedExtSurfaceAreaExclWindows;
		ValueResourceHelper.setCreate(result.uValueFacade(), uValueFacade);

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
				
				float wallLoss = (room.totalOutsideWallArea().getValue() - windowArea) * uValueFacade;

				float ceilShare = room.ceilingShare().getValue() / 100f;
				float uValueRoofFacade = hpData.uValueRoofFacade().getValue();
				float ceilLoss = (room.groundArea().getValue() * ceilShare) * uValueRoofFacade * uValueFacade;
				
				float pLoc = (windowLoss + wallLoss + ceilLoss) * deltaT;
				
				float basementShare = room.basementShare().getValue() / 100f;
				float uValueBasementFacade = hpData.uValueBasementFacade().getValue();
				float basementLoss = room.groundArea().getValue() * basementShare
						* uValueBasementFacade * uValueFacade;
				pLoc += basementLoss * (heatingLimitTemp - hpData.basementTempHeatingSeason().getCelsius());
				

				float comfortTemp = SmartEffResourceHelper
						.getOrDefault(room.comfortTemperature(), hpData.comfortTemp()).getValue();

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

			float cop = (float) SmartEffMapHelper.getInterpolated(temp, vLMax, hpParams.copCharacteristics());
			badRoomCops.put(temp, cop);
			
		}
		
	
		return badRoomCops;
	}


	/**
	 * Calculates priceLevel-independent prerequisites for {@link #calcPriceLevel()}
	 * @param result Sets the following result resources:
	 * powerLossBasementHeating, otherPowerLoss.
	 * Requires the following result resources to be set:
	 * numberOfHeatingDays, facadeWallArea, roofArea, basementArea, pLossWindow, uValueFacade, meanHeatingOutsideTemp.
	 * @param resultProposal Sets the following resultProposal resources: TODO
	 * @param hpData
	 * @param hpParams
	 */
	private void calcPriceLevelPrereq(HPAdaptResult result, HPAdaptData hpData, HPAdaptParams hpParams) {
		
		float heatingLimitTemp = hpData.heatingLimitTemp().getCelsius();

		float numberOfHeatingDays = result.numberOfHeatingDays().getValue();
		float facadeWallArea = result.facadeWallArea().getValue();
		float roofArea = result.roofArea().getValue();
		float basementArea = result.basementArea().getValue();
		float pLossWindow = result.pLossWindow().getValue();
		float uValueFacade = result.uValueFacade().getValue();
		

		float powerLossBasementHeating = uValueFacade * hpData.uValueBasementFacade().getValue()
				* (heatingLimitTemp - hpData.basementTempHeatingSeason().getCelsius()) * basementArea;
		ValueResourceHelper.setCreate(result.powerLossBasementHeating(), powerLossBasementHeating);
		
		float otherPowerLoss = pLossWindow
				+ facadeWallArea * uValueFacade
				+ roofArea * uValueFacade * hpData.uValueRoofFacade().getValue();
		ValueResourceHelper.setCreate(result.otherPowerLoss(), otherPowerLoss);

		
		/* CALCULATE totalEnergyPostRenovation and its dependencies. */
		
		float wwConsumption = hpData.wwConsumption().getValue();
		float wwTemp = hpData.wwTemp().getCelsius();
		float wwLossUnheatedAreas = hpData.wwLossUnheatedAreas().getValue() / 100f;
		float wwLossHeatedAreas = hpData.wwLossHeatedAreas().getValue() / 100f;
		
		float wwSupplyTemp = hpParams.wwSupplyTemp().getCelsius();

		float yearlyHeatingEnergyConsumption = getYearlyHeating(hpData.getParent());
		
		float base_energy = wwConsumption * 4.19f / 3.6f * (wwTemp - wwSupplyTemp);
		float base_energy_winter =
				base_energy * numberOfHeatingDays / 365 / (1 - wwLossUnheatedAreas);
		float base_energy_summer =
				base_energy * (365 - numberOfHeatingDays) / 365 / (1 - wwLossHeatedAreas - wwLossUnheatedAreas);
		
		boolean b_isInclTWW = true;
		float wwEnergyPreRenovation;
		if (!b_isInclTWW) wwEnergyPreRenovation = 0f;
		else {
			wwEnergyPreRenovation = base_energy_summer + base_energy_winter;
		}
		ValueResourceHelper.setCreate(result.wwEnergyPreRenovation(), wwEnergyPreRenovation);	
		
		float faktor_brennwert = 0.9f;
		float wwEnergyPostRenovation = wwEnergyPreRenovation * faktor_brennwert;
		ValueResourceHelper.setCreate(result.wwEnergyPostRenovation(), wwEnergyPostRenovation);	
		
		float heatingEnergyPreRenovation = yearlyHeatingEnergyConsumption - wwEnergyPreRenovation;
		ValueResourceHelper.setCreate(result.heatingEnergyPreRenovation(), heatingEnergyPreRenovation);	

		float faktor_sanierung = 0.85f;
		float heatingEnergyPostRenovation = heatingEnergyPreRenovation * faktor_sanierung * faktor_brennwert;
		ValueResourceHelper.setCreate(result.heatingEnergyPostRenovation(), heatingEnergyPostRenovation);
		
		float totalEnergyPostRenovation = wwEnergyPostRenovation + heatingEnergyPostRenovation;
		ValueResourceHelper.setCreate(result.totalEnergyPostRenovation(), totalEnergyPostRenovation);
		/* * * */
	}
	
	/**
	 * Perform COP evaluation / RoomEval3.
	 * @param result Sets the following result resources:
	 * maxPowerHPfromBadRoom, yearlyOperatingCosts for each scenario
	 * Requires the following result resources to be set:
	 * powerLossBasementHeating, otherPowerLoss, totalEnergyPostRenovation.
	 * @param hpData
	 * @param hpParams
	 * @param temperatureShares calculated by {@link #calcHeatingDaysAndTempShares}
	 * @param badRoomCops calculated by {@link #calcBadRoomCOP}
	 * @param priceScenario defined in HPAdaptData
	 */
	private void calcPriceLevel(HPAdaptResult result, HPAdaptData hpData, HPAdaptParams hpParams,
			Map<Integer, Integer> temperatureShares, Map<Integer, Float> badRoomCops, int priceScenario) {

		float heatingLimitTemp = hpData.heatingLimitTemp().getCelsius();

		float powerLossBasementHeating = result.powerLossBasementHeating().getValue();
		float otherPowerLoss = result.otherPowerLoss().getValue();
		float totalEnergyPostRenovation = result.totalEnergyPostRenovation().getValue();
		
		/* Get copMin */
		float priceVarHeatPower;
		float priceVarGas;
		/**
		 * The following result resources are only set in calcPriceLevel runs evaluating the user-defined price type
		 * ("dimensioning for price type"):
		 * maxPowerHPfromBadRoom
		 */
		boolean usingUserDefinedPriceType = false;
		if(priceScenario == hpData.dimensioningForPriceType().getValue())
			usingUserDefinedPriceType = true;

		FloatResource yearlyOperatingCostBivalent;
		FloatResource yearlyOperatingCostCondensing;

		if(priceScenario == HPAdaptData.USE_USER_DEFINED_PRICE_TYPE) {
			priceScenario = hpData.dimensioningForPriceType().getValue();
		}
		final PriceScenarioData scenario;
		if (priceScenario == HPAdaptData.PRICE_TYPE_100EE) {
			scenario = hpParams.prices100EE();
			yearlyOperatingCostBivalent = result.bivalent().yearlyOperatingCosts100EE();
			yearlyOperatingCostCondensing = result.condensing().yearlyOperatingCosts100EE();
		}
		else if (priceScenario == HPAdaptData.PRICE_TYPE_CO2_NEUTRAL) {
			scenario = hpParams.pricesCO2neutral();
			yearlyOperatingCostBivalent = result.bivalent().yearlyOperatingCostsCO2Neutral();
			yearlyOperatingCostCondensing = result.condensing().yearlyOperatingCostsCO2Neutral();
		}
		else { // Assume conventional
			scenario = hpParams.pricesConventional();
			yearlyOperatingCostBivalent = result.bivalent().yearlyOperatingCosts();
			yearlyOperatingCostCondensing = result.condensing().yearlyOperatingCosts();
		}
		priceVarHeatPower = scenario.electrictiyPriceHeatPerkWh().getValue();
		priceVarGas = scenario.gasPricePerkWh().getValue();
		// yearlyOperatingCostBivalent = result.bivalent().yearlyOperatingCostsCO2Neutral(); FIXME? Is this intentional?
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
		System.out.println("Price Type is " + HPAdaptData.PRICE_TYPE_NAMES_EN[priceScenario] + " (" + priceScenario + ")");
		System.out.println("sumHp is " + sumHp);
		System.out.println("sumBurn is " + sumBurn);
		System.out.println("maxPowerHPfromBadRoom is " + maxPowerHPfromBadRoom);
		System.out.println("copMin is " + copMin);

		float yearlyCostBivalent = sumHp * priceVarHeatPower + sumBurn * priceVarGas;
		ValueResourceHelper.setCreate(yearlyOperatingCostBivalent, yearlyCostBivalent);
		
		float yearlyCostCondensing = totalEnergyPostRenovation * priceVarGas;
		ValueResourceHelper.setCreate(yearlyOperatingCostCondensing, yearlyCostCondensing);

		// Save cost at chosen level for amortization calculation.
		if (usingUserDefinedPriceType) {
			this.yearlyCostBivalent = yearlyCostBivalent;
			this.yearlyCostCondensing = yearlyCostCondensing;
		}
		
		/* END RoomEval3 */
	}

	
	/**
	 * Calculate remaining simple results that depend on other results.
	 * @param result Sets the following result resources:
	 * powerLossAtFreezing, powerLossAtOutsideDesignTemp, fullLoadHoursInclWW,
	 * fullLoadHoursExclWW, boilerPowerBoilerOnly, hpPowerBivalentHP,
	 * boilerPowerBivalentHP, bivalent.costOfProject, condensing.costOfProject,
	 * bivalent.amortization.
	 * @param hpData
	 */
	private void calcRemaining(HPAdaptResult result, HPAdaptData hpData, HPAdaptParams hpParams) {

		/* GET VALUES */
		
		float otherPowerLoss = result.otherPowerLoss().getValue();
		float powerLossBasementHeating = result.powerLossBasementHeating().getValue();
		float heatingDegreeDays = result.heatingDegreeDays().getValue();
		float maxPowerHPfromBadRoom = result.maxPowerHPfromBadRoom().getValue();
		float totalEnergyPostRenovation = result.totalEnergyPostRenovation().getValue();

		float heatingLimitTemp = hpData.heatingLimitTemp().getCelsius();
		float outsideDesignTemp = hpData.outsideDesignTemp().getCelsius();
		
		float boilerChangeCDtoCD = hpParams.boilerChangeCDtoCD().getValue();
		float boilerChangeLTtoCD = hpParams.boilerChangeLTtoCD().getValue();
		float boilerChangeCDtoCDAdditionalPerkW = hpParams.boilerChangeCDtoCDAdditionalPerkW().getValue();
		float boilerChangeLTtoCDAdditionalPerkW = hpParams.boilerChangeLTtoCDAdditionalPerkW().getValue();
		float additionalBivalentHPBase = hpParams.additionalBivalentHPBase().getValue();
		float additionalBivalentHPPerkW = hpParams.additionalBivalentHPPerkW().getValue();
		
		BuildingData building = hpData.getParent();


		/* CALCULATE VALUES */

		float powerLossAtFreezing = powerLossBasementHeating + otherPowerLoss * heatingLimitTemp;
		ValueResourceHelper.setCreate(result.powerLossAtFreezing(), powerLossAtFreezing);
		
		float powerLossAtOutsideDesignTemp = powerLossBasementHeating
				+ otherPowerLoss * (heatingLimitTemp - outsideDesignTemp);
		ValueResourceHelper.setCreate(result.powerLossAtOutsideDesignTemp(), powerLossAtOutsideDesignTemp);

		float fullLoadHoursExclWW = 24 * heatingDegreeDays / (heatingLimitTemp - outsideDesignTemp);
		float fullLoadHoursInclWW = fullLoadHoursExclWW; // TODO ???
		ValueResourceHelper.setCreate(result.fullLoadHoursInclWW(), fullLoadHoursInclWW);
		ValueResourceHelper.setCreate(result.fullLoadHoursExclWW(), fullLoadHoursExclWW);
		
		float usage_hours;
		if (building.wwViaHeatingBurner().getValue())
			usage_hours = fullLoadHoursInclWW;
		else
			usage_hours = fullLoadHoursExclWW;
		
		float boilerPowerBoilerOnly = totalEnergyPostRenovation / usage_hours;
		ValueResourceHelper.setCreate(result.boilerPowerBoilerOnly(), boilerPowerBoilerOnly * 1000);

		float hpPowerBivalentHP = maxPowerHPfromBadRoom * boilerPowerBoilerOnly / powerLossAtOutsideDesignTemp;
		ValueResourceHelper.setCreate(result.hpPowerBivalentHP(), hpPowerBivalentHP * 1000);

		float boilerPowerBivalentHP = boilerPowerBoilerOnly - hpPowerBivalentHP;
		ValueResourceHelper.setCreate(result.boilerPowerBivalentHP(), boilerPowerBivalentHP * 1000);

		boolean isCondensingBurner = building.condensingBurner().getValue();

		float boilerChangeCostHP;
		if (isCondensingBurner)
			boilerChangeCostHP = boilerChangeCDtoCD + boilerChangeCDtoCDAdditionalPerkW * boilerPowerBivalentHP;
		else
			boilerChangeCostHP = boilerChangeLTtoCD + boilerChangeLTtoCDAdditionalPerkW * boilerPowerBivalentHP;
		float costOfInstallingBivalentSystem = boilerChangeCostHP + additionalBivalentHPBase
				+ additionalBivalentHPPerkW * hpPowerBivalentHP;
		
		float costOfInstallingCondensingBoiler;
		if(isCondensingBurner) {
			costOfInstallingCondensingBoiler =
					boilerChangeCDtoCD + boilerChangeCDtoCDAdditionalPerkW * boilerPowerBoilerOnly;
		} else {
			costOfInstallingCondensingBoiler =
					boilerChangeLTtoCD + boilerChangeLTtoCDAdditionalPerkW * boilerPowerBoilerOnly;
		}
		
		ValueResourceHelper.setCreate(result.bivalent().costOfProject(), costOfInstallingBivalentSystem);
		ValueResourceHelper.setCreate(result.condensing().costOfProject(), costOfInstallingCondensingBoiler);
				

		float amortization = (costOfInstallingBivalentSystem - costOfInstallingCondensingBoiler)
				/ (yearlyCostCondensing - yearlyCostBivalent);
		ValueResourceHelper.setCreate(result.bivalent().amortization(), amortization);

	}

	/* * * * * * * * * * * * * * * * * *
	 *      HELPER FUNCTIONS           *
	 * * * * * * * * * * * * * * * * * */

	final private static int HEATING_NO_YEARS_MAX = 3;
	/**
	 * Get yearly heating energy consumption for a building.
	 * Throws an exception if data not found.
	 * @param building
	 * @return yearly heating consumption in kWh, over the past
	 * {@value #HEATING_NO_YEARS_MAX} years.
	 */
	public static float getYearlyHeating(BuildingData building) {
		ResourceList<HeatCostBillingInfo> heatCostBillingInfo = building.heatCostBillingInfo();
		YearlyConsumption yearlyConsumption =
				BasicCalculations.getYearlyConsumption(heatCostBillingInfo, HEATING_NO_YEARS_MAX);
		if (yearlyConsumption == null)
			throw new RuntimeException("No yearly power consumption data provided!");
		return yearlyConsumption.avKWh;
	}

	/* * * * * * * * * * * * * * * * * * * * * * *
	 *   PROJECT PROVIDER FUNCTIONS              *
	 * * * * * * * * * * * * * * * * * * * * * * */

	@Override
	protected Class<? extends ProjectProposal100EE> getResultType() {
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
		
		if (Boolean.getBoolean("org.sp.calculator.hpadapt.HPAdaptEval.forceOverwriteCOPField"))
			params.copCharacteristics().delete();
		
		if (!params.exists() || !params.isActive())
			params.create();

		if (!params.copCharacteristics().exists()) {
			SmartEff2DMap cop = params.copCharacteristics().create();
			FloatArrayResource outsideTemp = cop.primaryKeys().create();
			FloatArrayResource supplyTemp = cop.secondaryKeys().create();
			float[] outsideTemps = {-20f, -15f, -10f, -5f, 0f, 5f, 10f, 15f, 20f, 25f, 30f};
			outsideTemp.setValues(outsideTemps);
			supplyTemp.setValues(new float[] {35, 50, 65});
			cop.characteristics().create();
			final float[][] copField = {
					/* out/supply	 35°C	50°C	65°C	*/
					/* -20°C */ {	1.90f,	1.90f,	1.00f,	},
					/* -15°C */ {	2.50f,	2.20f,	1.00f,	},
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
				SmartEff2DMapPrimaryValue val = cop.characteristics().add();
				val.index().create();
				val.index().setValue(i);
				val.val().create();
				val.val().setValues(copField[i]);
			}
			cop.activate(true);
		}
		if (!params.temperatureHistory().exists())
			params.temperatureHistory().create();
		
		// Perhaps move to properties?
		if(
				ValueResourceHelper.setIfNew(params.pricesCO2neutral().electrictiyPricePerkWh(), 0.259f) |
				ValueResourceHelper.setIfNew(params.prices100EE().electrictiyPricePerkWh(), 0.249f) |
				ValueResourceHelper.setIfNew(params.pricesConventional().electricityPriceHeatBase(), 112) |
				ValueResourceHelper.setIfNew(params.pricesCO2neutral().electricityPriceHeatBase(), 112) |
				ValueResourceHelper.setIfNew(params.prices100EE().electricityPriceHeatBase(), 112) |
				ValueResourceHelper.setIfNew(params.pricesConventional().electricityPriceBase(), 184) |
				ValueResourceHelper.setIfNew(params.pricesCO2neutral().electricityPriceBase(), 184) |
				ValueResourceHelper.setIfNew(params.prices100EE().electricityPriceBase(), 184) |
				ValueResourceHelper.setIfNew(params.pricesConventional().electrictiyPriceHeatPerkWh(), 0.191f) |
				ValueResourceHelper.setIfNew(params.pricesCO2neutral().electrictiyPriceHeatPerkWh(), 0.201f) |
				ValueResourceHelper.setIfNew(params.prices100EE().electrictiyPriceHeatPerkWh(), 0.201f) |
				ValueResourceHelper.setIfNew(params.pricesCO2neutral().gasPricePerkWh(), 0.099f) |
				ValueResourceHelper.setIfNew(params.prices100EE().gasPricePerkWh(), 0.15f) |
				ValueResourceHelper.setIfNew(params.pricesConventional().gasPricePerkWh(), 0.0532f) |
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
