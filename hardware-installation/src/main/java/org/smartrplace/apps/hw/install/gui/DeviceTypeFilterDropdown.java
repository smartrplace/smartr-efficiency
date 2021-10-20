package org.smartrplace.apps.hw.install.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.internationalization.util.LocaleHelper;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gui.filtering.GenericFilterBase;
import org.smartrplace.gui.filtering.GenericFilterOption;
import org.smartrplace.gui.filtering.SingleFilteringDirect;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class DeviceTypeFilterDropdown extends SingleFilteringDirect<InstallAppDevice> { //SingleFiltering<Integer, InstallAppDevice> {
	private static final long serialVersionUID = 1L;

    //private FILTERS installationFilterSelected = FILTERS.ALL;
    protected final ApplicationManager appMan;
    protected final DatapointService dpService;
    
    public DeviceTypeFilterDropdown(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode,
    		ApplicationManager appMan,  DatapointService dpService) {
		super(page, id, saveOptionMode, 10000, true);
		this.appMan = appMan;
		this.dpService = dpService;
	}

	@Override
	protected boolean isAttributeSinglePerDestinationObject() {
		return true;
	}

	@Override
	protected List<GenericFilterOption<InstallAppDevice>> getOptionsDynamic(OgemaHttpRequest req) {
        List<GenericFilterOption<InstallAppDevice>> items = new ArrayList<>();
        Collection<InstallAppDevice> allDevs = dpService.managedDeviceResoures(null);
        Set<String> knownDevHandIds = new HashSet<>();
        List<DeviceHandlerProviderDP<?>> types = new ArrayList<>();
 		for(InstallAppDevice iad: allDevs) {
        	String typeStr = iad.devHandlerInfo().getValue();
 			if(!knownDevHandIds.contains(typeStr)) {
 				knownDevHandIds.add(typeStr);
 				DeviceHandlerProviderDP<Resource> devHand = dpService.getDeviceHandlerProvider(iad);
 				if(devHand == null)
 					continue;
 				types.add(devHand);
 			}
        }
        for (final DeviceHandlerProviderDP<?> type : types) {
            GenericFilterOption<InstallAppDevice> genOption = new GenericFilterBase<InstallAppDevice>(LocaleHelper.getLabelMap(type.getTableTitle())) {

				@Override
				public boolean isInSelection(InstallAppDevice object, OgemaHttpRequest req) {
					return object.devHandlerInfo().getValue().equals(type.id());
				}
			};
			items.add(genOption);
        }
        return items;
	}
	
	@Override
	protected long getFrameworkTime() {
		return appMan.getFrameworkTime();
	}
}
