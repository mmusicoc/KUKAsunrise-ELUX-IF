package application.Cambrian;

import static EluxUtils.Utils.*;
import static EluxUtils.UMath.*;
import EluxAPI.*;
import EluxLogger.*;

import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.persistenceModel.IPersistenceEngine;
import com.kuka.roboticsAPI.persistenceModel.XmlApplicationDataSource;

public class RecipeBuilder {
	private _CambrianApp app;
	private xAPI_Move move;
	private ProLogger log;
	private RecipeMgrJoints rcp;
	
	public RecipeBuilder(_CambrianApp app) {
		this.app = app;
	}
	
	public void init() {
		this.move = app.move;
		this.log = app.log;
		this.rcp = app.rcp;	
	}
	
	public boolean createNewRecipe() {
		String templateRCP = app.pad.askName("Template name", "F", true, log.getPadLogger());
		if(!rcp.findRecipe(templateRCP)) {
			log.msg(Event.Rcp, "Recipe " + templateRCP + " not found", 1, false);
			return false;
		}
		String newRCP = app.pad.askName("New recipe name", "F", true, log.getPadLogger());
		if(rcp.findRecipe(newRCP)) {
			log.msg(Event.Rcp, "Recipe " + newRCP + " already exists", 1, false);
			return false;
		}
		rcp.copyRecipe(templateRCP, newRCP);
		copyFrames(templateRCP, newRCP);
		app.pad.info("Recipes created in JSON and XML appData frames, now:" +
				"\n  1. Put robot in T1, then press OK on this popup" +
				"\n  2. Deselect app" +
				"\n  3. Touchup ScanPoints for new recipe" +
				"\n  4. Sync project back to workstation" +
				"\n  5. Add new PNC & RCP to LUTrecipes.java file switch-case method" +
				"\n  6. Confirm that in ApplicationData tree the new recipe frames appear" +
				"\n  7. Sync project to robot controller" +
				"\n  8. Modify CambrianRecipes.JSON changing joint type, multiJoint if needed" +
				"\n  9. Modify CambrianParams.JSON putting all teachNominal to true, " +
				"\n           so on first scan they are recorded as new NJ");
		return true;
	}
	
	public void copyFrames(String templateRCP, String newRCP) {
		final IPersistenceEngine engine = app.getContext()
										.getEngine(IPersistenceEngine.class);
		final XmlApplicationDataSource appData =
						(XmlApplicationDataSource) engine.getDefaultDataSource();
		String recipesPathroot = _CambrianApp.FRAMES_PR + "/Recipes";
		String templateRCPpath = recipesPathroot + "/" + templateRCP;
		String newRCPpath = recipesPathroot + "/" + newRCP;
		appData.open();
		
		Frame newFrame, frame2copy;
		String subPath;
		
		// Create empty parent frame for new recipe
		newFrame = appData.addFrame(move.p2of(recipesPathroot)).copyWithRedundancy();
		appData.renameFrame(move.p2of(f2p(newFrame) + "P1"), newRCP);

		// RefBolt copy
		newFrame = appData.addFrame(move.p2of(newRCPpath)).copyWithRedundancy();
		appData.renameFrame(move.p2of(f2p(newFrame) + "P1"), "_RefBolt");
		frame2copy = move.p2f(templateRCPpath + "/_RefBolt");
		appData.changeFrameTransformation(move.p2of(newRCPpath + "/_RefBolt"), 
				frame2copy.getTransformationFromParent());
		
		// NominalJoints, create parent and points
		newFrame = appData.addFrame(move.p2of(newRCPpath)).copyWithRedundancy();
		appData.renameFrame(move.p2of(f2p(newFrame) + "P1"), "NominalJoints");
		subPath = "/NominalJoints";

		for(int i = 1; i <= 20; i++) {	// Create NJ frames keeping std no. and copy if exists
			newFrame = appData.addFrame(move.p2of(newRCPpath + subPath)).copy();
			copyFrameTrafo(appData, templateRCPpath + subPath + "/P" + i,
										newRCPpath + subPath + "/P" + i);
		}
		
		cleanFrames(appData, templateRCPpath + subPath, newRCPpath + subPath);
		
		// ScanPoints, create parent and points
		newFrame = appData.addFrame(move.p2of(newRCPpath)).copy();
		appData.renameFrame(move.p2of(f2p(newFrame) + "P1"), "ScanPoints");
		subPath = "/ScanPoints";

		// Need first to use spare point taught from smartPad to get tool, teach and redundancy data,
		// Then rewrite trafos with template frame data
		for(int i = 1; i <= 20; i++) {
			if(move.framePathExists(templateRCPpath + subPath + "/P" + i)) {
				if(moveSparePoint(appData, _CambrianApp.FRAMES_PR + "/SparePoints", newRCPpath + subPath, i))
					copyFrameTrafo(appData, templateRCPpath + subPath + "/P" + i,
						newRCPpath + subPath + "/P" + i);
				else break;
			}
		}

		appData.save();
		appData.saveFile();
		log.msg(Event.Rcp, "Updated recipe frames data", 1, true);
	}
	
	boolean moveSparePoint(XmlApplicationDataSource appData, String sparePath, String newParentPath, int finalName) {
		if(move.p2of(sparePath + "/P1") == null) {
			logErr("Unable to finish recipe frames copy due to lack of spare points");
			return false;
		}
		if(finalName == 1) appData.moveFrame(move.p2of(sparePath + "/P1"), move.p2of(newParentPath));
		else {
			appData.moveFrame(move.p2of(sparePath + "/P1"), move.p2of(sparePath + "/_FolderForRename"));
			appData.renameFrame(move.p2of(sparePath + "/_FolderForRename/P1"), "P" + finalName);
			appData.moveFrame(move.p2of(sparePath + "/_FolderForRename/P" + finalName), move.p2of(newParentPath));
		}
		for(int i = 2; true; i++) { // Shift down all remaining SparePoints for next use
			String nextSpareFrame = sparePath + "/P" + i;
			if(move.p2of(nextSpareFrame, false) == null) break;
			appData.renameFrame(move.p2of(nextSpareFrame), "P" + (i - 1));
		}
		return true;
	}
	
	void copyFrameTrafo(XmlApplicationDataSource appData, String templateFrame, String newFrame) {
		ObjectFrame tempOFrame = move.p2of(templateFrame, false);
		if(tempOFrame != null) {
			Frame frame2copy = tempOFrame.copyWithRedundancy();
			appData.changeFrameTransformation(move.p2of(newFrame), 
					frame2copy.getTransformationFromParent());
		}
	}
	
	void cleanFrames(XmlApplicationDataSource appData, String templatePath, String newPath) {
		for(int i = 1; i < 20; i++) {	// Remove frames that didn't exist in template
			if(!move.framePathExists(templatePath + "/P" + i)) {
				appData.removeFrame(move.p2of(newPath + "/P" + i));
			}
		}
	}
	
	public void setFrameTrafo(String path, Frame newFrame) {
		final IPersistenceEngine engine = app.getContext().getEngine(IPersistenceEngine.class);
		final XmlApplicationDataSource framesData = 
				(XmlApplicationDataSource) engine.getDefaultDataSource();
		framesData.open();
		Frame rebasedFrame = newFrame.copyWithRedundancy(move.p2f(path).getParent());
		framesData.changeFrameTransformation(move.p2of(path),
					rebasedFrame.getTransformationFromParent());
		framesData.save();
		framesData.saveFile();
		log.msg(Event.Rcp, "Updated " + path + " frame in XML", 1, true);
	}
}