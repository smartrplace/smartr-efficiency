package org.smartrplace.smarteff.admin.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.ExtensionUserDataNonEdit;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.util.directresourcegui.ResourceEditPage;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.button.TemplateInitSingleEmpty;
import de.iwes.widgets.html.form.button.TemplateRedirectButton;
/**
 * Frame for navigation pages
 */
public abstract class ExtensionNavigationPage<T extends ExtensionUserDataNonEdit, C extends ExtensionResourceAccessInitData> 
		implements ExtensionNavigationPageI<T, C>{
	protected abstract List<T> getUsers(OgemaHttpRequest req);
	
	public final WidgetPage<?> page;
	public final String url;
	public final String overviewUrl;
	
	public final TemplateInitSingleEmpty<C> init;
	protected void init(OgemaHttpRequest req) {};
	protected abstract  C getItemById(String configId, OgemaHttpRequest req);
	private final List<InitListener> initListeners = new ArrayList<>();
	
	public ExtensionNavigationPage(final WidgetPage<?> page, String url, String overviewUrl,
			String providerId) {
		this.page = page;
		this.url = url;
		this.overviewUrl = overviewUrl;
		init = new TemplateInitSingleEmpty<C>(page, "init"+providerId, false) {
			private static final long serialVersionUID = 3798965126759319288L;
			
			@Override
			public void init(OgemaHttpRequest req) {
				Map<String,String[]> params = getPage().getPageParameters(req);
				C res = null;
				if (params == null || params.isEmpty())
					res = ExtensionNavigationPage.this.getItemById(null, req);
				else {
					String[] patterns = params.get(TemplateRedirectButton.PAGE_CONFIG_PARAMETER);
					if (patterns == null || patterns.length == 0)
						res = ExtensionNavigationPage.this.getItemById(null, req);
					else {
						final String selected = patterns[0];
						try {
							res = ExtensionNavigationPage.this.getItemById(selected, req); // may return null or throw an exception
						} catch (Exception e) { // if the type does not match
							LoggerFactory.getLogger(TemplateInitSingleEmpty.class).info("Empty template widget could not be initialized with the selected value {}",selected,e);
						}
					}
				}
				if (res == null) {
					for(InitListener il: initListeners) il.onInitComplete(req);
					return;
				}
				getData(req).selectItem(res);
				ExtensionNavigationPage.this.init(req);
				for(InitListener il: initListeners) il.onInitComplete(req);
			}
			@Override
			protected C getItemById(String configId) {
				throw new IllegalStateException("Standard getItemById method replaced to get request !!!");
			}
		};
		page.append(init);
	}
	
	public void finalize(StaticTable table) {
		String mainUrl = overviewUrl;
		if(mainUrl != null) {
			RedirectButton mainPageBut = new RedirectButton(page, "mainPageBut", "Main page",
					mainUrl);
			page.append(mainPageBut);
		}
	}
	
	@Override
	public WidgetPage<?> getPage() {
		return page;
	}
	
	@Override
	public void registerInitExtension(InitListener initListener) {
		initListeners.add(initListener);
	}

	@Override
	public void registerDependentWidgetOnInit(OgemaWidget widget) {
		init.registerDependentWidget(widget);
	}
	
	@Override
	public void registerAppTableWidgetsDependentOnInit(StaticTable table) {
		ResourceEditPage.registerDependentWidgets(init, table);
	}
	
	@Override
	public ExtensionResourceAccessInitData getAccessData(OgemaHttpRequest req) {
		return init.getSelectedItem(req);
	}
}
