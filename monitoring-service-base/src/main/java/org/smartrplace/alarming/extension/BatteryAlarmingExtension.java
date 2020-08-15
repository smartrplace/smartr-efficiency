package org.smartrplace.alarming.extension;

import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.devicefinder.api.AlarmingExtension;
import org.ogema.devicefinder.api.AlarmingExtensionListener;
import org.ogema.model.extended.alarming.AlarmConfiguration;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class BatteryAlarmingExtension implements AlarmingExtension {

	@Override
	public String id() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String label(OgemaLocale locale) {
		return "BatteryLow";
	}

	@Override
	public boolean offerInGeneralAlarmingConfiguration(SingleValueResource res) {
		return true;
	}

	@Override
	public AlarmingExtensionListener getListener(SingleValueResource res, AlarmConfiguration ac) {
		// TODO Auto-generated method stub
		return null;
	}

}
