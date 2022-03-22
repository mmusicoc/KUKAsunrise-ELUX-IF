package application._Common;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.persistenceModel.IPersistenceEngine;
import com.kuka.roboticsAPI.persistenceModel.XmlApplicationDataSource;

public class ModifyAppDataFrame_Test extends RoboticsAPIApplication {


	@Override public void initialize() {
	}

	@Override public void run() {		
		final IPersistenceEngine engine = this.getContext().getEngine(IPersistenceEngine.class);
		final XmlApplicationDataSource defaultDataSource = (XmlApplicationDataSource) engine.getDefaultDataSource();
		//Example
		defaultDataSource.addFrame(getApplicationData().getFrame("/_Cambrian/Recipes"));
		defaultDataSource.renameFrame(getApplicationData().getFrame("/_Cambrian/Recipes/P1"), "F21");
	}
}