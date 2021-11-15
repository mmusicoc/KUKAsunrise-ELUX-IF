package EluxRecipe;

import java.util.ArrayList;

public class Recipe<I> {
	private String PNC;
	public ArrayList<I> items = new ArrayList<I>();
	
	public Recipe() { }	// CONSTRUCTOR
	
	public String getPNC() { return this.PNC; }
	
	public void setPNC(String _PNC) { this.PNC = _PNC; }
}