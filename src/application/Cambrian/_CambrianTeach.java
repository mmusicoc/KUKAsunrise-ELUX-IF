package application.Cambrian;

import static EluxAPI.Utils.*;
import EluxAPI.*;
import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class _CambrianTeach extends RoboticsAPIApplication {
	private static final String RECIPE_FILENAME = "CambrianRecipes.json";
	
	@Inject	@Named("Cambrian") private Tool GripperCambrian;
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_Move move = elux.getMove();
	@Inject private xAPI_Pad pad = elux.getPad();
	//@Inject private xAPI_Compliance compl = elux.getCompliance();
	//@Inject private CambrianAPI cambrian = new CambrianAPI(elux);
	@Inject private CambrianRecipeMgr rcpMgr = 
				new CambrianRecipeMgr();
	//private CambrianHistoryMgr historyMgr = new CambrianHistoryMgr();
	
	@Override public void initialize() {
		move.setTool(GripperCambrian);
		move.setTCP("/TCP");
		move.setGlobalSpeed(1);
		move.setJTconds(15.0);
		move.setBlending(20, 5);
		move.setHome("/_Cambrian/_Home");
		//move.PTPhome(1, false);
		//cambrian.init("192.168.2.50", 4000);
		rcpMgr.init(pad, RECIPE_FILENAME);
		rcpMgr.fetchAllRecipes();
		//historyMgr.init("CambrianHistory.csv");
	}

	@Override public void run() {
		switch(rcpMgr.askPNC()) {
			case -1:
				padLog("No PNC selected, program end.");
				break;
			case 0:
				newRecipe();
				break;
			case 1:
				switch(pad.question("What do you want to do?",
						"Cancel", "Visualize recipe data",
						"Select Joint"
						)) {
					case 0: break;
					case 1: padLog(rcpMgr.getRecipeToString(rcpMgr.getActivePNC())); 
							break;
					case 2:
						break;
					case 3:
						break;
				}
				
		}
	}
	
	private void newRecipe() {
		rcpMgr.newJoint("1", "Eluxweldedpipes");
		rcpMgr.setTarget(new Frame(30,70,60,0,0,0));
		rcpMgr.saveJoint();
		padLog("New recipe stored");
	}
	
	public void modifyRecipe() {
		int jointIndex = pad.question("Which joint do you want to modify?",
				rcpMgr.getJointListString());
		if(jointIndex == rcpMgr.getJointAmount() + 1) { }
		else if(jointIndex == rcpMgr.getJointAmount()) {
			int newJoint = pad.askValue("Joint Name", rcpMgr.getJointAmount());
			String cambrianModel = pad.askName("cambrianModel", "Eluxweldedpipes", false, false);
			rcpMgr.newJoint(Integer.toString(newJoint), cambrianModel);
		} else {
			rcpMgr.selectJoint(jointIndex);
		}
		rcpMgr.setTarget(move.getTCPpos());
		rcpMgr.saveJoint();
	}
}
