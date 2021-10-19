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
public class xAPI_Cobot extends RoboticsAPIApplication {
	// Standard KUKA API objects
	@Override public void run() { while (true) { break; } }
	@Inject private xAPI_MF mf;
	@Inject private xAPI_PLC plc;
	@Inject private xAPI_Move move;
	@Inject private xAPI_Compliance comp;
	
	// CONSTRUCTOR
	@Inject	public xAPI_Cobot(xAPI_MF _mf, xAPI_PLC _plc, xAPI_Move _move, xAPI_Compliance _comp) {
		this.mf = _mf;
		this.plc = _plc;
		this.move = _move;
		this.comp = _comp;
	}
	
	// COBOT MACROS ------------------------------------------------------------------
		
	public void checkGripper() {
		do {
			if (plc.gripperIsHolding()) break;
			else {
				plc.openGripper();
				comp.waitPushGesture();
				plc.closeGripper();
			}
		} while (true);
	}
	
	public void waitPushGestureX(Frame targetFrame) {
		Frame preFrame = targetFrame.copyWithRedundancy();
		preFrame.setX(preFrame.getX() + 50);
		move.LIN(preFrame, 0.1, true);
		comp.waitPushGesture();
		move.LIN(targetFrame, 0.1, true);
	}
	
	public void waitPushGestureY(Frame targetFrame) {
		Frame preFrame = targetFrame.copyWithRedundancy();
		preFrame.setY(preFrame.getY() + 50);
		move.LIN(preFrame, 0.1, true);
		comp.waitPushGesture();
		move.LIN(targetFrame, 0.1, true);
	}
	
	public void waitPushGestureZ(Frame targetFrame) {
		Frame preFrame = targetFrame.copyWithRedundancy();
		preFrame.setZ(preFrame.getZ() + 50);
		move.LIN(preFrame, 0.1, true);
		comp.waitPushGesture();
		move.LIN(targetFrame, 0.1, true);
	}
	
	// UNTESTED
	
	public void probe(double x, double y, double z, double relSpeed, double maxTorque) {
		Frame targetFrame = move.getFlangePos();
		boolean pieceFound = false;
		padLog("Checking component presence...");
		do {
			mf.setRGB("G");
			double prevMaxTorque = move.getMaxTorque();
			move.setJTconds(maxTorque);
			pieceFound = (move.LINREL(x, y, z, relSpeed, false) == 1);
			move.setJTconds(prevMaxTorque);
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
		double prevMaxTorque = move.getMaxTorque();
		move.setJTconds(maxTorque);
		mf.setRGB("G");
		if (move.LINREL(-tolerance, -tolerance, 0, relSpeed, false) == 1) mf.blinkRGB("RB", 200);
		else { 	mf.blinkRGB("GB", 200); found = true; }
		move.PTP(targetFrame, relSpeed, false);
		if (found) {
			if (move.LINREL(tolerance, tolerance, 0, relSpeed, false) != 1) mf.blinkRGB("GB", 200);
			else { mf.blinkRGB("RB", 200); found = false; }
			move.PTP(targetFrame, relSpeed, false);
		}
		move.setJTconds(prevMaxTorque);
		return found;
	}
}