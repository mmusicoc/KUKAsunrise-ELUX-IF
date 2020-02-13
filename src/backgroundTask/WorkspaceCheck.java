package backgroundTask;


import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import application.RobotApplication;

import com.kuka.common.ThreadUtil;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Frame;

/**
 * Implementation of a cyclic background task.
 * <p>
 * It provides the {@link RoboticsAPICyclicBackgroundTask#runCyclic} method 
 * which will be called cyclically with the specified period.<br>
 * Cycle period and initial delay can be set by calling 
 * {@link RoboticsAPICyclicBackgroundTask#initializeCyclic} method in the 
 * {@link RoboticsAPIBackgroundTask#initialize()} method of the inheriting 
 * class.<br>
 * The cyclic background task can be terminated via 
 * {@link RoboticsAPICyclicBackgroundTask#getCyclicFuture()#cancel()} method or 
 * stopping of the task.
 * @see UseRoboticsAPIContext
 * 
 */
public class WorkspaceCheck extends RoboticsAPICyclicBackgroundTask {
	@Inject								private Controller 				controller;
	@Inject 							private Plc_inputIOGroup 		plcin;
	@Inject 							private Plc_outputIOGroup 		plcout;
	@Inject 							private MediaFlangeIOGroup 		mfio;
	private Frame actPos;
	private LBR lbr;
	
	@Override
	public void initialize() {
		// initialize your task here
		initializeCyclic(0, 500, TimeUnit.MILLISECONDS,
				CycleBehavior.BestEffort);
		lbr = (LBR) getDevice(controller,"LBR_iiwa_14_R820_1");
	}

	@Override
	public void runCyclic() {
//		// your task execution starts here
//		try {
//			actPos = lbr.getCurrentCartesianPosition(lbr.getFlange());
//			if ((actPos.getX()<-340)&&(actPos.getX()>-350)){
//				if((actPos.getY()<550)&&(actPos.getY()>540)){
//					if((actPos.getZ()<370)&&(actPos.getZ()>330)){
//						plcout.setRobot_InHome(true);
//						mfio.setLEDBlue(true);
//						mfio.setLEDRed(false);
//					}
//				}
//			}
//			else{
//				plcout.setRobot_InHome(false);
//				mfio.setLEDBlue(false);
//			}
//			if (plcin.getApp_Reset()){
//			     try {
//			        getTaskManager().getTask(RobotApplication.class).stopAllInstances();
//			     } catch (InterruptedException e) {
//			        e.printStackTrace();
//			     }
//			     plcout.setApp_ResetDone(true);	  
//			     while(plcin.getApp_Reset()==true){
//			    	 ThreadUtil.milliSleep(50);
//			     }
//			     plcout.setApp_ResetDone(false);	
//			  }
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			ThreadUtil.milliSleep(200);
//		}
	}
}