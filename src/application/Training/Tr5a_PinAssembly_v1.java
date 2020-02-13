package application.Training;


import java.util.concurrent.TimeUnit;
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

import com.kuka.roboticsAPI.conditionModel.ForceCondition;
import com.kuka.roboticsAPI.conditionModel.JointTorqueCondition;
import com.kuka.roboticsAPI.deviceModel.JointEnum;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.executionModel.IFiredConditionInfo;
import com.kuka.roboticsAPI.geometricModel.CartDOF; 
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.Workpiece;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;
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
public class Tr5a_PinAssembly_v1 extends RoboticsAPIApplication {
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
	private boolean keyAlreadyPressed, isAttachedTool, reducedSpeed = false; 
	private int promptAns;
	private double maxTorque = 10.0;
	private double defSpeed = 0.15; 
	private Frame nextFrame;
	private String homeFramePath;
	
	private FrameWrapper fwo = new FrameWrapper();
 
	 
	private CartesianImpedanceControlMode ctrMode = new CartesianImpedanceControlMode();  	// for stiffless handguiding
	private CartesianImpedanceControlMode ctrModeStiff = new CartesianImpedanceControlMode();  // for gesture control
	private PositionHold posHold = new PositionHold(ctrMode, -1, null);  
	private IMotionContainer positionHoldContainer, moveCmdContainer;		//positionHoldContainer- for stiffless posHold;  moveCmdContainer - for torque break condition
	
	private JointTorqueCondition torqueBreakCondition1, torqueBreakCondition2, torqueBreakCondition3, torqueBreakCondition4, torqueBreakCondition5, torqueBreakCondition6, torqueBreakCondition7 ;					// for torque break condition
	private IFiredConditionInfo info;
	
	@Override
	public void initialize() {
		setRGB(false, false, true);
		// setting the Stiffness in HandGuiding mode
		ctrMode.parametrize(CartDOF.TRANSL).setStiffness(0.1).setDamping(1);			// stiffless handguiding
		ctrMode.parametrize(CartDOF.ROT).setStiffness(0.1).setDamping(1);  
		
		ctrModeStiff.parametrize(CartDOF.TRANSL).setStiffness(5000).setDamping(1);		// gesture control
		ctrModeStiff.parametrize(CartDOF.ROT).setStiffness(300).setDamping(1); 

		torqueBreakCondition1 = new JointTorqueCondition(JointEnum.J1, -maxTorque, maxTorque);	
		torqueBreakCondition2 = new JointTorqueCondition(JointEnum.J2, -maxTorque, maxTorque);
		torqueBreakCondition3 = new JointTorqueCondition(JointEnum.J3, -maxTorque, maxTorque);	
		torqueBreakCondition4 = new JointTorqueCondition(JointEnum.J4, -maxTorque, maxTorque);
		torqueBreakCondition5 = new JointTorqueCondition(JointEnum.J5, -maxTorque, maxTorque);	
		torqueBreakCondition6 = new JointTorqueCondition(JointEnum.J6, -maxTorque, maxTorque);
		torqueBreakCondition7 = new JointTorqueCondition(JointEnum.J7, -maxTorque, maxTorque);
			
		
		// initialize your application here 
		System.out.println("Initializing...");
		kiwa.setHomePosition(getApplicationData().getFrame("/PinAssem"));
		homeFramePath = "/PinAssem";
		state = States.state_home; 
		enableTEACHbuttons(); 
		enableTORQUEbuttons();
		fwo.Free();
		Gripper.attachTo(kiwa.getFlange()); 	
		
		setRGB(false, true, false);		 
	}

	@Override
	public void run() {
		// your application execution starts here
		while (true) {
			switch (state) {
			case state_home:
				System.out.println("Going home.");  
				movePtpWithTorqueCondition(homeFramePath);
				
				//attach the tool if not already done
				setRGB(false, false, true);
				promptAns = getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, "Is the tool attached?", "Yes", "No");
				setRGB(false, true, false);
				System.out.println("Prompt ans : "+promptAns);
				if (promptAns==0){
					isAttachedTool = true;
				}else{
					isAttachedTool = false;
				}
				if (isAttachedTool == false) {
					isAttachedTool = true;
					plcout.setPinza_Chiudi(false);
					plcout.setPinza_Apri(true);
					ThreadUtil.milliSleep(1000);

					setRGB(false, false, true);
					System.out.println("Insert the tool.");
					kiwa.move(positionHold(ctrModeStiff, -1, TimeUnit.SECONDS).breakWhen(torqueBreakCondition1).breakWhen(torqueBreakCondition2).breakWhen(torqueBreakCondition3).breakWhen(torqueBreakCondition4).breakWhen(torqueBreakCondition5).breakWhen(torqueBreakCondition6).breakWhen(torqueBreakCondition7));
					plcout.setPinza_Apri(false);
					plcout.setPinza_Chiudi(true);
					ThreadUtil.milliSleep(1000);
					setRGB(false, true, false);
				}
				
				state = States.state_teach;
				break;

			case state_teach:
				System.out.println("In teaching mode."); 
				movePtpWithTorqueCondition(homeFramePath); 
				fwo.Free();
				myHandGuiding(); 
				System.out.println("Exiting teach."); 
				state = States.state_run;

				break;


			case state_run:
				System.out.println("Running."); 
				setRGB(false, true, false); 
				ThreadUtil.milliSleep(50);
				
				for (int i = 0; i < fwo.GetCounter(); i++) { 							// last saved frame is  Counter-1
					
					if (fwo.GetFrame(i).hasAdditionalParameter("pick Pin")) {
						pickPin(fwo.GetFrame(i)); 	 										// go to pick the pin
						while (true) {
							if (checkInsertion() == false){									// check if pin is there
								setRGB(true, false, false); 
								moveLinRelWithCollisionDetection(0,0,50);								// go back after failed insertion check 
								ThreadUtil.milliSleep(500); 
								while(!mfio.getUserButton()){
									ThreadUtil.milliSleep(20);
								} 
								setRGB(true, false, false); 
								reducedSpeed = true; 					// set to false after 1 run
								pickPin(fwo.GetFrame(i));  
							}else { 
								moveLinRelWithCollisionDetection(0,0,50);
								break;
							}
						}  
					} else if (fwo.GetFrame(i).hasAdditionalParameter("insert Pin")) {
						insertPin(fwo.GetFrame(i));				
						while (true) {
							if (checkInsertion() == false){									// check if pin is inserted
								setRGB(true, false, false); 
								moveLinRelWithCollisionDetection(0,50,0);								// go back after failed insertion check 
								ThreadUtil.milliSleep(500); 
								while(!mfio.getUserButton()){
									ThreadUtil.milliSleep(20);
								} 
								setRGB(true, false, false); 
								reducedSpeed = true; 					// set to false after 1 run
								insertPin(fwo.GetFrame(i)); 
							}else {
								twistPin();
								ThreadUtil.milliSleep(500);
								moveLinRelWithCollisionDetection(0,50,0);
								
								break;
							}
						}
						
					}else { 
						System.out.println("Going to Frame "+ i +".");
						movePtpWithTorqueCondition(fwo.GetFrame(i));
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
	
	private void pickPin(Frame targetFrame) { 
		System.out.println("Picking up a pin.");   
		Frame preTargetFrame = targetFrame.copy();
		preTargetFrame.setZ(preTargetFrame.getZ()+40);			// 40 mm above
		movePtpWithTorqueCondition (preTargetFrame);
		
		moveLinRelWithCollisionDetection(0,0,-40); 								// 45 mm downwards
		
	}
	
	private void insertPin(Frame targetFrame) { 
		System.out.println("Inserting pin.");   
		Frame preTargetFrame = targetFrame.copy();
		preTargetFrame.setY(preTargetFrame.getY()+40);			// 40 mm infront
		movePtpWithTorqueCondition (preTargetFrame);
		
		moveLinRelWithCollisionDetection(0,-40,0); 								// 40 mm towards
		
	}

	private void movePtpWithTorqueCondition (Frame nextFrame){		// overloading for taught points

		double prevSpeed = defSpeed;
		if (reducedSpeed) {
			defSpeed = 0.05;
			reducedSpeed = false;
		}
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
		defSpeed = prevSpeed;
	}
	
	private void movePtpWithTorqueCondition (String framePath){		// overloading for appData points
		
		double prevSpeed = defSpeed;
		if (reducedSpeed) {
			defSpeed = 0.05;
			reducedSpeed = false;
		}
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
		defSpeed = prevSpeed;
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
					System.out.println("Added Frame "+fwo.GetCounter()+".");
				}else if (buttonInputCheck == ButtonInput.double_click) {					// Record current position & Pick Pin
					Frame tempFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
					tempFrame.setAdditionalParameter("pick Pin", 1); 
					fwo.Add(tempFrame);
					System.out.println("Added Frame "+fwo.GetCounter()+"."); 
 					System.out.println("Pin will be picked up from this location.");
					
				}else if (buttonInputCheck == ButtonInput.triple_click) {					// Record current position &  Insert Pin
					Frame tempFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
					tempFrame.setAdditionalParameter("insert Pin", 1); 
					fwo.Add(tempFrame);
					System.out.println("Added Frame "+fwo.GetCounter()+"."); 
					System.out.println("Pin will be inserted in this location.");
					
				}else {
					//do nothing
				}

			}
		} 
	}
  
	private void enableTEACHbuttons() { 										// TEACH buttons
		IUserKeyBar keyBar2 = getApplicationUI().createUserKeyBar("TEACH");

		IUserKeyListener listener2 = new IUserKeyListener() {

			@Override
			public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				if (0 == key.getSlot()) { 						//KEY - DELETE PREVIOUS

					if (!keyAlreadyPressed && state == States.state_teach) {
						fwo.deleteLastFrame();
						keyAlreadyPressed = true;	 
						
					} else if (keyAlreadyPressed) {
						keyAlreadyPressed = false; 
					}
				}
				if (1 == key.getSlot()) {  						//KEY - TEACH MODE			 
					state = States.state_teach;
					System.out.println("Going to TEACH mode after this cycle.");
					
				}
				if (2 == key.getSlot()) {   					//KEY - OPEN GRIPPER 
					
					if (!keyAlreadyPressed) {
						
						Frame tempFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
						tempFrame.setAdditionalParameter("open Gripper", 1); 
						fwo.Add(tempFrame);
						System.out.println("Added Frame "+fwo.GetCounter()+"." );
			
						setRGB(false, false, false);
						ThreadUtil.milliSleep(150);
						setRGB(false, true, false);
						ThreadUtil.milliSleep(150);
						setRGB(false, true, true);
						
						positionHoldContainer.cancel(); 
//						VacuumBody.detach();
//						System.out.println("Workpiece Detached."); 
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
				if (3 == key.getSlot()) {   					//KEY - CLOSE GRIPPER
					
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
//							System.out.println("Workpiece Not Attached.");
							positionHoldContainer = kiwa.moveAsync(posHold); 
						}else { 
//							System.out.println("Workpiece Not Attached.");
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

	private void enableTORQUEbuttons() { 									// TORQUE/SPEED Buttons
		IUserKeyBar keyBar = getApplicationUI().createUserKeyBar("TRQ./SPD.");

		IUserKeyListener listener1 = new IUserKeyListener() {

			@Override
			public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				if (0 == key.getSlot()) {  						//KEY - TORQUE								
					//setting joint torque break condition
					promptAns = getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, "Set the maximum External Torque ", "5 Nm", "10 Nm", "15 Nm");  
					switch (promptAns) {
					case 0:
						maxTorque = 5.0;
						break;
					case 1:
						maxTorque = 10.0; 
						break;
					case 2:
						maxTorque = 15.0; 
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
					promptAns = getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, "Set the relative axes speed ", "5%", "15%", "25%");  
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
	
	private void setRGB(boolean r, boolean g, boolean b){
		mfio.setLEDRed(r);
		mfio.setLEDGreen(g);
		mfio.setLEDBlue(b);
	}
	 
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
				setRGB(true, false, true);				// turn red to indicate long press reached
			}
			ThreadUtil.milliSleep(50);
		}

		if (buttonChangeCount == 2 && pressedTime>7) {
//			System.out.println("Long button press. : "+ pressedTime * 100 +" ms."); 
			setRGB(R, G, B);
			return ButtonInput.long_press;
		}else if (buttonChangeCount == 2 && unpressedTime >7 ) {
//			System.out.println("Short button press."); 
			setRGB(R, G, B);
			return ButtonInput.short_press;
		}else if (buttonChangeCount == 4) {
//			System.out.println("Double button press."); 
			setRGB(R, G, B);
			return ButtonInput.double_click;
		}else if (buttonChangeCount == 6) {
//			System.out.println("Triple button press."); 
			setRGB(R, G, B);
			return ButtonInput.triple_click;
		}else {
			System.out.println("Invalid button Input."); 
			setRGB(R, G, B);
			return ButtonInput.invalid;
		}
		 
	}
 
	private boolean checkInsertion(){							 
		System.out.println("Checking Insertion");
		setRGB(false, true, true);
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
			System.out.println("Check 1 successful");
			setRGB(false, true, false);
			ThreadUtil.milliSleep(500); 
			kiwa.move(ptp(lastCheckpoint).setJointVelocityRel(0.02)); //Go back to the center of the hole
			ThreadUtil.milliSleep(500); 
			setRGB(false, true, true);
		}  
		if (checkPositive == false) {
			System.out.println("Check 1 unsuccessful");
			setRGB(true, false, false);
			ThreadUtil.milliSleep(500);
			kiwa.move(ptp(lastCheckpoint).setJointVelocityRel(0.02)); //Go back to the center of the hole
			ThreadUtil.milliSleep(500); 
			setRGB(true, false, false);
			return false;
		} else {
			  
			motionCmd = kiwa.move(linRel(5, 5, 0).setJointVelocityRel(0.02).breakWhen(torqueBreakCondition1).breakWhen(torqueBreakCondition2).breakWhen(torqueBreakCondition3).breakWhen(torqueBreakCondition4).breakWhen(torqueBreakCondition5).breakWhen(torqueBreakCondition6).breakWhen(torqueBreakCondition7)); 

			info = motionCmd.getFiredBreakConditionInfo(); 
			if (info != null) {
				System.out.println("Check 2 successful");
				setRGB(false, true, false);
				ThreadUtil.milliSleep(500); 
				kiwa.move(ptp(lastCheckpoint).setJointVelocityRel(0.02)); //Go back to the center of the hole
				ThreadUtil.milliSleep(500); 
				System.out.println("Insertion check successful");
				
				return true;
			}else{
				System.out.println("Check 2 unsuccessful");
				setRGB(true, false, false);
				ThreadUtil.milliSleep(500);
				kiwa.move(ptp(lastCheckpoint).setJointVelocityRel(0.02)); //Go back to the center of the hole
				ThreadUtil.milliSleep(500); 
				setRGB(true, false, false);
				return false;
			}
		}

	}
	
	private void twistPin(){

		System.out.println("Twisting the pin");
		setRGB(false, true, true);
		IMotionContainer motionCmd;
		JointTorqueCondition breakCondition;
		IFiredConditionInfo info;

		ThreadUtil.milliSleep(500); 

		breakCondition = new JointTorqueCondition(JointEnum.J7, -0.7, 0.7); 

		motionCmd = kiwa.move(linRel(Transformation.ofDeg(0, 0, 0, 60, 0, 0)).setJointVelocityRel(0.15).breakWhen(breakCondition));	// x,y,z,a,b,c
		info = motionCmd.getFiredBreakConditionInfo(); 
		if (info != null) {
			System.out.println("Cannot twist anymore");
			System.out.println("Current torque: " + kiwa.getExternalTorque().getSingleTorqueValue(JointEnum.J7));
			setRGB(true, true, true);
			ThreadUtil.milliSleep(500);
		}
		setRGB(false, true, false);
	}

	private Frame makeTargetFrame (double x, double y, double z) {
		Frame targetFrame, currentFrame;
		currentFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
		targetFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange()).setX(currentFrame.getX()+x).setY(currentFrame.getY()+y).setZ(currentFrame.getZ()+z);

		//System.out.println("Current coordinates: " + currentFrame.getX()+", "+currentFrame.getY()+", "+currentFrame.getZ());
		//System.out.println("Target coordinates: " + targetFrame.getX()+", "+targetFrame.getY()+", "+targetFrame.getZ()+", " + targetFrame.distanceTo(currentFrame));

		return targetFrame;
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

		goToFrame = makeTargetFrame(x, y, z); 
	
		ThreadUtil.milliSleep(500); 
		 
		motionCmd = kiwa.move(lin(goToFrame).setJointVelocityRel(0.05).breakWhen(breakCondition1).breakWhen(breakCondition2).breakWhen(breakCondition3).breakWhen(breakCondition4).breakWhen(breakCondition5).breakWhen(breakCondition6).breakWhen(breakCondition7));  
		info = motionCmd.getFiredBreakConditionInfo(); 
		while (info != null){ 
			setRGB(true, false, false);

			currentFrame = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
			if (Math.abs(currentFrame.getX() - goToFrame.getX()) < 7 && Math.abs(currentFrame.getY() - goToFrame.getY()) < 7 && Math.abs(currentFrame.getZ() - goToFrame.getZ()) < 7) {
				System.out.println("Pin/Hole found by Force detection.");	
				setRGB(false, false, true);
				ThreadUtil.milliSleep(1000); 
				break;
			} else {
				kiwa.move(linRel(0, 0, -30).setJointVelocityRel(0.05));		// go back after collision 
				ThreadUtil.milliSleep(500);  
				 
					while(!mfio.getUserButton()){
						ThreadUtil.milliSleep(20);
					} 
				
				setRGB(false, true, false);
				ThreadUtil.milliSleep(500);			// This is necessary, OW. the next breakConditions are again triggered 
				motionCmd = kiwa.move(lin(goToFrame).setJointVelocityRel(0.05).breakWhen(breakCondition1).breakWhen(breakCondition2).breakWhen(breakCondition3).breakWhen(breakCondition4).breakWhen(breakCondition5).breakWhen(breakCondition6).breakWhen(breakCondition7)); 
				info = motionCmd.getFiredBreakConditionInfo();
			}

		}
//		System.out.println("Position reached.");
		setRGB(false, true, false); 
	}


}
