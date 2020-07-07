package EluxAPI;

/*******************************************************************
* <b> STANDARD API CLASS BY mario.musico@electrolux.com </b> <p>
*/

import static EluxAPI.Utils.*;

import javax.inject.Inject;
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

@Singleton
public class API_Movements extends RoboticsAPIApplication {
	// Standard KUKA API objects
	@Override public void run() { while (true) { break; } }		// Compulsory method for RoboticsAPIApplication derived classes
	@Inject private LBR kiwa;
	@Inject private API_MF mf;
	@Inject private API_Pad pad = new API_Pad(mf);
	private Tool _tool;
	private ObjectFrame tcp;
	
	// Private properties
	private static final boolean log1 = false;
	private ICondition _JTConds;
	private IMotionContainer _JTMotion;
	private IFiredConditionInfo _JTBreak;
	private CartesianImpedanceControlMode _softMode, _stiffMode;
	private PositionHold _posHold;
	private String _homeFramePath;
	private double[] _speed = {0,0,0,0,0,0,0};
	private double _maxTorque;
	private double _maxTorquePrev;
	private boolean _lockDir;
	private double _blendingRadius;
	private double _blendingRadiusPrev;
	private double _blendingAngle;
	private double _blendingAnglePrev;
	
	// CONSTRUCTOR
	@Inject	public API_Movements(API_MF _mf) {
		this.mf = _mf;
		_softMode = new CartesianImpedanceControlMode();
		_stiffMode = new CartesianImpedanceControlMode();
		_stiffMode.parametrize(CartDOF.TRANSL).setStiffness(5000).setDamping(1);
		_stiffMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(1);
		this.setGlobalSpeed(0.25);
		_maxTorque = 5;
		_lockDir = false;
		_blendingRadius = 0;
		_blendingAngle = 0;
	}

	// GETTERS
		
	public ObjectFrame getTCP() { return this.tcp; }
	public PositionHold getPosHold() { return _posHold; }
	public double getMaxTorque() { return _maxTorque; }
	public ICondition getJTConds() { return this._JTConds; }
	public Frame getFlangePos() { return kiwa.getCommandedCartesianPosition(kiwa.getFlange()); }
	public double[] scaleSpeed(double relSpeed) {
		double[] scaledSpeed = {1, 1, 1, 1, 1, 1, 1};
		for (int i = 0; i < 7; i++) scaledSpeed[i] = _speed[i] * relSpeed;
		return scaledSpeed;
	}
		
	// SETTERS
	
	public void setGlobalSpeed(double speed) { 
		for (int i = 0; i < 7; i++) this._speed[i] = speed;
	}
	public void setA7Speed(double speed) { this._speed[6] = speed; }
	public void setHome(String targetFramePath) {
		_homeFramePath = targetFramePath;
		kiwa.setHomePosition(getApplicationData().getFrame(targetFramePath));
	}
	
	public void setTool(Tool tool) {
		_tool = tool;
		tool.attachTo(kiwa.getFlange());
	}
	
	public void setTCP(String _tcp) {
		tcp = _tool.getFrame(_tcp);
		// padLog("TCP set to " + _tool.getName() + ", frame " + tcp.getName());
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
	
	public void setJTconds(double maxTorque){
		this._maxTorquePrev = this._maxTorque;
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
		if(log1) padLog("Max Axis Torque set to " + maxTorque + " Nm.");
	}
	
	public void resetJTconds() { this.setJTconds(this._maxTorquePrev); }
	
	public void setBlending(double radius, double angle) {
		this._blendingRadiusPrev = this._blendingRadius;
		this._blendingRadius = radius;
		this._blendingAnglePrev = this._blendingAngle;
		this._blendingAngle = deg2rad(angle);
	}
	
	public void resetBlending() {
		this._blendingRadius = this._blendingRadiusPrev;
		this._blendingAngle = this._blendingAnglePrev;		
	}
	
	// STANDARD MOVES, SIMPLE CALLS ***************************************************************
	
	public boolean PTP(Frame targetFrame, double relSpeed, boolean approx) {
		try { 
			mf.setRGB("G");
			if (approx) tcp.moveAsync(ptp(targetFrame).setJointVelocityRel(scaleSpeed(relSpeed)).setBlendingCart(_blendingRadius).setBlendingOri(_blendingAngle)); 
			else tcp.move(ptp(targetFrame).setJointVelocityRel(scaleSpeed(relSpeed)).setBlendingCart(0).setBlendingOri(0)); 
			return true; 
		} catch(CommandInvalidException e) {
			padErr("Unable to perform movement");
			mf.setRGB("RG");
			return false; 
		}
	}
	public boolean PTP(String targetFramePath, double relSpeed, boolean approx) {
		ObjectFrame targetFrame = getApplicationData().getFrame(targetFramePath);
		return this.PTP(targetFrame.copyWithRedundancy(), relSpeed, approx);
	}
	
	public boolean PTPhome(double relSpeed, boolean approx) {
		return this.PTP(_homeFramePath, relSpeed, approx);
	}
	
	public boolean LIN(Frame targetFrame, double relSpeed, boolean approx) {
		try {
			mf.setRGB("G");
			if (approx) tcp.moveAsync(lin(targetFrame).setJointVelocityRel(scaleSpeed(relSpeed)).setBlendingCart(_blendingRadius).setBlendingOri(_blendingAngle));
			else tcp.move(lin(targetFrame).setJointVelocityRel(scaleSpeed(relSpeed)).setBlendingCart(0).setBlendingOri(0));
			return true; 
		} catch(CommandInvalidException e) {
			padErr("Unable to perform movement");
			mf.setRGB("RG");
			return false;
		}
	}
	public boolean LIN(String targetFramePath, double relSpeed, boolean approx) {
		ObjectFrame targetFrame = getApplicationData().getFrame(targetFramePath);
		return this.LIN(targetFrame.copyWithRedundancy(), relSpeed, approx);
	}
	public boolean LINhome(double relSpeed) {
		return this.LIN(_homeFramePath, relSpeed, false);
	}
	
	public boolean LINREL(double x, double y, double z, double Rz, double Ry, double Rx, boolean absolute, double relSpeed) {
		try {
			mf.setRGB("G");
			if (absolute) kiwa.move(linRel(Transformation.ofDeg(x, y, z, Rz, Ry, Rx)).setJointVelocityRel(scaleSpeed(relSpeed))); 
			else tcp.move(linRel(Transformation.ofDeg(x, y, z, Rz, Ry, Rx)).setJointVelocityRel(scaleSpeed(relSpeed)));
			return true; 
		} catch(CommandInvalidException e) { 
			padErr("Unable to perform movement");
			mf.setRGB("RG");
			return false;
		}
	}
	
	public boolean LINREL(double x, double y, double z, boolean absolute, double relSpeed) {
		return LINREL(x, y, z, 0, 0, 0, absolute, relSpeed);
	}
	
	public boolean CIRC(Frame targetFrame1, Frame targetFrame2, double relSpeed) {
		try {
			mf.setRGB("G");
			tcp.move(circ(targetFrame1, targetFrame2).setJointVelocityRel(scaleSpeed(relSpeed)));
			return true;
		} catch(CommandInvalidException e) {
			padErr("Unable to perform movement");
			mf.setRGB("RG");
			return false;
		}
	}	
	public boolean CIRC(String targetFramePath1, String targetFramePath2, double relSpeed) {
		ObjectFrame targetFrame1 = getApplicationData().getFrame(targetFramePath1);
		ObjectFrame targetFrame2 = getApplicationData().getFrame(targetFramePath2);
		return this.CIRC(targetFrame1.copyWithRedundancy(), targetFrame2.copyWithRedundancy(), relSpeed);
	}
	
	// Torque sensing enabled movements **************************************************************
	
	public void PTPhomeCobot() {
		this.LINREL(0, 0, -0.01, true, 0.5);
		pad.info("Move away from the robot. It will move automatically to home.");
		this.LINREL(0, 0, -50, true, 0.5);
		do {} while (!this.PTPsafe(_homeFramePath, _speed[0]));
	}
	
	public boolean PTPsafe(Frame targetFrame, double relSpeed){		// overloading for taught points
		try {
			mf.setRGB("G");
			this._JTMotion = tcp.move(ptp(targetFrame).setJointVelocityRel(scaleSpeed(relSpeed)).breakWhen(this._JTConds)); 
			this._JTBreak = this._JTMotion.getFiredBreakConditionInfo();
			if (_JTBreak != null) {
				mf.setRGB("RB");
				padLog("Collision detected!");
				return false;
			} else return true;
		} catch(CommandInvalidException e) {
			padErr("Unable to perform movement");
			mf.setRGB("RG");
			return false;
		}
	}
	public boolean PTPsafe(String targetFramePath, double relSpeed){
		ObjectFrame targetFrame = getApplicationData().getFrame(targetFramePath);
		return this.PTPsafe(targetFrame.copyWithRedundancy(), relSpeed);
	}
	
	public boolean LINsafe(Frame targetFrame, double relSpeed){		// overloading for taught points
		try {
			mf.setRGB("G");
			this._JTMotion = tcp.move(lin(targetFrame).setJointVelocityRel(scaleSpeed(relSpeed)).breakWhen(this._JTConds)); 
			this._JTBreak = this._JTMotion.getFiredBreakConditionInfo();
			if (_JTBreak != null) {
				mf.setRGB("RB");
				padLog("Collision detected!");
				return false;
			} else return true;
		} catch(CommandInvalidException e) {
			padErr("Unable to perform movement");
			mf.setRGB("RG");
			return false;
		}
	}
	public boolean LINsafe(String targetFramePath, double relSpeed){
		ObjectFrame targetFrame = getApplicationData().getFrame(targetFramePath);
		return this.LINsafe(targetFrame.copyWithRedundancy(), relSpeed);
	}
	
	public boolean LINRELsafe(double x, double y, double z, double Rz, double Ry, double Rx, boolean absolute, double relSpeed) {
		try {
			mf.setRGB("G");
			if (absolute) this._JTMotion = kiwa.move(linRel(Transformation.ofDeg(x, y, z, Rz, Ry, Rx)).setJointVelocityRel(scaleSpeed(relSpeed))); 
			else this._JTMotion = tcp.move(linRel(Transformation.ofDeg(x, y, z, Rz, Ry, Rx)).setJointVelocityRel(scaleSpeed(relSpeed)));
			this._JTBreak= this._JTMotion.getFiredBreakConditionInfo();
			if (_JTBreak != null) {
				mf.setRGB("RB");
				padLog("Collision detected!");
				return false;
			}
			else return true; 
		} catch(CommandInvalidException e) { 
			padErr("Unable to perform movement");
			mf.setRGB("RG");
			return false;
		}
	}
	
	public boolean LINRELsafe(double x, double y, double z, boolean absolute, double relSpeed) {
		return LINRELsafe(x, y, z, 0, 0, 0, absolute, relSpeed);
	}
	
	public boolean CIRCsafe(Frame targetFrame1, Frame targetFrame2, double relSpeed){		// overloading for taught points
		try {
			mf.setRGB("G");
			this._JTMotion = tcp.move(circ(targetFrame1, targetFrame2).setJointVelocityRel(scaleSpeed(relSpeed)).breakWhen(this._JTConds)); 
			this._JTBreak = this._JTMotion.getFiredBreakConditionInfo();
			if (_JTBreak != null) {
				mf.setRGB("RB");
				padLog("Collision detected!");
				return false;
			} else return true;
		} catch(CommandInvalidException e) {
			padErr("Unable to perform movement");
			mf.blinkRGB("RG", 500);
			return false;
		}
	}
	
	public boolean CIRCsafe(String targetFramePath1, String targetFramePath2, double relSpeed){
		ObjectFrame targetFrame1 = getApplicationData().getFrame(targetFramePath1);
		ObjectFrame targetFrame2 = getApplicationData().getFrame(targetFramePath2);
		return this.CIRCsafe(targetFrame1.copyWithRedundancy(), targetFrame2.copyWithRedundancy(), relSpeed);
	}
	
	public void waitPushGesture() {
		mf.saveRGB();
		mf.setRGB("B");
		kiwa.move(positionHold(_stiffMode, -1, null).breakWhen(_JTConds));
		waitMillis(500);
		mf.resetRGB();
	}
	
	public boolean twistJ7safe(double minAngle, double maxAngle, double relSpeed, double maxTorque) {
		boolean fullTwist;
		this.LINREL(0, 0, 0, minAngle, 0, 0, false, relSpeed);
		this.setJTconds(maxTorque);
		fullTwist = this.LINRELsafe(0, 0, 0, maxAngle-minAngle, 0, 0, false, relSpeed);
		this.resetJTconds();
		if (fullTwist) mf.blinkRGB("GB", 500);
		return fullTwist;
	}
}