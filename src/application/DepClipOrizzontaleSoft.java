package application;


import javax.inject.Inject;

import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

import com.kuka.roboticsAPI.conditionModel.JointTorqueCondition;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR;
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
public class DepClipOrizzontaleSoft extends RoboticsAPIApplication {
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
		
		// Tool
		tool1 = (Tool)getApplicationData().createFromTemplate("ToolElectrType1");
		tool1.attachTo(lbr_iiwa_14_R820_1.getFlange());
		
		tool2 = (Tool)getApplicationData().createFromTemplate("ToolElectrType2");
		tool2.attachTo(lbr_iiwa_14_R820_1.getFlange());
		
		jointSoftMode = new JointImpedanceControlMode(100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 2000.0);
		jointSoftMode.setStiffness(3000.0, 3000.0, 3000.0, 3000.0, 3000.0, 3000.0, 200.0);
	}

	@Override
	public void run() 
	{
		TorqueEvaluator test = new TorqueEvaluator(lbr_iiwa_14_R820_1);
			
	    test.setTorqueMeasured(true);
		test.startEvaluation();
				
		// your application execution starts here
		lbr_iiwa_14_R820_1.move(ptp(getApplicationData().getFrame("/Prove/AvvPunto4")));
		tool2.move(lin(getApplicationData().getFrame("/Modello2/DepositoOR")).setCartVelocity(200));
				
		TorqueStatistic restorque = test.stopEvaluation();
	}
}