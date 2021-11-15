package application.Cambrian;

import static EluxAPI.Utils.*;
import EluxAPI.*;
import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class CambrianTeach extends RoboticsAPIApplication {
	private static final String RECIPE_FILENAME = "CambrianRecipes.json";
	
	@Inject	@Named("Cambrian") private Tool GripperCambrian;
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_Move move = elux.getMove();
	@Inject private xAPI_Pad pad = elux.getPad();
	//@Inject private xAPI_Compliance compl = elux.getCompliance();
	//@Inject private CambrianAPI cambrian = new CambrianAPI(elux);
	@Inject private CambrianRecipeMgr rcpMgr = 
				new CambrianRecipeMgr();
	
	@Override public void initialize() {
		move.setTool(GripperCambrian);
		move.setTCP("/TCP");
		move.setGlobalSpeed(1);
		move.setJTconds(15.0);
		move.setBlending(20, 5);
		move.setHome("/_Cambrian/_Home");
		//move.PTPhome(1, false);
		//cambrian.init("192.168.2.50", 4000);
		rcpMgr.init(RECIPE_FILENAME);
		rcpMgr.fetchAllRecipes();
	}

	@Override public void run() {
		int PNCindex = pad.question("Which PNC recipe do you want to modify?",
				rcpMgr.getPNClistString());
		if(PNCindex == rcpMgr.getPNCamount() + 1) {}
		else if(PNCindex == rcpMgr.getPNCamount()) newRecipe();
		else {
			rcpMgr.selectRecipeIndex(PNCindex);
			modifyRecipe();
		}
	}
	
	private void newRecipe() {
		String PNC = pad.askName("PNC", "");
		rcpMgr.newRecipe(PNC);
		rcpMgr.newJoint("1", "Eluxweldedpipes");
		rcpMgr.setTarget(new Frame(30,70,60,0,0,0));
		rcpMgr.saveJoint();
		padLog(rcpMgr.getRecipeToString(PNC));
	}
	
	private void modifyRecipe() {
		int jointIndex = pad.question("Which joint do you want to modify?",
				rcpMgr.getJointListString());
		if(jointIndex == rcpMgr.getJointAmount() + 1) { }
		else if(jointIndex == rcpMgr.getJointAmount()) {
			int newJoint = pad.askValue("Joint Name", rcpMgr.getJointAmount());
			String cambrianModel = pad.askName("cambrianModel", "Eluxweldedpipes");
			rcpMgr.newJoint(Integer.toString(newJoint), cambrianModel);
		} else {
			rcpMgr.selectJoint(jointIndex);
		}
		rcpMgr.setTarget(move.getTCPpos());
		rcpMgr.saveJoint();
	}
}
