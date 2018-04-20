package org.smartrplace.commontypes;

import java.util.HashMap;
import java.util.Map;

import org.ogema.core.model.simple.IntegerResource;
import org.smartrplace.smarteff.util.editgeneric.EditLineProvider;
import org.smartrplace.smarteff.util.editgeneric.EditLineProviderDisabling;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;
import org.smartrplace.util.format.ValueConverter;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.textfield.TextField;
import extensionmodel.smarteff.api.common.BuildingData;

public class BuildingEditPage extends EditPageGeneric<BuildingData> {
	public static final Map<String, String> BTYPEMAP_EN = new HashMap<>();
	public static final Map<String, String> BTYPEMAP_DE = new HashMap<>();
	public static final Map<String, String> USERTYPEMAP_EN = new HashMap<>();
	public static final Map<String, String> USERTYPEMAP_DE = new HashMap<>();
	
	public static final Map<String, String> HEATTYPEMAP_EN = new HashMap<>();
	public static final Map<String, String> HEATTYPEMAP_DE = new HashMap<>();
	public static final Map<String, String> RECSTATMAP_EN = new HashMap<>();
	public static final Map<String, String> RECSTATMAP_DE = new HashMap<>();

	static {
		BTYPEMAP_EN.put("1", "single family home (SFH)");
		BTYPEMAP_EN.put("2", " multi family home< (MFH)");
		BTYPEMAP_EN.put("3", "apartment");
		BTYPEMAP_EN.put("4", "office building");
		BTYPEMAP_EN.put("5", "other commercial building");
		BTYPEMAP_EN.put("10", "school");
		BTYPEMAP_EN.put("11", "gym");
		BTYPEMAP_EN.put("12", "lecture hall, conference center, theater etc.");
		BTYPEMAP_EN.put("20", "other");
		BTYPEMAP_DE.put("1", "Einfamilienhaus (EFH)");
		BTYPEMAP_DE.put("2", " Mehrfamilienhaus (MFH)");
		BTYPEMAP_DE.put("3", "Wohnung");
		BTYPEMAP_DE.put("4", "Bürogebäude");
		BTYPEMAP_DE.put("5", "Sonstiges gewerbliches Gebäude");
		BTYPEMAP_DE.put("10", "Schule");
		BTYPEMAP_DE.put("11", "Turnhalle");
		BTYPEMAP_DE.put("12", "Veranstaltungsgebäude, Theater etc.");
		BTYPEMAP_DE.put("20", "Sonstiges");

		USERTYPEMAP_EN.put("1", "owner");
		USERTYPEMAP_EN.put("2", "tenant");
		USERTYPEMAP_EN.put("3", "property manager");
		USERTYPEMAP_DE.put("1", "Eigentümer");
		USERTYPEMAP_DE.put("2", "Mieter");
		USERTYPEMAP_DE.put("3", "Gebäudeverwalter");
		
		HEATTYPEMAP_EN.put("1", "L gas");
		HEATTYPEMAP_EN.put("2", "H gas");
		HEATTYPEMAP_EN.put("10", "oil");
	    HEATTYPEMAP_EN.put("11", "charcoal");
	    HEATTYPEMAP_EN.put("12", "lignite");
	    HEATTYPEMAP_EN.put("13", "wood pellets");
	    HEATTYPEMAP_EN.put("14", "dry wood");
	    HEATTYPEMAP_EN.put("20", "district heating");
	    HEATTYPEMAP_EN.put("21", "building-internal heat meter");
	    HEATTYPEMAP_EN.put("30", "heat pump");
	    HEATTYPEMAP_EN.put("31", "night storage heating");
	    HEATTYPEMAP_EN.put("32", "direct electric heating");
		HEATTYPEMAP_DE.put("1", "L gas");
		HEATTYPEMAP_DE.put("2", "H gas");
		HEATTYPEMAP_DE.put("10", "Öl");
	    HEATTYPEMAP_DE.put("11", "Steinkohle");
	    HEATTYPEMAP_DE.put("12", "Braunkohle");
	    HEATTYPEMAP_DE.put("13", "Holzpellets");
	    HEATTYPEMAP_DE.put("14", "Holz");
	    HEATTYPEMAP_DE.put("20", "Fernwärme");
	    HEATTYPEMAP_DE.put("21", "Gebäude-interner Wärmemengenzähler ohne weitere Information");
	    HEATTYPEMAP_DE.put("30", "Wärmepumpe");
	    HEATTYPEMAP_DE.put("31", "Nachtspeicherheizung");
	    HEATTYPEMAP_DE.put("32", "Elektro-Direktheizung");

		RECSTATMAP_EN.put("1", "Not reconstructed");
		RECSTATMAP_EN.put("2", "Partially reconstructed");
		RECSTATMAP_EN.put("3", "Fully reconstructed to the state-of-the-art of the year of reconstruction");
		RECSTATMAP_DE.put("1", "nicht saniert");
		RECSTATMAP_DE.put("2", "teilsaniert");
		RECSTATMAP_DE.put("3", "vollsaniert auf den Stand der Technik des Sanierungsjahres");
	}
	
	@Override
	public void setData(BuildingData sr) {
		setLabel(sr.name(), EN, "Name");
		setLabel(sr.typeOfBuilding(), EN, "Type of Building", DE, "Art des Gebäudes");
		setDisplayOptions(sr.typeOfBuilding(), EN, BTYPEMAP_EN);
		setDisplayOptions(sr.typeOfBuilding(), DE, BTYPEMAP_DE);
		setTriggering(sr.typeOfBuilding());
		
		setLabel(sr.typeOfUser(), EN, "I am", DE, "Ich bin");
		setDisplayOptions(sr.typeOfUser(), EN, USERTYPEMAP_EN);
		setDisplayOptions(sr.typeOfUser(), DE, USERTYPEMAP_DE);
		
		// Example how to insert a special widget into the automated generated page
		// In this case also the standard intEdit could be generated and an EditLineProviderDisabling
		// be set, see provider for yearOfReconstruction below
		ValueConverter checker = new ValueConverter("numberOfUnitsInBuilding", alert, 1f, 99999f);
		TextField numberOfUnitsInBuildingEdit = new TextField(page, "numberOfUnitsInBuildingEdit") {
			private static final long serialVersionUID = 1L;
			private IntegerResource getResource(OgemaHttpRequest req) {
				return getReqData(req).numberOfUnitsInBuilding();
			}

			@Override
			public void onGET(OgemaHttpRequest req) {
				IntegerResource source = getResource(req);
				setValue(source.getValue()+"",req);
				int typeBuilding = getReqData(req).typeOfBuilding().getValue();
				if(typeBuilding == 1) disable(req);
				else enable(req);
			}

			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				IntegerResource source = getResource(req);
				String val = getValue(req);
				Integer value = checker.checkNewValueInt(val, req);
				if(value == null) return;
				if(!source.exists()) {
					source.create();
					source.setValue(value);
					source.activate(true);
				} else {
					source.setValue(value);
				}
			}
		};
		//Dependency is registered automatically for all widgets
		//numberOfUnitsInBuildingEdit.registerDependentWidget(checker.getAlert());
		EditLineProvider nrRoomProv = new EditLineProvider() {
			@Override
			public OgemaWidget valueColumn() {
				return numberOfUnitsInBuildingEdit;
			}
		};
		setLineProvider(sr.numberOfUnitsInBuilding(), nrRoomProv);
		//We still have to register the item and provide the label info, make sure it is inserted into list of labels
		setLabel(sr.numberOfUnitsInBuilding(), EN, "Number of tenants/property units in building", DE, "Zahl der Wohnungen/Nutzereinheiten im Gebäude", 1, 99999);
		
		
		setLabel(sr.roomNum(), EN, "Number of rooms in building", DE, "Zahl der Räume im Gebäude");
		
		setLabel(sr.address().street(), DE, "Straße und Hausnummer", EN, "Street / number");
		setLabel(sr.address().postalCode(), EN, "Postal Code", DE, "Postleitzahl");
		setLabel(sr.address().city(), EN, "City", DE, "Ort");
		setLabel(sr.address().country(), EN, "Country", DE, "Land");
		setDisplayOptions(sr.address().country(), EN, MasterUserRegistration.COUNTRYMAP_EN);
		setDisplayOptions(sr.address().country(), DE, MasterUserRegistration.COUNTRYMAP_DE);

		setLabel(sr.yearOfConstruction(), EN, "Year of construction", DE, "Baujahr");
		setLabel(sr.reconstructionStatus(), EN, "The building was", DE, "Das Gebäude wurde");
		setTriggering(sr.reconstructionStatus());
		setDisplayOptions(sr.reconstructionStatus(), EN, RECSTATMAP_EN);
		setDisplayOptions(sr.reconstructionStatus(), DE, RECSTATMAP_DE);
		setLabel(sr.yearOfReconstruction(), EN, "in year", DE, "im Jahr");
		setLineProvider(sr.yearOfReconstruction(), new EditLineProviderDisabling() {
			@Override
			protected boolean enable(OgemaHttpRequest req) {
				BuildingData res = getReqData(req);
				if(res.reconstructionStatus().getValue() < 2) return false;
				return true;
			}
		});
		
		setLabel(sr.heatedLivingSpace(), EN, "Heated Ground Floor in sqm", DE, "Beheizte Fläche m2");
		setLabel(sr.heatSource(), EN, "Type of Heating", DE, "Art der Heizung");
		setDisplayOptions(sr.heatSource(), EN, HEATTYPEMAP_EN);
		setDisplayOptions(sr.heatSource(), DE, HEATTYPEMAP_DE);
		setLabel(sr.coGeneration(), EN, "Cogeneration Device", DE, "KWK-Anlage");
		setLabel(sr.heatCostBillingInfo(), EN, "Edit Bills", DE, "Heizkostenabrechnungen");
	}

	@Override
	protected Class<BuildingData> primaryEntryTypeClass() {
		return BuildingData.class;
	}
	
	@Override //optional
	public String label(OgemaLocale locale) {
		return "Standard Building Edit Page";
	}
	
	@Override
	public boolean checkResource(BuildingData res) {
		ValueResourceHelper.setIfNew(res.typeOfBuilding(), 4);
		ValueResourceHelper.setIfNew(res.typeOfUser(), 1);
		ValueResourceHelper.setIfNew(res.numberOfUnitsInBuilding(), 1);
		ValueResourceHelper.setIfNew(res.address().country(), 49);
		ValueResourceHelper.setIfNew(res.heatedLivingSpace(), 10000);
		ValueResourceHelper.setIfNew(res.yearOfConstruction(), 1970);
		ValueResourceHelper.setIfNew(res.reconstructionStatus(), 1);

		if(res.reconstructionStatus().getValue() > 1 &&
				res.yearOfReconstruction().getValue() <= res.yearOfConstruction().getValue()) return false;
		
		return super.checkResource(res);
	}
}
