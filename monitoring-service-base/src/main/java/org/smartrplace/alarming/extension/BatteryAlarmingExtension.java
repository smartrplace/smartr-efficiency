package org.smartrplace.alarming.extension;

import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.devicefinder.api.AlarmingExtension;
import org.ogema.devicefinder.api.AlarmingExtensionListener;
import org.ogema.model.extended.alarming.AlarmConfiguration;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class BatteryAlarmingExtension implements AlarmingExtension {

	@Override
	public String id() {
		return this.getClass().getName();
	}

	@Override
	public String label(OgemaLocale locale) {
		return "BatteryLow";
	}

	@Override
	public boolean offerInGeneralAlarmingConfiguration(SingleValueResource res) {
		if(res.getName().equals("batteryLow") && res instanceof BooleanResource)
			return true;
		return false;
	}

	@Override
	public AlarmingExtensionListener getListener(SingleValueResource res, AlarmConfiguration ac) {
		return new AlarmingExtensionListener() {
			boolean isInAlarm = false;
			
			@Override
			public <T extends SingleValueResource> AlarmResult resourceChanged(T resource, float value, long now) {
				if(isInAlarm && value < 0.5) {
					isInAlarm = false;
					return new AlarmResult() {
						
						@Override
						public String message() {
							return "Release: Battery Alarm for "+resource.getLocation();
						}
						
						@Override
						public boolean isRelease() {
							return true;
						}
						
						@Override
						public int alarmValue() {
							return 0;
						}
					};
				} else if(value > 0.5 && (!isInAlarm)) {
					isInAlarm = true;
					return new AlarmResult() {
						
						@Override
						public String message() {
							return "Release: Battery Alarm for "+resource.getLocation();
						}
						
						@Override
						public boolean isRelease() {
							return true;
						}
						
						@Override
						public int alarmValue() {
							return 1;
						}
					};
				}
				return null;
			}
			
		};
	}

}
