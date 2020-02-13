package eluxLibs;

import static eluxLibs.Utils.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;
import javax.inject.Inject;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;

public class HandlerPickIt {
	private LBR kiwa;
	private HandlerMov move;
	private _syncDataThread _syncData = new _syncDataThread();
	private String _scanFramePath;
	private double _relSpeed;
	private int _stabilizeTime;
	private int _timeout;

	/// Pickit data ///
	private int _setup_id = 0;
	private int _product_id = 0;
	private volatile Transformation _pick_frame;
	private volatile Transformation _approach_frame;
	private volatile double _obj_age = 0;
	private volatile int _obj_type = 0;
	private volatile double[] obj_size = {0, 0, 0};
	private volatile int _obj_remaining = 0;
	private volatile int _pick_id = 0;
	private volatile double _pickref_id = 0;
	private Socket _socket;
	private DataOutputStream to_pickit;
	private DataInputStream from_pickit;
	private byte[] _data_to_pickit = new byte[12 * 4];
	private byte[] _data_from_pickit = new byte[16 * 4];

	@Inject public HandlerPickIt(HandlerMov _move) {		// Constructor
		this.move = _move;
	//	this.kiwa = this.move.getMovObj();
	}
	
	// GETTERS ------------------------------------------
	
	public int getPickID() { return _pick_id; }
	public int getObjType() { return _obj_type; }
	public int getRemainingObj() { return _obj_remaining; }
	public boolean isRunning() { return _status != _STOPPED && _status != _ERROR; }
	public boolean isReady() { return _status != _WAITING; }
	public boolean hasFoundObj() { return _status == _OBJ_FOUND; }
	public Transformation getPickFrame() { return _pick_frame; }
	
	public int getBox(boolean scan) {
		int timecounter = 0;
		Frame pickFrame = null;
		
		if (scan || this.getRemainingObj() == 0) {
			move.PTP(_scanFramePath, _relSpeed);
			waitMillis(350);
			this.doScanForObj();
		}
		else this.doCalcNextObj();
		while(!this.isReady()) {
			waitMillis(100);
			timecounter += 100;
			if (timecounter >= _timeout) {
				padErr("Timeout is overdue, PickIt didn't answer");
				return -1;
			}
		}
		if (this.hasFoundObj()) {
			return getRemainingObj();
		}
		else {
			padLog("Pickit was unable to find any objects");
			return 0;
		}
		
	}
	
	// SETTERS ------------------------------------------

	public synchronized void doCalibration() {
		waitMillis(50);		// default = 500ms
		//_status = _WAITING;
		//_command = _CALIBRATE;
		waitMillis(600);	// default = 6000ms
	}

	public synchronized void doScanForObj() {
		padLog("Pickit scan for objects");
		_status = _WAITING;
		_command = _SCAN_FOR_OBJ;
	}

	public synchronized void doWaitForObj() {
		padLog("Pickit wait for objects");
		_status = _WAITING;
		_command = _WAIT_FOR_OBJ;
	}

	public synchronized void doCalcNextObj() {
		padLog("Pickit calc next object to pick");
		_status = _WAITING;
		_command = _NEXT_OBJ;
	}
  
	public synchronized void doSendPickFrame() {
	    padLog("Pickit get pick frame data");
	    _status = _WAITING;
	    _command = _GET_PICKFRAME;
	}
  
	public synchronized boolean config(int setup_id, int product_id, String scanFramePath, double relSpeed, int stabilizeTime, int timeout) {
		_setup_id = setup_id;
		_product_id = product_id;
		_scanFramePath = scanFramePath;
		_relSpeed = relSpeed;
		_stabilizeTime = stabilizeTime;
		_timeout = timeout;
		
		_command = _CONFIGURE;
	/*	while (_status != _CONFIG_OK) {
			if (_status == _CONFIG_FAILED) {
				padErr("Pick-it did NOT configure correctly.");
				return false;
			}
			waitMillis(1000);
			padLog("Waiting for PickIt config confirmation");
			padLog(_status);
		}*/
		
		padLog("PickIt configured with Setup ID = " + setup_id + "and Product ID = " + product_id);
		return true;
	}
	
	public boolean init(String pickit_ip, int pickit_port) {
		padLog("Opening Ethernet communication _socket");
		_obj_remaining = 0;
		_status = _WAITING;
		try {
			_socket = new Socket(pickit_ip, pickit_port);
			to_pickit = new DataOutputStream(_socket.getOutputStream());
			from_pickit = new DataInputStream(_socket.getInputStream());
			_syncData.start();
		} catch (Exception e) {
			padErr(e.toString());
			return false;
		}
		return true;
	}

	public void terminate() {
		_syncData.terminate();
		try {
			_syncData.join(200);
		} catch (Exception e) {
			padErr("Failed to stop pickit threads");
		}
		padLog("Closing PickIt _socket");
		try {
			to_pickit.close();
			from_pickit.close();
			_socket.close();
		} catch (Exception e) {
			padErr("Exception during closing pickit comm");
		}
	}
	
	// HIGHER LEVEL ROBOT FUNCTIONS ------------------------------------------
	private class PickingConfig {
		// All units are in mm and degrees.
		public Transformation ee_T_tool = Transformation.ofTranslation(0, 0, 223); // TCP offset of 0.223m
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
	public Frame computePickPose(Frame base_T_current_ee, Transformation pickit_pose) {
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
	public Frame computePrePickPose(Frame pick_pose) {
		Frame pre_pick_pose = pick_pose.copy();
		//pre_pick_pose.setZ(picking_config.pre_pick_offset);
		pre_pick_pose.setZ(pre_pick_pose.getZ() + picking_config.pre_pick_offset);
		return pre_pick_pose;
	}

	// PRIVATE Handler functions ------------------------------------------
	
	private boolean receiveData() {
		try { from_pickit.readFully(_data_from_pickit); }
		catch (IOException e) { return false; }
		
		IntBuffer intBuf = ByteBuffer.wrap(_data_from_pickit)
			.order(ByteOrder.BIG_ENDIAN)
			.asIntBuffer();
		int[] data = new int[intBuf.remaining()];
		intBuf.get(data);
		if (data[14] != ROBOT_TYPE) {
			padErr("Pick-it is not configured to communicate with KUKA LBR.");
			return false;
		}
		if (data[15] != INTERFACE_VERSION) {
			padErr("The Pickit-it interface version does not match the version of this program.");
		}
		if (data[13] == _GET_PICKFRAME_OK) {		// Review
			_approach_frame = Transformation.ofDeg(
        		(double)data[0]/_FACTOR * 1000, (double)data[1]/_FACTOR * 1000, (double)data[2]/_FACTOR * 1000,
        		(double)data[3]/_FACTOR,        (double)data[4]/_FACTOR,        (double)data[5]/_FACTOR);
			_pick_id = data[8];		// PICKFRAME
			_pickref_id = (double)data[7]/_FACTOR;
			_status = data[13];
		} else {
			_pick_frame = Transformation.ofDeg(
				(double)data[0]/_FACTOR * 1000, (double)data[1]/_FACTOR * 1000, (double)data[2]/_FACTOR * 1000,
				(double)data[3]/_FACTOR,        (double)data[4]/_FACTOR,        (double)data[5]/_FACTOR);
			_obj_age = (double)data[7]/_FACTOR;
			_obj_type = data[8];
			obj_size[0] = (double)data[9]/_FACTOR;
			obj_size[1] = (double)data[10]/_FACTOR;
			obj_size[2] = (double)data[11]/_FACTOR;
			_obj_remaining = data[12];
			_status = data[13];
		}
		return true;
	}

	private boolean sendData() throws Exception {
		ByteBuffer byteBuffer = ByteBuffer.wrap(_data_to_pickit);
		Frame pose = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
		byteBuffer.putInt((int)(_FACTOR * pose.getX() / 1000));                // from mm to m    
		byteBuffer.putInt((int)(_FACTOR * pose.getY() / 1000));                // from mm to m    
		byteBuffer.putInt((int)(_FACTOR * pose.getZ() / 1000));                // from mm to m    
		byteBuffer.putInt((int)(_FACTOR * pose.getAlphaRad() * 180 / Math.PI));  //EulerZyx == yaw;  
		byteBuffer.putInt((int)(_FACTOR * pose.getBetaRad()  * 180 / Math.PI));  //EulerzYx == pitch;
		byteBuffer.putInt((int)(_FACTOR * pose.getGammaRad() * 180 / Math.PI));  //EulerzyX == roll; 
		byteBuffer.putInt((int)(_FACTOR * 0.0));
		byteBuffer.putInt(_command);
		byteBuffer.putInt(_setup_id);
		byteBuffer.putInt(_product_id);
		byteBuffer.putInt(ROBOT_TYPE);
		byteBuffer.putInt(INTERFACE_VERSION);
		_command = -1;
		try { to_pickit.write(_data_to_pickit); } 
		catch (IOException e) { return false; }
		return true;
	}
	
	private class _syncDataThread extends Thread {
		private volatile boolean running = true;
		public void terminate() {
			padLog("Terminating _syncData Thread");
			running = false;
		}
		@Override public void run() {
			padLog("Running _syncData Thread");
			while(running) {
				try {
					receiveData();
					if (running && !sendData()) {
						padErr("Failed to write");
						running = false;
					}
					Thread.sleep(10);
				} catch (Exception e) {
					padErr("Closed.");
					running = false; 
				}
			}
			padLog("Stopped RobotDataThread");
		}
	}
	
	// PICKIT SOCKET DATA EXCHANGE CODES -------------------------------
	
	/// Pickit object types ///
	public static final int _OBJ_SURFACE = 31;
	public static final int _OBJ_CYLINDER = 32;
	public static final int _OBJ_BALL = 33;
	public static final int _OBJ_BOX = 34;
	public static final int _OBJ_CONE = 35;

	/// Pickit status constants ///
	public volatile int _status = 0;
	private static final int _WAITING = 0;
	private static final int _OBJ_FOUND = 20;
	private static final int _OBJ_FOUND_NONE = 21;
	private static final int _ERROR = 30;
	private static final int _STOPPED = 31;
	private static final int _CONFIG_OK = 40;
	private static final int _CONFIG_FAILED = 41;
	private static final int _GET_PICKFRAME_OK = 70;
	private static final int _GET_PICKFRAME_FAILED = 71;

	/// Robot _commands constants ///
	private int _command = -1;
	private static final int _CALIBRATE = 10;
	private static final int _SCAN_FOR_OBJ = 20;
	private static final int _WAIT_FOR_OBJ = 21;
	private static final int _NEXT_OBJ = 30;
	private static final int _CONFIGURE = 40;
	private static final int _GET_PICKFRAME = 70;
	
	private static final int _FACTOR = 10000;
	private static final int ROBOT_TYPE = 5;
	private static final int INTERFACE_VERSION = 11;
}
