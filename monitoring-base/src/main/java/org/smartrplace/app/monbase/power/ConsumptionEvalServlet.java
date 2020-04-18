package org.smartrplace.app.monbase.power;

import java.util.Collection;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletPageProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

import com.iee.app.evaluationofflinecontrol.util.ExportBulkData.ComplexOptionDescription;


public class ConsumptionEvalServlet implements ServletPageProvider<String> {
	protected final ApplicationManager appMan;
	
	public ConsumptionEvalServlet(ApplicationManager appMan) {
		this.appMan = appMan;
	}

	@Override
	public Map<String, ServletValueProvider> getProviders(String object, String user,
			Map<String, String[]> parameters) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> getAllObjects(String user) {
		// TODO Auto-generated method stub
		return null;
	}


}
