package application.Cambrian;

import static EluxUtils.Utils.*;
import EluxAPI.*;
import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.geometricModel.Tool;

public class _CambrianTeach extends RoboticsAPIApplication {
	private static final String RECIPE_FILENAME = "CambrianRecipes.json";
	
	@Inject	@Named("Cambrian") private Tool tool;
	@Inject private xAPI__ELUX elux = new xAPI__ELUX();
	@Inject private xAPI_Move move = elux.getMove();
	@Inject private xAPI_Pad pad = elux.getPad();
	//@Inject private xAPI_Compliance compl = elux.getCompliance();
	//@Inject private CambrianAPI cambrian = new CambrianAPI(elux);
	@Inject private CambrianRecipeMgr rcpMgr = 
				new CambrianRecipeMgr();
	
	String PNC = "F3";
	int[] JOINT_SEQUENCE = {1,2,3,5,6,7,8,9,10,4,7};
	String SP_PATHROOT = "/_Cambrian/F3scanPoints/";
	String NJ_PATHROOT = "/_Cambrian/F3nominalJoints/";
	String cambrianModel = "Elux_weldedpipes";
	
	@Override public void initialize() {
		move.init("/_Cambrian/_Home",		// Home path
				tool, "/TCP",				// Tool, TCP
				1.0, 1.0,					// Relative speed and acceleration
				20.0, 5.0,					// Blending
				15.0, true,					// Collision detection (Nm), response
				false);						// Logging
		//move.PTPhome(1, false);
		//cambrian.init("192.168.2.50", 4000);
		rcpMgr.init(pad, RECIPE_FILENAME, false);
		rcpMgr.fetchAllRecipes();
		//selectPNC();
	}

	@Override public void run() {
		rcpMgr.selectRecipePNC(PNC);
		newRecipe();
		
		PNC = "F2";
		JOINT_SEQUENCE = new int[]{1,3,4,7};
		SP_PATHROOT = "/_Cambrian/F2scanPoints/";
		NJ_PATHROOT = "/_Cambrian/F2nominalJoints/";
		
		rcpMgr.selectRecipePNC(PNC);
		newRecipe();
		
		while(true) waitMillis(1000);
	}
	/*
	private void selectPNC() {
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
				case 1: 
					padLog(rcpMgr.getRecipeToString(rcpMgr.getActivePNC())); 
					break;
				case 2:
					rcpMgr.addItems(JOINT_SEQUENCE);
					rcpMgr.saveActiveRecipe(true);
					break;
			}
			break;
		}
	}*/
	
	private void newRecipe() {
		for (int i = 1; i <= JOINT_SEQUENCE.length; i++) {
			rcpMgr.newJoint(JOINT_SEQUENCE[i - 1]);
			rcpMgr.setModel(cambrianModel);
			rcpMgr.setTarget(move.toFrame(NJ_PATHROOT + "P" + JOINT_SEQUENCE[i - 1]));
			double[] detectionOffset = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6};
			rcpMgr.setDetectionOffset(detectionOffset);
			rcpMgr.saveActiveJoint();
		}
		rcpMgr.addItems(JOINT_SEQUENCE);
		rcpMgr.saveActiveRecipe(false);
	}
	
	public void modifyRecipe() {
		int jointIndex = pad.question("Which joint do you want to modify?",
				rcpMgr.getJointListString());
		if(jointIndex == rcpMgr.getItemsAmount() + 1) { }
		else if(jointIndex == rcpMgr.getItemsAmount()) {
			int newJoint = pad.askValue("Joint Name", rcpMgr.getItemsAmount());
			//String cambrianModel = pad.askName("cambrianModel", "Eluxweldedpipes", false, false);
			rcpMgr.newJoint(newJoint);
		} else {
			rcpMgr.selectJointIndex(jointIndex);
		}
		rcpMgr.setTarget(move.getTCPpos());
		rcpMgr.saveActiveJoint();
	}
}
