package org.sp.example.smarteff.eval.capability;

import org.smartrplace.smarteff.util.editgeneric.EditPageGenericParams;

import extensionmodel.smarteff.basic.evals.BuildingEvalData;

public class BuildingEvalParamsPage extends EditPageGenericParams<BuildingEvalData> {
	@Override
	public void setData(BuildingEvalData sr) {
		setLabel(sr.minimumAbsenceDuration(), EN, "Absence times below this time will be covered with presence",
				DE, "Minimale Abwesenheit: Kürzere Abwesenheiten werden durch Anwesenheit ersetzt.");
	}
	@Override
	protected Class<BuildingEvalData> primaryEntryTypeClass() {
		return BuildingEvalData.class;
	}
}
