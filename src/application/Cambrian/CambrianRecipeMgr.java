package application.Cambrian;

import static EluxUtils.Utils.*;
import static EluxUtils.UMath.*;
import EluxUtils.SimpleFrame;
import EluxRecipe.*;
import EluxAPI.xAPI_Pad;

import java.util.ArrayList;
import java.util.List;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Type;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.kuka.roboticsAPI.geometricModel.Frame;

public class CambrianRecipeMgr extends RecipeMgr<CambrianJoint> {
	private int activeJointIndex;
	private CambrianJoint activeJoint;
	
	public CambrianRecipeMgr() { // CONSTRUCTOR
		super();
	}
	
	@Override
	public void init(xAPI_Pad _pad, String _filename) {
		super.init(_pad, _filename);
		activeJointIndex = 0;
	}
	
	// GETTERS ---------------------------------------------------------------
	@Override
	public void fetchAllRecipes() {
		Gson gson = new Gson();
		try {
			JsonReader reader = new JsonReader(
					new FileReader(FILE_ROOTPATH + recipeDBFilename));
			recipeList = new ArrayList<Recipe<CambrianJoint>>();
			Type objType = new TypeToken<List<Recipe<CambrianJoint>>>(){}.getType();
			recipeList = gson.fromJson(reader, objType);
		} catch (FileNotFoundException e) {
			padErr("File " + recipeDBFilename + " not found");
		} 
	}
	
	public int getJointAmount() { return activeRecipe.items.size(); }
	
	public String getNextCambrianModel(int jointIndex) {
		int size = activeRecipe.items.size();
		if (jointIndex < 0 || jointIndex >= size) { 
			padErr("JointID not valid"); return " "; }
		else if (jointIndex == size - 1) return activeRecipe.items.get(0).getModel();
		else return activeRecipe.items.get(jointIndex + 1).getModel();
	}
	
	public Frame getTarget() {
		Frame target = new Frame();
		SimpleFrame targetSimple = activeJoint.getNominalTarget();
		target.setX(targetSimple.X());
		target.setY(targetSimple.Y());
		target.setZ(targetSimple.Z());
		target.setAlphaRad(d2r(targetSimple.A()));
		target.setBetaRad(d2r(targetSimple.B()));
		target.setGammaRad(d2r(targetSimple.C()));
		return target;
	}
	
	public String[] getJointListString() {
		int listSize = getJointAmount();
		String[] jointList = new String[listSize + 2];
		
		for(int i = 0; i < listSize; i++) {
			jointList[i] = i2s(activeRecipe.items.get(i).getID());
		}
		jointList[listSize] = "NEW";
		jointList[listSize + 1] = "CANC";
		return jointList;
	}
	
	// SETTERS ---------------------------------------------------------------
	public void saveActiveJoint() {
		activeRecipe.items.set(activeJointIndex, activeJoint);
		//saveActiveRecipe();
	}
	
	public void newJoint(int jointID) {
		activeJoint = new CambrianJoint();
		activeJoint.setID(jointID);
		activeJointIndex = activeRecipe.items.size();
		activeRecipe.items.add(activeJoint);
	}
	
	public void setActive(boolean enabled) { activeJoint.setActive(enabled); }
	public void setModel(String model) { activeJoint.setModel(model); }
	
	public void selectJoint(int jointIndex) {
		if(jointIndex >= 0 && jointIndex < activeRecipe.items.size()) {
			activeJointIndex = jointIndex;
			activeJoint = activeRecipe.items.get(activeJointIndex);
			padLog("Joint " + activeJoint.getID() + " has been selected");
		}
		else padErr("Joint Index not valid, kept existing one");
	}
	
	public void setTarget(Frame target) {
		activeJoint.setNominalTarget(
				round(target.getX(), 2),
				round(target.getY(), 2),
				round(target.getZ(), 2),
				roundAngle(r2d(target.getAlphaRad()), 2, 0.5),
				roundAngle(r2d(target.getBetaRad()), 2, 0.5),
				roundAngle(r2d(target.getGammaRad()), 2, 0.5));
	}
	
	public void setDetectionOffset(double[] off) {
		activeJoint.setDetectionOffset(off[0], off[1], off[2],
									   off[3], off[4], off[5]);
	}
}