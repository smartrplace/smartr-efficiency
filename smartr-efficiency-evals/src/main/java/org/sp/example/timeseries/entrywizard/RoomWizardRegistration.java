package org.sp.example.timeseries.entrywizard;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericWithTable;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.common.BuildingData;
import extensionmodel.smarteff.api.common.BuildingUnit;

public class RoomWizardRegistration {
	public static final Class<? extends SmartEffResource> TYPE_CLASS = BuildingUnit.class; //RoomData.class;
	
	public static class TypeDeclaration implements ExtensionResourceTypeDeclaration<SmartEffResource> {

		@Override
		public Class<? extends SmartEffResource> dataType() {
			return TYPE_CLASS;
		}

		@Override
		public String label(OgemaLocale locale) {
			return "Room Data";
		}

		@Override
		public Class<? extends SmartEffResource> parentType() {
			return BuildingData.class;
		}

		@Override
		public Cardinality cardinality() {
			return Cardinality.MULTIPLE_OPTIONAL;
		}
	}
	
	public static class EditPage extends EditPageGenericWithTable<BuildingUnit> {
		@Override
		public void setData(BuildingUnit sr) {
			setHeaderLabel(EN, "Room Data", DE, "Raumdaten");
			//setHeaderLink(EN, "https://en.wikipedia.org/wiki/Master_data");
			setLabel(sr.name(), EN, "Name",
					DE, "Voller Name");
			setTableHeader(sr.name(), EN, "Name");
			setLabel(sr.groundArea(), EN, "Ground Area",
					DE, "Nutzfläche");
			setTableHeader(sr.groundArea(), EN, "Ground Area", DE, "Nutzfläche");
		}

		@SuppressWarnings("unchecked")
		@Override
		public Class<BuildingUnit> primaryEntryTypeClass() {
			return (Class<BuildingUnit>) TYPE_CLASS;
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
