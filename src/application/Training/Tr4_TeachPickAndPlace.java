package application.Training;

import static utils.Utils.*;
import utils.HandlerPad;
import utils.HandlerMFio;
import utils.HandlerPLCio;
import utils.HandlerMov;
import utils.FrameList;

import javax.inject.Inject; 
import javax.inject.Named;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.executionModel.IFiredConditionInfo;
import com.kuka.roboticsAPI.motionModel.*;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;
import com.kuka.roboticsAPI.geometricModel.*;
import com.kuka.roboticsAPI.uiModel.userKeys.*;

public class Tr4_TeachPickAndPlace extends RoboticsAPIApplication {
	// #Define parameters
	private static final boolean log1 = true;	// Log level 1: main events
	private static final boolean log2 = false;	// Log level 2: standard events e.g. frames
//	private static final boolean log3 = false;	// Log level 3: basic events, redundant info
	
	// Standard KUKA API objects
	@Inject private LBR 				kiwa;
	@Inject private Plc_inputIOGroup 	plcin;
	@Inject private Plc_outputIOGroup 	plcout;
	@Inject private MediaFlangeIOGroup 	mfio;
	@Inject	@Named("Pinza") 		private Tool 		Gripper;
	// @Inject @Named("VacuumBody") 	private Workpiece 	VacuumBody;
	
	// Custom modularizing handler objects
	@Inject private HandlerMFio	mf = new HandlerMFio(mfio);
	@Inject private HandlerPad pad = new HandlerPad(mf);
	@Inject private HandlerPLCio plc = new HandlerPLCio(mf, plcin, plcout);
	@Inject private HandlerMov move = new HandlerMov(mf);
	
	// Private properties - application variables
	private FrameList frameList = new FrameList();
	private enum States {state_home, state_teach, state_loop};
	private States state; 
	private double relSpeed = 0.25;
	private String homeFramePath;
	private boolean endLoopRoutine = false;
	private boolean workpieceGripped = false;
	
	// Motion related KUKA API objects
	private CartesianImpedanceControlMode softMode = new CartesianImpedanceControlMode();  	// for stiffless handguiding
	private CartesianImpedanceControlMode stiffMode = new CartesianImpedanceControlMode();  // for gesture control
	private PositionHold posHold = new PositionHold(softMode, -1, null);  
	private IMotionContainer posHoldMotion;			// Motion container for position hold

	@Override
	public void initialize() {
		double maxTorque = 10.0;
		int promptAns;
		padLog("Initializing...");
		Gripper.attachTo(kiwa.getFlange());
		configPadKeysTEACH();
		state = States.state_home;
		homeFramePath = "/_HOME/_2_Teach_CENTRAL";
		move.setHome(homeFramePath);
		
		// Setting the stiffness in HandGuiding mode
		softMode.parametrize(CartDOF.ALL).setStiffness(0.1).setDamping(1);		// HandGuiding
		promptAns = pad.question("Lock DOF A", "YES", "NO");  
		switch (promptAns) {
			case 0:
				softMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(1);
				softMode.parametrize(CartDOF.A).setStiffness(0.1).setDamping(1);
				break;
			case 1: break;
		}
		stiffMode.parametrize(CartDOF.TRANSL).setStiffness(5000).setDamping(1);		// GestureControl
		stiffMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(1); 
		
		maxTorque = pad.askTorque();
		move.setJTConds(maxTorque);
		relSpeed = pad.askSpeed();
	}

	@Override
	public void run() {
		while (true) {
			switch (state) {
				case state_home:
					plc.askOpen();
					padLog("Going home.");
					move.PTPwithJTConds(homeFramePath, relSpeed);
					state = States.state_teach;
					break;
				case state_teach:
					frameList.free();
					teachRoutine(); 
					state = States.state_loop;
					break;
				case state_loop:
					endLoopRoutine = false;
					loopRoutine();
					break;
			}
		}
	}
	
	private void teachRoutine(){			// HANDGUIDING PHASE
		int btnInput;
		mf.waitUserButton();
		padLog("Start hand guiding."); 
		posHoldMotion = kiwa.moveAsync(posHold);
		
		teachLoop:
		while (true) {
			if (mf.getUserButton()) {
				Frame newFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
				btnInput = mf.checkButtonInput();		// Run the button press check
				switch (btnInput) {
					case 10: 							// Exit handguiding phase
						if (frameList.size() > 2) break teachLoop;
						else padLog("Record at least 2 positions to start running.");
						break;
					case 01: 					// Record current position
						frameList.add(newFrame, log1);
						break;
					case 02:					// Record current position & Close Gripper
						newCloseFrame(newFrame);
						frameList.add(newFrame, log1);
						break;
					case 03:					// Record current position & Open Gripper
						newOpenFrame(newFrame);
						frameList.add(newFrame, log1);
						break;
					default:
						padLog("Command not valid, try again");
						continue teachLoop;
				}
			}
		} 
		padLog("Exiting handguiding teaching mode...");
		posHoldMotion.cancel();
		move.PTPwithJTConds(homeFramePath, relSpeed);
	}
	
	private void loopRoutine(){
		padLog("Loop backwards.");
		for (int i = frameList.size()-1; i >= 0; i--) { 		// loop for going the opposite direction
			if (endLoopRoutine) {
				endLoopRoutine = false;
				return;
			}
			if (log2) padLog("Going to Frame "+ i +".");
			move.PTPwithJTConds(frameList.get(i), relSpeed);
			if (frameList.get(i).hasAdditionalParameter("Close Gripper")) {	// going the opposite direction so opposite command
				openGripperCheck(false);
			}
			if (frameList.get(i).hasAdditionalParameter("Open Gripper")) { // going the opposite direction so opposite command
				checkComponent(25);
				closeGripperCheck(false);
			}
		}
		
		padLog("Loop forward");
		for (int i = 0; i < frameList.size(); i++) {
			if (endLoopRoutine) {
				endLoopRoutine = false;
				return;
			}
			if (log2) padLog("Going to Frame "+ i +".");
			move.PTPwithJTConds(frameList.get(i), relSpeed);
			if (frameList.get(i).hasAdditionalParameter("Open Gripper")) {
				openGripperCheck(false);
			}
			if (frameList.get(i).hasAdditionalParameter("Close Gripper")) {
				checkComponent(25);
				closeGripperCheck(false);
			}
		} 
	}
	
	private void newCloseFrame(Frame newFrame) {
		newFrame.setAdditionalParameter("Close Gripper", 1);
		mf.blinkRGB("GB", 500);
		closeGripperCheck(true);
	}
	
	private void newOpenFrame(Frame newFrame) {
		newFrame.setAdditionalParameter("Open Gripper", 1); 
		mf.blinkRGB("RB", 500);
		openGripperCheck(true);
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
		//	VacuumBody.attachTo(Gripper.getDefaultMotionFrame()); 
			if (isPosHold) posHoldMotion = kiwa.moveAsync(posHold);
		} else {
			padLog("Workpiece NOT gripped");
		}
	}
	
	private void openGripperCheck(boolean isPosHold) {
		plc.openGripperAsync();
		if (!isPosHold) waitMillis(2000);
		if (workpieceGripped) {
			workpieceGripped = false;
			if (isPosHold) posHoldMotion.cancel();
			padLog("Workpiece released");
		//	VacuumBody.detach(); 
			if (isPosHold) posHoldMotion = kiwa.moveAsync(posHold);
		}
	}
	
	private void checkComponent(int probeDist){
		boolean pieceFound = false;
		IMotionContainer torqueBreakMotion;		// Motion container with torque break condition
		IFiredConditionInfo JTBreak;
		padLog("Checking component...");
		do {
			mf.setRGB("G");
			torqueBreakMotion = kiwa.move(linRel(0, 0, probeDist).setJointVelocityRel(0.01).breakWhen(move.getJTConds())); 
			JTBreak = torqueBreakMotion.getFiredBreakConditionInfo();
			if (JTBreak != null) {
				System.out.println("Component detected. " ); 
				mf.blinkRGB("GB", 800);
				kiwa.move(linRel(0, 0, -probeDist).setJointVelocityRel(relSpeed));
				pieceFound = true;
			} else {
				mf.setRGB("RB");
				System.out.println("No components detected, Reposition the workpiece correctly and push the cobot (gesture control)." );
				kiwa.move(linRel(0, 0, -probeDist).setJointVelocityRel(relSpeed));
				kiwa.move(positionHold(stiffMode, -1, null).breakWhen(move.getJTConds()));
			}
		} while (!pieceFound);
		waitMillis(250);
	}
	
	private void configPadKeysTEACH() { 					// TEACH buttons						
		IUserKeyListener padKeysListener = new IUserKeyListener() {
			boolean padKeyPressed = false;
			@Override
			public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				if (!padKeyPressed) {
					padKeyPressed = true;
					Frame newFrame;
					switch (key.getSlot()) {
						case 0:  						// KEY - TEACH MODE
							if (state == States.state_loop) {
								state = States.state_home;
								endLoopRoutine = true;
								break;
							} else padLog("Key not available in this mode.");
						case 1: 						// KEY - DELETE PREVIOUS
							if (state == States.state_teach) {
								if (frameList.getLast().hasAdditionalParameter("Close Gripper")) plc.openGripper();	
								else if (frameList.getLast().hasAdditionalParameter("Open Gripper")) plc.closeGripper();	
								frameList.removeLast();
								break;
							} else padLog("Key not available in this mode.");
						case 2:							// KEY - OPEN GRIPPER 
							if (state == States.state_teach) {
								newFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
								newOpenFrame(newFrame);
								frameList.add(newFrame, true);
								break;
							} else padLog("Key not available in this mode.");
						case 3:  						//KEY - CLOSE GRIPPER
							if (state == States.state_teach) {
								newFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
								newCloseFrame(newFrame);
								frameList.add(newFrame, true);
								break;
							} else padLog("Key not available in this mode.");
						default:
							break;
					}
				} else {
					padKeyPressed = false;
				}
			}
		};
		pad.keyBarSetup(padKeysListener, "TEACH", "TEACH", "Delete Previous", "Open Gripper", "Close Gripper");
	}
}