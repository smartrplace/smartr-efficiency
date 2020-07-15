package extensionmodel.smarteff.monitoring;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.SmartEffTimeSeries;

public interface AlarmConfigBase extends AlarmConfiguration, SmartEffResource {
	/** The resource or time series to be supervised by the AlarmConfig.
	 * The SmartEffTimeSeries may point to a schedule. In the future also
	 * {@link SmartEffTimeSeries#recordedDataParent()} shall be supported, but this is not implemented yet.
	 * Only either supervisedTS or supervisedSensor shall be present.
	 * If the respective resource or schedule has an active subresource of type IntegerResource named
	 * alarmStatus then this resource shall be set by the alarming logic.
	 */
	SmartEffTimeSeries supervisedTS();  // ignore for now
}
