package org.smartrplace.commontypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserData;
import extensionmodel.smarteff.api.common.MasterUserData;

public class MasterUserRegistration {
	public static final Class<? extends SmartEffResource> TYPE_CLASS = MasterUserData.class;
	
	public static final Map<String, String> COUNTRYMAP_EN = new HashMap<>();
	public static final Map<String, String> COUNTRYMAP_DE = new HashMap<>();
	static {
		COUNTRYMAP_EN.put("31", "Netherlands");
		COUNTRYMAP_EN.put("32", "Belgium"); 
		COUNTRYMAP_EN.put("33", "France");
		COUNTRYMAP_EN.put("41", "Switzerland");
		COUNTRYMAP_EN.put("43", "Austria");
		COUNTRYMAP_EN.put("44", "UK");
		COUNTRYMAP_EN.put("45", "Denmark");
		COUNTRYMAP_EN.put("46", "Sweden");
		COUNTRYMAP_EN.put("48", "Poland");
		COUNTRYMAP_EN.put("49", "Germany");
		COUNTRYMAP_EN.put("352", "Luxemburg");
		COUNTRYMAP_EN.put("420", "Czechia");
		
		COUNTRYMAP_DE.put("31", "Niederlande");
		COUNTRYMAP_DE.put("32", "Belgien"); 
		COUNTRYMAP_DE.put("33", "Frankreich");
		COUNTRYMAP_DE.put("41", "Schewiz");
		COUNTRYMAP_DE.put("43", "Österreich");
		COUNTRYMAP_DE.put("44", "UK");
		COUNTRYMAP_DE.put("45", "Dänemark");
		COUNTRYMAP_DE.put("46", "Schweden");
		COUNTRYMAP_DE.put("48", "Polen");
		COUNTRYMAP_DE.put("49", "Deutschland");
		COUNTRYMAP_DE.put("352", "Luxemburg");
		COUNTRYMAP_DE.put("420", "Tschechien");
	}
	
	public static class TypeDeclaration implements ExtensionResourceTypeDeclaration<SmartEffResource> {

		@Override
		public Class<? extends SmartEffResource> dataType() {
			return TYPE_CLASS;
		}

		@Override
		public String label(OgemaLocale locale) {
			return "Master User Data";
		}

		@Override
		public Class<? extends SmartEffResource> parentType() {
			return null;
		}

		@Override
		public Cardinality cardinality() {
			return Cardinality.SINGLE_VALUE_REQUIRED;
		}
	}
	
	public static class EditPage extends EditPageGeneric<MasterUserData> {
		@Override
		public void setData(MasterUserData sr) {
			setHeaderLabel(EN, "User Master Data", DE, "Nutzerstammdaten");
			setHeaderLink(EN, "https://en.wikipedia.org/wiki/Master_data");
			setHeaderLink(DE, "https://de.wikipedia.org/wiki/Stammdaten");
			setLabel(sr.name(), EN, "Name",
					DE, "Voller Name");
			setLabel(sr.addressExtension(), EN, "c/o or contact persion",
					DE, "Adresszusatz");
			setLabel(sr.address().street(), DE, "Straße und Hausnummer", EN, "Street / number");
			setLabel(sr.address().postalCode(), EN, "Postal Code", DE, "Postleitzahl");
			setLabel(sr.address().city(), EN, "City", DE, "Ort");
			setLabel(sr.address().country(), EN, "Country", DE, "Land");
			setDisplayOptions(sr.address().country(), EN, COUNTRYMAP_EN);
			setDisplayOptions(sr.address().country(), DE, COUNTRYMAP_DE);
			setLabel(sr.emailAddress(), EN, "Email");
			setLabel(sr.phoneNumber(), EN, "Phone", DE, "Telefon");
			setLabel(sr.makeNamePublic(), EN, "Show Name to Public", DE, "Name öffentlich anzeigen");
			setLabel(sr.makeEmailPublic(), EN, "Show Email to Public", DE, "Emailadresse öffentlich anzeigen");
			setLabel(sr.makeAddressPublic(), EN, "Show Address Number to Public", DE, "Adresse öffentlich anzeigen");
			setLabel(sr.makePhonePublic(), EN, "Show Phone Number to Public", DE, "Telefonnummer öffentlich anzeigen");
		}

		@SuppressWarnings("unchecked")
		@Override
		protected Class<MasterUserData> primaryEntryTypeClass() {
			return (Class<MasterUserData>) TYPE_CLASS;
		}
		
		@Override
		protected String getMaintainer() {
			return "test1";
		}
		
		@Override
		protected String getHeader(OgemaHttpRequest req) {
			return getReqData(req).getParent().getParent().getName();
		}
		
		/**************** For Starter page configuration *********
		 */
		@Override
		protected List<EntryType> getEntryTypes() {
			return null;
		}
		protected List<EntryType> getEntryTypesSuper() {
			return super.getEntryTypes();
		}
		
		@Override
		protected MasterUserData getReqData(OgemaHttpRequest req) {
			ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
			SmartEffUserData userData = ((SmartEffUserData)appData.userData());
			return userData.masterUserData();
		}
		protected MasterUserData getReqDataSuper(OgemaHttpRequest req) {
			return super.getReqData(req);
		}

	}
}
