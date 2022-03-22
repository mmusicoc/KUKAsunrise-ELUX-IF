package application.Cambrian;

import static EluxUtils.Utils.*;
import static EluxUtils.UMath.*;
import EluxAPI.*;
import EluxLogger.*;

import com.kuka.roboticsAPI.geometricModel.Frame;
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
		return true;
	}
	
	public void copyFrames(String templateRCP, String newRCP) {
		final IPersistenceEngine engine = app.getContext()
										.getEngine(IPersistenceEngine.class);
		final XmlApplicationDataSource framesData =
						(XmlApplicationDataSource) engine.getDefaultDataSource();
		String recipesPathroot = _CambrianApp.FRAMES_PR + "/Recipes";
		String templateRCPpath = recipesPathroot + "/" + templateRCP;
		String newRCPpath = recipesPathroot + "/" + newRCP;
		framesData.open();
		
		Frame newFrame, frame2copy;
		String subPath;
		
		// Create empty parent frame for new recipe
		newFrame = framesData.addFrame(move.p2of(recipesPathroot)).copy();
		framesData.renameFrame(move.p2of(f2p(newFrame) + "P1"), newRCP);

		// RefBolt copy
		newFrame = framesData.addFrame(move.p2of(newRCPpath)).copy();
		framesData.renameFrame(move.p2of(f2p(newFrame) + "P1"), "_RefBolt");
		frame2copy = move.p2f(templateRCPpath + "/_RefBolt");
		framesData.changeFrameTransformation(move.p2of(newRCPpath + "/_RefBolt"), 
				frame2copy.getTransformationFromParent());
		
		// NominalJoints
		newFrame = framesData.addFrame(move.p2of(newRCPpath)).copy();
		framesData.renameFrame(move.p2of(f2p(newFrame) + "P1"), "NominalJoints");
		subPath = "/NominalJoints";

		for(int i = 1; i < 20; i++) {
			frame2copy = move.p2f(templateRCPpath + subPath + "/P" + i);
			if(newFrame != null) {
				newFrame = framesData.addFrame(move.p2of(newRCPpath + subPath)).copy();
				framesData.renameFrame(move.p2of(f2p(newFrame) + "P1"), "P" + i);
				framesData.changeFrameTransformation(move.p2of(newRCPpath + subPath + "/P" + i), 
						frame2copy.getTransformationFromParent());
			}
		}
		/*
		
		// ScanPoints
		newFrame = framesData.addFrame(move.p2of(newRCPpath)).copy();
		framesData.renameFrame(newFrame, "ScanPoints");
		subPath = "/ScanPoints";

		for(int i = 1; i < 20; i++) {
			frame2copy = move.p2f(templateRCPpath + subPath + "/P" + i);
			if(newFrame != null) {
				newFrame = framesData.addFrame(move.p2of(newRCPpath + subPath)).copy();
				framesData.renameFrame(newFrame, "P" + i);
				framesData.changeFrameTransformation(move.p2of(newRCPpath + subPath + "/P" + i), 
						frame2copy.getTransformationFromParent());
			}
		}
		*/
		framesData.save();
		framesData.saveFile();
		logmsg("Updated recipe frames data");
	}
	
	public void updateFrame(String path, Frame newFrame) {
		final IPersistenceEngine engine = app.getContext().getEngine(IPersistenceEngine.class);
		final XmlApplicationDataSource framesData = (XmlApplicationDataSource) engine.getDefaultDataSource();
		framesData.open();
		Frame rebasedFrame = newFrame.copyWithRedundancy(move.p2f(path).getParent());
		framesData.changeFrameTransformation(move.p2of(path),rebasedFrame.getTransformationFromParent());
		framesData.save();
		framesData.saveFile();
		log.msg(Event.Rcp, "Updated " + path + " frame in XML", 0, true);
	}
}