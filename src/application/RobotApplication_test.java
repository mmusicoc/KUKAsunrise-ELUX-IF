package application;


import javax.inject.Inject;

import com.kuka.common.ThreadUtil;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

import com.kuka.roboticsAPI.conditionModel.ForceCondition;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.executionModel.IFiredConditionInfo;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.motionModel.IMotion;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;

/**
 * Implementation of a robot application.
 * <p>
 * The application provides a {@link RoboticsAPITask#initialize()} and a 
 * {@link RoboticsAPITask#run()} method, which will be called successively in 
 * the application lifecycle. The application will terminate automatically after 
 * the {@link RoboticsAPITask#run()} method has finished or after stopping the 
 * task. The {@link RoboticsAPITask#dispose()} method will be called, even if an 
 * exception is thrown during initialization or run. 
 * <p>
 * <b>It is imperative to call <code>super.dispose()</code> when overriding the 
 * {@link RoboticsAPITask#dispose()} method.</b> 
 * 
 * @see UseRoboticsAPIContext
 * @see #initialize()
 * @see #run()
 * @see #dispose()
 */
public class RobotApplication_test extends RoboticsAPIApplication {
	@Inject
	private LBR lbr;
	@Inject 							private Plc_inputIOGroup 		plcin;
	@Inject 							private Plc_outputIOGroup 		plcout;
	@Inject 							private MediaFlangeIOGroup 		mfio;
	private JointPosition home;
	private int Mission=0;
	private int lastMission=0;
	private boolean var_test = false;
	private Frame actPos;
	private IMotionContainer stop,pause;
	private ForceCondition forceCondition;
	private Frame f1= new Frame(0,0,-15);
	private Frame f2= new Frame(-290,0,0);
	


	@Override
	public void initialize() {
		// initialize your application here
		home = new JointPosition( Math.toRadians(138.50), Math.toRadians(40.95),  Math.toRadians(-9.57), Math.toRadians(-100.48),  Math.toRadians(54.92),  Math.toRadians(-55.72),  Math.toRadians(-30.90));
		lbr.setHomePosition(home);
		System.out.println("initialization");
		forceCondition = ForceCondition.createSpatialForceCondition(lbr.getFlange(),50);

		plcout.setPinza_Chiudi(false);
		ThreadUtil.milliSleep(1500);
		plcout.setPinza_Apri(true);
		ThreadUtil.milliSleep(1500);
		plcout.setPinza_Chiudi(false);
		ThreadUtil.milliSleep(1500);
		plcout.setPinza_Apri(true);
	}

	@Override
	public void run() {
		// your application execution starts here
		while(true){
			resetPlcOutput();
			System.out.println("Waiting for a mission");
			//mWait();
			//Mission=plcin.getMission_Index();
			Mission = 1;
			switch (Mission){
			case 1://repositioning
				System.out.println("reset");
				actPos = lbr.getCurrentCartesianPosition(lbr.getFlange());
				if(plcin.getPinza_NoPart() || plcin.getPinza_Error()||plcin.getPinza_Idle()){
					if(actPos.getX()<-200){//withdrawl
						//mWaitPosAllow();
						//movimenti
						lbr.move(linRel(0,0,-30).setCartVelocity(50));
						lbr.move(ptp(getApplicationData().getFrame("/AggancioHousing/Svincolo")).setJointAccelerationRel(0.1).setJointVelocityRel(0.1));
						lbr.move(ptp(getApplicationData().getFrame("/AggancioHousing/Approccio")).setJointVelocityRel(0.1));
						ThreadUtil.milliSleep(50);
						//mAtPos();
						ThreadUtil.milliSleep(50);
		
						//mWaitExitAllow();
						//movimenti
						ThreadUtil.milliSleep(50);
		
						//mExitDone();
						ThreadUtil.milliSleep(50);
		
						//mMissionEnd();
					}
					if(actPos.getX()>-200&&actPos.getY()>0){//joint
						//mWaitPosAllow();
						//movimenti
						
						lbr.move(ptp(getApplicationData().getFrame("/AggancioCarrozzeria/ApproccioPart2")).setJointAccelerationRel(0.1).setJointVelocityRel(0.1));
						lbr.move(ptp(getApplicationData().getFrame("/AggancioCarrozzeria/Approcciopart1")).setJointAccelerationRel(0.1).setJointVelocityRel(0.1));
						ThreadUtil.milliSleep(50);
						//mAtPos();
						ThreadUtil.milliSleep(50);
		
						//mWaitExitAllow();
						//movimenti
						lbr.move(ptp(getApplicationData().getFrame("/AggancioHousing/Svincolo")).setJointAccelerationRel(0.1).setJointVelocityRel(0.1));
						lbr.move(ptp(getApplicationData().getFrame("/AggancioHousing/Approccio")).setJointAccelerationRel(0.1).setJointVelocityRel(0.1));
						lbr.move(ptpHome().setJointVelocityRel(0.1));
						ThreadUtil.milliSleep(50);
		
						//mExitDone();
						ThreadUtil.milliSleep(50);
		
						//mMissionEnd();
					}
					if(actPos.getX()>-200&&actPos.getY()<0){//deposito
						//mWaitPosAllow();
						//movimenti
						
						lbr.move(ptp(getApplicationData().getFrame("/DepositoFinale/Approccio")).setJointVelocityRel(0.1));
						lbr.move(ptp(getApplicationData().getFrame("/AggancioCarrozzeria/ApproccioPart2")).setJointAccelerationRel(0.1).setJointVelocityRel(0.1));
						lbr.move(ptp(getApplicationData().getFrame("/AggancioCarrozzeria/Approcciopart1")).setJointAccelerationRel(0.1).setJointVelocityRel(0.1));
						ThreadUtil.milliSleep(50);
						//mAtPos();
						ThreadUtil.milliSleep(50);
		
						//mWaitExitAllow();
						//movimenti
						lbr.move(ptp(getApplicationData().getFrame("/AggancioHousing/Svincolo")).setJointAccelerationRel(0.1).setJointVelocityRel(0.1));
						lbr.move(ptp(getApplicationData().getFrame("/AggancioHousing/Approccio")).setJointAccelerationRel(0.1).setJointVelocityRel(0.1));
						lbr.move(ptpHome().setJointVelocityRel(0.1));
						ThreadUtil.milliSleep(50);
		
						//mExitDone();
						ThreadUtil.milliSleep(50);
		
						//mMissionEnd();
					}
				}
				
				lbr.move(ptpHome().setJointVelocityRel(0.1));
				System.out.println("end of reset");
				Mission = 11;
			break;
			case 11: //piece picking
				lastMission=11;
				System.out.println("picking");
				
				plcout.setPinza_Apri(true);
				lbr.move(ptp(getApplicationData().getFrame("/home")).setJointVelocityRel(0.2));
				plcout.setPinza_Apri(false);
				//System.out.println("waiting for posAllow");
				//mWaitPosAllow();
				//movimenti
				pause = lbr.move(ptp(getApplicationData().getFrame("/AggancioHousing/Approccio")).setJointVelocityRel(0.2).breakWhen(forceCondition));
				IFiredConditionInfo info = pause.getFiredBreakConditionInfo();
				while (info!=null){
					System.out.println("within the control cycle");
					mfio.setLEDRed(true);
					while(!mfio.getUserButton()){
						ThreadUtil.milliSleep(20);
					}
					pause = lbr.move(ptp(getApplicationData().getFrame("/AggancioHousing/Approccio")).setJointVelocityRel(0.2).breakWhen(forceCondition));
					info = pause.getFiredBreakConditionInfo();
				}
				mfio.setLEDRed(false);
				stop = lbr.move(ptp(getApplicationData().getFrame("/AggancioHousing")).breakWhen(forceCondition).setJointVelocityRel(0.2));
				info = stop.getFiredBreakConditionInfo();
				while (info!=null){
					mfio.setLEDRed(true);
					lbr.moveAsync(ptp(getApplicationData().getFrame("/AggancioHousing/Approccio")).setBlendingCart(10));
					stop = lbr.move(ptp(getApplicationData().getFrame("/AggancioHousing")).breakWhen(forceCondition).setJointVelocityRel(0.2));
					info = stop.getFiredBreakConditionInfo();
				}
				mfio.setLEDRed(false);
				//mAtPos();
				
				plcout.setPinza_Chiudi(true);
				ThreadUtil.milliSleep(100);
				while((plcin.getPinza_Gripping()==true)	&&	(plcin.getPinza_Holding()==false)	&&	(plcin.getPinza_NoPart()==false)	&&	(plcin.getPinza_Error()==false)){
					ThreadUtil.milliSleep(10);
				}
				if ((plcin.getPinza_Error()==true)||(plcin.getPinza_NoPart()==true)){
					System.out.println("clamping error");
					plcout.setMission_Result(3);
				}
				plcout.setPinza_Chiudi(false);
				//mWaitExitAllow();
				//movimenti
				if(plcin.getPinza_Holding()){
					System.out.println("piece in caliper");
					f2.setParent(getApplicationData().getFrame("/AggancioHousing").copy());
					lbr.move(linRel(-10,0,0).setCartVelocity(100));
					pause = lbr.move(lin(f2).setCartVelocity(250).breakWhen(forceCondition));
					info = pause.getFiredBreakConditionInfo();
					while (info!=null){
						System.out.println("within the control cycle");
						mfio.setLEDRed(true);
						while(!mfio.getUserButton()){
							ThreadUtil.milliSleep(20);
						}
						pause = lbr.move(lin(f2).setCartVelocity(250).breakWhen(forceCondition));
						info = pause.getFiredBreakConditionInfo();
					}
					mfio.setLEDRed(false);
					lbr.moveAsync(linRel(-100,-10,-10).setCartAcceleration(250).setCartVelocity(250).setBlendingCart(15));
					//mExitDone();
					//mMissionEnd();
				}
				if(plcin.getPinza_NoPart()){
					lbr.move(ptp(getApplicationData().getFrame("/home")).setJointVelocityRel(0.25));
					//mExitDone();
					//mMissionEnd();
				}
				
				Mission = 21;
			break;
			case 21://incastro sul pezzo
				lastMission=21;
				f1.setParent(getApplicationData().getFrame("/AggancioCarrozzeria").copy());
				System.out.println("interlocking");
				//mWaitPosAllow();
				//movimenti
				lbr.moveAsync(ptp(getApplicationData().getFrame("/AggancioCarrozzeria/Approcciopart1")).setBlendingCart(50).setJointVelocityRel(0.2));
				
				pause = lbr.move(ptp(getApplicationData().getFrame("/AggancioCarrozzeria/ApproccioPart2")).setJointVelocityRel(0.25).breakWhen(forceCondition));
				info = pause.getFiredBreakConditionInfo();
				while (info!=null){
					System.out.println("within the control cycle");
					mfio.setLEDRed(true);
					while(!mfio.getUserButton()){
						ThreadUtil.milliSleep(20);
					}
					pause = lbr.move(ptp(getApplicationData().getFrame("/AggancioCarrozzeria/ApproccioPart2")).setJointVelocityRel(0.25).breakWhen(forceCondition));
					info = pause.getFiredBreakConditionInfo();
				}
				mfio.setLEDRed(false);
				stop= lbr.move(lin(f1).setCartVelocity(100).breakWhen(forceCondition));
				info = stop.getFiredBreakConditionInfo();
				while (info!=null){
					mfio.setLEDRed(true);
					lbr.moveAsync(ptp(getApplicationData().getFrame("/AggancioCarrozzeria/ApproccioPart2")).setJointVelocityRel(0.25).setBlendingCart(10));
					stop= lbr.move(lin(f1).setCartVelocity(100).breakWhen(forceCondition));
					info = stop.getFiredBreakConditionInfo();
				}
				mfio.setLEDRed(false);
				
				lbr.move(lin(getApplicationData().getFrame("/AggancioCarrozzeria")).setCartVelocity(100));
				//mAtPos();
				ThreadUtil.milliSleep(500);
				//mWaitExitAllow();
				//movimenti
				lbr.moveAsync(lin(getApplicationData().getFrame("/AggancioCarrozzeria/ApproccioPart2")).setBlendingCart(10).setCartVelocity(250));
				lbr.moveAsync(linRel(0,-50,0).setBlendingCart(10).setCartVelocity(250).setCartAcceleration(250));
				//mExitDone();
				//mMissionEnd();
				Mission = 31;
			break;
			case 31://deposito finale
				lastMission=31;

				System.out.println("deposit");
				//mWaitPosAllow();
				//movimenti
				System.out.println("i m out of mWaitPosAllow");
				pause = lbr.move(lin(getApplicationData().getFrame("/DepositoFinale/Lineare")).setCartVelocity(250).breakWhen(forceCondition));
				info = pause.getFiredBreakConditionInfo();
				while (info!=null){
					System.out.println("within the control cycle");
					mfio.setLEDRed(true);
					while(!mfio.getUserButton()){
						ThreadUtil.milliSleep(20);
					}
					pause = lbr.move(lin(getApplicationData().getFrame("/DepositoFinale/Lineare")).setCartVelocity(250).breakWhen(forceCondition));
					info = pause.getFiredBreakConditionInfo();
				}
				mfio.setLEDRed(false);
				pause = lbr.move(ptp(getApplicationData().getFrame("/DepositoFinale/Approccio")).setJointVelocityRel(0.25).breakWhen(forceCondition));
				info = pause.getFiredBreakConditionInfo();
				while (info!=null){
					System.out.println("within the control cycle");
					mfio.setLEDRed(true);
					while(!mfio.getUserButton()){
						ThreadUtil.milliSleep(20);
					}
					pause = lbr.move(ptp(getApplicationData().getFrame("/DepositoFinale/Approccio")).setJointVelocityRel(0.25).breakWhen(forceCondition));
					info = pause.getFiredBreakConditionInfo();
				}
				mfio.setLEDRed(false);
				System.out.println("going to deposit");
				lbr.move(lin(getApplicationData().getFrame("/DepositoFinale")).setJointVelocityRel(0.25));
				//mAtPos();
				plcout.setPinza_Apri(true);
				while(plcin.getPinza_Idle()==false){
					ThreadUtil.milliSleep(20);
				}
				plcout.setPinza_Apri(false);
				//mWaitExitAllow();
				//movimenti
				pause = lbr.move(ptp(getApplicationData().getFrame("/DepositoFinale/Approccio")).setJointVelocityRel(0.25).breakWhen(forceCondition));
				info = pause.getFiredBreakConditionInfo();
				while (info!=null){
					System.out.println("within the control cycle");
					mfio.setLEDRed(true);
					while(!mfio.getUserButton()){
						ThreadUtil.milliSleep(20);
					}
					pause = lbr.move(ptp(getApplicationData().getFrame("/DepositoFinale/Approccio")).setJointVelocityRel(0.25).breakWhen(forceCondition));
					info = pause.getFiredBreakConditionInfo();
				}
				mfio.setLEDRed(false);
				pause = lbr.move(ptp(getApplicationData().getFrame("/DepositoFinale/Lineare")).setJointVelocityRel(0.25).breakWhen(forceCondition));
				while (info!=null){
					System.out.println("within the control cycle");
					mfio.setLEDRed(true);
					while(!mfio.getUserButton()){
						ThreadUtil.milliSleep(20);
					}
					pause = lbr.move(ptp(getApplicationData().getFrame("/DepositoFinale/Lineare")).setJointVelocityRel(0.25).breakWhen(forceCondition));
					info = pause.getFiredBreakConditionInfo();
				}
				mfio.setLEDRed(false);
				pause = lbr.move(lin(getApplicationData().getFrame("/AggancioCarrozzeria/ApproccioPart2")).setCartVelocity(250).breakWhen(forceCondition));
				info = pause.getFiredBreakConditionInfo();
				while (info!=null){
					System.out.println("within the control cycle");
					mfio.setLEDRed(true);
					while(!mfio.getUserButton()){
						ThreadUtil.milliSleep(20);
					}
					pause = lbr.move(lin(getApplicationData().getFrame("/AggancioCarrozzeria/ApproccioPart2")).setCartVelocity(250).breakWhen(forceCondition));
					info = pause.getFiredBreakConditionInfo();
				}
				mfio.setLEDRed(false);
				lbr.moveAsync(ptp(getApplicationData().getFrame("/AggancioCarrozzeria/Approcciopart1")).setJointVelocityRel(0.25).setBlendingCart(50));
				lbr.moveAsync(ptp(getApplicationData().getFrame("/AggancioHousing/Svincolo")).setJointVelocityRel(0.25).setBlendingCart(50));
				lbr.move(ptp(getApplicationData().getFrame("/home")).setJointVelocityRel(0.25));
				
				//mExitDone();
				//mMissionEnd();
			Mission = 1;
			break;
			}//end switch
			
			
			
		}//end while
	}//end run
	//------------------------------------------------------------------------------------------------------------------------------------------------------------------
	//methods
	private void mAtPos(){
		plcout.setMission_AtPos(true);
		mfio.setOutputX3Pin1(false);
	}
	
	private void mExitDone(){
		plcout.setMission_ExitDone(true);
	}
	
	private void mWaitExitAllow(){
		while(plcin.getMission_ExitAllow()==false){
			ThreadUtil.milliSleep(50);
		}
		plcout.setMission_AtPos(false);
	}
	
	private void mMissionEnd(){
		while((plcin.getMission_Start()==true)||(plcin.getMission_PosAllow()==true)||(plcin.getMission_ExitAllow()==true)||(plcin.getMission_Index()!=0) ){
			ThreadUtil.milliSleep(50);
		}
		plcout.setMission_ExitDone(false);
	}
	
	private void mWaitPosAllow(){
		mfio.setOutputX3Pin1(false); //DataLock substitute
		var_test = false;
		while(var_test==false){
			ThreadUtil.milliSleep(50);
			var_test = plcin.getMission_PosAllow();

		}
		mfio.setOutputX3Pin1(true); //Datalock substitute
	}
	
	private void mWait() {
		plcout.setMission_Run(false);
		plcout.setMission_AtPos(false);
		plcout.setMission_ExitDone(false);
		while (plcin.getMission_Start()==false){
			ThreadUtil.milliSleep(50);
		}
		if(plcout.getMission_IndexFBK()==0){
			ThreadUtil.milliSleep(50);
		}
		else {
			plcout.setMission_Result(0);
			plcout.setMission_Run(true);
		}
	}

	private void resetPlcOutput() {
		plcout.setMission_AtPos(false);
		plcout.setMission_IndexFBK(0);
		plcout.setMission_ExitDone(false);
		plcout.setMission_Result(0);
		plcout.setMission_Run(false);
		plcout.setApp_ResetDone(false);
	}
}
