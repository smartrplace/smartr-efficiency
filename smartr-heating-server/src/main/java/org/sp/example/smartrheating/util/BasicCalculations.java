package org.sp.example.smartrheating.util;

import java.util.Comparator;
import java.util.List;

import org.ogema.core.model.ResourceList;

import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.api.common.BuildingUnit;
import extensionmodel.smarteff.api.common.HeatCostBillingInfo;
import extensionmodel.smarteff.api.common.HeatRadiatorType;
import extensionmodel.smarteff.api.common.Window;
import extensionmodel.smarteff.api.common.WindowType;
import extensionmodel.smarteff.defaultproposal.DefaultProviderParams;

public class BasicCalculations {
	//TODO
	public static final float OIL2KWH = 10.3f;
	public static final float GAS2KWH = 10.2f;
	public static class YearlyConsumption {
		public float avKWh;
		public float avCostTotal; //total cost
		public Float avPriceperKWh; //variable price per kWh (may not exist)
	}
	public static final double DAY_MS = 24.0*3600*1000;
	public static final double YEAR_MS = 365.0*24*3600*1000; //more exact 365.25
	/** Estimate yearly consumption based on yearly billing data. The bills are weighted according to
	 * their duration, so a bill for a short time contributes less to the estimation
	 * 
	 * @param heatCostBillingInfo
	 * @param numberOfYearsMax the youngest bills up to this number are used - even if some
	 * 		of them are quite old e.g. because of a gap. This could be improved in the future.
	 * @return
	 */
	public static YearlyConsumption getYearlyConsumption(BuildingData building,
			Integer numberOfYearsMax, DefaultProviderParams myParB) {
		ResourceList<HeatCostBillingInfo> heatCostBillingInfo = building.heatCostBillingInfo();
		float kwHpSQM = myParB.defaultKwhPerSQM().getValue();

		if(heatCostBillingInfo == null | (!heatCostBillingInfo.isActive())) {
			YearlyConsumption result = new YearlyConsumption();
			result.avKWh = building.heatedLivingSpace().getValue() * kwHpSQM;
			result.avCostTotal = Float.NaN;
			result.avPriceperKWh = null;
			//TODO: use price data and calculate CO2
			return result;
		}
		return getYearlyConsumption(heatCostBillingInfo, numberOfYearsMax);
	}
	
	public static YearlyConsumption getYearlyConsumption(ResourceList<HeatCostBillingInfo> heatCostBillingInfo,
			Integer numberOfYearsMax) {
		int count = 0;
		long totalDuration = 0;
		float sumkWh = 0;
		float sumCostTotal = 0;
		long durationkWh = 0;
		float sumPriceKWh = 0;
		List<HeatCostBillingInfo> bills = heatCostBillingInfo.getAllElements();
		bills.sort(new Comparator<HeatCostBillingInfo>() {

			@Override
			public int compare(HeatCostBillingInfo o1, HeatCostBillingInfo o2) {
				//we want to have the youngest bills first, so we reverse order
				return Long.compare(o2.beginningOfBillingPeriodDay().getValue(),
						o1.beginningOfBillingPeriodDay().getValue());
			}
		});
		for(HeatCostBillingInfo bill: bills) {
			long duration = bill.endOfBillingPeriodDay().getValue() - bill.beginningOfBillingPeriodDay().getValue();
			//each day is given at 00:00:00, but end day is included
			//duration += DAY_MS;
			sumCostTotal += bill.cost().getValue();
			switch(bill.unit().getValue()) {
			case 1:
			case 2:
				if(bill.energyContentAccordingToBill().isActive() &&
						bill.energyContentAccordingToBill().getValue() > 0) {
					sumkWh += bill.billedConsumption().getValue() *
							bill.energyContentAccordingToBill().getValue();				
				} else {
					if(bill.unit().getValue() == 1)
						sumkWh += bill.billedConsumption().getValue() * GAS2KWH;
					else
						sumkWh += bill.billedConsumption().getValue() * OIL2KWH;
				}
				break;
			case 3:
				throw new IllegalStateException("Cannot calculate energy content of solid fuel!");
			case 4:
				//TODO: We are assuming gas/oil here
				sumkWh += bill.billedConsumption().getValue();
				break;
			case 5:
				sumkWh += bill.billedConsumption().getValue();
				break;
			default:
				throw new IllegalStateException("Unknown unit type on bill:"+bill.unit().getValue());
			}
			count++;
			totalDuration += duration;
			if(bill.costPerKWhVar().isActive() && bill.costPerKWhVar().getValue() >= 0) {
				sumPriceKWh += bill.costPerKWhVar().getValue();
				durationkWh += duration;
			}
			if(count >= numberOfYearsMax) break;
		}
		if(count == 0) return null;
		// In principle for each bill the cost and energy per ms should be calculated and weighted based
		// on the milliseconds, so division and multiplication of the duration cancel each other. Finally
		// we have to divide by the total duration, but have to normalize to a yearly consumption, which is
		// done with the factor below
		double duration2year = YEAR_MS/totalDuration;
		YearlyConsumption result = new YearlyConsumption();
		result.avKWh = (float) (sumkWh*duration2year);
		result.avCostTotal = sumCostTotal/totalDuration;
		if(durationkWh == 0)
			result.avPriceperKWh = null;
		else
			result.avPriceperKWh = (float) (sumPriceKWh*(YEAR_MS/durationkWh));
		return result;
	}
	
	public static Integer getNumberOfRooms(BuildingData building) {
		if(building.roomNum().isActive() && building.roomNum().getValue() > 0)
			return building.roomNum().getValue();
		if(building.buildingUnit().isActive() && building.buildingUnit().size() > 0)
			return building.buildingUnit().size();
		return null;
	}
	
	/** Get number of thermostats and rooms with thermostats
	 * @return [0]: number of thermostats, [1]: rooms with thermostats*/
	public static int[] getNumberOfRadiators(BuildingData building) {
		int count = 0;
		int countRooms = 0;
		int roomNumUnheated;
		if(building.unheatedRoomNum().isActive()) roomNumUnheated = building.unheatedRoomNum().getValue();
		else roomNumUnheated = -1;
		for(BuildingUnit room: building.buildingUnit().getAllElements()) {
			if(room.heatRadiator().size() > 0) {
				count += room.heatRadiator().size();
				countRooms++;
			}
		}
		if(count == 0 && (!building.heatRadiatorType().isActive()))
			return null;
		for(HeatRadiatorType rad: building.heatRadiatorType().getAllElements()) {
			count += rad.numberOfRadiators().getValue();
		}
		if(roomNumUnheated > -1) countRooms = getNumberOfRooms(building) - roomNumUnheated;
		return new int[] {count, countRooms};
	}
	
	/** Get number of window sensors, windows and rooms with windows
	 * @return [0]: number of window sensors, [1]: number of windows, 
	 * [2]: rooms with windows. Note that number of rooms is only calculated based on data provided
	 * by individual room modeling and the room number are currently not used at all.*/
	public static int[] getNumberOfWindowSensors(BuildingData building) {
		float countSens = 0;
		int countWin = 0;
		int countRoomsWin = 0;
		for(BuildingUnit room: building.buildingUnit().getAllElements()) {
			if(room.window().size() > 0) {
				countWin += room.window().size();
				countRoomsWin++;
				float sensNum = 0;
				for(Window win: room.window().getAllElements()) {
					sensNum += win.type().sensorInstallationShare().getValue();
				}
				countSens += sensNum;
			}
		}
		if(countWin == 0 && (!building.windowType().isActive()))
			return null;
		for(WindowType rad: building.windowType().getAllElements()) {
			countWin += rad.count().getValue();
			countSens += rad.count().getValue() * rad.sensorInstallationShare().getValue();
		}
		return new int[] {Math.round(countSens), countWin, countRoomsWin};
	}

}
