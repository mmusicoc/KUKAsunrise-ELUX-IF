package backgroundTask;

import static utils.Utils.*;
import utils.*;
import application.Training.*;

import java.util.concurrent.TimeUnit;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.uiModel.userKeys.*;
import javax.inject.Inject;
 
public class bkgGLOBAL extends RoboticsAPICyclicBackgroundTask {
	@Inject private Plc_inputIOGroup 		plcin;
	@Inject private Plc_outputIOGroup 		plcout;
	@Inject private MediaFlangeIOGroup 		mfio;
	@Inject private HandlerMFio	mf = new HandlerMFio(mfio);
	@Inject private HandlerPLCio plc = new HandlerPLCio(mf, plcin, plcout);
	@Inject private HandlerPad pad = new HandlerPad(mf);
	
	public void initialize() { 
		initializeCyclic(0, 500, TimeUnit.MILLISECONDS,	CycleBehavior.BestEffort);
		configPadKeysGLOBAL();
	}

	public void runCyclic() {}

	private void configPadKeysGLOBAL() {
		IUserKeyListener padKeysListener = new IUserKeyListener() {
			@Override public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				switch (key.getSlot()) {
					case 0:  						// KEY - OPEN GRIPPER
						plc.openGripper();
						break;
					case 1:  						// KEY - CLOSE GRIPPER
						plc.closeGripper();
						break;
					case 2:
						appSwitcher();
						break;
					case 3:
						break;
				}
			}
		};
		
		pad.keyBarSetup(padKeysListener, "GLOBAL", "OPEN gripper", "CLOSE gripper", " ", " ");
	}
	
	private void appSwitcher() {
		int promptAns = pad.question("Which program do you want to run?", "Cancel", "Sleep", "Home", "Tr1", "Tr2", "Tr3", "Tr4", "Tr5");
		switch (promptAns) {
			case 0:
				break;
			case 1:
				sleep();
				break;
			case 2:
				_Home runApp2 = new _Home();
				runApp2.initialize();
				runApp2.run();
				break;
			case 3:
				Tr1_BasicMotions runApp3 = new Tr1_BasicMotions();
				runApp3.initialize();
				runApp3.run();
				break;
			default:
				break;
		}
	}
}
