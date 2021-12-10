package EluxRemote;

public class Remote {
	private boolean idle;
	private double speed;
	private String prog;
	
	public Remote() { }	// CONSTRUCTOR
	
	// GETTERS ---------------------------------------------------------------
	public boolean getIdle() { return idle; }
	public double getSpeed() { return speed; }
	public String getProg() { return prog; }
	
	// SETTERS ---------------------------------------------------------------
	public void setIdle(boolean _idle) {
		idle = _idle;
	}
	
	public void setSpeed(double _speed) {
		speed = _speed;
	}
	
	public void setProg(String _prog) {
		prog = _prog;
	}
}