package org.sp.calculator.hpadapt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.schedule.AbsoluteSchedule;
import org.slf4j.LoggerFactory;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.proposal.ProjectProposal;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.MyParam;
import org.smartrplace.smarteff.util.ProjectProviderBase;
import org.sp.example.smartrheating.util.BaseInits;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.api.common.BuildingUnit;
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
	protected void calculateProposal(HPAdaptData hpData, ProjectProposal resultProposal, ExtensionResourceAccessInitData data) {
		
		if(!(resultProposal instanceof HPAdaptResult)) {
			// TODO Log error and throw exception
			System.out.println("Wrong Result type!");
			return;
		}
		else {
			HPAdaptResult result = (HPAdaptResult) resultProposal;
			calculateHPAdapt(hpData, result, resultProposal, data);
		}
	}
	
	/* Constants */
	final static int LOWEST_TEMP = -20;
	final static int HIGHEST_TEMP = 40;

	/**
	 * Calculate HPAdapt-specific Result
	 * Note: variables named_with_underscores indicate values that have yet to be added to their respective data models.
	 */
	protected void calculateHPAdapt(HPAdaptData hpData, HPAdaptResult result, ProjectProposal resultProposal,
			ExtensionResourceAccessInitData data) {
		
		/* Setup */
		MyParam<HPAdaptParams> hpParamHelper =
				CapabilityHelper.getMyParams(HPAdaptParams.class, data.userData(), appManExt);
		HPAdaptParams hpParams = hpParamHelper.get();
		
		MyParam<DefaultProviderParams> defParamHelper =
				CapabilityHelper.getMyParams(DefaultProviderParams.class, data.userData(), appManExt);
		DefaultProviderParams defParams = defParamHelper.get();
		
		BuildingData building = hpData.getParent();

		float heatingLimitTemp = hpData.heatingLimitTemp().getCelsius();
		/* Calculate: Heating days, Heating degree days on daily and hourly basis */
		int heatingDegreeDays = 0;
		int numberOfHeatingDays = 0;

		Map<Integer, Integer> temperatureShares = new HashMap<>();
		
		AbsoluteSchedule temperaturesHistory = hpData.temperatureHistory().recordedDataParent().program();
		Iterator<SampledValue> iter = temperaturesHistory.iterator();
		while(iter.hasNext()) {
			float meanOutsideDaytimeTemperature = iter.next().getValue().getFloatValue();
			if (meanOutsideDaytimeTemperature < heatingLimitTemp) {
				numberOfHeatingDays += 1;
				heatingDegreeDays += heatingLimitTemp - meanOutsideDaytimeTemperature;
			}

			int tempRounded = Math.round(meanOutsideDaytimeTemperature);
			int n = 1;
			if(temperatureShares.containsKey(tempRounded))
				n = temperatureShares.get(tempRounded) + 1;
			temperatureShares.put(tempRounded, n);
		}
		ValueResourceHelper.setCreate(result.heatingDegreeDays(), heatingDegreeDays);
		ValueResourceHelper.setCreate(result.numberOfHeatingDays(), numberOfHeatingDays);
		
		int heatingDegreeDaysHourly = 0;
		int numberOfHeatingDaysHourly = 0;
		// For each recorded hour of the year { // TODO
		/*	float outside_temperature = 0f; // TODO
				if (outside_temperature < heatingLimitTemp) {
					numberOfHeatingDaysHourly += 1;
					heatingDegreeDaysHourly += heatingLimitTemp - outside_temperature;
				}
		*/
		// }
		//ValueResourceHelper.setCreate(result.heatingDegreeDaysHourly(), heatingDegreeDaysHourly);
		//ValueResourceHelper.setCreate(result.numberOfHeatingDaysHourly(), numberOfHeatingDaysHourly);
		
		/* Perform calculations on temperature Shares */
		for(int i = LOWEST_TEMP; i <= HIGHEST_TEMP; i++) {
			// TODO?
		}
		
		/* U-VALUE AND ROOM VALUE BASED DEMAND */

		/* Calculate total window area and window losses / RoomEval1 */
		List<Window> allWindows = building.getSubResources(Window.class, true);
		
		float totalWindowArea = 0.0f; // m²
		float totalPLossWindow = 0.0f; // W/K
		
		for(Window window : allWindows) {
			float uValue = window.type().uValue().getValue(); // W/(m²*K)
			float height = window.type().height().getValue(); // m
			float width = window.width().getValue(); // m
			float area = height * width; // m²
			
			totalWindowArea += area;
			totalPLossWindow += uValue * area;
		}
		
		ValueResourceHelper.setCreate(result.windowArea(), totalWindowArea);
		ValueResourceHelper.setCreate(result.pLossWindow(), totalPLossWindow);
		
		/* End RoomEval1 */
		
		/* Find most critical room for each temperature / RoomEval2 */
		
		float vLMax = - Float.MAX_VALUE;
		float pMax = - Float.MAX_VALUE;
		
		float minTemp = hpData.outsideDesignTemp().getCelsius();
		
		List<BuildingUnit> rooms = building.getSubResources(BuildingUnit.class, true);
		if(rooms.isEmpty())
			LoggerFactory.getLogger(HPAdaptEval.class).error("No rooms found in {}!", building.name().getValue());

		/* Values needed for RoomEval3 */
		float totalRoofArea = 0f;
		float totalBasementArea = 0f;
		float totalFacadeWallArea = 0f;
		for(BuildingUnit room : rooms) {
			float wall_height = 2.8f; // TODO
			float roof_share = 1.0f; // TODO add
			totalRoofArea += room.groundArea().getValue() * roof_share;
			float basement_share = 1.0f; // TODO add
			totalBasementArea += room.groundArea().getValue() * basement_share;
			totalFacadeWallArea += room.totalOutsideWallArea().getValue() 
					+ wall_height * hpData.innerWallThickness().getValue() * 0.01
					- room.outsideWindowArea().getValue();
		}
		ValueResourceHelper.setCreate(result.roofArea(), totalRoofArea);
		ValueResourceHelper.setCreate(result.basementArea(), totalBasementArea);
		ValueResourceHelper.setCreate(result.facadeWallArea(), totalFacadeWallArea);
		

		/** Temperature -> COP for worst room at that temperature. */
		Map<Integer, Float> badRoomCops = new HashMap<>();

		for(int temp = Math.round(heatingLimitTemp); temp >= Math.round(minTemp); temp--) {
			
			BuildingUnit badRoom = null; // Most critical room for temperature
			float deltaT = heatingLimitTemp - temp;
			
			for(BuildingUnit room : rooms) {
				/* Get radiator data */
				int radType = 0;
				List<HeatRadiator> radiators = room.heatRadiator().getAllElements();
				if(radiators.size() > 0) {
					radType = radiators.get(0).radiatorType().radiatorType().getValue();
				}
				float radPower = 1200.0f / 35.0f; // TODO use actual value; = Pth bei Nenn-DeltaT (kW) / Nenn-DeltaT
				
				/* Window loss */
				List<Window> windows = room.getSubResources(Window.class, true);
				if(windows.isEmpty())
					LoggerFactory.getLogger(HPAdaptEval.class).warn("No windows found in {}!", room.name().getValue());
				float windowLoss = 0.0f;
				float windowArea = 0.0f;
				for(Window window : windows) {
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

				float ceil_share = 1.0f; // TODO use acual
				float ceilLoss = (room.groundArea().getValue() * ceil_share); // TODO what about U-Value?
				
				float pLoc = (windowLoss + wallLoss + ceilLoss) * deltaT;
				
				float basement_share = 1.0f; // TODO use actual value
				float basementLoss = room.groundArea().getValue() * basement_share; // TODO what about U-Value?
				pLoc += basementLoss * (heatingLimitTemp - hpData.basementTempHeatingSeason().getCelsius());
				
				float comfort_temp = 20.0f; // TODO use actual value
				
				float vLLoc = pLoc / radPower + comfort_temp;
				
				if(vLLoc > vLMax) {
					vLMax = vLLoc;
					pMax = pLoc;
					badRoom = room;
				}
			}
				
			if(badRoom == null) {
				LoggerFactory.getLogger(HPAdaptEval.class).warn("No BadRoom was determined!");
				continue;
			}

			// TODO store these?
			// temp
			// String badRoomName = badRoom.name().getValue();
			// pMax
			// vLMax

				
			/* Finally get COP and related data */
			float firstCol = -1f;
			float firstRow = -1f;
			float deltaCol = -1f;
			float deltaRow = -1f;
			
			/* TODO: This is a oversimplied interpolation, but should work for now */
			float factor = (float) (Math.sqrt(deltaRow * deltaRow + deltaCol * deltaCol) / Math.sqrt(2.0));
			float firstVal;
			float deltaVal;
			float cop = 1.0f; // TODO! calc and store
			
			badRoomCops.put(temp, cop);
			
		}
		/* End RoomEval2 */
		
		
		/* Bivalent optimization */
		
		
		boolean control_fixed_price_type = false; // TODO? add
		
		calcPriceLevel(HPAdaptData.USE_USER_DEFINED_PRICE_TYPE, hpData, result, resultProposal, hpParams,
				temperatureShares, badRoomCops); // Do we need this?
		
		if(control_fixed_price_type) {
			calcPriceLevel(HPAdaptData.USE_USER_DEFINED_PRICE_TYPE, hpData, result, resultProposal, hpParams,
					temperatureShares, badRoomCops);
		}
		else {
			for(int i = 0; i <= HPAdaptData.PRICE_TYPES_COUNT; i++) {
				calcPriceLevel(i, hpData, result, resultProposal, hpParams,
						temperatureShares, badRoomCops);
			}
		}
		
		hpParamHelper.close();
		defParamHelper.close();
	}
	
	


	/**
	 * Perform COP evaluation / RoomEval3
	 */
	protected void calcPriceLevel(int priceType, HPAdaptData hpData, HPAdaptResult result,
			ProjectProposal resultProposal, HPAdaptParams hpParams,
			Map<Integer, Integer> temperatureShares, Map<Integer, Float> badRoomCops) {
		
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
		if(priceType == HPAdaptData.USE_USER_DEFINED_PRICE_TYPE)
			priceType = hpData.dimensioningForPriceType().getValue();

		if(priceType == HPAdaptData.PRICE_TYPE_100EE) {
			priceVarHeatPower = hpParams.electrictiyPriceHeat100EEPerkWh().getValue();
			priceVarGas = hpParams.gasPrice100EEPerkWh().getValue();
		}
		else if(priceType == HPAdaptData.PRICE_TYPE_CO2_NEUTRAL) {
			priceVarHeatPower = hpParams.electrictiyPriceHeatCO2neutralPerkWh().getValue();
			priceVarGas = hpParams.electrictiyPriceHeatCO2neutralPerkWh().getValue();
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
		for(int temp = Math.round(heatingLimitTemp); temp >= Math.round(LOWEST_TEMP); temp--) {
			float cop = badRoomCops.getOrDefault(temp, 0.0f);
			int nShares = temperatureShares.getOrDefault(temp, 0);
			
			float meanHeatingOutsideTemp = heatingLimitTemp - heatingDegreeDays / numberOfHeatingDays;
			result.meanHeatingOutsideTemp().create();
			result.meanHeatingOutsideTemp().setCelsius(meanHeatingOutsideTemp);

			float yearly_heating_energy_consumption = 20000; // kWh, TODO add
			boolean condensing_boiler = false;
			float activePowerWhileHeating =
					yearly_heating_energy_consumption * (condensing_boiler ? 1 : 0) - 0; // TODO replace last 0 with ???
			ValueResourceHelper.setCreate(result.activePowerWhileHeating(), activePowerWhileHeating);
			
			float totalPowerloss = activePowerWhileHeating / (heatingLimitTemp - meanHeatingOutsideTemp);
			ValueResourceHelper.setCreate(result.totalPowerLoss(), totalPowerloss);

			float weightedExtSurfaceAreaExclWindows =
					facadeWallArea
					+ roofArea + hpData.uValueRoofFacade().getValue()
					+ basementArea + hpData.uValueBasementFacade().getValue()
					* (heatingLimitTemp + hpData.basementTempHeatingSeason().getValue());
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
			ValueResourceHelper.setCreate(result.pLossWindow(), pLossWindow);
			
			float p = powerLossBasementHeating + otherPowerLoss * (heatingLimitTemp - temp);
			// TODO save these?
			float pTotperkW = p / 1000f;
			float pLoc;
			float wElPerkWh;
			float wGas;
			if(cop >= copMin) {
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
		// maxPowerHp
		// sumHp
		// sumBurn
		
		// TODO save these?
		// sumHp
		// sumBurn
		System.out.println("sumHp is " + sumHp);
		System.out.println("sumBurn is " + sumBurn);
		
		/* END RoomEval3 */
	}
	

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
		HPAdaptParams params = (HPAdaptParams)paramsIn;

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
				ValueResourceHelper.setIfNew(params.wwSupplyTemp(), (273.15f + 8.0f))
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
