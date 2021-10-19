package application.SpringAssembly;

import static EluxAPI.Utils.*;
import EluxAPI.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class SpringAssemblyApp extends RoboticsAPIApplication {
	@Inject	@Named("GripperSpring") 	private Tool GripperSpring;
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_MF	mf = elux.getMF();
	@Inject private xAPI_PLC plc = elux.getPLC();
	@Inject private xAPI_Move move = elux.getMove();
	
	@Override public void initialize() {
		move.setJTconds(15.0);
		move.setGlobalSpeed(1);
		move.setBlending(20, 5);
		move.setTool(GripperSpring);
		move.setTCP("/SpringGrab");
		move.setHome("/_Spring/_Home");
		if(!move.PTPhome(1, true)) stop();
		if(plc.gripperIsHolding()) {
			move.PTP("/_Spring/_Home/SafeRelease", 1, false);
			plc.askOpen();
			move.PTPhome(1, true);
		}
	}

	@Override public void run() {
		do {
			padLog("Start picking sequence");
			boolean success;
			do {
				success = pick();
				if (!success) reload();
			} while (!success);
			if (success) placeRight();
		} while (true);
	}
	
	private boolean pick() {
		boolean success;
		move.PTPhome(1, true);
		move.setTCP("/SpringGrab");
		plc.openGripperAsync();
		move.PTP("/_Spring/PickJig/Approach", 0.75, true);
		move.LIN("/_Spring/PickJig", 0.25, false);
		plc.closeGripper();
		move.LIN("/_Spring/PickJig/Approach", 0.6, true);
		success = plc.gripperIsHolding();
		if (!success) padLog("Pick failed");
		return success;
	}
	
	private void placeRight() {
		move.setTCP("/SpringGrab/Hook1center");
		move.PTP("/_Spring/WashingTub/PlaceR/P1_approach", 1, true);
		move.LIN("/_Spring/WashingTub/PlaceR/P2_preinsert", 0.6, true);
		move.LIN("/_Spring/WashingTub/PlaceR/P3_postinsert", 0.15, true);
		move.LIN("/_Spring/WashingTub/PlaceR/P4_rotateZ", 0.3, true);
		move.setTCP("/SpringGrab/Hook1end");
		move.LIN("/_Spring/WashingTub/PlaceR/P5_rotateY", 0.6, false);
		plc.askOpen();
		waitMillis(2000);
		move.setTCP("/SpringGrab");
		move.LIN("/_Spring/WashingTub/PlaceR/P6_exit", 0.6, false);
	}
	
	/*
	private void placeLeft() {
	}
	*/
	
	private void reload() {
		move.PTPhome(1, false);
		mf.waitUserButton();
	}
	
	private void stop() {
		padLog("Program stopped");
		dispose();
	}
	
	@Override public void dispose() { 
		super.dispose(); 
	}
}
