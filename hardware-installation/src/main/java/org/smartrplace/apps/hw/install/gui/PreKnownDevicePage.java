package org.smartrplace.apps.hw.install.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.csv.CSVRecord;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.api.InstallationProgressService;
import org.ogema.devicefinder.api.InstallationProgressService.PreKnownDeviceDataUsageStatus;
import org.ogema.devicefinder.api.InstallationProgressService.PreknownUsage;
import org.ogema.devicefinder.api.InstallationProgressService.RouterInfo;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.devicefinder.util.LastContactLabel;
import org.ogema.drivers.homematic.xmlrpc.hl.types.HmInterfaceInfo;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.config.PreKnownDeviceData;
import org.smartrplace.csv.upload.generic.CSVUploadWidgets;
import org.smartrplace.csv.upload.generic.CSVUploadWidgets.CSVUploadListener;
import org.smartrplace.gui.filtering.GenericFilterBase;
import org.smartrplace.gui.filtering.GenericFilterOption;
import org.smartrplace.gui.filtering.SingleFiltering.OptionSavingMode;
import org.smartrplace.gui.filtering.SingleFilteringDirect;
import org.smartrplace.smarteff.defaultservice.TSManagementPage;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.directresourcegui.GUIHelperExtension;
import org.smartrplace.util.format.WidgetHelper;
import org.smartrplace.util.virtualdevice.ChartsUtil;
import org.smartrplace.util.virtualdevice.HmSetpCtrlManagerTHSetp;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.OGEMAResourceCopyHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.Linebreak;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.HeaderData;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.label.LabelData;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.html.html5.Flexbox;
import de.iwes.widgets.template.DefaultDisplayTemplate;

@SuppressWarnings("serial")
public class PreKnownDevicePage extends ObjectGUITablePage<PreKnownDeviceData, PreKnownDeviceData> {
	public static final long UPDATE_RATE = 4000;
	
	private final HardwareInstallController controller;
	private final boolean isFullTeachIn;
	
	protected static final String SERIAL_NUMBER_HEAD = "Last 4 digits of serial number";
	protected static final String DEVICE_ID_HEAD = "DeviceId number";
	protected static final String ROOM_HEADER = "Room (if known)";
	protected static final String LOCATION_HEADER = "Location (if known)";
	
	TemplateDropdown<InstallAppDevice> ccuSelectDrop;
	SingleFilteringDirect<PreKnownDeviceData> entryFilter;
	
	public PreKnownDevicePage(WidgetPage<?> page, HardwareInstallController controller, boolean isFullTeachIn) {
		super(page, controller.appMan, PreKnownDeviceData.class, false);
		this.controller = controller;
		this.isFullTeachIn = isFullTeachIn;
		triggerPageBuild();
	}

	@Override
	public void addWidgets(final PreKnownDeviceData object,
			ObjectResourceGUIHelper<PreKnownDeviceData, PreKnownDeviceData> vh, String id, OgemaHttpRequest req,
			Row row, ApplicationManager appMan) {
		if(req == null) {
			vh.registerHeaderEntry(SERIAL_NUMBER_HEAD);
			vh.registerHeaderEntry(DEVICE_ID_HEAD);
			if(isFullTeachIn) {
				vh.registerHeaderEntry("CCU");
				vh.registerHeaderEntry("Remove Association");
			}
			vh.registerHeaderEntry(ROOM_HEADER);
			vh.registerHeaderEntry(LOCATION_HEADER);
			vh.registerHeaderEntry("Device Type");
			vh.registerHeaderEntry("Comment");
			vh.registerHeaderEntry("Add/delete");
			return;
		}
		//vh.stringEdit(SERIAL_NUMBER_HEAD, id, object.deviceEndCode(), row, alert);
		
		TextField serialEdit = new TextField(mainTable, "serialEdit"+id, req) {
			@Override
			public void onGET(OgemaHttpRequest req) {
				setValue(object.deviceEndCode().getValue(), req);
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				String val = getValue(req).toUpperCase();
				if(!object.deviceEndCode().exists()) {
					object.deviceEndCode().create();
					object.deviceEndCode().setValue(val);
					object.deviceEndCode().activate(true);
				} else {
					object.deviceEndCode().setValue(val);
				}
				if(alert != null) alert.showAlert("New value: " + val, true, req);
				
			}
		};
		serialEdit.registerDependentWidget(serialEdit);
		row.addCell(WidgetHelper.getValidWidgetId(SERIAL_NUMBER_HEAD), serialEdit);

		
		TextField deviceIdEdit = new TextField(mainTable, "deviceIdEdit"+id, req) {
			@Override
			public void onGET(OgemaHttpRequest req) {
				setValue(object.deviceIdNumber().getValue(), req);
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				String val = getValue(req);
				if(val.length() < 4) {
					try {
						int devNum = Integer.parseInt(val);
						if(devNum >= 0) {
							val = String.format("%04d", devNum);
						}
					} catch(NumberFormatException e) {}
				}
				if(!object.deviceIdNumber().exists()) {
					object.deviceIdNumber().create();
					object.deviceIdNumber().setValue(val);
					object.deviceIdNumber().activate(true);
				} else {
					object.deviceIdNumber().setValue(val);
				}
				if(alert != null) alert.showAlert("New value: " + val, true, req);
				
			}
		};
		deviceIdEdit.registerDependentWidget(deviceIdEdit);
		row.addCell(WidgetHelper.getValidWidgetId(DEVICE_ID_HEAD), deviceIdEdit);
		//vh.stringEdit(DEVICE_ID_HEAD, id, object.deviceIdNumber(), row, alert);
		DeviceTableRaw.addRoomWidgetStatic(vh, id, req, row, controller.appMan, object.room(), ROOM_HEADER);
		vh.stringEdit(LOCATION_HEADER, id, object.installationLocation(), row, alert);
		
		TemplateDropdown<DeviceHandlerProviderDP<?>> devTypeDrop = new TemplateDropdown<DeviceHandlerProviderDP<?>>(
				mainTable, "devTypeDrop"+id, req) {
			@Override
			public void onGET(OgemaHttpRequest req) {
				String val = object.deviceHandlerId().getValue();
				DeviceHandlerProviderDP<?> selected;
				if(val == null || val.isEmpty())
					selected = null;
				else {
					selected = controller.dpService.getDeviceHandlerProvider(val);
				}
				selectItem(selected, req);
				
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				DeviceHandlerProviderDP<?> selected = getSelectedItem(req);
				String val;
				if(selected == null) {
					object.deviceHandlerId().setValue("");
				} else {
					val = selected.id();
					ValueResourceHelper.setCreate(object.deviceHandlerId(), val);
					if(alert != null) alert.showAlert("New value: " + val, true, req);
				}
			}
			
		};
		devTypeDrop.setTemplate(new DefaultDisplayTemplate<DeviceHandlerProviderDP<?>>() {
			@Override
			public String getLabel(DeviceHandlerProviderDP<?> object, OgemaLocale locale) {
				if(object == null)
					return "Any";
				return object.getTableTitle();
			}
		});
		List<DeviceHandlerProviderDP<?>> all = controller.dpService.getDeviceHandlerProviders();
		devTypeDrop.setDefaultItems(all);
		devTypeDrop.setDefaultAddEmptyOption(true, "Any");
		row.addCell(WidgetHelper.getValidWidgetId("Device Type"), devTypeDrop);
		
		vh.stringEdit("Comment", id, object.comment(), row, alert);
		if(isFullTeachIn) {
			InstallationProgressService ips = installService();
			if(ips == null)
				return;
			List<PreknownUsage> sinfo = ips.getUsageStatus(object);
			if(sinfo.isEmpty() || object.getLocation().startsWith("EvalCollection")) {
				vh.referenceDropdownFixedChoice("CCU", id, object.ccu(), row,  ips.getValuesToSet());				
			} else {
				String text;
				if(sinfo.size() == 1) {
					final PreknownUsage stat = sinfo.get(0);
					text = stat.iad.deviceId().getValue();
					vh.stringLabel("CCU", id, text, row);
					ButtonConfirm deleteAssoc = new ButtonConfirm(mainTable, id, req) {
						@Override
						public void onPOSTComplete(String data, OgemaHttpRequest req) {
							stat.remove();
						}
					};
					deleteAssoc.setDefaultText("Remove:"+getStatusShortName(stat.status));
					if(stat.status != PreKnownDeviceDataUsageStatus.RESOURCE_CONFIGURED)
						deleteAssoc.addStyle(ButtonData.BOOTSTRAP_ORANGE, req);
					deleteAssoc.setDefaultConfirmMsg("Really remove CCU Association for "+stat.iad.deviceId().getValue()+" ?");
					row.addCell(WidgetHelper.getValidWidgetId("Remove Association"), deleteAssoc);
				} else {
					text = "!! "+sinfo.size()+" !!";
					for(PreknownUsage stat: sinfo) {
						System.out.println(stat.iad.deviceId().getValue()+" for "+object.deviceEndCode());
					}
					vh.stringLabel("CCU", id, text, row);
				}
			}
		}
		
		if(object.getLocation().startsWith("EvalCollection")) {
			Button addButton = new Button(mainTable, "addButton"+id, "Add", req) {
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					String errorMessage = addEntryLine(object);
					if(errorMessage != null)
						alert.showAlert(errorMessage, false, req);
				}
			};
			row.addCell(WidgetHelper.getValidWidgetId("Add/delete"), addButton);
			addButton.registerDependentWidget(alert);
			addButton.registerDependentWidget(mainTable);
			addButton.registerDependentWidget(serialEdit);
			addButton.registerDependentWidget(deviceIdEdit);
			return;
		} else {
			GUIHelperExtension.addDeleteButton(null, object, mainTable, id, alert, "Add/delete",
					row, vh, req);
		}
		
		
	}

	public static String getStatusShortName(PreKnownDeviceDataUsageStatus status) {
		if(status == PreKnownDeviceDataUsageStatus.FAULTY)
			return "CCU_Fault";
		else if(status == PreKnownDeviceDataUsageStatus.CONFIGURATION_PENDING_ON_ROUTER_ONLY)
			return "CCU Only";
		else if(status == PreKnownDeviceDataUsageStatus.CONFIGURATION_PENDING)
			return "Setup in Progress";
		else
			return "Resource Done";
	}
	
	protected String addEntryLine(PreKnownDeviceData object) {
		String serialNumber = object.deviceEndCode().getValue().trim();
		if(serialNumber.length() < 4) {
			return "Serial number has less than 4 digits!";
		}
		String deviceId = object.deviceIdNumber().getValue().trim();
		if(deviceId.isEmpty()) {
			return "DeviceId cannot be empty!";												
		}
		Integer devNum = null;
		if(deviceId.length() <= 4) {
			try {
				devNum = Integer.parseInt(deviceId);
				if(devNum >= 0) {
					deviceId = String.format("%04d", devNum);
					object.deviceIdNumber().setValue(deviceId);
				}
			} catch(NumberFormatException e) {}
		}
		ResourceList<PreKnownDeviceData> preResList = controller.appConfigData.preKnownDevices();
		
		String devHandId = object.deviceHandlerId().getValue();
		List<PreKnownDeviceData> allExist = preResList.getAllElements();
		for(PreKnownDeviceData pre: allExist) {
			if(serialNumber.equals(pre.deviceEndCode().getValue())) {
				/** If we have the same serial number ending for two devices even from different types
				 * then we should add more information to make it unique
				 */
				return "Serial number already exists: "+serialNumber+" . If two serial numbers have the same last 4 digits please provide 5 digit endings for both devices!";													
			}
			String preDevHandId = pre.deviceHandlerId().getValue();
			if(preDevHandId != null && devHandId != null && (!preDevHandId.equals(devHandId)))
				continue;
			if(deviceId.equals(pre.deviceIdNumber().getValue())) {
				return "DeviceId already exists: "+deviceId;													
			}
		}
		
		PreKnownDeviceData newEl = ResourceListHelper.getOrCreateNamedElementFlex(serialNumber, preResList, true, false);
		OGEMAResourceCopyHelper.copySubResourceIntoDestination(newEl, object,
				controller.appMan, true);
		
		object.deviceEndCode().setValue("");
		if(devNum == null)
			object.deviceIdNumber().setValue("");
		else
			object.deviceIdNumber().setValue(String.format("%04d", devNum+1));
		return null;
	}
	
	@Override
	public PreKnownDeviceData getResource(PreKnownDeviceData object, OgemaHttpRequest req) {
		return object;
	}

	@Override
	public void addWidgetsAboveTable() {
		Header header = new Header(page, "header", isFullTeachIn?"Homematic Teach-In Page":"Preknown Devices");
		header.addDefaultStyle(HeaderData.TEXT_ALIGNMENT_LEFT);
		page.append(header).linebreak();
		
		if(isFullTeachIn) {
			ccuSelectDrop = new TemplateDropdown<InstallAppDevice>(page, "ccuSelectDrop") {
				@Override
				public void onGET(OgemaHttpRequest req) {
					InstallationProgressService ips = initOnGet(this, req);
					if(ips == null)
						return;
					String activeCcu = ips.getActiveRouter();
					Collection<RouterInfo> all = ips.getRouterInfos();
					List<InstallAppDevice> result = new ArrayList<>();
					InstallAppDevice selected = null;
					for(RouterInfo rinfo: all) {
						result.add(rinfo.iad);
						if(rinfo.iad.getLocation().equals(activeCcu))
							selected = rinfo.iad;
					}
					update(result, req);
					selectItem(selected, req);
				}
				
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					InstallationProgressService ips = initOnGet(this, req);
					if(ips == null)
						return;
					InstallAppDevice selected = getSelectedItem(req);
					if(selected == null)
						ips.setActiveRouter(null);
					else
						ips.setActiveRouter(selected.getLocation());
				}
			};
			ccuSelectDrop.setTemplate(new DefaultDisplayTemplate<InstallAppDevice>() {
				@Override
				public String getLabel(InstallAppDevice object, OgemaLocale locale) {
					return object.deviceId().getValue();
				}
			});
			ccuSelectDrop.setDefaultAddEmptyOption(true, "No Teach-in active");
			Label teachInStateLabel = new Label(page, "teachInStateLabel") {
				public void onGET(OgemaHttpRequest req) {
					InstallationProgressService ips = initOnGet(this, req);
					if(ips == null)
						return;
					int status = ips.getInstallationModeStatus();
					String text = (status==1 || status>=20)?"on":"off";
					setText(text, req);
					if(status >= 10)
						text += ("+OTHERS:"+(status%10));
					if(status == 1) {
						removeStyle(LabelData.BOOTSTRAP_ORANGE, req);
						removeStyle(LabelData.BOOTSTRAP_GREEN, req);
						addStyle(LabelData.BOOTSTRAP_BLUE, req);
					} else if(status >= 10) {
						removeStyle(LabelData.BOOTSTRAP_BLUE, req);
						addStyle(LabelData.BOOTSTRAP_ORANGE, req);
					} else {
						removeStyle(LabelData.BOOTSTRAP_ORANGE, req);
						removeStyle(LabelData.BOOTSTRAP_BLUE, req);
						addStyle(LabelData.BOOTSTRAP_GREEN, req);
					}
				}
			};
			teachInStateLabel.setDefaultPollingInterval(UPDATE_RATE);
			
			TemplateDropdown<Integer> selectCC_IPDrop = new TemplateDropdown<Integer>(page, "selectCC_IPDrop") {
				@Override
				public void onGET(OgemaHttpRequest req) {
					InstallationProgressService ips = initOnGet(this, req);
					if(ips == null)
						return;
					selectItem(ips.getActiveSubType(), req);
				}
				
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					InstallationProgressService ips = initOnGet(this, req);
					if(ips == null)
						return;
					Integer selected = getSelectedItem(req);
					ips.setActiveSubType(selected);
				}
			};
			List<Integer> items = new ArrayList<>();
			items.add(0);
			items.add(1);
			selectCC_IPDrop.setDefaultItems(items);
			selectCC_IPDrop.setTemplate(new DefaultDisplayTemplate<Integer>() {
				@Override
				public String getLabel(Integer object, OgemaLocale locale) {
					return object==1?"CC":"IP";
				}
			});
			
			DutyCycleLabelResult hmDC_IP = addDutyCycleLabel("hmDC_IP", false, false);
			DutyCycleLabelResult hmDC_CC = addDutyCycleLabel("hmDC_CC", false, true);
			DutyCycleLabelResult hmDC_IPmax = addDutyCycleLabel("hmDC_IPmax", true, false);
			DutyCycleLabelResult hmDC_CCmax = addDutyCycleLabel("hmDC_CCmax", true, true);
		
			Label faultEval = new Label(page, "faultEval") {
				@Override
				public void onGET(OgemaHttpRequest req) {
					RouterInfo rinfo = initRouterInfo(this, req);
					if(rinfo == null)
						return;
					String text = ""+rinfo.faultyDevices+" / "+rinfo.routerOnlyDevices+" / "+
						rinfo.unconfiguredResourceDevices+" / "+rinfo.totalResourceDevices;
					setText(text, req);
				}
			};
			faultEval.setDefaultPollingInterval(UPDATE_RATE);
			
			Label configPendingLabel = new Label(page, "configPendingLabel") {
				@Override
				public void onGET(OgemaHttpRequest req) {
					RouterInfo rinfo = initRouterInfo(this, req);
					if(rinfo == null)
						return;
					String text = " X of "+rinfo.totalResourceDevices+" CfP";
					setText(text, req);
				}				
			};
			configPendingLabel.setDefaultPollingInterval(UPDATE_RATE);
			
			entryFilter = new SingleFilteringDirect<PreKnownDeviceData>(
					page, "entryFilter", OptionSavingMode.GENERAL, 10000, true) {

				@Override
				protected long getFrameworkTime() {
					return appMan.getFrameworkTime();
				}

				@Override
				protected List<GenericFilterOption<PreKnownDeviceData>> getOptionsDynamic(OgemaHttpRequest req) {
					List<GenericFilterOption<PreKnownDeviceData>> result = new ArrayList<>();
					
					final InstallAppDevice ccuSelected = ccuSelectDrop.getSelectedItem(req);
					
					GenericFilterBase<PreKnownDeviceData> op = new GenericFilterBase<PreKnownDeviceData>("Selected and unassigned") {

						@Override
						public boolean isInSelection(PreKnownDeviceData object, OgemaHttpRequest req) {
							if(!object.ccu().isReference(false))
								return true;
							if(ccuSelected == null)
								return false;
							return object.ccu().equalsLocation(ccuSelected);
						}
					};
					result.add(op);
					GenericFilterBase<PreKnownDeviceData> op2 = new GenericFilterBase<PreKnownDeviceData>("Selected Only") {

						@Override
						public boolean isInSelection(PreKnownDeviceData object, OgemaHttpRequest req) {
							if(ccuSelected == null)
								return false;
							return object.ccu().equalsLocation(ccuSelected);
						}
					};
					result.add(op2);
					return result;
				}
				
			};
			
			StaticTable topTeachTable = new StaticTable(3, 6);
			topTeachTable.setContent(0, 0, "Select CCU to start teach-in mode").setContent(0, 1, ccuSelectDrop).setContent(0, 2, teachInStateLabel);
			
			topTeachTable.setContent(0, 3, selectCC_IPDrop).setContent(0, 4, hmDC_IP.dcLabel).setContent(0, 5, hmDC_CC.dcLabel);
			
			topTeachTable.setContent(1, 0, entryFilter).setContent(1, 1, configPendingLabel).setContent(1, 2, "CCU-Faults/Res missing/Res Pending/Total");
			topTeachTable.setContent(1, 3, faultEval).setContent(1, 4, hmDC_IPmax.dcLabel).setContent(1, 5, hmDC_CCmax.dcLabel);

			topTeachTable.setContent(2, 4, hmDC_IP.lastContactLabel).setContent(2, 5, hmDC_CC.lastContactLabel);

			page.append(topTeachTable);

		} //finish isFullTeachin
		
		/*DeviceTypeFilterDropdown typeFilterDrop = new DeviceTypeFilterDropdown(page, "devTypeFilterDrop",
				OptionSavingMode.PER_USER, appMan, controller.dpService);
		StaticTable topTable = new StaticTable(1, 7, new int[] {2, 2, 1, 2, 1, 2, 2});
		topTable.setContent(0, 0, "")
				.setContent(0, 1, "")
				.setContent(0, 3, typeFilterDrop);
		page.append(topTable);*/
		
		CSVUploadListener listener = new CSVUploadListener() {
			
			@Override
			public boolean fileUploaded(String filePath, OgemaHttpRequest req) {
				return true;
			}
			
			@Override
			public void newLineAvailable(String filePath, CSVRecord record, OgemaHttpRequest req) {
				PreKnownDeviceData addEl = ResourceHelper.getSampleResource(PreKnownDeviceData.class);
				readLine(addEl.deviceEndCode(), record, "Last 4 digits of serial number");
				readLine(addEl.deviceIdNumber(), record, "DeviceId number");
				readLine(addEl.installationLocation(), record, "Location (if known)");
				readLine(addEl.comment(), record, "comment");
				addEntryLine(addEl);
			}
			
			protected void readLine(StringResource dest, CSVRecord record, String col) {
				try {
					ValueResourceHelper.setCreate(dest, record.get(col));
				} catch(IllegalArgumentException e) {}
			}
		};
		CSVUploadWidgets uploadCSV = new CSVUploadWidgets(page, alert, pid(),
				"Import Building as CSV", listener , appMan);
		uploadCSV.uploader.getFileUpload().setDefaultPadding("1em", false, true, false, true);
		Flexbox flexLineCSV = TSManagementPage.getHorizontalFlexBox(page, "csvFlex"+pid(),
				uploadCSV.csvButton, uploadCSV.uploader.getFileUpload());
		page.append(flexLineCSV);
		page.append(Linebreak.LINEBREAK);

	}	

	@Override
	public Collection<PreKnownDeviceData> getObjectsInTable(OgemaHttpRequest req) {
		List<PreKnownDeviceData> result = new ArrayList<>(controller.appConfigData.preKnownDevices().getAllElements());
		if(isFullTeachIn) {
			result = entryFilter.getFiltered(result, req);
		}
		PreKnownDeviceData addEl = ResourceHelper.getSampleResource(PreKnownDeviceData.class);
		result.add(0, addEl);
		//result.add(addEl);
		return result;
	}

	@Override
	public String getLineId(PreKnownDeviceData object) {
		if(object.getLocation().startsWith("EvalCollection"))
			return "0000"+super.getLineId(object);
		return object.deviceIdNumber().getValue()+super.getLineId(object);
	}
	
	InstallationProgressService installService = null;
	InstallationProgressService installService() {
		if(installService != null)
			return installService;
		return controller.dpService.installationService("HM-FLEX");
	}
	
	InstallationProgressService initOnGet(OgemaWidget widget, OgemaHttpRequest req) {
		InstallationProgressService result = installService();
		if(result == null)
			widget.disable(req);
		else
			widget.enable(req);
		return result;
	}
	
	RouterInfo initRouterInfo(OgemaWidget widget, OgemaHttpRequest req) {
		InstallationProgressService ips = initOnGet(widget, req);
		if(ips == null)
			return null;
		String ccuName = ips.getActiveRouter();
		if(ccuName == null)
			return null;
		return ips.getRouterInfo(ccuName);
	}
	
	class DutyCycleLabelResult {
		Label dcLabel;
		Label lastContactLabel;
	}
	DutyCycleLabelResult addDutyCycleLabel(String id, final boolean isMax, final boolean isCC) {
		DutyCycleLabelResult result = new DutyCycleLabelResult();
		result.dcLabel = new Label(page, id) {
			public void onGET(OgemaHttpRequest req) {
				RouterInfo rinfo = initRouterInfo(this, req);
				if(rinfo == null) {
					setText("--", req);
					return;
				}
				FloatResource dcRes;
				if(isMax) {
					InstallAppDevice iadLoc = (isCC?rinfo.iadCC:rinfo.iadIP);
					if(iadLoc == null) {
						setText("--", req);
						return;
					}
					dcRes = iadLoc.getSubResource(HmSetpCtrlManagerTHSetp.dutyCycleMax, FloatResource.class);					
				} else {
					HmInterfaceInfo deviceLoc = (isCC?rinfo.deviceCC:rinfo.deviceIP); 
					if(deviceLoc == null) {
						setText("--", req);
						return;
					}
					dcRes = deviceLoc.dutyCycle().reading();
				}
				ChartsUtil.getDutyCycleLabelOnGET(isCC?rinfo.iadCC:rinfo.iadIP, dcRes, this, req);
			}
		};
		result.dcLabel.setDefaultPollingInterval(UPDATE_RATE);
		if(!isMax) {
			result.lastContactLabel = new LastContactLabel(page, id+"_lastCt", null, appMan) {
				@Override
				public void onGET(OgemaHttpRequest req) {
					RouterInfo rinfo = initRouterInfo(this, req);
					if(rinfo == null) {
						setText("--", req);
						return;
					}
					HmInterfaceInfo deviceLoc = (isCC?rinfo.deviceCC:rinfo.deviceIP); 
					if(deviceLoc == null) {
						setText("--", req);
						return;
					}
					long ts = deviceLoc.dutyCycle().reading().getLastUpdateTime();
					String val = StringFormatHelper.getFormattedAgoValue(appMan, ts);
					setText(val, req);
				}
			};
			result.lastContactLabel.setDefaultPollingInterval(UPDATE_RATE);
		}
		return result;
	}
	
}
