package org.smartrplace.extensionservice.gui;

import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.smartrplace.util.directresourcegui.LabelProvider;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.button.TemplateRedirectButton;
import de.iwes.widgets.resource.widget.dropdown.ResourceDropdown;
import de.iwes.widgets.resource.widget.init.ResourceInitSingleEmpty;
import de.iwes.widgets.template.DisplayTemplate;
/**
 * An HTML page, generated from the Java code.
 * TODO: The Dropdown should not be mandatory part of the page. This could be replaced by other
 * visual elements in the future.
 */
@Deprecated
public abstract class ExtensionResourceEditPage<T extends Resource> {
	protected abstract List<T> getOptionItems(OgemaHttpRequest req);
	
	public final long UPDATE_RATE = 5*1000;

	public final WidgetPage<?> page;
	public final ResourceInitSingleEmpty<T> init;
	public final ResourceDropdown<T> drop;

	/**Overwrite this to provide different url or set to null to avoid having the "Main Page" button*/
	protected String getOverviewPageUrl() {
		return "index.html";
	}
	
	public ExtensionResourceEditPage(final WidgetPage<?> page,
			final Class<T> resourceType, final LabelProvider<T> dropLabels) {
		this.page = page;
		init = new ResourceInitSingleEmpty<T>(page, "init", true, null) {
			private static final long serialVersionUID = 1L;
			@Override
			public T getSelectedItem(OgemaHttpRequest req) {
				T res = super.getSelectedItem(req);
				if(res!= null) return res;
				return drop.getSelectedItem(req);
			}
			@Override
			public void init(OgemaHttpRequest req) {
				Map<String,String[]> params = getPage().getPageParameters(req);
				if (params == null || params.isEmpty())
					return;
				String[] patterns = params.get(TemplateRedirectButton.PAGE_CONFIG_PARAMETER);
				if (patterns == null || patterns.length == 0)
					return;
//				final String selected = patterns[0].replace('_', '/');
				final String selected = patterns[0];
				T res = null;
				
				for(T el: getOptionItems(req)) {
					if(el.getLocation().equals(selected)) {
						res = el;
						break;
					}
				}
				if (res == null)
					return;
				getData(req).selectItem(res);
			}
		};
		
		drop = new ResourceDropdown<T>(page, "drop") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				init.selectItem(getSelectedItem(req), req);
			}
		};
		drop.setTemplate(new DisplayTemplate<T>() {
			@Override
			public String getLabel(T object, OgemaLocale locale) {
				return dropLabels.getLabel(object);
			}
			
			@Override
			public String getId(T object) {
				return object.getLocation();
			}
		});
		init.registerDependentWidget(drop);
		//page.append(drop);
		
	}
	
	public void finalize(StaticTable table) {
		if(table != null) registerDependentWidgets(drop, table);
		String mainUrl = getOverviewPageUrl();
		if(mainUrl != null) {
			RedirectButton mainPageBut = new RedirectButton(page, "mainPageBut", "Main page",
					mainUrl);
			page.append(mainPageBut);
		}
	}
	public static void registerDependentWidgets(OgemaWidget governor, StaticTable table) {
		for(OgemaWidget el: table.getSubWidgets()) {
			governor.triggerOnPOST(el);
		}
	}
}
