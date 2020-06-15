package org.smartrplace.app.monbase.resourceedit;

import java.util.Arrays;
import java.util.Collection;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.devicefinder.api.DatapointInfo;
import org.ogema.devicefinder.api.DatapointInfo.UtilityType;
import org.ogema.timeseries.eval.simple.api.KPIResourceAccess;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Header;

public class UtilityDataPage extends ObjectGUITablePage<UtilityType, FloatResource> {

	public UtilityDataPage(WidgetPage<?> page, ApplicationManager appMan) {
		super(page, appMan, UtilityType.UNKNOWN, false);
		triggerPageBuild();
	}

	/** Override if necessary*/
	protected UtilityType[] getTypes(OgemaHttpRequest req) {
		return DatapointInfo.defaultSRCTypes;		
	}
	
	@Override
	public void addWidgets(UtilityType object, ObjectResourceGUIHelper<UtilityType, FloatResource> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		vh.stringLabel("Name", id, DatapointInfo.getDefaultShortLabel(object, req!=null?req.getLocale():null), row);
		vh.stringLabel("Unit", id, DatapointInfo.getDefaultUnit(object), row);
		if(req == null) {
			vh.registerHeaderEntry("Price per Unit");
			vh.registerHeaderEntry("Efficiency (%)");
			return;
		}
		FloatResource priceRes = KPIResourceAccess.getDefaultPriceResource(object, appMan);
		if(priceRes != null) {
			vh.floatEdit("Price per Unit", id, priceRes, row, alert, 0, Float.MAX_VALUE, "Price cannot be negative!");
			FloatResource effRes = KPIResourceAccess.getDefaultEfficiencyResource(object, appMan);
			if(effRes != null)
				vh.floatEdit("Efficiency (%)", id, effRes, row, alert, 0, 120, "Efficiency must be between 0 and 120%!");
		}
	}

	@Override
	public FloatResource getResource(UtilityType object, OgemaHttpRequest req) {
		return KPIResourceAccess.getDefaultPriceResource(object, appMan);
	}

	@Override
	public void addWidgetsAboveTable() {
		Header header = new Header(page, "headerUtilPage", "Utility Type Pricing");
		page.append(header);
	}

	@Override
	public Collection<UtilityType> getObjectsInTable(OgemaHttpRequest req) {
		return Arrays.asList(getTypes(req));
	}

}
