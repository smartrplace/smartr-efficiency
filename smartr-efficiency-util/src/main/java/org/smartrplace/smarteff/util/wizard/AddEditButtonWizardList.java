package org.smartrplace.smarteff.util.wizard;

import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.IntegerResource;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.button.AddEditButton;
import org.smartrplace.smarteff.util.button.ButtonControlProvider;
import org.smartrplace.smarteff.util.editgeneric.DefaultWidgetProvider.SmartEffTimeSeriesWidgetContext;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;

import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;
import extensionmodel.smarteff.api.common.BuildingData;

public abstract class AddEditButtonWizardList<T extends Resource> extends AddEditButton {
	private static final long serialVersionUID = 1L;

	protected abstract Resource getEntryResource(OgemaHttpRequest req);
	protected abstract Class<T> getType();
	protected Boolean forceEnableState(OgemaHttpRequest req) {return null;}
	protected Resource getElementResource(OgemaHttpRequest req) {return null;}
	protected List<SmartEffTimeSeriesWidgetContext> tsCounters() {return null;}
	
	protected final ApplicationManagerMinimal appManMin;
	
	public class WizBexRoomContext {
		List<T> allResource;
		int currentIndex;
		boolean isShifted = false;
	}
	
	public AddEditButtonWizardList(WidgetPage<?> page, String id, String pid,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider,
			BACKTYPE isBackButton,
			ApplicationManagerMinimal appManExt) {
		super(page, id, pid, exPage, false, controlProvider);
		this.isBackButton = isBackButton;
		this.appManMin = appManExt;
	}
	public AddEditButtonWizardList(OgemaWidget widget, String id, String pid,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			ButtonControlProvider controlProvider,
			BACKTYPE isBackButton, ApplicationManagerMinimal appManExt,
			OgemaHttpRequest req) {
		super(widget, id, pid, exPage, controlProvider, req);
		this.isBackButton = isBackButton;
		this.appManMin = appManExt;
	}

	public enum BACKTYPE {
		BACK, START, FORWARD, SECOND, THIRD
	}
	protected final BACKTYPE isBackButton;
	
	protected WizBexRoomContext getMyContext(boolean inPOST, OgemaHttpRequest req) {
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		Object ct = appData.getConfigInfo().context;
		if((ct != null) && (ct instanceof AddEditButtonWizardList.WizBexRoomContext)) {
			@SuppressWarnings("unchecked")
			WizBexRoomContext wct = (WizBexRoomContext) ct;
			if(inPOST && (!wct.isShifted)) {
				shiftContext(wct);
				wct.isShifted = true;
			}
			return wct;
		}
		
		Resource entryResource = getEntryResource(req);
		@SuppressWarnings("unchecked")
		ResourceList<T> sub = entryResource.getSubResource(CapabilityHelper.getSingleResourceName(
				getType()), ResourceList.class);
		WizBexRoomContext wct = new WizBexRoomContext();
		if(isBackButton == BACKTYPE.SECOND)
			wct.currentIndex = 1;
		else if(isBackButton == BACKTYPE.THIRD)
			wct.currentIndex = 2;
		wct.allResource = sub.getAllElements();
		wct.allResource.sort(new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				IntegerResource subPos1 = o1.getSubResource("wizardPosition", IntegerResource.class);
				IntegerResource subPos2 = o2.getSubResource("wizardPosition", IntegerResource.class);
				return Integer.compare(subPos1.getValue(), subPos2.getValue());
			}
		});
		Resource el = getElementResource(req);
		if(el == null) {
			if(isBackButton == BACKTYPE.SECOND)
				wct.currentIndex = 1;
			else if(isBackButton == BACKTYPE.THIRD)
				wct.currentIndex = 2;
			else
				wct.currentIndex = 0;
		} else {
			wct.currentIndex = wct.allResource.indexOf(el);
			if(wct.currentIndex == -1) wct.currentIndex = 0;
		}
		wct.isShifted = true;
		return wct;					
	};
	@Override
	public void onGET(OgemaHttpRequest req) {
		super.onGET(req);
		Boolean forceE = forceEnableState(req);
		if(forceE != null) {
			if(forceE) enable(req);
			else disable(req);
			return;
		}
		WizBexRoomContext ct = getMyContext(false, req);
		//List<T> resList = ct.allResource;
		int idx = getShiftedIndex(ct);
		//if((ct.currentIndex < 0) || (ct.currentIndex >= resList.size())) disable(req);
		if((idx < 0)) disable(req);
		else enable(req);
	}
	
	/** Calling getMyContext once before POST is processed should make sure the shifting is
	 * done now
	 */
	/*@Override
	public void onPrePOST(String data, OgemaHttpRequest req) {
		getMyContext(true, req);
		super.onPrePOST(data, req);
	}*/
	
	@Override
	protected Resource getResource(ExtensionResourceAccessInitData appData,
			OgemaHttpRequest req) {
		WizBexRoomContext ct = getMyContext(false, req);
		List<T> resList = ct.allResource;
		int idx = getShiftedIndex(ct);
		if((idx < 0) || (idx >= resList.size())) {
			if(ct.allResource != null && !ct.allResource.isEmpty()) {
				return ct.allResource.get(0).getParent().getParent();
			}
			Resource entryResource = getEntryResource(req);
			if(entryResource instanceof BuildingData) return entryResource;
			return null;
		}
		else return ct.allResource.get(idx);
		//if((ct.currentIndex < 0) || (ct.currentIndex >= resList.size())) return null;
		//else return ct.allResource.get(ct.currentIndex);
	}
	
	@Override
	protected Object getContext(ExtensionResourceAccessInitData appData, Resource object,
			OgemaHttpRequest req) {
		WizBexRoomContext ct = getMyContext(true, req);
		WizBexRoomContext newContext = new WizBexRoomContext();
		newContext.allResource = ct.allResource;
		if(isBackButton == BACKTYPE.SECOND)
			newContext.currentIndex = 1;
		else if(isBackButton == BACKTYPE.THIRD)
			newContext.currentIndex = 2;
		else
			newContext.currentIndex = ct.currentIndex;
		return newContext;
	}
	
	private void shiftContext(WizBexRoomContext ct) {
		if(isBackButton==BACKTYPE.BACK)
			ct.currentIndex = ct.currentIndex - 1;
		else if(isBackButton == BACKTYPE.FORWARD)
			ct.currentIndex = ct.currentIndex + 1;
	}
	protected int getShiftedIndex(WizBexRoomContext ct) {
		if(ct.isShifted) return ct.currentIndex;
		if(isBackButton==BACKTYPE.BACK)
			return ct.currentIndex - 1;
		else if(isBackButton == BACKTYPE.FORWARD)
			return ct.currentIndex + 1;
		else return ct.currentIndex;
	}
	
	public static int getTimeSeriesNumWithoutValueForToday(String superResourceLocation,
			List<SmartEffTimeSeriesWidgetContext> tsCounters, ApplicationManagerMinimal appManMin) {
		long dayStart = AbsoluteTimeHelper.getIntervalStart(appManMin.getFrameworkTime(), AbsoluteTiming.DAY);
		//long dayEnd = AbsoluteTimeHelper.addIntervalsFromAlignedTime(dayStart, 1, AbsoluteTiming.DAY)-1;
		int countWithData = 0;
		int countTested = 0;
		for(SmartEffTimeSeriesWidgetContext ts: tsCounters) {
			for(Entry<String, Long> e: ts.lastTimeStamp.entrySet()) {
				if(superResourceLocation == null || e.getKey().startsWith(superResourceLocation)) {
					countTested++;
					if(e.getValue() >= dayStart) countWithData++;
					//there should only be a single entry in the map for the room as each time series has an own context
					if(superResourceLocation != null) break;
				}
			}
		}
		if(countTested < tsCounters.size()) return tsCounters.size() - countWithData;
		else return countTested - countWithData;
	}
	
	@Override
	protected Integer getSizeInternal(Resource myResource, ExtensionResourceAccessInitData appData) {
		if(tsCounters() != null && appManMin != null) {
			String roomLoc = myResource.getLocation();
			return getTimeSeriesNumWithoutValueForToday(roomLoc, tsCounters(), appManMin);
		}
		return null;
	}
}
