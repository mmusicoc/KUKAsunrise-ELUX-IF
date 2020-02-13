package application.Pickit;

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

public class PickIt {
  private static final int MULT = 10000;
  private static final int PICKIT_ROBOT_TYPE = 5;
  private static final int PICKIT_IF_VERSION = 11;

  ///////////////////
  /// Pickit data ///
  private volatile Transformation pickit_object_pose;
  public volatile double pickit_object_age = 0;
  public volatile int pickit_object_type = 0;
  public volatile double[] pickit_object_dim = {0, 0, 0};
  public volatile int pickit_remaining_obj = 0;
  public volatile int pickit_status = 0;
  private volatile Transformation pickit_offset_pose;
  public volatile int pickit_pick_id = 0;
  public volatile double pickit_pickref_id = 0;
  private Socket socket;
  private DataOutputStream to_pickit;
  private DataInputStream from_pickit;
  private byte[] to_pickit_data = new byte[12 * 4];
  private byte[] from_pickit_data = new byte[16 * 4];

  /// Pickit object types ///
  public static final int PICKIT_TYPE_SURFACE = 31;
  public static final int PICKIT_TYPE_CYLINDER = 32;
  public static final int PICKIT_TYPE_BALL = 33;
  public static final int PICKIT_TYPE_BOX = 34;
  public static final int PICKIT_TYPE_CONE = 35;

  /// Pickit status constants ///
  public static final int PICKIT_WAITING = 0;
  public static final int PICKIT_OBJECT_FOUND = 20;
  public static final int PICKIT_NO_OBJECTS = 21;
  public static final int PICKIT_ERROR = 30;
  public static final int PICKIT_STOPPED = 31;
  public static final int PICKIT_CONFIG_OK = 40;
  public static final int PICKIT_CONFIG_FAILED = 41;
  public static final int PICKIT_GETPICKFRAMEDATA_OK = 70;
  public static final int PICKIT_GETPICKFRAMEDATA_FAILED = 71;

  /// Robot Commands constants ///
  public static final int RC_PICKIT_CALIBRATE = 10;
  public static final int RC_PICKIT_LOOK_FOR_OBJ = 20;
  public static final int RC_PICKIT_WAIT_FOR_OBJ = 21;
  public static final int RC_PICKIT_NEXT_OBJ = 30;
  public static final int RC_PICKIT_CONFIGURE = 40;
  public static final int RC_PICKIT_GETPICKFRAMEDATA = 70;
  
  /// Robot command field ///
  private int pickit_r_command = -1;

  /// Configuration fields ///
  private int pickit_r_setup = 3;
  private int pickit_r_product = 2;

  private PickitDataThread pickit_data_thread = new PickitDataThread();
  private RobotDataThread robot_data_thread = new RobotDataThread();

  private LBR lbr;
  /// Public functions ///

  public PickIt(LBR lbr) {
    this.lbr = lbr;
  }

  public boolean pickit_socket_open(String pickit_ip, int pickit_port) {
    System.out.println("Opening Pickit socket new");
    pickit_remaining_obj = 0;
    pickit_status = PICKIT_WAITING;
    try {
      socket = new Socket(pickit_ip, pickit_port);
      to_pickit = new DataOutputStream(socket.getOutputStream());
      from_pickit = new DataInputStream(socket.getInputStream());
      System.out.println("Starting Pickit threads");
      pickit_data_thread.start();
      robot_data_thread.start();
    } catch (Exception e) {
      System.err.println(e.toString());
      return false;
    }
    return true;
  }

  public synchronized void pickit_do_calibration() {
    System.out.println("Pickit do calibration");
    pickit_status = PICKIT_WAITING;
    pickit_r_command = RC_PICKIT_CALIBRATE;
  }

  public synchronized void pickit_look_for_object() {
    System.out.println("Pickit look for object");
    pickit_status = PICKIT_WAITING;
    pickit_r_command = RC_PICKIT_LOOK_FOR_OBJ;
  }

  public synchronized void pickit_wait_for_object() {
    System.out.println("Pickit wait for object");
    pickit_status = PICKIT_WAITING;
    pickit_r_command = RC_PICKIT_WAIT_FOR_OBJ;
  }

  public synchronized void pickit_next_object() {
    System.out.println("Pickit next object");
    pickit_status = PICKIT_WAITING;
    pickit_r_command = RC_PICKIT_NEXT_OBJ;
  }
  
  public synchronized void pickit_get_pick_frame_data() {
	    System.out.println("Pickit get pick frame data");
	    pickit_status = PICKIT_WAITING;
	    pickit_r_command = RC_PICKIT_GETPICKFRAMEDATA;
	  }
 
  public int pickit_pick_id() {
	    return pickit_pick_id;
  }
  
  public int pickit_object_type() {
	    return pickit_object_type;
}

  public int pickit_remaining_objects() {
    return pickit_remaining_obj;
  }

  public boolean pickit_is_running() {
    return pickit_status != PICKIT_STOPPED && pickit_status != PICKIT_ERROR;
  }

  public boolean pickit_has_response() {
    return pickit_status != PICKIT_WAITING;
  }

  public boolean pickit_object_found() {
    return pickit_status == PICKIT_OBJECT_FOUND;
  }

  public Transformation pickit_get_pose() {
    return pickit_object_pose;
  }

  public synchronized boolean pickit_configure(int setup, int product) throws InterruptedException {
    pickit_r_setup = setup;
    pickit_r_product = product;
    pickit_r_command = RC_PICKIT_CONFIGURE;
    while (pickit_status != PICKIT_CONFIG_OK) {
      if (pickit_status == PICKIT_CONFIG_FAILED) {
        System.out.println("Pick-it did NOT configure correctly.");
        return false;
      }
      Thread.sleep(10);
    }
    return true;
  }

  public void pickit_socket_close() throws IOException {
    System.out.println("Stopping PickIt threads");
    pickit_data_thread.terminate();
    robot_data_thread.terminate();
    try {
      pickit_data_thread.join(200);
      robot_data_thread.join(200);
    } catch (InterruptedException e) {
    	System.err.println("Failed to stop pickit threads");
    }
    System.out.println("Closing PickIt socket");
    try {
      to_pickit.close();
      from_pickit.close();
      socket.close();
    } catch (IOException e) {
      //e.printStackTrace();
    	System.err.println("IOException during closing pickit comm");
    }
  }


  //////// Internal functions. Do not call directly. /////////
  private boolean recv_pickit_data() {
    try {
      from_pickit.readFully(from_pickit_data);
      System.out.println("Received pickit data");
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    IntBuffer intBuf = ByteBuffer.wrap(from_pickit_data)
        .order(ByteOrder.BIG_ENDIAN)
        .asIntBuffer();
    int[] array = new int[intBuf.remaining()];
    intBuf.get(array);
    if (array[14] != PICKIT_ROBOT_TYPE) {
      System.err.println("Pick-it is not configured to communicate with the JAVA API.");
      return false;
    }
    if (array[15] != PICKIT_IF_VERSION) {
      System.err.println("The Pickit-it interface version does not match the version of this program.");
    }
    if(array[13] == PICKIT_GETPICKFRAMEDATA_OK){
    	pickit_offset_pose = Transformation.ofDeg(
        		(double)array[0]/MULT * 1000, (double)array[1]/MULT * 1000, (double)array[2]/MULT * 1000,
        		(double)array[3]/MULT,        (double)array[4]/MULT,        (double)array[5]/MULT);
    	pickit_pick_id = array[8];		// PICKFRAME
    	pickit_pickref_id = (double)array[7]/MULT;
    	pickit_status = array[13];
    } else {
    pickit_object_pose = Transformation.ofDeg(
    		(double)array[0]/MULT * 1000, (double)array[1]/MULT * 1000, (double)array[2]/MULT * 1000,
    		(double)array[3]/MULT,        (double)array[4]/MULT,        (double)array[5]/MULT);
    pickit_object_age = (double)array[7]/MULT;
    pickit_object_type = array[8];
    pickit_object_dim[0] = (double)array[9]/MULT;
    pickit_object_dim[1] = (double)array[10]/MULT;
    pickit_object_dim[2] = (double)array[11]/MULT;
    pickit_remaining_obj = array[12];
    pickit_status = array[13];
    }
    return true;
  }

  private boolean send_robot_data() {
    ByteBuffer byteBuffer = ByteBuffer.wrap(to_pickit_data);
    Frame pose =lbr.getCurrentCartesianPosition(lbr.getFlange());
    byteBuffer.putInt((int)(MULT * pose.getX() / 1000));                // from mm to m    
    byteBuffer.putInt((int)(MULT * pose.getY() / 1000));                // from mm to m    
    byteBuffer.putInt((int)(MULT * pose.getZ() / 1000));                // from mm to m    
    byteBuffer.putInt((int)(MULT * pose.getAlphaRad() * 180 / Math.PI));  //EulerZyx == yaw;  
    byteBuffer.putInt((int)(MULT * pose.getBetaRad()  * 180 / Math.PI));   //EulerzYx == pitch;
    byteBuffer.putInt((int)(MULT * pose.getGammaRad() * 180 / Math.PI));  //EulerzyX == roll; 
    byteBuffer.putInt((int)(MULT * 0.0));
    byteBuffer.putInt(pickit_r_command);
    byteBuffer.putInt(pickit_r_setup);
    byteBuffer.putInt(pickit_r_product);
    byteBuffer.putInt(PICKIT_ROBOT_TYPE);
    byteBuffer.putInt(PICKIT_IF_VERSION);
    pickit_r_command = -1;

    try {
      to_pickit.write(to_pickit_data);
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  private class PickitDataThread extends Thread {
    private volatile boolean running = true;

    public void terminate() {
      System.out.println("Terminating PickIt Data Thread");
      running = false;
    }
    @Override
    public void run() {
      System.out.println("Running PickitDataThread");
      while(running) {
        try {
          recv_pickit_data();
          Thread.sleep(10);
        } catch (InterruptedException e) {
          System.out.println("Interrupted Pickit receiving thread");
          running = false;
        }
      }
      System.out.println("Stopped PickitDataThread");
    }
  }

private class RobotDataThread extends Thread {
    private volatile boolean running = true;

    public void terminate() {
      System.out.println("Terminating RobotDataThread");
      running = false;
    }

    @Override
    public void run() {
      System.out.println("Running RobotDataThread");
      while(running) {
        try {
          if (!send_robot_data()) {
        	  System.err.println("Failed to write stopping RobotDataThread");
        	  running = false;
          }
          
          Thread.sleep(10);
        } catch (InterruptedException e) {
          System.out.println("Interrupted Robot sending thread");
          running = false;
        }
      }
      System.out.println("Stopped RobotDataThread");
    }
  }
}
