package application.Pickit;


import static eluxLibs.Utils.*;
import eluxLibs.*;
//import pickit.PickIt;

import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Named;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.executionModel.CommandInvalidException;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;

public class PickItApp extends RoboticsAPIApplication {
	private Controller kuka_Sunrise_Cabinet_1;
	private LBR lbr;
	//private Tool gripper;
	private HandlerPickIt pickit;
	
	@Inject private Plc_inputIOGroup 	plcin;
	@Inject private Plc_outputIOGroup 	plcout;
	private MediaFlangeIOGroup mediaFlangeIOGroup;
	@Inject	@Named("PickItGripper") 		private Tool 		gripper;
	
	// Custom modularizing handler objects
			@Inject private HandlerMFio	mf = new HandlerMFio(mediaFlangeIOGroup);
			@Inject private HandlerPLCio plc = new HandlerPLCio(mf, plcin, plcout);
			@Inject private HandlerMov move = new HandlerMov(mf);
			@Inject private HandlerPad pad = new HandlerPad(mf);
	
	private class PickingConfig {
		// All units are in mm and degrees.
		public Transformation ee_T_tool = Transformation.ofTranslation(0, 0, 223); // long tool of 0.37m
		public Transformation tool_T_object = Transformation.ofDeg(0, 0, 0, 0, 180, 0);
		public double pre_pick_offset = 100;
		public double slow_vel = 0.15 / 2.0;  // Same factor for all joints.
		public double[] medium_vel = {0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 1.0};  // Allow axis 7 to move at full speed.
	}

	private PickingConfig picking_config = new PickingConfig();
	
	/**
	 * Function to compute the pick frame from object frame returned by Pick-it
	 * @param picking_config
	 * @param base_T_current_ee
	 * @param pickit_pose
	 * @return
	 */
	private Frame computePickPose(PickingConfig picking_config,
            						Frame base_T_current_ee, Transformation pickit_pose) {
		Transformation base_T_object = pickit_pose;		
		Frame pick_pose = base_T_current_ee.copy();
		pick_pose.setTransformationFromParent(
			base_T_object.compose(picking_config.tool_T_object.invert()
			.compose(picking_config.ee_T_tool.invert())));
		return pick_pose;
	}
	
	/**
	 * Function to compute a way-point above the pick frame
	 * @param picking_config
	 * @param pick_pose
	 * @return
	 */
	private Frame computePrePickPose(PickingConfig picking_config, Frame pick_pose) {
		Frame pre_pick_pose = pick_pose.copy();
		//pre_pick_pose.setZ(picking_config.pre_pick_offset);
		pre_pick_pose.setZ(pre_pick_pose.getZ() + picking_config.pre_pick_offset);
		return pre_pick_pose;
	}
	
	@Override public void initialize() {
		// Robot initialization
		kuka_Sunrise_Cabinet_1 = (Controller) getContext().getControllers().toArray()[0]; //getController("KUKA_Sunrise_Cabinet_1");
		lbr = (LBR) kuka_Sunrise_Cabinet_1.getDevices().toArray()[0]; //(LBR) getDevice(kuka_Sunrise_Cabinet_1, "LBR_iiwa_14_R820_1");
		gripper = getApplicationData().createFromTemplate("PickItGripper");
		
		pickit = new HandlerPickIt(lbr, "192.168.2.12", 30001);
	}
	
	@Override public void dispose() { super.dispose(); }

	@Override public void run() {
		try{
			gripper.attachTo(lbr.getFlange());
			while (selection() == 0) { ; }
			System.out.println("Start picking sequence");
			try {
				lbr.move(ptp(getApplicationData().getFrame("/_PickIt/Scan")).setJointVelocityRel(picking_config.medium_vel));
			} catch (CommandInvalidException e) {
				System.out.println("Unable to move to detection pose. Exiting...");
				return;
			}
			sleep();
			System.out.println("Configuring pickit ...");
			if (!pickit.config(3, 2)) {
				return;
			}
			while (pickit.isRunning()) {
				if (pickit.getRemainingObj() == 0) {
				    // Move to detection frame.
					lbr.move(ptp(getApplicationData().getFrame("/_PickIt/Scan")).setJointVelocityRel(picking_config.medium_vel));
					System.out.println("New Pick-it detection ...");
					// Required to make sure that the pcl is captured after coming to rest.
					Thread.sleep(350);
					pickit.doScanForObj();
				} else {
					System.out.println("Get next Pick-it object ...");
					pickit.doCalcNextObj();
				}
				int trycount = 0;
				while (!pickit.isReady() && trycount < 100) {
					Thread.sleep(100);
					trycount++;
				}
				if (trycount == 30) {
					System.out.println("Did not receive answer within 3 seconds");
					continue;
				}
				System.out.println("Received next Pick-it object ...");
								
				if (pickit.hasFoundObj()) {
					Transformation pickit_pose = pickit.getPickFrame();
		        	Frame base_T_current_ee =  lbr.getCurrentCartesianPosition(lbr.getFlange()).copy();
					Frame pick_pose = computePickPose(picking_config, base_T_current_ee, pickit_pose);
					Frame pre_pick_pose = computePrePickPose(picking_config, pick_pose);
					pickit.doSendPickFrame();
					Thread.sleep(150);
					padLog("The pick ID is " + pickit.getPickID());
					try {
						// Move to picking frame.
						/*
						Spline detect_to_pick_traj = new Spline(
							spl(pre_pick_pose).setJointVelocityRel(picking_config.medium_vel),
							lin(pick_pose).setJointVelocityRel(picking_config.slow_vel));
						gripper.move(detect_to_pick_traj);
						*/
						lbr.move(ptp(pre_pick_pose));
						lbr.move(lin(pick_pose));
						plc.closeGripper();
						lbr.move(lin(pre_pick_pose));
						switch (pickit.getObjType()) {
							case 1:
								lbr.move(ptp(getApplicationData().getFrame("/_PickIt/Pole_H/_4_Jig_approach_Z")));
								lbr.move(lin(getApplicationData().getFrame("/_PickIt/Pole_H/_5_Jig_pos")));
								plc.openGripper();
								lbr.move(lin(getApplicationData().getFrame("/_PickIt/Pole_H/_4_Jig_approach_Z")));
								break;
							case 2:
								lbr.move(ptp(getApplicationData().getFrame("/_PickIt/Pole_H/_3_Approach_Z")));
								lbr.move(lin(getApplicationData().getFrame("/_PickIt/Pole_H")));
								plc.openGripper();
								lbr.move(lin(getApplicationData().getFrame("/_PickIt/Pole_H/_1_Approach_X")));
								break;
							default: 
								break;
						}
 						//lbr.move(ptp(detection_pose).setJointVelocityRel(picking_config.medium_vel));
						padLog("Placed model type " + pickit.getPickID());
						while (!pickit.isReady()) {
							Thread.sleep(100);
						}
						padLog("Placed model type " + pickit.getPickID());
					} catch (CommandInvalidException e) {
						System.out.println("Unable to move to object, going to next detection ...");
					}
					padLog("Placed model type " + pickit.getPickID());
				}
			}
			pickit.socketClose();
		} catch (InterruptedException e) {
			System.out.println("Interrupted Robot Application");
			try {
				pickit.socketClose();
			} catch (IOException ee) {
				System.err.println("IOError during pickit socket close");
				//e.printStackTrace();
			}
		} catch (IOException e) {
			System.out.println("IOException during Robot application");
			try {
				pickit.socketClose();
			} catch (IOException ee) {
				System.err.println("IOError during pickit socket close");
				//e.printStackTrace();
			}
		}
	}

	/**
	 * Show selection pop-up for the user to choose between calibration or picking.
	 * @return
	 * @throws InterruptedException
	 */
	private int selection() throws InterruptedException {
		int ret = 99;
		while (ret != 0) {
			ret = getApplicationUI().displayModalDialog(
					ApplicationDialogType.QUESTION, "What's next?", "Multi-pose calibration", "Start picking");
			switch (ret) {
			case 0:
				doCalibration();
				return ret;
			case 1:
				return ret;				
			}
		}
		return ret;
	}
	
	/**
	 * Calibration function, requires 5 calibration poses to be taught-in, named Calib_pose1 to Calib_pose5
	 * @throws InterruptedException
	 */
	private void doCalibration() throws InterruptedException {
		int time_before = 500; // ms
		int time_after = 6000; // ms
		System.out.println("Starting Multi Pose Calibration ... ");
		lbr.move(ptp(getApplicationData().getFrame("/_PickIt/Calib/P1")));
		Thread.sleep(time_before);
		pickit.doCalibration();
		Thread.sleep(time_after);
		lbr.move(ptp(getApplicationData().getFrame("/_PickIt/Calib/P2")));
		Thread.sleep(time_before);
		pickit.doCalibration();
		Thread.sleep(time_after);		
		lbr.move(ptp(getApplicationData().getFrame("/_PickIt/Calib/P3")));
		Thread.sleep(time_before);
		pickit.doCalibration();
		Thread.sleep(time_after);
		lbr.move(ptp(getApplicationData().getFrame("/_PickIt/Calib/P4")));
		Thread.sleep(time_before);
		pickit.doCalibration();
		Thread.sleep(time_after);
		lbr.move(ptp(getApplicationData().getFrame("/_PickIt/Calib/P5")));
		Thread.sleep(time_before);
		pickit.doCalibration();
		Thread.sleep(time_after);
		lbr.move(ptp(getApplicationData().getFrame("/_PickIt/Calib")));
        System.out.println("Finished collecting calibration poses ... ");
	}
	/*
	public static void main(String[] args) {
		RobotMountedPickItBasicApplication app = new RobotMountedPickItBasicApplication();
		app.runApplication();
	}
	*/
}
