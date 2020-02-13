package application;

import java.util.Date;
import application.Context;
import com.kuka.generated.ioAccess.EthercatIOIOGroup;
import com.kuka.roboticsAPI.applicationModel.IApplicationControl;
import com.kuka.roboticsAPI.conditionModel.BooleanIOCondition;
import com.kuka.roboticsAPI.conditionModel.ConditionObserver;
import com.kuka.roboticsAPI.conditionModel.IAnyEdgeListener;
import com.kuka.roboticsAPI.conditionModel.ICondition;
import com.kuka.roboticsAPI.conditionModel.NotificationType;
import com.kuka.roboticsAPI.conditionModel.ObserverManager;
import com.kuka.task.ITaskLogger;

/**<b> SpeedReduction </b><br>
 * Drops the override to security speed if receive the PLC command.
 * 
 * @author francesco.ponsetti
 * */

public class SpeedReduction 
{
                              	
	private EthercatIOIOGroup plcin;
	private ConditionObserver speedObserver;
	private ObserverManager observerManager;
	private IAnyEdgeListener speedListener;
	private IApplicationControl appControl;
	private ITaskLogger log;
	private double safeOverride;
	boolean conditionValue;
    boolean getAutoMode;

	public SpeedReduction(Context context,double pOverride)
	{        		
		this.plcin				= context.getPlcin();
		this.observerManager 	= context.getObsManager();
		this.log 				= context.getLogger();
		this.appControl			= context.getAppControl();
		
		setOverride(pOverride);

		ICondition signals = new BooleanIOCondition(plcin.getInput("In_10"), true);
		speedListener = new IAnyEdgeListener()
		{
			@Override
			public void onAnyEdge(ConditionObserver conditionObserver, Date time, int missedEvents, boolean conditionValue) 
			{
				
				if (!plcin.getIn_10()) {
					appControl.setApplicationOverride(safeOverride);  
					log.info("");
				}
				else {
					appControl.setApplicationOverride(1);
				}
				
			}
		};
	
		speedObserver = observerManager.createConditionObserver(signals, NotificationType.OnEnable, speedListener);
	}
	
	public void setOverride(double override) {
		if(override >= 0 && override <= 1)
			this.safeOverride = override;
	}
		
	public double getOverride() {
		return safeOverride;
	}
	
	public void enable()
	{
		if (!plcin.getIn_10())
		{
			appControl.setApplicationOverride(safeOverride);
		}
		speedObserver.enable();				
	}
	
	public void disable()
	{
		speedObserver.disable();
		appControl.setApplicationOverride(1);
	}
	
}
