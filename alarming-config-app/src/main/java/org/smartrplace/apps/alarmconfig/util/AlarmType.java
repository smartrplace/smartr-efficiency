package org.smartrplace.apps.alarmconfig.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.ogema.generictype.GenericAttribute;
import org.ogema.generictype.GenericAttributeImpl;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class AlarmType {
	public static final String baseLink = "https://gitlab.smartrplace.de/i1/smartrplace/smartrplace-main/-/wikis/Operation/Alarms-for-Supervision#";
	private static final Map<String, AlarmType> knownTypes = new HashMap<>();
	
	private final String label;
	private final boolean isBasic;
	private final String link;
	private final List<String> knownSearchStrings;
	
	public AlarmType(String label) {
		this(label, label);
	}
	public AlarmType(String label, boolean isBasic) {
		this(label, getFullLink(label), isBasic, (String)null);
	}
	public AlarmType(String label, String searchStringAsLink) {
		this(label, getFullLink(searchStringAsLink), true, searchStringAsLink);
	}
	public AlarmType(String label, String shortLink, String searchString) {
		this(label,  getFullLink(shortLink), true, searchString);
	}
	public AlarmType(String label, String shortLink, String searchString, boolean isBasic) {
		this(label,  getFullLink(shortLink), isBasic, searchString);
	}
	public AlarmType(String label, String fullLink, boolean isBasic, String... searchStrings) {
		this.label = label;
		this.isBasic  = isBasic;
		this.link = fullLink;
		if(searchStrings.length == 0 || searchStrings[0] == null)
			knownSearchStrings = Collections.emptyList();
		else knownSearchStrings = Arrays.asList(searchStrings);
		knownTypes.put(id(), this);
	}

	public boolean isBasic() {
		return isBasic;
	}

	public String getLink() {
		return link;
	}

	public String id() {
		return label;
	}

	public String label(OgemaLocale locale) {
		return label;
	}

	/** Override if attributes exist */
	public List<GenericAttribute> attributes() {
		return Collections.emptyList();
	}
	
	/** Override if standard searching shall be override */
	public boolean isMessageRelevant(String message) {
		for(String searchStr: knownSearchStrings) {
			if(message.contains(searchStr))
				return true;
		}
		return false;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null || (!(obj instanceof GaRoDataTypeI)))
			return false;
		GaRoDataTypeI objc = (GaRoDataTypeI) obj;
		return id().equals(objc.id());
	}
	@Override
	public int hashCode() {
		   int prime = 31;
		   return prime + Objects.hashCode(this.id());    
	}
	
	public static String getFullLink(String baseLinkAsSearchString) {
		if(baseLinkAsSearchString==null)
			return null;
		return baseLink+baseLinkAsSearchString.toLowerCase().replace(" ", "-").replace("(", "").replace(")", "");
	}
	
	public static Collection<AlarmType> getKnownTypes() {
		return knownTypes.values();
	}
	
	public static AlarmType getAlarmType(String id) {
		return knownTypes.get(id);
	}
	
	//The following options are per-room
	public static final AlarmType NoMoreValuesReceived = new AlarmType("NoMoreValuesReceived", "ac200", "No more values received");
	public static final AlarmType SetpReact = new AlarmType("setpreact (Thermostat)", "ac120", "setpreact") {
		@Override
		public List<GenericAttribute> attributes() {
			return Arrays.asList(new GenericAttribute[] {GenericAttributeImpl.SENSOR_SEPARATE});
		}		
	};
	
	public static final AlarmType SystemRestartsLast2H = new AlarmType("NRI-GD1-SystemRestartsLast2h");
	public static final AlarmType DutyCylce = new AlarmType("DutyCycle", "ac125", "-DutyCycle");
	public static final AlarmType SystemUpdateStatus = new AlarmType("NRI-GD1-SystemUpdateStatus");
	public static final AlarmType MultiDeviceHighPrio = new AlarmType("NRI-MTPST2-hmDevicesLostHighPrio",
			"ac101",
			"NRI-MTPST2-hmDevicesLostHighPrio");
	public static final AlarmType SystemRestart = new AlarmType("NRI-GD1-SystemRestartsLast2h");
	public static final AlarmType BatteryLow = new AlarmType("BatteryLow", "ac130", "Battery Voltage");
	public static final AlarmType ValveErrorState = new AlarmType("ValveErrorState");
	public static final AlarmType DeviceMorningAlarm = new AlarmType("thermostats/roomcontrols (morning) still with open issues");
	public static final AlarmType CO2Concentration = new AlarmType("CO2Concentration");
	
	public static final AlarmType FaultDevice = new AlarmType("FaultDevice", "ac152", "NRI-FAUT");

	public static final AlarmType ThermostatJumpMinMax = new AlarmType("ThermostatJumpMinMax", "ac901", null, false);
}
