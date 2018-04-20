package org.smartrplace.smarteff.admin.gui;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.smarteff.admin.SpEffAdminController;
import org.smartrplace.smarteff.util.SPPageUtil;

import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.DynamicTable;
import de.iwes.widgets.html.complextable.RowTemplate;
import de.iwes.widgets.html.form.button.TemplateRedirectButton;
import de.iwes.widgets.html.form.label.Header;


/**
 * An HTML page, generated from the Java code.
 */
public class ServicePage {
	
	private final DynamicTable<SmartEffExtensionService> table;

	public ServicePage(final WidgetPage<?> page, final SpEffAdminController app) {
		
		Header header = new Header(page, "header", "Efficiency Planning Extension Modules");
		header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_LEFT);
		page.append(header);
		
		table = new DynamicTable<SmartEffExtensionService>(page, "evalviewtable") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				Collection<SmartEffExtensionService> providers = app.serviceAccess.getEvaluations().values(); 
				updateRows(providers, req);
			}
		};
		
		table.setRowTemplate(new RowTemplate<SmartEffExtensionService>() {

			@Override
			public Row addRow(SmartEffExtensionService eval, OgemaHttpRequest req) {
				Row row = new Row();
				String lineId = getLineId(eval);
				row.addCell("name", SPPageUtil.buildId(eval));
				//row.addCell("description", eval.description(OgemaLocale.ENGLISH));
				TemplateRedirectButton<SmartEffExtensionService> detailPageButton = new TemplateRedirectButton<SmartEffExtensionService>(
						table, "detailPageButton"+lineId, "Details", "", req) {

					private static final long serialVersionUID = 1L;
					
					@Override
					public void onPrePOST(String data, OgemaHttpRequest req) {
						selectItem(eval, req);
						setUrl("Details.html", req);
					}
					@Override
					protected String getConfigId(SmartEffExtensionService object) {
						return SPPageUtil.buildId(object);
					}
				};
												
				row.addCell("detailPageButton", detailPageButton);
				
				return row;
			}

			@Override
			public String getLineId(SmartEffExtensionService object) {
				return ResourceUtils.getValidResourceName(SPPageUtil.buildId(object));
			}

			@Override
			public Map<String, Object> getHeader() {
				final Map<String, Object> header = new LinkedHashMap<>();
				header.put("name", "Name/ID");
				//header.put("description", "Description");
				header.put("detailPageButton", "Open Detail Page");
				return header;
			}
		});
		
		page.append(table).linebreak();	
	}
}