package org.smartrplace.driverhandler.base.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.resourcemanipulator.timer.CountDownDelayedExecutionTimer;
import org.smartrplace.device.testing.ThermostatTestingConfig;
import org.smartrplace.util.virtualdevice.HmSetpCtrlManager.WritePrioLevel;
import org.smartrplace.util.virtualdevice.HmSetpCtrlManagerTHSetp;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;

public class DeviceHandlerThermostatUtil {
	public static final Map<Integer, String> valveErrorCodes = new HashMap<Integer, String>();
	static  {
		valveErrorCodes.put(0, "STATE_NOT_AVAILABLE"); //Zustand unbestimmt
		valveErrorCodes.put(1, "RUN_TO_START"); // nach dem einlegen der Batterien (im Display VALVE install)
		valveErrorCodes.put(2, "WAIT_FOR_ADAPTION"); // wartet darauf das die Boost Taste gedrückt wird um die Adaptionsfahrt zu beginnen (im Display VALVE adapt)
		valveErrorCodes.put(3, "ADAPTION_IN_PROGRESS"); // Adaptionsfahrt läuft (im Display VALVE adapt)
		valveErrorCodes.put(4, "ADAPTION_DONE"); // Adaptionsfahrt abgeschlossen
		valveErrorCodes.put(5, "TOO_TIGHT"); // Fehler F1 Ventil schwergängig
		valveErrorCodes.put(6, "ADJUSTMENT_TOO_BIG"); // Fehler F2 Stellbereich zu groß
		valveErrorCodes.put(7, "ADJUSTMENT_TOO_SMALL"); // Fehler F3 Stellbereich zu klein
		valveErrorCodes.put(8, "ERROR_POSITION"); //vermutlich wird hier die Frostschutz Ventilposition angefahren, wenn die Batterieschwelle einen gewisse Schwelle unterschreitet		
	}
	
	protected static CountDownDelayedExecutionTimer testSwitchTimer = null;
	protected static ThermostatTestingConfig testConfig;
	protected static class SetpointToTest {
		public SetpointToTest(TemperatureResource setp, HmSetpCtrlManagerTHSetp hmMan) {
			this.setp = setp;
			this.hmMan = hmMan;
		}
		TemperatureResource setp;
		HmSetpCtrlManagerTHSetp hmMan;
	}
	protected static Set<SetpointToTest> setpointsToTest = new HashSet<>();
	protected static Map<String, Float> testValue;

	public static void addThermostatToTestSwitch(Thermostat th, ApplicationManager appMan, HmSetpCtrlManagerTHSetp hmMan) {
		synchronized(DeviceHandlerThermostatUtil.class) {
			if(testConfig == null) {
				testConfig = ResourceHelper.getEvalCollection(appMan).getSubResource(
						"thermostatTestingConfig", ThermostatTestingConfig.class);				
			}
			if(testConfig.testSwitchingInterval().getValue() == 0)
				return;
			if(testSwitchTimer == null) {
				testSwitchTimer = startTestTimer(appMan);
			}	
			setpointsToTest.add(new SetpointToTest(th.temperatureSensor().settings().setpoint(), hmMan));
		}
	}
	
	protected static CountDownDelayedExecutionTimer startTestTimer(ApplicationManager appMan) {
		long interval = testConfig.testSwitchingInterval().getValue();
		final boolean isBack;
		if(interval < 0) {
			isBack = true;
			interval = Math.min(-interval, 10*TimeProcUtil.MINUTE_MILLIS);
		} else
			isBack = false;
		CountDownDelayedExecutionTimer result = new CountDownDelayedExecutionTimer(appMan, interval) {
			
			@Override
			public void delayedExecution() {
				try {
				if(isBack)
					createMap();
				else
					testValue = new HashMap<>();
				for(SetpointToTest setp: setpointsToTest) {
					if(isBack) {
						Float preVal = testValue.get(setp.setp.getLocation());
						if(preVal != null && preVal != setp.setp.getValue())
							continue;
					}
					float destValue = isBack?(setp.setp.getValue()+0.5f):(setp.setp.getValue()-0.5f);
					if(destValue < (273.15f+4.5f))
						destValue = 273.15f+5.0f;
					if(setp.hmMan != null)
						setp.hmMan.requestSetpointWrite(setp.setp, destValue, WritePrioLevel.CONDITIONAL, false);
					else
						setp.setp.setValue(destValue);
					if(!isBack) {
						testValue.put(setp.setp.getLocation(), destValue);
					}
				}
				if(!isBack)
					createMapResources();
				testConfig.testSwitchingInterval().setValue(-testConfig.testSwitchingInterval().getValue());
				if(testConfig.testSwitchingInterval().getValue() == 0)
					return;
				startTestTimer(appMan);
				} catch(Exception e) {
					appMan.getLogger().error("TEST SWITCHING failed:", e);
					startTestTimer(appMan);
				}
			}
		};
		return result;
	}
	
	protected static void createMap() {
		testValue = new HashMap<>();
		String[] locs = testConfig.testSwitchingLocation().getValues();
		float[] vals = testConfig.testSwitchingSetpoint().getValues();
		for(int idx=0; idx<locs.length; idx++) {
			testValue.put(locs[idx], vals[idx]);
		}
	}
	
	protected static void createMapResources() {
		String[] locs = new String[testValue.size()];
		float[] vals = new float[testValue.size()];
		int idx = 0;
		for(Entry<String, Float> e: testValue.entrySet()) {
			locs[idx] = e.getKey();
			vals[idx] = e.getValue();
			idx++;
		}
		ValueResourceHelper.setCreate(testConfig.testSwitchingLocation(), locs);
		ValueResourceHelper.setCreate(testConfig.testSwitchingSetpoint(), vals);
	}
	
	/** How to set thermostat auto-config?
	 * 
	 * @return 1: create, 0: do not change, -1: delete
	 */
	public static int autoModeStatus(PhysicalElement device) {
		if(Boolean.getBoolean("org.smartrplace.driverhandler.base.util.thermostatauto.all")) {
			return 1;
		}
		String addTherm = System.getProperty("org.smartrplace.driverhandler.base.util.thermostatauto.list");
		if(addTherm != null && addTherm.contains(device.getName())) {
			return 1;
		}
		if(Boolean.getBoolean("org.smartrplace.driverhandler.base.util.thermostatauto.none")) {
			return -1;
		}
		return 0;
	}
}
