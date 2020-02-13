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

public class Tr_PickPlace_old_v2 extends RoboticsAPIApplication {
	@Inject private LBR 				kiwa;
	@Inject private Plc_inputIOGroup 	plcin;
	@Inject private Plc_outputIOGroup 	plcout;
	@Inject private MediaFlangeIOGroup 	mfio;
	@Inject	@Named("Pinza") 		private Tool 		Gripper;
	@Inject @Named("VacuumBody") 	private Workpiece 	VacuumBody;
	@Inject private ITaskLogger 		logger;

	private enum States {state_home, state_teach, state_run, state_pause};
	private enum ButtonInput {short_press, long_press, double_click, triple_click, invalid};
	private States state; 
	private boolean keyAlreadyPressed = false;
	//private int pointCounter, 
	private int promptAns;
	private double maxTorque = 10.0;
	private double defSpeed = 0.15;
	//private Frame framesArr[] = new Frame[50]; 
	private Frame nextFrame;
	private String homeFramePath;
	private boolean exitForLoop = false;
	
	private FrameWrapper fwo = new FrameWrapper();
 
	 
	private CartesianImpedanceControlMode ctrMode = new CartesianImpedanceControlMode();  	// for stiffless handguiding
	private CartesianImpedanceControlMode ctrModeStiff = new CartesianImpedanceControlMode();  // for gesture control
	private PositionHold posHold = new PositionHold(ctrMode, -1, null);  
	private IMotionContainer positionHoldContainer, moveCmdContainer;		//positionHoldContainer- for stiffless posHold;  moveCmdContainer - for torque break condition
	
	private JointTorqueCondition torqueBreakCondition1, torqueBreakCondition2, torqueBreakCondition3, torqueBreakCondition4, torqueBreakCondition5, torqueBreakCondition6, torqueBreakCondition7 ;					// for torque break condition
	private ICondition jointTorqueBreakConditions ;
	private IFiredConditionInfo info;
	
	@Override
	public void initialize() {
		setRGB(false, false, true);
		// setting the Stiffness in HandGuiding mode
		ctrMode.parametrize(CartDOF.TRANSL).setStiffness(0.1).setDamping(1);			// stiffless handguiding
		ctrMode.parametrize(CartDOF.ROT).setStiffness(0.1).setDamping(1);  
		
		ctrModeStiff.parametrize(CartDOF.TRANSL).setStiffness(5000).setDamping(1);		// gesture control
		ctrModeStiff.parametrize(CartDOF.ROT).setStiffness(300).setDamping(1); 
		
		//setting joint torque break condition
		promptAns = getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, "Set the maximum External Torque ", "5 Nm", "10 Nm", "15 Nm", "20 Nm");  
		switch (promptAns) {
			case 0: maxTorque = 5.0; break;
			case 1: maxTorque = 10.0; break;
			case 2: maxTorque = 15.0; break;
			case 3: maxTorque = 20.0; break;
			default: maxTorque = 10.0; break;
		}
		logPad("Max Axis Torque set to " + maxTorque + " Nm.");		
		 
		torqueBreakCondition1 = new JointTorqueCondition(JointEnum.J1, -maxTorque, maxTorque);	
		torqueBreakCondition2 = new JointTorqueCondition(JointEnum.J2, -maxTorque, maxTorque);
		torqueBreakCondition3 = new JointTorqueCondition(JointEnum.J3, -maxTorque, maxTorque);	
		torqueBreakCondition4 = new JointTorqueCondition(JointEnum.J4, -maxTorque, maxTorque);
		torqueBreakCondition5 = new JointTorqueCondition(JointEnum.J5, -maxTorque, maxTorque);	
		torqueBreakCondition6 = new JointTorqueCondition(JointEnum.J6, -maxTorque, maxTorque);
		torqueBreakCondition7 = new JointTorqueCondition(JointEnum.J7, -maxTorque, maxTorque);
		
		jointTorqueBreakConditions = torqueBreakCondition1
									.or(torqueBreakCondition2)
									.or(torqueBreakCondition3)
									.or(torqueBreakCondition4)
									.or(torqueBreakCondition5)
									.or(torqueBreakCondition6)
									.or(torqueBreakCondition7);
		
		// initialize your application here 
		System.out.println("Initializing...");
		kiwa.setHomePosition(getApplicationData().getFrame("/Rest"));
		homeFramePath = "/Rest";
		state = States.state_home; 
		enableTEACHbuttons(); 
		//pointCounter =0;
		fwo.Free();
		Gripper.attachTo(kiwa.getFlange()); 	
		
		setRGB(false, true, false);		
	}

	@Override
	public void run() {
		for(;;) {
			switch (state) {
				case state_home:
				logPad("Going home.");  
				movePtpWithTorqueCondition(homeFramePath);
				state = States.state_teach;
				break;

			case state_teach:
				System.out.println("In teaching mode."); 
				movePtpWithTorqueCondition(homeFramePath);
				//pointCounter = 0;
				fwo.Free();
				myHandGuiding(); 
				System.out.println("Exiting teach."); 
				state = States.state_run;

				break;


			case state_run:
				System.out.println("Running."); 
				setRGB(false, true, false); 
				ThreadUtil.milliSleep(50);
				
				exitForLoop = false;
				for (int i = 0; i < fwo.GetCounter(); i++) { 							// last saved frame is pointCounter-1
					
					if (exitForLoop) {
						exitForLoop = false;
						break;
					}
					System.out.println("Going to Frame "+ i +".");
					
					 
					movePtpWithTorqueCondition(fwo.GetFrame(i));
					 
										
					if (fwo.GetFrame(i).hasAdditionalParameter("open Gripper")) {
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
					if (fwo.GetFrame(i).hasAdditionalParameter("close Gripper")) {
						
						System.out.println("Checking component.");
						while (true) {
							if (checkComponent()) {
								setRGB(false, true, false);  
								break;
							}else {
								System.out.println("Waiting for gesture input. ");
								kiwa.move(positionHold(ctrModeStiff, -1, null).breakWhen(jointTorqueBreakConditions)); 
							}
							ThreadUtil.milliSleep(250);
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
					System.out.println("Done.");
				} 
				break;
				
			case state_pause:
				ThreadUtil.milliSleep(50);
				break;

			default:
				System.out.println("Default case.");
				state = States.state_home;
				break;
			}
		}
	}
	
	private void movePtpWithTorqueCondition (Frame nextFrame){		// overloading for taught points
		moveCmdContainer = kiwa.move(ptp(nextFrame).setJointVelocityRel(defSpeed).breakWhen(jointTorqueBreakConditions)); 
		info = moveCmdContainer.getFiredBreakConditionInfo();
		while (info != null) {
			System.out.println("Collision detected . " ); 
			setRGB(true, false, false);
			while(!mfio.getUserButton()){
				ThreadUtil.milliSleep(20);
			} 
			ThreadUtil.milliSleep(500);			// This is necessary, OW. the next breakConditions are again triggered
			setRGB(false, true, false);
			
			System.out.println("Next state : " + state);		
			if (state != States.state_run) {	
				exitForLoop = true;
				System.out.println("breaking"); 
				break;
			}
			
			moveCmdContainer = kiwa.move(ptp(nextFrame).setJointVelocityRel(defSpeed).breakWhen(jointTorqueBreakConditions)); 
			info = moveCmdContainer.getFiredBreakConditionInfo();
		}
	}
	private void movePtpWithTorqueCondition (String framePath){		// overloading for appData points
		moveCmdContainer = kiwa.move(ptp(getApplicationData().getFrame(framePath)).setJointVelocityRel(defSpeed).breakWhen(jointTorqueBreakConditions)); 
		info = moveCmdContainer.getFiredBreakConditionInfo();
		while (info != null) {
			System.out.println("Collision detected . " ); 
			setRGB(true, false, false);
			while(!mfio.getUserButton()){
				ThreadUtil.milliSleep(20);
			} 
			ThreadUtil.milliSleep(500);			// This is necessary, OW. the next breakConditions are again triggered
			setRGB(false, true, false);
			
			System.out.println("Next state : " + state);   // go to teach position immediately if pressed
			if (state != States.state_run) {
				exitForLoop = true;
				System.out.println("breaking");
				break;
			}
			
			moveCmdContainer = kiwa.move(ptp(getApplicationData().getFrame(framePath)).setJointVelocityRel(defSpeed).breakWhen(jointTorqueBreakConditions)); 
			info = moveCmdContainer.getFiredBreakConditionInfo();
		}
	}
	private void myHandGuiding(){											// handguiding function
		
		System.out.println("Push USER button and start handguiding.");
		setRGB(false, true, true);
		while (true) {
			ThreadUtil.milliSleep(50); 
			if ( mfio.getUserButton()) {
				ThreadUtil.milliSleep(800);
				System.out.println("Starting stiffless handguiding"); 
				setRGB(false, false, false);
				positionHoldContainer = kiwa.moveAsync(posHold); 
				break;
			}
		}
 	 
		
		while (true) {
			ThreadUtil.milliSleep(50);
			// points are recorded through the buttons
			if ( mfio.getUserButton()) {
				ButtonInput buttonInputCheck = userButtonInput();						// run the button press check
				if (buttonInputCheck == ButtonInput.long_press) { 						// exit myHandGuiding
					if (fwo.GetCounter() >1) {
						positionHoldContainer.cancel();   
						ThreadUtil.milliSleep(500);
						System.out.println("Exiting stiffless handguiding"); 
						setRGB(false, true, false);
						movePtpWithTorqueCondition(homeFramePath);

						break;
					}else {
						System.out.println("Record at least 2 positions to start running.");
						ThreadUtil.milliSleep(200);
					}
				}else if (buttonInputCheck== ButtonInput.short_press) { 							// Record current position
					Frame tempFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange()); 
					fwo.Add(tempFrame);
					System.out.println("Added Frame "+fwo.GetCounter()+" : " + fwo.Last().toString());
				}else if (buttonInputCheck == ButtonInput.double_click) {					// Record current position & Open Gripper
					Frame tempFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
					tempFrame.setAdditionalParameter("open Gripper", 1); 
					fwo.Add(tempFrame);
					System.out.println("Added Frame "+fwo.GetCounter()+" : " + fwo.Last().toString()); 
					
					positionHoldContainer.cancel(); 
					VacuumBody.detach();
					System.out.println("Workpiece Detached."); 
					positionHoldContainer = kiwa.moveAsync(posHold); 
					plcout.setPinza_Chiudi(false);
					plcout.setPinza_Apri(true);
					ThreadUtil.milliSleep(2000);							 
					 
			//		pointCounter++;
					
				}else if (buttonInputCheck == ButtonInput.triple_click) {					// Record current position &  Close Gripper
					Frame tempFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
					tempFrame.setAdditionalParameter("close Gripper", 1); 
					fwo.Add(tempFrame);
					System.out.println("Added Frame "+fwo.GetCounter()+" : " + fwo.Last().toString()); 
					
					plcout.setPinza_Apri(false); 
					plcout.setPinza_Chiudi(true);
					ThreadUtil.milliSleep(1800);							// to wait while gripper closing so workpiece can be attached
					if (plcin.getPinza_Holding()) {
						positionHoldContainer.cancel(); 
//						VacuumBody.attachTo(Gripper.getDefaultMotionFrame()); 
						System.out.println("Workpiece Not Attached.");
						positionHoldContainer = kiwa.moveAsync(posHold); 
					}else { 
						System.out.println("Workpiece Not Attached.");
					}

				//	pointCounter++;
					
				}else {
					//do nothing
				}

			}
		} 
	}

//	private void enableMODESbuttons() { 									// MODES Buttons
//		IUserKeyBar keyBar = getApplicationUI().createUserKeyBar("MODES");
//
//		IUserKeyListener listener1 = new IUserKeyListener() {
//
//			@Override
//			public void onKeyEvent(IUserKey key, UserKeyEvent event) {
//				if (0 == key.getSlot()) {  						//KEY - ESM 1								
////					kiwa.setESMState("1"); 
////					System.out.println("ESM set to 1");
//				}				
//				if (1 == key.getSlot()) {  						//KEY - ESM 2
//					//					kiwa.setESMState("2");
////					System.out.println("ESM set to 2");
//				}
//				if (2 == key.getSlot()) {  						//KEY - TEACH MODE			 
//					state = States.state_teach;
//					System.out.println("Going to TEACH mode after this cycle.");
//				}
//				if (3 == key.getSlot()) {  						//KEY - PAUSE/PLAY 				
//					if (!keyAlreadyPressed && state==States.state_run) {
//						state = States.state_pause;
//						keyAlreadyPressed = true; 
//						System.out.println("Pauzing program."); 
//						ThreadUtil.milliSleep(500);							
//					}else if (!keyAlreadyPressed && state==States.state_pause) {
//						state = States.state_run;
//						keyAlreadyPressed = true; 
//						System.out.println("Resuming program."); 
//					} else if (keyAlreadyPressed) {
//						keyAlreadyPressed = false; 
//					}
//					
//				}
//			}
//		};
//
//		IUserKey button0 = keyBar.addUserKey(0, listener1, true);
//		IUserKey button1 = keyBar.addUserKey(1, listener1, true);
//		IUserKey button2 = keyBar.addUserKey(2, listener1, true);
//		IUserKey button3 = keyBar.addUserKey(3, listener1, true);
//
//		button0.setText(UserKeyAlignment.TopMiddle, "ESM1"); 
//		button1.setText(UserKeyAlignment.TopMiddle, "ESM2"); 
//		button2.setText(UserKeyAlignment.TopMiddle, "TEACH");
//		button3.setText(UserKeyAlignment.TopMiddle, "PAUSE/PLAY");
//
//
//		keyBar.publish();
//
//	}
	
	private void enableTEACHbuttons() { 										// TEACH buttons
		IUserKeyBar keyBar2 = getApplicationUI().createUserKeyBar("TEACH");
		IUserKeyListener listener2 = new IUserKeyListener() {
			@Override
			public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				if (key.getSlot() == 0) { 						//KEY - DELETE PREVIOUS
					if (!keyAlreadyPressed && state == States.state_teach) {
						if (fwo.Last().hasAdditionalParameter("close Gripper")) {
							plcout.setPinza_Chiudi(false);
							plcout.setPinza_Apri(true);
						}					
						fwo.deleteLastFrame(); 
						keyAlreadyPressed = true;	
					} else if (keyAlreadyPressed) {
						keyAlreadyPressed = false; 
					}
				}
	
//				if (1 == key.getSlot()) {  						//KEY 0 - RECORD POSITION
//					if (!keyAlreadyPressed && state == States.state_teach) {
//						frames[pointCounter] = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
//						System.out.println("Added Frame "+pointCounter+" : " + frames[pointCounter].toString()); 
//						setRGB(false, false, false);
//						ThreadUtil.milliSleep(150);
//						setRGB(false, true, false);
//						ThreadUtil.milliSleep(150);
//						setRGB(false, true, true);
//						pointCounter++;
//						keyAlreadyPressed = true;	
//					} else if (keyAlreadyPressed) {
//						keyAlreadyPressed = false; 
//					}
//				}

				if (key.getSlot() == 1) {  						//KEY 1 - TEACH MODE			 
					state = States.state_teach;
					exitForLoop = true;
					System.out.println("Going to TEACH mode.");
				}
				if (key.getSlot() == 2) {   					//KEY 2 - OPEN GRIPPER 
					if (!keyAlreadyPressed) {
						Frame tempFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
						tempFrame.setAdditionalParameter("open Gripper", 1); 
						fwo.Add(tempFrame);
						System.out.println("Added Frame "+fwo.GetCounter()+" : " + fwo.Last().toString());
			
						setRGB(false, false, false);
						ThreadUtil.milliSleep(150);
						setRGB(false, true, false);
						ThreadUtil.milliSleep(150);
						setRGB(false, true, true);
						
						positionHoldContainer.cancel(); 
						VacuumBody.detach();
						System.out.println("Workpiece Detached."); 
						positionHoldContainer = kiwa.moveAsync(posHold); 
						plcout.setPinza_Chiudi(false);
						plcout.setPinza_Apri(true);
						ThreadUtil.milliSleep(2000);							 
						 
					//	pointCounter++;
						keyAlreadyPressed = true;	
					} else if (keyAlreadyPressed) {
						keyAlreadyPressed = false; 
					}
				}
				if (key.getSlot() == 3) {   					//KEY 3 - CLOSE GRIPPER
					if (!keyAlreadyPressed) {
						Frame tempFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
						tempFrame.setAdditionalParameter("close Gripper", 1); 
						fwo.Add(tempFrame);
						System.out.println("Added Frame "+fwo.GetCounter()+" : " + fwo.Last().toString());
						
						setRGB(false, false, false);
						ThreadUtil.milliSleep(150);
						setRGB(false, true, false);
						ThreadUtil.milliSleep(150);
						setRGB(false, true, true);
						
						plcout.setPinza_Apri(false); 
						plcout.setPinza_Chiudi(true);
						ThreadUtil.milliSleep(1800);							// to wait while gripper closing so workpiece can be attached
						if (plcin.getPinza_Holding()) {
							positionHoldContainer.cancel(); 
//							VacuumBody.attachTo(Gripper.getDefaultMotionFrame()); 
							System.out.println("Workpiece Not Attached.");
							positionHoldContainer = kiwa.moveAsync(posHold); 
						}else { 
							System.out.println("Workpiece Not Attached.");
						}

					//	pointCounter++;
						keyAlreadyPressed = true;	
					} else if (keyAlreadyPressed) {
						keyAlreadyPressed = false; 
					}
				}
			}
		};

		IUserKey button0 = keyBar2.addUserKey(0, listener2, true);
		IUserKey button1 = keyBar2.addUserKey(1, listener2, true);
		IUserKey button2 = keyBar2.addUserKey(2, listener2, true);
		IUserKey button3 = keyBar2.addUserKey(3, listener2, true);

		button0.setText(UserKeyAlignment.TopMiddle, "Delete Previous"); 
		button1.setText(UserKeyAlignment.TopMiddle, "TEACH"); 
		button2.setText(UserKeyAlignment.TopMiddle, "Open Gripper");
		button3.setText(UserKeyAlignment.TopMiddle, "Close Gripper");

		keyBar2.publish();
	}
	
	private void setRGB(boolean r, boolean g, boolean b){
		mfio.setLEDRed(r);
		mfio.setLEDGreen(g);
		mfio.setLEDBlue(b);
	}
	
//	private ButtonInput userButtonInputOLD(){						// determine whether user button is only pressed or pressed and held
//		boolean R,G,B = false;
//		R = mfio.getLEDRed();
//		G = mfio.getLEDGreen();
//		B = mfio.getLEDBlue();
//		setRGB(false, false, false);
//		long pressedTime = 0;
//		while (true) {
//			ThreadUtil.milliSleep(100);
//			pressedTime = pressedTime + 1; 
//			if (!mfio.getUserButton()) {
//				break;
//			}
//			if (pressedTime>8) {
//				setRGB(true, false, true);
//			}
//		}
//		if (pressedTime < 8) {
//			System.out.println("Short button press. : "+ pressedTime * 100 +" ms.");
//			pressedTime = 0;
//			setRGB(R, G, B);
//			return ButtonInput.short_press;
//		} else {
//			System.out.println("Long button press. : "+ pressedTime * 100 +" ms.");
//			pressedTime = 0;  
//			setRGB(R, G, B);
//			return ButtonInput.long_press; 
//		}
//	}
	
	private ButtonInput userButtonInput(){						// determine whether user button is only pressed or pressed and held
		boolean R,G,B = false;
		R = mfio.getLEDRed();
		G = mfio.getLEDGreen();
		B = mfio.getLEDBlue();
		setRGB(false, false, false);
		long pressedTime =0, unpressedTime = 0;
		int buttonChangeCount = 1;
		boolean returnValue = false;
		
		setRGB(false, true, true);	 
		
		while (true) { 
			pressedTime = pressedTime + 1; 
			unpressedTime = 0;
			if (!mfio.getUserButton()) {
				setRGB(false, false, false);
				buttonChangeCount++;
				if (pressedTime>9 && buttonChangeCount ==2) {			// long press
					returnValue = true; 
				}else{
					pressedTime = 0;
					while (true) {
						ThreadUtil.milliSleep(100);
						unpressedTime = unpressedTime + 1; 
						if (unpressedTime >9) {							// break if button has been released for too long
							returnValue = true;
							break;				
						}
						if (mfio.getUserButton()) {
							setRGB(false, true, true); 
							buttonChangeCount++;
							break;							
						}
					}
				}
			}
			if (returnValue) {
				break;
			}
			if (pressedTime>8) {
				setRGB(true, false, true);
			}
			ThreadUtil.milliSleep(50);
		}

		if (buttonChangeCount == 2 && pressedTime>9) {
			System.out.println("Long button press. : "+ pressedTime * 100 +" ms."); 
			setRGB(R, G, B);
			return ButtonInput.long_press;
		}else if (buttonChangeCount == 2 && unpressedTime >9 ) {
			System.out.println("Short button press."); 
			setRGB(R, G, B);
			return ButtonInput.short_press;
		}else if (buttonChangeCount == 4) {
			System.out.println("Double button press."); 
			setRGB(R, G, B);
			return ButtonInput.double_click;
		}else if (buttonChangeCount == 6) {
			System.out.println("Triple button press."); 
			setRGB(R, G, B);
			return ButtonInput.triple_click;
		}else {
			System.out.println("Invalid button Input."); 
			setRGB(R, G, B);
			return ButtonInput.invalid;
		}
	}
	
	private boolean checkComponent(){
		moveCmdContainer = kiwa.move(linRel(0, 0, 25).setJointVelocityRel(0.01).breakWhen(jointTorqueBreakConditions)); 
		info = moveCmdContainer.getFiredBreakConditionInfo();
		if (info != null) {
			System.out.println("Component detected. " ); 
			setRGB(true, false, false);  
			ThreadUtil.milliSleep(500);
			kiwa.move(linRel(0, 0, -25).setJointVelocityRel(defSpeed));
			return true; 
			
		}else {
			System.out.println("No components detected. " ); 
			kiwa.move(linRel(0, 0, -25).setJointVelocityRel(defSpeed));
			return false;
		} 
	}
}
