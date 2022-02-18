package EluxRecipe;

import java.util.ArrayList;

public class Recipe<I> {
	private String RCP;
	public ArrayList<Integer> itemOrder = new ArrayList<Integer>();
	public ArrayList<I> items = new ArrayList<I>();
	
	public Recipe() { }	// CONSTRUCTOR
	
	public String getRCP() { return this.RCP; }
	public void setRCP(String _RCP) { this.RCP = _RCP; }
}