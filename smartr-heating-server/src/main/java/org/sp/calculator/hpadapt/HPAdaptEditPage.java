package org.sp.calculator.hpadapt;


import java.util.HashMap;
import java.util.Map;

import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.html.form.button.Button;
import extensionmodel.smarteff.hpadapt.HPAdaptData;

public class HPAdaptEditPage extends EditPageGeneric<HPAdaptData> {
	@Override
	public String label(OgemaLocale locale) {
		return "Bivalent heat pumpt calculator edit page";
	}

	@Override
	protected Class<HPAdaptData> primaryEntryTypeClass() {
		return HPAdaptData.class;
	}

	public static final Map<String, String> PRICE_TYPE_MAP_EN = new HashMap<>();
	public static final Map<String, String> PRICE_TYPE_MAP_DE = new HashMap<>();

	static {
		for (int i = 0; i < HPAdaptData.PRICE_TYPE_NAMES_EN.length; i++) {
			PRICE_TYPE_MAP_EN.put(Integer.toString(i), HPAdaptData.PRICE_TYPE_NAMES_EN[i]);
		}
		for (int i = 0; i < HPAdaptData.PRICE_TYPE_NAMES_DE.length; i++) {
			PRICE_TYPE_MAP_DE.put(Integer.toString(i), HPAdaptData.PRICE_TYPE_NAMES_DE[i]);
		}
	}
	
	@Override
	protected void addWidgets() {
		super.addWidgets();
		Button setDefaults = createDefaultsButton(DefaultSetModes.SET_IF_NEW, "Fill empty resources");
		page.append(setDefaults);
	}
	
	@Override
	public void setData(HPAdaptData data) {
		setLabel(data.name(), EN, "name", DE, "Name");

		setLabelWithUnit(data.roomHeight(),
				EN, "Default room height in building",
				DE, "Standard-Raumhöhe im Gebäude");

		setLabelWithUnit(data.savingsAfterBasicRenovation(),
				EN, "Estimated savings after basic renovation",
				DE, "Geschätzte Einsparung durch einfache Sanierung");

		setLabelWithUnit(data.wwConsumption(),
				EN, "Known or estimated warm drinking water consumption",
				DE, "Bekannter oder geschätzter Trinkwarmwasserverbrauch");

		setLabelWithUnit(data.wwLossHeatedAreas(),
				EN, "Estimated warm water energy loss from storage, circulation at current temperature in heated areas",
				DE, "Geschätzter Warmwasser-Energieverlost duch Speicher, Zirkulation"
						+ "bei aktueller Temperatur im beheizten Bereichen");

		setLabelWithUnit(data.wwLossUnheatedAreas(),
				EN, "Warm water energy loss in unheated areas",
				DE, "Warmwasser-Energieverlust im unbeheizten Bereichen");

		setLabelWithUnit(data.wwTemp(),
				EN, "Warm water temperature",
				DE, "Warmwassertemperatur");

		setLabelWithUnit(data.wwTempMin(),
				EN, "Warm water temperature can be lowered to",
				DE, "Warmwassertemperatur kann gesenkt werden auf");

		setLabelWithUnit(data.heatingLimitTemp(),
				EN, "Heating limit temperature",
				DE, "Heizgrenztemperatur");

		setLabelWithUnit(data.outsideDesignTemp(),
				EN, "Outside design temperature",
				DE, "Außentemperatur-Auslegung",
				-40, 50);

		setLabelWithUnit(data.savingsFromCDBoiler(),
				EN, "Estimated savings from condensing boiler",
				DE, "Geschätzte Einsparung durch Brennwertkessel");

		setLabelWithUnit(data.dimensioningForPriceType(),
				EN, "Dimensioning for price type",
				DE, "Auslegung auf Preistyp");
		setDisplayOptions(data.dimensioningForPriceType(), EN, PRICE_TYPE_MAP_EN);
		setDisplayOptions(data.dimensioningForPriceType(), DE, PRICE_TYPE_MAP_DE);

		setLabelWithUnit(data.uValueBasementFacade(),
				EN, "U-Value basement in relation to U-Value facade (equal = 1.0)",
				DE, "U-Wert Keller im Verhältnis zum U-Wert Fassade (gleich = 1.0)");

		setLabelWithUnit(data.uValueRoofFacade(),
				EN, "U-Value roof in relation to U-Value facade (equal = 1.0)",
				DE, "U-Wert Dach im Verhältnis zum U-Wert Fassade (gleich = 1.0)");

		setLabelWithUnit(data.innerWallThickness(),
				EN, "Thickness of inner walls",
				DE, "Dicke der Innenwände");

		setLabelWithUnit(data.basementTempHeatingSeason(),
				EN, "Basement temperature during heating season",
				DE, "Kellertemperatur in der Heizperiode");
		
		setLabel(data.outsideTempOffset(),
				EN, "Offset for adapting to historical outside temperature data (K)",
				DE, "Offset für Adaption der Außentemperaturdaten (K)",
				-30, 30);
		
		setLabelWithUnit(data.comfortTemp(),
				EN, "Comfort temperature",
				DE, "Komfort-Temperatur");
		
		setLabelWithUnit(data.roofAreaForPV(),
				EN, "Roof area (not north) for PV",
				DE, "Dachfläche nicht-Nord für PV");
		
		/* Documentation Links */
		setHeaderLink(EN, HPAdaptEval.WIKI_LINK + "#data");
		setLink(data.roomHeight(), EN, HPAdaptEval.WIKI_LINK +  "#default-room-height-in-building");
		setLink(data.savingsAfterBasicRenovation(), EN, HPAdaptEval.WIKI_LINK +  "#estimated-savings-after-basic-renovation");
		setLink(data.wwConsumption(), EN, HPAdaptEval.WIKI_LINK +  "#known-or-estimated-warm-drinking-water-consumption");
		//setLink(data.wwLossHeatedAreas(), EN, HPAdaptEval.WIKI_LINK +  "#estimated-warm-water-energy-loss-from-storage,-circulation-at-current-temperature-in-heated-areas");
		//setLink(data.wwLossUnheatedAreas(), EN, HPAdaptEval.WIKI_LINK +  "#warm-water-energy-loss-in-unheated-areas");
		//setLink(data.wwTemp(), EN, HPAdaptEval.WIKI_LINK +  "#warm-water-temperature");
		//setLink(data.wwTempMin(), EN, HPAdaptEval.WIKI_LINK +  "#warm-water-temperature-can-be-lowered-to");
		setLink(data.heatingLimitTemp(), EN, HPAdaptEval.WIKI_LINK +  "#heating-limit-temperature");
		//setLink(data.outsideDesignTemp(), EN, HPAdaptEval.WIKI_LINK +  "#outside-design-temperature");
		//setLink(data.savingsFromCDBoiler(), EN, HPAdaptEval.WIKI_LINK +  "#estimated-savings-from-condensing-boiler");
		//setLink(data.dimensioningForPriceType(), EN, HPAdaptEval.WIKI_LINK +  "#dimensioning-for-price-type");
		//setLink(data.uValueBasementFacade(), EN, HPAdaptEval.WIKI_LINK +  "#u-value-basement-in-relation-to-u-value-facade-(equal-=-1.0)");
		//setLink(data.uValueRoofFacade(), EN, HPAdaptEval.WIKI_LINK +  "#u-value-roof-in-relation-to-u-value-facade-(equal-=-1.0)");
		//setLink(data.innerWallThickness(), EN, HPAdaptEval.WIKI_LINK +  "#thickness-of-inner-walls");
		//setLink(data.basementTempHeatingSeason(), EN, HPAdaptEval.WIKI_LINK +  "#basement-temperature-during-heating-season");
		setLink(data.outsideTempOffset(), EN, HPAdaptEval.WIKI_LINK +  "#offset-for-adapting-to-historical-outside-temperature-data-(k)");
		
	}
	

	@Override
	protected void defaultValues(HPAdaptData data, DefaultSetModes mode) {
		setDefault(data.roomHeight(), 2.8f, mode);
		setDefault(data.savingsAfterBasicRenovation(), 15f, mode);
		setDefault(data.wwConsumption(), 21f, mode);
		setDefault(data.wwLossHeatedAreas(), 35f, mode);
		setDefault(data.wwLossUnheatedAreas(), 25f, mode);
		setDefault(data.wwTemp(), 40f, mode);
		setDefault(data.wwTempMin(), 40f, mode);
		setDefault(data.heatingLimitTemp(), 12f, mode);
		setDefault(data.outsideDesignTemp(), -20f, mode);
		setDefault(data.savingsFromCDBoiler(), 10f, mode);
		setDefault(data.dimensioningForPriceType(), HPAdaptData.PRICE_TYPE_CO2_NEUTRAL, mode);
		setDefault(data.uValueBasementFacade(), 3f, mode);
		setDefault(data.uValueRoofFacade(), .9f, mode);
		setDefault(data.innerWallThickness(), .25f, mode);
		setDefault(data.basementTempHeatingSeason(), 8f, mode);
		setDefault(data.outsideTempOffset(), 0f, mode);
		setDefault(data.comfortTemp(), 20f, mode);
		setDefault(data.roofAreaForPV(), 40f, mode);
	}


}
