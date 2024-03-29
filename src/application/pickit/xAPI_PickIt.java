package application.PickIt;

import static EluxUtils.Utils.*;
import static EluxUtils.UMath.*;

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

public class xAPI_PickIt{
	private LBR kiwa;
	private receiveDataThread receive_data_thread = new receiveDataThread();
	private sendDataThread send_data_thread = new sendDataThread();
	private int _timeout;

	/// Pickit data ///
	private int _setup_id = 0;
	private int _product_id = 0;
	private volatile int _obj_remaining = 0;
	private volatile int _obj_type = 0;
	private volatile int _pick_id = 0;
	private volatile Frame _pick_frame;
	private volatile double[] _pick_offset = {0,0,0,0,0,0};
	
	// (UNUSED)
	private volatile Transformation _pick_offset_tr;
	private volatile double _obj_age = 0;
	private volatile double[] _obj_size = {0, 0, 0};
	private volatile double _pickref_id = 0;
	
	// Ethernet socket communication
	private Socket _socket;
	private DataOutputStream to_pickit;
	private DataInputStream from_pickit;
	private byte[] _data_to_pickit = new byte[12 * 4];
	private byte[] _data_from_pickit = new byte[16 * 4];

	@Inject public xAPI_PickIt(LBR _kiwa) {		// Constructor
		this.kiwa = _kiwa;
	}
	
	// GETTERS ------------------------------------------
	
	public int getPickID() { return _pick_id; }
	public int getObjType() { return _obj_type; }
	public int getRemainingObj() { return _obj_remaining; }
	public Transformation getPickOffsetTr() { return _pick_offset_tr; }
	public double getObjAge() { return _obj_age; }
	public double[] getObjSize() { return _obj_size; }
	public double getPickRefID() { return _pickref_id; }
	public boolean isRunning() { return _status != _STOPPED && _status != _ERROR; }
	public boolean isReady() { return _status != _WAITING; }
	public Frame getPickFrame() { return _pick_frame; }
	public double getZrot() { return (deg(-_pick_offset[4] - 180)); }
	
	public int getBox() {
		if (!waitPickitAnswer()) return -1;
		if (_status == _OBJ_FOUND) {
			this.doSendPickFrameData();
			if (!waitPickitAnswer()) return -1;
			return getRemainingObj() + 1;
		} else if (_status == _OBJ_FOUND_NONE){
			logmsg("Pick-It was unable to find any reachable objects");
			waitMillis(1000);
			return -2;
		} else if (_status == _ROI_EMPTY){
			logmsg("Pick-It found the box empty");
			waitMillis(1000);
			return -3;
		} else return 0;
	}
	
	public boolean waitPickitAnswer() {
		int timeCounter = 0;
		if (!this.isReady()) {
			while(!this.isReady()) {
				waitMillis(10);
				timeCounter += 10;
				if (timeCounter >= _timeout) {
					logErr("Timeout is overdue, PickIt didn't answer");
					return false;
				}
			}
			// padLog("Answer delayed " + timeCounter + "ms");
		}
		return true;
	}
	
	// SETTERS ------------------------------------------

	public synchronized void doCalibration() {
		waitMillis(50);		// default = 500ms
		_status = _WAITING;
		_command = _CALIBRATE;
		waitMillis(6000);	// default = 6000ms
	}

	public synchronized void doScanForObj() {
		// padLog("Pickit scan for objects");
		_status = _WAITING;
		_command = _SCAN_FOR_OBJ;
	}

	public synchronized void doWaitForObj() {
		// padLog("Pickit wait for objects");
		_status = _WAITING;
		_command = _WAIT_FOR_OBJ;
	}

	public synchronized void doSendNextObj() {
		// padLog("Pickit calc next object to pick");
		_status = _WAITING;
		_command = _NEXT_OBJ;
	}
  
	public synchronized void doSendPickFrameData() {
	    // padLog("Pickit get pick frame data");
	    _status = _WAITING;
	    _command = _GET_PICKFRAME;
	}
  
	public synchronized boolean config(int setup_id, int product_id, int timeout) {
		_setup_id = setup_id;
		_product_id = product_id;
		_timeout = timeout;
		
		_command = _CONFIGURE;
		while (_status != _CONFIG_OK) {
			if (_status == _CONFIG_FAILED) {
				logErr("Pick-it did NOT configure correctly.");
				return false;
			}
			waitMillis(10);

		}
		logmsg("PickIt configured with Setup ID = " + setup_id + " and Product ID = " + product_id);
		return true;
	}
	
	public boolean init(String pickit_ip, int pickit_port) {
		_pick_frame = kiwa.getCurrentCartesianPosition(kiwa.getFlange()).copy();
		_obj_remaining = 0;
		_status = _WAITING;
		try {
			_socket = new Socket(pickit_ip, pickit_port);
			to_pickit = new DataOutputStream(_socket.getOutputStream());
			from_pickit = new DataInputStream(_socket.getInputStream());
			receive_data_thread.start();
			send_data_thread.start();
		} catch (Exception e) {
			logErr(e.toString());
			return false;
		}
		return true;
	}

	public void terminate() {
		try {
			receive_data_thread.terminate();
			send_data_thread.terminate();
			to_pickit.close();
			from_pickit.close();
			_socket.close();
		} catch (Exception e) {
			logErr("Exception during closing pickit comm");
		}
		_status = _STOPPED;
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
		if (data[14] != _ROBOT_TYPE) {
			logErr("Pick-it is not configured to communicate with KUKA LBR.");
			return false;
		}
		if (data[15] != _INTERFACE_VERSION) {
			logErr("The Pickit-it interface version does not match the version of this program.");
			return false;
		}
		_status = data[13];
		// this.printData(data);
		if (_status == _OBJ_FOUND) {
			_obj_age = (double)data[7]/_FACTOR;
			_obj_type = data[8];
			_obj_remaining = data[12];
			_pick_frame.setTransformationFromParent(Transformation.ofDeg(
	        		(double)data[0]/_FACTOR * 1000, (double)data[1]/_FACTOR * 1000, (double)data[2]/_FACTOR * 1000,
	        		(double)data[3]/_FACTOR,        (double)data[4]/_FACTOR,        (double)data[5]/_FACTOR));
			//padLog(rad2deg((double)data[4]/_FACTOR));
		} else if (_status == _OBJ_FOUND_NONE) {
		}
		else if (_status == _GET_PICKFRAME_OK) {
			for (int i = 0; i < 3; i++) _pick_offset[i] = (double)data[i]/_FACTOR * 1000;
			for (int i = 3; i < 6; i++) _pick_offset[i] = (double)data[i]/_FACTOR;
			_pick_offset_tr = Transformation.ofDeg(_pick_offset[0], 
												   _pick_offset[1],
												   _pick_offset[2],
												   _pick_offset[3],
												   _pick_offset[4],
												   _pick_offset[5]);
			_pickref_id = (double)data[7]/_FACTOR;
			_pick_id = data[8];
		} else if (_status == _GET_PICKFRAME_FAILED) {
			return false;
		}
		return true;
	}
	
	public void printData(int[] data){
		logmsg("Data: *****");
		logmsg("B00: " + (double)data[0]/_FACTOR * 1000);	// X
		logmsg("B01: " + (double)data[1]/_FACTOR * 1000);	// Y
		logmsg("B02: " + (double)data[2]/_FACTOR * 1000);	// Z
		logmsg("B03: " + (double)data[3]/_FACTOR);			// A
		logmsg("B04: " + (double)data[4]/_FACTOR);			// B
		logmsg("B05: " + (double)data[5]/_FACTOR);			// C
		logmsg("B06: " + data[6]);							// Nothing, must be 0
		logmsg("B07: " + data[7]);							// Object age (ms) / PickrefID
		logmsg("B08: " + data[8]);							// Object type (if status = 20) / PickID (if status = 70)
		logmsg("B09: " + data[9]);							// Object dimension (X)
		logmsg("B10: " + data[10]);							// Object dimension (Y)
		logmsg("B11: " + data[11]);							// Object dimension (Z)
		logmsg("B12: " + data[12]);							// Remaining objects (-1)
		logmsg("B13: " + data[13]);							// Status = 20 if object found
		logmsg("B14: " + data[14]);							// Robot type = 5 if KUKA LBR iiwa
		logmsg("B15: " + data[15]);							// Version number = 11
		logmsg("End of data. *****");
	}

	private boolean sendData() {
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
		byteBuffer.putInt(_ROBOT_TYPE);
		byteBuffer.putInt(_INTERFACE_VERSION);
		_command = -1;
		try { to_pickit.write(_data_to_pickit); } 
		catch (IOException e) { return false; }
		return true;
	}
	
	private class receiveDataThread extends Thread {
		private volatile boolean running = true;
		public void terminate() {
			logmsg("Terminating receiveDataThread");
			running = false;
		}
		@Override public void run() {
			while(running) {
				try { receiveData(); Thread.sleep(10); } 
				catch (InterruptedException e) {
					logmsg("Interrupted receiveDataThread");
					running = false;
				}
			}
		}
	}

	private class sendDataThread extends Thread {
		private volatile boolean running = true;
		public void terminate() {
			logmsg("Terminating sendDataThread");
	    	running = false;
		}
		@Override public void run() {
			while(running) {
				try {
					if (!sendData()) {
						logErr("Failed to write, stopping sendDataThread");
						running = false;
					}
					Thread.sleep(10);
				} catch (InterruptedException e) { 
					logmsg("Interrupted sendDataThread");
					running = false; 
				}
			}
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
	private static final int _ROI_EMPTY = 23;
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
	private static final int _ROBOT_TYPE = 5;
	private static final int _INTERFACE_VERSION = 11;
}
