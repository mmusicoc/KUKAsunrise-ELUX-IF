package digital_views;

import static java.lang.System.*;
import java.io.*;
import java.net.*;
import com.fazecast.jSerialComm.*;

public class DigitalViews {

	
private static void sequence(BufferedReader in, PrintWriter out) throws IOException {
	while(true) {
		in.readLine();
		out.println("SPEED 0.5");
		in.readLine();
		out.println("GOTO P1");
		in.readLine();
		out.println("GOTO P2");
		in.readLine();
		out.println("GOTO P1");
		in.readLine();
		out.println("GOTO P3");
		in.readLine();
		out.println("GOTO P1");
		in.readLine();
		out.println("GOTO P4");
		in.readLine();
		out.println("GOTO P1");
		in.readLine();
		out.println("GOTO P5");
		}
}
	
public static void main(String[] args) throws IOException {
/*	
		SerialPort port = SerialPort.getCommPort("COM3");
		boolean result = port.openPort();
		System.out.println(result?"COM3 Opened":"Serial port problem");
		byte[] data = ("R0\n").getBytes();
		port.writeBytes(data,data.length);
		System.exit(0);
	*/	
	
        System.out.println(args[0] +"		" + args[1] );
        if (args.length != 2) {
            System.err.println(
                "Usage: java EchoClient <host name> <port number>");
            System.exit(1);
        }

        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);

        try (
            Socket echoSocket = new Socket(hostName, portNumber);
            PrintWriter out =
                new PrintWriter(echoSocket.getOutputStream(), true);
            BufferedReader in =
                new BufferedReader(
                    new InputStreamReader(echoSocket.getInputStream()));
            BufferedReader stdIn =
                new BufferedReader(
                    new InputStreamReader(System.in))
        ) { 
            String userInput;
            String serverInput;
            
            sequence(in,out);
            
            while ((serverInput = in.readLine()) != null) {
                System.out.println("Server: " + serverInput);
                if (serverInput.equals("Listening...")) {
                System.out.print("Command : " );
                	while ((userInput = stdIn.readLine()) != null) { 
                        out.println(userInput);   
                        break;
                	}	
				}
            }
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                hostName);
            System.exit(1);
        } 
    }
}
