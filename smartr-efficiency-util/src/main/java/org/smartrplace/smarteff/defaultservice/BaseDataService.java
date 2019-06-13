package org.smartrplace.smarteff.defaultservice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application.AppStopReason;
import org.ogema.core.model.Resource;
import org.smartrplace.commontypes.BuildingEditPage;
import org.smartrplace.commontypes.BuildingTablePage;
import org.smartrplace.commontypes.HeatBillRegistration;
import org.smartrplace.commontypes.MasterUserRegistration;
import org.smartrplace.commontypes.RadiatorTypeRegistration;
import org.smartrplace.commontypes.RoomRadiatorRegistration;
import org.smartrplace.commontypes.RoomRegistration;
import org.smartrplace.commontypes.RoomWindowRegistration;
import org.smartrplace.commontypes.WindowTypeRegistration;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionCapability;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.extensionservice.SmartEffTimeSeries;
import org.smartrplace.smarteff.accesscontrol.AccessControlRegistration;
import org.smartrplace.smarteff.accesscontrol.AccessControlRegistration.EditPage;
import org.smartrplace.smarteff.accesscontrol.CrossUserBuildingTablePage;
import org.smartrplace.smarteff.util.NaviPageBase;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericWithTable;
import org.smartrplace.smarteff.util.editgeneric.GenericResourceByTypeTablePageBase;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.base.SmartEffGeneralData;
import extensionmodel.smarteff.api.base.SmartEffPriceData;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.api.common.BuildingUnit;
import extensionmodel.smarteff.api.common.HeatCostBillingInfo;
import extensionmodel.smarteff.api.common.HeatRadiator;
import extensionmodel.smarteff.api.common.HeatRadiatorType;
import extensionmodel.smarteff.api.common.Window;
import extensionmodel.smarteff.api.common.WindowType;
import extensionmodel.smarteff.defaultproposal.BuildingExampleAnalysis;
import extensionmodel.smarteff.defaultproposal.DefaultProviderParamsPage;
import extensionmodel.smarteff.defaultproposal.PriceDataParamsPage;

@Service(SmartEffExtensionService.class)
@Component
public class BaseDataService implements SmartEffExtensionService {
	//private ApplicationManagerSPExt appManExt;
	
	public final static ExtensionResourceTypeDeclaration<SmartEffResource> BUILDING_DATA = new ExtensionResourceTypeDeclaration<SmartEffResource>() {

		@Override
		public Class<? extends SmartEffResource> dataType() {
			return BuildingData.class;
		}

		@Override
		public String label(OgemaLocale locale) {
			return "Building Data";
		}

		@Override
		public Class<? extends SmartEffResource> parentType() {
			return null;
		}

		@Override
		public Cardinality cardinality() {
			return Cardinality.MULTIPLE_OPTIONAL;
		}
	};
	public final static ExtensionResourceTypeDeclaration<SmartEffResource> PRICE_DATA = new ExtensionResourceTypeDeclaration<SmartEffResource>() {

		@Override
		public Class<? extends SmartEffResource> dataType() {
			return SmartEffPriceData.class;
		}

		@Override
		public String label(OgemaLocale locale) {
			return "Basic Price Data";
		}

		@Override
		public Class<? extends SmartEffResource> parentType() {
			return SmartEffGeneralData.class;
		}

		@Override
		public Cardinality cardinality() {
			return Cardinality.SINGLE_VALUE_REQUIRED;
		}
		
	};
	public final static RoomRegistration.TypeDeclaration ROOM_TYPE = new RoomRegistration.TypeDeclaration();
	public final static RadiatorTypeRegistration.TypeDeclaration RADIATOR_TYPE = new RadiatorTypeRegistration.TypeDeclaration();
	public final static RoomRadiatorRegistration.TypeDeclaration ROOMRAD_TYPE = new RoomRadiatorRegistration.TypeDeclaration();

	public final static WindowTypeRegistration.TypeDeclaration WINDOW_TYPE_REGISTRATION
			= new WindowTypeRegistration.TypeDeclaration();
	public final static RoomWindowRegistration.TypeDeclaration ROOM_WINDOW_TYPE_REGISTRATION
			= new RoomWindowRegistration.TypeDeclaration();
	
	/*public final static org.smartrplace.smarteff.defaultservice.BuildingTablePage.Provider BUILDING_NAVI_PROVIDER = new BuildingTablePage().provider;
	public final static org.smartrplace.smarteff.defaultservice.BuildingEditPage.Provider BUILDING_EDIT_PROVIDER = new BuildingEditPage().provider;
	*/
	public final static NaviPageBase<Resource>.Provider RESOURCE_NAVI_PROVIDER = new ResourceTablePage().provider;
	public final static NaviPageBase<Resource>.Provider RESOURCEALL_NAVI_PROVIDER = new ResourceAllTablePage().provider;
	public final static NaviPageBase<Resource>.Provider PROPOSALTABLE_PROVIDER = new LogicProvTablePage().provider;
	public final static NaviPageBase<Resource>.Provider RESULTTABLE_PROVIDER = new ResultTablePage().provider;
	//This one opens as start page (shows alls resources of type in UserData)
	public final static NaviPageBase<Resource>.Provider RESBYTYPE_PROVIDER = new ResourceByTypeTablePage().provider;
	//public final static NaviPageBase<Resource>.Provider RESSUBBYTYPE_PROVIDER = new ResourceSubByTypeTablePage().provider;
	//This one opens for a certain resources, requires a resource as entry point
	public final static NaviPageBase<Resource>.Provider RESBYTYPE_ENTRYPOINT_PROVIDER = new GenericResourceByTypeTablePageBase().provider;

	public final static EditPageGenericWithTable<HeatCostBillingInfo> BILLEDIT = new HeatBillRegistration.EditPage();
	public final static GenericResourceByTypeTablePageBase BILLTABLE = BILLEDIT.getTablePage();
	public final static EditPageGenericWithTable<BuildingUnit> ROOMEDIT = new RoomRegistration.EditPage();
	public final static GenericResourceByTypeTablePageBase ROOMTABLE = ROOMEDIT.getTablePage();
		
	public final static NaviPageBase<SmartEffTimeSeries>.Provider TSMAN_EDIT = new TSManagementPage().provider;
	
	private final static EditPageGenericWithTable<HeatRadiatorType> RADIATOR_PAGE = new RadiatorTypeRegistration.EditPage();
	private final static GenericResourceByTypeTablePageBase RADIATOR_TABLE = RADIATOR_PAGE.getTablePage();
	private final static EditPageGenericWithTable<HeatRadiator> ROOMRAD_PAGE = new RoomRadiatorRegistration.EditPage();
	private final static GenericResourceByTypeTablePageBase ROOMRAD_TABLE = ROOMRAD_PAGE.getTablePage();

	private final static EditPageGenericWithTable<WindowType> WINDOW_PAGE =
			new WindowTypeRegistration.EditPage();
	private final static GenericResourceByTypeTablePageBase WINDOW_TABLE =
			WINDOW_PAGE.getTablePage();
	private final static EditPageGenericWithTable<Window> ROOM_WINDOW_PAGE =
			new RoomWindowRegistration.EditPage();
	private final static GenericResourceByTypeTablePageBase ROOM_WINDOW_TABLE =
			ROOM_WINDOW_PAGE.getTablePage();

	//public final static NaviPageBase<DefaultProviderParams>.Provider BA_PARAMSEDIT_PROVIDER = new DefaultProviderParamsPage().provider;
	//public final static NaviPageBase<Resource>.Provider TOPCONFIG_NAVI_PROVIDER = new TopConfigTablePage().provider;
	public BuildingExampleAnalysis BUILDINGANALYSIS_PROVIDER;
	public final static EditPage ACCESS_EDIT = new AccessControlRegistration.EditPage();
	public final static CrossUserBuildingTablePage CROSSUSERBUILDING_TABLE = new CrossUserBuildingTablePage();

	@Override
	public void start(ApplicationManagerSPExt appManExt) {
		//this.appManExt = appManExt;
		BUILDINGANALYSIS_PROVIDER = new BuildingExampleAnalysis(appManExt);
	}

	@Override
	public void stop(AppStopReason reason) {
	}

	@Override
	public Collection<ExtensionCapability> getCapabilities() {
		return Arrays.asList(new ExtensionCapability[] {new BuildingTablePage().provider, new BuildingEditPage().provider, RESOURCE_NAVI_PROVIDER, RESOURCEALL_NAVI_PROVIDER,
				PROPOSALTABLE_PROVIDER, RESULTTABLE_PROVIDER, new TopConfigTablePage().provider,
				BUILDINGANALYSIS_PROVIDER, new DefaultProviderParamsPage().provider,
				new MasterUserRegistration.EditPage().provider,
				ROOMEDIT.provider, ROOMTABLE.provider,
				RESBYTYPE_PROVIDER, RESBYTYPE_ENTRYPOINT_PROVIDER,
				//RESSUBBYTYPE_PROVIDER,
				BILLEDIT.provider, BILLTABLE.provider, ACCESS_EDIT.provider,
				CROSSUSERBUILDING_TABLE.provider,
				TSMAN_EDIT,
				RADIATOR_PAGE.provider, RADIATOR_TABLE.provider, ROOMRAD_PAGE.provider, ROOMRAD_TABLE.provider,
				WINDOW_PAGE.provider, WINDOW_TABLE.provider, ROOM_WINDOW_PAGE.provider, ROOM_WINDOW_TABLE.provider,
				new PriceDataParamsPage().provider});
	}

	@Override
	public Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> resourcesDefined() {
		Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> result = 
				new ArrayList<>();
		result.add(BUILDING_DATA);
		result.add(PRICE_DATA);
		result.add(BUILDINGANALYSIS_PROVIDER.getResultTypeDeclaration());
		if(BUILDINGANALYSIS_PROVIDER.getParamTypeDeclaration() != null) result.add(BUILDINGANALYSIS_PROVIDER.getParamTypeDeclaration());
		result.add(new MasterUserRegistration.TypeDeclaration());
		result.add(ROOM_TYPE);
		result.add(new HeatBillRegistration.TypeDeclaration());
		result.add(RADIATOR_TYPE);
		result.add(ROOMRAD_TYPE);
		result.add(WINDOW_TYPE_REGISTRATION);
		result.add(ROOM_WINDOW_TYPE_REGISTRATION);
		result.add(new AccessControlRegistration.TypeDeclaration());
		return result ;
	}
}
