package application;


import javax.inject.Inject;
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
import com.kuka.roboticsAPI.motionModel.IMotionContainer;

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
public class Tr_CollisionDetection extends RoboticsAPIApplication {

	@Inject 							private Plc_inputIOGroup 		plcin;
	@Inject 							private Plc_outputIOGroup 		plcout;
	@Inject 							private MediaFlangeIOGroup 		mfio;
	
	@Inject
	private LBR kiwa;
	
	private double defSpeed = 0.15;
	private double maxTorque = 10.0;
	private JointTorqueCondition torqueBreakCondition1, torqueBreakCondition2, torqueBreakCondition3, torqueBreakCondition4, torqueBreakCondition5, torqueBreakCondition6, torqueBreakCondition7 ;					// for torque break condition
	private ICondition jointTorqueBreakConditions ; 
	private IMotionContainer moveCmdContainer;
	@Override
	public void initialize() {
		// initialize your application here
		
		torqueBreakCondition1 = new JointTorqueCondition(JointEnum.J1, -maxTorque, maxTorque);	
		torqueBreakCondition2 = new JointTorqueCondition(JointEnum.J2, -maxTorque, maxTorque);
		torqueBreakCondition3 = new JointTorqueCondition(JointEnum.J3, -maxTorque, maxTorque);	
		torqueBreakCondition4 = new JointTorqueCondition(JointEnum.J4, -maxTorque, maxTorque);
		torqueBreakCondition5 = new JointTorqueCondition(JointEnum.J5, -maxTorque, maxTorque);	
		torqueBreakCondition6 = new JointTorqueCondition(JointEnum.J6, -maxTorque, maxTorque);
		torqueBreakCondition7 = new JointTorqueCondition(JointEnum.J7, -maxTorque, maxTorque);
		jointTorqueBreakConditions = torqueBreakCondition1.or(torqueBreakCondition2).or(torqueBreakCondition3).or(torqueBreakCondition4).or(torqueBreakCondition5).or(torqueBreakCondition6).or(torqueBreakCondition7);
	}

	@Override
	public void run() {
		// your application execution starts here
		
    ObjectFrame nextFrame = getApplicationData().getFrame("/Training/P1");
    moveWithCollisionDetection (nextFrame);
    
    ThreadUtil.milliSleep(1500);
    
    nextFrame = getApplicationData().getFrame("/Training/P2");
    moveWithCollisionDetection (nextFrame);
    
		
	}
	
	private void moveWithCollisionDetection (ObjectFrame destinationFrame){
		
		moveCmdContainer = kiwa.move(ptp(destinationFrame).setJointVelocityRel(defSpeed).breakWhen(jointTorqueBreakConditions)); 
		
		IFiredConditionInfo breakInfo = moveCmdContainer.getFiredBreakConditionInfo();
		while (breakInfo != null) {
			System.out.println("Collision detected." ); 
			while(!mfio.getUserButton()){
				ThreadUtil.milliSleep(20);
			} 
			ThreadUtil.milliSleep(500);			// This is necessary, OW. the next breakConditions are again triggered
			
			moveCmdContainer = kiwa.move(ptp(destinationFrame).setJointVelocityRel(defSpeed).breakWhen(jointTorqueBreakConditions)); 
			breakInfo = moveCmdContainer.getFiredBreakConditionInfo();
		}		
	}
}