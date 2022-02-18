package EluxRecipe;

import static EluxUtils.Utils.*;
import EluxUtils.JSONmgr;
import EluxAPI.xAPI_Pad;
//import java.util.ArrayList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class RecipeMgr<I> {
	protected String filename;
	protected JSONmgr<RecipeDB<I>> json;
	//protected ArrayList<Recipe<I>> recipeList;
	protected RecipeDB<I> db;
	protected Recipe<I> activeRcp;
	protected int activeIndex;
	protected xAPI_Pad pad;
	protected boolean logger;
	
	public RecipeMgr() { }	// CONSTRUCTOR
	
	protected void init(xAPI_Pad pad, String filename, boolean logger) {
		this.filename = filename;
		//this.recipeList = new ArrayList<Recipe<I>>();
		this.db = new RecipeDB<I>();
		this.activeRcp = new Recipe<I>();
		this.pad = pad;
		this.logger = logger;
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
	public int getRCPamount() { return db.rl.size(); }
	public int getTotItemAmount() { return activeRcp.items.size(); }
	public int getActiveItemAmount() { return activeRcp.itemOrder.size(); }
	public String getActiveRCP() { return activeRcp.getRCP(); }
	public int getActiveIndex() { return activeIndex; }
	
	public String[] getLastRCPs() {
		int listSize = (getRCPamount() > 10) ? 10 : getRCPamount();
		String[] RCPlist = new String[listSize + 2];
		for(int i = 0; i < listSize; i++) {
			RCPlist[i + 2] = db.rl.get(i).getRCP();
		}
		RCPlist[0] = "CANC";
		RCPlist[1] = "TYPE";
		return RCPlist;
	}
	
	public String getRecipeToString(String RCP) {
		Recipe<I> recipe = getRecipe(RCP);
		if (recipe == null) return "Nonexistent";
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(recipe);
		return json;
	}
	
	public Recipe<I> getRecipe(String RCP) {
		if(selectRecipeRCP(RCP)) return activeRcp;
		else return null;
	}
	
	// RECIPE SELECTOR -------------------------------------------------------
	public int askRCP() {
		int ans = pad.question("Which ANC do you want to select?", getLastRCPs());
		switch(ans) {
			case 0:
				padLog("Operation cancelled");
				return -1;
			case 1:
				String RCP = pad.askName("ANC", "", true, false);
				if(!selectRecipeRCP(RCP)) {
					switch(pad.question("Do you want to create a new recipe for ANC=" +
							RCP, "YES", "NO, CANCEL")) {
						case 0:
							newRecipe(RCP);
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
	
	public boolean selectRecipeRCP(String RCP) {
		int index = findRecipeIndex(RCP);
		if(index != -1) return selectRecipeIndex(index);
		else {
			padLog("Recipe for ANC=" + RCP + " not found, creating it...");
			newRecipe(RCP);
			return false;
		}
	}
	
	public boolean selectRecipeIndex(int index) {
		if(index >= 0 && index < getRCPamount()) {
			activeRcp = db.rl.get(index);
			if(logger) padLog("ANC=" + activeRcp.getRCP() + " has been selected");
			//saveActiveRecipe(false);
			return true;
		} else padErr("Index out of bound");
		return false;
	}
	
	public int findRecipeIndex(String RCP) {
		for(int i = 0; i < getRCPamount(); i++) {
			if(db.rl.get(i).getRCP().equals(RCP)) return i;
		}
		return -1;
	}
	
	public void fetchAllRecipes() { db = json.fetchData(db); } // OVERRIDED
	
	// SETTERS ---------------------------------------------------------------
	public void setLogger(boolean logger) { this.logger = logger; }
	
	public void newRecipe(String RCP) {
		activeRcp = new Recipe<I>();
		activeRcp.setRCP(RCP);
		//saveActiveRecipe(false);
	}
	
	public void saveActiveRecipe(boolean logger) {
		fetchAllRecipes();
		int RCPindex = findRecipeIndex(activeRcp.getRCP());
		if(RCPindex != -1) db.rl.remove(RCPindex);
		db.rl.add(0, activeRcp);
		if(json.saveData(db) & logger){
			String notice = "Recipe for ANC=" + activeRcp.getRCP() + " has been ";
			notice = notice + ((RCPindex == -1)?"added.":"updated.");
			padLog(notice);
		}
	}
}