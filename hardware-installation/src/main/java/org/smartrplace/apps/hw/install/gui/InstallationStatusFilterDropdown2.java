package org.smartrplace.apps.hw.install.gui;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.internationalization.util.LocaleHelper;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gui.filtering.GenericFilterBase;
import org.smartrplace.gui.filtering.GenericFilterOption;
import org.smartrplace.gui.filtering.SingleFiltering;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class InstallationStatusFilterDropdown2 extends SingleFiltering<Integer, InstallAppDevice> {
	private static final long serialVersionUID = 1L;

    public enum FILTERS {
        /** See {@link InstallAppDevice} for all status codes */
        ALL(null, "All", "Show all devices"),
        SN_RECORDED(1, "Serial number recorded"),
        SN_NOT_RECORDED(1, "Serial number not recorded"),
        PACKAGED(3, "Packaged for shipping"),
        NOT_PACKAGED(3, "Not packaged for shipping"),
        INSTALLED(10, "Installed"),
        NOT_INSTALLED(10, "Not installed");

        public final String description;
        /** Relevant status code */
        public final Integer statusCode;
        public final String id;

        private FILTERS(Integer s, String n) {
            this(s, n, n);
        }
        private FILTERS(Integer s, String n, String d) {
            this.statusCode = s;
            this.id = n;
            this.description = d;
        }
    }

    //private FILTERS installationFilterSelected = FILTERS.ALL;
    protected final ApplicationManager appMan;
    
    public InstallationStatusFilterDropdown2(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode,
    		ApplicationManager appMan) {
		super(page, id, saveOptionMode, 10000, true);
		this.appMan = appMan;
	}

	@Override
	protected boolean isAttributeSinglePerDestinationObject() {
		return true;
	}

	@Override
	protected Integer getAttribute(InstallAppDevice object) {
		return object.installationStatus().getValue();
	}
	
	@Override
	protected List<GenericFilterOption<Integer>> getOptionsDynamic(OgemaHttpRequest req) {
        List<GenericFilterOption<Integer>> items = new ArrayList<>();
        for (final FILTERS f : FILTERS.values()) {
            GenericFilterOption<Integer> genOption = new GenericFilterBase<Integer>(LocaleHelper.getLabelMap(f.description)) {

				@Override
				public boolean isInSelection(Integer object, OgemaHttpRequest req) {
					return matches(object, f);
				}
			};
			items.add(genOption );
        }
        return items;
	}
	
	@Override
	protected long getFrameworkTime() {
		return appMan.getFrameworkTime();
	}
	
    private static boolean matches(int status, FILTERS filter) {
        switch(filter) {
        case ALL:
        	return true;
        case SN_RECORDED:
        case PACKAGED:
        case INSTALLED:
            return status >= filter.statusCode;
        case NOT_PACKAGED:
        case NOT_INSTALLED:
        case SN_NOT_RECORDED:
            return status < filter.statusCode;
		default:
	        throw new IllegalStateException("Unknolwn Filter option:"+filter);
        }
    }
}
