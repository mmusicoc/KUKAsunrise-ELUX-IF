package utils;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKey;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyBar;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyListener;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyAlignment;
import javax.inject.Inject;

public class HandlerPad extends RoboticsAPIApplication {
	// Standard KUKA API objects
	@Override public void run() { while (true) { break; } }
	@Inject private HandlerMFio mf;
	
	@Inject			// CONSTRUCTOR
	public HandlerPad(HandlerMFio _mf) { 
		this.mf = _mf;
	}
	
	/***************************************************************************
	* STANDARD METHODS BY mario.musico@electrolux.com <p>
	***************************************************************************/
	
	public double askSpeed() { return this.askSpeed(0.15, 0.25, 0.5); }
	public double askSpeed(double s0, double s1, double s2){
		double relSpeed = 0.25;
		int promptAns = this.question("Set relative speed", s0 + "", s1 + "", s2 + "");  
		switch (promptAns) {
			case 0: relSpeed = s0; break;
			case 1: relSpeed = s1; break;
			case 2: relSpeed = s2; break;
		}
		return relSpeed;
	}
	
	public double askTorque() { return this.askTorque(5.0, 10.0, 15.0, 20.0); }
	public double askTorque(double t0, double t1, double t2, double t3){
		double maxTorque = 10.0;
		int promptAns = this.question("Set max External Torque", t0 + " Nm", t1 + " Nm", t2 + " Nm", t3 + " Nm");  
		switch (promptAns) {
			case 0: maxTorque = t0; break;
			case 1: maxTorque = t1; break;
			case 2: maxTorque = t2; break;
			case 3: maxTorque = t3; break;
		}
		return maxTorque;
	}
	
	public void keyBarSetup(IUserKeyListener padKeysListener, String barTitle, String key0, String key1, String key2, String key3) {
		IUserKeyBar padKeyBar = getApplicationUI().createUserKeyBar("TEACH");
		IUserKey padKey1 = padKeyBar.addUserKey(0, padKeysListener, true);
		IUserKey padKey2 = padKeyBar.addUserKey(1, padKeysListener, true);
		IUserKey padKey3 = padKeyBar.addUserKey(2, padKeysListener, true);
		IUserKey padKey4 = padKeyBar.addUserKey(3, padKeysListener, true);
		padKey1.setText(UserKeyAlignment.TopMiddle, key0); 
		padKey2.setText(UserKeyAlignment.TopMiddle, key1); 
		padKey3.setText(UserKeyAlignment.TopMiddle, key2);
		padKey4.setText(UserKeyAlignment.TopMiddle, key3);
		padKeyBar.publish();
	}
	
	public int question(String question, String ans0, String ans1) {
		int promptAns;
		mf.saveRGB();
		mf.setRGB("B");
		promptAns = getApplicationUI().displayModalDialog(ApplicationDialogType
			.QUESTION, question, ans0, ans1);
		mf.resetRGB();
		return  promptAns;
	}
	
	public int question(String question, String ans0, String ans1, String ans2) {
		int promptAns;
		mf.saveRGB();
		mf.setRGB("B");
		promptAns = getApplicationUI().displayModalDialog(ApplicationDialogType
			.QUESTION, question, ans0, ans1, ans2);
		mf.resetRGB();
		return  promptAns;
	}
	
	public int question(String question, String ans0, String ans1, String ans2, String ans3) {
		int promptAns;
		mf.saveRGB();
		mf.setRGB("B");
		promptAns = getApplicationUI().displayModalDialog(ApplicationDialogType
			.QUESTION, question, ans0, ans1, ans2, ans3);
		mf.resetRGB();
		return  promptAns;
	}
}