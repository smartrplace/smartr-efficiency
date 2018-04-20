package org.smartrplace.smarteff.admin.gui;

import java.util.Collection;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.admin.SpEffAdminController;
import org.smartrplace.smarteff.admin.object.SmartrEffExtResourceTypeData;
import org.smartrplace.smarteff.admin.util.SmartrEffUtil;
import org.smartrplace.smarteff.admin.util.SmartrEffUtil.AccessType;
import org.smartrplace.util.directresourcegui.ResourceGUIHelper;
import org.smartrplace.util.directresourcegui.ResourceGUITablePage;

import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.TemplateInitSingleEmpty;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.resource.widget.textfield.ValueResourceTextField;
import de.iwes.widgets.template.DefaultDisplayTemplate;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

/**
 * Page that shows all resources of a certain type
 * TODO: Inherit from NaviPageBase and use same addWidgets method as ResourceTablePage and ResourceAllTablePage.
 * TODO: For this probably for the transmission of the resource type not configId can be used but a
 * different HTML parameter has to be used. This could be a relevant general concept if a NaviPage needs
 * more input than just input resources. This could also be solved by a parameters Map accessible in 
 * {@link ExtensionResourceAccessInitData}.
 */
@Deprecated
public class DataExplorerPage extends ResourceGUITablePage<SmartEffResource> {
	protected static final String pid = DataExplorerPage.class.getSimpleName();

	public static final float MIN_COMFORT_TEMP = 4;
	public static final float MAX_COMFORT_TEMP = 30;
	public static final float DEFAULT_COMFORT_TEMP = 21;
	
	private final SpEffAdminController app;
	
	ValueResourceTextField<TimeResource> updateInterval;
	private TemplateDropdown<SmartrEffExtResourceTypeData> selectProvider;

	public DataExplorerPage(final WidgetPage<?> page, final SpEffAdminController app,
			SmartEffResource initData) {
		//super(page, app.appMan, initData);
		super(page, app.appMan, SmartEffResource.class);
		this.app = app;
	}
	
	private <T extends Resource> List<T> getNonEditResourcesToAccess(Class<T> type, SmartEffUserDataNonEdit userData) {
		List<T> result = userData.getSubResources(type, true);
		return result ;
	}
	private <T extends Resource> List<T> getEditableResourcesToAccess(Class<T> resType, SmartEffUserDataNonEdit userData) {
		List<T> result = userData.editableData().getSubResources(resType, true);
		return result ;
	}
	private <T extends Resource> List<T> getPublicResources(Class<T> type) {
		List<T> result = app.getUserAdmin().getAppConfigData().globalData().getSubResources(type, true);
		return result ;
	}
	private <T extends Resource> List<T> getAllResourcesToAccess(Class<T> resType, SmartEffUserDataNonEdit userData) {
		List<T> result = getEditableResourcesToAccess(resType, userData);
		result.addAll(getNonEditResourcesToAccess(resType, userData));
		result.addAll(getPublicResources(resType));
		return result ;
	}
	
	@Override
	public void addWidgetsAboveTable() {
		TemplateInitSingleEmpty<SmartrEffExtResourceTypeData> initResType = new TemplateInitSingleEmpty<SmartrEffExtResourceTypeData>(page, "initResType", false) {
			private static final long serialVersionUID = 1L;

			@Override
			protected SmartrEffExtResourceTypeData getItemById(String configId) {
				for(SmartrEffExtResourceTypeData eval: app.typeAdmin.resourceTypes.values()) {
					if(ResourceUtils.getValidResourceName(eval.resType.getName()).equals(configId)) return eval;
				}
				return null;
			}
			@Override
			public void init(OgemaHttpRequest req) {
				super.init(req);
				Collection<SmartrEffExtResourceTypeData> items = app.typeAdmin.resourceTypes.values();
				selectProvider.update(items , req);
				SmartrEffExtResourceTypeData eval = getSelectedItem(req);
				selectProvider.selectItem(eval, req);
				System.out.println("Data Explorer: Finished init");
			}
		};
		page.append(initResType);
		
		Header header = new Header(page, "header", "Data Explorer");
		header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_LEFT);
		page.append(header);
		
		selectProvider = new  TemplateDropdown<SmartrEffExtResourceTypeData>(page, "selectProvider");
		selectProvider.setTemplate(new DefaultDisplayTemplate<SmartrEffExtResourceTypeData>() {
			@Override
			public String getLabel(SmartrEffExtResourceTypeData object, OgemaLocale locale) {
				return object.typeDeclaration.label(locale);
			}
			
		});
		page.append(selectProvider);
		//Note: Synchronization issues with triggerAction
		//init.triggerOnPOST(selectProvider);
		//selectProvider.triggerOnPOST(mainTable);
		initResType.registerDependentWidget(selectProvider);
		selectProvider.registerDependentWidget(mainTable);
	}
	
	@Override
	public List<SmartEffResource> getResourcesInTable(OgemaHttpRequest req) {
		SmartrEffExtResourceTypeData item = selectProvider.getSelectedItem(req);
		if(item == null) throw new IllegalStateException("Widget dependencies not processed correctly!");
		System.out.println("Item:"+item.resType.getName());
		List<? extends Resource> list1 = getAllResourcesToAccess(item.resType, app.getUserAdmin().getUserData());
		@SuppressWarnings("unchecked")
		List<SmartEffResource> result = (List<SmartEffResource>)list1 ; 
		return result;
	}

	@Override
	public void addWidgets(SmartEffResource object, ResourceGUIHelper<SmartEffResource> vh,
			String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		//if(configRes != null) try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
		//ResourceTablePage.addWidgets(object, vh, id, req, row, appMan, pid, app.appManExt, exPage);
		id = pid + id;
		vh.stringLabel("Name", id, ResourceUtils.getHumanReadableName(object), row);
		vh.stringLabel("Elements", id, ""+object.getSubResources(false).size(), row);
		vh.linkingButton("Export", id, object, row, "Export", "export.html");
		vh.linkingButton("View", id, object, row, "Export", "view.html");
		vh.linkingButton("Evaluate", id, object, row, "Export", "evaluate.html");
		if((!(SmartrEffUtil.getAccessType(object) == AccessType.READWRITE)) || (req == null)) {
			vh.stringLabel("Edit", id, "--", row);
			vh.stringLabel("Delete", id, "--", row);
		} else {
			vh.linkingButton("Edit", id, object, row, "", "edit.html");
			Button deleteButton = new Button(vh.getParent(), "Delete"+pid, "Delete", req) {
				private static final long serialVersionUID = -6168031482180238199L;
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					app.removeResource(object);
				}
			};
			row.addCell("Delete", deleteButton);
		}
	}
	
	/*@Override
	public SmartEffResource getResource(SmartEffResource object, OgemaHttpRequest req) {
		return null;
	}*/
	
	/*@Override
	public String getLineId(SmartEffResource object) {
		String name = WidgetHelper.getValidWidgetId(ResourceUtils.getHumanReadableName(object));
		return name + super.getLineId(object);
	}*/
}
