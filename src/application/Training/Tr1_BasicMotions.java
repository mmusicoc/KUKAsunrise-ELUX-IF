package application.Training;

import static utils.Utils.*;
import utils.HandlerMFio;
import utils.HandlerMov;
import javax.inject.Inject;

import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

public class Tr1_BasicMotions extends RoboticsAPIApplication {
	@Inject	private LBR 	kiwa;
	@Inject private MediaFlangeIOGroup 	mfio;
	@Inject private HandlerMFio	mf = new HandlerMFio(mfio);
	@Inject private HandlerMov move = new HandlerMov(mf);
	
	public void initialize() {
		padLog("Initializing..");
		move.setHome("/Rest");
	}

	public void run() {
		padLog("Move PTP home");
		kiwa.move(ptpHome());
		for (double relSpeed  = 0.2; relSpeed < 1 ; relSpeed += 0.195){
			padLog("Speed is " + relSpeed + "/1");
			move.PTP("/_HOME/_0_Shutoff_REST", relSpeed);
			waitMillis(1000, true);
			move.LIN("/_HOME/_1_Teach_LEFT", relSpeed);
			waitMillis(1000, true);
			move.CIRC("/_HOME/_2_Teach_CENTRAL", "/_HOME/_3_Teach_RIGHT", relSpeed);
		}
		kiwa.move(ptpHome());
		padLog("Finished program");
		return;
	}
}