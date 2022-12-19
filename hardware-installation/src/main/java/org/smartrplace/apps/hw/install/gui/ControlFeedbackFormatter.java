package org.smartrplace.apps.hw.install.gui;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.smartrplace.util.directobjectgui.LabelFormatter;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class ControlFeedbackFormatter implements LabelFormatter {
	private final FloatResource control;
	private final FloatResource feedback;
	private final IntegerResource controlInt;
	private final IntegerResource feedbackInt;
	private final StringResource controlStr;
	private final StringResource feedbackStr;
	private final DatapointService dpService;

	public ControlFeedbackFormatter(FloatResource control, FloatResource feedback, DatapointService dpService) {
		this.control = control;
		this.feedback = feedback;
		this.controlInt = null;
		this.feedbackInt = null;
		this.controlStr = null;
		this.feedbackStr = null;
		this.dpService = dpService;
	}
	
	public ControlFeedbackFormatter(IntegerResource control, IntegerResource feedback, DatapointService dpService) {
		this.control = null;
		this.feedback = null;
		this.controlInt = control;
		this.feedbackInt = feedback;
		this.controlStr = null;
		this.feedbackStr = null;
		this.dpService = dpService;
	}

	public ControlFeedbackFormatter(StringResource control, StringResource feedback, DatapointService dpService) {
		this.control = null;
		this.feedback = null;
		this.controlInt = null;
		this.feedbackInt = null;
		this.controlStr = control;
		this.feedbackStr = feedback;
		this.dpService = dpService;
	}

	@Override
	public OnGETData getData(OgemaHttpRequest req) {
		if(control != null) {
			float val = control.getValue();
			float valFb = feedback.getValue();
			int state;
			if(!feedback.exists())
				state = 2;
			else
				state = ValueResourceHelper.isAlmostEqual(val, valFb)?1:0;
			Boolean isTemperature = null;
			if(dpService != null) {
				Datapoint dp = dpService.getDataPointAsIs(control);
				if(dp != null) {
					GaRoDataType garo = dp.getGaroDataType();
					if(garo != null)
						isTemperature = TemperatureResource.class.isAssignableFrom(garo.representingResourceType());
				}
			}
			if(isTemperature == null)
				isTemperature = (control instanceof TemperatureResource);
			if(isTemperature)
				return new OnGETData(String.format("%.1f / %.1f", val-273.15f, valFb-273.15f), state);
			return new OnGETData(String.format("%.1f / %.1f", val, valFb), state);
		} else if(controlStr != null) {
			String val = controlStr.getValue();
			String valFb = feedbackStr.getValue();
			int state;
			if(feedbackStr == null || (!feedbackStr.exists())) {
				state = 2;
				return new OnGETData(String.format("%s", val), state);			
			} else
				state = (val.equals(valFb))?1:0;
			return new OnGETData(String.format("%s", valFb), state);			
		} else if(controlInt != null) {
			int val = controlInt.getValue();
			int valFb = feedbackInt.getValue();
			int state;
			if(feedbackInt == null || (!feedbackInt.exists())) {
				state = 2;
				return new OnGETData(String.format("%d", val), state);
			} else
				state = (val==valFb)?1:0;
			return new OnGETData(String.format("%d / %d", val, valFb), state);
		}
		return new OnGETData("n/a", 2);
	}
}
