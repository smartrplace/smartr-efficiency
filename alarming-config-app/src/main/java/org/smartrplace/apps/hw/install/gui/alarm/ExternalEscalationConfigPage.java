package org.smartrplace.apps.hw.install.gui.alarm;

import java.util.Map.Entry;

import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.DpGroupUtil;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.apps.alarmingconfig.AlarmingConfigAppController;
import org.smartrplace.apps.alarmingconfig.escalationservices.ExternalEscalationProvider;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.template.DefaultDisplayTemplate;
import de.iwes.widgets.template.DisplayTemplate;

@SuppressWarnings("serial")
public class ExternalEscalationConfigPage extends DeviceAlarmingPage {

	public ExternalEscalationConfigPage(WidgetPage<?> page, AlarmingConfigAppController controller) {
		super(page, controller);
	}

	@Override
	protected String getHeader() {
		return "8. External Escalation Configuration per Device";
	}
	
	class EscSelectDropdown extends TemplateDropdown<ExternalEscalationProvider> {
		final InstallAppDevice object;
		final long escTime;
		
		public EscSelectDropdown(InstallAppDevice object, long escTime,
				OgemaWidget parent, String id, OgemaHttpRequest req) {
			super(parent, id, req);
			this.object = object;
			this.escTime = escTime;

			DisplayTemplate<ExternalEscalationProvider> escTemplate = new DefaultDisplayTemplate<ExternalEscalationProvider>() {
				@Override
				public String getLabel(ExternalEscalationProvider object, OgemaLocale locale) {
					return object.label(locale);
				}
			};
			setTemplate(escTemplate);
			setDefaultItems(controller.escMan.extEscProvs.values());
			setDefaultAddEmptyOption(true, "none selected");
		}

		@Override
		public void onGET(OgemaHttpRequest req) {
			ExternalEscalationProvider prov = getProviderFromResource();
			selectItem(prov, req);
		}
		
		@Override
		public void onPOSTComplete(String data, OgemaHttpRequest req) {
			int index = getIndexFromResource();
			long[] vals = object.externalEscalationProviderIds().getValues();
			if(index >= 0 && vals.length > index) {
				vals[index] = -1;
			}
			ExternalEscalationProvider newSel = getSelectedItem(req);
			if(newSel != null) {
				int newIndex = -1;
				for(Entry<Integer, ExternalEscalationProvider> entry: controller.escMan.extEscProvs.entrySet()) {
					if(entry.getValue().id().equals(newSel.id())) {
						newIndex = entry.getKey();
						break;
					}
				}
				if(newIndex >= 0) {
					if(newIndex >= vals.length) {
						long[] newVals = new long[newIndex+1];
						for(int i=0; i<vals.length; i++) {
							newVals[i] = vals[i];
						}
						vals = newVals;
					}
					vals[newIndex] = escTime;
				}
			}
			ValueResourceHelper.setCreate(object.externalEscalationProviderIds(), vals);
		}
		
		int getIndexFromResource() {
			if(!object.externalEscalationProviderIds().exists())
				return -1;
			long[] vals = object.externalEscalationProviderIds().getValues();
			for(int index=0; index<vals.length; index++) {
				if(vals[index] == escTime)
					return index;
			}
			return -1;
		}
		
		ExternalEscalationProvider getProviderFromResource() {
			int index = getIndexFromResource();
			if(index < 0)
				return null;
			return controller.escMan.extEscProvs.get(index);
		}
	}
	
	protected DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert, InstalledAppsSelector appSelector,
			final DeviceHandlerProvider<?> pe) {
		final String pageTitle;
		DatapointGroup grp = DpGroupUtil.getDeviceTypeGroup(pe, appManPlus.dpService(), false);
		if(grp != null)
			pageTitle = "Devices of type "+ grp.label(null);
		else
			pageTitle = "Devices of type "+ pe.label(null);
		return new AlarmingDeviceTableBase(page, appManPlus, alert, pageTitle, resData, commitButton, appSelector, pe) {
			protected void addAdditionalWidgets(final InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
					OgemaHttpRequest req, Row row, final ApplicationManager appMan,
					PhysicalElement device, final InstallAppDevice template) {
				if(req == null) {
					vh.registerHeaderEntry("Immediately1");
					vh.registerHeaderEntry("Immediately2");
					vh.registerHeaderEntry("Secondary");
					return;
				}
				
				TemplateDropdown<ExternalEscalationProvider> escProvDrop =
						new EscSelectDropdown(object, 0, mainTable, "escProvDrop_"+id, req);
				row.addCell(WidgetHelper.getValidWidgetId("Immediately1"), escProvDrop);

				TemplateDropdown<ExternalEscalationProvider> escProvDrop2 =
						new EscSelectDropdown(object, 1000, mainTable, "escProvDrop2_"+id, req);
				row.addCell(WidgetHelper.getValidWidgetId("Immediately2"), escProvDrop2);
				
				TemplateDropdown<ExternalEscalationProvider> escProvDrop3 =
						new EscSelectDropdown(object, 24*TimeProcUtil.HOUR_MILLIS, mainTable, "escProvDrop3_"+id, req);
				row.addCell(WidgetHelper.getValidWidgetId("Secondary"), escProvDrop3);
			}			
		};
		
	}
}
