package ExchangeData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import ExchangeData.*;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.task.ITaskFunctionMonitor;
import com.kuka.task.TaskFunctionMonitor;
//import com.kuka.roboticsAPI.applicationModel.tasks.ITaskFunction;
//import com.kuka.roboticsAPI.applicationModel.tasks.ITaskFunctionAccessor;
//import com.kuka.roboticsAPI.applicationModel.tasks.ITaskFunctionProvider;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.controllerModel.sunrise.api.Port;
import com.kuka.roboticsAPI.deviceModel.Device;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.deviceModel.LBRAlphaRedundancy;
import com.kuka.roboticsAPI.deviceModel.LBRE1Redundancy;
import com.kuka.roboticsAPI.deviceModel.LBRE1Redundancy.E1Parameter;
import com.kuka.roboticsAPI.deviceModel.StatusTurnRedundancy;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.geometricModel.World;
import com.kuka.roboticsAPI.geometricModel.math.Point;
import com.kuka.roboticsAPI.geometricModel.redundancy.IRedundancyCollection;
import com.sun.jmx.snmp.tasks.TaskServer;
import com.sun.jndi.url.ldaps.ldapsURLContextFactory;
import com.sun.servicetag.Installer;
import com.kuka.task.properties.TaskFunctionProvider;
import com.kuka.generated.ioAccess.LineaCassioliIOGroup;



/**
 * Implementation of a cyclic background task.
 * <p>
 * It provides the {@link RoboticsAPICyclicBackgroundTask#runCyclic} method
 * which will be called cyclically with the specified period.<br>
 * Cycle period and initial delay can be set by calling
 * {@link RoboticsAPICyclicBackgroundTask#initializeCyclic} method in the
 * {@link RoboticsAPIBackgroundTask#initialize()} method of the inheriting
 * class.<br>
 * The cyclic background task can be terminated via
 * {@link RoboticsAPICyclicBackgroundTask#getCyclicFuture()#cancel()} method or
 * stopping of the task.
 */
public class KrcServer extends RoboticsAPICyclicBackgroundTask 
{
	private ITaskFunctionMonitor appinfo;
	private Controller kuka_Sunrise_Cabinet_1;
	private LBR lbr_iiwa_14_R820_1;

	public static double VarTest = 0;
	public static boolean Caricato = false;
	public static int TestInt = 0;

	public static ObjectFrame Test;
	public static Frame oo;
	public static Point pp;
	public static JointPosition jpos;

	public ServerSocket listener;
	public Socket socket;
	private String header = "&*Header*&";

	private Byte[] bytes = new Byte[1024];
	private Byte[] queueMsg = new Byte[2048];

	private static KrcServer app = null;
	private IDataExchange dataExchanger;
	private int portNumber;
	private ServerSocket serverSocket;
	
	private LineaCassioliIOGroup IOLinea;

	public KrcServer()
	{
		this(30010);		
	}

	public KrcServer(int portNumber)
	{
		this.portNumber = portNumber;	
	}

	public void initialize()
	{		
		kuka_Sunrise_Cabinet_1 = getController("KUKA_Sunrise_Cabinet_1");
		lbr_iiwa_14_R820_1 = (LBR) getDevice(kuka_Sunrise_Cabinet_1,
				"LBR_iiwa_14_R820_1");	
		
		initializeCyclic(0, 500, TimeUnit.MILLISECONDS,
				CycleBehavior.BestEffort);
		
		IOLinea = new LineaCassioliIOGroup(kuka_Sunrise_Cabinet_1);
				
		dataExchanger = getTaskFunction(IDataExchange.class);
		appinfo = TaskFunctionMonitor.create(dataExchanger);
		Locale.setDefault(Locale.US);
				
		if (getApplicationData().getProcessData("ServerFault") != null)
		{			
			getApplicationData().getProcessData("ServerFault").setValue(false);
		}

		serverSocket = null;
		try
		{
			getApplicationData().getProcessData("ServerState").setValue(2);
			serverSocket = new ServerSocket(portNumber);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}

	public void runCyclic() {
		// System.out.println("BACKGROUND TASK");
		try {
			if (dataExchanger.isAvailable())
			{
				getApplicationData().getProcessData("ServerState").setValue(3);
				launch_Server();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * public static Boolean WriteVar(DataType _Var, String _Value, DataExchange
	 * dp) throws IllegalArgumentException, IllegalAccessException { try {
	 * if(_Var.Type instanceof Integer) { try { //_Var.setInt(app,
	 * Integer.parseInt(_Value)); dp.setInt(_Var.Name,
	 * Integer.parseInt(_Value)); } catch (Exception e) {
	 * System.out.println("Error converting int variable : " + _Var.Name);
	 * return false; } return true; } if(_Var.Type instanceof Double) { try {
	 * //_Var.setDouble(app, Double.parseDouble(_Value));
	 * dp.setDouble(_Var.Name, Double.parseDouble(_Value)); } catch (Exception
	 * e) { System.out.println("Error converting double variable : " +
	 * _Var.Name); return false; } return true; } if(_Var.Type instanceof
	 * Boolean) { Boolean Val = _Value.toUpperCase().equals("TRUE"); try {
	 * //_Var.setBoolean(app, Val); dp.setBool(_Var.Name, Val); } catch
	 * (Exception e) { System.out.println("Error converting boolean variable : "
	 * + _Var.Name); return false; } return true; } if(_Var.Type instanceof
	 * JointPosition) { try { double[] jv = new double[7]; String[] splitted =
	 * _Value.split(",");
	 * 
	 * for(int i = 0; i < splitted.length; i++) { String[] curAxisSplit =
	 * splitted[i].trim().split(" "); double curVal =
	 * Double.parseDouble(curAxisSplit[curAxisSplit.length-1].trim());
	 * 
	 * if(curAxisSplit[0].trim().equals("A1")) jv[0] = curVal;
	 * if(curAxisSplit[0].trim().equals("A2")) jv[1] = curVal;
	 * if(curAxisSplit[0].trim().equals("A3")) jv[2] = curVal;
	 * if(curAxisSplit[0].trim().equals("A4")) jv[3] = curVal;
	 * if(curAxisSplit[0].trim().equals("A5")) jv[4] = curVal;
	 * if(curAxisSplit[0].trim().equals("A6")) jv[5] = curVal;
	 * if(curAxisSplit[0].trim().equals("A7")) jv[6] = curVal; }
	 * 
	 * JointPosition curJP = new JointPosition(jv); dp.setJP(_Var.Name, curJP);
	 * //_Var.set(app, curJP); } catch (Exception e) {
	 * System.out.println("Error converting E7AXIS variable : " + _Var.Name);
	 * return false; } return true; }
	 * 
	 * return false; } catch (Exception e) { e.printStackTrace();
	 * System.out.println("Error: " + e.toString()); return false; } }
	 */

	public Boolean WriteVar(Field _Var, String _Value)
			throws IllegalArgumentException, IllegalAccessException {
		try {
			
			if (_Var.getType().getSimpleName().equalsIgnoreCase("int")) {
				try {
					// _Var.setInt(app, Integer.parseInt(_Value));
					//_Var.setInt(dp, Integer.parseInt(_Value));
				} catch (Exception e) {
					System.out.println("Error converting int variable : "
							+ _Var.getName());
					return false;
				}
				return true;
			}
			if (_Var.getType().getSimpleName().equalsIgnoreCase("double")) {
				try {
					//_Var.setDouble(dp, Double.parseDouble(_Value));
					// dp.setDouble(_Var.Name, Double.parseDouble(_Value));
				} catch (Exception e) {
					System.out.println("Error converting double variable : "
							+ _Var.getName());
					return false;
				}
				return true;
			}
			if (_Var.getType().getSimpleName().equalsIgnoreCase("boolean")) {
				Boolean Val = _Value.toUpperCase().equals("TRUE");
				try {
					//_Var.setBoolean(dp, Val);
					// dp.setBool(_Var.Name, Val);
				} catch (Exception e) {
					System.out.println("Error converting boolean variable : "
							+ _Var.getName());
					return false;
				}
				return true;
			}
			if (_Var.getType().getSimpleName().equalsIgnoreCase("JointPosition")) {
				try {
					double[] jv = new double[7];
					String[] splitted = _Value.replace('[', ' ')
							.replace(']', ' ').split(",");

					/*
					 * for(int i = 0; i < splitted.length; i++) { String[]
					 * curAxisSplit = splitted[i].trim().split(" "); double
					 * curVal =
					 * Double.parseDouble(curAxisSplit[curAxisSplit.length
					 * -1].trim());
					 * 
					 * if(curAxisSplit[0].trim().equals("A1")) jv[0] =
					 * curVal*Deg2Rad; if(curAxisSplit[0].trim().equals("A2"))
					 * jv[1] = curVal*Deg2Rad;
					 * if(curAxisSplit[0].trim().equals("A3")) jv[2] =
					 * curVal*Deg2Rad; if(curAxisSplit[0].trim().equals("A4"))
					 * jv[3] = curVal*Deg2Rad;
					 * if(curAxisSplit[0].trim().equals("A5")) jv[4] =
					 * curVal*Deg2Rad; if(curAxisSplit[0].trim().equals("A6"))
					 * jv[5] = curVal*Deg2Rad;
					 * if(curAxisSplit[0].trim().equals("A7")) jv[6] =
					 * curVal*Deg2Rad; }
					 */

					for (int i = 0; i < splitted.length && i < jv.length; i++)
						jv[i] = Math.toRadians(Double.parseDouble(splitted[i]));

					JointPosition curJP = new JointPosition(jv);
					// dp.setJP(_Var.Name, curJP);
					//_Var.set(dp, curJP);
				} catch (Exception e) {
					System.out.println("Error converting E7AXIS variable : "
							+ _Var.getName());
					return false;
				}
				return true;
			}		
				
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error: " + e.toString());
			return false;
		}
	}

	public Object ReadVar(Field _Var, IDataExchange dp)
			throws IllegalArgumentException, IllegalAccessException {
		try {
			String Tipo = _Var.getType().getSimpleName();
			if (Tipo.equalsIgnoreCase("int")) {
				return _Var.getInt(dp);
			}
			if (Tipo.equalsIgnoreCase("double")) {
				return _Var.getDouble(dp);
			}
			if (Tipo.equalsIgnoreCase("boolean")) 
			{				
				return _Var.getBoolean(dp);
			}
			if (Tipo.equalsIgnoreCase("JointPosition")) {
				String s = _Var.get(dp).toString();

				JointPosition f = (JointPosition)_Var.get(dp);
				return "[" + Math.toDegrees(f.get()[0]) + ", " +
				Math.toDegrees(f.get()[1]) + ", " + Math.toDegrees(f.get()[2])
				+ ", " + Math.toDegrees(f.get()[3]) + ", " +
				Math.toDegrees(f.get()[4])
				+ ", " + Math.toDegrees(f.get()[5]) + ", " +
				Math.toDegrees(f.get()[6]) + "]";
			}
			if (Tipo.equalsIgnoreCase("Frame")) {
				Frame e = (com.kuka.roboticsAPI.geometricModel.Frame) _Var
						.get(dp);
				return GetFrameString(e);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return null;
	}

	private String GetFrameString(Frame e) {

		double x = 0, y = 0, z = 0, a = 0, b = 0, c = 0;
		x = e.getX();
		y = e.getY();
		z = e.getZ();
		a = Math.toDegrees(e.getAlphaRad());
		b = Math.toDegrees(e.getBetaRad());
		c = Math.toDegrees(e.getGammaRad());

		String temp = "{E6POS: X " + x + ", Y " + y + ", Z " + z + ", A " + a
				+ ", B " + b + ", C " + c;

		IRedundancyCollection red = e
				.getRedundancyInformationForDevice(lbr_iiwa_14_R820_1);

		if (red != null) {
			if (red instanceof StatusTurnRedundancy) {
				temp += ", S " + ((StatusTurnRedundancy) red).getStatus()
						+ ", T " + ((StatusTurnRedundancy) red).getTurn();
			}
			if (red instanceof LBRAlphaRedundancy) {
				temp += ", R " + ((LBRAlphaRedundancy) red).getAlpha();
			}
		}
		temp += "}";

		return temp;
	}

	public void AnalizeData(String _Data, PrintWriter _out)
			throws IOException
			{
		try {
			String SpliString = "-!EuclidSeparator!-";
			String[] SplittedData;
			SplittedData = _Data.split(SpliString);

			if (SplittedData[0].equalsIgnoreCase("WriteVar")) {
				System.out.println("Received WriteVar command");
				// Devo scrivere una variabile

				System.out.println("splitted " + SplittedData[1]);

				if (SplittedData[1].equalsIgnoreCase("NewScan[]"))			
				{
					String tmp = SplittedData[2].replaceAll("\"", "");
					getApplicationData().getProcessData("NewScan").setValue(tmp.equalsIgnoreCase("true"));
				}
				if (SplittedData[1].equalsIgnoreCase("Buono[]"))			
				{
					String tmp = SplittedData[2].replaceAll("\"", "");
					getApplicationData().getProcessData("Buono").setValue(tmp.equalsIgnoreCase("true"));
				}
				if (SplittedData[1].equalsIgnoreCase("Scarto[]"))			
				{
					String tmp = SplittedData[2].replaceAll("\"", "");
					getApplicationData().getProcessData("Scarto").setValue(tmp.equalsIgnoreCase("true"));
				}
				if (SplittedData[1].equalsIgnoreCase("CheckAlignment[]"))			
				{
					String tmp = SplittedData[2].replaceAll("\"", "");
					getApplicationData().getProcessData("CheckAlignment").setValue(tmp.equalsIgnoreCase("true"));
				}
				if (SplittedData[1].equalsIgnoreCase("ResAlignment[]"))			
				{
					String tmp = SplittedData[2].replaceAll("\"", "");
					getApplicationData().getProcessData("ResAlignment").setValue(tmp.equalsIgnoreCase("true"));
				}
				if (SplittedData[1].equalsIgnoreCase("DeltaX[]"))			
				{
					String tmp = SplittedData[2].replaceAll("\"", "");
					getApplicationData().getProcessData("DeltaX").setValue(Double.parseDouble(tmp));
				}
				if (SplittedData[1].equalsIgnoreCase("DeltaY[]"))			
				{
					String tmp = SplittedData[2].replaceAll("\"", "");
					getApplicationData().getProcessData("DeltaY").setValue(Double.parseDouble(tmp));
				}
			}
			if (SplittedData[0].equalsIgnoreCase("ReadVar")) 
			{
				Boolean Result = null;
				Integer IntResult = null;
				
				String test = SplittedData[1];
				
				if (SplittedData[1].equalsIgnoreCase("NewScan"))			
				{
					Result = getApplicationData().getProcessData("NewScan").getValue();

					if (Result != null)
					{
						String ResString = "OK" + SpliString + Result.toString();
						_out.print(ResString);
						_out.flush();
					}					
				}
				if (test.equalsIgnoreCase("Buono"))			
				{
					Result = getApplicationData().getProcessData("Buono").getValue();

					if (Result != null)
					{
						String ResString = "OK" + SpliString + Result.toString();
						_out.print(ResString);
						_out.flush();
					}					
				}
				if (SplittedData[1].equalsIgnoreCase("Scarto"))			
				{
					Result = getApplicationData().getProcessData("Scarto").getValue();

					if (Result != null)
					{
						String ResString = "OK" + SpliString + Result.toString();
						_out.print(ResString);
						_out.flush();
					}					
				}
				if (SplittedData[1].equalsIgnoreCase("CheckAlignment"))			
				{
					Result = getApplicationData().getProcessData("CheckAlignment").getValue();

					if (Result != null)
					{
						String ResString = "OK" + SpliString + Result.toString();
						_out.print(ResString);
						_out.flush();
					}					
				}				
				if (SplittedData[1].equalsIgnoreCase("CodiceModello"))			
				{
					String ResString = "OK" + SpliString + IOLinea.getCodiceModelloIN();
					_out.print(ResString);
					_out.flush();	
				}
				return;
			}
			return;
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} finally {
		}
	}

	private Object GetSystemVar(String string) {

		if (string.equalsIgnoreCase("$AXIS_ACT")) {
			return lbr_iiwa_14_R820_1.getCurrentJointPosition();
		} else if (string.equalsIgnoreCase("$POS_ACT")) {
			return lbr_iiwa_14_R820_1.getCurrentCartesianPosition(World.Current
					.getRootFrame());
		}
		return null;
	}

	private Boolean ExtractPayload(String _data, String varValue,
			Boolean headerFound) {
		headerFound = false;
		int ptrFinishHeader = 0;
		byte[] sizeByte = new byte[4];

		char[] DataCharArray = _data.toCharArray();
		char[] HeaderCharArray = header.toCharArray();

		System.out.println("Start ricerca header");

		// Ricerca dell'header del messaggio
		for (int idQ = 0; idQ < DataCharArray.length; idQ++) {
			int startPtrAnalyze = idQ;
			int numFound = 0;

			for (int idH = 0; idH < DataCharArray.length; idH++) {
				if (DataCharArray[idQ] == HeaderCharArray[idH]) {
					idQ++;
					numFound++;
				} else {
					idQ = startPtrAnalyze;
					break;
				}
				if (numFound == HeaderCharArray.length) {
					System.out.println("Header found");
					headerFound = true;
					break;
				}
			}

			if (headerFound) {
				ptrFinishHeader = idQ;
				break;
			}

			if (!headerFound
					&& idQ >= (DataCharArray.length - HeaderCharArray.length
							- sizeByte.length - 1)) {
				System.out.println("Not header found");
				return false;
			}
		}

		if (!headerFound) {
			System.out.println("Not header found");
			return false;
		}

		// controllo di avere la size
		System.out.println("Controllo size");
		System.out.println("Finish header : " + ptrFinishHeader);
		int sizeInt = 0;
		sizeByte[0] = (byte) DataCharArray[ptrFinishHeader];
		sizeByte[1] = (byte) DataCharArray[ptrFinishHeader + 1];
		sizeByte[2] = (byte) DataCharArray[ptrFinishHeader + 2];
		sizeByte[3] = (byte) DataCharArray[ptrFinishHeader + 3];

		String curSize = new String(sizeByte);
		sizeInt = Integer.parseInt(curSize);
		System.out.println("Size:" + sizeInt);

		byte[] curPayload;

		// controllo di avere una dimensione sufficiente per avere payload e chk
		if ((_data.length() - ptrFinishHeader - sizeByte.length) > sizeInt) {
			curPayload = new byte[sizeInt - 1];
			int idPayload = 0;
			// array copy
			for (int id = ptrFinishHeader + sizeByte.length; id < ptrFinishHeader
					+ sizeByte.length + curPayload.length; id++) {
				curPayload[idPayload] = (byte) DataCharArray[id];
				idPayload++;
			}

			// controllo il chk
			int computeChk = ComposeCheck(sizeInt, curPayload);
			byte[] curChk = new byte[4];
			curChk[0] = (byte) DataCharArray[ptrFinishHeader + sizeByte.length
					+ curPayload.length];
			curChk[1] = (byte) DataCharArray[ptrFinishHeader + sizeByte.length
					+ curPayload.length + 1];
			curChk[2] = (byte) DataCharArray[ptrFinishHeader + sizeByte.length
					+ curPayload.length + 2];
			curChk[3] = (byte) DataCharArray[ptrFinishHeader + sizeByte.length
					+ curPayload.length + 3];

			String curChkStr = new String(curChk);
			int cChk = Integer.parseInt(curChkStr);

			if (computeChk != cChk) {
				return false;
			}
		} else {
			return false;
		}
		varValue = new String(curPayload);// System.Text.Encoding.ASCII.GetString(curPayload,
											// 0, curPayload.Length);
		System.out.println("Valore finale: " + varValue);
		return true;
	}

	private int ComposeCheck(int size, byte[] curPayload) {
		byte[] byteSize = new byte[2];

		byteSize[0] = (byte) (size >> 8);
		byteSize[1] = (byte) size;

		int chk = 0;
		for (byte byteS : byteSize) {
			chk = chk ^ byteS;
		}
		for (byte byteP : curPayload) {
			chk = chk ^ byteP;
		}
		return chk;
	}

	public void launch_Server() throws IOException {
		int charRead = 0;
		char[] cBuff = new char[1024];

		try {
			getApplicationData().getProcessData("ServerState").setValue(4);
			if (getApplicationData().getProcessData("ServerFault") != null
					&& (serverSocket == null || serverSocket.isClosed())) {
				getApplicationData().getProcessData("ServerFault").setValue(
						true);
				return;
			}

			if (dataExchanger.isAvailable()
					&& ((getApplicationData().getProcessData("StartServer") == null) || (getApplicationData()
							.getProcessData("StartServer") != null && (Boolean) (getApplicationData()
							.getProcessData("StartServer").getValue())))) {
				Socket clientSocket = null;
				try {

					if (getApplicationData().getProcessData("ServerOn") != null)
						getApplicationData().getProcessData("ServerOn")
								.setValue(true);
					// serverSocket.setSoTimeout(5000);

					do 
					{
						clientSocket = serverSocket.accept();
					} while (clientSocket == null
							&& ((getApplicationData().getProcessData(
									"StartServer") == null) || (getApplicationData()
									.getProcessData("StartServer") != null && (Boolean) (getApplicationData()
									.getProcessData("StartServer").getValue()))));

					if (clientSocket != null) {

						PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), false);
						BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
						System.out.println("Client connesso sulla porta "
								+ clientSocket.getLocalPort());

						while ((charRead = in.read(cBuff)) != -1 && ((getApplicationData()
								.getProcessData("StartServer") == null) || (getApplicationData()
										.getProcessData("StartServer") != null && (Boolean) (getApplicationData()
										.getProcessData("StartServer")
										.getValue())))) 
						{
							StringBuilder Received = new StringBuilder();
							for (int i = 0; i < charRead; i++)
								Received.append(cBuff[i]);
							String DataReceived = new String(Received);							
							System.out.println("Stringa arrivata: "
									+ DataReceived);
							AnalizeData(DataReceived, out);
						}
					}
				} 
				catch (Exception e)
				{
					e.printStackTrace();
				} finally 
				{
					if (clientSocket != null && clientSocket.isConnected())
					{
						System.out.println("Client disconnesso");
						clientSocket.close();
					}
				}
			}
		} catch (IOException e) {
			System.out
					.println("Exception caught when trying to listen on port "
							+ portNumber + " or listening for a connection");
		}

		if (getApplicationData().getProcessData("ServerOn") != null)
			getApplicationData().getProcessData("ServerOn").setValue(false);

		System.out.println("Applicazione in chiusura");
	}

	@Override
	public void dispose() {
		try 
		{
			if (getApplicationData().getProcessData("ServerOn") != null)
				getApplicationData().getProcessData("ServerOn").setValue(false);
		} 
		catch (Exception ex) 
		{
			ex.printStackTrace();
		}
		try 
		{
			if (serverSocket != null)
				serverSocket.close();
		} 
		catch (Exception ex) 
		{
			ex.printStackTrace();
		}
		app = null;
	}

	/**
	 * Auto-generated method stub. Do not modify the contents of this method.
	 */
	public static void main(String[] args) {
		if (app == null) 
		{
			int port = 30010;
			if (args.length > 0) 
			{
				try 
				{
					port = Integer.parseInt(args[0]);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			app = new KrcServer(port);
			app.run();
		}
	}

	
	@TaskFunctionProvider
	public IDataExchange getAppInfoFunction() 
	{
		return dataExchanger;
	}
	
}
