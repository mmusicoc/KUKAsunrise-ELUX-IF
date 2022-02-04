package application.Training;

import static EluxUtils.Utils.*;
import EluxUtils.FrameList;
import EluxAPI.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKey;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyListener;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyEvent;

public class Tr7_PinAssemblyTeach extends RoboticsAPIApplication {
	@Inject	@Named("SchunkGripper") private Tool gripper;
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_MF mf = elux.getMF();
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
	private static final double approachOffset = 40;
	private static final double approachSpeed = 0.1;
	private static final double probeSpeed = 0.05;
	
	private void progInfo() {
		pad.info("Description of this program operation:\n" + 
					"\tTeaching mode:\n" +
						"\t\t1 click: Register frame\n" +
						"\t\t2 clicks: Register frame where pin is picked\n" +
						"\t\t3 clicks: Register frame when pin is in hole\n" +
						"\t\t\tNOTE: twisting task will be performed automatically\n" +
						"\t\tLong press: Exit teaching mode\n" +
					"\tRun mode:\n" +
						"\t\tLoop back and forward along recorded frame list\n" +
						"\t\tPress TEACH Key to return to teach mode\n" +
						"\t\tDefault relSpeed = 0.25\n" +
						"\t\tDefault maxTorque = 10.0 Nm\n");
	}
	
	@Override public void initialize() {
		progInfo();
		move.setTool(gripper);
		configPadKeysGENERAL();
		state = States.home;
		move.setHome("/_PinAssembly/PrePick");
		move.setGlobalSpeed(0.25);
		move.setMaxTorque(10.0);
	}

	@Override public void run() {
		while (true) {
			switch (state) {
				case home:
					comp.swapLockDir();
					move.PTPhome(1, false);
					cobot.checkGripper();
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
		padLog("Start handguiding teaching mode."); 
		mf.waitUserButton();
		comp.posHoldStart();
		
		teachLoop:
		while (true) {
			if (mf.getUserButton()) {
				Frame newFrame = move.getFlangePos();
				btnInput = mf.checkButtonInput();			// Run the button press check
				switch (btnInput) {
					case 10: 							// Exit hand guiding phase
						if (frameList.size() >= 2) break teachLoop;
						else padLog("Record at least 2 positions to start running.");
						break;
					case 01: 					// Record current position
						frameList.add(newFrame, log1);
						break;
					case 02:
						newFrame.setAdditionalParameter("PICK", 1);
						mf.blinkRGB("GB", 500);
						frameList.add(newFrame, log1);
						padLog("Pick here");
						break;
					case 03:
						newFrame.setAdditionalParameter("PLACE", 1);
						mf.blinkRGB("RB", 500);
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
		move.PTPhome(1, false);
	}
	
	private void loopRoutine(){
		Frame targetFrame;
		endLoopRoutine = false;
		if (log1) padLog("Loop routine.");
		for (int i = 0; i < frameList.size(); i++) { 							// last saved frame is  Counter-1
			if (endLoopRoutine) { endLoopRoutine = false; return; }
			targetFrame = frameList.get(i);
			if (log2) padLog("Going to Frame "+ i +".");
			if (targetFrame.hasAdditionalParameter("PICK")) pickPinZ(targetFrame);
			else if (targetFrame.hasAdditionalParameter("PLACE")) placePinY(targetFrame);
			else move.PTP(targetFrame, 1, false);
		} 
	}
	
	private void pickPinZ(Frame targetFrame) {
		Frame preFrame = targetFrame.copyWithRedundancy();
		preFrame.setZ(preFrame.getZ() + approachOffset);
		move.PTP(preFrame, 1, false);
		padLog("Picking process");
		move.LIN(targetFrame, approachSpeed, false);
		cobot.checkPinPick(5, probeSpeed);
		move.LIN(preFrame, approachSpeed, false);
	}
	
	private void placePinY(Frame targetFrame) {
		boolean inserted;
		Frame preFrame = targetFrame.copyWithRedundancy();
		preFrame.setY(preFrame.getY() + approachOffset);
		do  {
			move.PTP(preFrame, 1, false);
			padLog("Picking process");
			move.LIN(targetFrame, approachSpeed, false);
			cobot.checkPinPlace(5, probeSpeed);
			inserted = move.twistJ7withCheck(45, 30, 0.15, 0.7);
			move.LINREL(0, 0, -30, approachSpeed, false);
		}
		while (!inserted);		
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
							move.setMaxTorque(maxTorque);
							break;
					}
				}
			}
		};
		pad.keyBarSetup(padKeysListener, "GENERAL", "Teach", "Delete Previous", "Speed", "Torque");
	}
}