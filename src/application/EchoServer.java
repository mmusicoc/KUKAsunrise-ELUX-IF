package application;


import javax.inject.Inject;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.*;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;

import java.net.*; 
import java.io.*; 
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
public class EchoServer extends RoboticsAPIApplication {
	@Inject
	private LBR kiwa;
	
	int port = 0;
	@Override
	public void initialize() {
		// initialize your application here
		port = 30000;
	}

	@Override
	public void run() {
		// your application execution starts here
		 
		try {
			echo(port);
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println(e.getMessage());
			port = port + getApplicationUI().displayModalDialog( ApplicationDialogType.QUESTION, "Iterate port number by 1? ", "No" , "Yes");
		} 
	}


    public void echo(int port) throws IOException 
    { 
        ServerSocket serverSocket = null; 

        try { 
             serverSocket = new ServerSocket(port); 
             System.out.println("Listening to port: " + port);
            } 
        catch (IOException e) 
            { 
             System.err.println("Could not listen to port: " + port); 
             System.exit(1); 
            } 

        Socket clientSocket = null; 
        System.out.println ("Waiting for connection.....");

        try { 
             clientSocket = serverSocket.accept(); 
            } 
        catch (IOException e) 
            { 
             System.err.println("Accept failed."); 
             System.exit(1); 
            } 

        System.out.println ("Connection successful");
        System.out.println("ServerSocker : InetAddress = " + serverSocket.getInetAddress() + ", Localport = " + serverSocket.getLocalPort());
        System.out.println("Client Socker : InetAddress = " + clientSocket.getInetAddress() + ", Localport = " + clientSocket.getLocalPort());
        System.out.println ("Waiting for input.....");

        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), 
                                          true); 
        BufferedReader in = new BufferedReader( 
                new InputStreamReader( clientSocket.getInputStream())); 

        String inputLine; 

        while ((inputLine = in.readLine()) != null) 
            { 
             System.out.println ("Server: " + inputLine); 
             out.println(inputLine); 

             if (inputLine.equals("Bye.")) 
                 break; 
            } 

        out.close(); 
        in.close(); 
        clientSocket.close(); 
        serverSocket.close(); 
       } 

}