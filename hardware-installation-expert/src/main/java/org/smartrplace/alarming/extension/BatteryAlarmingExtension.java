package org.smartrplace.alarming.extension;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.devicefinder.api.AlarmingExtension;
import org.ogema.devicefinder.api.AlarmingExtensionListener;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.smartrplace.alarming.extension.model.BatteryAlarmExtensionData;
import org.smartrplace.hwinstall.basetable.HardwareTableData;

import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class BatteryAlarmingExtension implements AlarmingExtension {
	protected final BatteryAlarmExtensionData batAlarmExt;
	
	/** Note that the constructor is only called on system startup. Operations that shall be performed on every restart
	 * of alarming need to be implemented in #onStartAlarming()
	 * @param appMan
	 */
	public BatteryAlarmingExtension(ApplicationManager appMan) {
		HardwareTableData hwData = new HardwareTableData(appMan);
		this.batAlarmExt = ResourceListHelper.getOrCreateNamedElementFlex(hwData.appConfigData.alarmingConfig(),
				BatteryAlarmExtensionData.class);
	}

	@Override
	public String id() {
		return this.getClass().getName();
	}

	@Override
	public String label(OgemaLocale locale) {
		return "BatteryLow";
	}

	@Override
	public boolean offerInGeneralAlarmingConfiguration(AlarmConfiguration ac) {
		if(ac == null)
			return true;
		Resource res = ac.sensorVal().getLocationResource();
		if(res.getName().equals("batteryLow") && res instanceof BooleanResource)
			return true;
		return false;
	}

	@Override
	public AlarmingExtensionListener getListener(SingleValueResource res, AlarmConfiguration ac) {
		final AlarmingExtension superThis = this;
		return new AlarmingExtensionListener() {
			boolean isInAlarm = false;
			
			@Override
			public <T extends SingleValueResource> AlarmResult resourceChanged(final T resource, float value, long now) {
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
							return "Battery Alarm for "+resource.getLocation();
						}
						
						@Override
						public boolean isRelease() {
							return false;
						}
						
						@Override
						public int alarmValue() {
							return 1;
						}
						
						@Override
						public Long retard() {
							if(batAlarmExt.alarmRetard().isActive())
								return batAlarmExt.alarmRetard().getValue();
							return null;
						}
					};
				}
				return null;
			}

			@Override
			public AlarmingExtension sourceExtension() {
				return superThis;
			}
			
		};
	}

}
