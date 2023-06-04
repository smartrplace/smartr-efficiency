package org.smartrplace.apps.alarmingconfig.release;

public enum FinalAnalysis {
	
	PN101("PN101", "VPN-Verbindung auf Laptop möglich, aber nicht vom Controller"),
	PN120("PN120", "Gateway offline (Internetverbindung über Kundenanschluss)"),
	PN120_1("PN120.1", "Fehlkonfiguration oder Softwarefehler Smartrplace");
	
	private final String code;
	private final String description;
	
	private FinalAnalysis(String code, String description) {
		this.code=code;
		this.description=description;
	}
	
	

}
