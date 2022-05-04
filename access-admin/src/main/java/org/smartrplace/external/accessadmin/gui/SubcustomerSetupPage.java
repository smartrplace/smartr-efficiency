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
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.util.extended.eval.widget.IntegerMultiButton;
import org.ogema.util.extended.eval.widget.IntegerResourceMultiButton;
import org.smartrplace.external.accessadmin.AccessAdminController;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.external.accessadmin.config.SubCustomerData;
import org.smartrplace.gui.tablepages.ObjectGUITablePageNamed;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.directresourcegui.GUIHelperExtension;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.WidgetStyle;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;

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
			}
		};
		addGroup.registerDependentWidget(mainTable);

		@SuppressWarnings("unchecked")
		IntegerResourceMultiButton modeSelect = new IntegerResourceMultiButton(page, "modeSelect",
				controller.appConfigData.subcustomerUserMode(),
				new WidgetStyle[] {ButtonData.BOOTSTRAP_RED, ButtonData.BOOTSTRAP_GREEN}) {
			
			@Override
			protected String getText(int state, OgemaHttpRequest req) {
				if(state == 0)
					return "All users see All";
				if(state == 1);
					return "Standard user-tenant access";
			}
		};
		
		topTable.setContent(1, 1, addGroup); //.setContent(1, 2, userAdminLink);
		topTable.setContent(1,  3, modeSelect);
		page.append(topTable);
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
			vh.registerHeaderEntry("Eco mode");
			vh.registerHeaderEntry("Room#");
			vh.registerHeaderEntry("User#");
			vh.registerHeaderEntry("Agg");
			vh.registerHeaderEntry("Delete");
			return;
		}
		addNameLabel(object, vh, id, row, req);
		Map<String, String> valuesToSet = new HashMap<>();
		Integer standardType = null;
		for(Entry<Integer, SubCustomerType> type: SubcustomerUtil.subCustomerTypes.entrySet()) {
			valuesToSet.put(""+type.getKey(), type.getValue().labelReq(req));
			if(standardType == null)
				standardType = type.getKey();
		}
		int curType = object.res.subCustomerType().getValue();
		if(SubcustomerUtil.subCustomerTypes.get(curType) == null) {
			object.res.subCustomerType().setValue(standardType);
			appMan.getLogger().error("Found unknown type "+curType+" for subcustomer "+object.res.getLocation()+", setting to "+standardType);
		}
		vh.dropdown("Type", id, object.res.subCustomerType(), row, valuesToSet);
		if(object.res.aggregationType().getValue() <= 0)
			vh.booleanEdit("Eco mode", id, object.res.ecoModeActive(), row);
		else {
			BooleanResource ecoAll = appMan.getResourceAccess().getResource("smartrplaceHeatcontrolConfig/ecoModeActive");
			if(ecoAll != null)
				vh.booleanLabel("Eco mode", id, ecoAll, row, 0);
		}
		int roomNum = object.res.roomGroup().rooms().size();
		vh.stringLabel("Room#", id, ""+roomNum, row);
		List<AccessConfigUser> users = SubcustomerUtil.getAllUsersForSubcustomer(object.res, appMan);
		int userNum = users.size();
		vh.stringLabel("User#", id, ""+userNum, row);
		vh.intLabel("Agg", id, object.res.aggregationType(), row, 0);
		
		GUIHelperExtension.addDeleteButton(null, object.res, mainTable, id, alert, "Delete", row,
				vh, req);
	}
	
	@Override
	public String getLineId(SubCustomer object) {
		return ResourceUtils.getHumanReadableName(object.res)+super.getLineId(object);
	}
}
