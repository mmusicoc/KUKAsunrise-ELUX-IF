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
public class Tr_HandGuiding extends RoboticsAPIApplication {
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
	
	private CartesianImpedanceControlMode ctrMode = new CartesianImpedanceControlMode();  	// for agile handguiding 
	private PositionHold posHold = new PositionHold(ctrMode, -1, null);  
	private IMotionContainer positionHoldContainer;		//positionHoldContainer- for agile posHold;   
	private boolean isMoving = false;
	@Override
	public void initialize() {
		// initialize your application here
		// setting the Stiffness in HandGuiding mode
				ctrMode.parametrize(CartDOF.TRANSL).setStiffness(0.1).setDamping(1);			// agile handguiding
				ctrMode.parametrize(CartDOF.ROT).setStiffness(0.1).setDamping(1);  

				Gripper.attachTo(kiwa.getFlange()); 
				setRGB(false, false, true);

	}

	@Override
	public void run() {
		while (true) {
			if (mfio.getUserButton()) {
				positionHoldContainer = kiwa.moveAsync(posHold); 
				isMoving = true;
				setRGB(false, true, false);
			}
			ThreadUtil.milliSleep(100);

		}
		// your application execution starts here
//		System.out.println("Press USER button to START agile handguiding.");
//		setRGB(false, true, false);
//		while (true) {
//			ThreadUtil.milliSleep(50); 
//			if ( mfio.getUserButton()) {
//				ThreadUtil.milliSleep(500);
//				System.out.println("Starting agile handguiding"); 
//				setRGB(false, false, true);
//				positionHoldContainer = kiwa.moveAsync(posHold); 
//
//				System.out.println("Press USER button to STOP handguiding.");
//				break;
//			}
//		}
 	 
		
//		while (true) {
//			ThreadUtil.milliSleep(50);
//			// points are recorded through the buttons
//			if ( mfio.getUserButton()) { 
//				ThreadUtil.milliSleep(500);
//
//				System.out.println("agile handguiding stopped."); 
//					positionHoldContainer.cancel(); 
//					setRGB(false, true, false);
//					break;
//					 
//		} 
//	}
	}
	private void setRGB(boolean r, boolean g, boolean b){
		mfio.setLEDRed(r);
		mfio.setLEDGreen(g);
		mfio.setLEDBlue(b);
	}
	
}