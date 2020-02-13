/*
 *
 * PickIT APPLICATON - (C) Intermodalics BVBA
 * 
 * Ruben Smits - ruben.smits@intermodalics.eu
 * Adolfo Rodriguez - adolfo.rodriguez@intermodalics.eu
 * Dominick Vanthienen - dominick.vanthienen@intermodalics.eu
 * 
 */

package application.pickit;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import static eluxLibs.Utils.*;
import eluxLibs.*;

import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
//import pickit.PickIt;

// import com.kuka.generated.ioAccess.FlexFellowIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;

import com.kuka.roboticsAPI.conditionModel.BooleanIOCondition;
import com.kuka.roboticsAPI.conditionModel.ForceCondition;
import com.kuka.roboticsAPI.conditionModel.ICallbackAction;
import com.kuka.roboticsAPI.conditionModel.MotionPathCondition;
import com.kuka.roboticsAPI.conditionModel.ReferenceType;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.executionModel.CommandInvalidException;
import com.kuka.roboticsAPI.executionModel.IFiredConditionInfo;
import com.kuka.roboticsAPI.executionModel.IFiredTriggerInfo;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.math.CoordinateAxis;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;
import com.kuka.roboticsAPI.ioModel.AbstractIO;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.motionModel.Spline;

import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;

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
public class PickItExtendedApplication extends RoboticsAPIApplication {
	private Controller kuka_Sunrise_Cabinet_1;
	private LBR lbr;
//	private FlexFellowIOGroup flexFellowIOGroup;
	@Inject private Plc_inputIOGroup 	plcin;
	@Inject private Plc_outputIOGroup 	plcout;
	private MediaFlangeIOGroup mediaFlangeIOGroup;
	//private Tool gripper;
	private PickIt pickit;
	
	@Inject	@Named("PickItGripper") 		private Tool 		gripper;
	
	// Custom modularizing handler objects
		@Inject private HandlerMFio	mf = new HandlerMFio(mediaFlangeIOGroup);
		@Inject private HandlerPLCio plc = new HandlerPLCio(mf, plcin, plcout);
		@Inject private HandlerMov move = new HandlerMov(mf);
		@Inject private HandlerPad pad = new HandlerPad(mf);
	
	private class PickingConfig {
		// All units are in mm and degrees.
		public Transformation ee_T_tool = Transformation.ofTranslation(0, 0, 347); // white tube tool
		//public Transformation ee_T_tool = Transformation.ofTranslation(0, 0, 160); // no white tube

		public Transformation tool_T_object = Transformation.ofDeg(0, 0, 0, 0, 180, 0);
		// Below z value should be smaller than the distance between prepick_high_z and top of bin.
		public Transformation pick_T_prepick_low = Transformation.ofTranslation(0, 0, -50);
		public double prepick_high_z = 300;
		public double slow_vel = 0.15 / 2.0;  // Same factor for all joints.
		public double[] medium_vel = {0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 1.0};  // Allow axis 7 to move at full speed.
		public double[] fast_vel = {0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 1.0};  // Allow axis 7 to move at full speed.
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
		pre_pick_pose.setZ(picking_config.prepick_high_z);
		return pre_pick_pose;
	}

	/**
	 * Function to compute a way-point below the pick frame
	 * @param picking_config
	 * @param pick_pose
	 * @return
	 */
	private Frame computePrePickLowFrame(PickingConfig picking_config, Frame base_T_pick) {
		Frame base_T_prepick_low = base_T_pick.copy();
		base_T_prepick_low.setTransformationFromParent(
			base_T_pick.getTransformationFromParent().compose(picking_config.pick_T_prepick_low));
		return base_T_prepick_low;
	}
	
	private boolean doRelease() throws InterruptedException {
		long timeout_ns = 500000000;  // NOTE: magic value!
//		flexFellowIOGroup.setDisableVacuum(true);
		long release_start_time_ns = System.nanoTime();
		while (timeout_ns > 0){ // flexFellowIOGroup.getIsVacuumLow() &&
			//   System.nanoTime() - release_start_time_ns < timeout_ns) {
			Thread.sleep(50);
		}
		return true; // !flexFellowIOGroup.getIsVacuumLow();
	}
	
	private void setSignalLight(int active_bin_config_id) {
		boolean green_on = active_bin_config_id == 0;
		boolean blue_on = !green_on;
//		flexFellowIOGroup.setSignalLightGreen(green_on);
//		flexFellowIOGroup.setSignalLightBlue(blue_on);
	}
	private void unsetSignalLight() {
//		flexFellowIOGroup.setSignalLightGreen(false);
//		flexFellowIOGroup.setSignalLightGreen(false);
	}
	
	private class BinConfig {
		public JointPosition detection_joint_positions;
		public Frame base_T_drop;
		public int pickit_setup_id;
		public int pickit_product_id;
	}

	private BinConfig[] bin_configs = new BinConfig[2];
	private int active_bin_config_id = 0;
	
	BooleanIOCondition have_vacuum_condition;
	BooleanIOCondition lost_vacuum_condition;
	MotionPathCondition enable_vacuum_condition;
	ForceCondition force_condition;
	ICallbackAction enable_vacuum_action;
	
	@Override
	public void initialize() {
		// To customize this demo to new bin locations, bin sizes or products to
		// pick, one must, for each bin:
		// - Determine the new joint configurations for Pick-it object detection
		//   (over one bin) and object dropping (over the other bin).
		// - Create the new Pick-it setup and/or product files, and reference
		//   their indices.
		bin_configs[0] = new BinConfig(); 
		bin_configs[0].detection_joint_positions = new JointPosition(
			Math.toRadians(90.0),
			Math.toRadians(7.0),
			Math.toRadians(0.0),
			Math.toRadians(-75.0),
			Math.toRadians(0.0),
			Math.toRadians(92.0),
			Math.toRadians(-1));
		bin_configs[0].base_T_drop = new Frame(620, -170, 300,
			Math.toRadians(180), Math.toRadians(0), Math.toRadians(180));
		bin_configs[0].pickit_product_id = 2;
		bin_configs[0].pickit_setup_id = 3;
		
		bin_configs[1] = new BinConfig();
		bin_configs[1].detection_joint_positions = new JointPosition(
			Math.toRadians(90.0),
			Math.toRadians(7.0),
			Math.toRadians(0.0),
			Math.toRadians(-75.0),
			Math.toRadians(0.0),
			Math.toRadians(92.0),
			Math.toRadians(-1));
		bin_configs[1].base_T_drop = new Frame(620, 170, 300,
			Math.toRadians(180), Math.toRadians(0), Math.toRadians(180));
		bin_configs[1].pickit_product_id = 12;
		bin_configs[1].pickit_setup_id = 8;
		
		// Robot initialization
		kuka_Sunrise_Cabinet_1 = (Controller) getContext().getControllers().toArray()[0]; //getController("KUKA_Sunrise_Cabinet_1");
		lbr = (LBR) kuka_Sunrise_Cabinet_1.getDevices().toArray()[0]; //(LBR) getDevice(kuka_Sunrise_Cabinet_1, "LBR_iiwa_14_R820_1");
//		flexFellowIOGroup = new FlexFellowIOGroup(kuka_Sunrise_Cabinet_1);
		mediaFlangeIOGroup = new MediaFlangeIOGroup(kuka_Sunrise_Cabinet_1);
		gripper = getApplicationData().createFromTemplate("Gripper");
		
		// Condition monitoring
//		AbstractIO vacuum_high_in = flexFellowIOGroup.getIO("IsVacuumHigh", false);
//		lost_vacuum_condition = new BooleanIOCondition(vacuum_high_in, false);
//		have_vacuum_condition = new BooleanIOCondition(vacuum_high_in, true);
		enable_vacuum_condition = new MotionPathCondition(
				ReferenceType.DEST, -150.0, 0);
		force_condition = ForceCondition.createNormalForceCondition(gripper.getFrame("/TCP"), CoordinateAxis.Z, 30.0);
		
		enable_vacuum_action = new ICallbackAction() {
			
			@Override
			public void onTriggerFired(IFiredTriggerInfo triggerInformation) {
				System.out.println("Enabling suction!");
	//			flexFellowIOGroup.setDisableVacuum(false);
			}
		};
		
		pickit = new PickIt(lbr);
		System.out.println("Opening pickit socket ...");
		pickit.pickit_socket_open("192.168.2.12", 30001);
//		flexFellowIOGroup.setDisableVacuum(true);
	}
	
	@Override
	public void dispose() {
		unsetSignalLight();
		mediaFlangeIOGroup.setLEDBlue(false);
	//	flexFellowIOGroup.setDisableVacuum(true);
		super.dispose();
	}

	@Override
	public void run() {
		try{
			gripper.attachTo(lbr.getFlange());
			System.out.println("Ask operator what to do ...");
			while (selection() == 0)
			{
				;
			}
			System.out.println("Start picking sequence");
			JointPosition detection_pose = bin_configs[active_bin_config_id].detection_joint_positions;
			try {
				lbr.move(ptp(detection_pose).setJointVelocityRel(picking_config.fast_vel));
			} catch (CommandInvalidException e) {
				System.out.println("Unable to move to detection pose. Exiting...");
				return;
			}
			System.out.println("Configuring pickit ...");
			if (!pickit.pickit_configure(bin_configs[active_bin_config_id].pickit_setup_id,
										 bin_configs[active_bin_config_id].pickit_product_id)) {
				return;
			}
			setSignalLight(active_bin_config_id);
			boolean pick_successful = false;
			int num_consecutive_no_picks = 0;
			while (pickit.pickit_is_running()) {
				mediaFlangeIOGroup.setLEDBlue(true);
				if (pickit.pickit_remaining_objects() == 0) {
				    // Move to detection frame.
					lbr.move(ptp(detection_pose).setJointVelocityRel(picking_config.fast_vel));
				}
				if (pick_successful || pickit.pickit_remaining_objects() == 0) {
					System.out.println("New Pick-it detection ...");
					// Required to make sure that the pcl is captured after coming to rest.
					Thread.sleep(350);
					pickit.pickit_look_for_object();
				} else {
					System.out.println("Get next Pick-it object ...");
					pickit.pickit_next_object();
				}
				pick_successful = false;  // Reset flag;
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
								
				mediaFlangeIOGroup.setLEDBlue(false);
				if (pickit.pickit_object_found()) {
					Transformation pickit_pose = pickit.pickit_get_pose();
		        	Frame base_T_current_ee =  lbr.getCurrentCartesianPosition(lbr.getFlange()).copy();
					Frame base_T_pick = computePickPose(picking_config, base_T_current_ee, pickit_pose);
					Frame base_T_prepick_high = computePrePickPose(picking_config, base_T_pick);
					Frame base_T_prepick_low = computePrePickLowFrame(picking_config, base_T_pick);
					try {
						// Move to picking frame.
						// We register two conditions:
						// - Enable vacuum when proximity to the goal is detected.
						// - Stop once we detect vacuum.
						Spline detect_to_pick_traj = new Spline(
							spl(base_T_prepick_high).setJointVelocityRel(picking_config.fast_vel),
							spl(base_T_prepick_low).setJointVelocityRel(picking_config.fast_vel),
							lin(base_T_pick).setJointVelocityRel(picking_config.slow_vel))
							/*.triggerWhen(enable_vacuum_condition, enable_vacuum_action).breakWhen(have_vacuum_condition)*/.breakWhen(force_condition);
						
						lbr.move(detect_to_pick_traj);
						
						// Pick object.
			//			pick_successful = flexFellowIOGroup.getIsVacuumHigh();
 						if (pick_successful) {
 							System.out.println("Pick succeeded!");
 							// Move to object dropping frame.
 							// We register a condition that will abort the motion if the pick
 							// is lost during execution.
 							Spline pick_to_drop_traj = new Spline(
 								lin(base_T_prepick_low).setJointVelocityRel(picking_config.slow_vel),
 								spl(base_T_prepick_high).setJointVelocityRel(picking_config.medium_vel),
 								spl(bin_configs[active_bin_config_id].base_T_drop).setJointVelocityRel(picking_config.medium_vel))
 								.breakWhen(lost_vacuum_condition);
 							IMotionContainer drop_motion_cmd = lbr.move(pick_to_drop_traj);
 							
 							// Release object.
 							boolean release_successful = doRelease();
 							if (!release_successful) {
 								System.out.println("Failed to detect object release!.");  // TODO: Take better action?.
 							}
 							
 							IFiredConditionInfo fired_cond_info = drop_motion_cmd.getFiredBreakConditionInfo();
 							if (fired_cond_info != null &&
 							  fired_cond_info.getFiredCondition().equals(lost_vacuum_condition)) {
 							  // Lost vacuum along the way.
 							  System.out.println("Lost pick!.");
 							  pick_successful = false;
 							}
 						}
 						
 						// Retract:
 						if (pick_successful) {
 						  // Move to detection frame.
 							lbr.move(ptp(detection_pose).setJointVelocityRel(picking_config.fast_vel));
 						} else {
 							System.out.println("Pick failed!");
 				//			flexFellowIOGroup.setDisableVacuum(true);
 							// Move to attempt picking the next object. Notice that this is not
 							// all the way back to the detection frame.
 							Spline pick_to_detect_traj = new Spline(
 								//lin(base_T_prepick_low).setJointVelocityRel(picking_config.slow_vel),
								spl(base_T_prepick_high).setJointVelocityRel(picking_config.fast_vel));
 							lbr.move(pick_to_detect_traj);
 						}
						
						
						while (!pickit.pickit_has_response()) {
							Thread.sleep(100);
						}
					} catch (CommandInvalidException e) {
						System.out.println("Unable to move to object, going to next detection ...");
					}
				}
				if (pick_successful) {
					num_consecutive_no_picks = 0;
				} else {
					// Detection round did not yield any valid picks, either because:
					// - There were no detections.
					// - There were detections, but they were unreachable by the robot.
					++num_consecutive_no_picks;
					System.out.format("Num consecutive zero detections: %d", num_consecutive_no_picks);
					if (num_consecutive_no_picks > 4) {  // NOTE: magic value!.
						// Switch to detecting objects in other bin.
						num_consecutive_no_picks = 0;
						active_bin_config_id = (active_bin_config_id + 1) % 2;
						System.out.println("Switching active bin");
						detection_pose = bin_configs[active_bin_config_id].detection_joint_positions;
						pickit.pickit_configure(bin_configs[active_bin_config_id].pickit_setup_id,
								                bin_configs[active_bin_config_id].pickit_product_id);
						setSignalLight(active_bin_config_id);
						try {
							lbr.move(ptp(detection_pose));
						} catch (CommandInvalidException e) {
							System.out.println("Unable to move to detection pose. Exiting...");
							return;
						}
					}
				}
			}
			pickit.pickit_socket_close();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private int selection() throws InterruptedException {
		int ret = 99;
		while (ret != 0) {

			ret = getApplicationUI().displayModalDialog(
					ApplicationDialogType.QUESTION, "What's next?", "Start Multi Pose Calibration", "Start picking");
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
	
	private void calibrate() throws InterruptedException {
		int time_before = 500; // ms
		int time_after = 6000; // ms
		System.out.println("Starting Multi Pose Calibration ... ");
		lbr.move(ptp(getApplicationData().getFrame("/_PickIt/Calib/P1")));
		Thread.sleep(time_before);
		pickit.pickit_do_calibration();
		Thread.sleep(time_after);
		lbr.move(ptp(getApplicationData().getFrame("/_PickIt/Calib/P2")));
		Thread.sleep(time_before);
		pickit.pickit_do_calibration();
		Thread.sleep(time_after);		
		lbr.move(ptp(getApplicationData().getFrame("/_PickIt/Calib/P3")));
		Thread.sleep(time_before);
		pickit.pickit_do_calibration();
		Thread.sleep(time_after);
		lbr.move(ptp(getApplicationData().getFrame("/_PickIt/Calib/P4")));
		Thread.sleep(time_before);
		pickit.pickit_do_calibration();
		Thread.sleep(time_after);
		lbr.move(ptp(getApplicationData().getFrame("/_PickIt/Calib/P5")));
		Thread.sleep(time_before);
		pickit.pickit_do_calibration();
		Thread.sleep(time_after);
		lbr.move(ptp(getApplicationData().getFrame("/_PickIt/Calib")));
        System.out.println("Finished collecting calibration poses ... ");
	}
	
	public static void main(String[] args) {
		PickItExtendedApplication app = new PickItExtendedApplication();
		app.runApplication();
	}
}
