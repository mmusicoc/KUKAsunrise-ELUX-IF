package EluxRecipe;

import static EluxUtils.Utils.*;
import EluxLogger.Event;
import EluxLogger.ProLogger;
import EluxUtils.JSONmgr;
import EluxAPI.xAPI_Pad;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class RecipeMgr<I> {
	protected String filename;
	protected JSONmgr<RecipeDB<I>> json;
	protected RecipeDB<I> db;
	protected Recipe<I> activeRcp;
	protected int activeIndex;
	protected xAPI_Pad pad;
	protected ProLogger log;
	
	public RecipeMgr() { }	// CONSTRUCTOR
	
	protected void init(xAPI_Pad pad, String filename, ProLogger log) {
		this.filename = filename;
		this.db = new RecipeDB<I>();
		this.activeRcp = new Recipe<I>();
		this.pad = pad;
		this.log = log;
		json = new JSONmgr<RecipeDB<I>>();
		json.init(filename);
	}
	
	// ITEM ORDER ------------------------------------------------------------
	
	public int getOItemID(int index) { return activeRcp.itemOrder.get(index); }
	public void addOItem(int value) { activeRcp.itemOrder.add(value); }
	public void addOItems(int[] values) {
		for(int i = 0; i < values.length; i++) activeRcp.itemOrder.add(values[i]);
	}
	
	// GETTERS ---------------------------------------------------------------
	public int getRCPamount() { return db.recipeList.size(); }
	public int getTotItemAmount() { return activeRcp.items.size(); }
	public int getActiveItemAmount() { return activeRcp.itemOrder.size(); }
	public String getActiveRCP() { return activeRcp.getRCP(); }
	public int getActiveIndex() { return activeIndex; }
	
	public String[] getLastRCPs() {
		int listSize = (getRCPamount() > 10) ? 10 : getRCPamount();
		String[] RCPlist = new String[listSize + 2];
		for(int i = 0; i < listSize; i++) {
			RCPlist[i + 2] = db.recipeList.get(i).getRCP();
		}
		RCPlist[0] = "CANC";
		RCPlist[1] = "TYPE";
		return RCPlist;
	}
	
	public String getRecipeToString(String RCP) {
		Recipe<I> recipe = getRecipe(RCP);
		if (recipe == null) return "Recipe nonexistent, cannot print contents";
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(recipe);
		return json;
	}
	
	public Recipe<I> getRecipe(String RCP) {
		int index = findRecipeIndex(RCP);
		if((index != -1) & ((index >= 0 && index < getRCPamount())))
			return db.recipeList.get(index);
		else return null;
	}
	
	public boolean findRecipe(String RCP) { return findRecipeIndex(RCP) != -1; }
	
	// RECIPE SELECTOR -------------------------------------------------------
		
	public boolean selectRecipeRCP(String RCP) {
		int index = findRecipeIndex(RCP);
		if(index != -1) return selectRecipeIndex(index);
		else {
			log.msg(Event.Rcp, "Recipe " + RCP + " not found in the json database", 1, true);
			//newRecipe(RCP);
			return false;
		}
	}
	
	public boolean selectRecipeIndex(int index) {
		if(index >= 0 && index < getRCPamount()) {
			activeRcp = db.recipeList.get(index);
			log.msg(Event.Rcp, "Recipe " + activeRcp.getRCP() + " has been selected", 0, true);
			return saveActiveRecipe(false);
		} else logErr("Index out of bound");
		return false;
	}
	
	public int findRecipeIndex(String RCP) {
		for(int i = 0; i < getRCPamount(); i++) {
			if(db.recipeList.get(i).getRCP().equals(RCP)) return i;
		}
		return -1;
	}
	
	public void fetchAllRecipes() { db = json.fetchData(db); } // OVERRIDED
	
	public int askRCP() {
		int ans = pad.question("Which recipe do you want to select?", getLastRCPs());
		switch(ans) {
			case 0:
				logmsg("Operation cancelled");
				return -1;
			case 1:
				String RCP = pad.askName("Recipe name", "", true, false);
				if(!findRecipe(RCP)) {
					switch(pad.question("Do you want to create a new recipe for ANC=" +
							RCP, "YES", "NO, CANCEL")) {
						case 0:
							//newRecipe(RCP);
							return 0;
						default:
							logmsg("Operation cancelled");
							return -1;
					}
				}
				else selectRecipeRCP(RCP);
				break;
			default:
				if(!selectRecipeIndex(ans - 2)) return -1;
		}
		return 1;
	}
	
	// SETTERS ---------------------------------------------------------------
	
/*	public void newRecipe(String RCP) {
		activeRcp = new Recipe<I>();
		activeRcp.setRCP(RCP);
		//saveActiveRecipe(false);
	}
	*/
	
	public boolean saveActiveRecipe(boolean logger) {
		fetchAllRecipes();
		int RCPindex = findRecipeIndex(activeRcp.getRCP());
		if(RCPindex != -1) db.recipeList.remove(RCPindex);
		db.recipeList.add(0, activeRcp);
		if(json.saveData(db)){
			String notice = "Json recipe " + activeRcp.getRCP() + " has been ";
			notice = notice + ((RCPindex == -1)?"added.":"updated.");
			log.msg(Event.Rcp, notice, 0, false);
			return true;
		}
		return false;
	}
	
	public boolean copyRecipe(String templateRCP, String newRCP) {
		fetchAllRecipes();
		int RCPindex = findRecipeIndex(newRCP);
		if(RCPindex != -1) {
			if(pad.questionYN("Do you want to replace existing "+ newRCP + "?")) {
				db.recipeList.remove(RCPindex);
			} else {
				log.msg(Event.Rcp, "RCP update aborted", 1, false);
				return false;	
			}
		}
		db.recipeList.add(0, getRecipe(templateRCP));
		json.saveData(db);
		this.fetchAllRecipes();
		db.recipeList.get(0).setRCP(newRCP);
		if(json.saveData(db)){
			String notice = "Json Recipe " + templateRCP + " has been copied to " + newRCP;
			log.msg(Event.Rcp, notice, 0, false);
			return true;
		}
		return false;
	}
}