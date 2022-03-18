package EluxAPI;

/*******************************************************************
* <b> STANDARD API CLASS BY mario.musico@electrolux.com </b> <p>
*/

import EluxOEE.*;
import EluxLogger.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;
import com.kuka.roboticsAPI.deviceModel.LBR;

@Singleton
public class xAPI__ELUX extends RoboticsAPIApplication {
	// Standard KUKA API objects
	@Inject private LBR kiwa;
	@Inject private Plc_inputIOGroup 	plcin;
	@Inject private Plc_outputIOGroup 	plcout;
	@Inject private MediaFlangeIOGroup 	mfio;
	
	// Custom modularizing handler objects
	@Inject private xAPI_MF	mf = new xAPI_MF(mfio);
	@Inject private xAPI_Pad pad = new xAPI_Pad(mf);
	@Inject private xAPI_PLC plc = new xAPI_PLC(mf, plcin, plcout);
	@Inject private xAPI_Move move = new xAPI_Move(mf, pad);
	@Inject private xAPI_Compliance comp = new xAPI_Compliance(mf, move);
	@Inject private xAPI_Cobot cobot = new xAPI_Cobot(mf, plc, move, comp);
	@Inject private OEEmgr oee = new OEEmgr();
	@Inject private ProLogger log = new ProLogger();
	

	@Inject	public xAPI__ELUX() { }
	
	@Override public void run() { while (true) { break; } }
	
	// GETTERS ---------------------------------------------------------------------
	
	public LBR getRobot() { return kiwa; }
	public Plc_inputIOGroup getPLCi() { return plcin; }
	public Plc_outputIOGroup getPLCo() { return plcout; }
	public MediaFlangeIOGroup getMFio() { return mfio; }
	
	public xAPI_MF getMF() { return mf; }
	public xAPI_Pad getPad() { return pad; }
	public xAPI_PLC getPLC() { return plc; }
	public xAPI_Move getMove() { return move; }
	public xAPI_Compliance getCompliance() { return comp; }
	public xAPI_Cobot getCobot() { return cobot; }
	public OEEmgr getOEE() { return oee; }
	public ProLogger getLog() { return log; }
}