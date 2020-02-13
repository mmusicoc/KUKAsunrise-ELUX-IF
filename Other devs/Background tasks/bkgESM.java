package backgroundTask;

import java.util.concurrent.TimeUnit;


import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.uiModel.userKeys.*;
import com.kuka.task.ITaskLogger;
import javax.inject.Inject;
 
public class bkgESM extends RoboticsAPICyclicBackgroundTask {
	private Controller kukaController;
	private LBR kiwa;
	@Inject 	private Plc_inputIOGroup 		plcin;
	@Inject 	private Plc_outputIOGroup 		plcout;
	@Inject 	private MediaFlangeIOGroup 		mfio;
	@Inject		private ITaskLogger 			logger;
	
	public void initialize() { 
		initializeCyclic(0, 500, TimeUnit.MILLISECONDS,	CycleBehavior.BestEffort);
		switchESMStates();
	}

	public void runCyclic() {}

	private void switchESMStates() {
		IUserKeyBar keyBar = getApplicationUI().createUserKeyBar("GRIPPER");
		IUserKeyListener btnListener = new IUserKeyListener() {
			@Override 
			public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				if (key.getSlot() == 0) {
					plcout.setPinza_Chiudi(false);
					plcout.setPinza_Apri(true);
				}				
				else if (key.getSlot() == 1) {
					plcout.setPinza_Apri(false); 
					plcout.setPinza_Chiudi(true);
				}
				else if (key.getSlot() == 2) {
					//					kiwa.setESMState("3");
				}
				else if (key.getSlot() == 3) {
					//					kiwa.setESMState("4");;
				}
			}
		};

		IUserKey button0 = keyBar.addUserKey(0, btnListener, true);
		IUserKey button1 = keyBar.addUserKey(1, btnListener, true);
		IUserKey button2 = keyBar.addUserKey(2, btnListener, true);
		IUserKey button3 = keyBar.addUserKey(3, btnListener, true);

		button0.setText(UserKeyAlignment.TopMiddle, "Gripper OPEN"); 
		button1.setText(UserKeyAlignment.TopMiddle, "Gripper CLOSE"); 
		button2.setText(UserKeyAlignment.TopMiddle, "...");
		button3.setText(UserKeyAlignment.TopMiddle, "...");

		keyBar.publish();
	}
}
