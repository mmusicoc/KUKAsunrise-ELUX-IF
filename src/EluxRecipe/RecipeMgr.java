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
	public int getPNCamount() { return db.rl.size(); }
	public int getTotItemAmount() { return activeRcp.items.size(); }
	public int getActiveItemAmount() { return activeRcp.itemOrder.size(); }
	public String getActivePNC() { return activeRcp.getPNC(); }
	public int getActiveIndex() { return activeIndex; }
	
	public String[] getLastPNCs() {
		int listSize = (getPNCamount() > 10) ? 10 : getPNCamount();
		String[] PNClist = new String[listSize + 2];
		for(int i = 0; i < listSize; i++) {
			PNClist[i + 2] = db.rl.get(i).getPNC();
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
		int index = findRecipeIndex(PNC);
		if(index != -1) return selectRecipeIndex(index);
		else {
			padLog("Recipe for PNC=" + PNC + " not found, creating it...");
			newRecipe(PNC);
			return false;
		}
	}
	
	public boolean selectRecipeIndex(int index) {
		if(index >= 0 && index < getPNCamount()) {
			activeRcp = db.rl.get(index);
			if(logger) padLog("PNC=" + activeRcp.getPNC() + " has been selected");
			//saveActiveRecipe(false);
			return true;
		} else padErr("Index out of bound");
		return false;
	}
	
	public int findRecipeIndex(String PNC) {
		for(int i = 0; i < getPNCamount(); i++) {
			if(db.rl.get(i).getPNC().equals(PNC)) return i;
		}
		return -1;
	}
	
	public void fetchAllRecipes() { db = json.fetchData(db); }
	
	// SETTERS ---------------------------------------------------------------
	public void setLogger(boolean logger) { this.logger = logger; }
	
	public void newRecipe(String PNC) {
		activeRcp = new Recipe<I>();
		activeRcp.setPNC(PNC);
		//saveActiveRecipe(false);
	}
	
	public void saveActiveRecipe(boolean logger) {
		fetchAllRecipes();
		int PNCindex = findRecipeIndex(activeRcp.getPNC());
		if(PNCindex != -1) db.rl.remove(PNCindex);
		db.rl.add(0, activeRcp);
		if(json.saveData(db) & logger){
			String notice = "Recipe for PNC=" + activeRcp.getPNC() + " has been ";
			notice = notice + ((PNCindex == -1)?"added.":"updated.");
			padLog(notice);
		}
	}
}