package backgroundTask;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.inject.Inject;

import com.kuka.common.ThreadUtil;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKey;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyBar;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyListener;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyAlignment;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyEvent;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyLED;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyLEDSize;
import com.kuka.task.ITaskLogger;

/**
 * Implementation of a cyclic background task.
 * <p>
 * It provides the {@link RoboticsAPICyclicBackgroundTask#runCyclic} method
 * which will be called cyclically with the specified period.<br>
 * Cycle period and initial delay can be set by calling
 * {@link RoboticsAPICyclicBackgroundTask#initializeCyclic} method in the
 * {@link RoboticsAPIBackgroundTask#initialize()} method of the inheriting
 * class.<br>
 * The cyclic background task can be terminated via
 * {@link RoboticsAPICyclicBackgroundTask#getCyclicFuture()#cancel()} method or
 * stopping of the task.
 * 
 * @see UseRoboticsAPIContext
 * 
 */
public class BackgroundTaskESM extends RoboticsAPICyclicBackgroundTask {
	private Controller controller;
	private LBR kiwa;
	@Inject 							private Plc_inputIOGroup 		plcin;
	@Inject 							private Plc_outputIOGroup 		plcout;
	@Inject 							private MediaFlangeIOGroup 		mfio;
	@Inject
	private ITaskLogger logger;
	public void initialize() { 
		initializeCyclic(0, 500, TimeUnit.MILLISECONDS,
				CycleBehavior.BestEffort);
		switchESMStates();
	}

	public void runCyclic() {

		
	}

	private void switchESMStates() { 
		IUserKeyBar keyBar = getApplicationUI().createUserKeyBar("GRIPPER");

		IUserKeyListener listener1 = new IUserKeyListener() {

			@Override
			public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				if (0 == key.getSlot()) {

					plcout.setPinza_Chiudi(false);
					plcout.setPinza_Apri(true);
				}				
				if (1 == key.getSlot()) {
					
					plcout.setPinza_Apri(false); 
					plcout.setPinza_Chiudi(true);
				}
				if (2 == key.getSlot()) {
					//					kiwa.setESMState("3");
				}
				if (3 == key.getSlot()) {
					//					kiwa.setESMState("4");;
				}
			}
		};

		IUserKey button0 = keyBar.addUserKey(0, listener1, true);
		IUserKey button1 = keyBar.addUserKey(1, listener1, true);
		IUserKey button2 = keyBar.addUserKey(2, listener1, true);
		IUserKey button3 = keyBar.addUserKey(3, listener1, true);

		button0.setText(UserKeyAlignment.TopMiddle, "Gripper OPEN"); 
		button1.setText(UserKeyAlignment.TopMiddle, "Gripper CLOSE"); 
		button2.setText(UserKeyAlignment.TopMiddle, "...");
		button3.setText(UserKeyAlignment.TopMiddle, "...");


		keyBar.publish();

	}
}
