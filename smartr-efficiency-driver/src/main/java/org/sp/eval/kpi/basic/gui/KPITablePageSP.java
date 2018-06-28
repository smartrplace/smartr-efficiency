package org.sp.eval.kpi.basic.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.resource.util.TimeUtils;
import org.ogema.util.directresourcegui.kpi.IntervalTypeDropdown;
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
import org.smartrplace.smarteff.util.button.LogicProvTableOpenButton;
import org.smartrplace.smarteff.util.button.ResourceTableOpenButton;
import org.smartrplace.smarteff.util.button.TableOpenButton;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;

import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.label.Label;
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
		private final ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage;
		
		public TablePage(ExtensionNavigationPageI<SmartEffUserDataNonEdit,
				ExtensionResourceAccessInitData> exPage, ApplicationManagerMinimal appManMin,
				EvalScheduler scheduler) {
			super(exPage.getPage(), appManMin, scheduler,
					Arrays.asList(INTERVALS_OFFERED),
					3, true, EvalResultManagementStd.STANDARD_MULTIEVAL_INTERVAL_STEP);
			this.exPage = exPage;
			//this.appManMin = appManMin;
			//triggerPageBuild();
			StaticTable topTable = new StaticTable(1, 5);
			TableOpenButton resourceButton2 = new ResourceTableOpenButton(page, "resourceButton", pid(), exPage, null);
			LogicProvTableOpenButton proposalTableOpenButton = new LogicProvTableOpenButton(page, "proposalTableOpenButton", pid(), exPage, null);
			topTable.setContent(0, 0, intervalDrop).setContent(0, 1, dateOfReport);
			topTable.setContent(0, 2, "--").setContent(0, 3, resourceButton2).setContent(0,4, proposalTableOpenButton);
			page.append(topTable);
			retardationOnGET = 1000;
		}

		@Override
		public void addWidgetsAboveTable() {
			this.intervalDrop = new IntervalTypeDropdown(page, "singleTimeInterval", false, intervalTypes, standardInterval);
			this.dateOfReport = new Label(page, "dateOfReport") {
				private static final long serialVersionUID = 1L;
				@Override
				public void onGET(OgemaHttpRequest req) {
					Resource entryRes = getReqData(req);
					ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
					List<KPIStatisticsManagement> evalsP = appData.getEvaluationManagement().getKPIManagement(entryRes);
					int baseInterval = -2;
					if(evalsP != null && (!evalsP.isEmpty())) {
						baseInterval = evalsP.get(0).getBaseInterval();
						if(baseInterval > 0) {
							SampledValue sv = evalsP.get(0).getIntervalSchedule(baseInterval).getPreviousValue(Long.MAX_VALUE);
							if(sv == null) baseInterval = -3;
							else
								currentTime = sv.getTimestamp();
					}
					if(baseInterval <= 0)
						currentTime = appManExt.getFrameworkTime();
					}					
					String timeOfReport = "Time of Report: " + TimeUtils.getDateAndTimeString(currentTime);
					setText(timeOfReport, req);
				}
			};
			triggerOnPost(dateOfReport, intervalDrop);
			//this.header = new Header(page, "header", "KPI Standard Overview");
			//page.append(header);
			//page.append(Linebreak.getInstance());
			//page.append(dateOfReport);
			//page.append(intervalDrop);

			//We finish this in constructor as we need exPage here and this method is called from super-constructor
		}
		@Override
		protected void addWidgetsBelowTable() {
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
		return "KPI base table page";
	}

	@Override
	protected void addWidgets() {
		tablePage = new TablePage(exPage, appManExt, scheduler);		
	}

	@Override
	protected String getHeader(OgemaHttpRequest req) {
		return "KPIs for "+ResourceUtils.getHumanReadableName(getReqData(req))+" (+Subtree)";
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
