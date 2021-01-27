package org.smartrplace.apps.hw.install.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.autoconfig.api.DeviceTypeProvider;
import org.smartrplace.autoconfig.api.DeviceTypeProvider.CreateAndConfigureResult;
import org.smartrplace.autoconfig.api.DeviceTypeProvider.DeviceTypeConfigData;
import org.smartrplace.autoconfig.api.DeviceTypeProvider.DeviceTypeConfigDataBase;
import org.smartrplace.gui.tablepages.ObjectGUITablePageNamed;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.html.textarea.TextArea;
import de.iwes.widgets.template.DefaultDisplayTemplate;

@SuppressWarnings("serial")
public class DeviceTypeConfigPage extends ObjectGUITablePageNamed<DeviceTypeConfigData<?>, Resource> {
	protected final HardwareInstallController controller;
	
	public DeviceTypeConfigPage(WidgetPage<?> page, HardwareInstallController controller) {
		super(page, controller.appMan, new DeviceTypeConfigData<Resource>(null, null, null));
		this.controller = controller;
		triggerPageBuild();
	}
	
	@Override
	protected String getHeader(OgemaLocale locale) {
		return "Device Configuration based on Device Type Database";
	}
	
	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();

		final TextArea descriptionArea = new TextArea(page, "descriptionArea");
		
		StaticTable topTable = new StaticTable(1, 6);
		
		final TextField address = new TextField(page, "address");
		final TextField password = new TextField(page, "password");
		final TextField configuration = new TextField(page, "configuration");
		
		final TemplateDropdown<DeviceTypeProvider<?>> providerDrop = new TemplateDropdown<DeviceTypeProvider<?>>(page, "providerDrop") {
			@Override
			public void onGET(OgemaHttpRequest req) {
				Collection<DeviceTypeProvider<?>> all = controller.getDeviceTypeProviders().values();
				update(all, req);
			}
			@Override
			public void updateDependentWidgets(OgemaHttpRequest req) {
				//Collection<DeviceTypeProvider<?>> all = controller.getDeviceTypeProviders().values();
				//update(all, req);
				DeviceTypeProvider<?> item = getSelectedItem(req);
				DeviceTypeConfigDataBase phData = item.getPlaceHolderData();
				descriptionArea.setText(item.description(req.getLocale()), req);
				if(phData != null && phData.address != null)
					address.setPlaceholder(phData.address, req);
				if(phData != null && phData.password != null)
					password.setPlaceholder(phData.password, req);
				if(phData != null && phData.configuration != null)
					configuration.setPlaceholder(phData.configuration, req);
			}
		};
		providerDrop.setTemplate(new DefaultDisplayTemplate<DeviceTypeProvider<?>>() {
			@Override
			public String getLabel(DeviceTypeProvider<?> arg0, OgemaLocale arg1) {
				return arg0.label(arg1);
			}
		});
		providerDrop.registerDependentWidget(descriptionArea);
		providerDrop.registerDependentWidget(address);
		providerDrop.registerDependentWidget(password);
		providerDrop.registerDependentWidget(configuration);
		
		Button testButton = new Button(page, "testButton", "Test") {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				DeviceTypeProvider<?> item = providerDrop.getSelectedItem(req);
				DeviceTypeConfigData<?> config = new DeviceTypeConfigData<Resource>(address.getValue(req),
						password.getValue(req), configuration.getValue(req));
				config.dtbProvider = (DeviceTypeProvider) item;
				String result = checkConfiguration(config);
				alert.showAlert(result, true, req);
			}
		};
		testButton.registerDependentWidget(alert);
	
		Button createButton = new Button(page, "createButton", "Configure") {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				DeviceTypeProvider<?> item = providerDrop.getSelectedItem(req);
				DeviceTypeConfigData<?> config = new DeviceTypeConfigData<Resource>(address.getValue(req),
						password.getValue(req), configuration.getValue(req));
				config.dtbProvider = (DeviceTypeProvider) item;
				CreateAndConfigureResult<?> result = addAndConfigure(config);
				alert.showAlert(result.resultMessage, true, req);
			}
		};
		createButton.registerDependentWidget(alert);
		createButton.registerDependentWidget(mainTable);
	
		topTable.setContent(0, 0, providerDrop);
		topTable.setContent(0, 1, address);
		topTable.setContent(0, 2, password);
		topTable.setContent(0, 3, configuration);
		topTable.setContent(0, 4, testButton);
		topTable.setContent(0, 5, createButton);
		page.append(topTable);
		page.append(descriptionArea);
	}
	
	@Override
	public void addWidgets(final DeviceTypeConfigData<?> object,
			ObjectResourceGUIHelper<DeviceTypeConfigData<?>, Resource> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan) {
		addNameLabel(object, vh, id, row, req);
		vh.stringLabel("Password", id, getPasswordString(object.password), row);
		vh.stringLabel("Configuration", id, object.configuration, row);
		vh.stringLabel("Resource", id,  object.governingResource!=null?object.governingResource.getLocation():"n/a", row);
		if(req == null) {
			vh.registerHeaderEntry("Provider");
			vh.registerHeaderEntry("Delete");
			return;
		}
		vh.stringLabel("Provider", id, object.dtbProvider.label(req!=null?req.getLocale():null), row);
		ButtonConfirm deleteButton = new ButtonConfirm(mainTable, "deleteButton"+id, req) {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				deleteObject(object);
			}
		};
		deleteButton.setText("Delete", req);
		deleteButton.setConfirmMsg("Really delete configuration and resource for "+object.address+" ?", req);
		row.addCell("Delete", deleteButton);
	}

	protected <T extends Resource> void deleteObject(DeviceTypeConfigData<T> object) {
		object.dtbProvider.deleteConfig(object);
	}
	protected <T extends Resource> String checkConfiguration(DeviceTypeConfigData<T> object) {
		return object.dtbProvider.checkConfiguration(object);
	}
	protected <T extends Resource> CreateAndConfigureResult<T> addAndConfigure(DeviceTypeConfigData<T> object) {
		return object.dtbProvider.addAndConfigureDevice(object);
	}
	
	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "Address";
	}

	@Override
	protected String getLabel(DeviceTypeConfigData<?> obj, OgemaHttpRequest req) {
		return obj.address;
	}

	@Override
	public Collection<DeviceTypeConfigData<?>> getObjectsInTable(OgemaHttpRequest req) {
		Collection<DeviceTypeProvider<?>> allProvs = controller.getDeviceTypeProviders().values();
		List<DeviceTypeConfigData<?>> result = new ArrayList<>();
		for(DeviceTypeProvider<?> dtbProv: allProvs) {
			result.addAll(dtbProv.getKnownConfigs());
		}
		return result;
	}

	public static String getPasswordString(String password) {
		if(password == null)
			return "--";
		if(password.isEmpty())
			return "";
		return new String(new char[password.length()]).replace("\0", "*");
	}
}
