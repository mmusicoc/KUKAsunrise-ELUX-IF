package application;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import com.kuka.generated.ioAccess.EthercatIOIOGroup;
import com.kuka.generated.ioAccess.LineaCassioliIOGroup;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPIBackgroundTask;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.controllerModel.recovery.IRecovery;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.task.ITaskLogger;

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
 * @see UseRoboticsAPIContext
 * 
 */
public class Sps extends RoboticsAPIBackgroundTask {
	@Inject
	private Controller kuka_Sunrise_Cabinet_1;
	private LBR lbr_iiwa_14_R820_1;
	
	private EthercatIOIOGroup ethercatModule;
	private LineaCassioliIOGroup IOLinea;
	
	private IRecovery _applicationRecoveryInterface;
	
	private Tool tool1;
	
	@Override
	public void initialize() 
	{
		// initialize your task here
		/*initializeCyclic(0, 10, TimeUnit.MILLISECONDS,
				CycleBehavior.BestEffort);
		*/
		//Inizializzazione controller e robot
		kuka_Sunrise_Cabinet_1 = getController("KUKA_Sunrise_Cabinet_1");
		lbr_iiwa_14_R820_1 = (LBR) getDevice(kuka_Sunrise_Cabinet_1,
				"LBR_iiwa_14_R820_1");
		
		//Inizializzazione moduli IO
		ethercatModule = new EthercatIOIOGroup(kuka_Sunrise_Cabinet_1);
		IOLinea = new LineaCassioliIOGroup(kuka_Sunrise_Cabinet_1);
		
		//Inizializzazione tool
		tool1 = (Tool)getApplicationData().createFromTemplate("ToolElectrType1");
		tool1.attachTo(lbr_iiwa_14_R820_1.getFlange());			
	}

	/*@Override
	public void runCyclic() 
	{
		
		//Verifico se devo dare l'External start
		if(lbr_iiwa_14_R820_1.getOperationMode().isModeAuto())
		{
			if(lbr_iiwa_14_R820_1.isReadyToMove() & !ethercatModule.getExtStart())
			{
				ethercatModule.setOutExtStart(true);
			}
			else
			{
				ethercatModule.setOutExtStart(false);
			}			
		}
		else
		{
			ethercatModule.setOutExtStart(false);
		}
		
		// Verifico stato selettori per accensione illuminatore		
		if(IOLinea.getLineaAuto() & !IOLinea.getRobotEscluso())
		{
			ethercatModule.setIlluminatoreON(true);
		}
		else
		{
			ethercatModule.setIlluminatoreON(false);
		}
		
		//Segnale di fuori ingombro alla linea
		Frame ActPos = lbr_iiwa_14_R820_1.getCurrentCartesianPosition(tool1.getFrame("TcpType1"));
		
		if(ActPos.getZ()<270)
		{
			IOLinea.setFuoriIngombro(false);
		}
		else
		{
			IOLinea.setFuoriIngombro(true);
		}
				
				
		//Comando per allineamento
		if(IOLinea.getLineaAuto() & !IOLinea.getRobotEscluso())
		{
			if(IOLinea.getLBInPos() & getApplicationData().getProcessData("LavDone").getValue().equals(false))
			{
				//getApplicationData().getProcessData("CheckAlignment").setValue("true");		
				//getApplicationData().getProcessData("NewScan").setValue("true");		
				getApplicationData().getProcessData("LavDone").setValue("true");				
			}
			if(IOLinea.getLBInPos() & getApplicationData().getProcessData("LavDone").getValue().equals(true))
			{
				if(getApplicationData().getProcessData("ResAlignment").getValue().equals(true))
				{					
					//IOLinea.setFineCiclo(true);
				}
			}
			
			if(!IOLinea.getLBInPos())
			{			
				getApplicationData().getProcessData("LavDone").setValue("false");
				getApplicationData().getProcessData("ResAlignment").setValue("false");
				IOLinea.setRobotNOK(true);
				IOLinea.setFineCiclo(false);
				IOLinea.setEsitoOK(false);
				IOLinea.setReset(false);
			}
		}
		else
		{			
			IOLinea.setRobotNOK(true);
			IOLinea.setFineCiclo(false);
			IOLinea.setEsitoOK(false);
			IOLinea.setReset(false);
		}
		
		//Feedback del codice programma alla linea				
		IOLinea.setCodiceModelloOut(IOLinea.getCodiceModelloIN());						
	}*/

	@Override
	public void run() throws Exception 
	{
		// TODO Auto-generated method stub
		//Verifico se devo dare l'External start
		while(true)
		{
			
			   ethercatModule.setOut_16(true);
			
			
				// Verifico stato selettori per accensione illuminatore		
				if(IOLinea.getLineaAuto() & !IOLinea.getRobotEscluso())
				{
					ethercatModule.setIlluminatoreON(true);
				}
				else
				{
					ethercatModule.setIlluminatoreON(false);
				}
												
				//Segnale di fuori ingombro alla linea
				Frame ActPos = lbr_iiwa_14_R820_1.getCurrentCartesianPosition(tool1.getFrame("TcpType1"));
				
				// Gestione fuori ingombro robot verso la linea
				if(ActPos.getZ()<270)
				{
					IOLinea.setFuoriIngombro(false);
				}
				else
				{
					IOLinea.setFuoriIngombro(true);
				}
				
				// Pulsante per il restart del ciclo senza spedire la lavatrice
				if(ethercatModule.getBtnRestartRobot())
				{
					getApplicationData().getProcessData("ResetCycle").setValue("true");	
				}		
				
				//Gestione pressione pulsante di azione 
				if(ethercatModule.getLedStartRobot())
				{
					if(ethercatModule.getBtStartRobot())
					{
						getApplicationData().getProcessData("BtnAction").setValue("true");		
					}						
				}
				
				// Verifico se il risultato del controllo è buono o scarto
				if(getApplicationData().getProcessData("Buono").getValue().equals(true))
				{
					IOLinea.setFineCiclo(true);
					getApplicationData().getProcessData("VisionDataOK").setValue("true");
				}
				
				//Se la visione mi da scarto accendo il led del pulsante scarto
				if(getApplicationData().getProcessData("Scarto").getValue().equals(true))
				{
					ethercatModule.setLedScarto(true);
					getApplicationData().getProcessData("VisionDataOK").setValue("true");
				}
				
				if(getApplicationData().getProcessData("Scarto").getValue().equals(true) & ethercatModule.getBtnScarto())
				{
					getApplicationData().getProcessData("Buono").setValue("false");	
					getApplicationData().getProcessData("Scarto").setValue("false");	
					ethercatModule.setLedScarto(false);
					IOLinea.setFineCiclo(true);
				}
						
				//Comando per allineamento
				if(IOLinea.getLineaAuto() & !IOLinea.getRobotEscluso())
				{
					//Pulsante di ripristino usato per spedire la lavatrice in caso si blocchi qualcosa
					if(ethercatModule.getBtnRipristino())
					{
						IOLinea.setFineCiclo(true);						
					}
					
					//Se  mi arriva una nuova lavatrice segnalo al sistema di visione di controllare la posizione della lavatrice
					if(IOLinea.getLBInPos() & getApplicationData().getProcessData("LavDone").getValue().equals(false))
					{
						getApplicationData().getProcessData("CheckAlignment").setValue("true");		
						getApplicationData().getProcessData("Buono").setValue("false");	
						getApplicationData().getProcessData("Scarto").setValue("false");
						getApplicationData().getProcessData("LavDone").setValue("true");				
					}
										
					// Se non ho la lavatrice in posizione faccio un reset generale delle variabili
					if(!IOLinea.getLBInPos())
					{			
						getApplicationData().getProcessData("LavDone").setValue("false");
						getApplicationData().getProcessData("ResAlignment").setValue("false");
						getApplicationData().getProcessData("Buono").setValue("false");	
						getApplicationData().getProcessData("Scarto").setValue("false");	
						ethercatModule.setLedScarto(false);
						IOLinea.setRobotNOK(true);
						IOLinea.setFineCiclo(false);
						IOLinea.setEsitoOK(false);
						IOLinea.setReset(false);
					}
				}
				else
				{			
					IOLinea.setRobotNOK(true);
					IOLinea.setFineCiclo(false);
					IOLinea.setEsitoOK(false);
					IOLinea.setReset(false);
				}
				
				//Feedback del codice programma alla linea				
				IOLinea.setCodiceModelloOut(IOLinea.getCodiceModelloIN());	
		}
	}
}