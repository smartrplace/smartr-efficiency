package org.smartrplace.apps.hw.install.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.ogema.core.resourcemanager.ResourceAccess;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.template.DefaultDisplayTemplate;

public class InstallationStatusFilterDropdown extends TemplateDropdown<String> {
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

    private FILTERS installationFilterSelected = FILTERS.ALL;
    private final HashMap<String, Integer> filterModes = new HashMap<>();

    private final ResourceAccess resAcc;
    private final HardwareInstallController controller;

    public InstallationStatusFilterDropdown(WidgetPage<?> page, String id, HardwareInstallController controller) {
        super(page, id);

        this.controller = controller;
        this.resAcc = controller.appMan.getResourceAccess();
        setTemplate(new DefaultDisplayTemplate<String>() {
            @Override
            public String getLabel(String arg0, OgemaLocale arg1) {
                try {
                    return FILTERS.valueOf(arg0).id;
                } catch (Exception e) {
                    return "";
                }
            }
        });
    }

    @Override
    public void onGET(OgemaHttpRequest req) {
        //List<Room> rooms = resAcc.getResources(Room.class);
        List<String> items = new ArrayList<>();
        for (FILTERS f : FILTERS.values())
            items.add(f.name());
        update(items, req);
        selectItem(installationFilterSelected.name(), req);
    }

    @Override
    public void onPOSTComplete(String data, OgemaHttpRequest req) {
        String item = getSelectedItem(req);
        FILTERS newFilter = getFilterMode(item);
        if(newFilter != null)
        	installationFilterSelected = newFilter;
        //controller.appConfigData.installationStatusFilter().setValue(item);
    }

    public FILTERS getFilterMode(String name) {
    	for(FILTERS f:FILTERS.values()) {
    		if(f.name().equals(name))
    			return f;
    	}
    	return null;
    }
    
    public FILTERS getFilterMode() {
        return installationFilterSelected;
    	/*String filter = controller.appConfigData.installationStatusFilter().getValue();
        try {
            return FILTERS.valueOf(filter);
        } catch (IllegalArgumentException e) {
            return null;
        }*/
    }

    /**
     * Return devices from devices that match the filter.
     * @param devices
     * @return
     */
    public List<InstallAppDevice> getDevicesSelected(List<InstallAppDevice> devices) {
        FILTERS filter = getFilterMode();
        if (filter == FILTERS.ALL || filter == null)
            return devices;
        List<InstallAppDevice> devicesSelected = new ArrayList<>();
        for(InstallAppDevice dev: devices) {
            if (matches(dev, filter))
                devicesSelected.add(dev);
        }
        return devicesSelected;
    }

    private static boolean matches(InstallAppDevice dev, FILTERS filter) {
        switch(filter) {
            case SN_RECORDED:
            case PACKAGED:
            case INSTALLED:
                return dev.installationStatus().getValue() >= filter.statusCode;
            case NOT_PACKAGED:
            case NOT_INSTALLED:
            case SN_NOT_RECORDED:
                return dev.installationStatus().getValue() < filter.statusCode;
        }
        return false;
    }
}

