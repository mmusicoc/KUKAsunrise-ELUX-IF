package EluxRecipe;

import java.util.ArrayList;

public class RecipeDB<I> {
	ArrayList<Recipe<I>> recipeList;
	
	public RecipeDB() { 
		recipeList = new ArrayList<Recipe<I>>();
	}	// CONSTRUCTOR
}