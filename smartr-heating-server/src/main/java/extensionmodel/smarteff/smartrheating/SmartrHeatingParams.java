package extensionmodel.smarteff.smartrheating;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.TemperatureResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;

public interface SmartrHeatingParams extends SmartEffResource {
	/** Supply temperature of drinking water into the building*/
	TemperatureResource wwSupplyTemp();
	
	/**Working hours of customer for project independently of number of rooms
	 * Note that customer hours are currently not modeled in the spreadsheet.
	 * These are working hours for up to 3 persons that will perform the administration of the system (with one
	 * person acting as project leader). The time for normal users reading short instrucutions for system usage
	 * or participating in a 15 to 30 minute training are not part of the estimation.*/
	FloatResource hoursOfCustomerBase();

	/**Working hours of customer per room*/
	FloatResource hoursOfCustomerPerRoom();
	
	/**Working hours of customer for training and preparation of window sensor installation
	 * (applies once if window sensor installation is used)*/
	FloatResource hoursOfCustomerWinSensBase();

	/**Working hours of customer for window sensor installation*/
	FloatResource hoursOfCustomerPerWindowSensor();
	
	/** CO2 emissions of burning natural gas (kg/kWh)*/
	FloatResource co2factorGas();	
	
	//StringResource internalParamProvider();
}
