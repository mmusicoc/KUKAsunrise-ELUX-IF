package application.Training;

import static eluxLibs.Utils.*;
import eluxLibs.*;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.CartDOF;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.motionModel.PositionHold;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;
import com.kuka.roboticsAPI.uiModel.userKeys.*;

public class Tr4_TeachPickAndPlace extends RoboticsAPIApplication {
	// #Define parameters
	private static final boolean log1 = true;	// Log level 1: main events
	private static final boolean log2 = false;	// Log level 2: standard events e.g. frames
	
	// Standard KUKA API objects
	@Inject private LBR 				kiwa;
	@Inject private Plc_inputIOGroup 	plcin;
	@Inject private Plc_outputIOGroup 	plcout;
	@Inject private MediaFlangeIOGroup 	mfio;
	@Inject	@Named("Pinza") 		private Tool 		gripper;
	// @Inject @Named("VacuumBody") 	private Workpiece 	workpiece;
	
	// Custom modularizing handler objects
	@Inject private HandlerMFio	mf = new HandlerMFio(mfio);
	@Inject private HandlerPLCio plc = new HandlerPLCio(mf, plcin, plcout);
	@Inject private HandlerMov move = new HandlerMov(mf);
	@Inject private HandlerPad pad = new HandlerPad(mf);
	
	// Private properties - application variables
	private FrameList frameList = new FrameList();
	private enum States {home, teach, loop};
	private States state;
	private boolean endLoopRoutine = false;
	private boolean workpieceGripped = false;
	private double relSpeed = 0.25;
	private final double approachOffset = 40;
	private final double approachSpeed = 0.1;
	private final String homeFramePath = "/_HOME/_2_Teach_CENTRAL";
	
	// Motion related KUKA API objects
	private CartesianImpedanceControlMode softMode = new CartesianImpedanceControlMode();  	// for stiffless handguiding
	private CartesianImpedanceControlMode stiffMode = new CartesianImpedanceControlMode();  // for gesture control
	private PositionHold posHold = new PositionHold(softMode, -1, null);  
	private IMotionContainer posHoldMotion;			// Motion container for position hold

	private void progInfo() {
		pad.info("Description of this program operation:\n" + 
					"\tTeaching mode:\n" +
						"\t\t1 click: Register frame\n" +
						"\t\t2 click: Register frame where gripper closes\n" +
						"\t\t3 click: Register frame where gripper opens\n" +
						"\t\tLong press: Exit teaching mode\n" +
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
	//	configPadKeysCONSTRAIN();
		state = States.home;
		move.setHome(homeFramePath);
		
		// Setting the stiffness in HandGuiding mode
		softMode.parametrize(CartDOF.ALL).setStiffness(0.1).setDamping(1);		// HandGuiding
		switch (pad.question("Lock DOF A", "YES", "NO")) {
			case 0:
				softMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(1);
				softMode.parametrize(CartDOF.A).setStiffness(0.1).setDamping(1);
				break;
			case 1: break;
		}
		stiffMode.parametrize(CartDOF.TRANSL).setStiffness(5000).setDamping(1);		// GestureControl
		stiffMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(1); 
		
		relSpeed = 0.25; //pad.askSpeed();
	//	double maxTorque = pad.askTorque();
		move.setJTConds(10.0);					
	}

	@Override public void run() {
		while (true) {
			switch (state) {
				case home:
					plc.askOpen();
					padLog("Going home.");
					move.PTPwithJTConds(homeFramePath, relSpeed);
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
		posHoldMotion = kiwa.moveAsync(posHold);
		
		teachLoop:
		while (true) {
			if (mf.getUserButton()) {
				Frame newFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
				btnInput = mf.checkButtonInput();		// Run the button press check
				switch (btnInput) {
					case 10: 							// Exit handguiding phase
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
					default:
						padLog("Command not valid, try again");
						continue teachLoop;
				}
			}
			waitMillis(5);
		} 
		padLog("Exiting handguiding teaching mode...");
		posHoldMotion.cancel();
		move.LINREL(0, 0, 0.01, 0.5);
		pad.info("Move away from the robot. It will start to replicate the tought sequence in loop.");
		move.PTPwithJTConds(homeFramePath, relSpeed);
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
			else move.PTPwithJTConds(targetFrame, relSpeed);
		}
		
		if (log1) padLog("Loop forward");
		for (int i = 0; i < frameList.size(); i++) {
			if (endLoopRoutine) { endLoopRoutine = false; return; }
			targetFrame = frameList.get(i);
			if (log2) padLog("Going to Frame "+ i +".");
			if (targetFrame.hasAdditionalParameter("PICK")) pickZ(targetFrame);			// Going forward
			else if (targetFrame.hasAdditionalParameter("PLACE")) placeZ(targetFrame);
			else move.PTPwithJTConds(targetFrame, relSpeed);
		} 
	}
	
	private void pickZ(Frame targetFrame) {
		Frame preFrame = targetFrame.copy();
		preFrame.setZ(preFrame.getZ() + approachOffset);
		move.PTPwithJTConds(preFrame, relSpeed);
		padLog("Picking process");
		move.LINwithJTConds(targetFrame, approachSpeed);
		move.checkPartZ(25, 0.01);
		closeGripperCheck(false);
		move.LINwithJTConds(preFrame, approachSpeed);
	}
	
	private void placeZ(Frame targetFrame) {
		Frame preFrame = targetFrame.copy();
		preFrame.setZ(preFrame.getZ() + approachOffset);
		move.PTPwithJTConds(preFrame, relSpeed);
		padLog("Placing process");
		move.LINwithJTConds(targetFrame, approachSpeed);
		openGripperCheck(false);
		move.LINwithJTConds(preFrame, approachSpeed);
	}
	
	private void closeGripperCheck(boolean isPosHold) {
		plc.closeGripperAsync();
		while (!plcin.getPinza_NoPart() & !plcin.getPinza_Holding()) {
			waitMillis(50);
		}
		if (plcin.getPinza_Holding()){
			padLog("Workpiece gripped");
			workpieceGripped = true;
			if (isPosHold) posHoldMotion.cancel();
		//	workpiece.attachTo(gripper.getDefaultMotionFrame()); 
			if (isPosHold) posHoldMotion = kiwa.moveAsync(posHold);
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
			padLog("Workpiece released");
		//	workpiece.detach(); 
			if (isPosHold) posHoldMotion = kiwa.moveAsync(posHold);
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
							relSpeed = pad.askSpeed();
							break;
						case 3:							// KEY - SET TORQUE
							double maxTorque = pad.askTorque();
							move.setJTConds(maxTorque);
							break;
					}
				}
			}
		};
		pad.keyBarSetup(padKeysListener, "GENERAL", "Teach", "Delete Previous", "Speed", "Torque");
	}
}