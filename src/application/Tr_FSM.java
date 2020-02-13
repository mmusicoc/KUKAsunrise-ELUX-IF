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
	private enum States {state_A, state_B, state_C, state_D};
	private States state; 
	private double defRelSpeed;
	
	

	@Override
	public void initialize() {
		// initialize your application here
		System.out.println("Initializing..");
		kiwa.setHomePosition(getApplicationData().getFrame("/Rest"));
		defRelSpeed = 0.15;
		state = States.state_A;
		
	}

	@Override
	public void run() {
		// your application execution starts here
		kiwa.move(ptpHome());
		
		while (true) {
			switch (state) {
			case state_A:
					System.out.println("Current state : state A");
					mfio.setLEDBlue(true);
					plcout.setPinza_Apri(false);
					ThreadUtil.milliSleep(1500);
					plcout.setPinza_Chiudi(true);
					ThreadUtil.milliSleep(1500);
						System.out.println("Press green button to continue");
					while (!mfio.getUserButton()) {
						ThreadUtil.milliSleep(50);
					}
					state = States.state_B;
				break;
				
			case state_B:
				System.out.println("Current state : state B");
				mfio.setLEDRed(true);
				ThreadUtil.milliSleep(1500);
				System.out.println("Press green button to continue");
				while (!mfio.getUserButton()) {
					ThreadUtil.milliSleep(50);
				}
				state = States.state_C; 
				break;
				
			case state_C: 
				System.out.println("Current state : state C");
				mfio.setLEDGreen(true);
				ThreadUtil.milliSleep(1500);
				System.out.println("Press green button to continue");
				while (!mfio.getUserButton()) {
					ThreadUtil.milliSleep(50);
				}
				state = States.state_D; 
				break;
				
			case state_D:
				System.out.println("Current state : state D");
				mfio.setLEDBlue(false);
				mfio.setLEDRed(false);
				mfio.setLEDGreen(false);

				ThreadUtil.milliSleep(1500);
				plcout.setPinza_Chiudi(false);
				ThreadUtil.milliSleep(1500);
				plcout.setPinza_Apri(true);
				System.out.println("Press green button to continue");
				while (!mfio.getUserButton()) {
					ThreadUtil.milliSleep(50);
				}
				state = States.state_A; 
				break;

			default:
				break;
			}
		}
	}
}