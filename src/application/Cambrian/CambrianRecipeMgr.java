package application.Cambrian;

import static EluxAPI.Utils.*;
import EluxAPI.SimpleFrame;
import EluxRecipe.*;

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
	public void init(String _filename) {
		super.init(_filename);
		activeJointIndex = 0;
	}
	
	// GETTERS ---------------------------------------------------------------
	@Override
	public void fetchAllRecipes() {
		Gson gson = new Gson();
		try {
			JsonReader reader = new JsonReader(new FileReader(filename));
			recipeList = new ArrayList<Recipe<CambrianJoint>>();
			Type objType = new TypeToken<List<Recipe<CambrianJoint>>>(){}.getType();
			recipeList = gson.fromJson(reader, objType);
		} catch (FileNotFoundException e) {
			padErr("File " + filename + " not found");
		} 
	}
	
	public int getJointAmount() { return activeRecipe.items.size(); }
	
	public String getNextCambrianModel(int jointIndex) {
		int size = activeRecipe.items.size();
		if (jointIndex < 0 || jointIndex >= size) { padErr("JointID not valid"); return " "; }
		else if (jointIndex == size - 1) return activeRecipe.items.get(0).getModel();
		else return activeRecipe.items.get(jointIndex + 1).getModel();
	}
	
	public Frame getTarget() {
		Frame target = new Frame();
		SimpleFrame targetSimple = activeJoint.getNominalTarget();
		target.setX(targetSimple.getX());
		target.setY(targetSimple.getY());
		target.setZ(targetSimple.getZ());
		target.setAlphaRad(deg2rad(targetSimple.getA()));
		target.setBetaRad(deg2rad(targetSimple.getB()));
		target.setGammaRad(deg2rad(targetSimple.getC()));
		return target;
	}
	
	public String[] getJointListString() {
		int listSize = getJointAmount();
		String[] jointList = new String[listSize + 2];
		
		for(int i = 0; i < listSize; i++) {
			jointList[i] = activeRecipe.items.get(i).getName();
		}
		jointList[listSize] = "NEW";
		jointList[listSize + 1] = "CANC";
		return jointList;
	}
	
	// SETTERS ---------------------------------------------------------------
	public void saveJoint() {
		activeRecipe.items.set(activeJointIndex, activeJoint);
		saveRecipe();
	}
	
	public void newJoint(String jointName, String cambrianModel) {
		activeJoint = new CambrianJoint();
		activeJoint.setName(jointName);
		activeJoint.setModel(cambrianModel);
		activeJointIndex = activeRecipe.items.size();
		activeRecipe.items.add(activeJoint);
	}
	
	public void selectJoint(int jointIndex) {
		if(jointIndex >= 0 && jointIndex < activeRecipe.items.size()) {
			activeJointIndex = jointIndex;
			activeJoint = activeRecipe.items.get(activeJointIndex);
			padLog("Joint " + activeJoint.getName() + " has been selected");
		}
		else padErr("Joint Index not valid, kept existing one");
	}
	
	public void setTarget(Frame target) {
		activeJoint.setNominalTarget(
				round(target.getX(), 2),
				round(target.getY(), 2),
				round(target.getZ(), 2),
				round(rad2deg(target.getAlphaRad()), 2),
				round(rad2deg(target.getBetaRad()), 2),
				round(rad2deg(target.getGammaRad()), 2));
	}
}