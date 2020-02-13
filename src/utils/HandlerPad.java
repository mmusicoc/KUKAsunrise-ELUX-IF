package utils;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKey;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyBar;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyListener;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyAlignment;

public class HandlerPad extends RoboticsAPIApplication {
	
	@Override
	public void run() {
		while (true) { break; }
	}
	
	/***************************************************************************
	* STANDARD METHODS BY mario.musico@electrolux.com <p>
	***************************************************************************/
	
	public void log(String msg) { System.out.println(msg); }
	public void log(int msg) { System.out.println(msg); }
	public void log(boolean msg) { System.out.println(msg); }
	public void log(double msg) { System.out.println(msg); }
	
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
	
	public int question(String question, String ans1, String ans2){
		return getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, question, ans1, ans2); }
	public int question(String question, String ans1, String ans2, String ans3){
		return getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, question, ans1, ans2, ans3); }
	public int question(String question, String ans1, String ans2, String ans3, String ans4){
		return getApplicationUI().displayModalDialog(ApplicationDialogType.QUESTION, question, ans1, ans2, ans3, ans4); }
}