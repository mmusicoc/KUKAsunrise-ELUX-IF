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

import static eluxLibs.Utils.*;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.motionModel.PositionHold;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;
import com.kuka.roboticsAPI.conditionModel.ICondition;
import com.kuka.roboticsAPI.conditionModel.JointTorqueCondition;
import com.kuka.roboticsAPI.deviceModel.JointEnum;
import com.kuka.roboticsAPI.executionModel.CommandInvalidException;
import com.kuka.roboticsAPI.executionModel.IFiredConditionInfo;
import com.kuka.roboticsAPI.geometricModel.*;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;
import com.kuka.task.ITaskLogger;

@Singleton
public class HandlerMov extends RoboticsAPIApplication {
	// Standard KUKA API objects
	@Override public void run() { while (true) { break; } }		// Compulsory method for RoboticsAPIApplication derived classes
	@Inject private LBR kiwa;
	@Inject private HandlerMFio mf;
	@Inject private ITaskLogger log;
	//@Inject @Named("Flange4kg") private Tool tcp;
	private ObjectFrame tcp;
	
	// Private properties
	private static final boolean log1 = false;
	private ICondition _JTConds;
	private IMotionContainer _JTBMotion;
	private IFiredConditionInfo _JTBreak;
	private CartesianImpedanceControlMode _softMode, _stiffMode;
	private PositionHold _posHold;
	private String _homeFramePath;
	private double[] _speed = {0,0,0,0,0,0,0};
	private double _maxTorque;
	private double _releaseDist;
	private boolean _lockDir;
	
	// CONSTRUCTOR
	@Inject	public HandlerMov(HandlerMFio _mf) {
		this.mf = _mf;
		_softMode = new CartesianImpedanceControlMode();
		_stiffMode = new CartesianImpedanceControlMode();
		_stiffMode.parametrize(CartDOF.TRANSL).setStiffness(5000).setDamping(1);
		_stiffMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(1);
		this.setGlobalSpeed(0.25);
		_maxTorque = 5;
		_releaseDist = 10;
		_lockDir = false;
	}
	
	// Custom modularizing handler objects
	@Inject private HandlerPad pad = new HandlerPad(mf);

	// GETTERS
		
	public ObjectFrame getTCP() { return this.tcp; }
	public PositionHold getPosHold() { return _posHold; }
	public ICondition getJTConds() { return this._JTConds; }
	public double[] scaleSpeed(double relSpeed) {
		double[] scaledSpeed = _speed;
		for (int i = 0; i < 7; i++) scaledSpeed[i] *= relSpeed;
		return scaledSpeed;
	}
		
	// SETTERS
	
	public void setGlobalSpeed(double speed) { 
		for (int i = 0; i < 7; i++) this._speed[i] = speed;
	}
	public void setA7Speed(double speed) { this._speed[6] = speed; }
	public void setSafeRelease(double releaseDist) { this._releaseDist = releaseDist; }
	public void setHome(String targetFramePath) {
		_homeFramePath = targetFramePath;
		kiwa.setHomePosition(getApplicationData().getFrame(targetFramePath));
	}
	
	public void setTCP(Tool _tool, String _tcp) {
		_tool.attachTo(kiwa.getFlange());
		tcp = _tool.getFrame(_tcp);
		padLog("TCP set to " + _tool.getName() + ", frame " + tcp.getName());
	}
	
	public void swapLockDir()  {
		this._softMode.parametrize(CartDOF.ALL).setStiffness(0.1).setDamping(1);		// HandGuiding
		_lockDir = !_lockDir;
		if (_lockDir) {
			this._softMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(1);
			this._softMode.parametrize(CartDOF.A).setStiffness(0.1).setDamping(1);
		}
		_posHold = new PositionHold(_softMode, -1, null);
	}
	
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
		_JTConds = JTCond[1].or(JTCond[2]).or(JTCond[3]).or(JTCond[4]).or(JTCond[5]).or(JTCond[6]).or(JTCond[7]);
		if(log1) log.info("Max Axis Torque set to " + maxTorque + " Nm.");
	}
	
	// Standard moves, simple calls ***************************************************************
	
	public boolean PTP(Frame targetFrame, double relSpeed) {
		try { tcp.move(ptp(targetFrame).setJointVelocityRel(scaleSpeed(relSpeed))); return true; }
		catch(CommandInvalidException e) { padErr("Unable to perform movement"); return false; }
	}
	public boolean PTP(String targetFramePath, double relSpeed) {
		ObjectFrame targetFrame = getApplicationData().getFrame(targetFramePath);
		return this.PTP(targetFrame.copyWithRedundancy(), relSpeed);
	}
	
	public boolean PTPHOME(double relSpeed) {
		return this.PTP(_homeFramePath, relSpeed);
	}
	
	public boolean LIN(Frame targetFrame, double relSpeed) {
		try { tcp.move(lin(targetFrame).setJointVelocityRel(scaleSpeed(relSpeed))); return true; }
		catch(CommandInvalidException e) { padErr("Unable to perform movement"); return false; }
	}
	public boolean LIN(String targetFramePath, double relSpeed) {
		ObjectFrame targetFrame = getApplicationData().getFrame(targetFramePath);
		return this.LIN(targetFrame.copyWithRedundancy(), relSpeed);
	}	
	
	public boolean LINREL(double x, double y, double z, double relSpeed) {
		try { tcp.move(linRel(x, y, z).setJointVelocityRel(scaleSpeed(relSpeed))); return true; }
		catch(CommandInvalidException e) { padErr("Unable to perform movement"); return false; }
	}
	
	public boolean CIRC(Frame targetFrame1, Frame targetFrame2, double relSpeed) {
		try { tcp.move(circ(targetFrame1, targetFrame2).setJointVelocityRel(scaleSpeed(relSpeed))); return true; }
		catch(CommandInvalidException e) { padErr("Unable to perform movement"); return false; }
	}	
	public boolean CIRC(String targetFramePath1, String targetFramePath2, double relSpeed) {
		ObjectFrame targetFrame1 = getApplicationData().getFrame(targetFramePath1);
		ObjectFrame targetFrame2 = getApplicationData().getFrame(targetFramePath2);
		return this.CIRC(targetFrame1.copyWithRedundancy(), targetFrame2.copyWithRedundancy(), relSpeed);
	}
	
	// Torque sensing enabled macros **************************************************************
	
	public void PTPHOMEsafe() {
		this.LINREL(0, 0, -0.01, 0.5);
		pad.info("Move away from the robot. It will move automatically to home.");
		this.LINREL(0, 0, -50, 0.5);
		this.PTPsafe(_homeFramePath, _speed[0]);
	}
	
	public void PTPsafe(Frame targetFrame, double relSpeed){		// overloading for taught points
		do {
			mf.setRGB("G");
			this._JTBMotion = kiwa.move(ptp(targetFrame).setJointVelocityRel(scaleSpeed(relSpeed)).breakWhen(this._JTConds)); 
			this._JTBreak = this._JTBMotion.getFiredBreakConditionInfo();
			if (_JTBreak != null) {
				mf.setRGB("RB");
				this.LINREL(0, 0, -_releaseDist, 1);
				log.warn("Collision detected!"); 
				mf.waitUserButton();
				relSpeed *= 0.5;
				PTPsafe(targetFrame, relSpeed);
			}
		} while (_JTBreak != null);
	}
	
	public void PTPsafe(String targetFramePath, double relSpeed){
		ObjectFrame targetFrame = getApplicationData().getFrame(targetFramePath);
		this.PTPsafe(targetFrame.copyWithRedundancy(), relSpeed);
	}
	
	public void LINsafe(Frame targetFrame, double relSpeed){		// overloading for taught points
		do {
			mf.setRGB("G");
			this._JTBMotion = kiwa.move(lin(targetFrame).setJointVelocityRel(scaleSpeed(relSpeed)).breakWhen(this._JTConds)); 
			this._JTBreak = this._JTBMotion.getFiredBreakConditionInfo();
			if (_JTBreak != null) {
				mf.setRGB("RB");
				this.LINREL(0, 0, -_releaseDist, 1);
				log.warn("Collision detected!"); 
				mf.waitUserButton();
				relSpeed *= 0.5;
				LINsafe(targetFrame, relSpeed);
			}
		} while (_JTBreak != null);
	}
	
	public void LINsafe(String targetFramePath, double relSpeed){
		ObjectFrame targetFrame = getApplicationData().getFrame(targetFramePath);
		this.LINsafe(targetFrame.copyWithRedundancy(), relSpeed);
	}
	
	public void CIRCsafe(Frame targetFrame1, Frame targetFrame2, double relSpeed){		// overloading for taught points
		do {
			mf.setRGB("G");
			this._JTBMotion = kiwa.move(circ(targetFrame1, targetFrame2).setJointVelocityRel(scaleSpeed(relSpeed)).breakWhen(this._JTConds)); 
			this._JTBreak = this._JTBMotion.getFiredBreakConditionInfo();
			if (_JTBreak != null) {
				mf.setRGB("RB");
				this.LINREL(0, 0, -_releaseDist, 1);
				log.warn("Collision detected!"); 
				mf.waitUserButton();
				relSpeed *= 0.5;
				LINsafe(targetFrame2, relSpeed);			// SECURITY MEASURE, NEW CIRC CAN OVERSHOOT!!
			}
		} while (_JTBreak != null);
	}
	
	public void CIRCsafe(String targetFramePath1, String targetFramePath2, double relSpeed){
		ObjectFrame targetFrame1 = getApplicationData().getFrame(targetFramePath1);
		ObjectFrame targetFrame2 = getApplicationData().getFrame(targetFramePath2);
		this.CIRCsafe(targetFrame1.copyWithRedundancy(), targetFrame2.copyWithRedundancy(), relSpeed);
	}
	
	public void waitPushGesture() {
		mf.saveRGB();
		mf.setRGB("B");
		kiwa.move(positionHold(_stiffMode, -1, null).breakWhen(_JTConds));
		waitMillis(500);
		mf.resetRGB();
	}
	
	public void waitPushGestureZ(Frame targetFrame) {
		Frame preFrame = targetFrame.copyWithRedundancy();
		preFrame.setZ(preFrame.getZ() + 50);
		this.LINsafe(preFrame, 0.1);
		this.waitPushGesture();
		this.LINsafe(targetFrame, 0.1);
	}
	
	public void waitPushGestureY(Frame targetFrame) {
		Frame preFrame = targetFrame.copyWithRedundancy();
		preFrame.setY(preFrame.getY() + 50);
		this.LINsafe(preFrame, 0.1);
		this.waitPushGesture();
		this.LINsafe(targetFrame, 0.1);
	}
	
	public void checkPartZ(int probeDist, double relSpeed, double maxTorque) {
		Frame targetFrame = kiwa.getCommandedCartesianPosition(kiwa.getFlange());
		double prevMaxTorque = this._maxTorque;
		boolean pieceFound = false;
		if(log1) log.info("Checking component presence...");
		do {
			mf.setRGB("G");
			this.setJTConds(maxTorque);
			this._JTBMotion = kiwa.move(linRel(0, 0, probeDist).setJointVelocityRel(scaleSpeed(relSpeed)).breakWhen(_JTConds)); 
			this.setJTConds(prevMaxTorque);
			this._JTBreak = this._JTBMotion.getFiredBreakConditionInfo();
			if (_JTBreak != null) {
				if(log1) log.info("Component detected. " ); 
				kiwa.move(lin(targetFrame).setJointVelocityRel(scaleSpeed(relSpeed)));
				mf.blinkRGB("GB", 400);
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
				if(log1) log.info("Pin found. " ); 
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
				if(log1) log.info("Hole found. " ); 
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
		if(log1) padLog("Testing hole insertion...");
		mf.blinkRGB("GB", 250);
		double prevMaxTorque = this._maxTorque;
		this.setJTConds(maxTorque);
		mf.setRGB("G");
		Frame holeFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
		this._JTBMotion = kiwa.move(linRel(-tolerance, -tolerance, 0).setJointVelocityRel(scaleSpeed(relSpeed)).breakWhen(_JTConds));
		this._JTBreak = this._JTBMotion.getFiredBreakConditionInfo();
		if (this._JTBreak != null) { mf.blinkRGB("GB", 200); found = true; }
		else mf.blinkRGB("RB", 200);
		this.PTP(holeFrame, relSpeed*_speed[0]);
		if (found) {
			this._JTBMotion = kiwa.move(linRel(tolerance, tolerance, 0).setJointVelocityRel(scaleSpeed(relSpeed)).breakWhen(_JTConds));
			this._JTBreak = this._JTBMotion.getFiredBreakConditionInfo();
			if (this._JTBreak != null) mf.blinkRGB("GB", 200);
			else { mf.blinkRGB("RB", 200); found = false; }
			this.PTP(holeFrame, relSpeed*_speed[0]);
		}
		this.setJTConds(prevMaxTorque);
		return found;
	}
	
	public boolean twistJ7withJTCond(double angle, double extra, double relSpeed, double maxTorque) {
		if(log1) padLog("Twisting the pin...");
		mf.blinkRGB("GB", 250);
		JointTorqueCondition JTCond = new JointTorqueCondition(JointEnum.J7, -maxTorque, maxTorque);
		try {
			kiwa.move(linRel(Transformation.ofDeg(0,0,0,angle,0,0)).setJointVelocityRel(_speed));
			this._JTBMotion = kiwa.move(linRel(Transformation.ofDeg(0, 0, 0, extra, 0, 0)).setJointVelocityRel(scaleSpeed(relSpeed)).breakWhen(JTCond)); // Tx, Ty, Tz, Rz, Ry, Rx
			this._JTBreak = this._JTBMotion.getFiredBreakConditionInfo();
			if (_JTBreak != null) {
				if(log1) padLog("Rotation reached limit due to torque sensing");
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