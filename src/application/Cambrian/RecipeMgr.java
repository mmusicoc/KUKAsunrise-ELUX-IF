package application.Cambrian;

import static EluxUtils.Utils.*;
import static EluxUtils.UMath.*;
import EluxUtils.SimpleFrame;
import EluxRecipe.*;
import EluxAPI.xAPI_Pad;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Type;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;

public class RecipeMgr extends EluxRecipe.RecipeMgr<JointRecipe> {
	private JointRecipe activeJoint;
	
	public RecipeMgr() { super(); } // CONSTRUCTOR
	
	@Override public void init(xAPI_Pad pad, String filename, boolean logger) {
		super.init(pad, filename, logger);
		activeIndex = 0;
	}
	
	// JOINT SELECTOR ------------------------------------------------------------
	
	public boolean selectJointID(int ID) {
		int index = findJointIndex(ID);
		if(index != -1) return selectJointIndex(index);
		else {
			padLog("Joint for ID=" + ID + " not found, creating it...");
			newJoint(ID);
			return false;
		}
	}
	
	public boolean selectJointIndex(int index) {
		if(index >= 0 && index < activeRcp.items.size()) {
			activeIndex = index;
			activeJoint = activeRcp.items.get(activeIndex);
			if(logger) padLog("Joint " + activeJoint.getID() + " has been selected");
			return true;
		}
		else padErr("Joint Index not valid, kept existing one");
		return false;
	}
	
	public int findJointIndex(int ID) {
		for(int i = 0; i < getTotItemAmount(); i++) {
			if(activeRcp.items.get(i).getID() == ID) return i;
		}
		return -1;
	}
	
	@Override
	public void fetchAllRecipes() {
		Gson gson = new Gson();
		try {
			JsonReader reader = new JsonReader(
					new FileReader(FILE_ROOTPATH + filename));
			db = new RecipeDB<JointRecipe>();
			Type objType = new TypeToken<RecipeDB<JointRecipe>>(){}.getType();
			db = gson.fromJson(reader, objType);
		} catch (FileNotFoundException e) {
			padErr("File " + filename + " not found");
		} 
	}
	
	// GETTERS ---------------------------------------------------------------
	
	public int getJointID() { return activeJoint.getID(); }
	public char getCurrentJointType() { return activeJoint.getJointType(); }
	public char getNextJointType(int orderIndex) {
		int size = getActiveItemAmount();
		if (orderIndex < 0 || orderIndex >= size) { 
			padErr("Joint Order Item not valid"); return '_'; }
		else if (orderIndex == size - 1) 
			 return activeRcp.items.get(findJointIndex(getOItemID(0))).getJointType();
		else return activeRcp.items.get(findJointIndex(getOItemID(orderIndex + 1))).getJointType();
	}
	
	public Frame getTarget() {
		Frame target = new Frame();
		SimpleFrame targetSimple = activeJoint.getNominalTarget();
		target.setX(targetSimple.X);
		target.setY(targetSimple.Y);
		target.setZ(targetSimple.Z);
		target.setAlphaRad(d2r(targetSimple.A));
		target.setBetaRad(d2r(targetSimple.B));
		target.setGammaRad(d2r(targetSimple.C));
		return target;
	}
	
	public Transformation getDO() { return activeJoint.getDO(); }
	
	public String[] getJointListString() {
		int listSize = getTotItemAmount();
		String[] jointList = new String[listSize + 2];
		
		for(int i = 0; i < listSize; i++) {
			jointList[i] = i2s(activeRcp.items.get(i).getID());
		}
		jointList[listSize] = "NEW";
		jointList[listSize + 1] = "CANC";
		return jointList;
	}
	
	
	// SETTERS ---------------------------------------------------------------
	
	public void saveActiveJoint() {
		int index = findJointIndex(activeJoint.getID());
		if(index != -1) activeRcp.items.set(index, activeJoint);
		else saveNewJoint();
		//saveActiveRecipe();
	}
	
	public void saveNewJoint() {
		for(int i = 0; i < getTotItemAmount(); i++) {
			if (activeRcp.items.get(i).getID() > activeJoint.getID()) {
				activeRcp.items.add(i, activeJoint);
				activeIndex = i;
				return;
			}
		}
		activeRcp.items.add(activeJoint);
		activeIndex = activeRcp.items.size() - 1;
	}
	
	public void newJoint(int jointID) {
		activeJoint = new JointRecipe();
		activeJoint.setID(jointID);
		//saveActiveJoint();
	}
	
	public void setJointType(char jointType) { activeJoint.setJointType(jointType); }
	/*
	public void setTarget(Frame target) {
		activeJoint.setNominalTarget(
				round(target.getX(), 2),
				round(target.getY(), 2),
				round(target.getZ(), 2),
				roundAngle(r2d(target.getAlphaRad()), 2, 0.5),
				roundAngle(r2d(target.getBetaRad()), 2, 0.5),
				roundAngle(r2d(target.getGammaRad()), 2, 0.5));
	}
	*/
	public void setDetectionOffset(double[] off) {
		activeJoint.setDetectionOffset(off[0], off[1], off[2],
									   off[3], off[4], off[5]);
	}
}