package application;

import static utils.Utils.*;
import utils.FrameWrapper;
import javax.inject.Inject; 
import javax.inject.Named;
import com.kuka.common.ThreadUtil;
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

public class Tr_PickPlace_good_v2 extends RoboticsAPIApplication {
	@Inject private LBR 				kiwa;
	@Inject private Plc_inputIOGroup 	plcin;
	@Inject private Plc_outputIOGroup 	plcout;
	@Inject private MediaFlangeIOGroup 	mfio;
	@Inject	@Named("Pinza") 		private Tool 		Gripper;
	@Inject @Named("VacuumBody") 	private Workpiece 	VacuumBody;

	private enum States {state_home, state_teach, state_loop, state_pause};
	private States state; 
	// private boolean padKeyPressed = false;
	//private double maxTorque = 10.0;
	private double defSpeed = 0.15;
	private String homeFramePath;
	private boolean exitForLoop = false;
	private FrameWrapper frameList = new FrameWrapper();

	private CartesianImpedanceControlMode softMode = new CartesianImpedanceControlMode();  	// for stiffless handguiding
	private CartesianImpedanceControlMode stiffMode = new CartesianImpedanceControlMode();  // for gesture control
	private PositionHold posHold = new PositionHold(softMode, -1, null);  
	private IMotionContainer posHoldMotion;			// Motion container for position hold
	private ImotionContainer torqueBreakMotion;		// Motion container with torque break condition
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
		int promptAns = padQuestion("Set max External Torque ", "5 Nm", "10 Nm", "15 Nm", "20 Nm");  
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
					setRGB("G");
					openGripper(true);
					logPad("Going home.");
					movePTPwithJTConds(homeFramePath);
					state = States.state_teach;
					break;
				case state_teach:
					frameList.Free();
					logPad("Start hand guiding."); 
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
		setRGB("B");
		waitUserButton();
		blinkRGB("G", 500);
		posHoldMotion = kiwa.moveAsync(posHold); 
		while (true) {
			if (mfio.getUserButton()) {
				Frame newFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
				btnInput = checkBtnInput();						// Run the button press check
				if (btnInput == 10) { 							// Exit myHandGuiding
					if (frameList.GetCounter() > 1) {
						logPad("Exiting handguiding teaching mode...");
						posHoldMotion.cancel();
						setRGB("G");
						movePTPwithJTConds(homeFramePath);
						break;
					}else {
						logPad("Record at least 2 positions to start running.");
						waitMillis(200);
					}
				} else if (btnInput == 01) { 					// Record current position
					frameList.Add(newFrame);
					logPad("Added Frame #" + frameList.GetCounter() + " : " + frameList.Last().toString());
				} else if (btnInput == 02) {					// Record current position & Open Gripper
					newFrame.setAdditionalParameter("Open Gripper", 1); 
					frameList.Add(newFrame);
					logPad("Added Frame #" + frameList.GetCounter() + " : " + frameList.Last().toString()); 
					/*
					if (plcin.getPinza_Holding()) {
						posHoldMotion.cancel(); 
						VacuumBody.detach();
						logPad("Workpiece detached."); 
						posHoldMotion = kiwa.moveAsync(posHold);
					}
					*/
					openGripper();
				} else if (btnInput == 03) {					// Record current position &  Close Gripper
					newFrame.setAdditionalParameter("Close Gripper", 1); 
					frameList.Add(newFrame);
					logPad("Added Frame #" + frameList.GetCounter()+" : " + frameList.Last().toString());
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
				} else {
					logPad("Command not valid, try again");
				}
			}
		} 
	}
	
	private void loopRoutine(boolean log){
		setRGB("G");
		logPad("Loop backwards.");
		for (int j = frameList.GetCounter()-1; j >= 0; j--) { 		// loop for going the opposite direction
			if (exitForLoop) {
				exitForLoop = false;
				break;
			}
			if (log) logPad("Going to Frame "+ j +".");
			movePTPwithJTConds(frameList.GetFrame(j));
			if (frameList.GetFrame(j).hasAdditionalParameter("Close Gripper")) {	// going the opposite direction so opposite command
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
			if (frameList.GetFrame(j).hasAdditionalParameter("Open Gripper")) { // going the opposite direction so opposite command
				while (true) {
					if (checkComponent()) {
						setRGB(false, true, false);  
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
		for (int i = 0; i < frameList.GetCounter(); i++) { 				// last saved frame is pointCounter-1
			if (exitForLoop) {
				exitForLoop = false;
				break;
			}
			if (log) logPad("Going to Frame "+ i +".");
			movePTPwithJTConds(frameList.GetFrame(i));
			if (frameList.GetFrame(i).hasAdditionalParameter("Open Gripper")) {
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
			if (frameList.GetFrame(i).hasAdditionalParameter("Close Gripper")) {
				while (true) {
					if (checkComponent()) {
						break;
					} else {
						System.out.println("Waiting for gesture input. ");
						kiwa.move(positionHold(stiffMode, -1, null).breakWhen(JTConds));
						setRGB("G");
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
		boolean padKeyPressed = false;
		IUserKeyBar teachKeyBar = getApplicationUI().createUserKeyBar("TEACH");
		IUserKeyListener listener2 = new IUserKeyListener() {
			@Override
			public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				if (key.getSlot() == 0) { 						//KEY - DELETE PREVIOUS

					if (!padKeyPressed && state == States.state_teach) {

						if (frameList.Last().hasAdditionalParameter("Close Gripper")) {
							plcout.setPinza_Chiudi(false);
							plcout.setPinza_Apri(true);
						}					
						frameList.deleteLastFrame(); 

						padKeyPressed = true;	
					} else if (padKeyPressed) {
						padKeyPressed = false; 
					}
				}
				if (key.getSlot() == 1) {  						//KEY - TEACH MODE			 
					state = States.state_home;
					exitForLoop = true;
				}
				
				if (key.getSlot() == 2) {   					//KEY - OPEN GRIPPER 
					if (!padKeyPressed) {
						Frame tempFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
						tempFrame.setAdditionalParameter("Open Gripper", 1); 
						frameList.Add(tempFrame);
						System.out.println("Added Frame "+frameList.GetCounter()+" : " + frameList.Last().toString());

						setRGB("OFF");
						waitMillis(150);
						setRGB("G");
						waitMillis(150);
						setRGB("GB");

						posHoldMotion.cancel(); 
						VacuumBody.detach();
						System.out.println("Workpiece Detached."); 
						posHoldMotion = kiwa.moveAsync(posHold); 
						openGripper();						 
						padKeyPressed = true;	
					} else if (padKeyPressed) {
						padKeyPressed = false; 
					}

				}
				if (key.getSlot() == 3) {   					//KEY - CLOSE GRIPPER
					if (!padKeyPressed) {
						Frame tempFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
						tempFrame.setAdditionalParameter("Close Gripper", 1); 
						frameList.Add(tempFrame);
						System.out.println("Added Frame "+frameList.GetCounter()+" : " + frameList.Last().toString());


						setRGB("OFF");
						waitMillis(150);
						setRGB("G");
						waitMillis(150);
						setRGB("GB");

						closeGripper();
						if (plcin.getPinza_Holding()) {
							posHoldMotion.cancel(); 
							//							VacuumBody.attachTo(Gripper.getDefaultMotionFrame()); 
							System.out.println("Workpiece Not Attached.");
							posHoldMotion = kiwa.moveAsync(posHold); 
						}else { 
							System.out.println("Workpiece Not Attached.");
						}

						//	pointCounter++;
						padKeyPressed = true;	
					} else if (padKeyPressed) {
						padKeyPressed = false; 
					}
				}
			}
		};

		IUserKey button0 = teachKeyBar.addUserKey(0, listener2, true);
		IUserKey button1 = teachKeyBar.addUserKey(1, listener2, true);
		IUserKey button2 = teachKeyBar.addUserKey(2, listener2, true);
		IUserKey button3 = teachKeyBar.addUserKey(3, listener2, true);
		button0.setText(UserKeyAlignment.TopMiddle, "Delete Previous"); 
		button1.setText(UserKeyAlignment.TopMiddle, "TEACH"); 
		button2.setText(UserKeyAlignment.TopMiddle, "Open Gripper");
		button3.setText(UserKeyAlignment.TopMiddle, "Close Gripper");
		teachKeyBar.publish();
	}

	private void movePTPwithJTConds (Frame nextFrame){		// overloading for taught points
		IFiredConditionInfo JTBreak;
		do {
			torqueBreakMotion = kiwa.move(ptp(nextFrame).setJointVelocityRel(defSpeed).breakWhen(JTConds)); 
			JTBreak = torqueBreakMotion.getFiredBreakConditionInfo();
			if (JTBreak != null) {
				logPad("Collision detected!"); 
				boolean prev[] = getRGB();
				setRGB("R");
				waitUserButton();
				setRGB(prev);
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
			blinkRGB("RG", 500);
			kiwa.move(linRel(0, 0, -25).setJointVelocityRel(defSpeed));
			return true;
		} else {
			System.out.println("No components detected. " ); 
			setRGB("RB");
			kiwa.move(linRel(0, 0, -25).setJointVelocityRel(defSpeed));
			return false;
		} 
	}

	private int checkBtnInput(){						// determine user button input
		boolean[] prevRGB = getRGB();
		int timeCount = 0;
		int pressCountShort = 0;
		int pressCountLong = 0;
		setRGB("GB");
		
		outerLoop:
		while(true) {
			while (true) {								// While pressed
				if (!mfio.getUserButton()) {			// Button unpressed
					setRGB("OFF");
					if (timeCount < 9) pressCountShort += 1;
					else pressCountLong += 1;
					timeCount = 0;
					break;
				}
				waitMillis(50);
				timeCount += 1;
				if (timeCount > 9) setRGB("R");
			}
			while (true) {								// While unpressed
				if (timeCount > 9) break outerLoop;		// Timeout, finished interaction
				if (mfio.getUserButton()) {				// new action detected
					setRGB("GB");
					timeCount = 0;
					break;
				}
				waitMillis(50);
				timeCount += 1;
			}
		}
		setRGB(prevRGB);
		logPad("#Short = " + pressCountShort + ", #Long = " + pressCountLong);
		return (pressCountLong * 10 + pressCountShort);
	}
	
	/***************************************************************************
	* STANDARD METHODS BY mario.musico@electrolux.com <p>
	***************************************************************************/
	
	public void waitUserButton() {
		logPad("Press USER GREEN BUTTON to continue");
		while (true) {
			if (mfio.getUserButton()) break;
			waitMillis(50);
		}
		waitMillis(500, false);		// Wait for torque to stabilize
	}
	
	public boolean [] getRGB() {
		boolean[] rgb = new boolean [3];
		rgb[0] = mfio.getLEDRed();
		rgb[1] = mfio.getLEDGreen();
		rgb[2] = mfio.getLEDBlue();
		return rgb;
	}
	
	public void setRGB(boolean[] rgb) {
		mfio.setLEDRed(rgb[0]);
		mfio.setLEDGreen(rgb[1]);
		mfio.setLEDBlue(rgb[2]);
	}
	
	public void setRGB(boolean r, boolean g, boolean b) {
		mfio.setLEDRed(r);
		mfio.setLEDGreen(g);
		mfio.setLEDBlue(b);
	}

	public void setRGB(String color, boolean log) {
		if (log) logPad("MediaFlange LED ring to " + color);
		if (color.equalsIgnoreCase("R")) setRGB(true,false,false);
		else if (color.equalsIgnoreCase("G")) setRGB(false,true,false);
		else if (color.equalsIgnoreCase("B")) setRGB(false,false,true);
		else if (color.equalsIgnoreCase("RG")) setRGB(true,true,false);
		else if (color.equalsIgnoreCase("RB")) setRGB(true,false,true);
		else if (color.equalsIgnoreCase("GB")) setRGB(false,true,true);
		else if (color.equalsIgnoreCase("RGB")) setRGB(true,true,true);
		else if (color.equalsIgnoreCase("OFF")) setRGB(false,false,false);
		else System.out.println("MediaFlange color not valid");
	}
	
	public void setRGB(String color) {
		setRGB(color, false);
	}
	
	public void blinkRGB(String color, int millis, boolean log) {
		boolean prev[] = getRGB();
		if (log) logPad("MediaFlange LED blink " + color + " for " + millis + " millis");
		setRGB(color);
		waitMillis(millis);
		setRGB(prev);
	}
	
	public void blinkRGB(String color, int millis) {
		blinkRGB(color, millis, false);
	}
	
	public void openGripper(int millis) {
		plcout.setPinza_Chiudi(false);
		waitMillis(10);
		plcout.setPinza_Apri(true);
		logPad("Opening gripper");
		waitMillis(millis);
	}
	
	public void openGripper(boolean async) {
		if (async) openGripper(10);
		else openGripper(2000);
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