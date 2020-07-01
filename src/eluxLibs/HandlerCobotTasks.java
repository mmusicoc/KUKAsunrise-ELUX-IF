package eluxLibs;

/*******************************************************************
* <b> STANDARD HANDLER CLASS BY mario.musico@electrolux.com </b> <p>
*/

//import static eluxLibs.Utils.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.*;


@Singleton
public class HandlerCobotTasks extends RoboticsAPIApplication {
	// Standard KUKA API objects
	@Override public void run() { while (true) { break; } }		// Compulsory method for RoboticsAPIApplication derived classes
	@Inject private HandlerMFio mf;
	@Inject private HandlerMov move;
	
	// Private properties
	private double _releaseDist;
	
	// CONSTRUCTOR
	@Inject	public HandlerCobotTasks(HandlerMFio _mf, HandlerMov _move) {
		this.mf = _mf;
		this.move = _move;
		_releaseDist = 10;
	}
	
	// Cobot macros **************************************************************
	
	public boolean PTPcobot(Frame targetFrame, double relSpeed, boolean forceEnd){		// overloading for taught points
		if (!move.PTPsafe(targetFrame, relSpeed)) {
			move.LINREL(0, 0, -_releaseDist, 1, true);
			mf.waitUserButton();
			relSpeed *= 0.8;
			if (forceEnd) this.PTPcobot(targetFrame, relSpeed, forceEnd);
			return false;
		}
		return true;
	}
	public boolean PTPcobot(String targetFramePath, double relSpeed, boolean forceEnd){
		ObjectFrame targetFrame = getApplicationData().getFrame(targetFramePath);
		return this.PTPcobot(targetFrame.copyWithRedundancy(), relSpeed, forceEnd);
	}
	
	public boolean LINcobot(Frame targetFrame, double relSpeed, boolean forceEnd){		// overloading for taught points
		if (!move.LINsafe(targetFrame, relSpeed)) {
			move.LINREL(0, 0, -_releaseDist, 1, true);
			mf.waitUserButton();
			relSpeed *= 0.8;
			if (forceEnd) this.LINcobot(targetFrame, relSpeed, forceEnd);
			return false;
		}
		return true;
	}
	
	public boolean LINcobot(String targetFramePath, double relSpeed, boolean forceEnd){
		ObjectFrame targetFrame = getApplicationData().getFrame(targetFramePath);
		return this.LINcobot(targetFrame.copyWithRedundancy(), relSpeed, forceEnd);
	}
	
	public boolean CIRCcobot(Frame targetFrame1, Frame targetFrame2, double relSpeed, boolean forceEnd){		// overloading for taught points
		if (!move.CIRCsafe(targetFrame1, targetFrame2, relSpeed)) {
			move.LINREL(0, 0, -_releaseDist, 1, true);
			mf.waitUserButton();
			relSpeed *= 0.8;
			if (forceEnd) LINcobot(targetFrame2, relSpeed, forceEnd);			// SECURITY MEASURE, NEW CIRC CAN OVERSHOOT!!
			return false;
		}
		return true;
	}
	boolean CIRCcobot(String targetFramePath1, String targetFramePath2, double relSpeed, boolean forceEnd){
		ObjectFrame targetFrame1 = getApplicationData().getFrame(targetFramePath1);
		ObjectFrame targetFrame2 = getApplicationData().getFrame(targetFramePath2);
		return this.CIRCcobot(targetFrame1.copyWithRedundancy(), targetFrame2.copyWithRedundancy(), relSpeed, forceEnd);
	}
	
	public void waitPushGestureX(Frame targetFrame) {
		Frame preFrame = targetFrame.copyWithRedundancy();
		preFrame.setX(preFrame.getX() + 50);
		this.LINcobot(preFrame, 0.1, true);
		move.waitPushGesture();
		this.LINcobot(targetFrame, 0.1, true);
	}
	
	public void waitPushGestureY(Frame targetFrame) {
		Frame preFrame = targetFrame.copyWithRedundancy();
		preFrame.setY(preFrame.getY() + 50);
		this.LINcobot(preFrame, 0.1, true);
		move.waitPushGesture();
		this.LINcobot(targetFrame, 0.1, true);
	}
	
	public void waitPushGestureZ(Frame targetFrame) {
		Frame preFrame = targetFrame.copyWithRedundancy();
		preFrame.setZ(preFrame.getZ() + 50);
		this.LINcobot(preFrame, 0.1, true);
		move.waitPushGesture();
		this.LINcobot(targetFrame, 0.1, true);
	}
	/*
	public void checkPartZ(int probeDist, double relSpeed, double maxTorque) {
		Frame targetFrame = kiwa.getCommandedCartesianPosition(kiwa.getFlange());
		double prevMaxTorque = move._maxTorque;
		boolean pieceFound = false;
		if(log1) padLog("Checking component presence...");
		do {
			mf.setRGB("G");
			this.setJTConds(maxTorque);
			this._JTBMotion = kiwa.move(linRel(0, 0, probeDist).setJointVelocityRel(scaleSpeed(relSpeed)).breakWhen(_JTConds)); 
			this.setJTConds(prevMaxTorque);
			this._JTBreak = this._JTBMotion.getFiredBreakConditionInfo();
			if (_JTBreak != null) {
				if(log1) padLog("Component detected. " ); 
				kiwa.move(lin(targetFrame).setJointVelocityRel(scaleSpeed(relSpeed)));
				mf.blinkRGB("GB", 400);
				pieceFound = true;
			} else {
				mf.setRGB("RB");
				padLog("No components detected, reposition the workpiece correctly and push the cobot (gesture control)." );
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
				if(log1) padLog("Pin found. " ); 
				mf.blinkRGB("GB", 800);
				pinFound = true;
			} else {
				mf.setRGB("RB");
				padLog("No pin found, insert one correctly and push the cobot (gesture control)." );
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
				if(log1) padLog("Hole found. " ); 
				mf.blinkRGB("GB", 800);
				holeFound = true;
			} else {
				mf.setRGB("RB");
				padLog("No hole found, reposition machine frame correctly and push the cobot (gesture control)." );
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
		this.PTP(holeFrame, relSpeed*_speed[0], false);
		if (found) {
			this._JTBMotion = kiwa.move(linRel(tolerance, tolerance, 0).setJointVelocityRel(scaleSpeed(relSpeed)).breakWhen(_JTConds));
			this._JTBreak = this._JTBMotion.getFiredBreakConditionInfo();
			if (this._JTBreak != null) mf.blinkRGB("GB", 200);
			else { mf.blinkRGB("RB", 200); found = false; }
			this.PTP(holeFrame, relSpeed*_speed[0], false);
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
			padLog("The wrist twist exceeded the limits, try to teach again a reachable position.");
			return true;
		}
	}
	*/
}