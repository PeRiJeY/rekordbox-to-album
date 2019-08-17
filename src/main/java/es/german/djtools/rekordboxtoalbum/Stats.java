package es.german.djtools.rekordboxtoalbum;

public class Stats {
	private static Stats stats = null;
	
	private long numElementsProcessed = 0;
	private long numElementsUpdated = 0;
	
	private Stats() {
		this.numElementsProcessed = 0;
		this.numElementsUpdated = 0;
	}
	
	public static Stats getInstance() {
		if (stats == null) stats = new Stats();
		return stats;
	}
	
	public long addElementProcessed() {
		return ++stats.numElementsProcessed;
	}
	
	public long addElementUpdated() {
		return ++stats.numElementsUpdated;
	}
	
	public long getElementsProcessed() {
		return stats.numElementsProcessed;
	}
	
	public long getElementsUpdated() {
		return stats.numElementsUpdated;
	}
	
}
