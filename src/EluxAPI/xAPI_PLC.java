package EluxAPI;

import static EluxUtils.Utils.*;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;

@Singleton
public class xAPI_PLC {
	private xAPI_MF _mf;
	private Plc_inputIOGroup _PLCin;
	private Plc_outputIOGroup _PLCout;
	
	// CONSTRUCTOR
	@Inject	public xAPI_PLC(xAPI_MF mf, Plc_inputIOGroup PLCin, Plc_outputIOGroup PLCout) {
		this._mf = mf;
		this._PLCin = PLCin;
		this._PLCout = PLCout;
	}
	
	// Custom modularizing handler objects
	@Inject private xAPI_Pad pad = new xAPI_Pad(_mf);
	
	/***************************************************************************
	* STANDARD METHODS BY mario.musico@electrolux.com <p>
	***************************************************************************/
	
	public boolean gripperIsHolding() {
		if (_PLCin.getPinza_Holding()) return true;
		else return false;
	}
	
	public boolean gripperIsIdle() {
		if (_PLCin.getPinza_Idle()) return true;
		else return false;
	}
	
	public boolean gripperIsEmpty() {
		if (_PLCin.getPinza_Idle()) return true;
		else return false;
	}
	
	public void openGripper(int millis) {
		boolean holding = _PLCin.getPinza_Holding();
		int timer = 0;
		_PLCout.setPinza_Chiudi(false);
		waitMillis(10);
		_PLCout.setPinza_Apri(true);
		while(timer < millis && holding) {
			holding = _PLCin.getPinza_Holding();
			waitMillis(100);
			timer += 100;
		}
	}
	public void openGripper() { this.openGripper(1500); } // Wait for the gripper to close before continuing with the next command
	public void openGripperAsync() { this.openGripper(10); }
	
	public void closeGripper(int millis) {
		int timer = 0;
		_PLCout.setPinza_Apri(false);
		waitMillis(10);
		_PLCout.setPinza_Chiudi(true);
		while(timer < millis && !_PLCin.getPinza_Holding()) {
			waitMillis(100);
			timer += 100;
		}
	}
	public void closeGripper() { this.closeGripper(1500); } // Wait for the gripper to close before continuing with the next command
	public void closeGripperAsync() { this.closeGripper(10); }
	
	public void askOpen() {
		if (!_PLCin.getPinza_Holding()) this.openGripperAsync();
		else if (_mf.waitUserButton(5000)) {
			waitMillis(1000);
			openGripper();
		} else if (pad.question("The gripper is gripping sth, do you want to open it before proceeding?", "YES", "NO") == 0) {
			waitMillis(1000);
			openGripper();
		}
	}
	
	public void askClose(boolean loop) {
		do {
			if (_PLCin.getPinza_Holding()) break;
			else if (_mf.waitUserButton(5000)) {
				this.openGripper();
				this.closeGripper();
			}
			else if (pad.question("The gripper didn't detect anything, do you want to close it now?", "YES", "NO") == 0) {
				this.openGripper();
				this.closeGripper();
			}
		} while (loop);
	}
	
	
}