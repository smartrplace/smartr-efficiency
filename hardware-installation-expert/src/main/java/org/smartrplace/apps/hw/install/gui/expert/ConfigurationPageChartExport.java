/**
 * ﻿Copyright 2014-2018 Fraunhofer-Gesellschaft zur Förderung der angewandten Wissenschaften e.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartrplace.apps.hw.install.gui.expert;

import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.model.gateway.LocalGatewayInformation;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.tissue.util.logconfig.VirtualSensorKPIMgmt;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.resource.widget.textfield.BooleanResourceCheckbox;
import de.iwes.widgets.resource.widget.textfield.ValueResourceTextField;

@SuppressWarnings("serial")
public class ConfigurationPageChartExport {
	//private final WidgetPage<?> page;
	private final HardwareInstallController controller;

	public ConfigurationPageChartExport(final WidgetPage<?> page, final HardwareInstallController app) {
		
		//this.page = page;
		this.controller = app;
		
		final Alert alert = new Alert(page, "alert", "");
		page.append(alert);
		
		final LocalGatewayInformation gwInfo = ResourceHelper.getLocalGwInfo(controller.appMan);
		
		BooleanResourceCheckbox exportGermanExcelCSV = new BooleanResourceCheckbox(page, "exportGermanExcelCSV",
				"", gwInfo.chartExportConfig().exportGermanExcelCSV()) {

			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				BooleanResource sel = getSelectedItem(req);
				if(!sel.isActive()) {
					sel.create();
					sel.activate(false);
				}
			}
		};
		ValueResourceTextField<StringResource> timeStampFormat = new ValueResourceTextField<StringResource>(page, "timeStampFormat");
		timeStampFormat.selectDefaultItem(gwInfo.chartExportConfig().timeStampFormat());

		
		BooleanResourceCheckbox addLabelLine = new BooleanResourceCheckbox(page, "allLabelLine",
				"", gwInfo.chartExportConfig().addLabels()) {

			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				BooleanResource sel = getSelectedItem(req);
				if(!sel.isActive()) {
					sel.create();
					sel.activate(false);
				}
			}
		};
		BooleanResourceCheckbox offerFixedStepExport = new BooleanResourceCheckbox(page, "offerFixedStepExport",
				"", gwInfo.chartExportConfig().performFixedStepExport()) {

			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				BooleanResource sel = getSelectedItem(req);
				if(!sel.isActive()) {
					sel.create();
					sel.activate(false);
				}
			}
		};
		
		ValueResourceTextField<FloatResource> fixedTimeStepSeconds = new ValueResourceTextField<FloatResource>(page, "fixedTimeStepSeconds");
		fixedTimeStepSeconds.selectDefaultItem(gwInfo.chartExportConfig().fixedTimeStepSeconds());

		ValueResourceTextField<FloatResource> maxValidValueIntervalSeconds = new ValueResourceTextField<FloatResource>(page, "maxValidValueIntervalSeconds");
		maxValidValueIntervalSeconds.selectDefaultItem(gwInfo.chartExportConfig().maxValidValueIntervalSeconds());

		ValueResourceTextField<StringResource> naNValue = new ValueResourceTextField<StringResource>(page, "naNValue");
		naNValue.selectDefaultItem(gwInfo.chartExportConfig().naNValue());


		StaticTable configTable = new StaticTable(7, 2);
		int i = 0;
		configTable.setContent(i, 0, "If selected than values will be exported with comma as decimal separator. Note that "
				+ "semicolon is used as CSV separator anyways:").
		setContent(i, 1, exportGermanExcelCSV);
		i++;
		configTable.setContent(i, 0, "Format to apply to time stamps like yyyy-MM-dd'T'HH:mm:ss. If empty standard\r\n" + 
				"	 * internal time stamps in milliseconds since epoch UTC are exported:").
		setContent(i, 1, timeStampFormat);
		i++;

		configTable.setContent(i, 0, "Perform aggregated fixed-step export for CSV").
		setContent(i, 1, offerFixedStepExport);
		i++;
		configTable.setContent(i, 0, "Add label line").
		setContent(i, 1, addLabelLine);
		i++;
		configTable.setContent(i, 0, "Time step for fixed-step export (seconds)").
				setContent(i, 1, fixedTimeStepSeconds);
		i++;
		configTable.setContent(i, 0, "Maximum distance of last value in source is found before NaN or empty value is written. "
				+ "Set to zero or Long.Max if this shall not occur.").
			setContent(i, 1, maxValidValueIntervalSeconds);
		i++;
		
		configTable.setContent(i, 0, "NaN value to be used if next value in source is too far away (empty to skip these values):").
		setContent(i, 1, naNValue);
		i++;		

		page.append(configTable);
	}
	
	public void setMainMeter(ElectricityConnection conn) {
		VirtualSensorKPIMgmt.registerEnergySumDatapointOverSubPhases(conn, AggregationMode.Meter2Meter, controller.util, controller.dpService,
				TimeProcUtil.SUM_PER_DAY_EVAL, true);
	}
}
