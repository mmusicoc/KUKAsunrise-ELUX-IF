package application;


import javax.inject.Inject;

import com.kuka.common.ThreadUtil;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.deviceModel.LBR;

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
public class Tr_basicMotions extends RoboticsAPIApplication {
	@Inject
	private LBR kiwa;
	private double defRelSpeed = 0.0;

	@Override
	public void initialize() {
		// initialize your application here
		System.out.println("Initializing..");
		defRelSpeed = 0.15;
		kiwa.setHomePosition(getApplicationData().getFrame("/Rest"));
	}

	@Override
	public void run() {
		// your application execution starts here
		kiwa.move(ptpHome());
		// fcghcfg
		kiwa.move(ptp(getApplicationData().getFrame("/Training/P2")).setJointJerkRel(defRelSpeed));
		ThreadUtil.milliSleep(1000);
		kiwa.move(lin(getApplicationData().getFrame("/Training/P1")).setJointJerkRel(defRelSpeed));
		ThreadUtil.milliSleep(1000);
		kiwa.move(circ(getApplicationData().getFrame("/Training/P2"), getApplicationData().getFrame("/Rest")).setJointJerkRel(defRelSpeed));
		
		return;
	}
}