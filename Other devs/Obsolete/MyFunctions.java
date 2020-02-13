package utils;

import static utils.Utils.*;
import javax.inject.Inject;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

/**********************************************
* CUSTOM METHODS BY mario.musico@electrolux.com <p>
* static void logPad(String msg)/(int msg)/(double msg)<p>
* static void waitMillis(int millis)/(int millis, boolean log)<p>
* static void sleep()
*/

public class MyFunctions extends RoboticsAPIApplication {
	@Inject	private LBR 				kiwa;
	@Inject private MediaFlangeIOGroup	mfio;
	@Inject private Plc_outputIOGroup 	plcout;
	
	@Override
	public void initialize(){}
	@Override
	public void run() {}
	
	/***************************************************************************
	* STANDARD METHODS BY mario.musico@electrolux.com <p>
	***************************************************************************/
	
// Standard moves
	
	@SuppressWarnings("unused")
	private void setHome(String frameName){
		kiwa.setHomePosition(getApplicationData().getFrame(frameName));
	}
	
	@SuppressWarnings("unused")
	private void movePTP(String frameName, double relSpeed) {
		logPad("Move PTP to " + frameName);
		kiwa.move(ptp(getApplicationData().getFrame(frameName))
					.setJointVelocityRel(relSpeed));
	}
	
	@SuppressWarnings("unused")
	private void moveLIN(String frameName, double relSpeed) {
		logPad("Move LIN to " + frameName);
		kiwa.move(lin(getApplicationData().getFrame(frameName))
					.setJointVelocityRel(relSpeed));
	}
	
	@SuppressWarnings("unused")
	private void moveCIRC(String frameName1, String frameName2, double relSpeed) {
		logPad("Move CIRC to " + frameName1 + " then to " + frameName2);
		kiwa.move(circ(	getApplicationData().getFrame(frameName1),
						getApplicationData().getFrame(frameName2))
					.setJointVelocityRel(relSpeed));
	}
}