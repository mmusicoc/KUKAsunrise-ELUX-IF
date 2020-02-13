package eluxLibs;

import static eluxLibs.Utils.waitMillis;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;

@Singleton
public class HandlerPLCio {
	// Private properties
	private HandlerMFio mf;
	private Plc_inputIOGroup PLCin;
	private Plc_outputIOGroup PLCout;
	
	// CONSTRUCTOR
	@Inject	public HandlerPLCio(HandlerMFio _mf, Plc_inputIOGroup _PLCin, Plc_outputIOGroup _PLCout) {
		this.mf = _mf;
		this.PLCin = _PLCin;
		this.PLCout = _PLCout;
	}
	
	// Custom modularizing handler objects
	@Inject private HandlerPad pad = new HandlerPad(mf);
	
	/***************************************************************************
	* STANDARD METHODS BY mario.musico@electrolux.com <p>
	***************************************************************************/
	
	public int getGripperState() {
		if (PLCin.getPinza_Holding()) return (int)1;
		else return 0;
	}
	
	public void openGripper(int millis) {
		PLCout.setPinza_Chiudi(false);
		waitMillis(10);
		PLCout.setPinza_Apri(true);
	//	padLog("Opening gripper");
		waitMillis(millis);
	}
	public void openGripper() { this.openGripper(1500); } // Wait for the gripper to close before continuing with the next command
	public void openGripperAsync() { this.openGripper(10); }
	
	public void closeGripper(int millis) {
		PLCout.setPinza_Apri(false);
		waitMillis(10);
		PLCout.setPinza_Chiudi(true);
	//	padLog("Closing gripper");
		waitMillis(millis);
	}
	public void closeGripper() { this.closeGripper(1500); } // Wait for the gripper to close before continuing with the next command
	public void closeGripperAsync() { this.closeGripper(10); }
	
	public void askOpen() {
		if (!PLCin.getPinza_Holding()) this.openGripper();
		else if (pad.question("The gripper is gripping sth, do you want to open it before proceeding?", "YES", "NO") == 0) openGripper();
	}
	
	public void askClose(boolean loop) {
		do {
			if (PLCin.getPinza_Holding()) break;
			else if (pad.question("The gripper didn't detect anything, do you want to close it now?", "YES", "NO") == 0) {
				this.openGripper();
				this.closeGripper();
			}
		} while (loop);
	}
}