package org.smartrplace.apps.alarmingconfig.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.internationalization.util.LocaleHelper;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gui.filtering.GenericFilterFixedSingle;
import org.smartrplace.gui.filtering.GenericFilterOption;
import org.smartrplace.gui.filtering.SingleFiltering;
import org.smartrplace.gui.filtering.SingleFiltering.OptionSavingMode;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

@SuppressWarnings("serial")
public class DeviceTypePage extends MainPage {
	protected final boolean showOnlyPrototype;
	protected SingleFiltering<String, AlarmConfiguration> deviceDrop;
	
	public DeviceTypePage(WidgetPage<?> page, ApplicationManagerPlus appManPlus, boolean showOnlyPrototype) {
		super(page, appManPlus);
		this.showOnlyPrototype = showOnlyPrototype;
	}

	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		
		deviceDrop = new SingleFiltering<String, AlarmConfiguration>(
				page, "deviceDrop", OptionSavingMode.GENERAL, 10000, true) {

			@Override
			protected boolean isAttributeSinglePerDestinationObject() {
				return true;
			}
			
			@Override
			public String getAttribute(AlarmConfiguration attr) {
				InstallAppDevice iad = ResourceHelper.getFirstParentOfType(attr, InstallAppDevice.class);
				if(iad == null)
					return null;
				if(showOnlyPrototype && (!iad.isTemplate().isActive()))
					return null;
				String devLoc = iad.device().getLocation();
				for(DatapointGroup dpGrp: appManPlus.dpService().getAllGroups()) {
					if(dpGrp.getType().equals("DEVICE_TYPE") && (dpGrp.getSubGroup(devLoc) != null)) {
						return dpGrp.id();
					}
				}
				return null;
			}
			
			@Override
			protected List<GenericFilterOption<String>> getOptionsDynamic(OgemaHttpRequest req) {
				List<GenericFilterOption<String>> result = new ArrayList<>();
				for(DatapointGroup dpGrp: appManPlus.dpService().getAllGroups()) {
					if(dpGrp.getType().equals("DEVICE_TYPE")) {
						GenericFilterOption<String> option = new GenericFilterFixedSingle<String>(
								dpGrp.id(), LocaleHelper.getLabelMap(dpGrp.label(null)));
						result.add(option );
					}
				}
				return result;
			}
		};
		deviceDrop.registerDependentWidget(mainTable);
		StaticTable secondTable = new StaticTable(1, 2);
		secondTable.setContent(0, 0, deviceDrop);
		
		page.append(secondTable);

	}
	
	@Override
	public Collection<AlarmConfiguration> getObjectsInTable(OgemaHttpRequest req) {
		Collection<AlarmConfiguration> all = super.getObjectsInTable(req);
		return deviceDrop.getFiltered(all, req);
	}
	
	@Override
	protected void addWidgetsBeforeMultiSelect(AlarmConfiguration sr,
			ObjectResourceGUIHelper<AlarmConfiguration, AlarmConfiguration> vh, String id, OgemaHttpRequest req,
			Row row, ApplicationManager appMan) {
		// TODO Auto-generated method stub
		super.addWidgetsBeforeMultiSelect(sr, vh, id, req, row, appMan);
	}
}
