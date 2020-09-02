package org.smartrplace.apps.hw.install.gui.expert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.OGEMADriverPropertyService;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.internationalization.util.LocaleHelper;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.gui.filtering.GenericFilterFixed;
import org.smartrplace.gui.filtering.GenericFilterOption;
import org.smartrplace.gui.filtering.SingleFiltering.OptionSavingMode;
import org.smartrplace.gui.filtering.SingleFilteringDirect;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.label.Header;

@SuppressWarnings("serial")
public class DriverPropertyPage extends ObjectGUITablePage<InstallAppDevice, InstallAppDevice> {
	protected HardwareInstallController controller;

	public DriverPropertyPage(WidgetPage<?> page, final HardwareInstallController controller) {
		super(page, controller.appMan, InstallAppDevice.class, false);
		this.controller = controller;
		triggerPageBuild();
	}

	@Override
	public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
			String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		final PhysicalElement device2 = BatteryPage.addNameWidgetStatic(object, vh, id, req, row, controller.appManPlus);
		Room deviceRoom = device2.location().room();
		DeviceTableRaw.addRoomWidgetStatic(object, vh, id, req, row, appMan, deviceRoom);
		DeviceTableRaw.addSubLocationStatic(object, vh, id, req, row, appMan, deviceRoom, alert);
		Button showPropertiesPageButton = new Button(mainTable, "showPropertiesPageButton"+id, req) {
			
		};
		row.addCell("Properties", showPropertiesPageButton);
	}

	@Override
	public InstallAppDevice getResource(InstallAppDevice object, OgemaHttpRequest req) {
		return object;
	}

	@Override
	public void addWidgetsAboveTable() {
		Header header = new Header(page, "headDrivProp", "Driver Property Overview");
		page.append(header);
		SingleFilteringDirect<OGEMADriverPropertyService<?>> serviceFilter = new SingleFilteringDirect<OGEMADriverPropertyService<?>>(
				page, "serviceFilter", OptionSavingMode.GENERAL, 10000, false) {

			@Override
			protected List<GenericFilterOption<OGEMADriverPropertyService<?>>> getOptionsDynamic(OgemaHttpRequest req) {
				List<GenericFilterOption<OGEMADriverPropertyService<?>>> result = new ArrayList<>();
				for(OGEMADriverPropertyService<?> serv: controller.hwInstApp.getDPropertyProviders().values()) {
					Map<OgemaLocale, String> optionLabel = LocaleHelper.getLabelMap(serv.label(req.getLocale()));
					result.add(new GenericFilterFixed<OGEMADriverPropertyService<?>>(serv, optionLabel));
				}
				return result ;
			}
			
		};
		page.append(serviceFilter);
	}

	@Override
	public Collection<InstallAppDevice> getObjectsInTable(OgemaHttpRequest req) {
		Collection<InstallAppDevice> result = MainPage.getDevicesSelectedDefault(null, controller, null, null);
		return result ;
	}

}
