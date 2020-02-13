package application;


import javax.inject.Inject;

import com.kuka.common.ThreadUtil;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;

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
public class ProvaNoPLC extends RoboticsAPIApplication {
	@Inject
	private LBR lbr;
	@Inject 							private Plc_inputIOGroup 			plcin;
	@Inject 							private Plc_outputIOGroup 		plcout;
	@Inject 							private MediaFlangeIOGroup 		mfio;
	private JointPosition home;

	@Override
	public void initialize() {
		// initialize your application here
		home = new JointPosition( Math.toRadians(138.32), Math.toRadians(41.22),  Math.toRadians(-9.84), Math.toRadians(-100.07),  Math.toRadians(54.38),  Math.toRadians(-55.9),  Math.toRadians(-30.39));
		lbr.setHomePosition(home);
	}

	@Override
	public void run() {
		// your application execution starts here
		plcout.setPinza_Apri(true);
		lbr.move(ptp(getApplicationData().getFrame("/home")));
		plcout.setPinza_Apri(false);
		lbr.moveAsync(ptp(getApplicationData().getFrame("/AggancioHousing/Approccio")).setBlendingCart(10));
		lbr.move(ptp(getApplicationData().getFrame("/AggancioHousing")));
		plcout.setPinza_Chiudi(true);
		ThreadUtil.milliSleep(1500);
		plcout.setPinza_Chiudi(false);
		lbr.moveAsync(linRel(-350,0,0).setBlendingCart(10));
		lbr.moveAsync(ptp(getApplicationData().getFrame("/AggancioCarrozzeria/Approcciopart1")).setBlendingCart(50));
		lbr.move(ptp(getApplicationData().getFrame("/AggancioCarrozzeria/ApproccioPart2")));
		lbr.move(lin(getApplicationData().getFrame("/AggancioCarrozzeria")).setCartVelocity(100));
		ThreadUtil.milliSleep(500);
		lbr.moveAsync(lin(getApplicationData().getFrame("/AggancioCarrozzeria/ApproccioPart2")).setBlendingCart(50));
		lbr.move(ptp(getApplicationData().getFrame("/DepositoFinale/Approccio")));
		lbr.move(lin(getApplicationData().getFrame("/DepositoFinale")));
		plcout.setPinza_Apri(true);
		ThreadUtil.milliSleep(500);
		plcout.setPinza_Apri(false);
		lbr.move(ptp(getApplicationData().getFrame("/DepositoFinale/Approccio")));
	}
}