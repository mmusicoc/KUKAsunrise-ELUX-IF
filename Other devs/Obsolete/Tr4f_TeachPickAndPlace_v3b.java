package application.Training;

import static utils.Utils.*;
import utils.HandlerMFio;
import utils.HandlerPLCio;
import utils.HandlerPad;
import utils.FrameList;

import javax.inject.Inject; 
import javax.inject.Named;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.conditionModel.ICondition;
import com.kuka.roboticsAPI.conditionModel.JointTorqueCondition;
import com.kuka.roboticsAPI.deviceModel.JointEnum;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.executionModel.IFiredConditionInfo;
import com.kuka.roboticsAPI.geometricModel.CartDOF; 
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.geometricModel.Tool;
//import com.kuka.roboticsAPI.geometricModel.Workpiece;
import com.kuka.roboticsAPI.motionModel.*;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKey;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyBar;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyListener;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyAlignment;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyEvent;

public class Tr4_TeachPickAndPlace extends RoboticsAPIApplication {
	@Inject private LBR 				kiwa;
	@Inject private Plc_inputIOGroup 	plcin;
	@Inject private Plc_outputIOGroup 	plcout;
	@Inject private MediaFlangeIOGroup 	mfio;
	@Inject	@Named("Pinza") 		private Tool 		Gripper;
	// @Inject @Named("VacuumBody") 	private Workpiece 	VacuumBody;
	// Handlers == custom modularizing libraries
	@Inject private HandlerPad pad = new HandlerPad();
	@Inject private HandlerMFio	mf = new HandlerMFio(mfio);
	@Inject private HandlerPLCio plc = new HandlerPLCio(plcin, plcout);
	
	private FrameList frameList = new FrameList();
	private enum States {state_home, state_teach, state_loop, state_pause};
	private States state; 
	private double defSpeed = 0.25;
	private String homeFramePath;
	private boolean endRoutine = false;
	private boolean workpieceGripped = false;
	
	private CartesianImpedanceControlMode softMode = new CartesianImpedanceControlMode();  	// for stiffless handguiding
	private CartesianImpedanceControlMode stiffMode = new CartesianImpedanceControlMode();  // for gesture control
	private PositionHold posHold = new PositionHold(softMode, -1, null);  
	private IMotionContainer posHoldMotion;			// Motion container for position hold
	private IMotionContainer torqueBreakMotion;		// Motion container with torque break condition
	private ICondition JTConds;

	@Override
	public void initialize() {
		double maxTorque;
		logPad("Initializing...");
		Gripper.attachTo(kiwa.getFlange());
		configTEACHpadKeys();
		state = States.state_home;
		homeFramePath = "/_HOME/_2_Teach_CENTRAL";
				
		// Setting the stiffness in HandGuiding mode
		softMode.parametrize(CartDOF.TRANSL).setStiffness(0.1).setDamping(1);		// HandGuiding
		softMode.parametrize(CartDOF.ROT).setStiffness(0.1).setDamping(1);  
		stiffMode.parametrize(CartDOF.TRANSL).setStiffness(5000).setDamping(1);		// GestureControl
		stiffMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(1); 

		// Setting joint torque break condition
		int promptAns = pad.question("Set max External Torque ", "5 Nm", "10 Nm", "15 Nm", "20 Nm");  
		switch (promptAns) {
			case 0: maxTorque = 5.0; break;
			case 1: maxTorque = 10.0; break;
			case 2: maxTorque = 15.0; break;
			case 3: maxTorque = 20.0; break;
			default: maxTorque = 10.0; break;
		}
		setupJTconds(maxTorque);
	}

	@Override
	public void run() {
		while (true) {
			switch (state) {
				case state_home:
					mf.setRGB("G");
					plc.askOpenAsync();
					logPad("Going home.");
					movePTPwithJTConds(homeFramePath);
					state = States.state_teach;
					break;
				case state_teach:
					frameList.free();
					handGuidingPhase(); 
					state = States.state_loop;
					break;
				case state_loop:
					endRoutine = false;
					loopRoutine(false);
					break;
				case state_pause:
					waitMillis(50);
					break;
				default:
					logPad("Default case.");
					state = States.state_home;
					break;
			}
		}
	}
	
	private void handGuidingPhase(){
		int btnInput;
		mf.setRGB("B");
		mf.waitUserButton();
		mf.blinkRGB("G", 500);
		logPad("Start hand guiding."); 
		posHoldMotion = kiwa.moveAsync(posHold);
		
		teachLoop:
		while (true) {
			if (mf.getUserButton()) {
				Frame newFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
				btnInput = mf.checkBtnInput();				// Run the button press check
				switch (btnInput) {
					case 10: 							// Exit handguiding phase
						if (frameList.size() > 1) {
							logPad("Exiting handguiding teaching mode...");
							posHoldMotion.cancel();
							mf.setRGB("G");
							movePTPwithJTConds(homeFramePath);
							break teachLoop;
						}else {
						logPad("Record at least 2 positions to start running.");
						waitMillis(200);
						}
						break;
					case 01: 					// Record current position
						frameList.add(newFrame, true);
						break;
					case 02:					// Record current position & Close Gripper
						newCloseFrame(newFrame);
						frameList.add(newFrame, true);
						break;
					case 03:					// Record current position & Open Gripper
						newOpenFrame(newFrame);
						frameList.add(newFrame, true);
						break;
					default:
						logPad("Command not valid, try again");
						continue teachLoop;
				}
			}
		} 
	}
	
	private void loopRoutine(boolean log){
		mf.setRGB("G");
		logPad("Loop backwards.");
		for (int j = frameList.size()-1; j >= 0; j--) { 		// loop for going the opposite direction
			if (endRoutine) {
				endRoutine = false;
				break;
			}
			if (log) logPad("Going to Frame "+ j +".");
			movePTPwithJTConds(frameList.get(j));
			if (frameList.get(j).hasAdditionalParameter("Close Gripper")) {	// going the opposite direction so opposite command
				openGripperCheck(false);
			}
			if (frameList.get(j).hasAdditionalParameter("Open Gripper")) { // going the opposite direction so opposite command
				checkComponent(25);
				closeGripperCheck(false);
			}
		}
		
		logPad("Loop forward");
		for (int i = 0; i < frameList.size(); i++) {
			if (endRoutine) {
				endRoutine = false;
				break;
			}
			if (log) logPad("Going to Frame "+ i +".");
			movePTPwithJTConds(frameList.get(i));
			if (frameList.get(i).hasAdditionalParameter("Open Gripper")) {
				openGripperCheck(false);
			}
			if (frameList.get(i).hasAdditionalParameter("Close Gripper")) {
				checkComponent(25);
				closeGripperCheck(false);
			}
		} 
	}
	
	private void closeGripperCheck(boolean isPosHold) {
		plc.closeGripperAsync();
		while (!plcin.getPinza_NoPart() & !plcin.getPinza_Holding()) {
			waitMillis(50);
		}
		if (plcin.getPinza_Holding()){
			logPad("Workpiece gripped");
			workpieceGripped = true;
			if (isPosHold) posHoldMotion.cancel();
		//	VacuumBody.attachTo(Gripper.getDefaultMotionFrame()); 
			if (isPosHold) posHoldMotion = kiwa.moveAsync(posHold);
		} else {
			logPad("Workpiece NOT gripped");
		}
	}
	
	private void openGripperCheck(boolean isPosHold) {
		plc.openGripperAsync();
		if (!isPosHold) waitMillis(2000);
		if (workpieceGripped) {
			workpieceGripped = false;
			if (isPosHold) posHoldMotion.cancel();
			logPad("Workpiece released");
		//	VacuumBody.detach(); 
			if (isPosHold) posHoldMotion = kiwa.moveAsync(posHold);
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
	
	private void configTEACHpadKeys() { 					// TEACH buttons						
		IUserKeyBar padKeyBarTeach = getApplicationUI().createUserKeyBar("TEACH");
		IUserKeyListener padKeyListener = new IUserKeyListener() {
			boolean padKeyPressed = false;
			@Override
			public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				if (state == States.state_teach) {
					if (!padKeyPressed) {
						padKeyPressed = true;
						Frame newFrame;
						switch (key.getSlot()) {
							case 0: 						// KEY - DELETE PREVIOUS
								if (frameList.getLast().hasAdditionalParameter("Close Gripper")) plc.openGripper();	
								else if (frameList.getLast().hasAdditionalParameter("Open Gripper")) plc.closeGripper();	
								frameList.removeLast();
								break;
							case 1:  						// KEY - TEACH MODE			 
								state = States.state_home;
								endRoutine = true;
								break;
							case 2:							// KEY - OPEN GRIPPER 
								newFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
								newOpenFrame(newFrame);
								frameList.add(newFrame, true);
								break;
							case 3:  						//KEY - CLOSE GRIPPER
								newFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
								newCloseFrame(newFrame);
								frameList.add(newFrame, true);
							default:
								break;
						}
					} else {
						padKeyPressed = false;
					}
				}
			}
		};

		IUserKey padKey1 = padKeyBarTeach.addUserKey(0, padKeyListener, true);
		IUserKey padKey2 = padKeyBarTeach.addUserKey(1, padKeyListener, true);
		IUserKey padKey3 = padKeyBarTeach.addUserKey(2, padKeyListener, true);
		IUserKey padKey4 = padKeyBarTeach.addUserKey(3, padKeyListener, true);
		padKey1.setText(UserKeyAlignment.TopMiddle, "Delete Previous"); 
		padKey2.setText(UserKeyAlignment.TopMiddle, "TEACH"); 
		padKey3.setText(UserKeyAlignment.TopMiddle, "Open Gripper");
		padKey4.setText(UserKeyAlignment.TopMiddle, "Close Gripper");
		padKeyBarTeach.publish();
	}
	
	private void setupJTconds (double maxTorque){
		JointTorqueCondition JTCond[] = new JointTorqueCondition[8];
		JTCond[1] = new JointTorqueCondition(JointEnum.J1, -maxTorque, maxTorque);	
		JTCond[2] = new JointTorqueCondition(JointEnum.J2, -maxTorque, maxTorque);
		JTCond[3] = new JointTorqueCondition(JointEnum.J3, -maxTorque, maxTorque);	
		JTCond[4] = new JointTorqueCondition(JointEnum.J4, -maxTorque, maxTorque);
		JTCond[5] = new JointTorqueCondition(JointEnum.J5, -maxTorque, maxTorque);	
		JTCond[6] = new JointTorqueCondition(JointEnum.J6, -maxTorque, maxTorque);
		JTCond[7] = new JointTorqueCondition(JointEnum.J7, -maxTorque, maxTorque);
		JTConds = JTCond[1].or(JTCond[2]).or(JTCond[3]).or(JTCond[4]).or(JTCond[5]).or(JTCond[6]).or(JTCond[7]);
		logPad("Max Axis Torque set to " + maxTorque + " Nm.");
	}

	private void movePTPwithJTConds (Frame nextFrame){		// overloading for taught points
		IFiredConditionInfo JTBreak;
		do {
			torqueBreakMotion = kiwa.move(ptp(nextFrame).setJointVelocityRel(defSpeed).breakWhen(JTConds)); 
			JTBreak = torqueBreakMotion.getFiredBreakConditionInfo();
			if (JTBreak != null) {
				logPad("Collision detected!"); 
				mf.saveRGB();
				mf.setRGB("R");
				mf.waitUserButton();
				posHoldMotion = kiwa.moveAsync(posHold);	// Enable unpinching
				mf.waitUserButton();
				posHoldMotion.cancel();
				movePTPwithJTConds(nextFrame);
				mf.resetRGB();
			}
		} while (JTBreak != null);
	}
	
	private void movePTPwithJTConds (String framePath){
		ObjectFrame nextFrame = getApplicationData().getFrame(framePath);
		movePTPwithJTConds(nextFrame.copyWithRedundancy());
	}
	
	private void checkComponent(int probeDist){
		boolean pieceFound = false;
		IFiredConditionInfo JTBreak;
		logPad("Checking component...");
		do {
			torqueBreakMotion = kiwa.move(linRel(0, 0, probeDist).setJointVelocityRel(0.01).breakWhen(JTConds)); 
			JTBreak = torqueBreakMotion.getFiredBreakConditionInfo();
			if (JTBreak != null) {
				System.out.println("Component detected. " ); 
				mf.blinkRGB("GB", 800);
				kiwa.move(linRel(0, 0, -probeDist).setJointVelocityRel(defSpeed));
				pieceFound = true;
			} else {
				mf.setRGB("RB");
				System.out.println("No components detected, Reposition the workpiece correctly and push the cobot (gesture control)." );
				kiwa.move(linRel(0, 0, -probeDist).setJointVelocityRel(defSpeed));
				kiwa.move(positionHold(stiffMode, -1, null).breakWhen(JTConds));
			}
		} while (!pieceFound);
		waitMillis(250);
	}
}