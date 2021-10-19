package EluxAPI;

// Movement returns: 1 = success, 0 = collision, -1 = unreachable, -2 = nonexistent

import static EluxAPI.Utils.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.conditionModel.ICondition;
import com.kuka.roboticsAPI.conditionModel.JointTorqueCondition;
import com.kuka.roboticsAPI.deviceModel.JointEnum;
import com.kuka.roboticsAPI.executionModel.CommandInvalidException;
import com.kuka.roboticsAPI.executionModel.IFiredConditionInfo;
import com.kuka.roboticsAPI.geometricModel.*;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;

@Singleton
public class xAPI_Move extends RoboticsAPIApplication {
	@Override public void run() { while (true) { break; } }
	@Inject private LBR kiwa;
	@Inject private xAPI_MF mf;
	@Inject private xAPI_Pad pad;
		
	// Global variables
	private static final boolean logger = false;
	private String homeFramePath;
	private Tool tool;
	private ObjectFrame tcp;
	private double[] speed = {0,0,0,0,0,0,0};
	private double bRadius, bAngle;
	
	// Collision control
	boolean collisionDetection, release, releaseAuto, forceEnd;
	private double releaseDist;
	private double maxTorque;
	private ICondition JTConds;
	private IMotionContainer JTMotion;
	private IFiredConditionInfo JTBreak;
	
	// CONSTRUCTOR ----------------------------------------------------------------------
	
	@Inject	public xAPI_Move(xAPI_MF _mf, xAPI_Pad _pad) {
		this.mf = _mf;
		this.pad = _pad;
		
		this.setGlobalSpeed(0.25);
		bRadius = bAngle = 0;
		
		//collisionDetection = true;
		release = true;
		releaseAuto = false;
		forceEnd = false;
		releaseDist = 10;
		maxTorque = 5;
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
	
	public Frame toFrame(String framePath) { return getApplicationData()
											.getFrame(framePath).copyWithRedundancy(); }
	public Frame getFlangePos() { return kiwa.getCurrentCartesianPosition(kiwa.
											getFlange()).copyWithRedundancy(); }
	public Frame getFlangeTarget() { return kiwa.getCommandedCartesianPosition(kiwa.
											getFlange()).copyWithRedundancy(); }
	
		
	// SETTERS --------------------------------------------------------------------------
	
	public void setHome(String targetPath) {
		homeFramePath = targetPath;
		kiwa.setHomePosition(toFrame(targetPath));
	}
	public void setTool(Tool _tool) { tool = _tool; tool.attachTo(kiwa.getFlange()); }
	public void setTCP(String _tcp) {
		tcp = tool.getFrame(_tcp);
		if(logger) padLog("TCP set to " + tool.getName() + ", frame " + tcp.getName());
	}
	
	public void setGlobalSpeed(double speed) { this.setGlobalSpeed(speed, false); }
	public void setGlobalSpeed(double speed, boolean log) { 
		for (int i = 0; i < 7; i++) this.speed[i] = speed;
		if(log) padLog("Now speed is " + String.format("%,.0f", speed * 100) + "%");
	}
	public void setA7Speed(double speed) { this.speed[6] = speed; }
	
	public void setBlending(double radius, double angle) {
		this.bRadius = radius;
		this.bAngle = deg2rad(angle);
	}
	
	// COLLISION MANAGEMENT ----------------------------------------------------
	
	public void setRelease(boolean _release) { release = _release; }
	public void setReleaseAuto(boolean _releaseAuto) { releaseAuto = _releaseAuto; }
	//public void setCollisionDetection(boolean _cd) { collisionDetection = _cd; }
	public void setForceEnd(boolean _forceEnd) { forceEnd = _forceEnd; }
	
	public void setJTconds(double maxTorque){
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
		if(logger) padLog("Max Axis Torque set to " + maxTorque + " Nm.");
	}
	
	// MOVEMENT COMMANDS ---------------------------------------------------------------
	
	public boolean release() {
		double rd = releaseDist;
		if(releaseAuto) { LINREL(0, 0, -rd, 0.5, false); return false; }
		int ans = 1;
		while (ans != 0) {
			ans = pad.question("Indicate desired movement of " + rd + 
					"mm in TOOL directions",
					"SKIP MOVEMENT","+X","+Y","+Z","+DIST",
					"RETRY TARGET  ","-X","-Y","-Z", "-DIST");
			switch(ans) {
				case 0: return false;
				case 1: LINREL(rd,0,0,1,false); break;
				case 2: LINREL(0,rd,0,1,false); break;
				case 3: LINREL(0,0,rd,1,false); break;
				case 4: rd += 10; break;
				case 5: return true;
				case 6: LINREL(-rd,0,0,1,false); break;
				case 7: LINREL(0,-rd,0,1,false); break;
				case 8: LINREL(0,0,-rd,1,false); break;
				case 9: rd -=10; if(rd == -10) rd = 0; break;
			}
		}
		return false;
	}
	
	// #################################################################################
	// MOTION COMMANDS
	// #################################################################################

	// PTP -----------------------------------------------------------------------------
	public boolean PTPhome(double relSpeed, boolean approx) {
		return this.PTP(homeFramePath, relSpeed, approx) == 1 ? true : false; }
	
	public int PTP(Frame target, double relSpeed, boolean approx) {
		try { 
			mf.setRGB("G");
			if(approx) tcp.moveAsync(ptp(target)
					.setJointVelocityRel(scaleSpeed(relSpeed))
					.setBlendingCart(bRadius)
					.setBlendingOri(bAngle));
			else {
				JTMotion = tcp.move(ptp(target)
						.setJointVelocityRel(scaleSpeed(relSpeed))
						.breakWhen(JTConds));
					JTBreak = JTMotion.getFiredBreakConditionInfo();
					if(JTBreak != null) {
						mf.setRGB("RB");
						padLog("Collision detected!");
						if (release) release();
						else if (release()) PTP(target, relSpeed, approx);
						return 0;
					}
				}
			return 1;
		} catch(CommandInvalidException e) {
			padErr("Unable to perform movement");
			mf.setRGB("RG");
			return -1; 
		}
	}
	
	public int PTP(String targetPath, double relSpeed, boolean approx) {
		int success = 0;
		try { success = PTP(toFrame(targetPath), relSpeed, approx); }
		catch(Exception e) { padErr("Target does not exist."); success = -2; }
		return success;
	}
	
	// LIN -----------------------------------------------------------------------------
	public boolean LINhome(double relSpeed, boolean approx) {
		return this.LIN(homeFramePath, relSpeed, approx) == 1 ? true : false; }
	
	public int LIN(Frame target, double relSpeed, boolean approx) {
		try { 
			mf.setRGB("G");
			if(approx) tcp.moveAsync(ptp(target)
					.setJointVelocityRel(scaleSpeed(relSpeed))
					.setBlendingCart(bRadius)
					.setBlendingOri(bAngle));
			else {
				JTMotion = tcp.move(ptp(target)
						.setJointVelocityRel(scaleSpeed(relSpeed))
						.breakWhen(JTConds));
					JTBreak = JTMotion.getFiredBreakConditionInfo();
					if(JTBreak != null) {
						mf.setRGB("RB");
						padLog("Collision detected!");
						if (release) release();
						else if (release()) LIN(target, relSpeed, approx);
						return 0;
					}
				}
			return 1;
		} catch(CommandInvalidException e) {
			padErr("Unable to perform movement");
			mf.setRGB("RG");
			return -1; 
		}
	}
	
	public int LIN(String targetPath, double relSpeed, boolean approx) {
		int success = 0;
		try { success = LIN(toFrame(targetPath), relSpeed, approx); }
		catch(Exception e) { padErr("Target does not exist."); success = -2; }
		return success;
	}
	
	// LINREL --------------------------------------------------------------------------
	public int LINREL(double x, double y, double z, double Rz, double Ry, double Rx, 
						double relSpeed, boolean approx) {
		try { 
			mf.setRGB("G");
			if(approx) tcp.moveAsync(linRel(Transformation.ofDeg(x, y, z, Rz, Ry, Rx))
					.setJointVelocityRel(scaleSpeed(relSpeed))
					.setBlendingCart(bRadius)
					.setBlendingOri(bAngle));
			else {
				JTMotion = tcp.move(linRel(Transformation.ofDeg(x, y, z, Rz, Ry, Rx))
						.setJointVelocityRel(scaleSpeed(relSpeed))
						.breakWhen(JTConds));
					JTBreak = JTMotion.getFiredBreakConditionInfo();
					if(JTBreak != null) {
						mf.setRGB("RB");
						padLog("Collision detected!");
						if (release) release();
						else if (release()) LINREL(x, y, z, Rz, Ry, Rx, relSpeed, approx);
						return 0;
					}
				}
			return 1;
		} catch(CommandInvalidException e) {
			padErr("Unable to perform movement");
			mf.setRGB("RG");
			return -1; 
		}
	}
	
	public int LINREL(double x, double y, double z, double relSpeed, boolean approx) {
		return LINREL(x, y, z, 0, 0, 0, relSpeed, approx);
	}
	
	// CIRC ----------------------------------------------------------------------------
	public int CIRC(Frame target1, Frame target2, double relSpeed, boolean approx) {
		try { 
			mf.setRGB("G");
			if(approx) tcp.moveAsync(circ(target1, target2)
					.setJointVelocityRel(scaleSpeed(relSpeed))
					.setBlendingCart(bRadius)
					.setBlendingOri(bAngle));
			else {
				JTMotion = tcp.move(circ(target1, target2)
						.setJointVelocityRel(scaleSpeed(relSpeed))
						.breakWhen(JTConds));
					JTBreak = JTMotion.getFiredBreakConditionInfo();
					if(JTBreak != null) {
						mf.setRGB("RB");
						padLog("Collision detected!");
						if (release) release();
						else if (release()) CIRC(target1, target2, relSpeed, approx);
						return 0;
					}
				}
			return 1;
		} catch(CommandInvalidException e) {
			padErr("Unable to perform movement");
			mf.setRGB("RG");
			return -1; 
		}
	}
	
	public int CIRC(String target1, String target2, double relSpeed, boolean approx) {
		int success = 0;
		try { success = CIRC(toFrame(target1), toFrame(target2), relSpeed, approx); }
		catch(Exception e) { padErr("Target does not exist."); success = -2; }
		return success;
	}
	
	// GENERAL TOOLS -------------------------------------------------------------------
	public Frame randomizeFrame(String path, int range) {
		Frame target = toFrame(path);
		target.transform(Transformation.ofDeg(
				range * (Math.random() - 0.5),
				range * (Math.random() - 0.5),
				range * (Math.random() - 0.5),
				0, 0, 0));
		return target;
	}
	
	public boolean twistJ7withCheck(double minAngle, double maxAngle, 
									double relSpeed, double maxTorque) {
		boolean fullTwist;
		this.LINREL(0, 0, 0, minAngle, 0, 0, relSpeed, false);
		double prevMaxTorque = this.getMaxTorque();
		this.setJTconds(maxTorque);
		fullTwist = (this.LINREL(0, 0, 0, maxAngle-minAngle, 0, 0, 
									relSpeed, false) == 1 ? true : false);
		this.setJTconds(prevMaxTorque);
		if (fullTwist) mf.blinkRGB("GB", 500);
		return fullTwist;
	}
}