package application;


import javax.inject.Inject; 
import javax.inject.Named;

import com.kuka.common.ThreadUtil;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import static com.kuka.roboticsAPI.motionModel.HRCMotions.*;

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
import com.sun.org.apache.bcel.internal.generic.Select;

/**
 * Implementation of a robot application.
 * <p>
 * The application provides a {@link RoboticsAPITask#initialize()} and a 
 * {@link RoboticsAPITask#run()} method, which will be called successively in 
 * the application lifecycle. The application will terminate automatically after 
 * the {@link RoboticsAPITask#run()} method has finished or after stopping the 
 * task. The {@link RoboticsAPITask#dispose()} method will be called, even if an 
 * exception is thrown during initialization or run. 
 * <p>
 * <b>It is imperative to call <code>super.dispose()</code> when overriding the 
 * {@link RoboticsAPITask#dispose()} method.</b> 
 * 
 * @see UseRoboticsAPIContext
 * @see #initialize()
 * @see #run()
 * @see #dispose()
 */
public class Training extends RoboticsAPIApplication {
	@Inject
	private LBR kiwa;
	
	@Inject	 
	@Named("Pinza")
	private Tool Gripper;
	
	@Inject
	@Named("VacuumBody")
	private Workpiece VacuumBody;
	
	@Inject 							private Plc_inputIOGroup 		plcin;
	@Inject 							private Plc_outputIOGroup 		plcout;
	@Inject 							private MediaFlangeIOGroup 		mfio;
	
	@Inject
	private ITaskLogger logger;

	private enum States {state_home, state_teach, state_run, state_pause};
	private enum ButtonInput {short_press, long_press, double_click, triple_click, invalid};
	private States state; 
	private boolean keyAlreadyPressed = false;
	private int pointCounter, promptAns;
	private double maxTorque = 10.0;
	private double defSpeed = 0.15;
	private Frame frames[] = new Frame[50]; 
	private Frame nextFrame;
	private String homeFramePath;
 
	 
	private CartesianImpedanceControlMode ctrMode = new CartesianImpedanceControlMode();  	// for agile handguiding
	private CartesianImpedanceControlMode ctrModeStiff = new CartesianImpedanceControlMode();  // for gesture control
	private PositionHold posHold = new PositionHold(ctrMode, -1, null);  
	private IMotionContainer positionHoldContainer, moveCmdContainer;		//positionHoldContainer- for agile posHold;  moveCmdContainer - for torque break condition
	
	private JointTorqueCondition torqueBreakCondition1, torqueBreakCondition2, torqueBreakCondition3, torqueBreakCondition4, torqueBreakCondition5, torqueBreakCondition6, torqueBreakCondition7 ;					// for torque break condition
	private IFiredConditionInfo info;
	
	@Override
	public void initialize() {
		setRGB(false, false, true);
		// setting the Stiffness in HandGuiding mode
		ctrMode.parametrize(CartDOF.TRANSL).setStiffness(0.1).setDamping(1);			// agile handguiding
		ctrMode.parametrize(CartDOF.ROT).setStiffness(0.1).setDamping(1);  
		
		ctrModeStiff.parametrize(CartDOF.TRANSL).setStiffness(5000).setDamping(1);		// gesture control
		ctrModeStiff.parametrize(CartDOF.ROT).setStiffness(300).setDamping(1); 
		
		//setting joint torque break condition 
		torqueBreakCondition1 = new JointTorqueCondition(JointEnum.J1, -maxTorque, maxTorque);	
		torqueBreakCondition2 = new JointTorqueCondition(JointEnum.J2, -maxTorque, maxTorque);
		torqueBreakCondition3 = new JointTorqueCondition(JointEnum.J3, -maxTorque, maxTorque);	
		torqueBreakCondition4 = new JointTorqueCondition(JointEnum.J4, -maxTorque, maxTorque);
		torqueBreakCondition5 = new JointTorqueCondition(JointEnum.J5, -maxTorque, maxTorque);	
		torqueBreakCondition6 = new JointTorqueCondition(JointEnum.J6, -maxTorque, maxTorque);
		torqueBreakCondition7 = new JointTorqueCondition(JointEnum.J7, -maxTorque, maxTorque);
			
		
		// initialize your application here 
		System.out.println("Initializing...");
		kiwa.setHomePosition(getApplicationData().getFrame("/Rest"));
		homeFramePath = "/Rest";
		state = States.state_home; 
		enableTEACHbuttons(); 
		enableTORQUEbuttons(); 
		pointCounter =0;
		Gripper.attachTo(kiwa.getFlange()); 	
		
		setRGB(false, true, false);		
		
//							while (true) {									// Just for test
//								if (mfio.getUserButton()) {
//									userButtonInput();
//								}
//								ThreadUtil.milliSleep(50);
//							}
	}

	@Override
	public void run() {
		// your application execution starts here
		while (true) {
			switch (state) {
			case state_home:
				System.out.println("Going home.");  
				movePtpWithTorqueCondition(homeFramePath);
				state = States.state_teach;
				break;

			case state_teach:
				System.out.println("In teaching mode."); 
				movePtpWithTorqueCondition(homeFramePath);
				pointCounter = 0;
				myHandGuiding(); 
				System.out.println("Exiting teach."); 
				state = States.state_run;

				break;


			case state_run:
				System.out.println("Running."); 
				setRGB(false, true, false); 
				ThreadUtil.milliSleep(50);
				
				for (int i = 0; i < pointCounter; i++) { 							// last saved frame is pointCounter-1
					System.out.println("Going to Position "+ i +".");
					
					movePtpWithTorqueCondition(frames[i]);
										
					if (frames[i].hasAdditionalParameter("open Gripper")) {
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
					if (frames[i].hasAdditionalParameter("close Gripper")) {
						
						System.out.println("Checking component.");
						while (true) {
							if (checkComponent()) {
								break;
							}else {
								System.out.println("Waiting for gesture input. ");
								kiwa.move(positionHold(ctrModeStiff, -1, null).breakWhen(torqueBreakCondition1).breakWhen(torqueBreakCondition2).breakWhen(torqueBreakCondition3).breakWhen(torqueBreakCondition4).breakWhen(torqueBreakCondition5).breakWhen(torqueBreakCondition6).breakWhen(torqueBreakCondition7)); 
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
//									System.out.println("Workpiece Not Attached");
								}
								break;
							}
						}
					} 
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
		moveCmdContainer = kiwa.move(ptp(nextFrame).setJointVelocityRel(defSpeed).breakWhen(torqueBreakCondition1).breakWhen(torqueBreakCondition2).breakWhen(torqueBreakCondition3).breakWhen(torqueBreakCondition4).breakWhen(torqueBreakCondition5).breakWhen(torqueBreakCondition6).breakWhen(torqueBreakCondition7)); 
		info = moveCmdContainer.getFiredBreakConditionInfo();
		while (info != null) {
			System.out.println("Collision detected . " ); 
			setRGB(true, false, false);
			while(!mfio.getUserButton()){
				ThreadUtil.milliSleep(20);
			} 
			ThreadUtil.milliSleep(500);			// This is necessary, OW. the next breakConditions are again triggered
			setRGB(false, true, false);
			moveCmdContainer = kiwa.move(ptp(nextFrame).setJointVelocityRel(defSpeed).breakWhen(torqueBreakCondition1).breakWhen(torqueBreakCondition2).breakWhen(torqueBreakCondition3).breakWhen(torqueBreakCondition4).breakWhen(torqueBreakCondition5).breakWhen(torqueBreakCondition6).breakWhen(torqueBreakCondition7)); 
			info = moveCmdContainer.getFiredBreakConditionInfo();
		}
	}
	private void movePtpWithTorqueCondition (String framePath){		// overloading for appData points
		moveCmdContainer = kiwa.move(ptp(getApplicationData().getFrame(framePath)).setJointVelocityRel(defSpeed).breakWhen(torqueBreakCondition1).breakWhen(torqueBreakCondition2).breakWhen(torqueBreakCondition3).breakWhen(torqueBreakCondition4).breakWhen(torqueBreakCondition5).breakWhen(torqueBreakCondition6).breakWhen(torqueBreakCondition7)); 
		info = moveCmdContainer.getFiredBreakConditionInfo();
		while (info != null) {
			System.out.println("Collision detected . " ); 
			setRGB(true, false, false);
			while(!mfio.getUserButton()){
				ThreadUtil.milliSleep(20);
			} 
			ThreadUtil.milliSleep(500);			// This is necessary, OW. the next breakConditions are again triggered
			setRGB(false, true, false);
			moveCmdContainer = kiwa.move(ptp(getApplicationData().getFrame(framePath)).setJointVelocityRel(defSpeed).breakWhen(torqueBreakCondition1).breakWhen(torqueBreakCondition2).breakWhen(torqueBreakCondition3).breakWhen(torqueBreakCondition4).breakWhen(torqueBreakCondition5).breakWhen(torqueBreakCondition6).breakWhen(torqueBreakCondition7)); 
			info = moveCmdContainer.getFiredBreakConditionInfo();
		}
	}
	private void myHandGuiding(){											// handguiding function
		
		System.out.println("Press USER button and start handguiding.");
		setRGB(false, true, true);
		while (true) {
			ThreadUtil.milliSleep(50); 
			if ( mfio.getUserButton()) {
				ThreadUtil.milliSleep(800);
				System.out.println("Starting agile handguiding"); 
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
					if (pointCounter >1) {
						positionHoldContainer.cancel();   
						ThreadUtil.milliSleep(500);
						System.out.println("Exiting agile handguiding"); 
						setRGB(false, true, false);
						movePtpWithTorqueCondition(homeFramePath);

						break;
					}else {
						System.out.println("Record at least 2 positions to start running.");
						ThreadUtil.milliSleep(200);
					}
				}else if (buttonInputCheck== ButtonInput.short_press) { 							// Record current position
					frames[pointCounter] = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
					System.out.println("Added Frame "+pointCounter+".");  
					pointCounter++; 
				}else if (buttonInputCheck == ButtonInput.double_click) {					// Record current position & Open Gripper
					frames[pointCounter] = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
					System.out.println("Added Frame "+pointCounter+". Gripper will be opened here." ); 
					frames[pointCounter].setAdditionalParameter("open Gripper", 1); 
					
					positionHoldContainer.cancel(); 
//					VacuumBody.detach();
//					System.out.println("Workpiece Detached."); 
					positionHoldContainer = kiwa.moveAsync(posHold); 
					plcout.setPinza_Chiudi(false);
					plcout.setPinza_Apri(true);
					ThreadUtil.milliSleep(2000);							 
					 
					pointCounter++;
					
				}else if (buttonInputCheck == ButtonInput.triple_click) {					// Record current position &  Close Gripper
					frames[pointCounter] = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
					System.out.println("Added Frame "+pointCounter+". Gripper will be closed here." ); 
					frames[pointCounter].setAdditionalParameter("close Gripper", 1); 
					
					plcout.setPinza_Apri(false); 
					plcout.setPinza_Chiudi(true);
					ThreadUtil.milliSleep(1800);							// to wait while gripper closing so workpiece can be attached
					if (plcin.getPinza_Holding()) {
						positionHoldContainer.cancel();  
//						System.out.println("Workpiece Not Attached.");
						positionHoldContainer = kiwa.moveAsync(posHold); 
					}else { 
//						System.out.println("Workpiece Not Attached.");
					}

					pointCounter++;
					
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
	
	private void enableTORQUEbuttons() { 									// TORQUE/SPEED Buttons
		IUserKeyBar keyBar = getApplicationUI().createUserKeyBar("TRQ./SPD.");

		IUserKeyListener listener1 = new IUserKeyListener() {

			@Override
			public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				if (0 == key.getSlot()) {  						//KEY - TORQUE								
					//setting joint torque break condition
					promptAns = getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, "Set the maximum External Torque ", "5 Nm", "10 Nm", "20 Nm", "30 Nm");  
					switch (promptAns) {
					case 0:
						maxTorque = 5.0;
						break;
					case 1:
						maxTorque = 10.0; 
						break;
					case 2:
						maxTorque = 20.0; 
						break;
					case 3:
						maxTorque = 30.0; 
					break;
					default:
						break;
					}

					torqueBreakCondition1 = new JointTorqueCondition(JointEnum.J1, -maxTorque, maxTorque);	
					torqueBreakCondition2 = new JointTorqueCondition(JointEnum.J2, -maxTorque, maxTorque);
					torqueBreakCondition3 = new JointTorqueCondition(JointEnum.J3, -maxTorque, maxTorque);	
					torqueBreakCondition4 = new JointTorqueCondition(JointEnum.J4, -maxTorque, maxTorque);
					torqueBreakCondition5 = new JointTorqueCondition(JointEnum.J5, -maxTorque, maxTorque);	
					torqueBreakCondition6 = new JointTorqueCondition(JointEnum.J6, -maxTorque, maxTorque);
					torqueBreakCondition7 = new JointTorqueCondition(JointEnum.J7, -maxTorque, maxTorque);

					System.out.println("Max Torque set to "+maxTorque+" Nm.");
				}				
				if (1 == key.getSlot()) {  						//KEY - SPEED
					promptAns = getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, "Set the relative axes speed ", "5%", "15%", "25%", "35%");  
					switch (promptAns) {
					case 0:
						defSpeed = 0.05; 
						break;
					case 1:
						defSpeed = 0.15; 
						break;
					case 2:
						defSpeed = 0.25; 
						break;
					case 3:
						defSpeed = 0.35; 
					break;
					default:
						break;
					}

				System.out.println("Relative speed set to "+defSpeed*100+"%.");
				}
				if (2 == key.getSlot()) {  						//KEY - Default torque			 
					maxTorque = 10;
					System.out.println("Max Torque set to "+maxTorque+" Nm.");
				}
				if (3 == key.getSlot()) {  						//KEY - Default speed				
					defSpeed = 0.15;
					System.out.println("Relative speed set to "+defSpeed*100+"%.");
				} 
			}
		};

		IUserKey button0 = keyBar.addUserKey(0, listener1, true);
		IUserKey button1 = keyBar.addUserKey(1, listener1, true);
		IUserKey button2 = keyBar.addUserKey(2, listener1, true);
		IUserKey button3 = keyBar.addUserKey(3, listener1, true);

		button0.setText(UserKeyAlignment.TopMiddle, "Set TORQUE"); 
		button1.setText(UserKeyAlignment.TopMiddle, "Set SPEED"); 
		button2.setText(UserKeyAlignment.TopMiddle, "Default TORQUE");
		button3.setText(UserKeyAlignment.TopMiddle, "Default SPEED");


		keyBar.publish();

	}
	
	private void enableTEACHbuttons() { 										// TEACH buttons
		IUserKeyBar keyBar2 = getApplicationUI().createUserKeyBar("TEACH");

		IUserKeyListener listener2 = new IUserKeyListener() {

			@Override
			public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				if (0 == key.getSlot()) { 						//KEY - DELETE PREVIOUS

					if (!keyAlreadyPressed && state == States.state_teach) {
						pointCounter = pointCounter-1;
						if (pointCounter<0) {
							pointCounter = 0;
							System.out.println("No Frames saved.");
						}else {
							System.out.println("Deleted Frame "+pointCounter+" : " + frames[pointCounter].toString());
						} 
						
						keyAlreadyPressed = true;	
					} else if (keyAlreadyPressed) {
						keyAlreadyPressed = false; 
					}

				}				
//				if (1 == key.getSlot()) {  						//KEY - RECORD POSITION
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
//					
//				}
				if (1 == key.getSlot()) {  						//KEY - TEACH MODE			 
					state = States.state_teach;
					System.out.println("Going to TEACH mode after this cycle.");
					
				}
				if (2 == key.getSlot()) {   					//KEY - OPEN GRIPPER 
					
					if (!keyAlreadyPressed) {
						frames[pointCounter] = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
						System.out.println("Added Frame "+pointCounter+" : " + frames[pointCounter].toString()); 
						frames[pointCounter].setAdditionalParameter("open Gripper", 1);
						setRGB(false, false, false);
						ThreadUtil.milliSleep(150);
						setRGB(false, true, false);
						ThreadUtil.milliSleep(150);
						setRGB(false, true, true);
						
						positionHoldContainer.cancel(); 
						VacuumBody.detach();
//						System.out.println("Workpiece Detached."); 
						positionHoldContainer = kiwa.moveAsync(posHold); 
						plcout.setPinza_Chiudi(false);
						plcout.setPinza_Apri(true);
						ThreadUtil.milliSleep(2000);							 
						 
						pointCounter++;
						keyAlreadyPressed = true;	
					} else if (keyAlreadyPressed) {
						keyAlreadyPressed = false; 
					}

					 
				}
				if (3 == key.getSlot()) {   					//KEY - CLOSE GRIPPER
					
					if (!keyAlreadyPressed) {
						frames[pointCounter] = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
						System.out.println("Added Frame "+pointCounter+" : " + frames[pointCounter].toString()); 
						frames[pointCounter].setAdditionalParameter("close Gripper", 1);
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
//							System.out.println("Workpiece Not Attached.");
							positionHoldContainer = kiwa.moveAsync(posHold); 
						}else { 
//							System.out.println("Workpiece Not Attached.");
						}

						pointCounter++;
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
				if (pressedTime>7 && buttonChangeCount ==2) {			// long press
					returnValue = true; 
				}else{
					pressedTime = 0;
					while (true) {
						ThreadUtil.milliSleep(100);
						unpressedTime = unpressedTime + 1; 
						if (unpressedTime >7) {							// break if button has been released for too long
							returnValue = true;
							setRGB(true, true, true);
							ThreadUtil.milliSleep(250);
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
			if (pressedTime>6) {
				setRGB(true, false, true);
			}
			ThreadUtil.milliSleep(50);
		}

		if (buttonChangeCount == 2 && pressedTime>7) {
			System.out.println("Long button press. : "+ pressedTime * 100 +" ms."); 
			setRGB(R, G, B);
			return ButtonInput.long_press;
		}else if (buttonChangeCount == 2 && unpressedTime >7 ) {
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
		
		moveCmdContainer = kiwa.move(linRel(0, 0, 25).setJointVelocityRel(0.01).breakWhen(torqueBreakCondition1).breakWhen(torqueBreakCondition2).breakWhen(torqueBreakCondition3).breakWhen(torqueBreakCondition4).breakWhen(torqueBreakCondition5).breakWhen(torqueBreakCondition6).breakWhen(torqueBreakCondition7)); 
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