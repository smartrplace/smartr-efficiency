package org.smartrplace.commontypes;

import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.smarteff.util.editgeneric.EditPageGenericWithTable;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.common.BuildingUnit;
import extensionmodel.smarteff.api.common.HeatRadiator;

public class RoomRadiatorRegistration {
	public static final Class<? extends SmartEffResource> TYPE_CLASS = HeatRadiator.class;
	
	public static class TypeDeclaration implements ExtensionResourceTypeDeclaration<SmartEffResource> {

		@Override
		public Class<? extends SmartEffResource> dataType() {
			return TYPE_CLASS;
		}

		@Override
		public String label(OgemaLocale locale) {
			return "Room Heat Radiator";
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
	
	public static class EditPage extends EditPageGenericWithTable<HeatRadiator> {
		@Override
		public void setData(HeatRadiator sr) {
			setLabel(sr.radiatorType(), EN, "Radiator type",
					DE, "Heizkörpertyp");
			setLabel(sr.radiatorLength(), EN, "Length (m)", DE, "Länge (m)");
			setLabel(sr.hasHeatCostAllocator(), EN, "Heat cost allocator available", DE ,"Heizkostenverteiler installiert");
			setLabel(sr.heatCostAllocatorReadings(), EN, "Read Heat cost allocator", DE, "Heizkostenverteiler ablesen (aktueller Wert)");
		}

		@SuppressWarnings("unchecked")
		@Override
		protected Class<HeatRadiator> primaryEntryTypeClass() {
			return (Class<HeatRadiator>) TYPE_CLASS;
		}
		
		@Override
		protected String getMaintainer() {
			return "test1";
		}
	}
}
