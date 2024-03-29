package application.PickIt;

import static EluxUtils.Utils.*;
import EluxAPI.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class PickItApp extends RoboticsAPIApplication {
	@Inject	@Named("GripperPickit") 	private Tool GripperPickit;
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_MF	mf = elux.getMF();
	@Inject private xAPI_PLC plc = elux.getPLC();
	@Inject private xAPI_Move move = elux.getMove();
	@Inject private xAPI_Pad pad = new xAPI_Pad(mf);
	@Inject private xAPI_PickIt pickit = new xAPI_PickIt(elux.getRobot());
	
	int failCounter = -1;
	boolean rot180 = false;
	
	@Override public void initialize() {
		move.setMaxTorque(15.0);
		move.setGlobalSpeed(1);
		move.setBlending(20, 5);
		move.setTool(GripperPickit);
		move.setTCP("/Cylinder");
		move.setHome("/_PickIt/Scan");
		if(!move.PTPhome(1, false)) stop();
		if(plc.gripperIsHolding()) {
			move.PTP("/_PickIt/Scan/Release", 1, false);
			plc.askOpen();
			move.PTPhome(1, false);
		}
		pickit.init("192.168.2.12", 30001);
		if(!pickit.config(3, 3, 5000)) stop();
	}

	@Override public void run() {
		logmsg("Start picking sequence");
		int box = 0;
		int pick = 0;
		scan(false);
		while (pickit.isRunning()) {
			box = pickit.getBox();
			if (box > 0) {
				logmsg("Found " + box + " objects, picking model " + pickit.getObjType() 
						+ ", pickPoint "+ pickit.getPickID());
				pick = pick(pickit.getPickFrame());
				if (pick == 0) {
					logmsg("Object found unreachable by robot, polling next one");
					if(pickit.getRemainingObj() > 0) pickit.doSendNextObj();
					else scan(false);
				} else if (pick == -1) {
					logmsg("Gripper collided with unexpected object");
					scan(false);
				} else if (pick == -2) {
					logmsg("Object couldn't be exctracted from box");
					scan(false);
				} else {
					failCounter = 0;
					move.setTCP("/Cylinder");
					move.PTPhome(1, false);
					scan(true);
					place();
					move.LINhome(1, false);
				}
				// while (!pickit.isReady()) { waitMillis(100); padLog("Waiting.");}
			} else if (box == -2){
				failCounter ++;
				scan(false);
			} else if (box == -3) {
				failCounter ++;
				pad.info("Fill the box, Pick-It detected it empty!");
				scan(false);
			}
		}
		pickit.terminate();
	}
	
	private void scan(boolean async) {
		if (!async) failCounter ++;
		if (!async || failCounter > 1) {
			move.setTCP("/Cylinder");
			move.PTPhome(1, false);
		}
		if (failCounter >= 3) {
			vibrate();
			failCounter = -1;
			scan(false);
		}
		if (!async) waitMillis(350);
		pickit.doScanForObj();
	}
	
	private int pick(Frame targetFrame) {
		Frame postFrame = targetFrame.copyWithRedundancy();
		postFrame.setZ(300 - (postFrame.getY() - 400) * 0.6);
		plc.openGripperAsync();
		move.setTCP("/Cylinder/Approach");
		if (move.PTP(targetFrame, 0.75, false) == -1) return 0;
		if (pickit.getObjType() == 1 && 
			(pickit.getPickID() == 1 || pickit.getPickID() == 2)) {
			move.setTCP("/Cylinder");
		} else move.setTCP("/Cylinder/End");
		if (move.LIN(targetFrame, 0.25, false) != 1) return -1;
		plc.closeGripper();
		if (move.LIN(postFrame, 0.6, false) != 1) {
			plc.openGripper();
			move.setTCP("/Cylinder/Approach");
			move.LIN(targetFrame, 0.6, false);
			return -2;
		}
		return 1;
	}
	
	private void place() {
		if ((pickit.getObjType() == 1 && pickit.getPickID() == 1)) {
			move.PTP("/_PickIt/PumpJig/Approach_Z",1, true);
			move.LIN("/_PickIt/PumpJig",0.3, false);
			plc.openGripper();
			move.LIN("/_PickIt/PumpJig/Approach_Z",0.8, true);
		} else if (pickit.getObjType() == 1 && pickit.getPickID() == 2) {
			move.PTP("/_PickIt/Pole/H_pole/H_Zoffset",1, true);
			move.LIN("/_PickIt/Pole/H_pole",0.3, false);
			plc.openGripper();
			move.LIN("/_PickIt/Pole/H_pole/H_Xoffset",0.8, false);
			reorientate();
			move.LIN("/_PickIt/Pole/H_pole",0.3, false);
			plc.closeGripper();
			move.LIN("/_PickIt/Pole/H_pole/H_Zoffset",1, true);
			move.LIN("/_PickIt/PumpJig/Approach_Z",1, true);
			move.LIN("/_PickIt/PumpJig",0.3, false);
			plc.openGripper();
			move.LIN("/_PickIt/PumpJig/Approach_Z",0.8, true);
		} else if (pickit.getObjType() == 3 && pickit.getPickID() == 1) {
			move.setTCP("/Cylinder/End");
			move.PTP("/_PickIt/Pole/V_Zoffset",1, true);
			move.LIN("/_PickIt/Pole", 0.3, false);
			plc.openGripper();
			move.LIN("/_PickIt/Pole/V_Zoffset",0.8, true);
		} else {
			logmsg("None");
		}
	}
	
	private void reorientate() {
		pad.info("Reorientate the piece in the pole for " + pickit.getZrot() 
				+ " deg." + "\n\nIn a final implementation, a servomotor " +
						"could be used to rotate the required angle");
	}
	
	private void vibrate() {
		pad.info("Vibrate the box");
	}
	
	private void stop() {
		logmsg("Program stopped");
		dispose();
	}
	
	@Override public void dispose() { 
		pickit.terminate();
		super.dispose(); 
	}
}
