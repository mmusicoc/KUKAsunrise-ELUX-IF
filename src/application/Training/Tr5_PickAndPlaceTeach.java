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

public class Tr5_PickAndPlaceTeach extends RoboticsAPIApplication {
	@Inject	@Named("SchunkGripper") private Tool gripper;
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_MF	mf = elux.getMF();
	@Inject private xAPI_Pad pad = elux.getPad();
	@Inject private xAPI_PLC plc = elux.getPLC();
	@Inject private xAPI_Move move = elux.getMove();
	@Inject private xAPI_Compliance comp = elux.getCompliance();
	@Inject private xAPI_Cobot cobot = elux.getCobot();
	
	private static final boolean log1 = false;	// Log level 1: main events
	private static final boolean log2 = false;	// Log level 2: standard events e.g. frames
	
	// Private properties - application variables
	private FrameList frameList = new FrameList();
	private enum States {home, teach, loop};
	private States state;
	private boolean endLoopRoutine = false;
	private boolean workpieceGripped = false;
	private static final double approachOffset = 40;
	private static final double approachSpeed = 0.1;

	private void progInfo() {
		pad.info("Description of this program operation:\n" + 
					"\tTeaching mode:\n" +
						"\t\t1 click: Register frame\n" +
						"\t\t2 clicks: Register frame where gripper closes\n" +
						"\t\t3 clicks: Register frame where gripper opens\n" +
						"\t\tLong press: Exit teaching mode\n" +
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
		move.setHome("/__HOME/_2_Teach_CENTRAL");
		move.setTool(gripper);
		move.setTCP("/GripperCenter");
		move.setGlobalSpeed(0.25);
		move.setJTconds(10.0);					
	}

	@Override public void run() {
		while (true) {
			switch (state) {
				case home:
					plc.askOpen();
					comp.swapLockDir();
					move.PTPhome(1, false);
					plc.askOpen();
					state = States.teach;
					break;
				case teach:
					frameList.free();
					teachRoutine(); 
					state = States.loop;
					break;
				case loop:
					loopRoutine();
					break;
			}
		}
	}
	
	private void teachRoutine(){			// HANDGUIDING PHASE
		int btnInput;
		mf.waitUserButton();
		padLog("Start handguiding teaching.");
		mf.setRGB("B");
		comp.posHoldStart();
		
		teachLoop:
		while (true) {
			if (mf.getUserButton()) {
				Frame newFrame = move.getFlangePos();
				btnInput = mf.checkButtonInput();		// Run the button press check
				switch (btnInput) {
					case 10: 							// Exit hand guiding phase
						if (frameList.size() >= 2) break teachLoop;
						else padLog("Record at least 2 positions to start running.");
						break;
					case 01: 					// Record current position
						frameList.add(newFrame, log1);
						break;
					case 02:					// Record current position & PICK
						newFrame.setAdditionalParameter("PICK", 1);
						mf.blinkRGB("GB", 500);
						closeGripperCheck(true);
						frameList.add(newFrame, log1);
						padLog("Pick here");
						break;
					case 03:					// Record current position & PLACE
						newFrame.setAdditionalParameter("PLACE", 1); 
						mf.blinkRGB("RB", 500);
						openGripperCheck(true);
						frameList.add(newFrame, log1);
						padLog("Place here");
						break;
					case 11:
						mf.blinkRGB("RGB", 500);
						comp.swapLockDir();
						comp.posHoldCancel();
						move.LINREL(0, 0, 0.01, 0.5, false);
						comp.posHoldStart();
					default:
						padLog("Command not valid, try again");
						continue teachLoop;
				}
			}
			waitMillis(5);
		} 
		padLog("Exiting handguiding teaching mode...");
		comp.posHoldCancel();
		move.LINREL(0, 0, 0.01, 0.5, false);
		pad.info("Move away from the robot. It will start to replicate the tought sequence in loop.");
		//move.PTPHOMEsafe();
	}
	
	private void loopRoutine(){
		Frame targetFrame;
		endLoopRoutine = false;
		if (log1) padLog("Loop backwards.");
		for (int i = frameList.size()-1; i >= 0; i--) { 		// loop for going the opposite direction
			if (endLoopRoutine) { endLoopRoutine = false; return; }
			targetFrame = frameList.get(i);
			if (log2) padLog("Going to Frame "+ i +".");
			if (targetFrame.hasAdditionalParameter("PICK")) placeZ(targetFrame);		// Going backwards, inverse actions
			else if (targetFrame.hasAdditionalParameter("PLACE")) pickZ(targetFrame);
			else move.PTP(targetFrame, 1, false);
		}
		
		if (log1) padLog("Loop forward");
		for (int i = 0; i < frameList.size(); i++) {
			if (endLoopRoutine) { endLoopRoutine = false; return; }
			targetFrame = frameList.get(i);
			if (log2) padLog("Going to Frame "+ i +".");
			if (targetFrame.hasAdditionalParameter("PICK")) pickZ(targetFrame);			// Going forward
			else if (targetFrame.hasAdditionalParameter("PLACE")) placeZ(targetFrame);
			else move.PTP(targetFrame, 1, false);
		} 
	}
	
	private void pickZ(Frame targetFrame) {
		Frame preFrame = targetFrame.copy();
		preFrame.setZ(preFrame.getZ() + approachOffset);
		move.PTP(preFrame, 1, false);
		if(log1) padLog("Picking process");
		move.LIN(targetFrame, approachSpeed, false);
		cobot.probe(0, 0, 25, 0.1, 3);
		closeGripperCheck(false);
		move.LIN(preFrame, approachSpeed, false);
	}
	
	private void placeZ(Frame targetFrame) {
		Frame preFrame = targetFrame.copy();
		preFrame.setZ(preFrame.getZ() + approachOffset);
		move.PTP(preFrame, 1, false);
		if(log1) padLog("Placing process");
		move.LIN(targetFrame, approachSpeed, false);
		openGripperCheck(false);
		move.LIN(preFrame, approachSpeed, false);
	}
	
	private void closeGripperCheck(boolean isPosHold) {
		plc.closeGripperAsync();
		while (!plc.gripperIsEmpty() & !plc.gripperIsHolding()) {
			waitMillis(50);
		}
		if (plc.gripperIsHolding()){
			if(log1) padLog("Workpiece gripped");
			workpieceGripped = true;
			if (isPosHold) comp.posHoldCancel();
		//	workpiece.attachTo(gripper.getDefaultMotionFrame()); 
			if (isPosHold) comp.posHoldStart();
		} else {
			padLog("Workpiece NOT gripped");
		}
	}
	
	private void openGripperCheck(boolean isPosHold) {
		plc.openGripperAsync();
		if (!isPosHold) waitMillis(1500);
		if (workpieceGripped) {
			workpieceGripped = false;
			if (isPosHold) comp.posHoldCancel();
			if(log1) padLog("Workpiece released");
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
								endLoopRoutine = true;
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