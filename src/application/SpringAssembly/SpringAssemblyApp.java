package application.SpringAssembly;

import static eluxLibs.Utils.*;
import eluxLibs.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class SpringAssemblyApp extends RoboticsAPIApplication {
	
	@Inject private Plc_inputIOGroup 	plcin;
	@Inject private Plc_outputIOGroup 	plcout;
	@Inject private MediaFlangeIOGroup 			mediaFlangeIOGroup;
	@Inject	@Named("GripperSpring") 	private Tool GripperSpring;
	
	// Custom modularizing handler objects
	@Inject private HandlerMFio	mf = new HandlerMFio(mediaFlangeIOGroup);
	@Inject private HandlerPLCio plc = new HandlerPLCio(mf, plcin, plcout);
	@Inject private HandlerMov move = new HandlerMov(mf);
	//@Inject private HandlerPad pad = new HandlerPad(mf);
	
	@Override public void initialize() {
		move.setJTConds(15.0);
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
		move.LINsafe("/_Spring/PickJig", 0.25);
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
