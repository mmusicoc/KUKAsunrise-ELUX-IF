package application.Pickit;

import static eluxLibs.Utils.*;
import eluxLibs.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;

public class HandlerPickIt {	

	/// Configuration fields ///
	private int _setup_id = 0;
	private int _product_id = 0;

	private PickitDataThread pickit_data_thread = new PickitDataThread();
	private RobotDataThread robot_data_thread = new RobotDataThread();
	private LBR lbr;

	/// Pickit data ///
	private volatile Transformation _pick_frame;
	private volatile Transformation _approach_frame;
	private volatile double _obj_age = 0;
	private volatile int _obj_type = 0;
	private volatile double[] pickit_object_dim = {0, 0, 0};
	private volatile int _obj_remaining = 0;
	private volatile int _pick_id = 0;
	private volatile double _pickref_id = 0;
	private Socket socket;
	private DataOutputStream to_pickit;
	private DataInputStream from_pickit;
	private byte[] _data_to_pickit = new byte[12 * 4];
	private byte[] _data_from_pickit = new byte[16 * 4];
/*
	/// Pickit object types ///
	public static final int _OBJ_SURFACE = 31;
	public static final int _OBJ_CYLINDER = 32;
	public static final int _OBJ_BALL = 33;
	public static final int _OBJ_BOX = 34;
	public static final int _OBJ_CONE = 35; // UNUSED */ 

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

	/// Robot Commands constants ///
	private int command = -1;
	private static final int _CALIBRATE = 10;
	private static final int _SCAN_FOR_OBJ = 20;
	private static final int _WAIT_FOR_OBJ = 21;
	private static final int _NEXT_OBJ = 30;
	private static final int _CONFIGURE = 40;
	private static final int _GET_PICKFRAME = 70;
	
	private static final int _FACTOR = 10000;
	private static final int ROBOT_TYPE = 5;
	private static final int INTERFACE_VERSION = 11;
	
	/// Public functions ///

	public HandlerPickIt(LBR _lbr, String ipAdress, int socketPort) {		// Constructor
		this.lbr = _lbr;
		this.socketOpen(ipAdress, socketPort);
	}
	
	// GETTERS ------------------------------------------
	
	public int getPickID() { return _pick_id; }
	public int getObjType() { return _obj_type; }
	public int getRemainingObj() { return _obj_remaining; }
	public boolean isRunning() { return _status != _STOPPED && _status != _ERROR; }
	public boolean isReady() { return _status != _WAITING; }
	public boolean hasFoundObj() { return _status == _OBJ_FOUND; }
	public Transformation getPickFrame() { return _pick_frame; }
	
	// SETTERS ------------------------------------------

	public synchronized void doCalibration() { 
		padLog("Pickit do calibration");
		_status = _WAITING;
		command = _CALIBRATE;
	}

	public synchronized void doScanForObj() {
		padLog("Pickit scan for objects");
		_status = _WAITING;
		command = _SCAN_FOR_OBJ;
	}

	public synchronized void doWaitForObj() {
		padLog("Pickit wait for objects");
		_status = _WAITING;
		command = _WAIT_FOR_OBJ;
	}

	public synchronized void doCalcNextObj() {
		padLog("Pickit next object");
		_status = _WAITING;
		command = _NEXT_OBJ;
	}
  
	public synchronized void doSendPickFrame() {
	    padLog("Pickit get pick frame data");
	    _status = _WAITING;
	    command = _GET_PICKFRAME;
	}
  
	public synchronized boolean config(int setup_id, int product_id) throws InterruptedException {
		_setup_id = setup_id;
		_product_id = product_id;
		command = _CONFIGURE;
		while (_status != _CONFIG_OK) {
			if (_status == _CONFIG_FAILED) {
				padErr("Pick-it did NOT configure correctly.");
				return false;
			}
			Thread.sleep(10);
		}
		padLog("PickIt configured with Setup ID = " + setup_id + "and Product ID = " + product_id);
		return true;
	}
	
	public boolean socketOpen(String pickit_ip, int pickit_port) {
		padLog("Opening Ethernet communication socket");
		_obj_remaining = 0;
		_status = _WAITING;
		try {
			socket = new Socket(pickit_ip, pickit_port);
			to_pickit = new DataOutputStream(socket.getOutputStream());
			from_pickit = new DataInputStream(socket.getInputStream());
			System.out.println("Starting Pickit threads");
			pickit_data_thread.start();
			robot_data_thread.start();
		} catch (Exception e) {
			padErr(e.toString());
			return false;
		}
		return true;
	}

	public void socketClose() throws IOException {
		padLog("Stopping PickIt threads");
		pickit_data_thread.terminate();
		robot_data_thread.terminate();
		try {
			pickit_data_thread.join(200);
			robot_data_thread.join(200);
		} catch (InterruptedException e) {
			padErr("Failed to stop pickit threads");
		}
		padLog("Closing PickIt socket");
		try {
			to_pickit.close();
			from_pickit.close();
			socket.close();
		} catch (Exception e) {
			//e.printStackTrace();
			padErr("Exception during closing pickit comm");
		}
	}

	//////// Internal functions. Do not call directly. /////////
	private boolean receiveData() {
		try {
			from_pickit.readFully(_data_from_pickit);
			System.out.println("Received pickit data");
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		IntBuffer intBuf = ByteBuffer.wrap(_data_from_pickit)
			.order(ByteOrder.BIG_ENDIAN)
			.asIntBuffer();
		int[] array = new int[intBuf.remaining()];
		intBuf.get(array);
		if (array[14] != ROBOT_TYPE) {
			System.err.println("Pick-it is not configured to communicate with the JAVA API.");
			return false;
		}
		if (array[15] != INTERFACE_VERSION) {
			System.err.println("The Pickit-it interface version does not match the version of this program.");
		}
		if(array[13] == _GET_PICKFRAME_OK){
			_approach_frame = Transformation.ofDeg(
        		(double)array[0]/_FACTOR * 1000, (double)array[1]/_FACTOR * 1000, (double)array[2]/_FACTOR * 1000,
        		(double)array[3]/_FACTOR,        (double)array[4]/_FACTOR,        (double)array[5]/_FACTOR);
			_pick_id = array[8];		// PICKFRAME
			_pickref_id = (double)array[7]/_FACTOR;
			_status = array[13];
		} else {
			_pick_frame = Transformation.ofDeg(
				(double)array[0]/_FACTOR * 1000, (double)array[1]/_FACTOR * 1000, (double)array[2]/_FACTOR * 1000,
				(double)array[3]/_FACTOR,        (double)array[4]/_FACTOR,        (double)array[5]/_FACTOR);
			_obj_age = (double)array[7]/_FACTOR;
			_obj_type = array[8];
			pickit_object_dim[0] = (double)array[9]/_FACTOR;
			pickit_object_dim[1] = (double)array[10]/_FACTOR;
			pickit_object_dim[2] = (double)array[11]/_FACTOR;
			_obj_remaining = array[12];
			_status = array[13];
		}
		return true;
	}

	private boolean sendData() {
		ByteBuffer byteBuffer = ByteBuffer.wrap(_data_to_pickit);
		Frame pose = lbr.getCurrentCartesianPosition(lbr.getFlange());
		byteBuffer.putInt((int)(_FACTOR * pose.getX() / 1000));                // from mm to m    
		byteBuffer.putInt((int)(_FACTOR * pose.getY() / 1000));                // from mm to m    
		byteBuffer.putInt((int)(_FACTOR * pose.getZ() / 1000));                // from mm to m    
		byteBuffer.putInt((int)(_FACTOR * pose.getAlphaRad() * 180 / Math.PI));  //EulerZyx == yaw;  
		byteBuffer.putInt((int)(_FACTOR * pose.getBetaRad()  * 180 / Math.PI));  //EulerzYx == pitch;
		byteBuffer.putInt((int)(_FACTOR * pose.getGammaRad() * 180 / Math.PI));  //EulerzyX == roll; 
		byteBuffer.putInt((int)(_FACTOR * 0.0));
		byteBuffer.putInt(command);
		byteBuffer.putInt(_setup_id);
		byteBuffer.putInt(_product_id);
		byteBuffer.putInt(ROBOT_TYPE);
		byteBuffer.putInt(INTERFACE_VERSION);
		command = -1;

		try { to_pickit.write(_data_to_pickit); } 
		catch (IOException e) { e.printStackTrace(); return false; }
		return true;
	}
  

	private class PickitDataThread extends Thread {
		private volatile boolean running = true;
		
		public void terminate() { 
			padLog("Terminating PickIt Data Thread"); 
			running = false;
		}
		
		@Override public void run() {
			padLog("Running PickitDataThread");
			while(running) {
				try { receiveData(); Thread.sleep(10); }
				catch (InterruptedException e) { running = false; }
			}
			padLog("Stopped PickitDataThread");
		}
	}

	private class RobotDataThread extends Thread {
		private volatile boolean running = true;

		public void terminate() {
			padLog("Terminating Robot Data Thread");
			running = false;
		}

		@Override public void run() {
			padLog("Running Robot Data Thread");
			while(running) {
				try {
					if (!sendData()) {
						padErr("Failed to write");
						running = false;
					}
					Thread.sleep(10);
				} catch (InterruptedException e) { running = false; }
			}
			padLog("Stopped RobotDataThread");
		}
	}
	
	private class SyncDataThread extends Thread {
		private volatile boolean running = true;

		public void terminate() {
			padLog("Terminating SyncData Thread");
			running = false;
		}

		@Override public void run() {
			padLog("Running SyncData Thread");
			while(running) {
				try {
					receiveData();
					if (!sendData()) {
						padErr("Failed to write");
						running = false;
					}
					Thread.sleep(10);
				} catch (InterruptedException e) { running = false; }
			}
			padLog("Stopped RobotDataThread");
		}
	}
}
