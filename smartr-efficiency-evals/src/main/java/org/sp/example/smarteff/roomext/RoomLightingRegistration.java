package org.sp.example.smarteff.roomext;

import java.util.LinkedHashMap;
import java.util.Map;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.common.BuildingUnit;
import extensionmodel.smarteff.basic.evals.RoomLightingData;

public class RoomLightingRegistration {
	public static final Class<? extends SmartEffResource> TYPE_CLASS = RoomLightingData.class;
	
	public static final Map<String, String> LITSIT_MAP_EN = new LinkedHashMap<>();
	public static final Map<String, String> LITSIT_MAP_DE = new LinkedHashMap<>();
	static {
		LITSIT_MAP_EN.put("0", "Lights off"); 
		LITSIT_MAP_EN.put("1", "Lights on and room is used"); 
		LITSIT_MAP_EN.put("2", "Lights on and room is not used (currently)"); 
		LITSIT_MAP_EN.put("3", "Lights on and room has obviously not been used for more than an hour");
		
		LITSIT_MAP_DE.put("0", "Lichter aus"); 
		LITSIT_MAP_DE.put("1", "Licht(er) an, Raum in Benutzung"); 
		LITSIT_MAP_DE.put("2", "Licht(er) an, Raum aber gegenwärtig nicht in Benutzung"); 
		LITSIT_MAP_DE.put("3", "Licht(er) an, Raum offensichtlich seit >1 Stunde nicht in Benutzung");
	}
	
	public static class TypeDeclaration implements ExtensionResourceTypeDeclaration<SmartEffResource> {

		@Override
		public Class<? extends SmartEffResource> dataType() {
			return TYPE_CLASS;
		}

		@Override
		public String label(OgemaLocale locale) {
			return "Room Lighting Extension Data";
		}

		@Override
		public Class<? extends SmartEffResource> parentType() {
			return BuildingUnit.class;
		}

		@Override
		public Cardinality cardinality() {
			return Cardinality.SINGLE_VALUE_OPTIONAL;
		}
	}
	
	public static class EditPage extends EditPageGeneric<RoomLightingData> {
		@Override
		public void setData(RoomLightingData sr) {
			setHeaderLabel(EN, "Room Lighting Data", DE, "Raumdaten Beleuchtung");
			//setHeaderLink(EN, "https://en.wikipedia.org/wiki/Master_data");
			setLabel(sr.lightNum(), EN, "Number of electrical lights in the room",
					DE, "Zahl der einzeln schaltbaren elektrischen Leuchten im Room");
			setLabel(sr.installedLightPower(), EN, "Total electrical power of lights in the room (W)",
					DE ,"Gesamtleistung der elektrischen Leuchten im Raum (W)");
			setLabel(sr.installedLuminousFlux(), EN, "Total Lumen of lights in the room (lm)",
					DE ,"Gesamtbeleuchtungsleistung in Lumen aller elektrischen Leuchten im Raum (lm)");
			setLabel(sr.lightingSituation(), EN, "Current lighting/usage situation",
					DE, "Aufnahme der aktuellen Beleuchtungs-/Nutzungssituation");
			setDisplayOptions(sr.lightingSituation(), EN, LITSIT_MAP_EN);
			setDisplayOptions(sr.lightingSituation(), DE, LITSIT_MAP_DE);
		}

		@SuppressWarnings("unchecked")
		@Override
		protected Class<RoomLightingData> primaryEntryTypeClass() {
			return (Class<RoomLightingData>) TYPE_CLASS;
		}
		
		@Override
		protected String getMaintainer() {
			return "test1";
		}
		
		@Override
		protected String getHeader(OgemaHttpRequest req) {
			return getReqData(req).getParent().getParent().getName();
		}
	}
}
