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
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKey;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyListener;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyEvent;

public class Tr7_PinAssemblyTeach extends RoboticsAPIApplication {
	// #Define parameters
	private static final boolean log1 = false;	// Log level 1: main events
	private static final boolean log2 = false;	// Log level 2: standard events e.g. frames
	
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
	private boolean endLoopRoutine = false;
	private static final double approachOffset = 40;
	private static final double approachSpeed = 0.1;
	private static final double probeSpeed = 0.05;
	
	// Motion related KUKA API objects
	private IMotionContainer posHoldMotion;			// Motion container for position hold
	
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
		gripper.attachTo(kiwa.getFlange());
		configPadKeysGENERAL();
		state = States.home;
		move.setHome("/_PinAssembly/PrePick");
		move.setGlobalSpeed(0.25);
		move.setJTconds(10.0);
	}

	@Override public void run() {
		while (true) {
			switch (state) {
				case home:
					move.swapLockDir();
					move.PTPhomeCobot();
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
		posHoldMotion = kiwa.moveAsync(move.getPosHold());
		
		teachLoop:
		while (true) {
			if (mf.getUserButton()) {
				Frame newFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
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
						move.swapLockDir();
						posHoldMotion.cancel();
						move.LINREL(0, 0, 0.01, true, 0.5);
						posHoldMotion = kiwa.moveAsync(move.getPosHold());
					default:
						padLog("Command not valid, try again");
						continue teachLoop;
				}
			}
			waitMillis(5);
		}
		padLog("Exiting handguiding teaching mode...");
		posHoldMotion.cancel();
		move.LINREL(0, 0, 0.01, true, 0.5);
		pad.info("Move away from the robot. It will start to replicate the tought sequence in loop.");
		move.PTPhomeCobot();
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
			else move.PTPsafe(targetFrame, 1);
		} 
	}
	
	private void pickPinZ(Frame targetFrame) {
		Frame preFrame = targetFrame.copyWithRedundancy();
		preFrame.setZ(preFrame.getZ() + approachOffset);
		move.PTPsafe(preFrame, 1);
		padLog("Picking process");
		move.LINsafe(targetFrame, approachSpeed);
		cobot.checkPinPick(5, probeSpeed);
		move.LINsafe(preFrame, approachSpeed);
	}
	
	private void placePinY(Frame targetFrame) {
		boolean inserted;
		Frame preFrame = targetFrame.copyWithRedundancy();
		preFrame.setY(preFrame.getY() + approachOffset);
		do  {
			move.PTPsafe(preFrame, 1);
			padLog("Picking process");
			move.LINsafe(targetFrame, approachSpeed);
			cobot.checkPinPlace(5, probeSpeed);
			inserted = move.twistJ7safe(45, 30, 0.15, 0.7);
			move.LINREL(0, 0, -30, true, approachSpeed);
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
							move.setJTconds(maxTorque);
							break;
					}
				}
			}
		};
		pad.keyBarSetup(padKeysListener, "GENERAL", "Teach", "Delete Previous", "Speed", "Torque");
	}
}