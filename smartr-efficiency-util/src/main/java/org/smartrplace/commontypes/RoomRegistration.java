package org.smartrplace.commontypes;

import org.ogema.core.model.units.AreaResource;
import org.ogema.core.model.units.VolumeResource;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.smarteff.util.editgeneric.EditPageGeneric;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.common.BuildingData;

public class RoomRegistration {
	public static final Class<? extends SmartEffResource> TYPE_CLASS = RoomData.class;
	
	public static interface RoomData extends SmartEffResource {
		AreaResource groundArea();
		VolumeResource volume();
		AreaResource outsideWindowArea();
		/**Including window area*/
		AreaResource totalOutsideWallArea();
	}
	
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
	
	public static class EditPage extends EditPageGeneric<RoomData> {
		@Override
		public void setData(RoomData sr) {
			setHeaderLabel(EN, "Room Data", DE, "Raumdaten");
			//setHeaderLink(EN, "https://en.wikipedia.org/wiki/Master_data");
			setLabel(sr.name(), EN, "Name",
					DE, "Voller Name");
			setLabel(sr.groundArea(), EN, "Ground Area",
					DE, "Nutzfläche");
		}

		@SuppressWarnings("unchecked")
		@Override
		protected Class<RoomData> primaryEntryTypeClass() {
			return (Class<RoomData>) TYPE_CLASS;
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
