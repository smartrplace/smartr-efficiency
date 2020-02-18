package extensionmodel.smarteff.monitoring;

import javax.xml.crypto.Data;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.model.sensors.Sensor;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.SmartEffTimeSeries;

public interface AlarmConfigBase extends SmartEffResource {
	/** The resource or time series to be supervised by the AlarmConfig.
	 * The SmartEffTimeSeries may point to a schedule. In the future also
	 * {@link SmartEffTimeSeries#recordedDataParent()} shall be supported, but this is not implemented yet.
	 * Only either supervisedTS or supervisedSensor shall be present.
	 * If the respective resource or schedule has an active subresource of type IntegerResource named
	 * alarmStatus then this resource shall be set by the alarming logic.
	 */
	SmartEffTimeSeries supervisedTS();
	/** TODO: Change this to SingleValueResource in the future! Currently no alarming is possible e.g.
	 * for input from drivers not providing as sensors like JMBUS
	 * @return
	 */
	Sensor supervisedSensor();

	/** TODO: Implement this
	 * @return
	 */
	//Resource supervisedResource();
	
	FloatResource lowerLimit();
	FloatResource upperLimit();
	
	/** If the values are outside the limits specified not more than the interval time given here
	 * then no alarm will be generated (minutes)
	 */
	FloatResource maxViolationTimeWithoutAlarm();
	
	/** If the all is restarted usually all alarms are resent if the current value is still
	 * outside the limits. After sending/writing the alarm once
	 * the system shall wait for the duration specified here before sending/writing the alarm
	 * again if the value is outside the limits. If the alarmStatus is reset manually and the
	 * alarms occurs again then another alarm writing shall occur immediately.
	 * Given in minutes.
	 */
	FloatResource alarmRepetitionTime();
	
	/** If an alarm is detected and the alarmStatus resource is active then the value given
	 * here is written into the alarmStatus resource.
	 */
	IntegerResource alarmLevel();
	
	/** Maximum time between new values (minutes)*/
	FloatResource maxIntervalBetweenNewValues();
	
	/** If false or not set the alarm will not send messages. If both sendAlarm and 
	 * {@link #performAdditinalOperations()} is false then the alarm listener is not
	 * activated*/
	BooleanResource sendAlarm();
	
	/** Only relevant if the respective sensor type has additional operations such as a
	 * supervision that switches are always on
	 */
	BooleanResource performAdditinalOperations();
}
