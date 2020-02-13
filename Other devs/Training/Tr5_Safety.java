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

public class Tr5_Safety extends RoboticsAPIApplication {
	@Inject		private LBR kiwa;
	@Inject 	private Plc_inputIOGroup 		plcin;
	@Inject 	private Plc_outputIOGroup 		plcout;
	@Inject 	private MediaFlangeIOGroup 		mfio;
	@Inject		@Named("Pinza")			private Tool Gripper;
	@Inject		@Named("VacuumBody")	private Workpiece VacuumBody;
	 
	private double maxTorque = 10.0;
	private double defSpeed = 0.15; 
	private Frame nextFrame;
	private String homeFramePath;
	private int promptAns;
  
	private JointTorqueCondition torqueBreakCondition1,
								 torqueBreakCondition2,
								 torqueBreakCondition3, 
								 torqueBreakCondition4, 
								 torqueBreakCondition5, 
								 torqueBreakCondition6, 
								 torqueBreakCondition7;		// for torque break condition
	private IFiredConditionInfo info;
	private IMotionContainer moveCmdContainer;	
	
	@Override
	public void initialize() {		
		//setting joint torque break condition 
		torqueBreakCondition1 = new JointTorqueCondition(JointEnum.J1, -maxTorque, maxTorque);	
		torqueBreakCondition2 = new JointTorqueCondition(JointEnum.J2, -maxTorque, maxTorque);
		torqueBreakCondition3 = new JointTorqueCondition(JointEnum.J3, -maxTorque, maxTorque);	
		torqueBreakCondition4 = new JointTorqueCondition(JointEnum.J4, -maxTorque, maxTorque);
		torqueBreakCondition5 = new JointTorqueCondition(JointEnum.J5, -maxTorque, maxTorque);	
		torqueBreakCondition6 = new JointTorqueCondition(JointEnum.J6, -maxTorque, maxTorque);
		torqueBreakCondition7 = new JointTorqueCondition(JointEnum.J7, -maxTorque, maxTorque);
			
		System.out.println("Initializing...");
		homeFramePath = "/Training"; 
		kiwa.setHomePosition(getApplicationData().getFrame(homeFramePath));
		enableTORQUEbuttons();  
		Gripper.attachTo(kiwa.getFlange()); 	
		
		setRGB(false, false, true);		
		System.out.println("Press USER button to start.");
		while (true) {									// Just for test
			if (mfio.getUserButton()) {
				break;
			}
			ThreadUtil.milliSleep(50);
		}
		setRGB(false, true, false);
	}

	@Override
	public void run() { 
		while (true) {
			movePtpWithTorqueCondition("/Training");
			ThreadUtil.milliSleep(250);
			movePtpWithTorqueCondition("/Training/P1");
			ThreadUtil.milliSleep(250);
			movePtpWithTorqueCondition("/Training/P2");
			ThreadUtil.milliSleep(250);
			movePtpWithTorqueCondition("/Training/P3");
			ThreadUtil.milliSleep(250);
		}
	}
	
	private void setRGB(boolean r, boolean g, boolean b){
		mfio.setLEDRed(r);
		mfio.setLEDGreen(g);
		mfio.setLEDBlue(b);
	}
	
	private void enableTORQUEbuttons() { 									// TORQUE/SPEED Buttons
		IUserKeyBar keyBar = getApplicationUI().createUserKeyBar("TRQ./SPD.");
		IUserKeyListener listener1 = new IUserKeyListener() {
			@Override
			public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				if (0 == key.getSlot()) {  		//KEY - TORQUE								
					//setting joint torque break condition
					promptAns = getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION,
																			"Set the maximum External Torque ", 
																			"5 Nm", "10 Nm", "20 Nm", "30 Nm");  
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

					moveCmdContainer.cancel();
					System.out.println("Max Torque set to "+maxTorque+" Nm.");
				}
				
				if (1 == key.getSlot()) {  						//KEY - SPEED
					promptAns = getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, 
																			"Set the relative axes speed ", 
																			"10%", "25%", "50%", "100%");  
					switch (promptAns) {
						case 0:
							defSpeed = 0.10; 
							break;
						case 1:
							defSpeed = 0.25; 
							break;
						case 2:
							defSpeed = 0.50; 
							break;
						case 3:
							defSpeed = 1.0; 
							break;
						default:
							break;
					}
					moveCmdContainer.cancel();
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

	private void movePtpWithTorqueCondition (Frame nextFrame){		// overloading for taught points
		moveCmdContainer = kiwa.move(ptp(nextFrame)
										.setJointVelocityRel(defSpeed)
										.breakWhen(torqueBreakCondition1)
										.breakWhen(torqueBreakCondition2)
										.breakWhen(torqueBreakCondition3)
										.breakWhen(torqueBreakCondition4)
										.breakWhen(torqueBreakCondition5)
										.breakWhen(torqueBreakCondition6)
										.breakWhen(torqueBreakCondition7)); 
		info = moveCmdContainer.getFiredBreakConditionInfo();
		while (info != null) {
			System.out.println("Collision detected . " ); 
			setRGB(true, false, false);
			while(!mfio.getUserButton()){
				ThreadUtil.milliSleep(20);
			} 
			ThreadUtil.milliSleep(500);			// This is necessary, OW. the next breakConditions are again triggered
			setRGB(false, true, false);
			moveCmdContainer = kiwa.move(ptp(nextFrame)
											.setJointVelocityRel(defSpeed)
											.breakWhen(torqueBreakCondition1)
											.breakWhen(torqueBreakCondition2)
											.breakWhen(torqueBreakCondition3)
											.breakWhen(torqueBreakCondition4)
											.breakWhen(torqueBreakCondition5)
											.breakWhen(torqueBreakCondition6)
											.breakWhen(torqueBreakCondition7)); 
			info = moveCmdContainer.getFiredBreakConditionInfo();
		}
	}
	
	private void movePtpWithTorqueCondition (String framePath){		// overloading for appData points
		moveCmdContainer = kiwa.move(ptp(getApplicationData().getFrame(framePath))
										.setJointVelocityRel(defSpeed)
										.breakWhen(torqueBreakCondition1)
										.breakWhen(torqueBreakCondition2)
										.breakWhen(torqueBreakCondition3)
										.breakWhen(torqueBreakCondition4)
										.breakWhen(torqueBreakCondition5)
										.breakWhen(torqueBreakCondition6)
										.breakWhen(torqueBreakCondition7)); 
		info = moveCmdContainer.getFiredBreakConditionInfo();
		while (info != null) {
			System.out.println("Collision detected . " ); 
			setRGB(true, false, false);
			while(!mfio.getUserButton()){
				ThreadUtil.milliSleep(20);
			} 
			ThreadUtil.milliSleep(500);			// This is necessary, OW. the next breakConditions are again triggered
			setRGB(false, true, false);
			moveCmdContainer = kiwa.move(ptp(getApplicationData().getFrame(framePath))
											.setJointVelocityRel(defSpeed)
											.breakWhen(torqueBreakCondition1)
											.breakWhen(torqueBreakCondition2)
											.breakWhen(torqueBreakCondition3)
											.breakWhen(torqueBreakCondition4)
											.breakWhen(torqueBreakCondition5)
											.breakWhen(torqueBreakCondition6)
											.breakWhen(torqueBreakCondition7)); 
			info = moveCmdContainer.getFiredBreakConditionInfo();
		}
	}
}