package org.sp.example.samrteff.driver.basic.gui;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.util.directresourcegui.ResourceGUIHelper;
import org.smartrplace.util.directresourcegui.ResourceGUITablePage;

import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.jaxb.GaRoMultiEvalDataProviderJAXB;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.dropdown.DropdownData;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.selectiontree.SelectionItem;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.driver.basic.BasicGaRoDataProviderConfig;

public class AdminPage extends ResourceGUITablePage<BasicGaRoDataProviderConfig> {
	/*private ApplicationManagerSPExt appManExt;
	public void setAppManExt(ApplicationManagerSPExt appManExt) {
		this.appManExt = appManExt;
	}*/
	private final GaRoMultiEvalDataProviderJAXB jaxbProvider;
	
	public AdminPage(WidgetPage<?> page, ApplicationManager appMan, GaRoMultiEvalDataProviderJAXB jaxbProvider) {
		super(page, appMan, BasicGaRoDataProviderConfig.class);
		this.jaxbProvider = jaxbProvider;
	}

	@Override
	public void addWidgets(BasicGaRoDataProviderConfig object, ResourceGUIHelper<BasicGaRoDataProviderConfig> vh,
			String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		if(req != null) {
			TemplateDropdown<BuildingData> buildingDrop = new TemplateDropdown<BuildingData>(page, "buildingDrop"+id) {
				private static final long serialVersionUID = 1L;
				public void onGET(OgemaHttpRequest req) {
					SmartEffUserDataNonEdit userNonEdit = object.getParent().getParent();
					List<BuildingData> buildings = userNonEdit.editableData().buildingData().getAllElements();
					update(buildings, req);
				};
			};
			row.addCell("Building configured");
			TemplateDropdown<SelectionItem> gwDrop = new TemplateDropdown<SelectionItem>(page, "gwDrop"+id) {
				private static final long serialVersionUID = 1L;
				@Override
				public void onGET(OgemaHttpRequest req) {
					List<SelectionItem> items = jaxbProvider.getOptions(GaRoMultiEvalDataProvider.GW_LEVEL, null);
					update(items, req);
					BuildingData bd = buildingDrop.getSelectedItem(req);
					if(bd == null) selectSingleOption(DropdownData.EMPTY_OPT_ID, req);
					else {
						String loc = bd.getLocation();
						String gwId = null;
						for(int i=0; i<object.buildingLocations().getValues().length; i++) {
							if(object.buildingLocations().getValues()[i].equals(loc)) {
								gwId = object.gwIdsAllowed().getValues()[i];
								break;
							}
						}
						if(gwId == null) selectSingleOption(DropdownData.EMPTY_OPT_ID, req);
						else for(SelectionItem item: items) {
							if(item.id().equals(gwId)) selectItem(item, req);
						}
					}
				}
			};
			gwDrop.setDefaultAddEmptyOption(true, "---");
			triggerOnPost(buildingDrop, gwDrop);
			row.addCell("Gateway Id", gwDrop);
		} else {
			vh.registerHeaderEntry("Building configured");
			vh.registerHeaderEntry("Gateway Id");
		}
	}

	@Override
	public void addWidgetsAboveTable() {
		Header header = new Header(page, "header", "Basic GaRo DataProvider Access Admin page");
		page.append(header);
	}

	@Override
	public List<BasicGaRoDataProviderConfig> getResourcesInTable(OgemaHttpRequest req) {
		List<SmartEffUserDataNonEdit> userData = appMan.getResourceAccess().getResources(SmartEffUserDataNonEdit.class);
		List<BasicGaRoDataProviderConfig> result = new ArrayList<>();
		for(SmartEffUserDataNonEdit ud: userData) {
			String name = CapabilityHelper.getSingleResourceName(BasicGaRoDataProviderConfig.class);
			result.add(ud.configurationSpace().getSubResource(name, BasicGaRoDataProviderConfig.class));
			//result.add(CapabilityHelper.getSubResourceSingle(ud.configurationSpace(), BasicGaRoDataProviderConfig.class, appManExt));
		}
		return result ;
	}
}
