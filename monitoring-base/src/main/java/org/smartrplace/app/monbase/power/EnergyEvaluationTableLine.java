package org.smartrplace.app.monbase.power;

import java.util.List;

import org.ogema.model.connections.ElectricityConnection;
import org.smartrplace.app.monbase.power.ConsumptionEvalAdmin.SumType;

public class EnergyEvaluationTableLine extends ConsumptionEvalTableLineBase {
	//protected final EnergyEvalObjI conn;

	/** 
	 * 
	 * @param conn
	 * @param label
	 * @param lineShowsPower
	 * @param sumUpIndex a sumup is a value that is calculated as the sum  of several other lines. The
	 * sumUpIndex indicates to which sum the line shall contribute. Sumup indices must be used consequently
	 * starting from zero.
	 * @param sumUpResult if not null the line is a sumup result line. The line must be ordered after all
	 * contributors are processed. If negative the line is the init line in which all sumUps are reset.
	 */
	public EnergyEvaluationTableLine(ElectricityConnection conn, String label, boolean lineShowsPower,
			//EnergyEvaluationTableLine sumUpIndex,
			SumType type,
			List<ConsumptionEvalTableLineI> sourcesToSum,
			//List<EnergyEvaluationTableLine> sumLinesToClear,
			int index) {
		this(new EnergyEvalElConnObj(conn), label, lineShowsPower, type, sourcesToSum, index);
	}
	public EnergyEvaluationTableLine(EnergyEvalObjI conn, String label, boolean lineShowsPower,
			//EnergyEvaluationTableLine sumUpIndex,
			SumType type,
			List<ConsumptionEvalTableLineI> sourcesToSum,
			//List<EnergyEvaluationTableLine> sumLinesToClear,
			int index) {
		super(conn, label, lineShowsPower, type, sourcesToSum, index);
		//this.conn = conn;
		//this.lineShowsPower = lineShowsPower;
		//this.label = label;
		//this.sumUpIndex2 = sumUpIndex;
		//this.type = type;
		//this.sourcesToSum = sourcesToSum;
		//this.sumLinesToClear = sumLinesToClear;
		//this.index = String.format("%06d", index);
		//	sumUps = null;
	}
}
