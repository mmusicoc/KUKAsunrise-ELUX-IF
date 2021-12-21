package application.Cambrian;

import static EluxUtils.Utils.*;
import static EluxUtils.UMath.*;
import EluxAPI.*;
import EluxUtils.CSVLogger;

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
	RecipeMgr rcp = new RecipeMgr();
	CSVLogger csv = new CSVLogger();
	
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
		rcp.init(pad, RECIPE_FILENAME, false);
		rcp.fetchAllRecipes();
		
		csv.init("CambrianDC.csv", false);
		//selectPNC();
	}

	@Override public void run() {
	//	rcp.selectRecipePNC(PNC);
	//	newRecipe();
		
		PNC = "F2";
	//	JOINT_SEQUENCE = new int[]{1,3,4,7};
	//	SP_PATHROOT = "/_Cambrian/F2scanPoints/";
	//	NJ_PATHROOT = "/_Cambrian/F2nominalJoints/";
		
		rcp.selectRecipePNC(PNC);
		//newRecipe();
		
		DOtoCSV();
		
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
	
	void DOtoCSV() {
		csv.open();
		for (int i = 0; i < rcp.getTotItemAmount(); i++) {
			rcp.selectJointIndex(i);
			csv.log(i, false);
			csv.log(rcp.getDO().getX(), false);
			csv.log(rcp.getDO().getY(), true);
			csv.log(rcp.getDO().getZ(), true);
			csv.log(r2d(rcp.getDO().getAlphaRad()), true);
			csv.log(r2d(rcp.getDO().getBetaRad()), true);
			csv.log(r2d(rcp.getDO().getGammaRad()), true);
			csv.eol();
		}
		csv.close(true);
	}
	
	void newRecipe() {
		for (int i = 1; i <= JOINT_SEQUENCE.length; i++) {
			rcp.newJoint(JOINT_SEQUENCE[i - 1]);
			rcp.setModel(cambrianModel);
			//rcp.setTarget(move.toFrame(NJ_PATHROOT + "P" + JOINT_SEQUENCE[i - 1]));
			//double[] detectionOffset = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6};
			//rcp.setDetectionOffset(detectionOffset);
			rcp.saveActiveJoint();
		}
		rcp.addOItems(JOINT_SEQUENCE);
		rcp.saveActiveRecipe(false);
	}
	/*
	void modifyRecipe() {
		int jointIndex = pad.question("Which joint do you want to modify?",
				rcp.getJointListString());
		if(jointIndex == rcp.getItemAmount() + 1) { }
		else if(jointIndex == rcp.getItemAmount()) {
			int newJoint = pad.askValue("Joint Name", rcp.getItemAmount());
			//String cambrianModel = pad.askName("cambrianModel", "Eluxweldedpipes", false, false);
			rcp.newJoint(newJoint);
		} else {
			rcp.selectJointIndex(jointIndex);
		}
		//rcp.setTarget(move.getTCPpos());
		rcp.saveActiveJoint();
	}
	*/
}
