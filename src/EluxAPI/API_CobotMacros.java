package EluxAPI;

/*******************************************************************
* <b> STANDARD API CLASS BY mario.musico@electrolux.com </b> <p>
*/

import static EluxAPI.Utils.*;
//import static com.kuka.roboticsAPI.motionModel.BasicMotions.positionHold;

import javax.inject.Inject;
import javax.inject.Singleton;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.*;


@Singleton
public class API_CobotMacros extends RoboticsAPIApplication {
	// Standard KUKA API objects
	@Override public void run() { while (true) { break; } }		// Compulsory method for RoboticsAPIApplication derived classes
	@Inject private API_MF mf;
	@Inject private API_PLC plc;
	@Inject private API_Movements move;
	
	// Private properties
	private double _releaseDist;
	
	// CONSTRUCTOR
	@Inject	public API_CobotMacros(API_MF _mf, API_PLC _plc, API_Movements _move) {
		this.mf = _mf;
		this.move = _move;
		_releaseDist = 10;
	}
	
	// SETTERS ******************************************************************
	
	public void setSafeRelease(double releaseDist) { this._releaseDist = releaseDist; }
	
	// COBOT MACROS **************************************************************
	
	public boolean PTPcobot(Frame targetFrame, double relSpeed, boolean forceEnd){		// overloading for taught points
		if (!move.PTPsafe(targetFrame, relSpeed)) {
			move.LINREL(0, 0, -_releaseDist, true, 1);
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
			move.LINREL(0, 0, -_releaseDist, true, 1);
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
			move.LINREL(0, 0, -_releaseDist, true, 1);
			mf.waitUserButton();
			relSpeed *= 0.8;
			if (forceEnd) LINcobot(targetFrame2, relSpeed, forceEnd);			// SECURITY MEASURE, NEW CIRC CAN OVERSHOOT!!
			return false;
		}
		return true;
	}
	
	public boolean CIRCcobot(String targetFramePath1, String targetFramePath2, double relSpeed, boolean forceEnd){
		ObjectFrame targetFrame1 = getApplicationData().getFrame(targetFramePath1);
		ObjectFrame targetFrame2 = getApplicationData().getFrame(targetFramePath2);
		return this.CIRCcobot(targetFrame1.copyWithRedundancy(), targetFrame2.copyWithRedundancy(), relSpeed, forceEnd);
	}
	
	public void checkGripper() {
		do {
			if (plc.gripperIsHolding()) break;
			else {
				plc.openGripper();
				move.waitPushGesture();
				plc.closeGripper();
			}
		} while (true);
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
	
	// UNTESTED
	
	public void probe(double x, double y, double z, boolean absolute, double relSpeed, double maxTorque) {
		Frame targetFrame = move.getFlangePos();
		boolean pieceFound = false;
		padLog("Checking component presence...");
		do {
			mf.setRGB("G");
			move.setJTconds(maxTorque);
			pieceFound = move.LINRELsafe(x, y, z, absolute, relSpeed);
			move.resetJTconds();
			if (pieceFound) {
				move.LIN(targetFrame, relSpeed, false);
				mf.blinkRGB("GB", 400);
			} else {
				mf.setRGB("RB");
				padLog("Reposition the workpiece correctly and push the cobot (gesture control)." );
				this.waitPushGestureZ(targetFrame);
			}
		} while (!pieceFound);
		waitMillis(250);
	}
	
	public void checkPinPick(double tolerance, double relSpeed) {
		Frame targetFrame = move.getFlangePos();
		boolean pinFound;
		do {
			pinFound = probeXY(tolerance, relSpeed, 3);
			if (pinFound) {
				padLog("Pin found. " ); 
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
		Frame targetFrame = move.getFlangePos();
		boolean holeFound;
		do {
			holeFound = probeXY(tolerance, relSpeed, 3);
			if (holeFound) {
				padLog("Hole found. " ); 
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
		Frame targetFrame = move.getFlangePos();
		boolean found = false;
		mf.blinkRGB("GB", 250);
		move.setJTconds(maxTorque);
		mf.setRGB("G");
		if (move.LINRELsafe(-tolerance, -tolerance, 0, true, relSpeed)) mf.blinkRGB("RB", 200);
		else { 	mf.blinkRGB("GB", 200); found = true; }
		move.PTP(targetFrame, relSpeed, false);
		if (found) {
			if (!move.LINRELsafe(tolerance, tolerance, 0, true, relSpeed)) mf.blinkRGB("GB", 200);
			else { mf.blinkRGB("RB", 200); found = false; }
			move.PTP(targetFrame, relSpeed, false);
		}
		move.resetJTconds();
		return found;
	}
}