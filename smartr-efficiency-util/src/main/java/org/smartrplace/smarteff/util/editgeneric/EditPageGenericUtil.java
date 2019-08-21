package org.smartrplace.smarteff.util.editgeneric;

import java.util.Locale;

import org.ogema.core.model.Resource;
import org.smartrplace.extensionservice.proposal.ProjectProposal;
import org.smartrplace.extensionservice.proposal.ProjectProposal100EE;
import org.smartrplace.extensionservice.proposal.ProjectProposalEfficiency;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class EditPageGenericUtil {
	public static OgemaLocale EN = OgemaLocale.ENGLISH;
	public static OgemaLocale DE = OgemaLocale.GERMAN;
	public static OgemaLocale FR = OgemaLocale.FRENCH;
	public static OgemaLocale CN = OgemaLocale.CHINESE;
	public static OgemaLocale FORMAT = new OgemaLocale(Locale.TRADITIONAL_CHINESE);

	public static <T extends Resource> void setDataProject(ProjectProposal result, EditPageGeneric<T> page) {
		page.setLabel(result.costOfProject(), EN, "Total Cost (EUR)", DE, "Gesamtkosten (EUR)");
		page.setLabel(result.ownHours(), EN, "Own hours of building owner",
				DE, "Eigene Arbeitsstunden des Auftraggebers");
	}

	public static <T extends Resource> void setDataProjectEff(ProjectProposalEfficiency result, EditPageGeneric<T> page) {
		setDataProject(result, page);
		page.setLabel(result.yearlySavings(), EN, "Savings/a (EUR)",
				DE, "Jährliche Kosteneinsparung (EUR)");
		page.setLabel(result.yearlyCO2savings(), EN, "CO2-Saved/a (kg)",
				DE, "Jährliche Einsparung CO2-Emissionen (kg)");
		page.setLabel(result.amortization(), EN, "Payback time in years", DE, "Amortisationszeit (Jahre)");
	}

	public static <T extends Resource> void setDataProject100EE(ProjectProposal100EE result, EditPageGeneric<T> page) {
		setDataProjectEff(result, page);
		page.setLabel(result.yearlyOperatingCosts(), EN, "Yearly operating cost conventional (EUR)",
				DE, "Jährliche Betriebskosten konventionell (EUR)");
		page.setLabel(result.yearlyOperatingCostsCO2Neutral(), EN, "Yearly operating cost CO2-neutral (EUR)",
				DE, "Jährliche Betriebskosten CO2-neutral (EUR)");
		page.setLabel(result.yearlyOperatingCosts100EE(), EN, "Yearly operating cost 100EE (EUR)",
				DE, "Jährliche Betriebskosten 100EE (EUR)");
	}
}
