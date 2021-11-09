package org.smartrplace.external.accessadmin.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.accessadmin.api.SubcustomerUtil;
import org.ogema.accessadmin.api.SubcustomerUtil.SubCustomer;
import org.ogema.accessadmin.api.SubcustomerUtil.SubCustomerType;
import org.ogema.core.application.ApplicationManager;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.external.accessadmin.config.SubCustomerData;
import org.smartrplace.gui.tablepages.ObjectGUITablePageNamed;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.directresourcegui.GUIHelperExtension;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;

@SuppressWarnings("serial")
public class SubcustomerSetupPage extends ObjectGUITablePageNamed<SubCustomer, SubCustomerData> {
	protected final AccessAdminController controller;
	
	public SubcustomerSetupPage(WidgetPage<?> page, AccessAdminController controller) {
		super(page, controller.appMan, null);
		this.controller = controller;
		triggerPageBuild();
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return "Subcustomer Configuration";
	}
	
	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "Subcustomer";
	}

	@Override
	protected String getLabel(SubCustomer obj, OgemaHttpRequest req) {
		return ResourceUtils.getHumanReadableName(obj.res);
	}

	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		StaticTable topTable = new StaticTable(2, 5);
		
		Button addGroup = new Button(page, "addGroup", "Add Subcustomer") {

			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				SubcustomerUtil.addSubcustomer(10, "New Subcustomer", Collections.emptyList(), appMan);
				/*SubCustomerData grp = ResourceListHelper.createNewNamedElement(
						controller.appConfigData.subCustomers(),
						"New Subcustomer", false);
				grp.activate(true);*/
			}
		};
		addGroup.registerDependentWidget(mainTable);

		topTable.setContent(1, 1, addGroup); //.setContent(1, 2, userAdminLink);
		page.append(topTable);

		/*Alert info = new Alert(page, "description","Explanation") {

			@Override
	    	public void onGET(OgemaHttpRequest req) {
	    		String text = "Change the label of room attributes here. The mapping of individual rooms to the attributes can be set on the page "
	    				+ "<a href=\"" + ROOM_GROUP_MAPPING_LINK + "\"><b>Room Configuration</b></a>.";
				setHtml(text, req);
	    		allowDismiss(true, req);
	    		autoDismiss(-1, req);
	    	}
	    	
	    };
	    info.addDefaultStyle(AlertData.BOOTSTRAP_INFO);
	    info.setDefaultVisibility(true);
	    page.append(info);*/
	}
	
	@Override
	protected void addNameLabel(SubCustomer object,
			ObjectResourceGUIHelper<SubCustomer, SubCustomerData> vh, String id, Row row,
			OgemaHttpRequest req) {
		vh.valueEdit(getTypeName(null), id, object.res.name(), row, alert);
	}

	@Override
	public Collection<SubCustomer> getObjectsInTable(OgemaHttpRequest req) {
		List<SubCustomerData> all = controller.appConfigData.subCustomers().getAllElements();
		
		List<SubCustomer> result = new ArrayList<>();
		for(SubCustomerData subc: all) {
			SubCustomer data = SubcustomerUtil.getFullObject(subc);
			result.add(data);
		}
		
		return result;
	}

	@Override
	public void addWidgets(SubCustomer object, ObjectResourceGUIHelper<SubCustomer, SubCustomerData> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		if(req == null) {
			vh.registerHeaderEntry(getTypeName(null));
			vh.registerHeaderEntry("Type");
			vh.registerHeaderEntry("Delete");
			return;
		}
		addNameLabel(object, vh, id, row, req);
		Map<String, String> valuesToSet = new HashMap<>();
		for(Entry<Integer, SubCustomerType> type: SubcustomerUtil.subCustomerTypes.entrySet()) {
			valuesToSet.put(""+type.getKey(), type.getValue().labelReq(req));
		}
		vh.dropdown("Type", id, object.res.subCustomerType(), row, valuesToSet);
		GUIHelperExtension.addDeleteButton(null, object.res, mainTable, id, alert, row,
				vh, req);
	}
	
	@Override
	public String getLineId(SubCustomer object) {
		return ResourceUtils.getHumanReadableName(object.res)+super.getLineId(object);
	}
}
