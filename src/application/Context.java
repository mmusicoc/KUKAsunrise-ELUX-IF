package application;

import com.kuka.generated.ioAccess.EthercatIOIOGroup;
import com.kuka.roboticsAPI.applicationModel.IApplicationControl;
import com.kuka.roboticsAPI.applicationModel.IApplicationData;
import com.kuka.roboticsAPI.conditionModel.ObserverManager;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.task.ITaskLogger;


public class Context 
{
	public Controller controller;
	private LBR lbr;   	
	private EthercatIOIOGroup plcin;    	
	private IApplicationData appData;
	private ObserverManager obsManager;
	private ITaskLogger logger;
	private IApplicationControl appControl;
	

	public Context(Controller controller,LBR lbr,EthercatIOIOGroup plcin,IApplicationData appData,ObserverManager obsManager,ITaskLogger logger,IApplicationControl appControl)
	{
		this.controller = controller;
		this.lbr = lbr;  		
		this.plcin= plcin;				
		this.appData = appData;
		this.obsManager = obsManager;
		this.logger = logger;
		this.appControl = appControl;
	}
	
	public Controller getController()
	{
		return controller;
	}
	
	public LBR getLbr()
	{
		return lbr;
	}                       	
	
	public EthercatIOIOGroup getPlcin() {
		return plcin;
	}        		

	public IApplicationData getAppData() {
		return appData;
	}

	public ObserverManager getObsManager() {
		return obsManager;
	}

	public ITaskLogger getLogger() {
		return logger;
	}

	public IApplicationControl getAppControl() {
		return appControl;
	}
	
}
