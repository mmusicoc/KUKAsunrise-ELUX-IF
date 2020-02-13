package application;


import javax.inject.Inject;
import javax.inject.Named;

import sun.awt.windows.ThemeReader;

import com.kuka.common.ThreadUtil;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.CartDOF;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.motionModel.PositionHold;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;

public class Tr4_CompliantRobot extends RoboticsAPIApplication {
	@Inject		private LBR kiwa;
	@Inject	 	@Named("Pinza")		private Tool Gripper;
	@Inject 	private Plc_inputIOGroup 		plcin;
	@Inject 	private Plc_outputIOGroup 		plcout;
	@Inject 	private MediaFlangeIOGroup 		mfio;
	
	// 1. Create a Cartesian Impedance controller for compliance mode
	private CartesianImpedanceControlMode ctrMode = new CartesianImpedanceControlMode(); 
	// 2. Create a motion command PositionHold
	private PositionHold posHold = new PositionHold(ctrMode, -1, null); 
	// 3. Create a MotionContainer for the PositionHold motion 
	private IMotionContainer positionHoldContainer;
	
	@Override
	public void initialize() {
		
		// 4. Attach the Gripper (Tool object) with accurately determined load data !! 
		Gripper.attachTo(kiwa.getFlange()); 
		// 5. Paramaterize the Cartesian Impediance controller (see Chapter 18.8.1)
		ctrMode.parametrize(CartDOF.TRANSL).setStiffness(500).setDamping(1);	// compliance mode paramaters
		ctrMode.parametrize(CartDOF.ROT).setStiffness(100).setDamping(1);   
	}

	@Override
	public void run() {
		kiwa.move(ptpHome());					// go to home position
		while(!mfio.getUserButton()) {			// wait until user button is pressed
			ThreadUtil.milliSleep(50);
		}
		// 6. Execute the PositionHold motion command using moveAsync() 
		positionHoldContainer = kiwa.moveAsync(posHold);	// start position hold
		
		while(true) {							// stay in hold position
			ThreadUtil.milliSleep(50);
			if (mfio.getUserButton()) {							// if button pressed, exit hold position
				positionHoldContainer.cancel();
				break;
			}
		}
	}
}