/*
 *
 * PickIT basic example application for a KUKA IIWA mounted camera - (C)Intermodalics BVBA
 * 
 * Ruben Smits - ruben.smits@intermodalics.eu
 * Adolfo Rodriguez - adolfo.rodriguez@intermodalics.eu
 * Dominick Vanthienen - dominick.vanthienen@intermodalics.eu
 * 
 */

package application;

import java.io.IOException;

import pickit.PickIt;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.executionModel.CommandInvalidException;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;
import com.kuka.roboticsAPI.motionModel.Spline;

import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;

/**
 * Implementation of a sample robot application.
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
public class RobotMountedPickItBasicApplication extends RoboticsAPIApplication {
	private Controller kuka_Sunrise_Cabinet_1;
	private LBR lbr;
	private Tool gripper;
	private PickIt pickit;
	
	private class PickingConfig {
		// All units are in mm and degrees.
		public Transformation ee_T_tool = Transformation.ofTranslation(0, 0, 370); // long tool of 0.37m
		public Transformation tool_T_object = Transformation.ofDeg(0, 0, 0, 0, 180, 0);
		public double pre_pick_offset = 300;
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
		pre_pick_pose.setZ(picking_config.pre_pick_offset);
		return pre_pick_pose;
	}
	
	@Override
	public void initialize() {
		// Robot initialization
		kuka_Sunrise_Cabinet_1 = getController("KUKA_Sunrise_Cabinet_1");
		lbr = (LBR) getDevice(kuka_Sunrise_Cabinet_1, "LBR_iiwa_14_R820_1");
		gripper = getApplicationData().createFromTemplate("Gripper");
		
		pickit = new PickIt(lbr);
		System.out.println("Opening pickit socket ...");
		pickit.pickit_socket_open("172.31.1.2", 30001);
	}
	
	@Override
	public void dispose() {
		super.dispose();
	}

	@Override
	public void run() {
		try{
			gripper.attachTo(lbr.getFlange());
			System.out.println("Ask operator what to do ...");
			// show a selection pop-up
			while (selection() == 0)
			{
				;
			}
			System.out.println("Start picking sequence");
			// position from where to observe the objects with the camera
			JointPosition detection_pose = new JointPosition(
					Math.toRadians(0.0),
					Math.toRadians(7.0),
					Math.toRadians(0.0),
					Math.toRadians(-75.0),
					Math.toRadians(0.0),
					Math.toRadians(92.0),
					Math.toRadians(-1));
			try {
				lbr.move(ptp(detection_pose).setJointVelocityRel(picking_config.medium_vel));
			} catch (CommandInvalidException e) {
				System.out.println("Unable to move to detection pose. Exiting...");
				return;
			}
			System.out.println("Configuring pickit ...");
			// fill in here the id of the product as indicated between brackets next to the product name
			// on the configuration tab of the Pick-it web interface
			int pickit_product_id = 12;
			// fill in here the id of the setup as indicated between brackets next to the setup name
			// on the configuration tab of the Pick-it web interface
			int pickit_setup_id = 7;
			if (!pickit.pickit_configure(pickit_setup_id,
										 pickit_product_id)) {
				return;
			}
			while (pickit.pickit_is_running()) {
				if (pickit.pickit_remaining_objects() == 0) {
				    // Move to detection frame.
					lbr.move(ptp(detection_pose).setJointVelocityRel(picking_config.medium_vel));
					System.out.println("New Pick-it detection ...");
					// Required to make sure that the pcl is captured after coming to rest.
					Thread.sleep(350);
					pickit.pickit_look_for_object();
				} else {
					System.out.println("Get next Pick-it object ...");
					pickit.pickit_next_object();
				}
				int trycount = 0;
				while (!pickit.pickit_has_response() && trycount < 30) {
					Thread.sleep(100);
					trycount++;
				}
				if (trycount == 30) {
					System.out.println("Did not receive answer within 3 seconds");
					continue;
				}
				System.out.println("Received next Pick-it object ...");
								
				if (pickit.pickit_object_found()) {
					Transformation pickit_pose = pickit.pickit_get_pose();
		        	Frame base_T_current_ee =  lbr.getCurrentCartesianPosition(lbr.getFlange()).copy();
					Frame pick_pose = computePickPose(picking_config, base_T_current_ee, pickit_pose);
					Frame pre_pick_pose = computePrePickPose(picking_config, pick_pose);
					try {
						// Move to picking frame.
						Spline detect_to_pick_traj = new Spline(
							spl(pre_pick_pose).setJointVelocityRel(picking_config.medium_vel),
							lin(pick_pose).setJointVelocityRel(picking_config.slow_vel));
						lbr.move(detect_to_pick_traj);
 						// Retract: Move to detection frame.
 						lbr.move(ptp(detection_pose).setJointVelocityRel(picking_config.medium_vel));
						
						while (!pickit.pickit_has_response()) {
							Thread.sleep(100);
						}
					} catch (CommandInvalidException e) {
						System.out.println("Unable to move to object, going to next detection ...");
					}
				}
			}
			pickit.pickit_socket_close();
		} catch (InterruptedException e) {
			System.out.println("Interrupted Robot Application");
			try {
				pickit.pickit_socket_close();
			} catch (IOException ee) {
				System.err.println("IOError during pickit socket close");
				//e.printStackTrace();
			}
		} catch (IOException e) {
			System.out.println("IOException during Robot application");
			try {
				pickit.pickit_socket_close();
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
				calibrate();
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
	private void calibrate() throws InterruptedException {
		int time_before = 500; // ms
		int time_after = 6000; // ms
		System.out.println("Starting Multi Pose Calibration ... ");
		lbr.move(ptp(getApplicationData().getFrame("/Calib_pose1")));
		Thread.sleep(time_before);
		pickit.pickit_do_calibration();
		Thread.sleep(time_after);
		lbr.move(ptp(getApplicationData().getFrame("/Calib_pose2")));
		Thread.sleep(time_before);
		pickit.pickit_do_calibration();
		Thread.sleep(time_after);		
		lbr.move(ptp(getApplicationData().getFrame("/Calib_pose3")));
		Thread.sleep(time_before);
		pickit.pickit_do_calibration();
		Thread.sleep(time_after);
		lbr.move(ptp(getApplicationData().getFrame("/Calib_pose4")));
		Thread.sleep(time_before);
		pickit.pickit_do_calibration();
		Thread.sleep(time_after);
		lbr.move(ptp(getApplicationData().getFrame("/Calib_pose5")));
		Thread.sleep(time_before);
		pickit.pickit_do_calibration();
		Thread.sleep(time_after);
		lbr.move(ptp(getApplicationData().getFrame("/Calib_pose1")));
        System.out.println("Finished collecting calibration poses ... ");
	}
	
	public static void main(String[] args) {
		RobotMountedPickItBasicApplication app = new RobotMountedPickItBasicApplication();
		app.runApplication();
	}
}
