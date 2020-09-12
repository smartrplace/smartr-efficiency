package org.smartrplace.apps.hw.install.gui.expert;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.DriverPropertySuccessHandler;
import org.ogema.devicefinder.api.OGEMADriverPropertyService.AccessAvailability;
import org.ogema.devicefinder.api.PropType;
import org.ogema.devicefinder.api.PropertyService;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.apps.hw.install.gui.expert.MainPageExpert.GetPlotButtonResult;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.textfield.TextField;

@SuppressWarnings("serial")
public class MainPageExpertProps extends MainPage {

	private static final String DATAPOINT_INFO_HEADER = "DP/Log/Transfer/Tmpl";

	private static final List<String> PROPS = new ArrayList<>();
	static {
		for(PropType semantic: PropType.STD_PROPS) {
			PROPS.add(semantic.label(null));
		}
	}

	@Override
	protected String getHeader() {return "Device Setup and Configuration with Properties";}

	public MainPageExpertProps(WidgetPage<?> page, final HardwareInstallController controller) {
		super(page, controller);
		
		//topTable.setContent(0, 4, updateDatapoints);
	}

	@Override
	public void addWidgetsExpert(final InstallAppDevice object,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req, Row row,
			final ApplicationManager appMan) {
		//vh.stringLabel("IAD", id, object.getName(), row);
		//vh.stringLabel("ResLoc", id, object.device().getLocation(), row);
		if(req == null) {
			vh.registerHeaderEntry(DATAPOINT_INFO_HEADER);
			vh.registerHeaderEntry("Plot");
			vh.registerHeaderEntry("Semantic properties");
			vh.registerHeaderEntry("Read");
			vh.registerHeaderEntry("Write");
			vh.registerHeaderEntry("Update");
			return;
		}

		final GetPlotButtonResult logResult = MainPageExpert.getPlotButton(id, object, controller, true, vh, row, req);
		if(logResult.devHand != null) {
			row.addCell("Plot", logResult.plotButton);
			
			final PropertyService propHand = logResult.devHand.getPropertyService();
			if(propHand == null)
				return;
			final TemplateDropdown<PropType> propDrop = new TemplateDropdown<PropType>(vh.getParent(), "propDrop"+id, req) {
				@Override
				public void onGET(OgemaHttpRequest req) {
					List<PropType> propsInDev = new ArrayList<>();
					for(PropType prop: PropType.STD_PROPS) {
						String vals = propHand.getProperty(object.device(), prop, null);
						if(vals != null)
							propsInDev.add(prop);
					}
					update(propsInDev, req);
				}
			};
			propDrop.setDefaultItems(PropType.STD_PROPS);
			row.addCell(WidgetHelper.getValidWidgetId("Semantic properties"), propDrop);
			Label readProp = new Label(vh.getParent(), WidgetHelper.getValidWidgetId("readProp"+id), req) {
				@Override
				public void onGET(OgemaHttpRequest req) {
					PropType sel = propDrop.getSelectedItem(req);
					if(sel ==  null) {
						setWidgetVisibility(false, req);
						return;
					}
					if(sel.access == AccessAvailability.READ || sel.access == AccessAvailability.WRITE) {
						setWidgetVisibility(true, req);
						String vals = propHand.getProperty(object.device(), sel, null);
						setText(vals, req);
					} else
						setWidgetVisibility(false, req);
				}
			};
			row.addCell("Read", readProp);
			propDrop.registerDependentWidget(readProp, req);
			
			TextField writeProp = new TextField(vh.getParent(), WidgetHelper.getValidWidgetId("writeProp"+id), req) {
				@Override
				public void onGET(OgemaHttpRequest req) {
					PropType sel = propDrop.getSelectedItem(req);
					if(sel ==  null) {
						setWidgetVisibility(false, req);
						return;
					}
					if(sel.access == AccessAvailability.WRITE_ONLY || sel.access == AccessAvailability.WRITE) {
						setWidgetVisibility(true, req);
						String vals = propHand.getProperty(object.device(), sel, null);
						setValue(vals, req);
					} else
						setWidgetVisibility(false, req);
				}
				
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					PropType sel = propDrop.getSelectedItem(req);
					String vals = getValue(req);
					propHand.setProperty(object.device(), sel, vals, null);
				}
			};
			row.addCell("Write", writeProp);
			propDrop.registerDependentWidget(writeProp, req);
			
			Button updateProp = new Button(vh.getParent(), WidgetHelper.getValidWidgetId("updateProp"+id), req) {
				@Override
				public void onGET(OgemaHttpRequest req) {
					PropType sel = propDrop.getSelectedItem(req);
					if(sel ==  null) {
						setWidgetVisibility(false, req);
						return;
					}
					setText("Update "+sel.label(req.getLocale()), req);
					setWidgetVisibility(true, req);
				}
				
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					final PropType sel = propDrop.getSelectedItem(req);
					final long start = appMan.getFrameworkTime();
					DriverPropertySuccessHandler<?> handler = new DriverPropertySuccessHandler<Resource>() {

						@Override
						public void operationFinished(Resource dataPointResource, String propertyId, boolean success,
								String message) {
							String vals = propHand.getProperty(object.device(), sel, null);
System.out.println("Post-Reading val: "+vals+ " after ms:"+(appMan.getFrameworkTime()-start));
						}
					};
					String vals = propHand.getProperty(object.device(), sel, handler);
System.out.println("Pre-Reading val: "+vals);
				}
				
			};
			row.addCell("Update",updateProp);
			propDrop.registerDependentWidget(updateProp, req);
		}
		
	}
}
