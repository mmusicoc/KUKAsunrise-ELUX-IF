package application.Training;

import static EluxUtils.Utils.*;
import EluxUtils.FrameList;
import EluxAPI.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.uiModel.userKeys.*;

public class Tr4_PickAndPlace extends RoboticsAPIApplication {
	@Inject	@Named("SchunkGripper") private Tool gripper;
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_Pad pad = elux.getPad();
	@Inject private xAPI_PLC plc = elux.getPLC();
	@Inject private xAPI_Move move = elux.getMove();
	@Inject private xAPI_Compliance comp = elux.getCompliance();
	@Inject private xAPI_Cobot cobot = elux.getCobot();
	
	private static final boolean log1 = false;
	
	// Private properties - application variables
	private FrameList frameList = new FrameList();
	private enum States {home, teach, loop};
	private States state;
	private boolean workpieceGripped = false;
	private static final double approachOffset = 40;
	private static final double approachSpeed = 0.1;
	
	// Motion related KUKA API objects

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
		move.setTool(gripper);
		configPadKeysGENERAL();
		state = States.home;
		move.setHome("/_HOME/_2_Teach_CENTRAL");
		move.setGlobalSpeed(0.25);
		move.setMaxTorque(10.0);					
	}

	@Override public void run() {
		while (true) {
			switch (state) {
				case home:
					plc.askOpen();
					move.PTPhome(0.25, false);
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
		move.LIN("/_PickPlace/_0_Planes", 1, false);
		pickZ("/_PickPlace/_0_Planes/Pick");
		move.LIN("/_PickPlace/_0_Planes/XY", 1, false);
		for(int i = 1; i <= loop; i++) {
			move.LIN("/_PickPlace/_0_Planes/XY/P2", 1, false);
			move.LIN("/_PickPlace/_0_Planes/XY/P3", 1, false);
			move.LIN("/_PickPlace/_0_Planes/XY/P4", 1, false);
			move.LIN("/_PickPlace/_0_Planes/XY", 1, false);
		}
		for(int i = 1; i <= loop; i++) {
			move.LIN("/_PickPlace/_0_Planes/XZ/P2", 1, false);
			move.LIN("/_PickPlace/_0_Planes/XZ/P3", 1, false);
			move.LIN("/_PickPlace/_0_Planes/XZ/P4", 1, false);
			move.LIN("/_PickPlace/_0_Planes/XZ", 1, false);
		}
		for(int i = 1; i <= loop; i++) {
			move.LIN("/_PickPlace/_0_Planes/YZ/P2", 1, false);
			move.LIN("/_PickPlace/_0_Planes/YZ/P3", 1, false);
			move.LIN("/_PickPlace/_0_Planes/YZ/P4", 1, false);
			move.LIN("/_PickPlace/_0_Planes/YZ", 1, false);
		}
		move.LIN("/_PickPlace/_1_Circular/P1r", 1, false);
		for(int i = 1; i <= loop; i++) {
			move.CIRC("/_PickPlace/_1_Circular","/_PickPlace/_1_Circular/P2r",1, false);
			move.CIRC("/_PickPlace/_1_Circular","/_PickPlace/_1_Circular/P1r",1, false);
		}
		move.LIN("/_PickPlace/_1_Circular/P1", 1, false);
		for(int i = 1; i <= loop; i++) {
			move.CIRC("/_PickPlace/_1_Circular","/_PickPlace/_1_Circular/P2", 1, false);
			move.CIRC("/_PickPlace/_1_Circular","/_PickPlace/_1_Circular/P1", 1, false);
		}
		move.LIN("/_PickPlace/_1_Circular", 1, false);
		placeZ("/_PickPlace/_0_Planes/Pick");
		
		move.LIN("/_PickPlace/_2_Drag", 1, false);
		move.LIN("/_PickPlace/_2_Drag/P01", 1, false);
		move.LIN("/_PickPlace/_2_Drag/P02", 0.25, false);
		move.LIN("/_PickPlace/_2_Drag/P03", 1, false);
		move.LIN("/_PickPlace/_2_Drag/P04", 0.5, false);
		move.LIN("/_PickPlace/_2_Drag/P05", 1, false);
		move.LIN("/_PickPlace/_2_Drag/P06", 0.25, false);
		move.LIN("/_PickPlace/_2_Drag/P07", 1, false);
		move.LIN("/_PickPlace/_2_Drag/P08", 1, false);
		move.LIN("/_PickPlace/_2_Drag", 1, false);
	}
	
	private void pickZ(Frame target) {
		Frame preFrame = target.copy();
		preFrame.setZ(preFrame.getZ() - approachOffset);
		move.PTP(preFrame, 1, false);
		if(log1) logmsg("Picking process");
		move.LIN(target, approachSpeed, false);
		cobot.probe(0, 0, 15, 0.1, 2);
		closeGripperCheck(false);
		move.LIN(preFrame, approachSpeed, false);
	}
	
	private void pickZ(String targetPath) {
		if(log1) logmsg("Place part macro at " + targetPath);
		this.pickZ(move.p2f(targetPath));
	}
	
	private void placeZ(Frame target) {
		Frame preFrame = target.copy();
		preFrame.setZ(preFrame.getZ() - approachOffset);
		move.PTP(preFrame, 1, false);
		if(log1) logmsg("Placing process");
		move.LIN(target, approachSpeed, false);
		openGripperCheck(false);
		move.LIN(preFrame, approachSpeed, false);
	}
	
	private void placeZ(String targetPath) {
		if(log1) logmsg("Place part macro at " + targetPath);
		this.placeZ(move.p2f(targetPath));
	}
	
	private void closeGripperCheck(boolean isPosHold) {
		plc.closeGripperAsync();
		while (!plc.gripperIsEmpty() & !plc.gripperIsHolding()) {
			waitMillis(50);
		}
		if (plc.gripperIsHolding()){
			if(log1) logmsg("Workpiece gripped");
			workpieceGripped = true;
			if (isPosHold) comp.posHoldCancel();
		//	workpiece.attachTo(gripper.getDefaultMotionFrame()); 
			if (isPosHold) comp.posHoldStart();
		} else {
			logmsg("Workpiece NOT gripped");
		}
	}
	
	private void openGripperCheck(boolean isPosHold) {
		plc.openGripperAsync();
		if (!isPosHold) waitMillis(1500);
		if (workpieceGripped) {
			workpieceGripped = false;
			if (isPosHold) comp.posHoldCancel();
			if(log1) logmsg("Workpiece released");
		//	workpiece.detach(); 
			if (isPosHold) comp.posHoldStart();
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
							} else logmsg("Already going to teach mode.");
							break;
						case 1: 						// KEY - DELETE PREVIOUS
							if (state == States.teach) {
								if (frameList.getLast().hasAdditionalParameter("PICK")) plc.openGripper();	
								else if (frameList.getLast().hasAdditionalParameter("PLACE")) plc.closeGripper();	
								frameList.removeLast();
							} else logmsg("Key not available in this mode.");
							break;
						case 2:  						// KEY - SET SPEED
							move.setGlobalSpeed(pad.askSpeed());
							break;
						case 3:							// KEY - SET TORQUE
							double maxTorque = pad.askTorque();
							move.setMaxTorque(maxTorque);
							break;
					}
				}
			}
		};
		pad.keyBarSetup(padKeysListener, "GENERAL", "Teach", "Delete Previous", "Speed", "Torque");
	}
}