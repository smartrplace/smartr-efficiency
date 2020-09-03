package org.smartrplace.apps.hw.install.gui.prop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.array.StringArrayResource;
import org.ogema.devicefinder.api.OGEMADriverPropertyService;
import org.ogema.devicefinder.api.OGEMADriverPropertyService.AccessAvailability;
import org.ogema.model.prototypes.PhysicalElement;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.tissue.util.resource.ValueResourceHelperSP;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.resource.widget.dropdown.ResourceDropdown;

public class PropertyPage extends ObjectGUITablePage<PropertyData, PhysicalElement> {
	public static final long UPDATE_RATE = 4000;
	private final HardwareInstallController app;
	private ResourceDropdown<Resource> deviceDrop;
	//protected ResourceInitSingleEmpty<OGEMADriverPropertyService<?>> init;
	
	protected final AggregatedDevicePropertyService propService;
	
	public PropertyPage(WidgetPage<?> page, HardwareInstallController controller) {
		super(page, controller.appMan, new PropertyData(null, null, null), false);
		this.app = controller;
		Collection<OGEMADriverPropertyService<?>> services = controller.usedServices.keySet();
		this.propService = new AggregatedDevicePropertyService(services , "AggregatedDevicePropertyService", controller.log);
		triggerPageBuild();
	}

	@Override
	public void addWidgets(final PropertyData object, ObjectResourceGUIHelper<PropertyData, PhysicalElement> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		vh.stringLabel("Name", id, object.propertyName, row);
		Label valueLabel = vh.stringLabel("Value", id, object.propertyValue, row);
		if(valueLabel != null) valueLabel.setPollingInterval(UPDATE_RATE, req);
		if(object.propertyName == null) {
			vh.registerHeaderEntry("Update");
		} else {
			Button updateButton = new Button(mainTable, "updateButton"+id, "Update", req) {
				private static final long serialVersionUID = 1L;

				public void onPrePOST(String data, OgemaHttpRequest req) {
					Resource device = deviceDrop.getSelectedItem(req);
					propService.updateProperty(device, object.propertyName, app.log);					
				}
			};
			updateButton.triggerOnPOST(valueLabel, req);
			row.addCell("Update", updateButton);
		}
		if(object.propertyName == null) {
			vh.registerHeaderEntry("Edit");
		} else {
			AccessAvailability cc = propService.getReadWriteType(null, object.propertyName);
			if(cc == AccessAvailability.WRITE) {
				TextField valueEdit = new TextField(mainTable, "valueEdit"+id, req) {
					private static final long serialVersionUID = 1L;
					
					/*@Override
					public void onGET(OgemaHttpRequest req) {
						Resource device = deviceDrop.getSelectedItem(req);
						Boolean wsuc = propService.getWriteSuccess(device, object.propertyName);
						if(wsuc != null && (!wsuc))
							disable(req);
						else enable(req);
					}*/

					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						String val = getValue(req);
						Resource device = deviceDrop.getSelectedItem(req);
						propService.writeProperty(device, object.propertyName, app.log, val);
					}
				};
				row.addCell("Edit", valueEdit);
			}
		}
	}

	@Override
	public List<PropertyData> getObjectsInTable(OgemaHttpRequest req) {
		Resource device = deviceDrop.getSelectedItem(req);
		if(device == null)
			return Collections.emptyList();
		StringArrayResource propNames = device.getSubResource("propertyNames", StringArrayResource.class);
		StringArrayResource propValues = device.getSubResource("propertyValues", StringArrayResource.class);
		if(!(propNames.isActive() && propValues.isActive())) {
			propService.updateProperties(device, app.log);
			propNames = device.getSubResource("propertyNames", StringArrayResource.class);
			propValues = device.getSubResource("propertyValues", StringArrayResource.class);
		}
		String[] names = propNames.getValues();
		String[] values = propValues.getValues();
		//TODO
		List<PropertyData> result = new ArrayList<>();
		try {
		if(names.length != values.length) {
			System.out.println("Properties:"+ValueResourceHelperSP.getAsString(names, true));
			System.out.println("Names:"+ValueResourceHelperSP.getAsString(values, true));
			throw new IllegalStateException("Property name vs length#:"+names.length+ " / "+values.length);
		}
		for(int i=0; i<names.length; i++) {
			PropertyData data = new PropertyData(names[i],
			values[i],
			device.getLocation());
			result.add(data);
		}
		} catch(Exception e) {}
		result.sort(new Comparator<PropertyData>() {

			@Override
			public int compare(PropertyData o1, PropertyData o2) {
				return o1.propertyName.compareTo(o2.propertyName);
			}
		});
		return result;
	}
		
	@Override
	public void addWidgetsAboveTable() {
		//init = new ResourceInitSingleEmpty<OGEMADriverPropertyService<?>>(page, "init", true, app.appMan);
		deviceDrop = new ResourceDropdown<Resource>(page, "deviceSubDrop") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				Set<String> allLoc = app.knownResources.keySet();
				//Resource initItem = init.getSelectedItem(req);
				//if(initItem != null) {
				//	
				//}
				List<Resource> all = new ArrayList<>();
				for(String loc: allLoc)
					all.add(appMan.getResourceAccess().getResource(loc));
				update(all, req);
				//selectItem(initItem, req);
			}
		};
		page.append(deviceDrop).linebreak();
		//init.registerDependentWidget(deviceDrop);
		deviceDrop.registerDependentWidget(mainTable);
		Button updateAll = new Button(page, "updateAll", "Update (reload page after some seconds") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				Resource device = deviceDrop.getSelectedItem(req);
				propService.updateProperties(device, app.log);
			}
		};
		StaticTable topTable = new StaticTable(1, 2);
		topTable.setContent(0, 0, updateAll); //.setContent(0, 1, back);
		page.append(topTable).linebreak();
	}

	@Override
	public PhysicalElement getResource(PropertyData object, OgemaHttpRequest req) {
		return appMan.getResourceAccess().getResource(object.dataPointResourceLocation);
	}
	
	static int lineCounter = 0;
	@Override
	public String getLineId(PropertyData object) {
		lineCounter++;
		return ""+lineCounter+"_"+super.getLineId(object);
	}
}
