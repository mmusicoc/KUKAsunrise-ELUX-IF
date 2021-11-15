package application.Cambrian;

import EluxAPI.SimpleFrame;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;

public class CambrianJoint {
	private String jointName;
	private String cambrianModel;
	private SimpleFrame nominalTarget;
	private Transformation detectionOffset; // X,Y,Z
	private Transformation scanPointOffset; // X,Y,Z
	private double threshold[];					// X+,X-,Y+,Y-,Z+,Z-
	
	public CambrianJoint() { 	// CONSTRUCTOR
		nominalTarget = new SimpleFrame();
	}
	
	// GETTERS ---------------------------------------------------------------
	public String getName() { return this.jointName; }
	public String getModel() { return this.cambrianModel; }
	public SimpleFrame getNominalTarget() { return this.nominalTarget; }
	public Transformation getDetectionOffset() { return this.detectionOffset; }
	public Transformation getScanPointOffset() { return this.scanPointOffset; }
	
	public boolean checkWithinThreshold(Frame detection) {
		Frame detectionWithOffset = detection.transform(scanPointOffset);
		boolean outOfThreshold = false;
		double delta;
		delta = detectionWithOffset.getX() - nominalTarget.getX();
		if(delta > threshold[0] || delta < threshold[1]) outOfThreshold = true;
		delta = detectionWithOffset.getY() - nominalTarget.getY();
		if(delta > threshold[2] || delta < threshold[3]) outOfThreshold = true;
		delta = detectionWithOffset.getZ() - nominalTarget.getZ();
		if(delta > threshold[4] || delta < threshold[5]) outOfThreshold = true;
		return outOfThreshold;
	}
	
	// SETTERS --------------------------------------------------------------
	public void setName(String _name) { this.jointName = _name; }
	public void setModel(String _model) { this.cambrianModel = _model; }
	public void setNominalTarget(double x, double y, double z, 
			double a, double b, double c) { 
		nominalTarget.build(x, y, z, a, b, c); }
	
	public void setDetectionOffset(Transformation offset) {
		detectionOffset = offset;
	}
	public void setScanPointOffset(Transformation offset) {
		scanPointOffset = offset; }
	
	public void setThreshold(double Xpos, double Xneg, 
							 double Ypos, double Yneg,
							 double Zpos, double Zneg) {
		threshold[0] = Xpos;
		threshold[1] = Xneg;
		threshold[2] = Ypos;
		threshold[3] = Yneg;
		threshold[4] = Zpos;
		threshold[5] = Zneg;
	}
}