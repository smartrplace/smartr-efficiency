package org.smartrplace.apps.alarmingconfig.expert.gui;

import java.util.ArrayList;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.apps.alarmingconfig.gui.DeviceTypePage;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gui.filtering.GenericFilterFixedSingle;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

@SuppressWarnings("serial")
public class DeviceTypePageExpert extends DeviceTypePage {

	public DeviceTypePageExpert(WidgetPage<?> page, ApplicationManagerPlus appManPlus,
			boolean showOnlyPrototype, AlarmingConfigAppController controller) {
		super(page, appManPlus, showOnlyPrototype, controller);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void addWidgets(AlarmConfiguration object,
			ObjectResourceGUIHelper<AlarmConfiguration, AlarmConfiguration> vh, String id, OgemaHttpRequest req,
			Row row, ApplicationManager appMan) {
		super.addWidgets(object, vh, id, req, row, appMan);
		vh.stringLabel("Res.Location", id, object.getLocation(), row);
	}
	
	@Override
	protected String getHeader(OgemaLocale locale) {
		return "1. Device Template Alarming Configuration Superadmin";
	}
	
	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		ButtonConfirm cleanUpButton = new ButtonConfirm(page, "cleanUpButton") {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				int count = cleanUp(req);
				alert.showAlert("Removed "+count+" doublets", false, req);				
			}
		};
		cleanUpButton.setDefaultConfirmMsg("Really remove all doublets?");
		cleanUpButton.setDefaultText("Remove line doublets");
		cleanUpButton.registerDependentWidget(alert);
		
		//StaticTable thirdTable = new StaticTable(1, 4);
		//thirdTable.setContent(0, 0, cleanUpButton);
		//page.append(thirdTable);
		topTable.setContent(0, 3, cleanUpButton);
	}
	
	protected int cleanUp(OgemaHttpRequest req) {
		List<InstallAppDevice> allDev = new ArrayList<>();
		int count = 0;
		for(InstallAppDevice dev: appMan.getResourceAccess().getResources(InstallAppDevice.class)) {
			DatapointGroup devTypeGrp = getDeviceTypeGroup(dev);
			GenericFilterFixedSingle<String> selected = (GenericFilterFixedSingle<String>) deviceDrop.getSelectedItem(req);
			if(devTypeGrp == null || (!devTypeGrp.id().equals(selected.getValue())))
				continue;
			allDev.add(dev);
		}
		for(InstallAppDevice dev: allDev) {
			List<AlarmConfiguration> doublets = AlarmingUtiH.getDoubletsForReferencingSensorVal(dev.alarms());
			count += doublets.size();
			for(AlarmConfiguration d: doublets)
				d.delete();
		}
		return count;
	}

}
