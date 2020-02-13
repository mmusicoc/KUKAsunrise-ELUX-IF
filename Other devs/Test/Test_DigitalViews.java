package application;

import javax.inject.Inject;
import com.kuka.common.params.IParameterSet;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.deviceModel.CartesianCoordinates;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.deviceModel.LBRE1Redundancy;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.redundancy.IRedundancyCollection;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;
import com.sun.org.apache.bcel.internal.generic.NEW;
import com.sun.org.apache.bcel.internal.generic.NEWARRAY;
import java.net.*; 
import java.util.Arrays;
import java.io.*; 

public class Test_DigitalViews extends RoboticsAPIApplication {
	@Inject
	private LBR kiwa;
	@Inject private Plc_inputIOGroup 		plcin;
	@Inject private Plc_outputIOGroup 		plcout;
	@Inject private MediaFlangeIOGroup 		mfio;
	private int port = 30000;
	private int answer = 0;
	
	@Override
	public void initialize() { }

	@Override
	public void run() {
		try {
			echo();
		} catch (Exception e) {
			System.out.println(e.getMessage()); 
		} 
	}

	public void echo() throws IOException { 
		ServerSocket serverSocket = null; 
		while(true) {
			try { 
				serverSocket = new ServerSocket(port); 
				System.out.println("Listening to port: " + port);
				break;
			} catch (IOException e) { 
				System.err.println("Could not listen to port: " + port); 
				answer = getApplicationUI().displayModalDialog(
							ApplicationDialogType.QUESTION, "Iterate port number by 1?", "No" , "Yes");
				port = port + answer;
			} 
		}
		Socket clientSocket = null; 
		System.out.println ("Waiting for connection.....");

		try {
			clientSocket = serverSocket.accept(); 
		} catch (IOException e) { 
			System.err.println("Accept failed."); 
		} 

		System.out.println ("Connection successful");
		System.out.println(	"ServerSocket : InetAddress = " + serverSocket.getInetAddress() +
							", Localport = " + serverSocket.getLocalPort());
		System.out.println(	"Client Socket : InetAddress = " + clientSocket.getInetAddress() + 
							", Localport = " + clientSocket.getLocalPort()); 

		PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true); 
		BufferedReader in = new BufferedReader(new InputStreamReader( clientSocket.getInputStream())); 

		out.println("Connection Successful."); 
		System.out.println("Listening...");
		out.println("Listening...");
		String inputLine;
		
		double speed = 0.15;

		while((inputLine = in.readLine()) != null) { 
			System.out.println ("Client says: " + inputLine); 
			String[] tokens = inputLine.split("\\s+");
			
			if(tokens[0].equals("STOP")) {
				out.println("Terminating connection."); 
				System.out.println("Terminating connection."); 
				break; 
			} else if(tokens[0].equals("SPEED")) {
				try {
					speed = Double.parseDouble(tokens[1]);
					if(speed>1) speed=1;
					if(speed<0.15) speed=0.15;
					out.println("Setting speed to "+speed+".");
				} catch (NumberFormatException nfe) {
					out.println("Speed format error.");
				}
			} else if(tokens[0].equals("GOTO")) {
				String position = tokens[1];
				System.out.println("Going to "+position+".");
				kiwa.move(ptp(getApplicationData().getFrame("/DigitalViews/"+position)).setJointVelocityRel(speed)); 
				Frame currentPos = kiwa.getCurrentCartesianPosition(kiwa.getFlange());
				System.out.println("Current Position: " + currentPos.toString());
			} else {
				System.out.println("Invalid Command!"); 
				out.println("Invalid Command!"); 
			} 
			
			System.out.println("Listening..."); 
			out.println("Listening..."); 
		} 
		out.close(); 
		in.close(); 
		clientSocket.close(); 
		serverSocket.close(); 
	}
}