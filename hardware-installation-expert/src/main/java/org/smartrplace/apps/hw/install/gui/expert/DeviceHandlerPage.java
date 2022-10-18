package org.smartrplace.apps.hw.install.gui.expert;

import java.util.Collection;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gui.tablepages.ObjectGUITablePageNamed;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

public class DeviceHandlerPage extends ObjectGUITablePageNamed<DeviceHandlerProviderDP<?>, Resource> {
	//private final ApplicationManagerPlus appManPlus;
	private final DatapointService dpService;
	
	public DeviceHandlerPage(WidgetPage<?> page, ApplicationManagerPlus appManPlus) {
		super(page, appManPlus.appMan(), null);
		//this.appManPlus = appManPlus;
		this.dpService = appManPlus.dpService();
		triggerPageBuild();
	}

	@Override
	public String getHeader(OgemaLocale locale) {
		return "Device Type Handler Overview";
	}
	
	@Override
	public void addWidgets(DeviceHandlerProviderDP<?> object,
			ObjectResourceGUIHelper<DeviceHandlerProviderDP<?>, Resource> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan) {
		addNameLabel(object, vh, id, row, req);
		
		if(req == null) {
			vh.registerHeaderEntry("Title");
			vh.registerHeaderEntry("# Devices");
			vh.registerHeaderEntry("# Trash");
			return;
		}
		vh.stringLabel("Title", id, object.getTableTitle(), row);
		int devNum = 0;
		int trashNum = 0;
		Collection<InstallAppDevice> devices = dpService.managedDeviceResoures(object.id(), false, true);
		for(InstallAppDevice iad: devices) {
			if(iad.isTrash().getValue())
				trashNum++;
			else
				devNum++;
		}
		vh.stringLabel("# Devices", id, ""+devNum, row);
		vh.stringLabel("# Trash", id, ""+trashNum, row);
	}

	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "ShortID";
	}

	@Override
	protected String getLabel(DeviceHandlerProviderDP<?> obj, OgemaHttpRequest req) {
		return obj.getDeviceTypeShortId(dpService);
	}

	@Override
	public Collection<DeviceHandlerProviderDP<?>> getObjectsInTable(OgemaHttpRequest req) {
		return dpService.getDeviceHandlerProviders();
	}

}
