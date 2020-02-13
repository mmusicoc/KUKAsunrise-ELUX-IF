package application.Training;

import static utils.Utils.*;
import utils.HandlerMFio;
import utils.FrameList;
import javax.inject.Inject; 
import javax.inject.Named;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import static com.kuka.roboticsAPI.motionModel.HRCMotions.*;
import com.kuka.roboticsAPI.conditionModel.ICondition;
import com.kuka.roboticsAPI.conditionModel.JointTorqueCondition;
import com.kuka.roboticsAPI.deviceModel.JointEnum;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.executionModel.IFiredConditionInfo;
import com.kuka.roboticsAPI.geometricModel.CartDOF; 
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.Workpiece;
import com.kuka.roboticsAPI.motionModel.*;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;
import com.kuka.roboticsAPI.motionModel.controlModeModel.HandGuidingControlMode;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKey;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyBar;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyListener;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyAlignment;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyEvent;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyLED;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyLEDSize;
import com.kuka.task.ITaskLogger;
import com.sun.java.swing.plaf.nimbus.ButtonPainter;
import com.sun.org.apache.bcel.internal.generic.NEW;
import com.sun.org.apache.bcel.internal.generic.Select;

public class Tr4a_TeachPickAndPlace extends RoboticsAPIApplication {
	@Inject private LBR 				kiwa;
	@Inject private Plc_inputIOGroup 	plcin;
	@Inject private Plc_outputIOGroup 	plcout;
	@Inject private MediaFlangeIOGroup 	mfio;
	@Inject	@Named("Pinza") 		private Tool 		Gripper;
	@Inject @Named("VacuumBody") 	private Workpiece 	VacuumBody;
	
	// Handlers == custom modularizing libraries
	private HandlerMFio MFio = new HandlerMFio(mfio);

	private enum States {state_home, state_teach, state_loop, state_pause};
	private States state; 
	private double defSpeed = 0.25;
	private String homeFramePath;
	private boolean exitForLoop = false;
	private FrameList frameList = new FrameList();

	private CartesianImpedanceControlMode softMode = new CartesianImpedanceControlMode();  	// for stiffless handguiding
	private CartesianImpedanceControlMode stiffMode = new CartesianImpedanceControlMode();  // for gesture control
	private PositionHold posHold = new PositionHold(softMode, -1, null);  
	private IMotionContainer posHoldMotion;			// Motion container for position hold
	private IMotionContainer torqueBreakMotion;		// Motion container with torque break condition
	private JointTorqueCondition JTCond[] = new JointTorqueCondition[8];
	private ICondition JTConds;

	@Override
	public void initialize() {
		double maxTorque;
		logPad("Initializing...");
		Gripper.attachTo(kiwa.getFlange());
		enableTeachButtons();
		state = States.state_home;
		homeFramePath = "/_HOME/_2_Teach_CENTRAL";
				
		// Setting the stiffness in HandGuiding mode
		softMode.parametrize(CartDOF.TRANSL).setStiffness(0.1).setDamping(1);		// HandGuiding
		softMode.parametrize(CartDOF.ROT).setStiffness(0.1).setDamping(1);  
		stiffMode.parametrize(CartDOF.TRANSL).setStiffness(5000).setDamping(1);		// GestureControl
		stiffMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(1); 

		// Setting joint torque break condition
		int promptAns = 2;//padQuestion("Set max External Torque ", "5 Nm", "10 Nm", "15 Nm", "20 Nm");  
		switch (promptAns) {
			case 0: maxTorque = 5.0; break;
			case 1: maxTorque = 10.0; break;
			case 2: maxTorque = 15.0; break;
			case 3: maxTorque = 20.0; break;
			default: maxTorque = 10.0; break;
		}
		logPad("Max Axis Torque set to " + maxTorque + " Nm.");		

		JTCond[1] = new JointTorqueCondition(JointEnum.J1, -maxTorque, maxTorque);	
		JTCond[2] = new JointTorqueCondition(JointEnum.J2, -maxTorque, maxTorque);
		JTCond[3] = new JointTorqueCondition(JointEnum.J3, -maxTorque, maxTorque);	
		JTCond[4] = new JointTorqueCondition(JointEnum.J4, -maxTorque, maxTorque);
		JTCond[5] = new JointTorqueCondition(JointEnum.J5, -maxTorque, maxTorque);	
		JTCond[6] = new JointTorqueCondition(JointEnum.J6, -maxTorque, maxTorque);
		JTCond[7] = new JointTorqueCondition(JointEnum.J7, -maxTorque, maxTorque);
		JTConds = JTCond[1].or(JTCond[2]).or(JTCond[3]).or(JTCond[4]).or(JTCond[5]).or(JTCond[6]).or(JTCond[7]);
	}

	@Override
	public void run() {
		while (true) {
			switch (state) {
				case state_home:
					MFio.setRGB("G");
					openGripper(true);
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
					exitForLoop = false;
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
		MFio.setRGB("B");
		MFio.waitUserButton();
		MFio.blinkRGB("G", 500);
		logPad("Start hand guiding."); 
		posHoldMotion = kiwa.moveAsync(posHold);
		
		teachLoop:
		while (true) {
			if (mfio.getUserButton()) {
				Frame newFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
				btnInput = checkBtnInput();				// Run the button press check
				switch (btnInput) {
					case 10: 							// Exit handguiding phase
						if (frameList.size() > 1) {
							logPad("Exiting handguiding teaching mode...");
							posHoldMotion.cancel();
							MFio.setRGB("G");
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
	
	private void newCloseFrame(Frame newFrame){
		newFrame.setAdditionalParameter("Close Gripper", 1);
		MFio.blinkRGB("GB", 500);
		closeGripper();
		/*
		if (plcin.getPinza_Holding()) {
			posHoldMotion.cancel(); 
			VacuumBody.attachTo(Gripper.getDefaultMotionFrame()); 
			System.out.println("Workpiece Attached.");
			posHoldMotion = kiwa.moveAsync(posHold); 
		} else { 
			System.out.println("Workpiece Not Attached.");
		}
		*/
	}
	
	private void newOpenFrame(Frame newFrame){
		newFrame.setAdditionalParameter("Open Gripper", 1); 
		/*
		if (plcin.getPinza_Holding()) {
			posHoldMotion.cancel(); 
			VacuumBody.detach();
			logPad("Workpiece detached."); 
			posHoldMotion = kiwa.moveAsync(posHold);
		}
		*/
		MFio.blinkRGB("GB", 500);
		openGripper();
	}
	
	private void loopRoutine(boolean log){
		MFio.setRGB("G");
		logPad("Loop backwards.");
		for (int j = frameList.size()-1; j >= 0; j--) { 		// loop for going the opposite direction
			if (exitForLoop) {
				exitForLoop = false;
				break;
			}
			if (log) logPad("Going to Frame "+ j +".");
			movePTPwithJTConds(frameList.get(j));
			if (frameList.get(j).hasAdditionalParameter("Close Gripper")) {	// going the opposite direction so opposite command
				System.out.println("Opening gripper.");
				plcout.setPinza_Chiudi(false);
				plcout.setPinza_Apri(true); 
				while (true) {
					if (plcin.getPinza_Idle()) {
						VacuumBody.detach();
						break;
					}
				}
			}
			if (frameList.get(j).hasAdditionalParameter("Open Gripper")) { // going the opposite direction so opposite command
				while (true) {
					if (checkComponent()) {
						MFio.setRGB(false, true, false);  
						break;
					} else {
						System.out.println("Waiting for gesture input. ");
						kiwa.move(positionHold(stiffMode, -1, null).breakWhen(JTConds)); 
					}
					waitMillis(250);
				}
				System.out.println("Closing gripper.");
				plcout.setPinza_Apri(false);
				plcout.setPinza_Chiudi(true);
				while (true) {
					if (plcin.getPinza_NoPart() || plcin.getPinza_Holding()) {
						if (plcin.getPinza_Holding()) {
							//VacuumBody.attachTo(Gripper.getDefaultMotionFrame());
							System.out.println("Workpiece Not Attached");
						}
						break;
					}
				}
			}
		}
		logPad("Loop forward");
		for (int i = 0; i < frameList.size(); i++) { 				// last saved frame is pointCounter-1
			if (exitForLoop) {
				exitForLoop = false;
				break;
			}
			if (log) logPad("Going to Frame "+ i +".");
			movePTPwithJTConds(frameList.get(i));
			if (frameList.get(i).hasAdditionalParameter("Open Gripper")) {
				System.out.println("Opening gripper.");
				plcout.setPinza_Chiudi(false);
				plcout.setPinza_Apri(true); 
				while (true) {
					if (plcin.getPinza_Idle()) {
						VacuumBody.detach();
						break;
					}
				}
			}
			if (frameList.get(i).hasAdditionalParameter("Close Gripper")) {
				while (true) {
					if (checkComponent()) {
						break;
					} else {
						System.out.println("Waiting for gesture input. ");
						kiwa.move(positionHold(stiffMode, -1, null).breakWhen(JTConds));
						MFio.setRGB("G");
					}
					waitMillis(250);
				}

				System.out.println("Closing gripper.");
				plcout.setPinza_Apri(false);
				plcout.setPinza_Chiudi(true);
				while (true) {
					if (plcin.getPinza_NoPart() || plcin.getPinza_Holding()) {
						if (plcin.getPinza_Holding()) {
						//VacuumBody.attachTo(Gripper.getDefaultMotionFrame());
						System.out.println("Workpiece Not Attached");
						}
						break;
					}
				}
			}
		} 
	}
	
	private void enableTeachButtons() { 										// TEACH buttons						
		IUserKeyBar padKeyBarTeach = getApplicationUI().createUserKeyBar("TEACH");
		IUserKeyListener padKeyListener = new IUserKeyListener() {
			boolean padKeyPressed = false;
			@Override
			public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				if (!padKeyPressed) {
					padKeyPressed = true;
					Frame newFrame;
					switch (key.getSlot()) {
						case 0: 						// KEY - DELETE PREVIOUS
							if (state == States.state_teach) {
								if (frameList.getLast().hasAdditionalParameter("Close Gripper")) openGripper();				
								frameList.removeLast();
							}
							break;
						case 1:  						// KEY - TEACH MODE			 
							state = States.state_home;
							exitForLoop = true;
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

	private void movePTPwithJTConds (Frame nextFrame){		// overloading for taught points
		IFiredConditionInfo JTBreak;
		do {
			torqueBreakMotion = kiwa.move(ptp(nextFrame).setJointVelocityRel(defSpeed).breakWhen(JTConds)); 
			JTBreak = torqueBreakMotion.getFiredBreakConditionInfo();
			if (JTBreak != null) {
				logPad("Collision detected!"); 
				boolean prevRGB[] = MFio.getRGB();
				MFio.setRGB("R");
				MFio.waitUserButton();
				posHoldMotion = kiwa.moveAsync(posHold);	// Enable unpinching
				MFio.waitUserButton();
				posHoldMotion.cancel();
				movePTPwithJTConds(nextFrame);
				MFio.setRGB(prevRGB);
			}
		} while (JTBreak != null);
	}
	
	private void movePTPwithJTConds (String framePath){
		ObjectFrame nextFrame = getApplicationData().getFrame(framePath);
		movePTPwithJTConds(nextFrame.copyWithRedundancy());
	}
	
	private boolean checkComponent(){
		IFiredConditionInfo JTBreak;
		logPad("Checking component...");
		torqueBreakMotion = kiwa.move(linRel(0, 0, 25).setJointVelocityRel(0.01).breakWhen(JTConds)); 
		JTBreak = torqueBreakMotion.getFiredBreakConditionInfo();
		if (JTBreak != null) {
			System.out.println("Component detected. " ); 
			MFio.blinkRGB("GB", 800);
			kiwa.move(linRel(0, 0, -25).setJointVelocityRel(defSpeed));
			return true;
		} else {
			System.out.println("No components detected. " ); 
			MFio.setRGB("RB");
			kiwa.move(linRel(0, 0, -25).setJointVelocityRel(defSpeed));
			return false;
		} 
	}

	private int checkBtnInput(){						// determine user button input
		boolean[] prevRGB = MFio.getRGB();
		int timeCount = 0;
		int pressCountShort = 0;
		int pressCountLong = 0;
		MFio.setRGB("GB");
		
		outerLoop:
		while(true) {
			while (true) {								// While pressed
				if (!mfio.getUserButton()) {			// Button unpressed
					MFio.setRGB(prevRGB);
					if (timeCount < 9) pressCountShort += 1;
					else pressCountLong += 1;
					timeCount = 0;
					break;
				}
				waitMillis(50);
				timeCount += 1;
				if (timeCount > 9) MFio.setRGB("R");
			}
			while (true) {								// While unpressed
				if (timeCount > 9) break outerLoop;		// Timeout, finished interaction
				if (mfio.getUserButton()) {				// new action detected
					MFio.setRGB("GB");
					timeCount = 0;
					break;
				}
				waitMillis(50);
				timeCount += 1;
			}
		}
		MFio.setRGB(prevRGB);
		logPad("#Short = " + pressCountShort + ", #Long = " + pressCountLong);
		return (pressCountLong * 10 + pressCountShort);
	}
	
	/***************************************************************************
	* STANDARD METHODS BY mario.musico@electrolux.com <p>
	***************************************************************************/
	
	public void openGripper(int millis) {
		plcout.setPinza_Chiudi(false);
		waitMillis(10);
		plcout.setPinza_Apri(true);
		logPad("Opening gripper");
		waitMillis(millis);
	}
	
	public void openGripper(boolean async) {
		if (async) openGripper(10);
		else openGripper(2000);			 // Wait for the gripper to close before continuing with the next command
	}
	
	public void openGripper() {
		openGripper(false);
	}
	
	public void closeGripper() {
		plcout.setPinza_Apri(false);
		waitMillis(10);
		plcout.setPinza_Chiudi(true);
		logPad("Closing gripper");
		waitMillis(2000);
	}
	
	public int padQuestion(String question, String ans1, String ans2, String ans3, String ans4){
		return getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, question, ans1, ans2, ans3, ans4); 
	}
}