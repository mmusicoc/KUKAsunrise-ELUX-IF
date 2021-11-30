package application.Cambrian;

import static EluxAPI.Utils.padErr;
import EluxRecipe.LastHistory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;


public class CambrianHistoryMgr {
	private String historyFilename;
	private LastHistory lastPNCs = new LastHistory(8);
	private LastHistory lastJoints = new LastHistory(8);
	
	public CambrianHistoryMgr() { 	// CONSTRUCTOR
	}
	
	public void init(String _filename) {
		this.historyFilename = _filename;
		fetchHistory();
	}
	
	// GETTERS ---------------------------------------------------------------
	public String[] getLastPNCs() { return lastPNCs.getHistoryString(); }
	public String[] getLastJoints() { return lastJoints.getHistoryString(); }
	
	public void fetchHistory() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(historyFilename));
			String line = new String(br.readLine());
			String[] aux = line.substring(5).split(",");	// Skip header
			lastPNCs.setHistory(new ArrayList<String>(Arrays.asList(aux)));
			line = br.readLine();
			aux = line.substring(7).split(",");				// Skip header
			lastJoints.setHistory(new ArrayList<String>(Arrays.asList(aux)));
		} catch (FileNotFoundException e) {
			padErr("File " + historyFilename + " not found");
		} catch (IOException e) {
			padErr("Error initializing output stream"); }
	}
	
	// SETTERS ---------------------------------------------------------------
	public void addPNC(String PNC) { lastPNCs.addItem(PNC); storeHistory();}
	public void addJoint(String jointName) { lastJoints.addItem(jointName); storeHistory();}
	
	public void storeHistory() {
		try{
			FileWriter fw = new FileWriter(new File(historyFilename));
			fw.append("PNCs,");
			String[] aux = new String[lastPNCs.getHistorySize()];
			aux = lastPNCs.getHistoryString();
			for(int i = 2; i < lastPNCs.getHistorySize(); i++) {
				fw.append(aux[i]);
				if(i < lastPNCs.getHistorySize() - 1) fw.append(",");
			}
			fw.append("\nJoints,");
			aux = new String[lastJoints.getHistorySize()];
			aux = lastJoints.getHistoryString();
			for(int i = 2; i < lastJoints.getHistorySize(); i++) {
				fw.append(aux[i]);
				if(i < lastJoints.getHistorySize() - 1) fw.append(",");
			}
		} catch (FileNotFoundException e) {
			padErr("File " + historyFilename + " not found");
		} catch (IOException e) {
			padErr("Error initializing output stream"); }
	}
}