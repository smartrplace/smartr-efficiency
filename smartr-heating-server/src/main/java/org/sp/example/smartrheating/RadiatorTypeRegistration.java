package org.sp.example.smartrheating;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericWithTable;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.smartrheating.SHeatRadiatorType;
import extensionmodel.smarteff.smartrheating.SmartrHeatingData;

public class RadiatorTypeRegistration {
	public static final Class<? extends SmartEffResource> TYPE_CLASS = SHeatRadiatorType.class;
	
	public static class TypeDeclaration implements ExtensionResourceTypeDeclaration<SmartEffResource> {

		@Override
		public Class<? extends SmartEffResource> dataType() {
			return TYPE_CLASS;
		}

		@Override
		public String label(OgemaLocale locale) {
			return "Radiator type data";
		}

		@Override
		public Class<? extends SmartEffResource> parentType() {
			return SmartrHeatingData.class;
		}

		@Override
		public Cardinality cardinality() {
			return Cardinality.MULTIPLE_OPTIONAL;
		}
	}
	
	public static class EditPage extends EditPageGenericWithTable<SHeatRadiatorType> {
		@Override
		public void setData(SHeatRadiatorType sr) {
			setHeaderLabel(EN, "Radiator Type Data", DE, "Daten Thermostattype im Gebäude");
			setLabel(sr.numberOfRadiators(), EN, "Number of radiators of this type in building",
					DE, "Zahl der Thermostate des Typs im Gebäude");
			setTableHeader(sr.numberOfRadiators(), EN, "# in building",
					DE, "# im Gebäude");
			setLabel(sr.radiatorDescription(), EN, "Radiator description");
			setTableHeader(sr.radiatorDescription(), EN, "Description");
			setLabel(sr.radiatorPictureURLs(), EN, "Picture URLs");
		}
		
		@SuppressWarnings("unchecked")
		@Override
		protected Class<SHeatRadiatorType> primaryEntryTypeClass() {
			return (Class<SHeatRadiatorType>) TYPE_CLASS;
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
