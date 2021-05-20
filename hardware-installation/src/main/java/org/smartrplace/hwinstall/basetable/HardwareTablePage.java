package org.smartrplace.hwinstall.basetable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.model.gateway.EvalCollection;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.locations.Room;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.InstallationStatusFilterDropdown2;
import org.smartrplace.gui.filtering.DualFiltering;
import org.smartrplace.gui.filtering.SingleFiltering.OptionSavingMode;
import org.smartrplace.gui.filtering.util.RoomFiltering2Steps;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.HeaderData;
import de.iwes.widgets.html.form.label.Label;

@SuppressWarnings("serial")
/** Note that only the method getDevicesSelected is really implemented here*/
public class HardwareTablePage implements InstalledAppsSelector { //extends DeviceTablePageFragment
	//protected final HardwareInstallController controller;
	protected final WidgetPage<?> page;
	protected final Alert alert;
	protected final ApplicationManager appMan;
	protected final ApplicationManagerPlus appManPlus;
	protected final DeviceHandlerAccess devHandAcc;
	protected final HardwareTableData resData;

	public enum FilterMode {
		STANDARD,
		KNOWN_FAULTS
	}
	protected final FilterMode filterMode;
	
	private Header header;
	//protected RoomSelectorDropdown roomsDrop;
	protected RoomFiltering2Steps<InstallAppDevice> roomsDrop;
	protected InstallationStatusFilterDropdown2 installFilterDrop;
	protected DualFiltering<Room, InstallAppDevice, InstallAppDevice> finalFilter;
	//protected final InstalledAppsSelector instAppsSelector;
	protected final StaticTable topTable;

	protected int getTopTableLines() {
		return 1;
	}
	protected boolean offerAddRoomButton() {
		return true;
	}
	
	protected class SubTableData {
		DeviceHandlerProvider<?> pe;
		DeviceTableBase table;
		public SubTableData(DeviceHandlerProvider<?> pe, DeviceTableBase table) {
			this.pe = pe;
			this.table = table;
		}
	}
	protected final List<SubTableData> subTables = new ArrayList<>();
	
	protected String pid() {
		return WidgetHelper.getValidWidgetId(this.getClass().getName());
	}
	protected String getHeader() {return "Device Setup and Configuration";}

	public HardwareTablePage(WidgetPage<?> page, final ApplicationManagerPlus appManPlus,
			final DeviceHandlerAccess devHandAcc, HardwareTableData resData) {
		this(page, appManPlus, devHandAcc, resData, true);
	}
	public HardwareTablePage(WidgetPage<?> page, final ApplicationManagerPlus appManPlus,
			final DeviceHandlerAccess devHandAcc, HardwareTableData resData, boolean triggerFinishConstructorAutomatically) {
		this(page, appManPlus, devHandAcc, resData, triggerFinishConstructorAutomatically, FilterMode.STANDARD);
	}
	public HardwareTablePage(WidgetPage<?> page, final ApplicationManagerPlus appManPlus,
			final DeviceHandlerAccess devHandAcc, HardwareTableData resData, boolean triggerFinishConstructorAutomatically,
			FilterMode filterMode) {
		this.page = page;
		this.appMan = appManPlus.appMan();
		this.appManPlus = appManPlus;
		this.devHandAcc = devHandAcc;
		this.resData = resData;
		this.filterMode = filterMode;
		
		//this.controller = controller;
		//init all widgets
		this.alert = new Alert(page, WidgetHelper.getValidWidgetId("alert"+pid()), "");
		
		//this.instAppsSelector = this;

		header = new Header(page, "header", getHeader());
		header.addDefaultStyle(HeaderData.TEXT_ALIGNMENT_LEFT);
		page.append(header).linebreak();
		
		OgemaWidget installMode;
		if(devHandAcc == null) {
			installMode = new Label(page, "installModeLab", "Search for new devices: "+
					(resData.appConfigData.isInstallationActive().getValue()?"active":"inactive"));
		} else {
			installMode = new BooleanResourceButton(page, "installMode", "Search for new devices",
					resData.appConfigData.isInstallationActive()) {
				private static final long serialVersionUID = 1L;
	
				@Override
				public void updateDependentWidgets(OgemaHttpRequest req) {
					for(SubTableData tabData: subTables) {
						if(isObjectsInTableEmpty(tabData.pe, req)) {
							tabData.table.getMainTable().setWidgetVisibility(false, req);
							tabData.table.getHeaderWidget().setWidgetVisibility(false, req);
						} else {
							tabData.table.getMainTable().setWidgetVisibility(true, req);						
							tabData.table.getHeaderWidget().setWidgetVisibility(true, req);
						}
					}
				}
	
				@Override
				public void onPrePOST(String data, OgemaHttpRequest req) {
					super.onPrePOST(data, req);
					//controller.checkDemands();
				}
			};
		}
		//roomsDrop = new RoomSelectorDropdown(page, "roomsDrop", controller);
		ResourceList<BuildingPropertyUnit> roomGroups = resData.accessAdminConfigRes.roomGroups();
		roomsDrop = new RoomFiltering2Steps<InstallAppDevice>(page, "roomsDrop",
				OptionSavingMode.GENERAL, 10000, roomGroups, appManPlus, true) {

					@Override
					protected Room getAttribute(InstallAppDevice object) {
						return object.device().location().room().getLocationResource();
					}
			
		};
		//installFilterDrop = new InstallationStatusFilterDropdown(page, "installFilterDrop", controller);
		installFilterDrop = new InstallationStatusFilterDropdown2(page, "installFilterDrop",
				OptionSavingMode.PER_USER, appMan,
				filterMode==FilterMode.KNOWN_FAULTS?true:false);
		finalFilter = new DualFiltering<Room, InstallAppDevice, InstallAppDevice>(roomsDrop, installFilterDrop);
		
		//RedirectButton roomLinkButton = new RedirectButton(page, "roomLinkButton", "Room Administration", "/de/iwes/apps/roomlink/gui/index.html");
		
		//RedirectButton calendarConfigButton = new RedirectButton(page, "calendarConfigButton",
		//		"Calendar Configuration", "/org/smartrplace/apps/smartrplaceheatcontrolv2/extensionpage.html");
		
		OgemaWidget commitBtn = getCommitButton();
		/*Button commitBtn = new Button(page, "commitBtn", "Commit changes") {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				EvalCollection ec = ResourceHelper.getEvalCollection(appMan);
				if(!ec.roomDeviceUpdateCounter().isActive()) {
					ec.roomDeviceUpdateCounter().create().activate(false);
				}
				ec.roomDeviceUpdateCounter().getAndAdd(1);
			}
		};*/
		
		topTable = new StaticTable(getTopTableLines(), 7, new int[] {2, 2, 1, 2, 1, 2, 2});
		int installFilterCol=3;
		topTable.setContent(0, 0, roomsDrop.getFirstDropdown())
				.setContent(0, 1, roomsDrop)
				.setContent(0, installFilterCol, installFilterDrop)
				.setContent(0, installFilterCol+2, installMode);//setContent(0, 2, roomLinkButton).
		if(commitBtn != null)
			topTable.setContent(0, 2, commitBtn);

		if(offerAddRoomButton()) {
			RedirectButton addRoomLink = new RedirectButton(page, "addRoomLink", "Add room", "/org/smartrplace/external/accessadmin/roomconfig.html");
			topTable.setContent(0, installFilterCol+3, addRoomLink);
		}
		//RoomEditHelper.addButtonsToStaticTable(topTable, (WidgetPage<RoomLinkDictionary>) page,
		//		alert, appMan, 0, 3);
		//topTable.setContent(0, 5, calendarConfigButton);
		page.append(topTable);
		
		if(triggerFinishConstructorAutomatically)
			finishConstructor();
	}
	
	protected OgemaWidget getCommitButton() {
		Button commitBtn = new Button(page, "commitBtn", "Commit changes") {
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				EvalCollection ec = ResourceHelper.getEvalCollection(appMan);
				if(!ec.roomDeviceUpdateCounter().isActive()) {
					ec.roomDeviceUpdateCounter().create().activate(false);
				}
				ec.roomDeviceUpdateCounter().getAndAdd(1);
			}
		};
		return commitBtn;
	}
	
	protected void finishConstructor() {
		updateTables();
	}
	
	protected Set<String> tableProvidersDone = new HashSet<>();
	public void updateTables() {
		synchronized(tableProvidersDone) {
		if(devHandAcc != null) for(DeviceHandlerProvider<?> pe: devHandAcc.getTableProviders().values()) {
			//if(isObjectsInTableEmpty(pe))
			//	continue;
			String id = pe.id();
			if(tableProvidersDone.contains(id))
				continue;
			tableProvidersDone.add(id);
			DeviceTableBase tableLoc = pe.getDeviceTable(page, alert, this);
			tableLoc.triggerPageBuild();
			installFilterDrop.registerDependentWidget(tableLoc.getMainTable());
			roomsDrop.registerDependentWidget(tableLoc.getMainTable());
			roomsDrop.getFirstDropdown().registerDependentWidget(tableLoc.getMainTable());
			subTables.add(new SubTableData(pe, tableLoc));
			
		}
		}
	}

	protected boolean isObjectsInTableEmpty(DeviceHandlerProvider<?> pe, OgemaHttpRequest req) {
		List<InstallAppDevice> all = getDevicesSelected(pe, req);
		return all.isEmpty();
	}

	@Override
	public List<InstallAppDevice> getDevicesSelected(DeviceHandlerProvider<?> devHand, OgemaHttpRequest req) {
		List<InstallAppDevice> all = getDevices(devHand);
//System.out.println("For "+pe.label(null)+" before filter:"+all.size());
		return finalFilter.getFiltered(all, req);
	}
	
	public <T extends Resource> List<InstallAppDevice> getDevices(DeviceHandlerProvider<T> tableProvider) {
		//boolean includeInactiveDevices = resData.appConfigData.includeInactiveDevices().getValue();
		//return getDevices(tableProvider, includeInactiveDevices, false);
		return getDevices(tableProvider, false);
	}
	public <T extends Resource> List<InstallAppDevice> getDevices(DeviceHandlerProvider<T> tableProvider,
			boolean includeTrash) {
		List<InstallAppDevice> result = new ArrayList<>();
		for(InstallAppDevice install: resData.appConfigData.knownDevices().getAllElements()) {
			if((!includeTrash) && install.isTrash().getValue())
				continue;
			if(tableProvider == null) {
				result.add(install);
				continue;
			}
			if(tableProvider.id().equals(install.devHandlerInfo().getValue()))	{
				result.add(install);
			}
		}
		return result;
	}

	
	@Override
	public InstallAppDevice getInstallResource(Resource device) {
		throw new UnsupportedOperationException("Not implemented in HardwareTableBase!");
	}
	@Override
	public <T extends Resource> InstallAppDevice addDeviceIfNew(T model, DeviceHandlerProvider<T> tableProvider) {
		throw new UnsupportedOperationException("Not implemented in HardwareTableBase!");
	}
	@Override
	public <T extends Resource> InstallAppDevice removeDevice(T model) {
		throw new UnsupportedOperationException("Not implemented in HardwareTableBase!");
	}
	@Override
	public <T extends Resource> void startSimulation(DeviceHandlerProvider<T> tableProvider, T device) {
		throw new UnsupportedOperationException("Not implemented in HardwareTableBase!");		
	}
	@Override
	public void addWidgetsExpert(DeviceHandlerProvider<?> tableProvider, InstallAppDevice object,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan) {
		throw new UnsupportedOperationException("Not implemented in HardwareTableBase!");
	}
}


