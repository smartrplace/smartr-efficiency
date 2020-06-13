package org.ogema.devicefinder.basedata;

import org.ogema.core.application.TimerListener;

public abstract class TimerListenerInitialElapsed implements TimerListener {
	public TimerListenerInitialElapsed() {
		timerElapsed(null);
	}
}
