package application._Common;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.persistenceModel.IPersistenceEngine;
import com.kuka.roboticsAPI.persistenceModel.XmlApplicationDataSource;

public class ModifyAppDataFrame_Test extends RoboticsAPIApplication {


	@Override public void initialize() {
	}

	@Override public void run() {		
		final IPersistenceEngine engine = this.getContext().getEngine(IPersistenceEngine.class);
		final XmlApplicationDataSource appData = (XmlApplicationDataSource) engine.getDefaultDataSource();
		//Example
		appData.addFrame(getApplicationData().getFrame("/_Cambrian/Recipes"));
		appData.renameFrame(getApplicationData().getFrame("/_Cambrian/Recipes/P1"), "F21");
		appData.moveFrame(getApplicationData().getFrame("/_Cambrian/SparePoints/P1"), 
				getApplicationData().getFrame("/_Cambrian"));
	}
}