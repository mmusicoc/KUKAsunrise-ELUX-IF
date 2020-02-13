package utils;

import static utils.Utils.*;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class HandlerPLCio {
	private Plc_inputIOGroup PLCin;
	private Plc_outputIOGroup PLCout;
	@Inject private HandlerPad pad = new HandlerPad();
	
	@Inject
	public HandlerPLCio(Plc_inputIOGroup _PLCin, Plc_outputIOGroup _PLCout) {	// CONSTRUCTOR
		this.PLCin = _PLCin;
		this.PLCout = _PLCout;
	}
	
	/***************************************************************************
	* STANDARD METHODS BY mario.musico@electrolux.com <p>
	***************************************************************************/
	
	public void openGripper(int millis) {
		PLCout.setPinza_Chiudi(false);
		waitMillis(10);
		PLCout.setPinza_Apri(true);
		pad.log("Opening gripper");
		waitMillis(millis);
	}
	public void openGripper() { openGripper(2000); } // Wait for the gripper to close before continuing with the next command
	public void openGripperAsync() { openGripper(10); }
	
	public void closeGripper(int millis) {
		PLCout.setPinza_Apri(false);
		waitMillis(10);
		PLCout.setPinza_Chiudi(true);
		pad.log("Closing gripper");
		waitMillis(millis);
	}
	public void closeGripper() { closeGripper(2000); } // Wait for the gripper to close before continuing with the next command
	public void closeGripperAsync() { closeGripper(10); }
	
	public void askOpenAsync() {
		boolean openGripper = false;
		if (PLCin.getPinza_Holding()){
			if (pad.question("Do you want to open the gripper?", "YES", "NO") == 0) openGripper = true;
		}
		else openGripper = true;
		if (openGripper) openGripperAsync();
	}
}