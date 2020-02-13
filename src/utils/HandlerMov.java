package utils;

/*******************************************************************
* <b> STANDARD HANDLER CLASS BY mario.musico@electrolux.com </b> <p>
* void setHome(String nextFramePath) <p>
* void PTPhome() <p>
* void PTP (String nextFramePath, double relSpeed) <p>
* void LIN (String nextFramePath, double relSpeed) <p>
* void CIRC (String nextFramePath1, String nextFramePath2, double relSpeed) <p>
* ICondition getJTConds() <p>
* void setJTConds (double maxTorque) <p>
* void PTPwithJTConds (Frame nextFrame / String nextFramePath, double relSpeed) <p>
* void LINRELwithJTConds (int x, int y, int z, double relSpeed) <p>
* void checkComponentZ (int probeDist, double relSpeed) <p>
*/

import static com.kuka.roboticsAPI.motionModel.BasicMotions.circ;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.lin;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.linRel;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.positionHold;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.ptp;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.ptpHome;
import static utils.Utils.padLog;
import static utils.Utils.waitMillis;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.conditionModel.ICondition;
import com.kuka.roboticsAPI.conditionModel.JointTorqueCondition;
import com.kuka.roboticsAPI.deviceModel.JointEnum;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.executionModel.IFiredConditionInfo;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;
import com.kuka.task.ITaskLogger;

@Singleton
public class HandlerMov extends RoboticsAPIApplication {
	// Standard KUKA API objects
	@Override public void run() { while (true) { break; } }		// Compulsory method for RoboticsAPIApplication derived classes
	@Inject private LBR kiwa;
	@Inject private HandlerMFio mf;
	@Inject private ITaskLogger log;
	
	// Private properties
	private ICondition JTConds;
	private IMotionContainer JTBMotion;
	private IFiredConditionInfo JTBreak;
	
	// CONSTRUCTOR
	@Inject	public HandlerMov(HandlerMFio _mf) {
		this.mf = _mf;
	}
	
	// Standard moves, simple calls ***************************************************************
	
	public void setHome(String nextFramePath) {
		kiwa.setHomePosition(getApplicationData().getFrame(nextFramePath));
	}
	
	public void PTPhome() { kiwa.move(ptpHome()); }
	
	public void PTP(String nextFramePath, double relSpeed) {
		log.info("Move PTP to " + nextFramePath);
		kiwa.move(ptp(getApplicationData().getFrame(nextFramePath))
					.setJointVelocityRel(relSpeed));
	}
	
	public void PTP(Frame nextFrame, double relSpeed) {
		kiwa.move(ptp(nextFrame).setJointVelocityRel(relSpeed));
	}
	
	public void LIN(String nextFramePath, double relSpeed) {
		log.info("Move LIN to " + nextFramePath);
		kiwa.move(lin(getApplicationData().getFrame(nextFramePath))
					.setJointVelocityRel(relSpeed));
	}
	
	public void CIRC(String nextFramePath1, String nextFramePath2, double relSpeed) {
		log.info("Move CIRC to " + nextFramePath1 + " then to " + nextFramePath2);
		kiwa.move(circ(	getApplicationData().getFrame(nextFramePath1),
						getApplicationData().getFrame(nextFramePath2))
					.setJointVelocityRel(relSpeed));
	}
	
	// Torque sensing enabled macros **************************************************************
	
	public ICondition getJTConds() { return this.JTConds; }
	
	public void setJTConds(double maxTorque){
		JointTorqueCondition JTCond[] = new JointTorqueCondition[8];
		JTCond[1] = new JointTorqueCondition(JointEnum.J1, -maxTorque, maxTorque);	
		JTCond[2] = new JointTorqueCondition(JointEnum.J2, -maxTorque, maxTorque);
		JTCond[3] = new JointTorqueCondition(JointEnum.J3, -maxTorque, maxTorque);	
		JTCond[4] = new JointTorqueCondition(JointEnum.J4, -maxTorque, maxTorque);
		JTCond[5] = new JointTorqueCondition(JointEnum.J5, -maxTorque, maxTorque);	
		JTCond[6] = new JointTorqueCondition(JointEnum.J6, -maxTorque, maxTorque);
		JTCond[7] = new JointTorqueCondition(JointEnum.J7, -maxTorque, maxTorque);
		JTConds = JTCond[1].or(JTCond[2]).or(JTCond[3]).or(JTCond[4]).or(JTCond[5]).or(JTCond[6]).or(JTCond[7]);
		log.info("Max Axis Torque set to " + maxTorque + " Nm.");
	}
	
	public void PTPwithJTConds(Frame nextFrame, double relSpeed){		// overloading for taught points
		do {
			mf.setRGB("G");
			this.JTBMotion = kiwa.move(ptp(nextFrame).setJointVelocityRel(relSpeed).breakWhen(this.JTConds)); 
			this.JTBreak = this.JTBMotion.getFiredBreakConditionInfo();
			if (JTBreak != null) {
				mf.setRGB("RB");
				kiwa.move(linRel(0, 0, -30).setJointVelocityRel(1));
				log.warn("Collision detected!"); 
				mf.waitUserButton();
				PTPwithJTConds(nextFrame, relSpeed);
			}
		} while (JTBreak != null);
	}
	
	public void PTPwithJTConds(String nextFramePath, double relSpeed){
		ObjectFrame nextFrame = getApplicationData().getFrame(nextFramePath);
		this.PTPwithJTConds(nextFrame.copyWithRedundancy(), relSpeed);
	}
	
	public void LINwithJTConds(Frame nextFrame, double relSpeed){		// overloading for taught points
		do {
			mf.setRGB("G");
			this.JTBMotion = kiwa.move(lin(nextFrame).setJointVelocityRel(relSpeed).breakWhen(this.JTConds)); 
			this.JTBreak = this.JTBMotion.getFiredBreakConditionInfo();
			if (JTBreak != null) {
				mf.setRGB("RB");
				kiwa.move(linRel(0, 0, -30).setJointVelocityRel(1));
				log.warn("Collision detected!"); 
				mf.waitUserButton();
				LINwithJTConds(nextFrame, relSpeed);
			}
		} while (JTBreak != null);
	}
	
	public boolean twistJ7(double angle, double relSpeed, double maxTorque) {
		padLog("Twisting the pin...");
		mf.blinkRGB("GB", 250);
		JointTorqueCondition JTCond = new JointTorqueCondition(JointEnum.J7, -maxTorque, maxTorque);
		this.JTBMotion = kiwa.move(linRel(Transformation.ofDeg(0, 0, 0, angle, 0, 0)).setJointVelocityRel(relSpeed).breakWhen(JTCond)); // Tx, Ty, Tz, Rz, Ry, Rx
		this.JTBreak = this.JTBMotion.getFiredBreakConditionInfo();
		if (JTBreak != null) {
			padLog("Rotation reached limit due to torque sensing");
			mf.blinkRGB("GB", 500);
			return true;
		} else {
			padLog("Full rotation performed, no obstacle detected");
			mf.blinkRGB("RB", 500);
			return false;
		}
	}
	
	public boolean ckeckHole(double tolerance, double relSpeed, double maxTorque) {
		padLog("Testing hole insertion...");
		mf.blinkRGB("GB", 250);
		ICondition tempJTConds = this.JTConds;
		this.setJTConds(maxTorque);
		mf.setRGB("G");
		Frame holeFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
		
		this.JTBMotion = kiwa.move(linRel(-tolerance, -tolerance, 0).setJointVelocityRel(relSpeed).breakWhen(JTConds));
		this.JTBreak = this.JTBMotion.getFiredBreakConditionInfo();
		if (this.JTBreak != null) mf.blinkRGB("GB", 200);
		else { mf.blinkRGB("RB", 200); return false; }
		this.PTP(holeFrame, relSpeed);
		
		this.JTBMotion = kiwa.move(linRel(tolerance, tolerance, 0).setJointVelocityRel(relSpeed).breakWhen(JTConds));
		this.JTBreak = this.JTBMotion.getFiredBreakConditionInfo();
		if (this.JTBreak != null) mf.blinkRGB("GB", 200);
		else { mf.blinkRGB("RB", 200); return false; }
		this.PTP(holeFrame, relSpeed);
		
		this.JTConds = tempJTConds;
		return true;
	}
	
	public void LINRELwithJTConds(int x, int y, int z, double relSpeed) {
		int holeFindError = 7;
		Frame currentFrame, targetFrame;
		ICondition tempJTConds = this.JTConds;
		this.setJTConds(3);
		mf.setRGB("G");
		this.JTBMotion = kiwa.move(linRel(x, y, z).setJointVelocityRel(relSpeed).breakWhen(JTConds)); 
		do {
			this.JTBreak = this.JTBMotion.getFiredBreakConditionInfo();
			if (JTBreak != null) {
				currentFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
				targetFrame = kiwa.getCommandedCartesianPosition(kiwa.getFlange());
				if (Math.abs(currentFrame.getX() - targetFrame.getX()) < holeFindError && 
					Math.abs(currentFrame.getY() - targetFrame.getY()) < holeFindError && 
					Math.abs(currentFrame.getZ() - targetFrame.getZ()) < holeFindError) {
					log.warn("Collision inside error margin for Pin/Hole search.");
					mf.blinkRGB("GB", 500);
					break; 
				} else {
					log.warn("Collision detected!"); 
					mf.setRGB("RB");
					kiwa.move(linRel(0, 0, -30).setJointVelocityRel(relSpeed));
					mf.waitUserButton();
					mf.setRGB("G");
					this.JTBMotion = kiwa.move(lin(targetFrame).setJointVelocityRel(relSpeed).breakWhen(JTConds));
				}
			}
		} while (JTBreak != null);
		this.JTConds = tempJTConds;
	}
	
	public void checkComponentZ(int probeDist, double relSpeed, CartesianImpedanceControlMode stiffMode){
		Frame currentFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
		boolean pieceFound = false;
		log.info("Checking component presence...");
		do {
			mf.setRGB("G");
			this.JTBMotion = kiwa.move(linRel(0, 0, probeDist).setJointVelocityRel(relSpeed).breakWhen(JTConds)); 
			this.JTBreak = this.JTBMotion.getFiredBreakConditionInfo();
			if (JTBreak != null) {
				log.info("Component detected. " ); 
				mf.blinkRGB("GB", 800);
				kiwa.move(lin(currentFrame).setJointVelocityRel(relSpeed));
				pieceFound = true;
			} else {
				mf.setRGB("RB");
				log.warn("No components detected, Reposition the workpiece correctly and push the cobot (gesture control)." );
				kiwa.move(lin(currentFrame).setJointVelocityRel(relSpeed));
				kiwa.move(positionHold(stiffMode, -1, null).breakWhen(JTConds));
			}
		} while (!pieceFound);
		waitMillis(250);
	}
}