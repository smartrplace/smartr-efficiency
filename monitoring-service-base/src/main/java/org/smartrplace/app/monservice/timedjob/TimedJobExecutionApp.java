package org.smartrplace.app.monservice.timedjob;

import java.util.Collection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.TimedJobMgmtService;
import org.ogema.devicefinder.api.TimedJobProvider;
import org.ogema.devicefinder.util.TimedJobMemoryData;
import org.ogema.devicefinder.util.TimedJobMgmtServiceImpl;

@Component(specVersion = "1.2", immediate = true)
@Service({Application.class, TimedJobMgmtService.class})
public class TimedJobExecutionApp implements Application, TimedJobMgmtService {

	TimedJobMgmtServiceImpl tserv = null;
	
	@Override
	public void start(ApplicationManager appManager) {
		tserv = new TimedJobMgmtServiceImpl(appManager);
	}

	@Override
	public void stop(AppStopReason reason) {
		if(tserv != null)
			tserv.stop();
		tserv = null;
	}

	@Override
	public TimedJobMemoryData registerTimedJobProvider(TimedJobProvider prov) {
		return tserv.registerTimedJobProvider(prov);
	}

	@Override
	public TimedJobMemoryData unregisterTimedJobProvider(TimedJobProvider prov) {
		return tserv.unregisterTimedJobProvider(prov);
	}

	@Override
	public Collection<TimedJobMemoryData> getAllProviders() {
		return tserv.getAllProviders();
	}

	@Override
	public TimedJobMemoryData getProvider(String id) {
		return tserv.getProvider(id);
	}

}
