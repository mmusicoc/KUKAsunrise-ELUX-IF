package EluxAPI;

import static EluxAPI.Utils.*;
import javax.inject.Inject;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKey;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyBar;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyListener;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyAlignment;

public class API_Pad extends RoboticsAPIApplication {
	// Standard KUKA API objects
	@Override public void run() { while (true) { break; } }
	@Inject private API_MF mf;
	
	// CONSTRUCTOR
	@Inject	public API_Pad(API_MF _mf) { 
		this.mf = _mf;
	}
	
	/***************************************************************************
	* STANDARD METHODS BY mario.musico@electrolux.com <p>
	***************************************************************************/
	
	public double askSpeed() { return this.askSpeed(0.15, 0.25, 0.5, 0.75, 1); }
	public double askSpeed(double s0, double s1, double s2, double s3, double s4){
		double relSpeed = 0.1;
		switch (this.question("Set relative speed", s0 + "", s1 + "", s2 + "", s3 + "", s4 + "")) {
			case 0: relSpeed = s0; break;
			case 1: relSpeed = s1; break;
			case 2: relSpeed = s2; break;
			case 3: relSpeed = s3; break;
			case 4: relSpeed = s4; break;
		}
		return relSpeed;
	}
	
	public double askTorque() { return this.askTorque(5.0, 10.0, 15.0, 20.0); }
	public double askTorque(double t0, double t1, double t2, double t3){
		double maxTorque = 5.0;
		switch (this.question("Set max External Torque", t0 + " Nm", t1 + " Nm", t2 + " Nm", t3 + " Nm")) {
			case 0: maxTorque = t0; break;
			case 1: maxTorque = t1; break;
			case 2: maxTorque = t2; break;
			case 3: maxTorque = t3; break;
		}
		return maxTorque;
	}
	
	public int changeValue(String variable, int startValue) {
		int value = startValue;
		int ans = 1;
		mf.saveRGB();
		mf.setRGB("B");
		while (ans > 0) {
			ans = this.question("Current value of " + variable + " is: " + value, "OK", 
					"+1", "+5", "+10", "+50", "+100", "CANC",
					"-1", "-5", "-10", "-50", "0");
			switch (ans) {
				case 0:							break;
				case 1: value = value + 1;		break;
				case 2: value = value + 5;		break;
				case 3: value = value + 10;		break;
				case 4: value = value + 50;		break;
				case 5: value = value + 100;	break;
				case 6: value = startValue; ans = 0; break;
				case 7: value = value - 1;		break;
				case 8: value = value - 5;		break;
				case 9: value = value - 10;		break;
				case 10: value = value - 50;	break;
				case 11: value = 0;				break;
			}
		}
		mf.resetRGB();
		padLog("The variable " + variable + " has been set to " + value);
		return value;
	}
	
	public double changeValue(String variable, double startValue) {
		double value = startValue;
		int ans = 1;
		mf.saveRGB();
		mf.setRGB("B");
		while (ans > 0) {
			ans = this.question("Current value of " + variable + " is: " + value, "OK", 
					"+0.01", "+0.1", "+1", "+10", "+100", "CANC",
					"-0.01", "-0.1", "-1", "-10", "0");
			switch (ans) {
				case 0:							break;
				case 1: value = value + 0.01;	break;
				case 2: value = value + 0.1;	break;
				case 3: value = value + 1;		break;
				case 4: value = value + 10;		break;
				case 5: value = value + 100;	break;
				case 6: value = startValue; ans = 0; break;
				case 7: value = value - 0.01;	break;
				case 8: value = value - 0.1;	break;
				case 9: value = value - 1;		break;
				case 10: value = value - 10;	break;
				case 11: value = 0;				break;
			}
		}
		mf.resetRGB();
		padLog("The variable " + variable + " has been set to " + value);
		return value;
	}
	
	// TEMPLATE FOR SOFT KEYS
	/*
	private void configPadKeys() { 					// BUTTONS						
		IUserKeyListener padKeysListener = new IUserKeyListener() {
			@Override public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				if (event == UserKeyEvent.KeyDown) {
					switch (key.getSlot()) {
						case 0:  						// KEY - 

							break;
						case 1: 						// KEY - 

							break;
						case 2:  						// KEY - 

							break;
						case 3:							// KEY - 

							break;
					}
				}
			}
		};
		pad.keyBarSetup(padKeysListener, "GROUP NAME", " ", " ", " ", " ");
	}
	*/
	
	public void keyBarSetup(IUserKeyListener padKeysListener, String barTitle, String key0, String key1, String key2, String key3) {
		IUserKeyBar padKeyBar = getApplicationUI().createUserKeyBar(barTitle);
		IUserKey padKey1 = padKeyBar.addUserKey(0, padKeysListener, true);
		IUserKey padKey2 = padKeyBar.addUserKey(1, padKeysListener, true);
		IUserKey padKey3 = padKeyBar.addUserKey(2, padKeysListener, true);
		IUserKey padKey4 = padKeyBar.addUserKey(3, padKeysListener, true);
		padKey1.setText(UserKeyAlignment.Middle, key0); 
		padKey2.setText(UserKeyAlignment.Middle, key1); 
		padKey3.setText(UserKeyAlignment.Middle, key2);
		padKey4.setText(UserKeyAlignment.Middle, key3);
		padKeyBar.publish();
	}
	
	public int info(String info) {
		int promptAns;
		mf.saveRGB();
		mf.setRGB("B");
		promptAns = getApplicationUI().displayModalDialog(ApplicationDialogType
			.INFORMATION, info, "OK");
		mf.resetRGB();
		return  promptAns;
	}
	
	public int question(String question, String ...ans) {
		mf.saveRGB();
		mf.setRGB("B");
		int promptAns = getApplicationUI().displayModalDialog(ApplicationDialogType
				.QUESTION, question, ans);
		mf.resetRGB();
		return promptAns;
	}
}