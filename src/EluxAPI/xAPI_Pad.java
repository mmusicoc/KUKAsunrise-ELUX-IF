package EluxAPI;

import static EluxAPI.Utils.*;
import javax.inject.Inject;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKey;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyBar;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyListener;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyAlignment;

public class xAPI_Pad extends RoboticsAPIApplication {
	// Standard KUKA API objects
	@Override public void run() { while (true) { break; } }
	@Inject private xAPI_MF mf;
	
	// CONSTRUCTOR
	@Inject	public xAPI_Pad(xAPI_MF _mf) { 
		this.mf = _mf;
	}
	
	/***************************************************************************
	* STANDARD METHODS BY mario.musico@electrolux.com <p>
	***************************************************************************/
	
	public double askSpeed() { return this.askSpeed(0.15, 0.25, 0.5, 0.75, 1); }
	public double askSpeed(double s0, double s1, double s2, double s3, double s4){
		double relSpeed = 0.1;
		switch(this.question("Set relative speed",
				s0 + "", s1 + "", s2 + "", s3 + "", s4 + "")) {
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
		switch(this.question("Set max External Torque", 
				t0 + " Nm", t1 + " Nm", t2 + " Nm", t3 + " Nm")) {
			case 0: maxTorque = t0; break;
			case 1: maxTorque = t1; break;
			case 2: maxTorque = t2; break;
			case 3: maxTorque = t3; break;
		}
		return maxTorque;
	}
	
	public int askValue(String variable, int startValue) {
		int value = startValue;
		int ans;
		mf.saveRGB();
		mf.setRGB("B");
		do {
			ans = this.question("Current value of " + variable + " is: " + value,
					"OK",  "+1", "+5", "+10", "+50", "+100",
					"CANC","-1", "-5", "-10", "-50", "0");
			switch(ans) {
				case 0:					break;
				case 1: value += 1;		break;
				case 2: value += 5;		break;
				case 3: value += 10;	break;
				case 4: value += 50;	break;
				case 5: value += 100;	break;
				case 6: value = startValue; ans = 0; break;
				case 7: value -= 1;		break;
				case 8: value -= 5;		break;
				case 9: value -= 10;	break;
				case 10: value -= 50;	break;
				case 11: value = 0;		break;
			}
		} while (ans > 0);
		mf.resetRGB();
		padLog(variable + " has been set to " + value);
		return value;
	}
	
	public double askValue(String variable, double startValue) {
		double value = startValue;
		int ans;
		mf.saveRGB();
		mf.setRGB("B");
		do {
			ans = this.question("Current value of " + variable + " is: " + value,
					"OK",  "+0.01", "+0.1", "+1", "+10", "+100",
					"CANC","-0.01", "-0.1", "-1", "-10", "0");
			switch(ans) {
				case 0:					break;
				case 1: value += 0.01;	break;
				case 2: value += 0.1;	break;
				case 3: value += 1;		break;
				case 4: value += 10;	break;
				case 5: value += 100;	break;
				case 6: value = startValue; ans = 0; break;
				case 7: value -= 0.01;	break;
				case 8: value -= 0.1;	break;
				case 9: value -= 1;		break;
				case 10: value -= 10;	break;
				case 11: value = 0;		break;
			}
		} while (ans > 0);
		mf.resetRGB();
		padLog(variable + " has been set to " + value);
		return value;
	}
	
	public String askName(String variable, String startName, 
			boolean forceCaps, boolean log) {
		String name = startName;
		boolean end = false;
		boolean caps = true;
		int page = 0;
		mf.saveRGB();
		mf.setRGB("B");
		do {
			switch(page) {
				case 0:
					switch(this.question(variable + " will be "	+ name + ".",
						"OK","CANC","CLEAR","DEL","SPACE","-","_","NUMS",
						"A-I","J-R","S-Z")) {
						case  0: end = true;			break;
						case  1: name = startName; end = true; break;
						case  2: name = "";				break;
						case  3: name = name.substring(0, name.length() - 1); break;
						case  4: name = name + " ";		break;
						case  5: name = name + "-";		break;
						case  6: name = name + "_";		break;
						case  7: page = 1;				break;
						case  8: page = 2;				break;
						case  9: page = 3;				break;
						case 10: page = 4;				break;
					} break;
				case 1:
					switch(this.question(variable + " will be " + name + ".",
						"MENU","LETTERS","0","1","2","3","4","5","6","7","8","9")) {
						case  0: page = 0;				break;
						case  1: page = 2;				break;
						case  2: name = name + "0";		break;
						case  3: name = name + "1";		break;
						case  4: name = name + "2";		break;
						case  5: name = name + "3";		break;
						case  6: name = name + "4";		break;
						case  7: name = name + "5";		break;
						case  8: name = name + "6";		break;
						case  9: name = name + "7";		break;
						case 10: name = name + "8";		break;
						case 11: name = name + "9";		break;
					} break;
				case 2:
					switch(this.question(variable + " will be " + name + "." + 
						(caps?" (CAPS is ON)":""),
						"MENU","CAPS","MORE LETTERS",
						"A","B","C","D","E","F","G","H","I")) {
						case  0: page = 0;						break;
						case  1: caps = (forceCaps?true:!caps);	break;
						case  2: page++;						break;
						case  3: name = name + (caps?"A":"a"); 	break;
						case  4: name = name + (caps?"B":"b"); 	break;
						case  5: name = name + (caps?"C":"c"); 	break;
						case  6: name = name + (caps?"D":"d"); 	break;
						case  7: name = name + (caps?"E":"e"); 	break;
						case  8: name = name + (caps?"F":"f"); 	break;
						case  9: name = name + (caps?"G":"g"); 	break;
						case 10: name = name + (caps?"H":"h"); 	break;
						case 11: name = name + (caps?"I":"i"); 	break;
					} break;
				case 3:
					switch(this.question(variable + " will be " + name + "." + 
							(caps?" (CAPS is ON)":""),
							"MENU","CAPS","MORE LETTERS",
							"J","K","L","M","N","O","P","Q","R")) {
						case  0: page = 0;						break;
						case  1: caps = (forceCaps?true:!caps);	break;
						case  2: page++;						break;
						case  3: name = name + (caps?"J":"j"); 	break;
						case  4: name = name + (caps?"K":"k"); 	break;
						case  5: name = name + (caps?"L":"l"); 	break;
						case  6: name = name + (caps?"M":"m"); 	break;
						case  7: name = name + (caps?"N":"n"); 	break;
						case  8: name = name + (caps?"O":"o"); 	break;
						case  9: name = name + (caps?"P":"p"); 	break;
						case 10: name = name + (caps?"Q":"q"); 	break;
						case 11: name = name + (caps?"R":"r");	break;
					} break;
				case 4:
					switch(this.question("The name of " + variable + " will be "
							+ name + "." + (caps?" (CAPS is ON)":""),
							"MENU","CAPS","MORE LETTERS","NUMS",
							"S","T","U","V","W","X","Y","Z")) {
						case  0: page = 0;						break;
						case  1: caps = (forceCaps?true:!caps);	break;
						case  2: page = 2;						break;
						case  3: page = 1;						break;
						case  4: name = name + (caps?"S":"s"); 	break;
						case  5: name = name + (caps?"T":"t");	break;
						case  6: name = name + (caps?"U":"u");	break;
						case  7: name = name + (caps?"V":"v");	break;
						case  8: name = name + (caps?"W":"w");	break;
						case  9: name = name + (caps?"X":"x");	break;
						case 10: name = name + (caps?"Y":"y");	break;
						case 11: name = name + (caps?"Z":"z");	break;
					} break;
			}
		} while (!end);
		mf.resetRGB();
		if(log)padLog(variable + " has been set to " + name);
		return name;
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
	
	public void keyBarSetup(IUserKeyListener padKeysListener, String barTitle, 
			String key0, String key1, String key2, String key3) {
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
		int promptAns = 0;
		if (ans.length > 12) padErr("Cannot contain more than 12 choice buttons.");
		else promptAns = getApplicationUI().displayModalDialog(ApplicationDialogType
				.QUESTION, question, ans);
		mf.resetRGB();
		return promptAns;
	}
}