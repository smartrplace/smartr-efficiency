package org.smartrplace.smarteff.util.eval;

import java.util.List;

import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoSelectionItem;
import de.iwes.widgets.html.selectiontree.SelectionItem;

public class SmartEffGaRoProviderTS extends GaRoMultiEvalDataProvider<GaRoSelectionItem> {

	@Override
	protected List<SelectionItem> getOptions(int level, GaRoSelectionItem superItem) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean providesMultipleGateways() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setGatewaysOffered(List<SelectionItem> gwSelectionItemsToOffer) {
		// TODO Auto-generated method stub
		
	}

}
