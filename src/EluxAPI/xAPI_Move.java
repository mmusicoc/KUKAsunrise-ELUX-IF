package EluxAPI;

/* Movement returns: 
		1 = success
		-1 = collision
		-10 = unreachable
		-100 = nonexistent
		*/

import static EluxUtils.Utils.*;
import static EluxUtils.UMath.*;
//import EluxLogger.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.conditionModel.ICondition;
import com.kuka.roboticsAPI.conditionModel.JointTorqueCondition;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.JointEnum;
import com.kuka.roboticsAPI.executionModel.CommandInvalidException;
import com.kuka.roboticsAPI.executionModel.IFiredConditionInfo;
import com.kuka.roboticsAPI.geometricModel.*;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;

@Singleton
public class xAPI_Move extends RoboticsAPIApplication {
	@Override public void run() { while (true) { break; } }
	@Inject private LBR kiwa;
	@Inject private Controller kiwaController;
	@Inject private xAPI_MF mf;
	@Inject private xAPI_Pad pad;
		
	// Global variables
	private boolean logger;
	private String homeFramePath;
	private Tool tool;
	private ObjectFrame tcp;
	private double[] speed = {0,0,0,0,0,0,0};
	private double[] accel = {0,0,0,0,0,0,0};
	private double bRadius, bAngle;
	
	// Collision control
	private double releaseDist;
	private double releaseSpeed;
	private double maxTorque;
	private int releaseMode;
	private ICondition JTConds;
	private IMotionContainer JTMotion;
	private IFiredConditionInfo JTBreak;
	
	// CONSTRUCTOR ----------------------------------------------------------------------
	
	@Inject	public xAPI_Move(xAPI_MF _mf, xAPI_Pad _pad) {
		this.mf = _mf;
		this.pad = _pad;
		
		logger = false;
		
		this.setGlobalSpeed(0.25);
		this.setGlobalAccel(0.25);
		bRadius = bAngle = 0;
		
		this.setMaxTorque(5);
		this.setReleaseMode(0);
		releaseDist = 10;
		releaseSpeed = 0.2;
	}
	
	/**
	 * @param home String
	 * @param tool Tool
	 * @param tcp String
	 * @param globalSpeed float 0<x<=1
	 * @param globalAccel float 0<x<=1
	 * @param blendingRadius float > 0 (mm)
	 * @param blendingAngle float > 0 (degrees)
	 * @param maxTorque float > 0 (Nm)
	 * @param releaseMode int 0: skip, 1: manual, 2: auto + skip, 3: auto + retry
	 * @param log boolean
	 */
	
	public void init(String home,
						Tool tool, String tcp,
						double globalSpeed, double globalAccel,
						double blendingRadius, double blendingAngle,
						double maxTorque, int releaseMode,
						boolean log) {
		mf.setRGB("OFF");
		this.setLogger(log);
		this.setHome(home);
		this.setTool(tool);
		this.setTCP(tcp);
		this.setGlobalSpeed(globalSpeed, true);
		this.setGlobalAccel(globalAccel, false);
		this.setBlending(blendingRadius, blendingAngle);
		this.setMaxTorque(maxTorque);
		this.setReleaseMode(releaseMode);	
	}

	// GETTERS --------------------------------------------------------------------------		
	public ObjectFrame getTCP() { return this.tcp; }
	public double getMaxTorque() { return maxTorque; }
	public ICondition getJTConds() { return this.JTConds; }
	public double getGlobalSpeed() { return this.speed[0]; }
	public double[] scaleSpeed(double relSpeed) {
		double[] scaledSpeed = {1, 1, 1, 1, 1, 1, 1};
		for (int i = 0; i < 7; i++) scaledSpeed[i] = speed[i] * relSpeed;
		return scaledSpeed;
	}
	public double[] scaleAccel(double relAccel) {
		double[] scaledAccel = {1, 1, 1, 1, 1, 1, 1};
		for (int i = 0; i < 7; i++) scaledAccel[i] = accel[i] * relAccel;
		return scaledAccel;
	}
	
	public Frame p2f(String framePath) { return p2f(framePath, true); }
	public Frame p2f(String framePath, boolean errNotice) { 
		ObjectFrame OF = p2of(framePath, errNotice);
		if(OF == null) return null;
		return OF.copyWithRedundancy();
		//try { return getApplicationData().getFrame(framePath).copyWithRedundancy(); }
		//catch(Exception e) { if(errMgm) nonexistent(framePath); return null; }
		// Could be implemented differently with tryGetFrame
	}
	
	public ObjectFrame p2of(String framePath) { return p2of(framePath, true); }
	public ObjectFrame p2of(String framePath, boolean errNotice) { 
		try { return getApplicationData().getFrame(framePath); }
		catch(Exception e) { 
			if(errNotice) {
				nonexistent(framePath);
				if(logger) {
					System.err.println(e);
					e.printStackTrace();
				}
			}
			return null;
		}
	}
	
	public boolean framePathExists(String framePath) { return (p2of(framePath, false) != null); }
	
	public Frame getFlangePos() { return kiwa.getCurrentCartesianPosition(kiwa.
											getFlange()).copyWithRedundancy(); }
	public Frame getFlangeTarget() { return kiwa.getCommandedCartesianPosition(kiwa.
											getFlange()).copyWithRedundancy(); }
	public Frame getTCPpos() {
		Frame tcpPos = new Frame();
		Frame flangePos = getFlangePos();
		tcpPos.setParent(flangePos, false);
		tcpPos.setTransformationFromParent(Transformation.ofRad(
				tcp.getX(), tcp.getY(), tcp.getZ(),
				tcp.getAlphaRad(), tcp.getBetaRad(), tcp.getGammaRad()));
		tcpPos.setTransformationFromParent(tcpPos.transformationFromWorld());
		return tcpPos;
	}
		
	// SETTERS --------------------------------------------------------------------------
	
	public void setHome(String targetPath) {
		kiwaController = (Controller) getContext().getControllers().toArray()[0];
		kiwa = (LBR) kiwaController.getDevices().toArray()[0];
		homeFramePath = targetPath;
		kiwa.setHomePosition(p2f(targetPath));
	}
	public void setTool(Tool _tool) { tool = _tool; tool.attachTo(kiwa.getFlange()); }
	public void setTCP(String _tcp) {
		tcp = tool.getFrame(_tcp);
		if(logger) logmsg("TCP set to " + tool.getName() + ", frame " + tcp.getName());
	}
	
	public void setGlobalSpeed(double speed) { this.setGlobalSpeed(speed, false); }
	public void setGlobalSpeed(double speed, boolean log) { 
		for (int i = 0; i < 7; i++) this.speed[i] = speed;
		if(log) logmsg("Now speed is " + String.format("%,.0f", speed * 100) + "%");
	}
	public void setA7Speed(double speed) { this.speed[6] = speed; }
	
	public void setGlobalAccel(double accel) { this.setGlobalAccel(accel, false); }
	public void setGlobalAccel(double accel, boolean log) { 
		for (int i = 0; i < 7; i++) this.accel[i] = accel;
		if(log) logmsg("Now accel is " + String.format("%,.0f", accel * 100) + "%");
	}
	public void setA7Accel(double accel) { this.accel[6] = accel; }
	
	public void setBlending(double radius, double angle) {
		this.bRadius = radius;
		this.bAngle = d2r(angle);
	}
	
	public void setLogger(boolean log) { logger = log; }
	
	// ERROR/COLLISION MANAGEMENT -----------------------------------------------
	
	public void setReleaseMode(int releaseMode) { this.releaseMode = releaseMode; }
	
	public void setMaxTorque(double maxTorque){
		this.maxTorque = maxTorque;
		JointTorqueCondition JTCond[] = new JointTorqueCondition[8];
		JTCond[1] = new JointTorqueCondition(JointEnum.J1, -maxTorque, maxTorque);	
		JTCond[2] = new JointTorqueCondition(JointEnum.J2, -maxTorque, maxTorque);
		JTCond[3] = new JointTorqueCondition(JointEnum.J3, -maxTorque, maxTorque);	
		JTCond[4] = new JointTorqueCondition(JointEnum.J4, -maxTorque, maxTorque);
		JTCond[5] = new JointTorqueCondition(JointEnum.J5, -maxTorque, maxTorque);	
		JTCond[6] = new JointTorqueCondition(JointEnum.J6, -maxTorque, maxTorque);
		JTCond[7] = new JointTorqueCondition(JointEnum.J7, -maxTorque, maxTorque);
		this.JTConds = JTCond[1].or(JTCond[2]).or(JTCond[3])
				.or(JTCond[4]).or(JTCond[5]).or(JTCond[6]).or(JTCond[7]);
		if(logger) logmsg("Max Axis Torque set to " + maxTorque + " Nm.");
	}
	
	/**
	 * @param releaseMode int 0: skip, 1: manual, 2: auto + skip, 3: auto + retry
	 */
	public boolean release() {
		double rd = releaseDist;
		double rs = releaseSpeed;
		switch (releaseMode) {
			case 0: return false;
			case 1:
				int ans = 1;
				while (ans != 0) {
					ans = pad.question("Indicate desired movement of " + rd + 
							"mm in TOOL directions",
							"SKIP MOVEMENT","+X","+Y","+Z","+DIST",
							"RETRY TARGET  ","-X","-Y","-Z", "-DIST");
					switch(ans) {
						case 0: return false;
						case 1: LINREL(rd,0,0,rs,false); break;
						case 2: LINREL(0,rd,0,rs,false); break;
						case 3: LINREL(0,0,rd,rs,false); break;
						case 4: rd += 10; break;
						case 5: return true;
						case 6: LINREL(-rd,0,0,rs,false); break;
						case 7: LINREL(0,-rd,0,rs,false); break;
						case 8: LINREL(0,0,-rd,rs,false); break;
						case 9: rd -=10; if(rd < 10) rd = 10; break;
					}
				}
			case 2:	LINREL(0, 0, -rd, rs, true); return false;
			case 3:	LINREL(0, 0, -rd, rs, true); return true;
			default: return false;
		}
	}
	
	public int collision() {
		if(logger) logmsg("Collision detected at\n" + getFlangePos().toStringInWorld());
		mf.blinkRGB("RB", 500);
		return -1;
	}
	
	public int unreachable() {
		logErr("Unable to perform movement to\n" + getFlangeTarget().toStringInWorld());
		mf.blinkRGB("RG", 500);
		return -10;
	}
	
	public int nonexistent(String targetPath) {
		logErr("Target \"" + targetPath + "\" does not exist.");
		mf.blinkRGB("R", 500);
		return -100;
	}
	
	public void setMoveColorMF() {
		mf.setRGB("OFF");
	}
	
	// #################################################################################
	// MOTION COMMANDS
	// #################################################################################

	// PTP -----------------------------------------------------------------------------
	public boolean PTPhome(double relSpeed, boolean approx) {
		return this.PTP(homeFramePath, relSpeed, approx) == 1 ? true : false; }
	
	public int PTP(Frame target, double relSpeed, boolean approx) {
		try {
			int success = 1;
			setMoveColorMF();
			if(approx) tcp.moveAsync(ptp(target)
					.setJointVelocityRel(scaleSpeed(relSpeed))
					.setJointAccelerationRel(accel)
					.setBlendingCart(bRadius)
					.setBlendingOri(bAngle));
			else {
				JTMotion = tcp.move(ptp(target)
					.setJointVelocityRel(scaleSpeed(relSpeed))
					.setJointAccelerationRel(accel)
					.breakWhen(JTConds));
				JTBreak = JTMotion.getFiredBreakConditionInfo();
				if(JTBreak != null) {
					success = collision();
					if (release()) return PTP(target, relSpeed, approx);
				}
			}
			return success;
		} catch(CommandInvalidException e) { return unreachable(); }
	}
	
	public int PTP(String targetPath, double relSpeed, boolean approx) {
		try {
			if(logger) logmsg("Moving to " + targetPath);
			return PTP(p2f(targetPath), relSpeed, approx); }
		catch(Exception e) { return nonexistent(targetPath); }
	}
	
	public int PTP(double x, double y, double z,
				   double a, double b, double c,
				   double relSpeed, boolean approx) {
		Frame target = new Frame(x,y,z,d2r(a),d2r(b),d2r(c));
		return PTP(target, relSpeed, approx);
	}
	
	public int PTP(double[] a, double relSpeed, boolean approx) {
		try { 
			int success = 1;
			setMoveColorMF();
			if(approx) tcp.moveAsync(ptp(d2r(a[0]),d2r(a[1]),d2r(a[2]),d2r(a[3]),
										 d2r(a[4]),d2r(a[5]),d2r(a[6]))
					.setJointVelocityRel(scaleSpeed(relSpeed))
					.setJointAccelerationRel(accel)
					.setBlendingCart(bRadius)
					.setBlendingOri(bAngle));
			else {
				JTMotion = tcp.move(ptp(d2r(a[0]),d2r(a[1]),d2r(a[2]),d2r(a[3]),
						 				d2r(a[4]),d2r(a[5]),d2r(a[6]))
					.setJointVelocityRel(scaleSpeed(relSpeed))
					.setJointAccelerationRel(accel)
					.breakWhen(JTConds));
				JTBreak = JTMotion.getFiredBreakConditionInfo();
				if(JTBreak != null) {
					success = collision();
					if (release()) return PTP(a, relSpeed, approx);
				}
			}
			return success;
		} catch(CommandInvalidException e) { return unreachable(); }
	}
	
	// LIN -----------------------------------------------------------------------------
	public boolean LINhome(double relSpeed, boolean approx) {
		return this.LIN(homeFramePath, relSpeed, approx) == 1 ? true : false; }
	
	public int LIN(Frame target, double relSpeed, boolean approx) {
		try {
			int success = 1;
			setMoveColorMF();
			if(approx) tcp.moveAsync(ptp(target)
					.setJointVelocityRel(scaleSpeed(relSpeed))
					.setJointAccelerationRel(accel)
					.setBlendingCart(bRadius)
					.setBlendingOri(bAngle));
			else {
				JTMotion = tcp.move(ptp(target)
					.setJointVelocityRel(scaleSpeed(relSpeed))
					.setJointAccelerationRel(accel)
					.breakWhen(JTConds));
				JTBreak = JTMotion.getFiredBreakConditionInfo();
				if(JTBreak != null) {
					success = collision();
					if (release()) return LIN(target, relSpeed, approx);
				}
			}
			return success;
		} catch(CommandInvalidException e) { return unreachable(); }
	}
	
	public int LIN(String targetPath, double relSpeed, boolean approx) {
		try { return LIN(p2f(targetPath), relSpeed, approx); }
		catch(Exception e) { return nonexistent(targetPath); }
	}
	
	public int LIN(double x, double y, double z,
			   double a, double b, double c,
			   double relSpeed, boolean approx) {
	Frame target = new Frame(x,y,z,d2r(a),d2r(b),d2r(c));
	return LIN(target, relSpeed, approx);
}
	
	// LINREL --------------------------------------------------------------------------
	public int LINREL(double x, double y, double z, double Rz, double Ry, double Rx, 
						double relSpeed, boolean approx) {
		try {
			int success = 1;
			setMoveColorMF();
			if(approx) tcp.moveAsync(linRel(Transformation.ofDeg(x, y, z, Rz, Ry, Rx))
					.setJointVelocityRel(scaleSpeed(relSpeed))
					.setJointAccelerationRel(accel)
					.setBlendingCart(bRadius)
					.setBlendingOri(bAngle));
			else {
				JTMotion = tcp.move(linRel(Transformation.ofDeg(x, y, z, Rz, Ry, Rx))
					.setJointVelocityRel(scaleSpeed(relSpeed))
					.setJointAccelerationRel(accel)
					.breakWhen(JTConds));
				JTBreak = JTMotion.getFiredBreakConditionInfo();
				if(JTBreak != null) {
					success = collision();
					if (release()) return LINREL(x, y, z, Rz, Ry, Rx, relSpeed, approx);
				}
			}
			return success;
		} catch(CommandInvalidException e) { return unreachable(); }
	}
	
	public int LINREL(double x, double y, double z, double relSpeed, boolean approx) {
		return LINREL(x, y, z, 0, 0, 0, relSpeed, approx);
	}
	
	// CIRC ----------------------------------------------------------------------------
	public int CIRC(Frame target1, Frame target2, double relSpeed, boolean approx) {
		try { 
			int success = 1;
			setMoveColorMF();
			if(approx) tcp.moveAsync(circ(target1, target2)
					.setJointVelocityRel(scaleSpeed(relSpeed))
					.setJointAccelerationRel(accel)
					.setBlendingCart(bRadius)
					.setBlendingOri(bAngle));
			else {
				JTMotion = tcp.move(circ(target1, target2)
					.setJointVelocityRel(scaleSpeed(relSpeed))
					.setJointAccelerationRel(accel)
					.breakWhen(JTConds));
				JTBreak = JTMotion.getFiredBreakConditionInfo();
				if(JTBreak != null) {
					success = collision();
					if (release()) return CIRC(target1, target2, relSpeed, approx);
				}
			}
			return success;
		} catch(CommandInvalidException e) { return unreachable(); }
	}
	
	public int CIRC(String target1, String target2, double relSpeed, boolean approx) {
		try { return CIRC(p2f(target1), p2f(target2), relSpeed, approx); }
		catch(Exception e) { return nonexistent(target1 + " or " + target2); }
	}
	
	// GENERAL TOOLS -------------------------------------------------------------------
	
	public boolean twistJ7withCheck(double minAngle, double maxAngle, 
									double relSpeed, double maxTorque) {
		boolean fullTwist;
		this.LINREL(0, 0, 0, minAngle, 0, 0, relSpeed, false);
		double prevMaxTorque = this.getMaxTorque();
		this.setMaxTorque(maxTorque);
		fullTwist = (this.LINREL(0, 0, 0, maxAngle-minAngle, 0, 0, 
									relSpeed, false) == 1 ? true : false);
		this.setMaxTorque(prevMaxTorque);
		if (fullTwist) mf.blinkRGB("GB", 500);
		return fullTwist;
	}
}