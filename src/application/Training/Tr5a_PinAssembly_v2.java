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
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.geometricModel.Tool;
// import com.kuka.roboticsAPI.geometricModel.Workpiece;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.motionModel.PositionHold;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKey;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyListener;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyEvent;

public class Tr5a_PinAssembly_v2 extends RoboticsAPIApplication {
	// #Define parameters
	private static final boolean log1 = true;	// Log level 1: main events
	private static final boolean log2 = false;	// Log level 2: standard events e.g. frames
	//private static final boolean log3 = false;	// Log level 3: basic events, redundant info
	
	// Standard KUKA API objects
	@Inject private LBR 				kiwa;
	@Inject private Plc_inputIOGroup 	plcin;
	@Inject private Plc_outputIOGroup 	plcout;
	@Inject private MediaFlangeIOGroup 	mfio;
	@Inject	@Named("Pinza") 		private Tool 		Gripper;
	//@Inject @Named("VacuumBody")	private Workpiece 	VacuumBody;
	// @Inject	private ITaskLogger 		logger;
	
	// Custom modularizing handler objects
	@Inject private HandlerMFio	mf = new HandlerMFio(mfio);
	@Inject private HandlerPad pad = new HandlerPad(mf);
	@Inject private HandlerPLCio plc = new HandlerPLCio(mf, plcin, plcout);
	@Inject private HandlerMov move = new HandlerMov(mf);
	
	// Private properties - application variables
	private FrameList frameList = new FrameList();
	private enum States {home, teach, loop};
	private States state;
	private boolean endLoopRoutine = false;
	private double relSpeed = 0.25;
	private final double approachOffset = 40;
	private final double approachSpeed = 0.1;
	private final String homeFramePath = "/_PinAssembly/Pick";
	
	// Motion related KUKA API objects
	private CartesianImpedanceControlMode softMode = new CartesianImpedanceControlMode();  	// for stiffless handguiding
	private PositionHold posHold = new PositionHold(softMode, -1, null);  
	private IMotionContainer posHoldMotion;			// Motion container for position hold
	
	private void progInfo() {
		pad.info("Description of this program operation:\n" + 
					"\tTeaching mode:\n" +
						"\t\t1 click: Register frame\n" +
						"\t\t2 click: Register frame where pin is picked\n" +
						"\t\t3 click: Register frame where pin is inserted and twisted\n" +
						"\t\tLong press: Exit teaching mode\n" +
					"\tRun mode:\n" +
						"\t\tLoop back and forward along recorded frame list\n" +
						"\t\tPress TEACH Key to return to teach mode\n" +
						"\t\tDefault relSpeed = 0.25\n" +
						"\t\tDefault maxTorque = 10.0 Nm");
	}
	
	@Override public void initialize() {
		progInfo();
		Gripper.attachTo(kiwa.getFlange());
		configPadKeysGENERAL();
		state = States.home;
		move.setHome(homeFramePath);
		
		// Setting the stiffness in HandGuiding mode
		softMode.parametrize(CartDOF.TRANSL).setStiffness(0.01).setDamping(1);		// HandGuiding
		softMode.parametrize(CartDOF.ROT).setStiffness(0.01).setDamping(1);
		
		relSpeed = 0.25; //pad.askSpeed();
		// double maxTorque = pad.askTorque();
		move.setJTConds(10.0);
	}

	@Override public void run() {
		while (true) {
			switch (state) {
				case home:
					padLog("Going home.");
					move.PTPwithJTConds(homeFramePath, relSpeed);
					checkGripper();
					state = States.loop;
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
				btnInput = mf.checkButtonInput();			// Run the button press check
				switch (btnInput) {
					case 10: 							// Exit handguiding phase
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
		endLoopRoutine = false;
		if (log1) padLog("Loop routine.");
		move.PTPwithJTConds("_PinAssembly/Pick", relSpeed);
		if (endLoopRoutine) return;
		pickPinZ("_PinAssembly/Pick/pick1");
		if (endLoopRoutine) return;
		move.PTPwithJTConds("_PinAssembly/Place", relSpeed);
		if (endLoopRoutine) return;
		placePinY("_PinAssembly/Place/place1");
		if (endLoopRoutine) return;
		move.PTPwithJTConds("_PinAssembly/Place", relSpeed);
		if (endLoopRoutine) return;
		move.PTPwithJTConds("_PinAssembly/Pick", relSpeed);
		if (endLoopRoutine) return;
		pickPinZ("_PinAssembly/Pick/pick2");
		if (endLoopRoutine) return;
		move.PTPwithJTConds("_PinAssembly/Place", relSpeed);
		if (endLoopRoutine) return;
		placePinY("_PinAssembly/Place/place2");
		if (endLoopRoutine) return;
		move.PTPwithJTConds("_PinAssembly/Place", relSpeed);
		if (endLoopRoutine) return;
		move.PTPwithJTConds("_PinAssembly/Pick", relSpeed);
		if (endLoopRoutine) return;
		pickPinZ("_PinAssembly/Pick/pick3");
		if (endLoopRoutine) return;
		move.PTPwithJTConds("_PinAssembly/Place", relSpeed);
	}
	
	private void checkGripper() {
		do {
			if (plc.getGripperState() == 1) break;
			else {
				plc.openGripper();
				move.waitPushGesture();
				plc.closeGripper();
			}
		} while (true);
	}
	
	private void pickPinZ(Frame targetFrame) {
		Frame preFrame = targetFrame.copyWithRedundancy();
		preFrame.setZ(preFrame.getZ() + approachOffset);
		move.PTPwithJTConds(preFrame, relSpeed);
		padLog("Picking process");
		move.LINwithJTConds(targetFrame, approachSpeed);
		move.checkPinPick(5, 0.01);
		move.LINwithJTConds(preFrame, approachSpeed);
	}
	
	private void pickPinZ(String targetFramePath) {
		padLog("Pick pin macro at " + targetFramePath);
		ObjectFrame targetFrame = getApplicationData().getFrame(targetFramePath);
		this.pickPinZ(targetFrame.copyWithRedundancy());
	}
	
	private void placePinY(Frame targetFrame) {
		boolean inserted;
		Frame preFrame = targetFrame.copyWithRedundancy();
		preFrame.setY(preFrame.getY() + approachOffset);
		do  {
			move.PTPwithJTConds(preFrame, relSpeed);
			padLog("Picking process");
			move.LINwithJTConds(targetFrame, approachSpeed);
			move.checkPinPlace(5, 0.01);
			inserted = move.twistJ7withJTCond(45, 30, 0.15, 0.7);
			move.LINREL(0, 0, -30, 0.01);
		}
		while (!inserted);		
	}
	
	private void placePinY(String targetFramePath) {
		padLog("Place pin macro at " + targetFramePath);
		ObjectFrame targetFrame = getApplicationData().getFrame(targetFramePath);
		this.placePinY(targetFrame.copyWithRedundancy());
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