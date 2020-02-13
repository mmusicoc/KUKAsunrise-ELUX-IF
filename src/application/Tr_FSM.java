package application;


import javax.inject.Inject;
 

import com.kuka.common.ThreadUtil;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
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
public class Tr_FSM extends RoboticsAPIApplication {
	@Inject 							private Plc_inputIOGroup 		plcin;
	@Inject 							private Plc_outputIOGroup 		plcout;
	@Inject 							private MediaFlangeIOGroup 		mfio;
	
	@Inject
	private LBR kiwa;
	private enum States {state_home, state_1, state_2, state_3};
	private States state; 
	private double defRelSpeed;
	
	

	@Override
	public void initialize() {
		// initialize your application here
		System.out.println("Initializing..");
		kiwa.setHomePosition(getApplicationData().getFrame("/Rest"));
		defRelSpeed = 0.15;
		state = States.state_home;
		
	}

	@Override
	public void run() {
		// your application execution starts here
		kiwa.move(ptpHome());
		
		while (true) {
			switch (state) {
			case state_home:
					System.out.println("In state HOME.");
					mfio.setLEDBlue(true);
					plcout.setPinza_Apri(false);
					ThreadUtil.milliSleep(1500);
					plcout.setPinza_Chiudi(true);
					ThreadUtil.milliSleep(1500);
					while (!mfio.getUserButton()) {
						ThreadUtil.milliSleep(50);
					}
					state = States.state_1;
				break;
				
			case state_1:
				System.out.println("In state 1.");
				mfio.setLEDRed(true);
				ThreadUtil.milliSleep(1500);
				while (!mfio.getUserButton()) {
					ThreadUtil.milliSleep(50);
				}
				state = States.state_2; 
				break;
				
			case state_2: 
				System.out.println("In state 2.");
				mfio.setLEDGreen(true);
				ThreadUtil.milliSleep(1500);
				while (!mfio.getUserButton()) {
					ThreadUtil.milliSleep(50);
				}
				state = States.state_3; 
				break;
				
			case state_3:
				System.out.println("In state 3.");
				mfio.setLEDBlue(false);
				mfio.setLEDRed(false);
				mfio.setLEDGreen(false);

				ThreadUtil.milliSleep(1500);
				plcout.setPinza_Chiudi(false);
				ThreadUtil.milliSleep(1500);
				plcout.setPinza_Apri(true);
				while (!mfio.getUserButton()) {
					ThreadUtil.milliSleep(50);
				}
				state = States.state_home; 
				break;

			default:
				break;
			}
		}
	}
}