package org.smartrplace.app.monbase.servlet;

import java.util.Collection;

import org.smartrplace.app.monbase.power.ConsumptionEvalTableI;
import org.smartrplace.app.monbase.power.ConsumptionEvalTableLineI;

public class ConsumptionEvalPageEntry {
	public ConsumptionEvalTableLineI line;
	public Collection<ConsumptionEvalTableLineI> allLines;
	public ConsumptionEvalTableI<?> page;
}
