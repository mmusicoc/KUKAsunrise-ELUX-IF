package application.Training;

import static com.kuka.roboticsAPI.motionModel.BasicMotions.lin;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.linRel;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.ptp;
import static utils.Utils.padLog;
import static utils.Utils.waitMillis;

import javax.inject.Inject;
import javax.inject.Named;

import utils.FrameList;
import utils.HandlerMFio;
import utils.HandlerMov;
import utils.HandlerPLCio;
import utils.HandlerPad;

import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.conditionModel.JointTorqueCondition;
import com.kuka.roboticsAPI.deviceModel.JointEnum;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.executionModel.IFiredConditionInfo;
import com.kuka.roboticsAPI.geometricModel.CartDOF;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.Workpiece;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.motionModel.PositionHold;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKey;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyListener;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyEvent;

public class Tr5b_PinAssembly_v2 extends RoboticsAPIApplication {
	// #Define parameters
	private static final boolean log1 = true;	// Log level 1: main events
	private static final boolean log2 = false;	// Log level 2: standard events e.g. frames
	private static final boolean log3 = false;	// Log level 3: basic events, redundant info
	
	// Standard KUKA API objects
	@Inject private LBR 				kiwa;
	@Inject private Plc_inputIOGroup 	plcin;
	@Inject private Plc_outputIOGroup 	plcout;
	@Inject private MediaFlangeIOGroup 	mfio;
	@Inject	@Named("Pinza") 		private Tool 		Gripper;
	@Inject @Named("VacuumBody")	private Workpiece 	VacuumBody;
	// @Inject	private ITaskLogger 		logger;
	
	// Custom modularizing handler objects
	@Inject private HandlerMFio	mf = new HandlerMFio(mfio);
	@Inject private HandlerPad pad = new HandlerPad(mf);
	@Inject private HandlerPLCio plc = new HandlerPLCio(mf, plcin, plcout);
	@Inject private HandlerMov move = new HandlerMov(mf);
	
	// Private properties - application variables
	private FrameList frameList = new FrameList();
	private enum States {state_home, state_teach, state_loop};
	private States state;
	private boolean endLoopRoutine = false;
	private double relSpeed = 0.25;
	private final double approachZ = 40;
	private final double approachSpeed = 0.1;
	private final String homeFramePath = "/_PinAssembly/Pick";
	
	// Motion related KUKA API objects
	private CartesianImpedanceControlMode softMode = new CartesianImpedanceControlMode();  	// for stiffless handguiding
	private CartesianImpedanceControlMode stiffMode = new CartesianImpedanceControlMode();  // for gesture control
	private PositionHold posHold = new PositionHold(softMode, -1, null);  
	private IMotionContainer posHoldMotion;			// Motion container for position hold
	
	
	// DEPRECATED
	private boolean reducedSpeed = false; 
	private double maxTorque = 10.0;
	private JointTorqueCondition torqueBreakCondition1, torqueBreakCondition2, torqueBreakCondition3, torqueBreakCondition4, torqueBreakCondition5, torqueBreakCondition6, torqueBreakCondition7 ;					// for torque break condition
	private IFiredConditionInfo info;
	
	@Override public void initialize() {
		double maxTorque;
		padLog("Initializing...");
		Gripper.attachTo(kiwa.getFlange());
		configPadKeysGENERAL();
		configPadKeysCONSTRAIN();
		state = States.state_home;
		move.setHome(homeFramePath);
		
		// Setting the stiffness in HandGuiding mode
		softMode.parametrize(CartDOF.TRANSL).setStiffness(0.1).setDamping(1);		// HandGuiding
		softMode.parametrize(CartDOF.ROT).setStiffness(0.1).setDamping(1);
		stiffMode.parametrize(CartDOF.TRANSL).setStiffness(5000).setDamping(1);		// GestureControl
		stiffMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(1); 
		
		maxTorque = pad.askTorque();
		move.setJTConds(maxTorque);
		relSpeed = pad.askSpeed();
	}

	@Override public void run() {
		while (true) {
			switch (state) {
				case state_home:
					padLog("Going home.");
					move.PTPwithJTConds(homeFramePath, relSpeed);
					plc.askClose(true);
					state = States.state_teach;
					break;
				case state_teach:
					frameList.free();
					teachRoutine(); 
					state = States.state_loop;
					break;
				case state_loop:
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
				btnInput = mf.checkButtonInput();			// Run the button press check
				switch (btnInput) {
					case 10: 							// Exit handguiding phase
						if (frameList.size() > 2) break teachLoop;
						else padLog("Record at least 2 positions to start running.");
						break;
					case 01: 					// Record current position
						frameList.add(newFrame, log1);
						break;
					case 02:
						newFrame.setAdditionalParameter("PICK", 1);
						mf.blinkRGB("GB", 500);
						frameList.add(newFrame, log1);
						break;
					case 03:
						newFrame.setAdditionalParameter("PLACE", 1);
						mf.blinkRGB("RB", 500);
						frameList.add(newFrame, log1);
						break;
				}
			}
			waitMillis(5);
		}
		padLog("Exiting handguiding teaching mode...");
		posHoldMotion.cancel();
		move.PTPwithJTConds(homeFramePath, relSpeed);
	}
	
	private void loopRoutine(){
		Frame targetFrame, preFrame;
		endLoopRoutine = false;
		if (log1) padLog("Loop routine.");
		for (int i = 0; i < frameList.size(); i++) { 							// last saved frame is  Counter-1
			if (endLoopRoutine) {
				endLoopRoutine = false;
				return;
			}
			targetFrame = frameList.get(i);
			if (log2) padLog("Going to Frame "+ i +".");
			if (frameList.get(i).hasAdditionalParameter("PICK")) {
				
				pickPin(frameList.get(i)); 	 										// go to pick the pin
				while (true) {
					if (checkInsertion() == false){									// check if pin is there
						mf.setRGB("R"); 
						moveLinRelWithCollisionDetection(0,0,50);								// go back after failed insertion check 
						mf.waitUserButton();
						mf.setRGB("R"); 
						reducedSpeed = true; 					// set to false after 1 run
						pickPin(frameList.get(i));  
					} else { 
						moveLinRelWithCollisionDetection(0,0,50);
						break;
					}
				}  
			} else if (frameList.get(i).hasAdditionalParameter("PLACE")) {
				insertPin(frameList.get(i));				
				while (true) {
					if (checkInsertion() == false){									// check if pin is inserted
						mf.setRGB("R"); 
						moveLinRelWithCollisionDetection(0,50,0);								// go back after failed insertion check 
						mf.waitUserButton();
						mf.setRGB("R"); 
						reducedSpeed = true; 					// set to false after 1 run
						insertPin(frameList.get(i)); 
					} else {
						move.twistJ7(60, 0.15, 0.7);
						waitMillis(500);
						moveLinRelWithCollisionDetection(0,50,0);
						break;
					}
				}
			} else { 
				move.PTPwithJTConds(frameList.get(i), relSpeed);
			}
		} 
	}
	
	private void pickPin(Frame targetFrame) { 
		padLog("Picking up a pin.");
		Frame preTargetFrame = targetFrame.copy();
		preTargetFrame.setZ(preTargetFrame.getZ()+40);			// 40 mm above
		move.PTPwithJTConds(preTargetFrame, relSpeed);
		moveLinRelWithCollisionDetection(0,0,-40); 								// 45 mm downwards
	}
	
	private void insertPin(Frame targetFrame) { 
		padLog("Inserting pin.");   
		Frame preTargetFrame = targetFrame.copy();
		preTargetFrame.setY(preTargetFrame.getY()+40);			// 40 mm infront
		move.PTPwithJTConds(preTargetFrame, relSpeed);
		moveLinRelWithCollisionDetection(0,-40,0); 								// 40 mm towards
	}
	
	private boolean checkInsertion(){							 
		padLog("Checking insertion.");
		mf.setRGB("GB");
		IMotionContainer motionCmd; 
		boolean checkPositive = false;
		JointTorqueCondition breakCondition1, breakCondition2, breakCondition3, breakCondition4, breakCondition5, breakCondition6, breakCondition7 ;					// for torque break condition
		breakCondition1 = new JointTorqueCondition(JointEnum.J1, -3, 3);	
		breakCondition2 = new JointTorqueCondition(JointEnum.J2, -3, 3);
		breakCondition3 = new JointTorqueCondition(JointEnum.J3, -3, 3);	
		breakCondition4 = new JointTorqueCondition(JointEnum.J4, -3, 3);
		breakCondition5 = new JointTorqueCondition(JointEnum.J5, -3, 3);	
		breakCondition6 = new JointTorqueCondition(JointEnum.J6, -3, 3);
		breakCondition7 = new JointTorqueCondition(JointEnum.J7, -3, 3);
		Frame lastCheckpoint = kiwa.getCurrentCartesianPosition(kiwa.getFlange());	// Save the location of center of the hole
		motionCmd = kiwa.move(linRel(-5, -5, 0).setJointVelocityRel(0.02).breakWhen(breakCondition1).breakWhen(breakCondition2).breakWhen(breakCondition3).breakWhen(breakCondition4).breakWhen(breakCondition5).breakWhen(breakCondition6).breakWhen(breakCondition7)); 
		info = motionCmd.getFiredBreakConditionInfo(); 

		if (info != null) {
			checkPositive = true;
			padLog("Check 1 successful");
			mf.setRGB("G");
			waitMillis(500); 
			kiwa.move(ptp(lastCheckpoint).setJointVelocityRel(0.02)); //Go back to the center of the hole
			waitMillis(500); 
			mf.setRGB("GB");
		}  
		if (checkPositive == false) {
			padLog("Check 1 unsuccessful");
			mf.setRGB("R");
			waitMillis(500);
			kiwa.move(ptp(lastCheckpoint).setJointVelocityRel(0.02)); //Go back to the center of the hole
			waitMillis(500); 
			mf.setRGB("R");
			return false;
		} else {
			motionCmd = kiwa.move(linRel(5, 5, 0).setJointVelocityRel(0.02).breakWhen(torqueBreakCondition1).breakWhen(torqueBreakCondition2).breakWhen(torqueBreakCondition3).breakWhen(torqueBreakCondition4).breakWhen(torqueBreakCondition5).breakWhen(torqueBreakCondition6).breakWhen(torqueBreakCondition7)); 
			info = motionCmd.getFiredBreakConditionInfo(); 
			if (info != null) {
				padLog("Check 2 successful");
				mf.setRGB("G");
				waitMillis(500); 
				kiwa.move(ptp(lastCheckpoint).setJointVelocityRel(0.02)); //Go back to the center of the hole
				waitMillis(500); 
				padLog("Insertion check successful");
				
				return true;
			}else{
				padLog("Check 2 unsuccessful");
				mf.setRGB("R");
				waitMillis(500);
				kiwa.move(ptp(lastCheckpoint).setJointVelocityRel(0.02)); //Go back to the center of the hole
				waitMillis(500); 
				mf.setRGB("R");
				return false;
			}
		}
	}

	private void moveLinRelWithCollisionDetection (double x, double y, double z){
		IMotionContainer motionCmd;
		Frame goToFrame, currentFrame;
		JointTorqueCondition breakCondition1, breakCondition2, breakCondition3, breakCondition4, breakCondition5, breakCondition6, breakCondition7 ;					// for torque break condition
		breakCondition1 = new JointTorqueCondition(JointEnum.J1, -3, 3);	
		breakCondition2 = new JointTorqueCondition(JointEnum.J2, -3, 3);
		breakCondition3 = new JointTorqueCondition(JointEnum.J3, -3, 3);	
		breakCondition4 = new JointTorqueCondition(JointEnum.J4, -3, 3);
		breakCondition5 = new JointTorqueCondition(JointEnum.J5, -3, 3);	
		breakCondition6 = new JointTorqueCondition(JointEnum.J6, -3, 3);
		breakCondition7 = new JointTorqueCondition(JointEnum.J7, -3, 3);
		IFiredConditionInfo info; 
		currentFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
		goToFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange()).setX(currentFrame.getX()+x).setY(currentFrame.getY()+y).setZ(currentFrame.getZ()+z);
		waitMillis(500); 
		motionCmd = kiwa.move(lin(goToFrame).setJointVelocityRel(0.05).breakWhen(breakCondition1).breakWhen(breakCondition2).breakWhen(breakCondition3).breakWhen(breakCondition4).breakWhen(breakCondition5).breakWhen(breakCondition6).breakWhen(breakCondition7));  
		info = motionCmd.getFiredBreakConditionInfo(); 
		while (info != null){ 
			mf.setRGB("R");

			currentFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
			if (Math.abs(currentFrame.getX() - goToFrame.getX()) < 7 && Math.abs(currentFrame.getY() - goToFrame.getY()) < 7 && Math.abs(currentFrame.getZ() - goToFrame.getZ()) < 7) {
				padLog("Pin/Hole found by Force detection.");	
				mf.setRGB("B");
				waitMillis(1000); 
				break;
			} else {
				kiwa.move(linRel(0, 0, -30).setJointVelocityRel(0.05));		// go back after collision 
				waitMillis(500);  
				 
					while(!mfio.getUserButton()){
						waitMillis(20);
					} 
				
				mf.setRGB("G");
				waitMillis(500);			// This is necessary, OW. the next breakConditions are again triggered 
				motionCmd = kiwa.move(lin(goToFrame).setJointVelocityRel(0.05).breakWhen(breakCondition1).breakWhen(breakCondition2).breakWhen(breakCondition3).breakWhen(breakCondition4).breakWhen(breakCondition5).breakWhen(breakCondition6).breakWhen(breakCondition7)); 
				info = motionCmd.getFiredBreakConditionInfo();
			}
		}
//		padLog("Position reached.");
		mf.setRGB("G"); 
	}
	
	private void configPadKeysGENERAL() { 					// TEACH buttons						
		IUserKeyListener padKeysListener = new IUserKeyListener() {
			@Override public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				if (event == UserKeyEvent.KeyDown) {
					Frame targetFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
					switch (key.getSlot()) {
						case 0:  						// KEY - TEACH MODE
							if (state == States.state_loop) {
								state = States.state_home;
								endLoopRoutine = true;
							} else padLog("Key not available in this mode.");
							break;
						case 1: 						// KEY - DELETE PREVIOUS
							if (state == States.state_teach) {
								if (frameList.getLast().hasAdditionalParameter("PICK")) plc.openGripper();	
								else if (frameList.getLast().hasAdditionalParameter("PLACE")) plc.closeGripper();	
								frameList.removeLast();
							} else padLog("Key not available in this mode.");
							break;
						case 2:  						// KEY - SET SPEED
							relSpeed = pad.askSpeed();
							break;
						case 3:							// KEY - SET TORQUE
							maxTorque = pad.askTorque();
							move.setJTConds(maxTorque);
							break;
					}
				}
			}
		};
		pad.keyBarSetup(padKeysListener, "GENERAL", "Teach", "Delete Previous", "Speed", "Torque");
	}
	
	private void configPadKeysCONSTRAIN() {
		IUserKeyListener padKeysListener = new IUserKeyListener() {
			@Override public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				if (event == UserKeyEvent.KeyDown) {
					Frame targetFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
					switch (key.getSlot()) {
						case 0:  						// KEY - CONSTRAIN POSITION
							if (state == States.state_teach) {

							} else padLog("Key not available in this mode.");
							break;
						case 1: 						// KEY - CONSTRAIN ORIENTATION
							if (state == States.state_teach) {
				
							} else padLog("Key not available in this mode.");
							break;
						case 2:
							break;
						case 3:
							break;
					}
				}
			}
		};
		pad.keyBarSetup(padKeysListener, "CONSTRAIN", "Position", "Orientation", "Approach", "");
	}
}