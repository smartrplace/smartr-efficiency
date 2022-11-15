package org.ogema.util.timedjob;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.devicefinder.api.TimedJobMemoryData;
import org.ogema.devicefinder.api.TimedJobMgmtService;
import org.ogema.devicefinder.api.TimedJobProvider;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.resourcemanipulator.timer.CountDownAbsoluteTimer;
import org.ogema.tools.resourcemanipulator.timer.CountDownDelayedExecutionTimer;
import org.ogema.util.timedjob.TimedJobMgmtServiceImpl.TimedJobMgmtData;
import org.smartrplace.apps.eval.timedjob.TimedJobConfig;
import org.smartrplace.tsproc.persist.TsProcPersistUtil;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsolutePersistentTimer;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTimerListener;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;

/** Data for a TimedJob that is not stored persistently*/
public class TimedJobMemoryDataImpl implements TimedJobMemoryData {
	public static final float MINIMUM_MINUTES_FOR_TIMER_START = 2.5f;
	public static final long MIN_FREE_MEMORY_MB = Long.getLong("org.ogema.devicefinder.util.minfreemb", 200);
	
	protected static volatile int countSinceLastGC = 1;
	
	public long lastRunStart() {
		return lastRunStart;
	}

	public long lastRunDuration() {
		return lastRunDuration;
	}

	public long lastRunEnd() {
		return lastRunEnd;
	}

	public long maxRunDuration() {
		return maxRunDuration;
	}

	public long nextScheduledStart() {
		return nextScheduledStart;
	}

	/** The {@link TimedJobMgmtService} makes sure that each job cannot be triggered again while it is running.
	 * It will also not be queued then. If minimum gaps are required between starting a job
	 * (e.g. for jobs performing bundle restarts) then the job has to implement such a
	 * supervision itself. See BundleRestartButton as an example.*/
	public boolean isRunning() {
		return isRunning;
	}

	public boolean triggeredForExecutionOnceOutsideTime() {
		return myThreadForRunningOnceFromOutside != null;
	}
	public TimedJobConfig res() {
		return res;
	}

	public TimedJobProvider prov() {
		return prov;
	}

	protected volatile long lastRunStart = -1;
	protected volatile long lastRunEnd;
	
	protected volatile long lastRunDuration = -1;
	protected volatile long maxRunDuration = -1;
	protected volatile long nextScheduledStart = 0;
	
	//protected volatile long executionTimeCounter = 0;
	//protected volatile long freeTimeCounter = 0;
	//protected volatile long lastLoadReport = -1;
	
	protected volatile boolean isRunning;
	
	TimedJobConfig res;
	protected TimedJobProvider prov;
	protected final TimedJobMgmtData jobData;
	
	protected Timer timerUnaligned = null;
	protected AbsolutePersistentTimer timerAligned = null;
	
	private final ApplicationManager appMan;
	private final TimedJobMgmtServiceImpl timedJobMgmtServiceImpl;
	
	public TimedJobMemoryDataImpl(ApplicationManager appMan, TimedJobMgmtServiceImpl timedJobMgmtServiceImpl,
			TimedJobMgmtData jobData) {
		this.appMan = appMan;
		this.timedJobMgmtServiceImpl = timedJobMgmtServiceImpl;
		this.jobData = jobData;
		long now = appMan.getFrameworkTime();
		lastRunEnd = now;
		//lastLoadReport = now;
	}

	boolean isAligned;
	int align;
	long interval;
	protected boolean executeBlockingOnceFromTimer() {
		nextScheduledStart = getNextScheduledStartIfExecutingNow(isAligned, align, interval);
		return executeBlockingOnce();
	}
	//Should not called directly from the outside
	protected boolean executeBlockingOnce() {
		if(isRunning())
			return false;
		if(skipKJobForTesting())
			return false;
		lastRunStart = appMan.getFrameworkTime();
		timedJobMgmtServiceImpl.reportEvaluationStart(prov.id(), true);
		//long lastFreeTime = lastRunStart - lastRunEnd;
		//freeTimeCounter += lastFreeTime;
		isRunning = true;
Runtime rt = Runtime.getRuntime();
if(Boolean.getBoolean("jobdebug")) {
	System.out.println("Starting job of provider:"+prov.id()+" Free:"+rt.freeMemory()/(1024*1024));
}
		timedJobMgmtServiceImpl.reportEvaluationStart(prov.id(), false);
		try {
			prov.execute(lastRunStart, this);
		} catch(Exception e) {
			appMan.getLogger().warn("Exception in provider "+prov.id(), e);
			e.printStackTrace();
		}
		isRunning = false;
		lastRunEnd = appMan.getFrameworkTime();
		timedJobMgmtServiceImpl.reportEvaluationEnd(prov.id(), false);
		ValueResourceHelper.setCreate(jobData.logResource.jobIdxStarted(), res.persistentIndex().getValue());
		lastRunDuration = lastRunEnd - lastRunStart;
		if(lastRunDuration > maxRunDuration)
			maxRunDuration = lastRunDuration;
		//executionTimeCounter += lastRunDuration;
		ValueResourceHelper.setCreate(jobData.logResource.jobDuration(), lastRunDuration);
		/*if(lastRunEnd - lastLoadReport > LOAD_REPORT_INTERVAL) {
			float load = (float) (((double)executionTimeCounter)/(executionTimeCounter+freeTimeCounter));
			ValueResourceHelper.setCreate(jobData.logResource.jobLoad(), load);
			lastLoadReport = lastRunEnd;
		}*/
		if(prov.evalJobType() == 0) {
			timedJobMgmtServiceImpl.reportEvaluationEnd(prov.id(), true);
			return true;
		}
		long free = rt.freeMemory()/(1024*1024);
if(Boolean.getBoolean("jobdebug")) System.out.println("Finished after "+lastRunDuration+" msec job of provider:"+prov.id()+" Free:"+free+" not saved:"+countSinceLastGC);
		if(!Boolean.getBoolean("org.ogema.util.timedjob.intermediatesaving")) {
			timedJobMgmtServiceImpl.reportEvaluationEnd(prov.id(), true);
			return true;
		}
		if(free < MIN_FREE_MEMORY_MB) {
			//rt.gc();
			if((countSinceLastGC > 10) &&
					(TsProcPersistUtil.lastSaved < TsProcPersistUtil.getTsNumToSaveTotal())) {
				if(timedJobMgmtServiceImpl.triggerSavetoDisk())
					countSinceLastGC = 0;
				//if(Boolean.getBoolean("jobdebug")) System.out.println("Triggering Save2Disk from evaluation...");
				//TimedJobMemoryData saveJob = timedJobMgmtServiceImpl.getProvider("SaveVDP2Disk");
				//if(saveJob != null) {
				//	saveJob.executeBlockingOnceOnYourOwnRisk();
				//	countSinceLastGC = 0;
				//}
			}
			//long freeAfter = rt.freeMemory()/(1024*1024);
			//System.out.println(" Free after GC:"+freeAfter+" not saved:"+countSinceLastGC);
			/*if(countSinceLastGC == 0 && freeAfter < MIN_FREE_MEMORY_MB) {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				rt.gc();
				freeAfter = rt.freeMemory()/(1024*1024);
				System.out.println(" Free after second GC:"+freeAfter);
			}*/
		}
		countSinceLastGC++;
		timedJobMgmtServiceImpl.reportEvaluationEnd(prov.id(), true);
		return true;
	}
	
	public boolean skipKJobForTesting() {
		if(Boolean.getBoolean("org.ogema.devicefinder.util.skipalljobs"))
			return true;
		if((prov.evalJobType()>0) && (Boolean.getBoolean("org.ogema.devicefinder.util.skipevaljobs")))
			return true;
		return false;
	}
	
	//protected volatile Thread myThread = null;
	protected volatile CountDownDelayedExecutionTimer myThreadForRunningOnceFromOutside = null;
	protected volatile ResourceValueListener<IntegerResource> myThreadForRunningDirect = null;
	protected abstract class DirectRunner implements ResourceValueListener<IntegerResource> {
		protected abstract void asapExecution();
		
		public DirectRunner() {
			if(myThreadForRunningOnceFromOutside != null || myThreadForRunningDirect != null)
				return;
			myThreadForRunningDirect = this;
			res.triggerTimedJobStartsWithoutDelay().addValueListener(myThreadForRunningDirect, true);
			res.triggerTimedJobStartsWithoutDelay().getAndAdd(1);
		}

		@Override
		public void resourceChanged(IntegerResource resource) {
			try {
				res.triggerTimedJobStartsWithoutDelay().removeValueListener(myThreadForRunningDirect);
				asapExecution();
	        } catch(Exception e) {
	            e.printStackTrace();
	        } finally {
	        	myThreadForRunningDirect = null;
			}
		}
	}
	
	public boolean executeBlockingOnceOnYourOwnRisk() {
		return executeBlockingOnce();
	}
	public boolean executeNonBlockingOnce() {
		
		if(isRunning())
			return false;
		new DirectRunner() {
			
			@Override
			protected void asapExecution() {
				executeBlockingOnce();
			}
		};
		
		/*myThreadForRunningOnceFromOutside = new CountDownDelayedExecutionTimer(appMan, 1) {
			
			@Override
			public void delayedExecution() {
		        try {
		        	executeBlockingOnce();
		        } catch(Exception e) {
		            e.printStackTrace();
		        } finally {
					myThreadForRunningOnceFromOutside = null;
				}
		    } 
		};*/
		return true;
	}
	
	/** Start timed operation
	 * @param appMan
	 * @return error code: 0:no error, 10: disabled, 20: failed
	 */
	public int startTimerIfNotStarted() {
		if(isTimerActive())
			return 0;
		return restartTimer();
	}
	
	public int restartTimer() {
		stopTimerIfRunning();
		
		if(res.disable().getValue()) {
			return 10;
		}
		
		//If settings are not suitable for startup, we just perform initial run if configured
		final long nextScheduledStartWithoutStart;
		if(canTimerBeActivated()) {
			interval = (long) (res.interval().getValue()*TimeProcUtil.MINUTE_MILLIS);
			align = res.alignedInterval().getValue();
			isAligned = (align > 0);
			if(!isAligned) {
				timerUnaligned = appMan.createTimer(interval, new TimerListener() {
					
					@Override
					public void timerElapsed(Timer timer) {
						executeBlockingOnceFromTimer();
					}
				});
			} else {
				timerAligned = new AbsolutePersistentTimer(res.lastStartStorage(), align,
						new AbsoluteTimerListener() {
							
							@Override
							public void timerElapsed(CountDownAbsoluteTimer myTimer, long absoluteTime, long timeStep) {
								if(interval > 0 && interval < 10000) {
									executeBlockingOnceFromTimer();
								} else if(interval > 0) {
									new CountDownDelayedExecutionTimer(appMan, interval) {
										@Override
										public void delayedExecution() {
											executeBlockingOnceFromTimer();
										}
									};
								} else
									executeBlockingOnceFromTimer();
								
							}
						}, appMan);
			}
			nextScheduledStartWithoutStart = getNextScheduledStartIfExecutingNow(isAligned, align, interval);
			
			prov.timerStartedNotification(this);
		} else
			nextScheduledStartWithoutStart = -1;
		
		float startup = res.performOperationOnStartUpWithDelay().getValue();
		if(startup >= 0) {
			long delay = (long) (startup*TimeProcUtil.MINUTE_MILLIS);
			if(delay < 1)
				delay = 1;
			nextScheduledStart = appMan.getFrameworkTime() + delay;

			if(delay > 0 && delay < 10000) {
				new DirectRunner() {
					
					@Override
					protected void asapExecution() {
						nextScheduledStart = nextScheduledStartWithoutStart;
						executeBlockingOnce();
					}
				};
			} else  {
				new CountDownDelayedExecutionTimer(appMan, delay) {
					@Override
					public void delayedExecution() {
						nextScheduledStart = nextScheduledStartWithoutStart;
						executeBlockingOnce();
					}
				};
			}
		} else
			nextScheduledStart = nextScheduledStartWithoutStart;

		return 0;
	}
	
	public void stopTimerIfRunning() {
		if(timerAligned != null) {
			timerAligned.stop();
			prov.timerStoppedNotification(this);
			timerAligned = null;
		}
		if(timerUnaligned != null) {
			timerUnaligned.stop();
			timerUnaligned.destroy();
			prov.timerStoppedNotification(this);
			timerUnaligned = null;
		}		
	}
	
	public boolean isTimerActive() {
		return (timerAligned != null || timerUnaligned != null);
	}
	
	/** Check timer settings*/
	public boolean canTimerBeActivated() {
		int alignLoc = res.alignedInterval().getValue();
		if(alignLoc > 0)
			return true;
		float intervalLoc = res.interval().getValue();
		if((intervalLoc >= MINIMUM_MINUTES_FOR_TIMER_START) || prov.id().contains("ExternalEscalationProvider"))
			return true;
		return false;
	}
	
	protected long getNextScheduledStartIfExecutingNow(boolean isAligned, int align, long interval) {
		long now = appMan.getFrameworkTime();
		if(isAligned) {
			return AbsoluteTimeHelper.getNextStepTime(now, align)+interval;
		} else
			return now+interval;
	}
	
	@Override
	public String toString() {
		try {
			return prov.id()+":"+prov.label(null);
		} catch(Exception e) {
			if(res != null)
				return "WOjob:"+res.getLocation();
			else
				return "WOres:"+super.toString();
		}
	}
	
	public static Button getTimedJobStatusButton(final TimedJobMemoryData object, OgemaWidget mainTable,
			String id, OgemaHttpRequest req, final Alert alert) {
		final boolean timerActive = object.isTimerActive();
		return getTimedJobStatusButton(object, mainTable, id, req, alert, timerActive);
	}
	public static Button getTimedJobStatusButton(final TimedJobMemoryData object, OgemaWidget mainTable,
			String id, OgemaHttpRequest req, final Alert alert,
			final boolean timerActivePre) {
		Button startButton;
		if((!timerActivePre) && (!object.canTimerBeActivated())) {
			startButton = new Button(mainTable, "startBut"+id, "Itv short", req);
			startButton.disable(req);
		} else {
			startButton = new Button(mainTable, "startBut"+id, req) {
				private static final long serialVersionUID = 9186426428229396643L;

				@Override
				public void onGET(OgemaHttpRequest req) {
					final boolean timerActive = object.isTimerActive();
					if(timerActive) {
						addStyle(ButtonData.BOOTSTRAP_GREEN, req);
						removeStyle(ButtonData.BOOTSTRAP_RED, req);
						setText("Stop Timer", req);
					} else {
						setText("Start timer", req);
						addStyle(ButtonData.BOOTSTRAP_RED, req);
						removeStyle(ButtonData.BOOTSTRAP_GREEN, req);
					}
				}
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					final boolean timerActive = object.isTimerActive();
					if(timerActive) {
						ValueResourceHelper.setCreate(object.res().disable(), true);
						object.stopTimerIfRunning();
						if(alert != null)
							alert.showAlert("Stopped timer for service "+object.prov().label(req.getLocale()), true, req);
					}
					else {
						object.res().disable().setValue(false);
						int result = object.startTimerIfNotStarted();
						if(result == 0)
							alert.showAlert("Started timer for service "+object.prov().label(req.getLocale()), true, req);
						else
							alert.showAlert("Could not start timer for service "+object.prov().label(req.getLocale())+" ("+result+")", true, req);
					}
				}
			};
		}
		if(alert != null)
			startButton.registerDependentWidget(alert);
		startButton.registerDependentWidget(startButton);
		return startButton;
	}
}
