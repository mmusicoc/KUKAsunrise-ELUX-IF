package eluxLibs;

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
* void probeZ (int probeDist, double relSpeed) <p>
*/

import static com.kuka.roboticsAPI.motionModel.BasicMotions.circ;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.lin;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.linRel;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.positionHold;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.ptp;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.ptpHome;
import static eluxLibs.Utils.padLog;
import static eluxLibs.Utils.waitMillis;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.conditionModel.ICondition;
import com.kuka.roboticsAPI.conditionModel.JointTorqueCondition;
import com.kuka.roboticsAPI.deviceModel.JointEnum;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.executionModel.IFiredConditionInfo;
import com.kuka.roboticsAPI.geometricModel.CartDOF;
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
	private CartesianImpedanceControlMode stiffMode;
	private double _maxTorque;
	
	// CONSTRUCTOR
	@Inject	public HandlerMov(HandlerMFio _mf) {
		this.mf = _mf;
		stiffMode = new CartesianImpedanceControlMode();
		stiffMode.parametrize(CartDOF.TRANSL).setStiffness(5000).setDamping(1);
		stiffMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(1); 
	}
	
	// Custom modularizing handler objects
		@Inject private HandlerPad pad = new HandlerPad(mf);
	
	// Standard moves, simple calls ***************************************************************
	
	public void setHome(String nextFramePath) {
		kiwa.setHomePosition(getApplicationData().getFrame(nextFramePath));
	}
	
	public void PTPhome() { kiwa.move(ptpHome()); }
	
	public void PTP(Frame targetFrame, double relSpeed) {
		kiwa.move(ptp(targetFrame).setJointVelocityRel(relSpeed));
	}
	
	public void PTP(String targetFramePath, double relSpeed) {
		log.info("Move PTP to " + targetFramePath);
		ObjectFrame targetFrame = getApplicationData().getFrame(targetFramePath);
		this.PTP(targetFrame.copyWithRedundancy(), relSpeed);
	}
	
	public void LIN(Frame targetFrame, double relSpeed) {
		kiwa.move(lin(targetFrame).setJointVelocityRel(relSpeed));
	}
	
	public void LIN(String targetFramePath, double relSpeed) {
		log.info("Move LIN to " + targetFramePath);
		ObjectFrame targetFrame = getApplicationData().getFrame(targetFramePath);
		this.LIN(targetFrame.copyWithRedundancy(), relSpeed);
	}	
	
	public void LINREL(double x, double y, double z, double relSpeed) {
		kiwa.move(linRel(x, y, z).setJointVelocityRel(relSpeed));
	}
	
	public void CIRC(Frame targetFrame1, Frame targetFrame2, double relSpeed) {
		kiwa.move(circ(targetFrame1, targetFrame2).setJointVelocityRel(relSpeed));
	}	
	
	public void CIRC(String targetFramePath1, String targetFramePath2, double relSpeed) {
		log.info("Move CIRC to " + targetFramePath1 + " then to " + targetFramePath2);
		ObjectFrame targetFrame1 = getApplicationData().getFrame(targetFramePath1);
		ObjectFrame targetFrame2 = getApplicationData().getFrame(targetFramePath2);
		this.CIRC(targetFrame1.copyWithRedundancy(), targetFrame2.copyWithRedundancy(), relSpeed);
	}
	
	// Torque sensing enabled macros **************************************************************
	
	public ICondition getJTConds() { return this.JTConds; }
	
	public void setJTConds(double maxTorque){
		this._maxTorque = maxTorque;
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
	
	public void PTPwithJTConds(Frame targetFrame, double relSpeed){		// overloading for taught points
		do {
			mf.setRGB("G");
			this.JTBMotion = kiwa.move(ptp(targetFrame).setJointVelocityRel(relSpeed).breakWhen(this.JTConds)); 
			this.JTBreak = this.JTBMotion.getFiredBreakConditionInfo();
			if (JTBreak != null) {
				mf.setRGB("RB");
				this.LINREL(0, 0, -30, 1);
				log.warn("Collision detected!"); 
				mf.waitUserButton();
				relSpeed *= 0.5;
				PTPwithJTConds(targetFrame, relSpeed);
			}
		} while (JTBreak != null);
	}
	
	public void PTPwithJTConds(String targetFramePath, double relSpeed){
		ObjectFrame targetFrame = getApplicationData().getFrame(targetFramePath);
		this.PTPwithJTConds(targetFrame.copyWithRedundancy(), relSpeed);
	}
	
	public void LINwithJTConds(Frame targetFrame, double relSpeed){		// overloading for taught points
		do {
			mf.setRGB("G");
			this.JTBMotion = kiwa.move(lin(targetFrame).setJointVelocityRel(relSpeed).breakWhen(this.JTConds)); 
			this.JTBreak = this.JTBMotion.getFiredBreakConditionInfo();
			if (JTBreak != null) {
				mf.setRGB("RB");
				this.LINREL(0, 0, -30, 1);
				log.warn("Collision detected!"); 
				mf.waitUserButton();
				relSpeed *= 0.5;
				LINwithJTConds(targetFrame, relSpeed);
			}
		} while (JTBreak != null);
	}
	
	public void waitPushGesture() {
		mf.saveRGB();
		mf.setRGB("B");
		kiwa.move(positionHold(stiffMode, -1, null).breakWhen(JTConds));
		waitMillis(500);
		mf.resetRGB();
	}
	
	public void waitPushGestureZ(Frame targetFrame) {
		Frame preFrame = targetFrame.copyWithRedundancy();
		preFrame.setZ(preFrame.getZ() + 50);
		this.LINwithJTConds(preFrame, 0.1);
		this.waitPushGesture();
		this.LINwithJTConds(targetFrame, 0.1);
	}
	
	public void waitPushGestureY(Frame targetFrame) {
		Frame preFrame = targetFrame.copyWithRedundancy();
		preFrame.setY(preFrame.getY() + 50);
		this.LINwithJTConds(preFrame, 0.1);
		this.waitPushGesture();
		this.LINwithJTConds(targetFrame, 0.1);
	}
	
	public void checkPartZ(int probeDist, double relSpeed) {
		Frame targetFrame = kiwa.getCommandedCartesianPosition(kiwa.getFlange());
		boolean pieceFound = false;
		log.info("Checking component presence...");
		do {
			mf.setRGB("G");
			this.JTBMotion = kiwa.move(linRel(0, 0, probeDist).setJointVelocityRel(relSpeed).breakWhen(JTConds)); 
			this.JTBreak = this.JTBMotion.getFiredBreakConditionInfo();
			if (JTBreak != null) {
				log.info("Component detected. " ); 
				mf.blinkRGB("GB", 800);
				kiwa.move(lin(targetFrame).setJointVelocityRel(relSpeed));
				pieceFound = true;
			} else {
				mf.setRGB("RB");
				log.warn("No components detected, reposition the workpiece correctly and push the cobot (gesture control)." );
				this.waitPushGestureZ(targetFrame);
			}
		} while (!pieceFound);
		waitMillis(250);
	}
	
	public void checkPinPick(double tolerance, double relSpeed) {
		Frame targetFrame = kiwa.getCommandedCartesianPosition(kiwa.getFlange());
		boolean pinFound;
		do {
			pinFound = probeXY(tolerance, relSpeed, 3);
			if (pinFound) {
				log.info("Pin found. " ); 
				mf.blinkRGB("GB", 800);
				pinFound = true;
			} else {
				mf.setRGB("RB");
				log.warn("No pin found, insert one correctly and push the cobot (gesture control)." );
				this.waitPushGestureZ(targetFrame);
			}
		} while (!pinFound);
		waitMillis(250);
	}
	
	public void checkPinPlace(double tolerance, double relSpeed) {
		Frame targetFrame = kiwa.getCommandedCartesianPosition(kiwa.getFlange());
		boolean holeFound;
		do {
			holeFound = probeXY(tolerance, relSpeed, 3);
			if (holeFound) {
				log.info("Hole found. " ); 
				mf.blinkRGB("GB", 800);
				holeFound = true;
			} else {
				mf.setRGB("RB");
				log.warn("No hole found, reposition machine frame correctly and push the cobot (gesture control)." );
				this.waitPushGestureY(targetFrame);
			}
		} while (!holeFound);
		waitMillis(250);
	}
	
	private boolean probeXY(double tolerance, double relSpeed, double maxTorque) {
		boolean found = false;
		padLog("Testing hole insertion...");
		mf.blinkRGB("GB", 250);
		double prevMaxTorque = this._maxTorque;
		this.setJTConds(maxTorque);
		mf.setRGB("G");
		Frame holeFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
		this.JTBMotion = kiwa.move(linRel(-tolerance, -tolerance, 0).setJointVelocityRel(relSpeed).breakWhen(JTConds));
		this.JTBreak = this.JTBMotion.getFiredBreakConditionInfo();
		if (this.JTBreak != null) { mf.blinkRGB("GB", 200); found = true; }
		else mf.blinkRGB("RB", 200);
		this.PTP(holeFrame, relSpeed);
		if (found) {
			this.JTBMotion = kiwa.move(linRel(tolerance, tolerance, 0).setJointVelocityRel(relSpeed).breakWhen(JTConds));
			this.JTBreak = this.JTBMotion.getFiredBreakConditionInfo();
			if (this.JTBreak != null) mf.blinkRGB("GB", 200);
			else { mf.blinkRGB("RB", 200); found = false; }
			this.PTP(holeFrame, relSpeed);
		}
		this.setJTConds(prevMaxTorque);
		return found;
	}
	
	public boolean twistJ7withJTCond(double angle, double extra, double relSpeed, double maxTorque) {
		padLog("Twisting the pin...");
		mf.blinkRGB("GB", 250);
		JointTorqueCondition JTCond = new JointTorqueCondition(JointEnum.J7, -maxTorque, maxTorque);
		try {
			kiwa.move(linRel(Transformation.ofDeg(0,0,0,angle,0,0)).setJointVelocityRel(relSpeed));
			this.JTBMotion = kiwa.move(linRel(Transformation.ofDeg(0, 0, 0, extra, 0, 0)).setJointVelocityRel(relSpeed).breakWhen(JTCond)); // Tx, Ty, Tz, Rz, Ry, Rx
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
		} catch (Exception e) {
			pad.info("The wrist twist exceeded the limits, try to teach again a reachable position.");
			return true;
		}
	}
}