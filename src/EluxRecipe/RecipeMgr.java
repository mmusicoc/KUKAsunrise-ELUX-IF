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
	protected Recipe<I> activeRecipe;
	protected xAPI_Pad pad;
	
	public RecipeMgr() { 	// CONSTRUCTOR
	}
	
	protected void init(xAPI_Pad _pad, String _filename) {
		this.recipeDBFilename = _filename;
		this.recipeList = new ArrayList<Recipe<I>>();
		this.activeRecipe = new Recipe<I>();
		this.pad = _pad;
	}
	// GETTERS ---------------------------------------------------------------
	public int getPNCamount() { return recipeList.size(); }
	
	public String getActivePNC() { return activeRecipe.getPNC(); }
	
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
		if(index == -1) {
			padErr("Recipe for PNC=" + PNC + " not found.");
			return false;
		} else return selectRecipeIndex(index);
	}
	
	public boolean selectRecipeIndex(int index) {
		if(index < 0 || index >= recipeList.size()) padErr("Index out of bound");
		else {
			activeRecipe = recipeList.get(index);
			padLog("PNC=" + activeRecipe.getPNC() + " has been selected");
			saveActiveRecipe(false);
			return true;
		}
		return false;
	}
	
	public String getRecipeToString(String PNC) {
		Recipe<I> recipe = getRecipe(PNC);
		if (recipe == null) return "Nonexistent";
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(recipe);
		return json;
	}
	
	public Recipe<I> getRecipe(String PNC) {
		fetchAllRecipes();
		int index = findRecipe(PNC);
		if(index == -1) {
			padErr("Recipe for PNC=" + PNC + " not found.");
			return null;
		} else return recipeList.get(index);
	}
	
	public int findRecipe(String PNC) {
		for(int i=0; i < recipeList.size(); i++) {
			if(recipeList.get(i).getPNC().equals(PNC)) return i;
		}
		return -1;
	}
	
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
	public void newRecipe(String PNC) {
		activeRecipe.setPNC(PNC);
		saveActiveRecipe(false);
	}
	
	public void saveActiveRecipe(boolean log) {
		fetchAllRecipes();
		int PNCindex = findRecipe(activeRecipe.getPNC());
		if(PNCindex != -1) recipeList.remove(PNCindex);
		recipeList.add(0, activeRecipe);
		try {
			FileWriter fw = new FileWriter(
					new File(FILE_ROOTPATH + recipeDBFilename), false);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			gson.toJson(recipeList, fw);
			fw.flush();
			fw.close();
			if(log) {
				String notice = "Recipe for PNC=" + activeRecipe.getPNC() + " has been ";
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
	
	public void addRecipe(Recipe<I> recipe) {
		boolean added = false;
		for(int i=0; i < recipeList.size(); i++) {
			if(recipeList.get(i).getPNC().compareTo(recipe.getPNC()) > 0) {
				recipeList.add(i,recipe);
				added = true;
				break;
			}
		}
		if (!added) recipeList.add(recipe);
	}
}