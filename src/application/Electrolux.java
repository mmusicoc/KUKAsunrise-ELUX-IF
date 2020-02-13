package application;

import static com.kuka.roboticsAPI.motionModel.BasicMotions.lin;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.linRel;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.ptp;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.ptpHome;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import com.kuka.common.ThreadUtil;
import com.kuka.generated.ioAccess.EthercatIOIOGroup;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.LineaCassioliIOGroup;
import com.kuka.roboticsAPI.ioModel.AbstractIO;
import com.kuka.roboticsAPI.applicationModel.IApplicationControl;
import com.kuka.roboticsAPI.applicationModel.IApplicationData;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.conditionModel.BooleanIOCondition;
import com.kuka.roboticsAPI.conditionModel.ConditionObserver;
import com.kuka.roboticsAPI.conditionModel.IAnyEdgeListener;
import com.kuka.roboticsAPI.conditionModel.ICallbackAction;
import com.kuka.roboticsAPI.conditionModel.ICondition;
import com.kuka.roboticsAPI.conditionModel.JointTorqueCondition;
import com.kuka.roboticsAPI.conditionModel.MotionPathCondition;
import com.kuka.roboticsAPI.conditionModel.NotificationType;
import com.kuka.roboticsAPI.conditionModel.ObserverManager;
import com.kuka.roboticsAPI.conditionModel.ReferenceType;
import com.kuka.roboticsAPI.controllerModel.Controller;
import com.kuka.roboticsAPI.controllerModel.sunrise.ISunriseRequestService;
import com.kuka.roboticsAPI.controllerModel.sunrise.ResumeMode;
import com.kuka.roboticsAPI.controllerModel.sunrise.SunriseExecutionService;
import com.kuka.roboticsAPI.controllerModel.sunrise.api.SSR;
import com.kuka.roboticsAPI.controllerModel.sunrise.api.SSRFactory;
import com.kuka.roboticsAPI.controllerModel.sunrise.connectionLib.Message;
import com.kuka.roboticsAPI.controllerModel.sunrise.positionMastering.PositionMastering;
import com.kuka.roboticsAPI.deviceModel.Device;
import com.kuka.roboticsAPI.deviceModel.JointEnum;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.deviceModel.LBR;
import com.kuka.roboticsAPI.deviceModel.OperationMode;
import com.kuka.roboticsAPI.executionModel.IFiredTriggerInfo;
import com.kuka.roboticsAPI.geometricModel.CartDOF;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.motionModel.ErrorHandlingAction;
import com.kuka.roboticsAPI.motionModel.IErrorHandler;
import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.motionModel.PTP;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;
import com.kuka.roboticsAPI.motionModel.controlModeModel.JointImpedanceControlMode;
import com.kuka.task.ITaskLogger;

import application.Context;
import application.SpeedReduction;

public class Electrolux extends RoboticsAPIApplication {

	// Ingressi uscite
	// ethercatModule in 1 Pulsante apri-chiudi pinza da ciclo
	// ethercatModule in 2 Selettore inserimento visione
	// ethercatModule in 3 Pulsante Restart ciclo robot
	// ethercatModule in 4 Pulsante spedisci scarto scarto
	// ethercatModule in 4 Pulsante ripristino emergenze
	// ethercatModule out 1 Led pulsante carica graffette
	// ethercatModule out 2 Led pulsante tubo inserito
	// ethercatModule out 3 Led pulsante buono
	// ethercatModule out 4 Led pulsante scarto
	// ethercatModule out 7 pinza apri
	// ethercatModule out 8 pinza chiudi

	// IOPinza pin 13 tipo pinza
	// IOPinza pin 10 pinza aperta
	// IOPinza pin 16 pinza chiusa

	// private static BackgroundServer Server;

	private SpeedReduction speedReduction;
	
	private Context context;
	
	private final static double sideOffset = Math.toRadians(5); // offset in
																// radians for
																// side motion
	private static double joggingVelocity = 0.2; // relative velocity
	private final static int axisId[] = { 0, 1, 2, 3, 4, 5, 6 }; // axes to be
																	// referenced
	private final static int GMS_REFERENCING_COMMAND = 2; // safety command for
															// GMS referencing
	private final static int COMMAND_SUCCESSFUL = 1;
	private int positionCounter = 0;

	@Inject
	private Controller kuka_Sunrise_Cabinet_1;
	@Inject
	private LBR lbr_iiwa_14_R820_1;

	@Inject
	private MediaFlangeIOGroup IOPinza;
	@Inject
	private LineaCassioliIOGroup IOLinea;

	// Tool
	// private Tool actTool;
	private Tool tool1;
	private Tool tool2;

	public int depSequence;

	@Inject
	private EthercatIOIOGroup ethercatModule;

	private Boolean inPrel = false;
	private Boolean inDep = false;

	// Dichiarazione variabili collisione
	private boolean enableCheckCollision = false;
	private boolean emergencyStop = false;

	private int CodiceModello;

	private IAnyEdgeListener stop;
	private JointTorqueCondition joint1, joint2, joint3, joint4, joint5,
			joint6, joint7;
	private ICondition collision;
	private ConditionObserver normalObserver;

	private BooleanIOCondition PulsanteApriChiudiCnd;
	private BooleanIOCondition PulsanteRestartCnd;
	private BooleanIOCondition PinzaChiusaCnd;
	private BooleanIOCondition PinzaApertaCnd;
	private BooleanIOCondition PulsanteScartoCnd;

	private AbstractIO PulsanteApriChiudi;
	private AbstractIO SelettoreVisione;
	private AbstractIO PulsanteRestart;
	private AbstractIO PulsanteScarto;
	private AbstractIO LavInPos;
	private AbstractIO RobotEscluso;

	private BooleanIOCondition RobotInclusoCond;
	private BooleanIOCondition RobotEsclusoCond;
	private AbstractIO PinzaChiusa;
	private AbstractIO PinzaAperta;

	private Boolean BtnBuono;
	private Boolean BtnScarto;
	private Boolean UseCamera;

	public JointImpedanceControlMode jointSoftMode;

	private IErrorHandler errorHandler;

	MotionPathCondition pathCondition = new MotionPathCondition(
			ReferenceType.DEST, 0, 0);

	private boolean CycleAborted = false;

	private boolean DoPhoto = false;
	
	private static final double _SECURITY_OVERRIDE = 0.0;
	
	@Inject	private ObserverManager		obsManager;
	@Inject	private ITaskLogger			logger;
	@Inject	private IApplicationControl appControl;
	@Inject private IApplicationData 	appData;

	ICallbackAction SetFineCiclo = new ICallbackAction() {

		@Override
		public void onTriggerFired(IFiredTriggerInfo triggerInformation) {
			// TODO Auto-generated method stub
			IOLinea.setFineCiclo(true);
		}

	};

	@Override
	public void initialize() {
		// kuka_Sunrise_Cabinet_1 = getController("KUKA_Sunrise_Cabinet_1");
		// lbr_iiwa_14_R820_1 = (LBR) getDevice(kuka_Sunrise_Cabinet_1,
		// "LBR_iiwa_14_R820_1");

		// Tool
		tool1 = (Tool) getApplicationData().createFromTemplate(
				"ToolElectrType1");
		tool1.attachTo(lbr_iiwa_14_R820_1.getFlange());

		tool2 = (Tool) getApplicationData().createFromTemplate(
				"ToolElectrType2");
		tool2.attachTo(lbr_iiwa_14_R820_1.getFlange());

		JointPosition HomePos = new JointPosition(Math.toRadians(-90), 0, 0, 0,
				0, 0, Math.toRadians(-75));

		lbr_iiwa_14_R820_1.setHomePosition(HomePos);
		
		context = new Context(kuka_Sunrise_Cabinet_1, lbr_iiwa_14_R820_1, ethercatModule, appData, obsManager, logger, appControl);
		
		speedReduction = new SpeedReduction(context,_SECURITY_OVERRIDE);
		
		speedReduction.enable();

		errorHandler = new IErrorHandler() {

			@Override
			public ErrorHandlingAction handleError(Device device,
					IMotionContainer failedContainer,
					List<IMotionContainer> canceledContainers) {
				// TODO Auto-generated method stub
				System.out.println("riga193 Errore movimento: "
						+ failedContainer.getCommand().toString());

				for (int i = 0; i < canceledContainers.size(); i++) {
					System.out
							.println("riga197 Errore movimento: "
									+ canceledContainers.get(i).getCommand()
											.toString());
				}

				return ErrorHandlingAction.Ignore;
			}

		};

		inPrel = false;
		inDep = false;

		enableCheckCollision = true;
		emergencyStop = false;

		ethercatModule.setLedRestartRobot(false);
		ethercatModule.setLedRipristino(false);
		ethercatModule.setLedScarto(false);
		ethercatModule.setLedStartRobot(false);

		// Impostazione segnali e condizioni per ingressi bottone
		PulsanteApriChiudi = ethercatModule.getInput("BtStartRobot");
		SelettoreVisione = ethercatModule.getInput("NoVision");
		PulsanteRestart = ethercatModule.getInput("BtnRestartRobot");
		PulsanteScarto = ethercatModule.getInput("BtnScarto");

		PinzaChiusa = IOPinza.getInput("PinzaChiusa");
		PinzaAperta = IOPinza.getInput("PinzaAperta");

		PulsanteRestartCnd = new BooleanIOCondition(PulsanteRestart, true);
		PulsanteApriChiudiCnd = new BooleanIOCondition(PulsanteApriChiudi, true);
		PinzaChiusaCnd = new BooleanIOCondition(PinzaChiusa, true);
		PinzaApertaCnd = new BooleanIOCondition(PinzaAperta, true);
		PulsanteScartoCnd = new BooleanIOCondition(PulsanteScarto, true);

		LavInPos = IOLinea.getInput("LBInPos");

		RobotEscluso = IOLinea.getInput("RobotEscluso");

		RobotInclusoCond = new BooleanIOCondition(RobotEscluso, false);
		RobotEsclusoCond = new BooleanIOCondition(RobotEscluso, true);

		joint1 = new JointTorqueCondition(lbr_iiwa_14_R820_1, JointEnum.J1,
				-20, +20);
		joint2 = new JointTorqueCondition(lbr_iiwa_14_R820_1, JointEnum.J2,
				-120, 120);
		joint3 = new JointTorqueCondition(lbr_iiwa_14_R820_1, JointEnum.J3,
				-20, +20);
		joint4 = new JointTorqueCondition(lbr_iiwa_14_R820_1, JointEnum.J4,
				-90, 90);
		joint5 = new JointTorqueCondition(lbr_iiwa_14_R820_1, JointEnum.J5,
				-15, +15);
		joint6 = new JointTorqueCondition(lbr_iiwa_14_R820_1, JointEnum.J6,
				-25, 25);
		joint7 = new JointTorqueCondition(lbr_iiwa_14_R820_1, JointEnum.J7,
				-15, +15);
		collision = joint1.or(joint2, joint3, joint4, joint5, joint6, joint7);

		jointSoftMode = new JointImpedanceControlMode(100.0, 100.0, 100.0,
				100.0, 100.0, 100.0, 2000.0);
		jointSoftMode.setStiffness(3000.0, 3000.0, 3000.0, 3000.0, 3000.0,
				3000.0, 200.0);

		stop = new IAnyEdgeListener() {
			@Override
			public void onAnyEdge(ConditionObserver conditionObserver,
					java.util.Date time, int missedEvents,
					boolean conditionValue) {
				if (enableCheckCollision) {
					if (!emergencyStop
							&& ((SunriseExecutionService) kuka_Sunrise_Cabinet_1
									.getExecutionService()).isPaused()) {
						ThreadUtil.milliSleep(250);
						if (inPrel) {
							System.out.println("riga293 COLLISIONE Prelievo");
							inPrel = false;
							enableCheckCollision = false;
						} else if (inDep) {
							System.out.println("riga299 COLLISIONE Deposito");
							inDep = false;
							enableCheckCollision = false;
						}
						// resume movement
						((SunriseExecutionService) kuka_Sunrise_Cabinet_1
								.getExecutionService())
								.resumeExecution(ResumeMode.OnPath);
						enableCheckCollision = true;
					} else {
						System.out.println("riga309 COLLISIONE Sicurezza");
						((SunriseExecutionService) kuka_Sunrise_Cabinet_1
								.getExecutionService()).startPause(); // pause
																		// movement
					}
					ThreadUtil.milliSleep(500);
				}
			}
		};

		normalObserver = getObserverManager().createConditionObserver(
				collision, NotificationType.OnEnable, stop);

		getApplicationControl().registerMoveAsyncErrorHandler(errorHandler);

		DoPhoto = false;
	}

	// Gestione pinza *************************************************
	public void CloseGripper() {
		// IOPinza.setOutputX3Pin1(true);
		ethercatModule.setApriPinza(false);
		ethercatModule.setChiudiPinza(true);
		Boolean closeRes = getObserverManager().waitFor(PinzaChiusaCnd, 1000,
				TimeUnit.MILLISECONDS);
		if (!closeRes) {
			System.out.println("riga 321 Pinza non chiusa, verificare");
			getApplicationControl().halt();
		}
	}

	public void OpenGripper() {
		ethercatModule.setApriPinza(true);
		ethercatModule.setChiudiPinza(false);
		Boolean openRes = getObserverManager().waitFor(PinzaApertaCnd, 100,
				TimeUnit.MILLISECONDS);
		if (!openRes) {
			System.out.println("riga 332 Pinza non aperta, verificare");
			// getApplicationControl().halt();
		}
	}

	// ****************************************************************

	public void FromDepORTOHome() {
		// Robot escluso, ritorno in home
		System.out.println("riga 340 Procedura di ritorno a home");
		lbr_iiwa_14_R820_1.moveAsync(lin(
				getApplicationData().getFrame("/PrelievoOR/AvvPrel1"))
				.setCartVelocity(100).setBlendingCart(100));
		lbr_iiwa_14_R820_1.move(ptp(
				getApplicationData().getFrame("/HomeElectr"))
				.setJointVelocityRel(0.25));
	}

	// Metodo per il calcolo della correzione del punto di inserimento
	public Frame GetCorrection(ObjectFrame _iniPos) {
		Frame res = _iniPos.copy();
		double corrX = getApplicationData().getProcessData("DeltaX").getValue();
		double corrY = getApplicationData().getProcessData("DeltaY").getValue();
		if ((corrX < -5) | (corrX > 5)) {
			corrX = 0;
			System.out.println("riga 356 Delta X fuori dai limiti");
		}
		if ((corrY < -5) | (corrY > 5)) {
			corrY = 0;
			System.out.println("riga 360 Delta Y fuori dai limiti");
		}
		res.setX(res.getX() + corrX);
		res.setY(res.getY() + corrY);
		return res;
	}

	public void ProgPezzo() {
		getApplicationData().getProcessData("BtnAction").setValue(false);
		ThreadUtil.milliSleep(1);
		enableCheckCollision = true;

		// Prelievo OR
		ethercatModule.setLedStartRobot(false);

		// Movimenti per andare in posizione di inserimento clip
		lbr_iiwa_14_R820_1.moveAsync(ptp(
				getApplicationData().getFrame("/PrelievoOR/AvvPrel1"))
				.setJointVelocityRel(1.0).setBlendingCart(100));
		lbr_iiwa_14_R820_1.move(ptp(
				getApplicationData().getFrame("/PrelievoOR/PrelOR"))
				.setJointVelocityRel(1.0));
		inPrel = true;

		IOLinea.setEsitoOK(false);
		IOLinea.setRobotNOK(true);

		ethercatModule.setLedStartRobot(true);
		ethercatModule.setLedScarto(false);

		ThreadUtil.milliSleep(1);
		System.out.println("riga 378 Posizione inserimento graffette");

		OpenGripper();

		getApplicationData().getProcessData("ResetCycle").setValue(false);

		System.out.println("riga 384 Pinze aperte");

		// Verifico se dal ciclo precedente ho un comando di controllo pezzo per
		// la telecamera
		// Attenzione getNoVIsion è contrario, cioè se 1 la visione è inserita,
		// 0 non inserita
		if (ethercatModule.getNoVision() & DoPhoto) {

			DoPhoto = false;
			getApplicationData().getProcessData("NewScan").setValue("true");

			System.out.println("riga 395 Scatto la foto");

			int timertelecamera = 0;

			do {
				ThreadUtil.milliSleep(1);
				timertelecamera++;
			} while (getApplicationData().getProcessData("NewScan").getValue()
					.equals(true)
					& (timertelecamera < 3000));

			if ((timertelecamera >= 3000)) {
				getApplicationData().getProcessData("Scarto").setValue(true);
				System.out
						.println("riga 409 Sistema di visione non risponde, segnalo lo scarto");
			} else {
				System.out.println("riga 411 Foto scattata");
				System.out.println("riga 412 Attendo il risultato");
			}

			// Attendo risultati dalla visione
			do {
				ThreadUtil.milliSleep(1);
			} while (getApplicationData().getProcessData("Scarto").getValue()
					.equals(false)
					& IOLinea.getLBInPos());

			if (getApplicationData().getProcessData("Scarto").getValue()
					.equals(false)) {
				System.out.println("riga 424 Lavatrice buona");
			} else {
				System.out
						.println("riga 427 Lavatrice scarta, attendo controllo dell'operatore");
			}
		}

		DoPhoto = false;

		System.out.println("riga 433 Attesa pulsante o esclusione robot");

		// Rimango in attesa o di un comando per la chiusura della pinza o di
		// esclusione del robot
		do {
			ThreadUtil.milliSleep(1);
		} while (getApplicationData().getProcessData("BtnAction").getValue()
				.equals(false)
				& !IOLinea.getRobotEscluso());

		if (IOLinea.getRobotEscluso())
			System.out.println("riga 444 Robot escluso, ritorno in home");

		// Spengo i led dei pulsanti
		ethercatModule.setLedStartRobot(false);

		int delaywait = 0;

		// Se non ho il robot escluso faccio il ciclo di lavoro
		if (!IOLinea.getRobotEscluso()) {
			System.out
					.println("riga 454 Robot inserito, vado a depositare le clip");
			CloseGripper();

			// Prima di partire verifico che i dati della lavatrice precedente
			// siano arrivati
			// Attenzione getNoVIsion è contrario, cioè se 1 la visione è
			// inserita, 0 non inserita

			// Se la lavatrice precedente era uno scarto attendo che l'operatore
			// la mandi avanti manualmente
			if (getApplicationData().getProcessData("Scarto").getValue()
					.equals(true))
				System.out.println("riga 466 Attesa pulsante scarto");

			do {

			} while (getApplicationData().getProcessData("Scarto").getValue()
					.equals(true));

			getApplicationData().getProcessData("VisionDataOK").setValue(
					"false");

			System.out
					.println("riga 477 Attendo lavatrice in posizione o robot escluso");

			// Attendo la lavatrice in posizione o il robot escluso
			do {

			} while (!IOLinea.getLBInPos() & !IOLinea.getRobotEscluso());

			getApplicationData().getProcessData("BtnAction").setValue(false);

			if (!IOLinea.getRobotEscluso()) {
				System.out.println("riga 487 Lavatrice arrivata");

				CodiceModello = IOLinea.getCodiceModelloIN();

				System.out.println("riga 491 Codice modello :" + CodiceModello);

				if (!CheckPinza()) {
					// pinza sbagliata, cambiare pinza
					System.out
							.println("riga 496 Pinza sbagliata, cambiare pinza");
					lbr_iiwa_14_R820_1.move(lin(
							getApplicationData().getFrame(
									"/PrelievoOR/AvvPrel1")).setCartVelocity(
							100));
					do {
						ThreadUtil.milliSleep(1000);
					} while (!CheckPinza());

					// Pinza cambiata, vado in posizione di deposito clip
					lbr_iiwa_14_R820_1
							.move(ptp(
									getApplicationData().getFrame(
											"/PrelievoOR/PrelOR"))
									.setJointVelocityRel(1.0));

					ethercatModule.setLedStartRobot(true);
					ethercatModule.setLedScarto(false);

					ThreadUtil.milliSleep(1);

					OpenGripper();

					System.out
							.println("riga 520 Posizione inserimento graffette");

					do {
						ThreadUtil.milliSleep(1);
					} while (getApplicationData().getProcessData("BtnAction")
							.getValue().equals(false)
							& !IOLinea.getRobotEscluso());
				}

				// Prima di partire attendo 1 secondo in modo che il sistema di
				// visione faccia la foto
				ThreadUtil.milliSleep(1000);

				System.out.println("riga 533 Parto per deposito graffette");

				IOLinea.setRobotNOK(false);

				// Deposito OR
				System.out
						.println("riga 539 Avvicinamento 1 deposito graffette");
				// lbr_iiwa_14_R820_1.move(ptp(getApplicationData().getFrame("/PrelievoOR/AllPrel1_inter")).setJointVelocityRel(0.7));
				lbr_iiwa_14_R820_1.move(ptp(
						getApplicationData().getFrame("/PrelievoOR/AllPrel1"))
						.setJointVelocityRel(0.7));

				System.out.println("riga 545 Modello : " + CodiceModello);

				while (CodiceModello == 0) {
					System.out
							.println("riga 549 Ciclo while codice modello == 0");
					CodiceModello = IOLinea.getCodiceModelloIN();
					// Prima di andare a depositare faccio un controllo che la
					// pinza sia corretta
					if (!CheckPinza()) {
						// pinza sbagliata, cambiare pinza
						System.out
								.println("riga 556  sbagliata, cambiare pinza");
						lbr_iiwa_14_R820_1.move(lin(
								getApplicationData().getFrame(
										"/PrelievoOR/AvvPrel1"))
								.setCartVelocity(100));
						do {
							ThreadUtil.milliSleep(1000);
						} while (!CheckPinza());

						// Pinza cambiata, vado in posizione di deposito clip
						lbr_iiwa_14_R820_1.move(ptp(
								getApplicationData().getFrame(
										"/PrelievoOR/PrelOR"))
								.setJointVelocityRel(1.0));

						ethercatModule.setLedStartRobot(true);
						ethercatModule.setLedScarto(false);

						ThreadUtil.milliSleep(1);

						OpenGripper();

						getApplicationData().getProcessData("ResetCycle")
								.setValue(false);

						System.out
								.println("riga 582 Posizione inserimento graffette");
						System.out.println("riga 583 Attesa pulsante");

						do {
							ThreadUtil.milliSleep(1);
						} while (getApplicationData()
								.getProcessData("BtnAction").getValue()
								.equals(false)
								& !IOLinea.getRobotEscluso());
					}
				}

				Frame CorrPos;

				CycleAborted = false;

				// Movimenti di inserimento per i vari modelli di lavatrice
				System.out.println("riga 599 Mi muovo verso la vaschetta");

				do {
					ThreadUtil.milliSleep(1);
				} while (getApplicationData().getProcessData("Scarto").getValue()
						.equals(true));
				
				// Attendo la lavatrice in posizione o il robot escluso
				do {

				} while (!IOLinea.getLBInPos());
				
				delaywait = 0;

				switch (CodiceModello) {
				case 1:
					System.out.println("riga 605 Movimenti modello 1");

					depSequence = 0;
					do {
						switch (depSequence) {
						case 0:
							System.out.println("riga 605 Movimento Avv1");
							tool2.move(ptp(
									getApplicationData().getFrame(
											"/Modello1/Avv1"))
									.setJointVelocityRel(1.0));
							if (getApplicationData()
									.getProcessData("ResetCycle").getValue()
									.equals(true)) {
								depSequence = 100;
							} else {
								depSequence = 1;
							}
							break;
						case 1:
							System.out.println("riga 625 Movimento Avv2");
							tool2.move(ptp(
									getApplicationData().getFrame(
											"/Modello1/Avv2"))
									.setJointVelocityRel(1.0));
							if (getApplicationData()
									.getProcessData("ResetCycle").getValue()
									.equals(true)) {
								depSequence = 0;
							} else {
								depSequence = 2;
							}
							break;
						case 2:
							System.out.println("riga 639 Movimento Avv3");
							tool2.move(lin(
									getApplicationData().getFrame(
											"/Modello1/Avv3")).setCartVelocity(
									400));
							if (getApplicationData()
									.getProcessData("ResetCycle").getValue()
									.equals(true)) {
								depSequence = 1;
							} else {
								depSequence = 100;
							}
							break;
						}
					} while (depSequence != 100);

					if (getApplicationData().getProcessData("ResetCycle")
							.getValue().equals(false)) {
						// Verifico se ho la telecamera inserita o no
						if (ethercatModule.getNoVision()) {
							// Attendo l'allineamento da telecamera
							do {
								ThreadUtil.milliSleep(1);
								delaywait++;
							} while (getApplicationData()
									.getProcessData("ResAlignment").getValue()
									.equals(false)
									& (delaywait < 4000));
						}
					}
					
					// Se non mi arriva entro 5 secondi vado in posizione 0
					if ((delaywait >= 4000) | !ethercatModule.getNoVision()) {
						getApplicationData().getProcessData("DeltaX").setValue(
								0);
						getApplicationData().getProcessData("DeltaY").setValue(
								0);
					}

					System.out
							.println("riga 679 verifico lavatrice in posizione");

					do {
						ThreadUtil.milliSleep(1);
					} while (!IOLinea.getLBInPos());

					// Se ho un reset ciclo attivo torno indietro
					if (getApplicationData().getProcessData("ResetCycle")
							.getValue().equals(true)) {
						// Abort del ciclo
						CycleAborted = true;
						getApplicationData().getProcessData("ResetCycle")
								.setValue(false);
						lbr_iiwa_14_R820_1.moveAsync(ptp(
								getApplicationData().getFrame(
										"/PrelievoOR/AllPrel1"))
								.setJointVelocityRel(1.0).setBlendingCart(25));
					} else {
						CorrPos = GetCorrection(getApplicationData().getFrame(
								"/Modello1/DepositoOR"));
						tool2.move(lin(CorrPos).setCartVelocity(100)
								.setJointAccelerationRel(0.4));// .setMode(jointSoftMode));
					}
					break;
				case 2:
					System.out.println("riga 704 Movimenti modello 2");
					// Tir 2/3 vie

					depSequence = 0;
					do {
						switch (depSequence) {
						case 0:
							System.out.println("riga 711 Movimento Avv1");
							tool1.move(ptp(
									getApplicationData().getFrame(
											"/Modello2/Avv1"))
									.setJointVelocityRel(1.0));
							if (getApplicationData()
									.getProcessData("ResetCycle").getValue()
									.equals(true)) {
								depSequence = 100;
							} else {
								depSequence = 1;
							}
							break;
						case 1:
							System.out.println("riga 725 Movimento Avv2");
							tool1.move(ptp(
									getApplicationData().getFrame(
											"/Modello2/Avv2"))
									.setJointVelocityRel(1.0));
							if (getApplicationData()
									.getProcessData("ResetCycle").getValue()
									.equals(true)) {
								depSequence = 0;
							} else {
								depSequence = 2;
							}
							break;
						case 2:
							System.out.println("riga 739 Movimento Avv3");
							tool1.move(lin(
									getApplicationData().getFrame(
											"/Modello2/Avv3")).setCartVelocity(
									400));
							if (getApplicationData()
									.getProcessData("ResetCycle").getValue()
									.equals(true)) {
								depSequence = 1;
							} else {
								depSequence = 100;
							}
							break;
						}
					} while (depSequence != 100);

					if (getApplicationData().getProcessData("ResetCycle")
							.getValue().equals(false)) {
						// Verifico se ho la telecamera inserita o no
						if (ethercatModule.getNoVision()) {
							// Attendo l'allineamento da telecamera
							do {
								ThreadUtil.milliSleep(1);
								delaywait++;
							} while (getApplicationData()
									.getProcessData("ResAlignment").getValue()
									.equals(false)
									& (delaywait < 4000));
						}
					}
					
					// Se non mi arriva entro 5 secondi vado in posizione 0
					if ((delaywait >= 4000) | !ethercatModule.getNoVision()) {
						getApplicationData().getProcessData("DeltaX").setValue(
								0);
						getApplicationData().getProcessData("DeltaY").setValue(
								0);
					}

					System.out
							.println("riga 779 verifico lavatrice in posizione");

					do {
						ThreadUtil.milliSleep(1);
					} while (!IOLinea.getLBInPos());

					if (getApplicationData().getProcessData("ResetCycle")
							.getValue().equals(true)) {
						// Abort del ciclo
						CycleAborted = true;
						getApplicationData().getProcessData("ResetCycle")
								.setValue(false);
						lbr_iiwa_14_R820_1.moveAsync(ptp(
								getApplicationData().getFrame(
										"/PrelievoOR/AllPrel1"))
								.setJointVelocityRel(1.0).setBlendingCart(25));
					} else {
						CorrPos = GetCorrection(getApplicationData().getFrame(
								"/Modello2/DepositoOR"));
						tool1.move(lin(CorrPos).setCartVelocity(100)
								.setJointAccelerationRel(0.4));// .setMode(jointSoftMode));
					}

					break;
				case 4:
					System.out.println("riga 804 Movimenti modello 4");
					// P one

					depSequence = 0;
					do {
						switch (depSequence) {
						case 0:
							System.out.println("riga 811 Movimento Avv1");
							tool2.move(ptp(
									getApplicationData().getFrame(
											"/Modello4/Avv1"))
									.setJointVelocityRel(1.0));
							if (getApplicationData()
									.getProcessData("ResetCycle").getValue()
									.equals(true)) {
								depSequence = 100;
								System.out.println("reset concluso");
							} else {
								depSequence = 1;
							}
							break;
						case 1:
							System.out.println("riga 826 Movimento Avv2");
							tool2.move(ptp(
									getApplicationData().getFrame(
											"/Modello4/Avv2"))
									.setJointVelocityRel(1.0));
							if (getApplicationData()
									.getProcessData("ResetCycle").getValue()
									.equals(true)) {
								System.out.println("torno da movimento 2 a 1");
								depSequence = 0;
							} else {
								depSequence = 2;
							}
							break;
						case 2:
							System.out.println("riga 841 Movimento Avv3");
							tool2.move(lin(
									getApplicationData().getFrame(
											"/Modello4/Avv3")).setCartVelocity(
									400));
							if (getApplicationData()
									.getProcessData("ResetCycle").getValue()
									.equals(true)) {
								depSequence = 1;
								System.out.println("torno da movimento 3 a 2");
							} else {
								depSequence = 100;
							}
							break;
						}
					} while (depSequence != 100);

					if (getApplicationData().getProcessData("ResetCycle")
							.getValue().equals(false)) {
						// Verifico se ho la telecamera inserita o no
						if (ethercatModule.getNoVision()) {
							// Attendo l'allineamento da telecamera
							do {
								ThreadUtil.milliSleep(1);
								delaywait++;
							} while (getApplicationData()
									.getProcessData("ResAlignment").getValue()
									.equals(false)
									& (delaywait < 4000));
						}
					}
					
					// Se non mi arriva entro 5 secondi vado in posizione 0
					if ((delaywait >= 4000) | !ethercatModule.getNoVision()) {
						getApplicationData().getProcessData("DeltaX").setValue(
								0);
						getApplicationData().getProcessData("DeltaY").setValue(
								0);
					}

					System.out
							.println("riga 882 verifico lavatrice in posizione");

					do {
						ThreadUtil.milliSleep(1);
					} while (!IOLinea.getLBInPos());

					CorrPos = GetCorrection(getApplicationData().getFrame(
							"/Modello4/DepositoOR"));

					if (getApplicationData().getProcessData("ResetCycle")
							.getValue().equals(true)) {
						// Abort del ciclo
						CycleAborted = true;
						getApplicationData().getProcessData("ResetCycle")
								.setValue(false);
						lbr_iiwa_14_R820_1.moveAsync(ptp(
								getApplicationData().getFrame(
										"/PrelievoOR/AllPrel1"))
								.setJointVelocityRel(1.0).setBlendingCart(25));
					} else {
						CorrPos = GetCorrection(getApplicationData().getFrame(
								"/Modello4/DepositoOR"));
						tool2.move(lin(CorrPos).setCartVelocity(100)
								.setJointAccelerationRel(0.4));// .setMode(jointSoftMode));
					}
					break;
				case 10:
					System.out.println("riga 909 Movimenti modello 10");
					// P 10 3 vie

					depSequence = 0;
					do {
						switch (depSequence) {
						case 0:
							System.out.println("riga 916 Movimento Avv1");
							tool2.move(ptp(
									getApplicationData().getFrame(
											"/Modello10/Avv1"))
									.setJointVelocityRel(1.0));
							if (getApplicationData()
									.getProcessData("ResetCycle").getValue()
									.equals(true)) {
								depSequence = 100;
							} else {
								depSequence = 1;
							}
							break;
						case 1:
							System.out.println("riga 930 Movimento Avv2");
							tool2.move(ptp(
									getApplicationData().getFrame(
											"/Modello10/Avv2"))
									.setJointVelocityRel(1.0));
							if (getApplicationData()
									.getProcessData("ResetCycle").getValue()
									.equals(true)) {
								depSequence = 0;
							} else {
								depSequence = 2;
							}
							break;
						case 2:
							System.out.println("riga 944 Movimento Avv3");
							tool2.move(lin(
									getApplicationData().getFrame(
											"/Modello10/Avv3"))
									.setCartVelocity(400));
							if (getApplicationData()
									.getProcessData("ResetCycle").getValue()
									.equals(true)) {
								depSequence = 1;
							} else {
								depSequence = 100;
							}
							break;
						}
					} while (depSequence != 100);

					if (getApplicationData().getProcessData("ResetCycle")
							.getValue().equals(false)) {
						// Verifico se ho la telecamera inserita o no
						if (ethercatModule.getNoVision()) {
							// Attendo l'allineamento da telecamera
							do {
								ThreadUtil.milliSleep(1);
								delaywait++;
							} while (getApplicationData()
									.getProcessData("ResAlignment").getValue()
									.equals(false)
									& (delaywait < 4000));
						}
					}
					
					// Se non mi arriva entro 5 secondi vado in posizione 0
					if ((delaywait >= 4000) | !ethercatModule.getNoVision()) {
						getApplicationData().getProcessData("DeltaX").setValue(
								0);
						getApplicationData().getProcessData("DeltaY").setValue(
								0);
					}

					System.out
							.println("riga 984 verifico lavatrice in posizione");

					do {
						ThreadUtil.milliSleep(1);
					} while (!IOLinea.getLBInPos());

					CorrPos = GetCorrection(getApplicationData().getFrame(
							"/Modello10/DepositoOR"));

					if (getApplicationData().getProcessData("ResetCycle")
							.getValue().equals(true)) {
						// Abort del ciclo
						CycleAborted = true;
						getApplicationData().getProcessData("ResetCycle")
								.setValue(false);
						lbr_iiwa_14_R820_1.moveAsync(ptp(
								getApplicationData().getFrame(
										"/PrelievoOR/AllPrel1"))
								.setJointVelocityRel(1.0).setBlendingCart(25));
					} else {
						CorrPos = GetCorrection(getApplicationData().getFrame(
								"/Modello10/DepositoOR"));
						tool2.move(lin(CorrPos).setCartVelocity(100)
								.setJointAccelerationRel(0.4));// .setMode(jointSoftMode));
					}
					break;
				}

				if (!CycleAborted) {
					ThreadUtil.milliSleep(1);
					System.out.println("riga 1014 Posizione rilascio graffette");
					ethercatModule.setLedStartRobot(true);

					Boolean Restart = false;

					do {
						ThreadUtil.milliSleep(1);
					} while (getApplicationData().getProcessData("BtnAction")
							.getValue().equals(false)
							& !IOLinea.getRobotEscluso()
							& getApplicationData().getProcessData("ResetCycle")
									.getValue().equals(false));

					System.out
							.println("riga 1028 Verifico non ci sia richiesta di reset");

					if (getApplicationData().getProcessData("ResetCycle")
							.getValue().equals(true)) {
						// Abort del ciclo
						CycleAborted = true;
						getApplicationData().getProcessData("ResetCycle")
								.setValue(false);
					}

					// Spengo i led dei pulsanti
					ethercatModule.setLedStartRobot(false);
					UseCamera = ethercatModule.getNoVision();

					System.out.println("riga 1042 Apro le pinze");

					OpenGripper();

					getApplicationData().getProcessData("ResetCycle").setValue(
							false);

					if (!CycleAborted) {
						System.out.println("riga 1050 Ciclo non resettato");
						if (UseCamera) {
							System.out
									.println("riga 1053 Visione inserita: mi allontano dal deposito");
							DoPhoto = true;
							tool2.move(linRel(-70, 0, 0).setCartVelocity(300));
						} else {
							System.out
									.println("riga 1058 Visione non inserita: mi allontano dal deposito");
							tool2.move(linRel(-70, 0, 0).setCartVelocity(300)
									.triggerWhen(pathCondition, SetFineCiclo));
						}
					} else {
						System.out.println("riga 1063 Ciclo resettato");
						tool2.move(linRel(-70, 0, 0).setCartVelocity(300));
					}
					enableCheckCollision = true;

					System.out.println("riga 1068 Movimento AllDep1");

					lbr_iiwa_14_R820_1.move(lin(
							getApplicationData()
									.getFrame("/DepositoOR/AllDep1"))
							.setCartVelocity(800));

					if (!UseCamera & !CycleAborted) {
						IOLinea.setFineCiclo(true);
					}
				}
			} else {
				System.out.println("riga 1080 Torno in home");
				FromDepORTOHome();
			}
		} else {
			FromDepORTOHome();
		}
	}

	@Override
	public void run() {
		System.out.println("riga 1090 Avvio server");
		getApplicationData().getProcessData("ServerState").setValue(0);
		getApplicationData().getProcessData("actForce").setValue(0);

		enableCheckCollision = false;
		// normalObserver.enable();

		ethercatModule.setLedStartRobot(false);

		CycleAborted = false;

		OpenGripper();

		System.out.println("riga 1103 RESET CICLO");
		Frame posAct = lbr_iiwa_14_R820_1
				.getCurrentCartesianPosition(lbr_iiwa_14_R820_1.getFlange());
		lbr_iiwa_14_R820_1.move(ptp(posAct).setJointVelocityRel(0.1));

		enableCheckCollision = true;

		// Verifico la posizione del robot per eventuali movimenti sicuri verso
		// home
		if (posAct.getZ() < 400) {
			posAct.setX(600);
			posAct.setY(-100);
			posAct.setZ(420);
			lbr_iiwa_14_R820_1.move(lin(posAct).setCartVelocity(50));
		}

		if (posAct.getZ() < 700) {
			posAct.setZ(420);
			lbr_iiwa_14_R820_1.move(ptp(
					getApplicationData().getFrame("/HomeElectr"))
					.setJointVelocityRel(0.25));
			lbr_iiwa_14_R820_1.moveAsync(ptp(
					getApplicationData().getFrame("/ToBrakeTest/Pos1"))
					.setJointVelocityRel(0.4).setBlendingCart(50));
			lbr_iiwa_14_R820_1.moveAsync(ptp(
					getApplicationData().getFrame("/ToBrakeTest/Pos2"))
					.setJointVelocityRel(0.4).setBlendingCart(50));
		}

		// IOLinea.SetFuoriIngombro(true);
		lbr_iiwa_14_R820_1.move(ptpHome().setJointVelocityRel(0.4));

		Boolean gms = lbr_iiwa_14_R820_1.getSafetyState()
				.areAllAxesGMSReferenced();
		Boolean posref = lbr_iiwa_14_R820_1.getSafetyState()
				.areAllAxesPositionReferenced();

		// Verifico se il robot necessita del referencing
		if (!gms | !posref) {
			PosReferencing();
		}

		// Ritorno in home
		lbr_iiwa_14_R820_1.moveAsync(ptp(
				getApplicationData().getFrame("/ToBrakeTest/Pos2"))
				.setJointVelocityRel(0.4).setBlendingCart(50));

		lbr_iiwa_14_R820_1.moveAsync(ptp(
				getApplicationData().getFrame("/ToBrakeTest/Pos1"))
				.setJointVelocityRel(0.4).setBlendingCart(50));

		lbr_iiwa_14_R820_1.moveAsync(ptp(
				getApplicationData().getFrame("/HomeElectr"))
				.setJointVelocityRel(0.4).setBlendingCart(50));

		// Set movimento soft
		CartesianImpedanceControlMode impedanceControlMode = new CartesianImpedanceControlMode();
		// impedanceControlMode.parametrize(CartDOF.X).setStiffness(stiffnessX);
		// impedanceControlMode.parametrize(CartDOF.Y).setStiffness(stiffnessY);
		// impedanceControlMode.parametrize(CartDOF.Z).setStiffness(stiffnessZ);
		// impedanceControlMode.parametrize(CartDOF.ALL).setDamping(0.7);
		impedanceControlMode.parametrize(CartDOF.A).setStiffness(200);

		// JointImpedanceControlMode jointSoftMode = new
		// JointImpedanceControlMode(100.0, 100.0, 100.0, 100.0, 100.0, 100.0,
		// 2000.0);
		// jointSoftMode.setStiffness(3000.0, 3000.0, 3000.0, 3000.0, 3000.0,
		// 3000.0, 200.0);

		DoPhoto = false;

		getObserverManager().waitFor(RobotInclusoCond);

		while (true) {
			// Se ho il robot escluso non faccio niente
			if (!IOLinea.getRobotEscluso()) {
				ProgPezzo();
			}
		}
	}

	// Verifica della corrispondenza pinza-codice pezzo
	private boolean CheckPinza() {
		switch (CodiceModello) {
		case 1:
			if (IOPinza.getInputX3Pin13()) {
				return true;
			} else {
				System.out.println("riga 1191 Pinza errata");
				return false;
			}
		case 2:
			if (!IOPinza.getInputX3Pin13()) {
				return true;
			} else {
				System.out.println("riga 1198 Pinza errata");
				return false;
			}
		case 4:
			if (IOPinza.getInputX3Pin13()) {
				return true;
			} else {
				System.out.println("riga 1205 Pinza errata");
				return false;
			}
		case 10:
			if (IOPinza.getInputX3Pin13()) {
				return true;
			} else {
				System.out.println("riga 1212 Pinza errata");
				return false;
			}
		default:
			System.out.println("riga 1216 Pinza errata");
			return false;
		}
	}

	public void PosReferencing() {
		PositionMastering mastering = new PositionMastering(lbr_iiwa_14_R820_1);

		boolean allAxesMastered = true;
		for (int i = 0; i < axisId.length; ++i) {
			// Check if the axis is mastered - if not, no referencing is
			// possible
			boolean isMastered = mastering.isAxisMastered(axisId[i]);
			if (!isMastered) {
				getLogger()
						.warn("Axis with axisId "
								+ axisId[i]
								+ " is not mastered, therefore it cannot be referenced");
			}

			allAxesMastered &= isMastered;
		}

		// We can move faster, if operation mode is T1
		if (OperationMode.T1 == lbr_iiwa_14_R820_1.getOperationMode()) {
			joggingVelocity = 0.4;
		}

		if (allAxesMastered) {
			getLogger().info(
					"Perform position and GMS referencing with 5 positions");

			// Move to home position
			getLogger().info("Moving to home position");
			lbr_iiwa_14_R820_1.move(ptpHome().setJointVelocityRel(
					joggingVelocity));

			// In this example 5 positions are defined, though each one
			// will be reached from negative and from positive axis
			// direction resulting 10 measurements. The safety needs
			// exactly 10 measurements to perform the referencing.
			performMotion(new JointPosition(Math.toRadians(0.0),
					Math.toRadians(16.18), Math.toRadians(23.04),
					Math.toRadians(37.35), Math.toRadians(-67.93),
					Math.toRadians(38.14), Math.toRadians(-2.13)));

			performMotion(new JointPosition(Math.toRadians(18.51),
					Math.toRadians(9.08), Math.toRadians(-1.90),
					Math.toRadians(49.58), Math.toRadians(-2.92),
					Math.toRadians(18.60), Math.toRadians(-31.18)));

			performMotion(new JointPosition(Math.toRadians(-18.53),
					Math.toRadians(-25.76), Math.toRadians(-47.03),
					Math.toRadians(-49.55), Math.toRadians(30.76),
					Math.toRadians(-30.73), Math.toRadians(20.11)));

			performMotion(new JointPosition(Math.toRadians(-65.66),
					Math.toRadians(35.0), Math.toRadians(-11.52),
					Math.toRadians(10.48), Math.toRadians(-11.38),
					Math.toRadians(-20.70), Math.toRadians(-40.0)));

			performMotion(new JointPosition(Math.toRadians(-90.0),
					Math.toRadians(-19.00), Math.toRadians(24.72),
					Math.toRadians(-82.04), Math.toRadians(14.65),
					Math.toRadians(-29.95), Math.toRadians(1.57)));

			// Move to home position at the end
			getLogger().info("Moving to home position");
			lbr_iiwa_14_R820_1.move(ptpHome().setJointVelocityRel(
					joggingVelocity));
		}
	}

	private void performMotion(JointPosition position) {
		getLogger().info("Moving to position #" + (++positionCounter));

		PTP mainMotion = new PTP(position).setJointVelocityRel(joggingVelocity);
		lbr_iiwa_14_R820_1.move(mainMotion);

		getLogger().info("Moving to current position from negative direction");
		JointPosition position1 = new JointPosition(
				lbr_iiwa_14_R820_1.getJointCount());
		for (int i = 0; i < lbr_iiwa_14_R820_1.getJointCount(); ++i) {
			position1.set(i, position.get(i) - sideOffset);
		}
		PTP motion1 = new PTP(position1).setJointVelocityRel(joggingVelocity);
		lbr_iiwa_14_R820_1.move(motion1);
		lbr_iiwa_14_R820_1.move(mainMotion);

		// Wait a little to reduce robot vibration after stop.
		ThreadUtil.milliSleep(2500);

		// Send the command to safety to trigger the measurement
		sendSafetyCommand();

		getLogger().info("Moving to current position from positive direction");
		JointPosition position2 = new JointPosition(
				lbr_iiwa_14_R820_1.getJointCount());
		for (int i = 0; i < lbr_iiwa_14_R820_1.getJointCount(); ++i) {
			position2.set(i, position.get(i) + sideOffset);
		}
		PTP motion2 = new PTP(position2).setJointVelocityRel(joggingVelocity);
		lbr_iiwa_14_R820_1.move(motion2);
		lbr_iiwa_14_R820_1.move(mainMotion);

		// Wait a little to reduce robot vibration after stop
		ThreadUtil.milliSleep(2500);

		// Send the command to safety to trigger the measurement
		sendSafetyCommand();
	}

	private void sendSafetyCommand() {
		ISunriseRequestService requestService = (ISunriseRequestService) (kuka_Sunrise_Cabinet_1
				.getRequestService());
		SSR ssr = SSRFactory.createSafetyCommandSSR(GMS_REFERENCING_COMMAND);
		Message response = requestService.sendSynchronousSSR(ssr);
		int result = response.getParamInt(0);
		if (COMMAND_SUCCESSFUL != result) {
			getLogger().warn(
					"Command did not execute successfully, response = "
							+ result);
		}
	}

	/**
	 * Auto-generated method stub. Do not modify the contents of this method.
	 */
	// public static void main(String[] args)
	// {
	// Electrolux app = new Electrolux();
	// Server = new KrcServer();
	// KrcServer.main(args);
	// app.runApplication();
	// }

}
