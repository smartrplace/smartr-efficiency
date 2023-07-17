package org.smartrplace.apps.hw.install.gui.alarm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.SubcustomerUtil;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.external.accessadmin.config.SubCustomerData;
import org.smartrplace.external.accessadmin.config.SubCustomerSuperiorData;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Header;

public class GatewayMasterDataPage extends ObjectGUITablePage<SubCustomerSuperiorData, SubCustomerSuperiorData> {
	final boolean isEditable;
	
	public static final Map<String, String> batteryChangeLevelOptions = new HashMap<>();
	public static final Map<String, String> summerWinterModeSwitchingOptions = new HashMap<>();
	public static final Map<String, String> networkRestrictionsOptions = new HashMap<>();
	public static final Map<String, String> contactSalesBeforeInformationOptions = new HashMap<>();
	public static final Map<String, String> centralHeatingWeekendModeOptions = new HashMap<>();
	public static final Map<String, String> antiTheftTypeOptions = new HashMap<>();
	public static final Map<String, String> antiVandalismTypeOptions = new HashMap<>();
	public static final Map<String, String> adapterTypeOptions = new HashMap<>();
	
	static {
		batteryChangeLevelOptions.put("0", "unknown");
		batteryChangeLevelOptions.put("1", "customer does not change any batteries");
		batteryChangeLevelOptions.put("2", "customer changes single batteries in urgent cases");
		batteryChangeLevelOptions.put("3", "customer performs all battery changes");
		
		//TODO: ...
	}
	
	public GatewayMasterDataPage(WidgetPage<?> page, ApplicationManager appMan, boolean isEditable) {
		super(page, appMan, SubCustomerSuperiorData.class, false);
		this.isEditable = isEditable;
		triggerPageBuild();
	}

	@Override
	public void addWidgets(SubCustomerSuperiorData object,
			ObjectResourceGUIHelper<SubCustomerSuperiorData, SubCustomerSuperiorData> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		if(req == null) {
			vh.registerHeaderEntry("Name");
			vh.registerHeaderEntry("Email_Tech");
			vh.registerHeaderEntry("Anrede_Tech");
			vh.registerHeaderEntry("Telefon_Tech");
			vh.registerHeaderEntry("Email_IT");
			vh.registerHeaderEntry("Anrede_IT");
			vh.registerHeaderEntry("Telefon_IT");
			
			vh.registerHeaderEntry("BatteryChange");
			vh.registerHeaderEntry("AdaptByCust");
			vh.registerHeaderEntry("SummerWinter");
			vh.registerHeaderEntry("Network");
			vh.registerHeaderEntry("SalesFirst");
			vh.registerHeaderEntry("LoweringStart");
			vh.registerHeaderEntry("LoweringEnd");
			vh.registerHeaderEntry("LoweringWeekend");
			
			vh.registerHeaderEntry("Anti-Theft");
			vh.registerHeaderEntry("Anti-Vandlm");
			vh.registerHeaderEntry("TH-Adapter");

			return;
		}
		vh.stringLabel("Name", id, ResourceUtils.getHumanReadableShortName(object), row);
		addStringElement("Email_Tech", object.additionalAdminEmailAddresses(), vh, id, row);
		addStringElement("Anrede_Tech", object.personalSalutations(), vh, id, row);
		addStringElement("Telefon_Tech", object.phoneNumbers(), vh, id, row);
		addStringElement("Email_IT", object.emailAddressesIT(), vh, id, row);
		addStringElement("Anrede_IT", object.personalSalutationsIT(), vh, id, row);
		addStringElement("Telefon_IT", object.phoneNumbersIT(), vh, id, row);
		
		addDropdownElement("BatteryChange", object.batteryChangeLevel(), batteryChangeLevelOptions, vh, id, row);
		//TODO ...
		
		vh.booleanEdit("AdaptByCust", id, object.customerPerformsThermostatAdapt(), row);
		vh.integerEdit("LoweringStart", id, object.centralHeatingNightlyLowering(), row, alert, 0, 1440, "Lowering allowed: 0=unknown, 1=yes, 2=no, otherise minutes from start of day");
		vh.integerEdit("LoweringEnd", id, object.centralHeatingNightlyLowering(), row, alert, 0, 1440, "Lowering allowed: 0=unknown, 1=yes, 2=no, otherise minutes from start of day");
	}

	private OgemaWidget addStringElement(String label, StringResource res, ObjectResourceGUIHelper<SubCustomerSuperiorData, SubCustomerSuperiorData> vh,
			String id, Row row) {
		if(isEditable)
			return vh.stringEdit(label, id, res, row, alert);
		else
			return vh.stringLabel(label, id, res, row);
	}
	
	private OgemaWidget addDropdownElement(String label, SingleValueResource res,
			Map<String, String> options,
			ObjectResourceGUIHelper<SubCustomerSuperiorData, SubCustomerSuperiorData> vh,
			String id, Row row) {
		if(isEditable)
			return vh.dropdown(label, id, res, row, options);
		else {
			String key = ValueResourceUtils.getValue(res);
			String value = options.get(key);
			return vh.stringLabel(label, id, value, row);
		}
	}

	@Override
	public SubCustomerSuperiorData getResource(SubCustomerSuperiorData object, OgemaHttpRequest req) {
		return object;
	}

	@Override
	public void addWidgetsAboveTable() {
		Header header = new Header(page, "headMasterPage", "Gateway Contact Master Database");
		page.append(header);
	}

	@Override
	public Collection<SubCustomerSuperiorData> getObjectsInTable(OgemaHttpRequest req) {
		// TODO: Caching/Listener may be helpful to avoid scanning resource tree on each call
		// On gateways it may be easier to just search in gatewaySuperiorDataRes
		List<SubCustomerSuperiorData> result = appMan.getResourceAccess().getResources(SubCustomerSuperiorData.class);
		if(result.isEmpty() || Boolean.getBoolean("org.smartplace.app.srcmon.server.issuperior"))
			return result;
		List<SubCustomerData> local = SubcustomerUtil.getSubcustomers(appMan);
		result = new ArrayList<>();
		for(SubCustomerData subc: local) {
			result.add(SubcustomerUtil.getDatabaseData(subc, appMan));
		}
		return result;
	}

}
