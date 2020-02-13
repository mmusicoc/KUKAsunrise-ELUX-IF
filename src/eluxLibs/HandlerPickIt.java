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

public class HandlerPickIt{
	private LBR kiwa;
	//private HandlerMov move;
	private receiveDataThread receive_data_thread = new receiveDataThread();
	private sendDataThread send_data_thread = new sendDataThread();
	private int _timeout;

	/// Pickit data ///
	private int _setup_id = 0;
	private int _product_id = 0;
	private volatile Frame pick_frame;
	private volatile Transformation _pick_offset;
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

	@Inject public HandlerPickIt(LBR _kiwa, HandlerMov _move) {		// Constructor
		this.kiwa = _kiwa;
		//this.move = _move;
	}
	
	// GETTERS ------------------------------------------
	
	public int getPickID() { return _pick_id; }
	public int getObjType() { return _obj_type; }
	public int getRemainingObj() { return _obj_remaining; }
	public boolean isRunning() { return _status != _STOPPED && _status != _ERROR; }
	public boolean isReady() { return _status != _WAITING; }
	public boolean hasFoundObj() { return _status == _OBJ_FOUND; }
	public Frame getPickFrame() { return pick_frame; }
	
	public int getBox() {
		int timeCounter = 0;
		int remaining = 0;
		while(!this.isReady()) {
			waitMillis(10);
			timeCounter += 10;
			if (timeCounter >= _timeout) {
				padErr("Timeout is overdue, PickIt didn't answer");
				return -1;
			}
		}
		padLog("Answer took " + timeCounter + "ms");
		if (this.hasFoundObj()) {
			this.doSendPickFrameData();
			remaining = getRemainingObj() + 1;
			padLog("Found " + remaining + " objects");
			return remaining;
		}
		else {
			padLog("Pickit was unable to find any reachable objects");
			return 0;
		}
		
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
		padLog("Pickit wait for objects");
		_status = _WAITING;
		_command = _WAIT_FOR_OBJ;
	}

	public synchronized void doSendNextObj() {
		padLog("Pickit calc next object to pick");
		_status = _WAITING;
		_command = _NEXT_OBJ;
	}
  
	public synchronized void doSendPickFrameData() {
	    padLog("Pickit get pick frame data");
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
				padErr("Pick-it did NOT configure correctly.");
				return false;
			}
			waitMillis(10);

		}
		padLog("PickIt configured with Setup ID = " + setup_id + " and Product ID = " + product_id);
		return true;
	}
	
	public boolean init(String pickit_ip, int pickit_port) {
		pick_frame = kiwa.getCurrentCartesianPosition(kiwa.getFlange()).copy();
		_obj_remaining = 0;
		_status = _WAITING;
		try {
			_socket = new Socket(pickit_ip, pickit_port);
			to_pickit = new DataOutputStream(_socket.getOutputStream());
			from_pickit = new DataInputStream(_socket.getInputStream());
			receive_data_thread.start();
			send_data_thread.start();
		} catch (Exception e) {
			padErr(e.toString());
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
			padErr("Exception during closing pickit comm");
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
			padErr("Pick-it is not configured to communicate with KUKA LBR.");
			return false;
		}
		if (data[15] != _INTERFACE_VERSION) {
			padErr("The Pickit-it interface version does not match the version of this program.");
			return false;
		}
		_status = data[13];
		// this.printData(data);
		if (_status == _OBJ_FOUND) {
			_obj_age = (double)data[7]/_FACTOR;
			_obj_type = data[8];
			_obj_remaining = data[12];
			pick_frame.setTransformationFromParent(Transformation.ofDeg(
	        		(double)data[0]/_FACTOR * 1000, (double)data[1]/_FACTOR * 1000, (double)data[2]/_FACTOR * 1000,
	        		(double)data[3]/_FACTOR,        (double)data[4]/_FACTOR,        (double)data[5]/_FACTOR));
		} else if (_status == _GET_PICKFRAME_OK) {
			_pick_offset = Transformation.ofDeg(
	        		(double)data[0]/_FACTOR * 1000, (double)data[1]/_FACTOR * 1000, (double)data[2]/_FACTOR * 1000,
	        		(double)data[3]/_FACTOR,        (double)data[4]/_FACTOR,        (double)data[5]/_FACTOR);
			_pickref_id = (double)data[7]/_FACTOR;
			_pick_id = data[8];
			return false; 
		}
		return true;
	}
	
	private void printData(int[] data){
		padLog("Data: *****");
		padLog("B00: " + (double)data[0]/_FACTOR * 1000);	// X
		padLog("B01: " + (double)data[1]/_FACTOR * 1000);	// Y
		padLog("B02: " + (double)data[2]/_FACTOR * 1000);	// Z
		padLog("B03: " + (double)data[3]/_FACTOR);			// A
		padLog("B04: " + (double)data[4]/_FACTOR);			// B
		padLog("B05: " + (double)data[5]/_FACTOR);			// C
		padLog("B06: " + data[6]);							// Nothing, must be 0
		padLog("B07: " + data[7]);							// Object age (ms) / PickrefID
		padLog("B08: " + data[8]);							// Object type (if status = 20) / PickID (if status = 70)
		padLog("B09: " + data[9]);							// Object dimension (X)
		padLog("B10: " + data[10]);							// Object dimension (Y)
		padLog("B11: " + data[11]);							// Object dimension (Z)
		padLog("B12: " + data[12]);							// Remaining objects (-1)
		padLog("B13: " + data[13]);							// Status = 20 if object found
		padLog("B14: " + data[14]);							// Robot type = 5 if KUKA LBR iiwa
		padLog("B15: " + data[15]);							// Version number = 11
		padLog("End of data. *****");
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
			padLog("Terminating receiveDataThread");
			running = false;
		}
		@Override public void run() {
			while(running) {
				try { receiveData(); Thread.sleep(10); } 
				catch (InterruptedException e) {
					padLog("Interrupted receiveDataThread");
					running = false;
				}
			}
		}
	}

	private class sendDataThread extends Thread {
		private volatile boolean running = true;
		public void terminate() {
			padLog("Terminating sendDataThread");
	    	running = false;
		}
		@Override public void run() {
			while(running) {
				try {
					if (!sendData()) {
						padErr("Failed to write, stopping sendDataThread");
						running = false;
					}
					Thread.sleep(10);
				} catch (InterruptedException e) { 
					padLog("Interrupted sendDataThread");
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
