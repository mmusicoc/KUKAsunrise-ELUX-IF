package application.Cambrian;

//import static EluxUtils.Utils.*;
//import static EluxUtils.UMath.*;
import EluxUtils.SimpleFrame;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;

public class Joint {
	private int jointID;
	private String cambrianModel;
	private SimpleFrame NJ;			// Nominal Joint position
	private SimpleFrame DO; 		// Detection Avg Offset to Nominal Joint
	private SimpleFrame SPO; 		// ScanPoint relative to Nominal Joint
	private double threshold[];					// X+,X-,Y+,Y-,Z+,Z-
	
	public Joint() { 	// CONSTRUCTOR
		NJ = new SimpleFrame();
		DO = new SimpleFrame();
		//SPO = new SimpleFrame();
	}
	
	// GETTERS ---------------------------------------------------------------
	public int getID() { return this.jointID; }
	public String getModel() { return this.cambrianModel; }
	public SimpleFrame getNominalTarget() { return this.NJ; }
	public Transformation getDO() { 
		return Transformation.ofDeg(DO.X, DO.Y, DO.Z, 
									DO.A, DO.B, DO.C); }
	public Transformation getScanPointOffset() { 
		return Transformation.ofDeg(SPO.X, SPO.Y, SPO.Z, 
									SPO.A, SPO.B, SPO.C); }
	
	public boolean checkWithinThreshold(Frame detection) {
		Frame detectionWithOffset = detection.transform(this.getDO());
		boolean outOfThreshold = false;
		double delta;
		delta = detectionWithOffset.getX() - NJ.X;
		if(delta > threshold[0] || delta < threshold[1]) outOfThreshold = true;
		delta = detectionWithOffset.getY() - NJ.Y;
		if(delta > threshold[2] || delta < threshold[3]) outOfThreshold = true;
		delta = detectionWithOffset.getZ() - NJ.Z;
		if(delta > threshold[4] || delta < threshold[5]) outOfThreshold = true;
		return outOfThreshold;
	}
	
	// SETTERS --------------------------------------------------------------
	public void setID(int _id) { this.jointID = _id; }
	public void setModel(String _model) { this.cambrianModel = _model; }
	public void setNominalTarget(double x, double y, double z, 
								 double a, double b, double c) { 
		NJ.build(x, y, z, a, b, c); }
	
	public void setDetectionOffset(double x, double y, double z, 
								 double a, double b, double c) { 
		DO.build(x, y, z, a, b, c); }
	
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