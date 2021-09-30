package EluxAPI;

import static EluxAPI.Utils.*;

//import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import javax.inject.Inject;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.math.Transformation;

public class API_Cambrian {
    private Client_Socket socket;
    private String data_received;
    private String CI_MSG;
    private int CI_MSG_TYPE;
    private String CI_REPLY;
    
    private Frame target_frame;
    private Frame approach_frame;
    private int pick_type;
    private int approach_dist;
    private int depth_offset;

    ////////////////////////////////////////////////////////////////////////////////////

	

    ////////////////////////////////////////////////////////////////////////////////////

    public void init(String IP, int port) { this.socket = new Client_Socket(IP, port); }
    public void ping(){ sendRequest("PING", ""); }
    public void setApproachDist(int approach_dist) { this.approach_dist = approach_dist; }
    public void setDepthOffset(int depth_offset) { this.depth_offset = depth_offset; }
    public void startCalibration(){ sendRequest("START CALIBRATION", ""); }
    public void captureCalibration(){ sendRequest("CAPTURE CALIBRATION IMAGE", ""); }
    public void loadModel(String model_name){ sendRequest("LOAD MODEL", model_name); }
    
    public boolean getNewPrediction(String model_name) {
    	return getPrediction("GET PREDICTION", model_name); }
    
    public boolean getNextPrediction(String model_name) {
    	return getPrediction("GET NEXT PREDICTION", ""); }
    
    public Frame getTargetFrame() { return target_frame; }
	public Frame getApproachFrame() { return approach_frame; }
	public int getPickType() { return pick_type; }

    ////////////////////////////////////////////////////////////////////////////////////

    private LBR kiwa;
	@Inject public API_Cambrian(LBR _kiwa) { this.kiwa = _kiwa; }

    ////////////////////////////////////////////////////////////////////////////////////

	private boolean getPrediction(String prediction_type, String model_name) {
        if(sendRequest(prediction_type, model_name)) {
            try {
                String[] results = CI_REPLY.split(",");
                if(Integer.parseInt(results[0]) == 1) {
                    Double x, y, z, a, b, c;
                    int type;
    
                    x    = Double.parseDouble(results[1]);
                    y    = Double.parseDouble(results[2]);
                    z    = Double.parseDouble(results[3]) + depth_offset;
                    a    = -Double.parseDouble(results[4]) + 90;
                    b    = Double.parseDouble(results[5]);
                    c    = Double.parseDouble(results[6]);
                    type = Integer.parseInt(results[7]);
    
                    target_frame = new Frame(x, y, z, a, b, c);
                    approach_frame = new Frame(x, y, z, a, b, c);
                    Frame current_flange_pos = kiwa.getCurrentCartesianPosition(kiwa.getFlange()).copy();
                    target_frame.setParent(current_flange_pos, false);
                    target_frame.setTransformationFromParent(Transformation.ofDeg(x, y, z, a, b, c));
    				approach_frame.setParent(target_frame, false);
    				approach_frame.setTransformationFromParent(Transformation.ofDeg
    						(0, 0, -this.approach_dist, 0, 0, 0));
                    
                    //padLog(pick_frame_tcp.toStringInWorld());
    				//padLog(pick_frame_tcp.toStringTrafo());
                    pick_type = type;
                    return true;
                }                
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Failed to parse prediction result");
            }
        }
        return false;
    }
	
	private boolean sendRequest(String command, String data) {
        Frame pose = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
        String robot_pose = "p[";
        robot_pose += Double.toString(pose.getX()) + ",";
        robot_pose += Double.toString(pose.getY()) + ",";
        robot_pose += Double.toString(pose.getZ()) + ",";
        robot_pose += Double.toString(rad2deg(pose.getAlphaRad())) + ",";
        robot_pose += Double.toString(rad2deg(pose.getBetaRad())) + ",";
        robot_pose += Double.toString(rad2deg(pose.getGammaRad()));
        robot_pose += "]";

        String request = command + "#" + data + "#" + robot_pose + "#p[0,0,0,0,0,0]#p[0,0,0,0,0,0]#<<"; 

        socket.Send(request);
        data_received = socket.Read();

        if(data_received != "") {
            try {
                String aux = data_received;
                CI_MSG = aux.substring(aux.indexOf("<<") + 2, aux.indexOf(">>"));
                aux = aux.substring(aux.indexOf(">>") + 2);
                CI_MSG_TYPE = Integer.parseInt(aux.substring(aux.indexOf("<<") + 2, aux.indexOf(">>")));
                aux = aux.substring(aux.indexOf(">>") + 2);        
                CI_REPLY = aux.substring(aux.indexOf("<<") + 2, aux.indexOf(">>"));
                
                //System.out.println("CI_MSG: " + CI_MSG);
                //System.out.println("CI_MSG_TYPE: " + CI_MSG_TYPE);
                //System.out.println("CI_REPLY: " + CI_REPLY);                

                return true;
            } catch (Exception e) {
                System.out.println("Failed to parse received data");
                return false;
            }
        } else {
            System.out.println("Received no data");
            return false;
        }
    }
}

////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////

class Client_Socket {
    private String TCP_IP;
	private int TCP_port;
    private Socket sc;

    public Client_Socket(String IP, int port) {
        this.TCP_IP = IP;
        this.TCP_port = port;
    }

	public void Send(String command) {
		//System.out.println(command);
		try {
			// Create a new Socket Client
			sc = new Socket(TCP_IP, TCP_port);
			if (sc.isConnected()){
				// Create stream for data
				DataOutputStream out;
				out = new DataOutputStream(sc.getOutputStream());
				
				// Send command
				out.write(command.getBytes("US-ASCII"));
				out.flush();
			} else {
                System.out.println("Failed to open socket");
                sc.close();
            }
		} catch (IOException e) { System.out.println(e); }
	}

    public String Read() {
        try {
            InputStream input = sc.getInputStream();
            InputStreamReader reader = new InputStreamReader(input);
            int character;
            StringBuilder data = new StringBuilder();
            while ((character = reader.read()) != -1) {
                data.append((char) character);
            }
            //System.out.println(data);
            //System.out.println(data.toString());
            return data.toString();
        } catch (IOException e) { System.out.println(e); }
        
        return "";
    }
}