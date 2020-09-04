package org.smartrplace.apps.hw.install.gui.prop;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableInt;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.array.StringArrayResource;
import org.ogema.devicefinder.api.DriverPropertySuccessHandler;
import org.ogema.devicefinder.api.OGEMADriverPropertyService;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.prop.DriverPropertyUtils;
import org.smartrplace.gui.tablepages.ObjectGUITablePageNamed;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;

@SuppressWarnings("serial")
public class DriverPropertyPageAll extends ObjectGUITablePageNamed<OGEMADriverPropertyService<?>, Resource> {
	protected HardwareInstallController controller;

	public DriverPropertyPageAll(WidgetPage<?> page, final HardwareInstallController controller) {
		super(page, controller.appMan, null);
		this.controller = controller;
		triggerPageBuild();
	}

	@Override
	public void addWidgets(final OGEMADriverPropertyService<?> object,
			ObjectResourceGUIHelper<OGEMADriverPropertyService<?>, Resource> vh, String id, OgemaHttpRequest req,
			Row row, final ApplicationManager appMan) {
		addNameLabel(object, vh, id, row, req);
		if(req == null) {
			vh.registerHeaderEntry("Type");
			vh.registerHeaderEntry("Started");
			vh.registerHeaderEntry("Known resources");
			vh.registerHeaderEntry("Update resource tree");
			return;
		}
		vh.stringLabel("Type", id, object.getDataPointResourceType().getName(), row);
		Set<String> ress = controller.usedServices.get(object);
		vh.stringLabel("Started", id, ""+(ress != null), row);
		if(ress != null) {
			vh.stringLabel("Known resources", id, ""+ress.size(), row);
		}
		Button updateButton = new Button(mainTable, "updateButton"+id, "Update resource tree", req) {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				int resNum = updateAllResources(object, alert, req);
				alert.showAlert("Started property updates for "+object.label(req.getLocale())+" on "+resNum+" resources.", true, req);
			}
		};
		row.addCell(WidgetHelper.getValidWidgetId("Update resource tree"), updateButton);
	}

	protected <R extends Resource> int updateAllResources(final OGEMADriverPropertyService<R> object,
			final Alert alert, final OgemaHttpRequest req) {
		Class<R> type = object.getDataPointResourceType();
		final List<R> all = appMan.getResourceAccess().getResources(type);
		final MutableInt count = new MutableInt(0);
		final MutableInt finished = new MutableInt(0);
		for(final R res: all) {
			final StringArrayResource[] props = DriverPropertyUtils.getPropertyResources(res, false);
			final int befSize;
			if(props != null)
				befSize = props[0].getValues().length;
			else
				befSize = -1;
			DriverPropertySuccessHandler<R> sucHand = new DriverPropertySuccessHandler<R>() {

				@Override
				public void operationFinished(R dataPointResource, String propertyId, boolean success,
						String message) {
					synchronized (finished) {
					finished.increment();
					if(props == null) {
						StringArrayResource[] props2 = DriverPropertyUtils.getPropertyResources(res, false);
						if(props2 != null) {
							controller.addPropServiceEntry(res, object);
							count.increment();
						}
					} else {
						int afterSize = props[0].getValues().length;
						if(afterSize > befSize) {
							controller.addPropServiceEntry(res, object);
							count.increment();
						}
					}
					if(finished.getValue() == all.size()) {
						if(alert != null && req != null)
							alert.showAlert("Added properties for "+count+" resources",
									success, Long.MAX_VALUE, req);
					}
					}
				}
			};
			object.updateProperties(res, controller.log, sucHand);
		}
		return all.size();
	}
	
	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "Name";
	}

	@Override
	protected String getLabel(OGEMADriverPropertyService<?> obj, OgemaHttpRequest req) {
		return obj.label(req.getLocale());
	}

	@Override
	public Collection<OGEMADriverPropertyService<?>> getObjectsInTable(OgemaHttpRequest req) {
		return controller.hwInstApp.getDPropertyProviders().values();
	}
	
	@Override
	protected String getHeader(OgemaLocale locale) {
		return "DriverProperty Services Overview";
	}

}
