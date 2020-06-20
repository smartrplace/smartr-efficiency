package org.smartrplace.external.accessadmin.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.smartrplace.gui.filtering.ObjectGUITablePageNamed;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.multiselect.TemplateMultiselect;
import de.iwes.widgets.template.DefaultDisplayTemplate;

/** 
 * 
 * @author dnestle
 *
 * @param <T> type of columns
 * @param <G> type of groups to which T object can be added
 */
public abstract class PerMultiselectConfigPage<T, G, R extends Resource> extends ObjectGUITablePageNamed<T, R> {
	protected abstract String getGroupColumnLabel();
	protected abstract List<G> getAllGroups(OgemaHttpRequest req); 
	protected abstract List<G> getGroups(T object, OgemaHttpRequest req);
	protected abstract void setGroups(T object, List<G> groups, OgemaHttpRequest req);
	protected abstract String getGroupLabel(G object);

	//protected abstract String getGroupLabel(G group);
	//protected abstract 
	
	public PerMultiselectConfigPage(WidgetPage<?> page, ApplicationManager appMan, T initObject) {
		super(page, appMan, initObject);
		//this.controller = controller;
	}

	//abstract protected TemplateMultiselect<G> getMultiselect(T object, String lineId, OgemaHttpRequest req); 
	
	protected void addWidgetsBeforeMultiSelect(T object, ObjectResourceGUIHelper<T, R> vh, String id, OgemaHttpRequest req,
			Row row, ApplicationManager appMan) {}
	protected void addWidgetsAfterMultiSelect(T object, ObjectResourceGUIHelper<T, R> vh, String id, OgemaHttpRequest req,
			Row row, ApplicationManager appMan) {}
	
	protected Map<G, String> groupLabels = new HashMap<>();

	@Override
	public void addWidgets(T object, ObjectResourceGUIHelper<T, R> vh, String id, OgemaHttpRequest req,
			Row row, ApplicationManager appMan) {
		
		addNameLabel(object, vh, id, row);
		//vh.stringLabel("Name", id, object.userName(), row);
		if(req == null) {
			vh.registerHeaderEntry(getGroupColumnLabel());
			return;
		}
		//TemplateMultiselect<G> groupSelect = getMultiselect(object, id, req);
		TemplateMultiselect<G> groupSelect = new TemplateMultiselect<G>(mainTable, "groupSelect"+id, req) {
				private static final long serialVersionUID = 1L;
				@Override
				public void onGET(OgemaHttpRequest req) {
					List<G> selected = getGroups(object, req);
					//TODO: Maybe a caching could be implemented
					List<G> all = getAllGroups(req);
					update(all, req);
					selectItems(selected, req);
				}
				
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					List<G> selected = getSelectedItems(req);
					setGroups(object, selected , req);
				}
			};
			groupSelect.setTemplate(new DefaultDisplayTemplate<G>() {
				@Override
				public String getLabel(G object, OgemaLocale locale) {
					String result = groupLabels.get(object);
					if(result != null)
						return result;
					result = getGroupLabel(object);
					groupLabels.put(object, result);
					return result;
				}
			});
		row.addCell(WidgetHelper.getValidWidgetId(getGroupColumnLabel()), groupSelect);
		//MultiSelectExtendedStringArray<AccessConfigUser> groupSelect = new MultiSelectExtendedStringArray<AccessConfigUser>(mainTable, "groupSelect"+id,
		//		true, true, true, object.accessConfig.superGroups(), req);
	}
}
