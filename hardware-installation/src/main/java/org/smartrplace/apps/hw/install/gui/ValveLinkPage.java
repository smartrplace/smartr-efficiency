package org.smartrplace.apps.hw.install.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.util.DatapointImpl;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.devices.connectiondevices.ThermalValve;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gui.tablepages.ObjectGUITablePageNamed;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

public class ValveLinkPage extends ObjectGUITablePageNamed<ThermalValvePlus, ThermalValve> {
	private final ApplicationManagerPlus appManPlus;
	
	public ValveLinkPage(WidgetPage<?> page, ApplicationManagerPlus appManPlus) {
		super(page, appManPlus.appMan(), null);
		this.appManPlus = appManPlus;
		triggerPageBuild();
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return "Link FAL230 thermal valves to wall thermostats";
	}
	
	@Override
	public void addWidgets(ThermalValvePlus object, ObjectResourceGUIHelper<ThermalValvePlus, ThermalValve> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		addNameLabel(object, vh, id, row, req);
		if(req == null) {
			vh.registerHeaderEntry("Linked to Wall Thermostat");
			return;
		}
		Map<Thermostat, String> valuesToSet = new HashMap<>();
		DeviceHandlerProviderDP<Thermostat> devHand = appManPlus.dpService().getDeviceHandlerProvider("org.smartrplace.homematic.devicetable.WallThermostatHandler");
		Collection<InstallAppDevice> wallIads = appManPlus.dpService().managedDeviceResoures("WallThermostatHandler", true);
		for(InstallAppDevice wallth: wallIads) {
			Thermostat wth = wallth.device().getLocationResource();
			String wthname = DatapointImpl.getDeviceLabel(wallth, null, appManPlus.dpService(), devHand);
			valuesToSet.put(wth, wthname);
		}
		vh.referenceDropdownFixedChoice("Linked to Wall Thermostat", id, object.res.getSubResource("linkedThermostat", Thermostat.class), row,
				valuesToSet );
	}

	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "Valve";
	}

	@Override
	protected String getLabel(ThermalValvePlus obj, OgemaHttpRequest req) {
		String subName = DeviceTableRaw.getSubNameAfterSeparator(obj.res, '_');
		return obj.fal230Device.deviceId().getValue()+"_"+subName;
	}

	@Override
	public Collection<ThermalValvePlus> getObjectsInTable(OgemaHttpRequest req) {
		List<ThermalValvePlus> result = new ArrayList<>();
		Collection<InstallAppDevice> iads = appManPlus.dpService().managedDeviceResoures("FAL230DeviceHandler", true);
		for(InstallAppDevice iad: iads) {
			List<ThermalValve> valves = iad.device().getLocationResource().getSubResources(ThermalValve.class, false);
			for(ThermalValve valve: valves) {
				ThermalValvePlus el = new ThermalValvePlus();
				el.fal230Device = iad;
				el.res = valve;
				result.add(el);
			}
		}
		return result;
	}

	@Override
	public String getLineId(ThermalValvePlus object) {
		String subName = DeviceTableRaw.getSubNameAfterSeparator(object.res, '_');
		return subName+super.getLineId(object);
	}
}
