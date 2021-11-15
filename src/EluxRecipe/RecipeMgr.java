package EluxRecipe;

import static EluxAPI.Utils.*;

import java.util.ArrayList;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class RecipeMgr<I> {
	protected String filename;
	protected ArrayList<Recipe<I>> recipeList;
	protected Recipe<I> activeRecipe;
	
	public RecipeMgr() { 	// CONSTRUCTOR
	}
	
	public void init(String _filename) {
		this.filename = _filename;
		this.recipeList = new ArrayList<Recipe<I>>();
		this.activeRecipe = new Recipe<I>();
	}
	// GETTERS ---------------------------------------------------------------
	public int getPNCamount() { return recipeList.size(); }
	
	public void selectRecipePNC(String PNC) {
		activeRecipe = getRecipe(PNC);
		if (activeRecipe == null) padErr("Recipe for PNC=" + PNC + " not found.");
		else padLog("PNC=" + activeRecipe.getPNC() + " has been selected");
	}
	
	public void selectRecipeIndex(int index) {
		activeRecipe = recipeList.get(index);
		padLog("PNC=" + activeRecipe.getPNC() + " has been selected");
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
	
	public String[] getPNClistString() {
		int listSize = getPNCamount();
		String[] PNClist = new String[listSize + 2];
		for(int i = 0; i < listSize; i++) {
			PNClist[i] = recipeList.get(i).getPNC();
		}
		PNClist[listSize] = "NEW";
		PNClist[listSize + 1] = "CANC";
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
	}
	
	public void saveRecipe() {
		fetchAllRecipes();
		int index = findRecipe(activeRecipe.getPNC());
		if(index == -1) addRecipe(activeRecipe);
		else recipeList.set(index, activeRecipe);
		try {
			FileWriter fw = new FileWriter(new File(filename), false);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			gson.toJson(recipeList, fw);
			fw.flush();
			fw.close();
			String notice = "Recipe for PNC=" + activeRecipe.getPNC() + " has been ";
			notice = notice + ((index == -1)?"added.":"updated.");
			padLog(notice);
		} catch (FileNotFoundException e) {
			padErr("File " + filename + " not found");
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