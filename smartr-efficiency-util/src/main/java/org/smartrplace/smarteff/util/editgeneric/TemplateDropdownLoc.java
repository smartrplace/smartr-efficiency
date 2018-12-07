package org.smartrplace.smarteff.util.editgeneric;

import java.util.Objects;

import org.json.JSONObject;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.dropdown.TemplateDropdownData;
import de.iwes.widgets.template.DisplayTemplate;

public class TemplateDropdownLoc<T> extends TemplateDropdown<T> {
	private static final long serialVersionUID = 1L;
	
	/** If templateFlexible is true, getLabel has to be overwritten*/
	protected final boolean templateFlexible;
	protected String getFlexLabel(T object, OgemaHttpRequest req) {return null;}
	
	public TemplateDropdownLoc(WidgetPage<?> page, String id) {
		this(page, id, false);
	}
	public TemplateDropdownLoc(WidgetPage<?> page, String id, boolean templateFlexible) {
		super(page, id);
		this.templateFlexible = templateFlexible;
	}
	
	@Override
	public TemplateDropdownData<T> createNewSession() {
		return new TemplateDropdownLocData<>(this);
	}
	
	@Override
	public void onGET(OgemaHttpRequest req) {
		TemplateDropdownLocData<T> data = (TemplateDropdownLocData<T>) getData(req);
		data.lastReq = req;
	}
	
	public static class TemplateDropdownLocData<T> extends TemplateDropdownData<T> {
		public OgemaHttpRequest lastReq;
		//public OgemaLocale locale; // = OgemaLocale.GERMAN;
		public OgemaLocale localePrev = null;

		public TemplateDropdownLocData(TemplateDropdown<T> dropdown) {
			super(dropdown);
		}
		
		@Override
		public JSONObject retrieveGETData(OgemaHttpRequest req) {
			OgemaLocale newLocale = req.getLocale();
			if(localePrev == null || (localePrev != newLocale)) {
				lastReq = req;
				options.clear();
				update(getItems(), getSelectedItem());
				localePrev = newLocale;
			}
			return super.retrieveGETData(req);
		}
		
		/*protected String[] getValueAndLabel(T item) {
			if(((TemplateDropdownLoc<T>) widget).templateFlexible)
				throw new IllegalStateException("For flexible template need OgemaHttpRequest");
			return getValueAndLabel(item, null);
		}*/
		@Override
		@SuppressWarnings("unchecked")
		protected String[] getValueAndLabel(T item) {
			DisplayTemplate<T> template = ((TemplateDropdownLoc<T>) widget).template;
			OgemaLocale locale;
			if(lastReq == null) {
				lastReq = getInitialRequest();
				locale = (lastReq != null ? lastReq.getLocale() : OgemaLocale.ENGLISH);
			} else locale = lastReq.getLocale();
			String label;
			if(((TemplateDropdownLoc<T>) widget).templateFlexible) {
				label = ((TemplateDropdownLoc<T>) widget).getFlexLabel(item, lastReq);
			} else label = template.getLabel(item, locale); // XXX
			String value = template.getId(item);
			Objects.requireNonNull(label);
			Objects.requireNonNull(value);
			return new String[]{value, label};
		}

	}
	
}
