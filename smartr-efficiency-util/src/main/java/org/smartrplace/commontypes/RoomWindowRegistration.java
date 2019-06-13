package org.smartrplace.commontypes;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericWithTable;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingUnit;
import extensionmodel.smarteff.api.common.Window;

public class RoomWindowRegistration {
	public static final Class<? extends SmartEffResource> TYPE_CLASS = Window.class;
	
	public static class TypeDeclaration implements ExtensionResourceTypeDeclaration<SmartEffResource> {

		@Override
		public Class<? extends SmartEffResource> dataType() {
			return TYPE_CLASS;
		}

		@Override
		public String label(OgemaLocale locale) {
			return "Room Window";
		}

		@Override
		public Class<? extends SmartEffResource> parentType() {
			return BuildingUnit.class;
		}

		@Override
		public Cardinality cardinality() {
			return Cardinality.MULTIPLE_OPTIONAL;
		}
	}
	
	public static class EditPage extends EditPageGenericWithTable<Window> {
		@Override
		public void setData(Window sr) {
			setLabel(sr.type(), EN, "Window type",
					DE, "Fenstertyp");
		}

		@SuppressWarnings("unchecked")
		@Override
		protected Class<Window> primaryEntryTypeClass() {
			return (Class<Window>) TYPE_CLASS;
		}
		
		@Override
		protected String getMaintainer() {
			return "test1";
		}
	}
}
