package EluxRecipe;

import static EluxUtils.Utils.*;
import EluxAPI.xAPI_Pad;

import java.util.ArrayList;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class RecipeMgr<I> {
	protected String recipeDBFilename;
	protected ArrayList<Recipe<I>> recipeList;
	protected Recipe<I> activeRcp;
	protected xAPI_Pad pad;
	protected boolean logger;
	
	public RecipeMgr() { 	// CONSTRUCTOR
	}
	
	protected void init(xAPI_Pad pad, String filename, boolean logger) {
		this.recipeDBFilename = filename;
		this.recipeList = new ArrayList<Recipe<I>>();
		this.activeRcp = new Recipe<I>();
		this.pad = pad;
		this.logger = logger;
	}
	
	// ITEM ORDER ------------------------------------------------------------
	
	public int getItem(int index) { return activeRcp.itemOrder.get(index); }
	
	public void addItem(int value) { activeRcp.itemOrder.add(value); }
	
	public void addItems(int[] values) {
		for(int i = 0; i < values.length; i++) activeRcp.itemOrder.add(values[i]);
	}
	
	// GETTERS ---------------------------------------------------------------
	public int getPNCamount() { return recipeList.size(); }
	public int getItemsAmount() { return activeRcp.items.size(); }
	
	public String getActivePNC() { return activeRcp.getPNC(); }
	
	public String[] getLastPNCs() {
		int listSize = (getPNCamount() > 10) ? 10 : getPNCamount();
		String[] PNClist = new String[listSize + 2];
		for(int i = 0; i < listSize; i++) {
			PNClist[i + 2] = recipeList.get(i).getPNC();
		}
		PNClist[0] = "CANC";
		PNClist[1] = "TYPE";
		return PNClist;
	}
	
	public String getRecipeToString(String PNC) {
		Recipe<I> recipe = getRecipe(PNC);
		if (recipe == null) return "Nonexistent";
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(recipe);
		return json;
	}
	
	public Recipe<I> getRecipe(String PNC) {
		if(selectRecipePNC(PNC)) return activeRcp;
		else return null;
	}
	
	// RECIPE SELECTOR -------------------------------------------------------
	public int askPNC() {
		int ans = pad.question("Which PNC do you want to select?", getLastPNCs());
		switch(ans) {
			case 0:
				padLog("Operation cancelled");
				return -1;
			case 1:
				String PNC = pad.askName("PNC", "", true, false);
				if(!selectRecipePNC(PNC)) {
					switch(pad.question("Do you want to create a new recipe for PNC=" +
							PNC, "YES", "NO, CANCEL")) {
						case 0:
							newRecipe(PNC);
							return 0;
						default:
							padLog("Operation cancelled");
							return -1;
					}
				}
				break;
			default:
				if(!selectRecipeIndex(ans - 2)) return -1;
		}
		return 1;
	}
	
	public boolean selectRecipePNC(String PNC) {
		int index = findRecipe(PNC);
		if(index != -1) return selectRecipeIndex(index);
		else {
			padLog("Recipe for PNC=" + PNC + " not found, creating it...");
			newRecipe(PNC);
			return false;
		}
	}
	
	public boolean selectRecipeIndex(int index) {
		if(index >= 0 && index < getPNCamount()) {
			activeRcp = recipeList.get(index);
			if(logger) padLog("PNC=" + activeRcp.getPNC() + " has been selected");
			//saveActiveRecipe(false);
			return true;
		} else padErr("Index out of bound");
		return false;
	}
	
	public int findRecipe(String PNC) {
		for(int i = 0; i < getPNCamount(); i++) {
			if(recipeList.get(i).getPNC().equals(PNC)) return i;
		}
		return -1;
	}
	
	public void fetchAllRecipes() {
		/* IMPLEMENT IN NON-ABSTRACT CLASS
		Gson gson = new Gson();
		try {
			JsonReader reader = new JsonReader(new FileReader(filename));
			recipeList = new ArrayList<Recipe<I>>();
			Type objType = new TypeToken<List<Recipe<I>>>(){}.getType();
			recipeList = gson.fromJson(reader, objType);
		} catch (FileNotFoundException e) {
			padErr("File " + filename + " not found");
		} 
		*/
	}
	
	// SETTERS ---------------------------------------------------------------
	public void setLogger(boolean logger) { this.logger = logger; }
	
	public void newRecipe(String PNC) {
		activeRcp = new Recipe<I>();
		activeRcp.setPNC(PNC);
		//saveActiveRecipe(false);
	}
	
	public void saveActiveRecipe(boolean log) {
		fetchAllRecipes();
		int PNCindex = findRecipe(activeRcp.getPNC());
		if(PNCindex != -1) recipeList.remove(PNCindex);
		recipeList.add(0, activeRcp);
		try {
			FileWriter fw = new FileWriter(
					new File(FILE_ROOTPATH + recipeDBFilename), false);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			gson.toJson(recipeList, fw);
			fw.flush();
			fw.close();
			if(log) {
				String notice = "Recipe for PNC=" + activeRcp.getPNC() + " has been ";
				notice = notice + ((PNCindex == -1)?"added.":"updated.");
				padLog(notice);
			}
		} catch (FileNotFoundException e) {
			padErr("File " + recipeDBFilename + " not found");
		} catch (IOException e) {
			//e.printStackTrace();
			padLog("Unexpected error");
		}
	}
}