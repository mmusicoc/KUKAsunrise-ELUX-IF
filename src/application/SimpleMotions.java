package application;


import javax.inject.Inject;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.deviceModel.LBRE1Redundancy;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.motionModel.LIN;
import com.kuka.roboticsAPI.motionModel.PTP;
import com.kuka.roboticsAPI.motionModel.Spline;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;
import com.sun.org.apache.bcel.internal.generic.NEW;

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
public class SimpleMotions extends RoboticsAPIApplication {
	@Inject
	private LBR lbr;
	
	private final static String informationText = "Move to start Position? \n";
	private Frame startFrame;

	@Override
	public void initialize() {
		// initialize your application here
		System.out.println("Starting simple motions");
		startFrame = lbr.getCurrentCartesianPosition(lbr.getFlange());
		
		
	}

	@Override
	public void run() {
		// your application execution starts here
		System.out.println("Moving to home");
		lbr.move(ptpHome().setJointVelocityRel(0.25));
		
		int continue_process = getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, informationText, "OK" , "Cancel"); 
		
		if (continue_process == 0) {
			System.out.println("Moving back to original position");
			lbr.move(ptp(startFrame).setJointVelocityRel(0.25));
		}
		
		return;	
	}
}