package application;

import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.common.ThreadUtil;
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
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;

public class Tr3_CollisionDetection extends RoboticsAPIApplication {
	@Inject 	private Plc_inputIOGroup 		plcin;
	@Inject 	private Plc_outputIOGroup 		plcout;
	@Inject 	private MediaFlangeIOGroup 		mfio;
	@Inject		@Named("Pinza") 	private Tool Gripper;
	@Inject 	private LBR kiwa;
	
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
		int torqueAdjust = getApplicationUI()
							.displayModalDialog(ApplicationDialogType.QUESTION, 
													"Set the torque max value", 
													"5 Nm", "10 Nm", "20 Nm");
		switch (torqueAdjust) {
			case 0:
				maxTorque = 5.0;
				break;
			case 1:
				maxTorque = 10.0;
				break;
			case 2:
				maxTorque = 20.0;
				break;
			default:
				break;
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
		ObjectFrame nextFrame = getApplicationData().getFrame("/Training/P1");
		moveWithCollisionDetection(nextFrame);
		ThreadUtil.milliSleep(1500);
		nextFrame = getApplicationData().getFrame("/Training/P2");
		moveWithCollisionDetection (nextFrame);
	}
	
	private void moveWithCollisionDetection (ObjectFrame destinationFrame){
		// 2. Transfering the Break Condition to the Motion Command
		moveCmdContainer = kiwa.move(ptp(destinationFrame)
							.setJointVelocityRel(defSpeed)
							.breakWhen(jointTorqueBreakConditions)); 
		// 3. Evaluating the fired Break Condition
		IFiredConditionInfo breakInfo = moveCmdContainer.getFiredBreakConditionInfo();
		while (breakInfo != null) {
			System.out.println("Collision detected." ); 
			while(!mfio.getUserButton()){		// Keep the robot in pause until the User button is pressed
				ThreadUtil.milliSleep(20);
			} 
			ThreadUtil.milliSleep(500);			// This is necessary so the user does not retrigger when pressing, OW. the next breakConditions are again triggered
			moveCmdContainer = kiwa.move(ptp(destinationFrame)
								.setJointVelocityRel(defSpeed)
								.breakWhen(jointTorqueBreakConditions)); 
			breakInfo = moveCmdContainer.getFiredBreakConditionInfo();
		}		
	}
}