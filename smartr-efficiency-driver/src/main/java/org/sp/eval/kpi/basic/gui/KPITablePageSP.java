package org.sp.eval.kpi.basic.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.util.directresourcegui.kpi.KPIMonitoringReport;
import org.ogema.util.directresourcegui.kpi.KPIResultType;
import org.ogema.util.directresourcegui.kpi.KPIStatisticsManagement;
import org.ogema.util.evalcontrol.EvalScheduler;
import org.ogema.util.jsonresult.management.EvalResultManagementStd;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.resourcecreate.ProviderPublicDataForCreate.PagePriority;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.NaviPageBase;
import org.smartrplace.smarteff.util.button.ProposalProvTableOpenButton;
import org.smartrplace.smarteff.util.button.ResourceTableOpenButton;
import org.smartrplace.smarteff.util.button.TableOpenButton;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;

import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class KPITablePageSP extends NaviPageBase<Resource> {
	public static final Integer[] INTERVALS_OFFERED =
			new Integer[]{AbsoluteTiming.DAY, AbsoluteTiming.WEEK, AbsoluteTiming.MONTH};
	//public final static Class<MultiKPIEvalConfiguration> TYPE_SHOWN = MultiKPIEvalConfiguration.class;
	
	protected TablePage tablePage;
	
	private final EvalScheduler scheduler;
	
	public KPITablePageSP(EvalScheduler scheduler) {
		super();
		this.scheduler = scheduler;
	}

	public class TablePage extends KPIMonitoringReport {
		//private final ApplicationManagerMinimal appManMin;
		private ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage;
		
		public TablePage(ExtensionNavigationPageI<SmartEffUserDataNonEdit,
				ExtensionResourceAccessInitData> exPage, ApplicationManagerMinimal appManMin,
				EvalScheduler scheduler) {
			super(exPage.getPage(), appManMin, scheduler,
					Arrays.asList(INTERVALS_OFFERED),
					3, true, EvalResultManagementStd.STANDARD_MULTIEVAL_INTERVAL_STEP);
			this.exPage = exPage;
			//this.appManMin = appManMin;
			//triggerPageBuild();
		}

		@Override
		public void addWidgetsAboveTable() {
			StaticTable topTable = new StaticTable(1, 3);
			TableOpenButton resourceButton2 = new ResourceTableOpenButton(page, "resourceButton", pid(), exPage, null);
			ProposalProvTableOpenButton proposalTableOpenButton = new ProposalProvTableOpenButton(page, "proposalTableOpenButton", pid(), exPage, null);
			topTable.setContent(0, 0, "--").setContent(0, 1, resourceButton2).setContent(0, 2, proposalTableOpenButton);
			page.append(topTable);
		}

		@Override
		public Collection<KPIResultType> getObjectsInTable(OgemaHttpRequest req) {
			List<KPIResultType> result = new ArrayList<>();
			
			Resource entryRes = getReqData(req);
			ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
			List<KPIStatisticsManagement> evalsP = appData.getEvaluationManagement().getKPIManagement(entryRes);
			for(KPIStatisticsManagement ksm: evalsP) {
				result.add(new KPIResultType(ksm));
			}
			return result;
		}
	}

	
	@Override
	protected Class<Resource> primaryEntryTypeClass() {
		return Resource.class;
	}
	
	@Override
	public String label(OgemaLocale locale) {
		return "Results for Resource Recursive";
	}

	@Override
	protected void addWidgets() {
		tablePage = new TablePage(exPage, appManExt, scheduler);		
	}

	@Override
	protected String getHeader(OgemaHttpRequest req) {
		return "Results for "+ResourceUtils.getHumanReadableName(getReqData(req))+" (+Subtree)";
	}
	
	@Override
	public List<EntryType> getEntryTypes() {
		return CapabilityHelper.getStandardEntryTypeList(primaryEntryTypeClass());
	}

	@Override
	protected PageType getPageType() {
		return PageType.TABLE_PAGE;
	}
	
	@Override
	protected PagePriority getPriority() {
		return PagePriority.HIDDEN;
	}
}
