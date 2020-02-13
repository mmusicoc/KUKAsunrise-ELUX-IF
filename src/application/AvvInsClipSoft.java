package application;


import javax.inject.Inject;

import com.kuka.common.ThreadUtil;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

import com.kuka.roboticsAPI.conditionModel.ConditionObserver;
import com.kuka.roboticsAPI.conditionModel.IAnyEdgeListener;
import com.kuka.roboticsAPI.conditionModel.ICondition;
import com.kuka.roboticsAPI.conditionModel.JointTorqueCondition;
import com.kuka.roboticsAPI.conditionModel.NotificationType;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.controllerModel.sunrise.ResumeMode;
import com.kuka.roboticsAPI.controllerModel.sunrise.SunriseExecutionService;
import com.kuka.roboticsAPI.deviceModel.JointEnum;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.motionModel.controlModeModel.JointImpedanceControlMode;
import com.kuka.roboticsAPI.sensorModel.TorqueEvaluator;
import com.kuka.roboticsAPI.sensorModel.TorqueStatistic;

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
public class AvvInsClipSoft extends RoboticsAPIApplication {
	@Inject
	private Controller kuka_Sunrise_Cabinet_1;
	private LBR lbr_iiwa_14_R820_1;
	
	// Tool	
	private Tool tool1;
	private Tool tool2;
	
	private TorqueEvaluator test; 
	
	public JointImpedanceControlMode jointSoftMode;

	@Override
	public void initialize() 
	{
		// initialize your application here
		kuka_Sunrise_Cabinet_1 = getController("KUKA_Sunrise_Cabinet_1");
		lbr_iiwa_14_R820_1 = (LBR) getDevice(kuka_Sunrise_Cabinet_1,
				"LBR_iiwa_14_R820_1");
		
		tool1 = (Tool)getApplicationData().createFromTemplate("ToolElectrType1");
		tool1.attachTo(lbr_iiwa_14_R820_1.getFlange());
		
		tool2 = (Tool)getApplicationData().createFromTemplate("ToolElectrType2");
		tool2.attachTo(lbr_iiwa_14_R820_1.getFlange());
		
		test = new TorqueEvaluator(lbr_iiwa_14_R820_1); 
				
		jointSoftMode = new JointImpedanceControlMode(100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 2000.0);
		jointSoftMode.setStiffness(3000.0, 3000.0, 3000.0, 3000.0, 3000.0, 3000.0, 200.0);
	}

	@Override
	public void run() 
	{     
		test.setTorqueMeasured(true);
		test.startEvaluation();
		
		lbr_iiwa_14_R820_1.moveAsync(ptp(getApplicationData().getFrame("/PrelievoOR/AvvPrel1")).setJointVelocityRel(1.0).setBlendingCart(100));
		lbr_iiwa_14_R820_1.move(ptp(getApplicationData().getFrame("/PrelievoOR/PrelOR")).setJointVelocityRel(1.0));
									
		ThreadUtil.milliSleep(2000);
		
		tool2.moveAsync(ptp(getApplicationData().getFrame("/Modello10/Avv1")).setJointVelocityRel(0.7).setBlendingCart(50));
     	tool2.moveAsync(ptp(getApplicationData().getFrame("/Modello10/Avv2")).setJointVelocityRel(1.0).setBlendingCart(50));//.setMode(jointSoftMode));
     	tool2.moveAsync(lin(getApplicationData().getFrame("/Modello10/Avv3")).setCartVelocity(200).setBlendingCart(20));
     	tool2.move(lin(getApplicationData().getFrame("/Modello10/DepositoOR")).setCartVelocity(100).setJointAccelerationRel(0.4));//.setMode(jointSoftMode));
     	
	}
}