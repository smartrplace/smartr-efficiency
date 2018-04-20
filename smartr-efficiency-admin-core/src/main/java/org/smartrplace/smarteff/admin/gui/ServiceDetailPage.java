package org.smartrplace.smarteff.admin.gui;

import java.util.Collection;

import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.extensionservice.ExtensionCapability;
import org.smartrplace.smarteff.admin.SpEffAdminController;
import org.smartrplace.smarteff.util.SPPageUtil;

import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.TemplateInitSingleEmpty;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.template.DisplayTemplate;

public class ServiceDetailPage {
	private final SpEffAdminController controller;
	final TemplateDropdown<SmartEffExtensionService> selectProvider;
	
	public ServiceDetailPage(final WidgetPage<?> page, final SpEffAdminController app) {
		this.controller = app;
		
		TemplateInitSingleEmpty<SmartEffExtensionService> init = new TemplateInitSingleEmpty<SmartEffExtensionService>(page, "init", false) {
			private static final long serialVersionUID = 1L;

			@Override
			protected SmartEffExtensionService getItemById(String configId) {
				for(SmartEffExtensionService eval: controller.serviceAccess.getEvaluations().values()) {
					if(SPPageUtil.buildId(eval).equals(configId)) return eval;
				}
				return null;
			}
			@Override
			public void updateDependentWidgets(OgemaHttpRequest req) {
				Collection<SmartEffExtensionService> items = controller.serviceAccess.getEvaluations().values();
				selectProvider.update(items , req);
				SmartEffExtensionService eval = getSelectedItem(req);
				selectProvider.selectItem(eval, req);
			}
		};
		page.append(init);
		
		Header header = new Header(page, "header", "Multi-Service Detail Page");
		header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_LEFT);
		page.append(header);
		
		// provider selection
		selectProvider = new  TemplateDropdown<SmartEffExtensionService>(page, "selectProvider");
		selectProvider.setTemplate(new DisplayTemplate<SmartEffExtensionService>() {
			@Override
			public String getId(SmartEffExtensionService object) {
				return SPPageUtil.buildId(object);
			}

			@Override
			public String getLabel(SmartEffExtensionService object, OgemaLocale locale) {
				return getId(object);
			}
			
		});
		
		final Label capabilitiesLabel = new Label(page, "capabilitiesLabel") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				SmartEffExtensionService selectedProvider = selectProvider.getSelectedItem(req);

				String caps = null;
				for(ExtensionCapability c: selectedProvider.getCapabilities()) {
					if(caps != null)
						caps += "; "+c.description(req.getLocale());
					else
						caps = c.description(req.getLocale());
				}
				
				setText(caps, req);
			}
			
		};
		
		selectProvider.triggerAction(capabilitiesLabel, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	
		StaticTable table1 = new StaticTable(2, 3);
		page.append(table1);
		table1.setContent(0, 0, "Name/ID");
		table1.setContent(0, 1, selectProvider);
		table1.setContent(0, 2, " ");
		
		table1.setContent(1, 0, "Capabilities");
		table1.setContent(1, 1, capabilitiesLabel);
		table1.setContent(1, 2, "       ");
	}
}
