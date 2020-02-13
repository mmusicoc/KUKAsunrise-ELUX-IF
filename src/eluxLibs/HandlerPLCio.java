package eluxLibs;

import static eluxLibs.Utils.waitMillis;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;

@Singleton
public class HandlerPLCio {
	// Private properties
	private HandlerMFio _mf;
	private Plc_inputIOGroup _PLCin;
	private Plc_outputIOGroup _PLCout;
	
	// CONSTRUCTOR
	@Inject	public HandlerPLCio(HandlerMFio mf, Plc_inputIOGroup PLCin, Plc_outputIOGroup PLCout) {
		this._mf = mf;
		this._PLCin = PLCin;
		this._PLCout = PLCout;
	}
	
	// Custom modularizing handler objects
	@Inject private HandlerPad pad = new HandlerPad(_mf);
	
	/***************************************************************************
	* STANDARD METHODS BY mario.musico@electrolux.com <p>
	***************************************************************************/
	
	public int getGripperState() {
		if (_PLCin.getPinza_Holding()) return (int)1;
		else return 0;
	}
	
	public void openGripper(int millis) {
		_PLCout.setPinza_Chiudi(false);
		waitMillis(10);
		_PLCout.setPinza_Apri(true);
	//	padLog("Opening gripper");
		waitMillis(millis);
	}
	public void openGripper() { this.openGripper(1500); } // Wait for the gripper to close before continuing with the next command
	public void openGripperAsync() { this.openGripper(10); }
	
	public void closeGripper(int millis) {
		_PLCout.setPinza_Apri(false);
		waitMillis(10);
		_PLCout.setPinza_Chiudi(true);
	//	padLog("Closing gripper");
		waitMillis(millis);
	}
	public void closeGripper() { this.closeGripper(1500); } // Wait for the gripper to close before continuing with the next command
	public void closeGripperAsync() { this.closeGripper(10); }
	
	public void askOpen() {
		if (!_PLCin.getPinza_Holding()) this.openGripperAsync();
		else if (pad.question("The gripper is gripping sth, do you want to open it before proceeding?", "YES", "NO") == 0) openGripper();
	}
	
	public void askClose(boolean loop) {
		do {
			if (_PLCin.getPinza_Holding()) break;
			else if (pad.question("The gripper didn't detect anything, do you want to close it now?", "YES", "NO") == 0) {
				this.openGripper();
				this.closeGripper();
			}
		} while (loop);
	}
}