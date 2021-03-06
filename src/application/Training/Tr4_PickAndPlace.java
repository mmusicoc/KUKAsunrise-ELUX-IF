package application.Training;

import static EluxAPI.Utils.*;
import EluxAPI.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.uiModel.userKeys.*;

public class Tr4_PickAndPlace extends RoboticsAPIApplication {
	private static final boolean log1 = false;
	
	// Standard KUKA API objects
	@Inject private LBR 				kiwa;
	@Inject private Plc_inputIOGroup 	plcin;
	@Inject private Plc_outputIOGroup 	plcout;
	@Inject private MediaFlangeIOGroup 	mfio;
	@Inject	@Named("Gripper") 		private Tool 		gripper;
	
	// Custom modularizing handler objects
	@Inject private API_MF	mf = new API_MF(mfio);
	@Inject private API_Pad pad = new API_Pad(mf);
	@Inject private API_PLC plc = new API_PLC(mf, plcin, plcout);
	@Inject private API_Movements move = new API_Movements(mf);
	@Inject private API_CobotMacros cobot = new API_CobotMacros(mf, plc, move);
	
	// Private properties - application variables
	private FrameList frameList = new FrameList();
	private enum States {home, teach, loop};
	private States state;
	private boolean workpieceGripped = false;
	private static final double approachOffset = 40;
	private static final double approachSpeed = 0.1;
	
	// Motion related KUKA API objects
	private IMotionContainer posHoldMotion;			// Motion container for position hold

	private void progInfo() {
		pad.info("Description of this program operation:\n" + 
					"\tRun mode:\n" +
						"\t\tLoop back and forward along recorded frame list\n" +
						"\t\tPress TEACH Key to return to teach mode\n" +
						"\t\tDefault relSpeed = 0.25\n" +
						"\t\tDefault maxTorque = 10.0 Nm");
	}
	
	@Override public void initialize() {
		progInfo();
		gripper.attachTo(kiwa.getFlange());
		configPadKeysGENERAL();
		state = States.home;
		move.setHome("/_HOME/_2_Teach_CENTRAL");
		move.setGlobalSpeed(0.25);
		move.setJTconds(10.0);					
	}

	@Override public void run() {
		while (true) {
			switch (state) {
				case home:
					plc.askOpen();
					move.PTPhomeCobot();
					plc.askOpen();
					state = States.loop;
					break;
				case loop:
					loopRoutine();
					break;
			}
		}
	}
	
	private void loopRoutine(){
		int loop = 2;
		move.LINsafe("/_PickPlace/_0_Planes",1);
		pickZ("/_PickPlace/_0_Planes/Pick");
		move.LINsafe("/_PickPlace/_0_Planes/XY",1);
		for(int i = 1; i <= loop; i++) {
			move.LINsafe("/_PickPlace/_0_Planes/XY/P2",1);
			move.LINsafe("/_PickPlace/_0_Planes/XY/P3",1);
			move.LINsafe("/_PickPlace/_0_Planes/XY/P4",1);
			move.LINsafe("/_PickPlace/_0_Planes/XY",1);
		}
		for(int i = 1; i <= loop; i++) {
			move.LINsafe("/_PickPlace/_0_Planes/XZ/P2",1);
			move.LINsafe("/_PickPlace/_0_Planes/XZ/P3",1);
			move.LINsafe("/_PickPlace/_0_Planes/XZ/P4",1);
			move.LINsafe("/_PickPlace/_0_Planes/XZ",1);
		}
		for(int i = 1; i <= loop; i++) {
			move.LINsafe("/_PickPlace/_0_Planes/YZ/P2",1);
			move.LINsafe("/_PickPlace/_0_Planes/YZ/P3",1);
			move.LINsafe("/_PickPlace/_0_Planes/YZ/P4",1);
			move.LINsafe("/_PickPlace/_0_Planes/YZ",1);
		}
		move.LINsafe("/_PickPlace/_1_Circular/P1r",1);
		for(int i = 1; i <= loop; i++) {
			move.CIRC("/_PickPlace/_1_Circular","/_PickPlace/_1_Circular/P2r",1);
			move.CIRC("/_PickPlace/_1_Circular","/_PickPlace/_1_Circular/P1r",1);
		}
		move.LINsafe("/_PickPlace/_1_Circular/P1",1);
		for(int i = 1; i <= loop; i++) {
			move.CIRC("/_PickPlace/_1_Circular","/_PickPlace/_1_Circular/P2",1);
			move.CIRC("/_PickPlace/_1_Circular","/_PickPlace/_1_Circular/P1",1);
		}
		move.LINsafe("/_PickPlace/_1_Circular",1);
		placeZ("/_PickPlace/_0_Planes/Pick");
		
		move.LINsafe("/_PickPlace/_2_Drag", 1);
		move.LINsafe("/_PickPlace/_2_Drag/P01", 1);
		move.LINsafe("/_PickPlace/_2_Drag/P02", 0.25);
		move.LINsafe("/_PickPlace/_2_Drag/P03", 1);
		move.LINsafe("/_PickPlace/_2_Drag/P04", 0.5);
		move.LINsafe("/_PickPlace/_2_Drag/P05", 1);
		move.LINsafe("/_PickPlace/_2_Drag/P06", 0.25);
		move.LINsafe("/_PickPlace/_2_Drag/P07", 1);
		move.LINsafe("/_PickPlace/_2_Drag/P08", 1);
		move.LINsafe("/_PickPlace/_2_Drag", 1);
	}
	
	private void pickZ(Frame targetFrame) {
		Frame preFrame = targetFrame.copy();
		preFrame.setZ(preFrame.getZ() - approachOffset);
		move.PTPsafe(preFrame, 1);
		if(log1) padLog("Picking process");
		move.LINsafe(targetFrame, approachSpeed);
		cobot.probe(0, 0, 15, true, 0.1, 2);
		closeGripperCheck(false);
		move.LINsafe(preFrame, approachSpeed);
	}
	
	private void pickZ(String targetFramePath) {
		if(log1) padLog("Place part macro at " + targetFramePath);
		ObjectFrame targetFrame = getApplicationData().getFrame(targetFramePath);
		this.pickZ(targetFrame.copyWithRedundancy());
	}
	
	private void placeZ(Frame targetFrame) {
		Frame preFrame = targetFrame.copy();
		preFrame.setZ(preFrame.getZ() - approachOffset);
		move.PTPsafe(preFrame, 1);
		if(log1) padLog("Placing process");
		move.LINsafe(targetFrame, approachSpeed);
		openGripperCheck(false);
		move.LINsafe(preFrame, approachSpeed);
	}
	
	private void placeZ(String targetFramePath) {
		if(log1) padLog("Place part macro at " + targetFramePath);
		ObjectFrame targetFrame = getApplicationData().getFrame(targetFramePath);
		this.placeZ(targetFrame.copyWithRedundancy());
	}
	
	private void closeGripperCheck(boolean isPosHold) {
		plc.closeGripperAsync();
		while (!plcin.getPinza_NoPart() & !plcin.getPinza_Holding()) {
			waitMillis(50);
		}
		if (plcin.getPinza_Holding()){
			if(log1) padLog("Workpiece gripped");
			workpieceGripped = true;
			if (isPosHold) posHoldMotion.cancel();
		//	workpiece.attachTo(gripper.getDefaultMotionFrame()); 
			if (isPosHold) posHoldMotion = kiwa.moveAsync(move.getPosHold());
		} else {
			padLog("Workpiece NOT gripped");
		}
	}
	
	private void openGripperCheck(boolean isPosHold) {
		plc.openGripperAsync();
		if (!isPosHold) waitMillis(1500);
		if (workpieceGripped) {
			workpieceGripped = false;
			if (isPosHold) posHoldMotion.cancel();
			if(log1) padLog("Workpiece released");
		//	workpiece.detach(); 
			if (isPosHold) posHoldMotion = kiwa.moveAsync(move.getPosHold());
		}
	}
	
	private void configPadKeysGENERAL() { 					// TEACH buttons						
		IUserKeyListener padKeysListener = new IUserKeyListener() {
			@Override public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				if (event == UserKeyEvent.KeyDown) {
					switch (key.getSlot()) {
						case 0:  						// KEY - TEACH MODE
							if (state == States.loop) {
								state = States.home;
								break;
							} else padLog("Already going to teach mode.");
							break;
						case 1: 						// KEY - DELETE PREVIOUS
							if (state == States.teach) {
								if (frameList.getLast().hasAdditionalParameter("PICK")) plc.openGripper();	
								else if (frameList.getLast().hasAdditionalParameter("PLACE")) plc.closeGripper();	
								frameList.removeLast();
							} else padLog("Key not available in this mode.");
							break;
						case 2:  						// KEY - SET SPEED
							move.setGlobalSpeed(pad.askSpeed());
							break;
						case 3:							// KEY - SET TORQUE
							double maxTorque = pad.askTorque();
							move.setJTconds(maxTorque);
							break;
					}
				}
			}
		};
		pad.keyBarSetup(padKeysListener, "GENERAL", "Teach", "Delete Previous", "Speed", "Torque");
	}
}