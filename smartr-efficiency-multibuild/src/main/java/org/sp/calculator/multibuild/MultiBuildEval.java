package org.sp.calculator.multibuild;

import java.util.HashSet;
import java.util.Set;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.proposal.ProjectProposal;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.BaseInits;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.MyParam;
import org.smartrplace.smarteff.util.ProjectProviderBase;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.multibuild.BuildingComponent;
import extensionmodel.smarteff.multibuild.BuildingComponentUsage;
import extensionmodel.smarteff.multibuild.CommunicationBusType;
import extensionmodel.smarteff.multibuild.MultiBuildData;
import extensionmodel.smarteff.multibuild.MultiBuildParams;
import extensionmodel.smarteff.multibuild.MultiBuildParamsUser;
import extensionmodel.smarteff.multibuild.MultiBuildResult;

/** Currently this calculator is opened via a building, although it is a multi-building calculator.
 * Currently it does not use the buildings data, but it could use the data in the future. In this
 * case the building is used as a prototype for the multi-building setup.*/
public class MultiBuildEval extends ProjectProviderBase<MultiBuildData> {
	
	public static final String WIKI_LINK =
			"https://github.com/smartrplace/smartr-efficiency/blob/master/MultiBuilding.md";
	private static final String PROVIDING_USER = "master";
	
	@Override
	public String label(OgemaLocale locale) {
		return "Multi-building IoT Project";
	}

	@Override
	protected void calculateProposal(MultiBuildData hpData, ProjectProposal resultProposal,
			ExtensionResourceAccessInitData data) {
		
		if (!(resultProposal instanceof MultiBuildResult))
			throw new RuntimeException("Wrong Result type. Can't evaluate.");

		MultiBuildResult result = (MultiBuildResult) resultProposal;
		
		// Completely clear result
		result.delete();
		result.create();
		result.activate(true);
		
		// Set up names of results
		ValueResourceHelper.setCreate(result.name(),
				"Multi-building IoT project");
		
		calculateMultiBuild(hpData, result, data);
	}

	/**
	 * Calculate MultiBuild-specific result. Main evaluation function.
	 * Note: variables named_with_underscores indicate values that have yet to be added to their respective data models.
	 * @param hpData
	 * @param result
	 * @param dataExt
	 */
	public void calculateMultiBuild(MultiBuildData hpData, MultiBuildResult result,
			ExtensionResourceAccessInitData dataExt) {
		
		/* SETUP */

		MyParam<MultiBuildParamsUser> hpParamHelperUser =
				CapabilityHelper.getMyParams(MultiBuildParamsUser.class, dataExt.userData(), appManExt);
		MultiBuildParamsUser hpParamsUser = hpParamHelperUser.get();
		String internalUser = PROVIDING_USER;
		MultiBuildParams params = dataExt.getCrossuserAccess().getAccess("multiBuildParams", internalUser,
				MultiBuildParams.class, this);
		
		float costPerBuilding = params.costSPBox().getValue();
		
		//Build result table
		result.offerLineInit().delete();
		result.offerLineInit().create();
		result.offerLineRecurrent().delete();
		result.offerLineRecurrent().create();

		Set<CommunicationBusType> comAdapts = new HashSet<>();
		for(BuildingComponentUsage cpusage:hpData.buildingComponentUsage().getAllElements()) {
			BuildingComponent cp = setComponentType(cpusage, params); //cpusage.paramType();
			costPerBuilding += cpusage.number().getValue() * cp.cost().getValue();
			
			if(cpusage.number().getValue() > 0) comAdapts.add(cp.type().getLocationResource());
		}
		for(CommunicationBusType cbtype: comAdapts) {
			costPerBuilding += cbtype.cost().getValue();
		}
		ValueResourceHelper.setCreate(result.costPerBuilding(), costPerBuilding);
		float totalCost = hpData.buildingNum().getValue() * costPerBuilding +
				params.costProjectBase().getValue(); 
		ValueResourceHelper.setCreate(result.costOfProject(), totalCost);
		
		hpParamHelperUser.close();		
	}

	/* * * * * * * * * * * * * * * * * * * * * * *
	 *   PROJECT PROVIDER FUNCTIONS              *
	 * * * * * * * * * * * * * * * * * * * * * * */

	@Override
	protected Class<? extends ProjectProposal> getResultType() {
		return MultiBuildResult.class;
	}
	@Override
	protected Class<MultiBuildData> typeClass() {
		return MultiBuildData.class;
	}
	@Override
	public Class<? extends SmartEffResource> getParamType() {
		return MultiBuildParamsUser.class;
	}
	@Override
	public Class<? extends SmartEffResource> getInternalParamType() {
		return MultiBuildParams.class;
	}
	
	@Override
	protected boolean initParams(SmartEffResource paramsIn) {
		BaseInits.initSmartrEffPriceData(appManExt, this.getClass().getName());
		MultiBuildParamsUser params = (MultiBuildParamsUser) paramsIn;
		if (!params.communicationBusType().exists()) {
			params.communicationBusType().create();
		}
		if (!params.buildingComponent().exists()) {
			params.buildingComponent().create();
		}
		return false;
	}
	
	@Override
	protected boolean initInternalParams(SmartEffResource paramsIn) {
		MultiBuildParams params = (MultiBuildParams) paramsIn;
		
		if (!params.exists() || !params.isActive())
			params.create();

		// Perhaps move to properties?
		if (!params.communicationBusType().exists()) {
			params.communicationBusType().create();
			CommunicationBusType hm = initAddCommAdapter(params, "homematic", 50, 0,0, "https://www.elv.de/");
			CommunicationBusType wmbus = initAddCommAdapter(params, "wmbus", 40, 0, 5000, null);
			initAddCommAdapter(params, "rexo", 60, 0,0, null);
			initAddCommAdapter(params, "wlan", 50, 0,0, null);
			if (!params.buildingComponent().exists()) {
				params.buildingComponent().create();
				initAddBuildingComponent(params, "Inside temp/humidity sensor", 30, 0, hm, "https://www.elv.de/homematic-ip-temperatur-und-luftfeuchtigkeitssensor-innen.html");
				initAddBuildingComponent(params, "Outside temp/humidity sensor", 50, 0, hm, null);
				initAddBuildingComponent(params, "FlowMeter 1,5m3/h", 210, 0, wmbus, "https://www.energie-zaehler.com/epages/61422236.sf/de_DE/?ObjectPath=/Shops/61422236/Products/WMZ15F-CMF-IST");
			}
		}
		if(
				ValueResourceHelper.setIfNew(params.costSPBox(), 990f) |
				ValueResourceHelper.setIfNew(params.costProjectBase(), 9900f) |
				ValueResourceHelper.setIfNew(params.operationalCost(), 4000f)
		) {
			return true;
		}
		return false;
	}
	
	private CommunicationBusType initAddCommAdapter(MultiBuildParams params, String name, float cost,
			float yearlyCost, float development, String link) {
		CommunicationBusType item = params.communicationBusType().add();
		ValueResourceHelper.setCreate(item.name(), name);
		ValueResourceHelper.setCreate(item.cost(), cost);
		ValueResourceHelper.setCreate(item.yearlyCost(), yearlyCost);
		ValueResourceHelper.setCreate(item.development(), development);
		if(link != null) ValueResourceHelper.setCreate(item.link(), link);
		return item;
	}
	private void initAddBuildingComponent(MultiBuildParams params, String name, float cost,
			float yearlyCost, CommunicationBusType comBus, String link) {
		BuildingComponent item = params.buildingComponent().add();
		ValueResourceHelper.setCreate(item.name(), name);
		ValueResourceHelper.setCreate(item.cost(), cost);
		ValueResourceHelper.setCreate(item.yearlyCost(), yearlyCost);
		item.type().setAsReference(comBus);
		if(link != null) ValueResourceHelper.setCreate(item.link(), link);
	}
	
	public MultiBuildEval(ApplicationManagerSPExt appManExt) {
		super(appManExt);
	}
	
	@Override
	public String userName() {
		return "master";
	}

	/** If usage is not linked to building component identify the component and set link
	 * TODO: Add support for user-specific components*/
	public static BuildingComponent setComponentType(BuildingComponentUsage cpusage, MultiBuildParams params) {
		if(cpusage.paramType().exists())
			return cpusage.paramType();
		String name;
		if(cpusage.name().exists())
			name = cpusage.name().getValue();
		else name = cpusage.getName();
		for(BuildingComponent comp: params.buildingComponent().getAllElements()) {
			if(comp.name().getValue().equals(name)) {
				cpusage.paramType().setAsReference(comp);
				return comp;
			}
		}
		return null;
	}
}
