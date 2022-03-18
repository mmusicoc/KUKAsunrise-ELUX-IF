package application.Cambrian;

import static EluxUtils.Utils.*;
import EluxLogger.*;

import com.kuka.roboticsAPI.uiModel.userKeys.IUserKey;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyListener;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyEvent;

public class UserKeys {
	private _CambrianApp app;
	private ProLogger log;
	//private boolean logger;
	
	
	public UserKeys(_CambrianApp cambrianApp, ProLogger log) {
		this.app = cambrianApp;
		this.log = log;
	}
	
	public void configPadKeys() { // BUTTONS --------------------------------------------------------
		IUserKeyListener padKeysListener1 = new IUserKeyListener() {
			@Override public void onKeyEvent(IUserKey key, UserKeyEvent event) {
				if (event == UserKeyEvent.KeyDown) {
					switch (key.getSlot()) {
						case 0:  						// KEY - OEE DATA
							switch (app.pad.question("Manage / print OEE data", "CANCEL", "Save OEE data",
									"Restore OEE data", "Reset OEE to 0", "Reset CT",
									"Print Fridges", "Print Joint Sum", "Print 1 joint", "Print ALL")) {
								case 0: break;
								case 1: app.oee.saveOEEimage(log.getPadLogger()); break;
								case 2: app.oee.restoreOEEimage(log.getPadLogger()); break;
								case 3: app.resetAllOEE();
										break;
								case 4: app.oee.resetCycleTime();
										app.oee.saveOEEimage(log.getPadLogger());
										break;
								case 5: app.oee.printStatsCycle(); break;
								case 6: app.oee.printStatsItem(0); break;
								case 7: app.oee.printStatsItem(
											app.pad.question("Which joint do you want to view?",
											"1","2","3","4","5","6","7","8", "9", "10") + 1); break;
								case 8: app.oee.printStatsCycle();
										for(int i = 0; i <= 10; i++) app.oee.printStatsItem(i); break;
							}
							break;
						case 1:							// KEY - RECORD INP
							switch(app.pad.question("Where do you want to record a precision failure?",
									"CANC", "This joint")) {
								case 0: break;
								case 1:
									app.oee.addINP(app.jointID);
									app.failure[0]++;
									log.msg(Event.HMI, "Intent Not Precise Recorded for J" + app.jointID, 1, true);
									break;
							} break;
						case 2: 						// KEY - SLEEP
							if(app.idle == 0) switch(app.pad.question("Sleep before next...",
									"CANC", "Joint", "Fridge cycle")) {
								case 0: break;
								case 1: logmsg("Robot will pause before next joint.");
										app.idle = 1; break;
								case 2: logmsg("Robot will pause before next fridge cycle.");
										app.idle = 2; break;
							} else {
								logmsg("Robot will resume operations.");
								app.idle = 0;
							}
							if(app.idle != app.remote.getIdle()) app.remote.setIdle(app.idle);
							break;
						case 3:  						// KEY - OTHER
							switch(app.pad.question("Select option", "CANC", "Choose joint",
												"Speed", "Sniff pause","Approach mode", 
												(app.logger?"Disable":"Enable") + " Logger")) {
								case 0: break;
								case 1: 
									int prev_loop = app.loop_joint;
									app.loop_joint = app.pad.question("Which joint do you want to test?",
											"Loop ALL","1","2","3","4","5","6","7","8","9","10","11");
									if (app.loop_joint == 0 && prev_loop != 0) {
										logmsg("Looping all from now");
										app.oee.startCycle();
									}
									else logmsg("Looping joint #" + app.loop_joint);
									break;
								case 2:
									double newSpeed = app.pad.askSpeed();
									if (newSpeed != app.move.getGlobalSpeed()) app.setSpeed(newSpeed);
									else logmsg("Speed didn't change, still " + 
											String.format("%,.0f", newSpeed * 100) + "%");
									break;
								case 3:
									if(app.pad.question("Sniffing Pause", "True - 3s", "Test - 0.5s") == 0)
										app.sniffing_pause = 3000;
									else app.sniffing_pause = 500;
									logmsg("Sniffing pause is now " + app.sniffing_pause + "ms.");
									break;
								case 4:
									app.approachMode = app.pad.question("Select operation mode",
											"Just scan", "Scan + Approach", "Scan + approach + test");
									break;
								case 5:
									app.setLogger(!app.logger);
									break;
							}
							break;
					}
				}
			}
		};
		app.pad.keyBarSetup(padKeysListener1, "SNIFFER", "OEE", "INP", "SLEEP", "OTHER");
	}
}
