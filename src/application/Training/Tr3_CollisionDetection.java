package application.Training;

import static utils.Utils.*;
import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.conditionModel.ICondition;
import com.kuka.roboticsAPI.conditionModel.JointTorqueCondition;
import com.kuka.roboticsAPI.deviceModel.JointEnum;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.executionModel.IFiredConditionInfo;
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;

public class Tr3_CollisionDetection extends RoboticsAPIApplication {
	@Inject private LBR 					kiwa;
	@Inject private MediaFlangeIOGroup 		mfio;
	@Inject	@Named("Pinza") private Tool 	Gripper;
	
	private double defSpeed = 0.15;
	private double maxTorque = 10.0;
	private JointTorqueCondition torqueBreakCondition1,
								 torqueBreakCondition2,
								 torqueBreakCondition3,
								 torqueBreakCondition4,
								 torqueBreakCondition5,
								 torqueBreakCondition6,
								 torqueBreakCondition7;		// for torque based break condition
	private ICondition jointTorqueBreakConditions; 
	private IMotionContainer moveCmdContainer;
	
	@Override
	public void initialize() {
		Gripper.attachTo(kiwa.getFlange()); 	
		int promptAns = padQuestion("Set the torque max value", "5 Nm", "10 Nm", "20 Nm");
		switch (promptAns) {
			case 0: maxTorque = 5.0; break;
			case 1: maxTorque = 10.0; break;
			case 2: maxTorque = 20.0; break;
			default: break;
		}
		
		// 1. Creating break conditions
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
	}

	@Override
	public void run() {
		ObjectFrame nextFrame;
		setRGB("B");
		for (;;) {
			nextFrame = getApplicationData().getFrame("/Training/P1");
			moveWithCollisionDetection(nextFrame);
			waitMillis(1500, true);
			nextFrame = getApplicationData().getFrame("/Training/P2");
			moveWithCollisionDetection(nextFrame);
			waitMillis(1500, true);
		}
	}
	
	private void moveWithCollisionDetection (ObjectFrame destinationFrame){
		// 2. Transfering the Break Condition to the Motion Command
		moveCmdContainer = kiwa.move(ptp(destinationFrame)
							.setJointVelocityRel(defSpeed)
							.breakWhen(jointTorqueBreakConditions));
		
		// 3. Evaluating the fired Break Condition
		IFiredConditionInfo breakInfo = moveCmdContainer.getFiredBreakConditionInfo();
		while (breakInfo != null) {
			setRGB("R");
			padLog("Collision detected." );
			waitUserButton();
			setRGB("B");
			moveCmdContainer = kiwa.move(ptp(destinationFrame)
								.setJointVelocityRel(defSpeed)
								.breakWhen(jointTorqueBreakConditions)); 
			breakInfo = moveCmdContainer.getFiredBreakConditionInfo();
		}		
	}
	
	/**********************************************
	* CUSTOM METHODS BY mario.musico@electrolux.com <p>
	***********************************************/
	
	public void waitUserButton() {
		padLog("Press USER GREEN BUTTON to continue");
		while (true) {
			if (mfio.getUserButton()) break;
			waitMillis(50);
		}
		waitMillis(500, false);		// Wait for torque to stabilize
	}
	
	public void setRGB(boolean r, boolean g, boolean b) {
		mfio.setLEDRed(r);
		mfio.setLEDGreen(g);
		mfio.setLEDBlue(b);
	}

	public void setRGB(String color, boolean log) {
		if (log) padLog("MediaFlange LED ring to " + color);
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
	
	public int padQuestion(String question, String ans1, String ans2){
		return getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, question, ans1, ans2); }
	public int padQuestion(String question, String ans1, String ans2, String ans3){
		return getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, question, ans1, ans2, ans3); }
	public int padQuestion(String question, String ans1, String ans2, String ans3, String ans4){
		return getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, question, ans1, ans2, ans3, ans4);
	}
}