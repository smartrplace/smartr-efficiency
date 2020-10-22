package org.smartrplace.alarming.extension.model;

import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.prototypes.Configuration;

public interface BatteryAlarmExtensionData extends Configuration {
	TimeResource alarmRetard();
}
