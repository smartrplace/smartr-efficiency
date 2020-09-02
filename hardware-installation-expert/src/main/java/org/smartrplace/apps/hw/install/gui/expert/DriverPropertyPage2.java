package org.smartrplace.apps.hw.install.gui.expert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.OGEMADriverPropertyService;
import org.ogema.internationalization.util.LocaleHelper;
import org.smartrplace.apps.hw.install.HardwareInstallController;
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
import de.iwes.widgets.html.form.label.Header;

@SuppressWarnings("serial")
public class DriverPropertyPage2 extends ObjectGUITablePage<String, Resource> {
	protected HardwareInstallController controller;

	public DriverPropertyPage2(WidgetPage<?> page, final HardwareInstallController controller) {
		super(page, controller.appMan, "init", false);
		this.controller = controller;
		triggerPageBuild();
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
	public void addWidgets(String object, ObjectResourceGUIHelper<String, Resource> vh, String id, OgemaHttpRequest req,
			Row row, ApplicationManager appMan) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public Resource getResource(String object, OgemaHttpRequest req) {
		return null;
	}


	@Override
	public Collection<String> getObjectsInTable(OgemaHttpRequest req) {
		// TODO Auto-generated method stub
		return null;
	}

}
