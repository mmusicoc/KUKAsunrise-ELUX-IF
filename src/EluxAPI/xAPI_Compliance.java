package EluxAPI;

/*******************************************************************
* <b> STANDARD API CLASS BY mario.musico@electrolux.com </b> <p>
*/

import static EluxAPI.Utils.waitMillis;
import static com.kuka.roboticsAPI.motionModel.BasicMotions.positionHold;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.deviceModel.LBR;

import com.kuka.roboticsAPI.motionModel.IMotionContainer;
import com.kuka.roboticsAPI.motionModel.PositionHold;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;
import com.kuka.roboticsAPI.geometricModel.*;

@Singleton
public class xAPI_Compliance extends RoboticsAPIApplication {
	@Inject private LBR kiwa;
	@Inject private MediaFlangeIOGroup 	mfio;
	@Inject private xAPI_MF	mf = new xAPI_MF(mfio);
	@Inject private xAPI_Move move;
	@Override public void run() { while (true) { break; } }
	private CartesianImpedanceControlMode softMode, stiffMode;
	private IMotionContainer posHoldMotion;
	private PositionHold posHold;
	private boolean lockDir;

	@Inject	public xAPI_Compliance(xAPI_MF _mf, xAPI_Move _move) {
		this.mf = _mf;
		this.move = _move;
		softMode = new CartesianImpedanceControlMode();
		stiffMode = new CartesianImpedanceControlMode();
		stiffMode.parametrize(CartDOF.TRANSL).setStiffness(5000).setDamping(1);
		stiffMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(1);
		lockDir = false;
	}
		
//	public PositionHold getPosHold() { return posHold; }
	public void posHoldStart() { posHoldMotion = kiwa.moveAsync(posHold); }
	public void posHoldCancel() { posHoldMotion.cancel(); }
	
	public void swapLockDir() {
		this.softMode.parametrize(CartDOF.ALL).setStiffness(0.1).setDamping(1);
		lockDir = !lockDir;
		if (lockDir) {
			this.softMode.parametrize(CartDOF.ROT).setStiffness(300).setDamping(1);
			this.softMode.parametrize(CartDOF.A).setStiffness(0.1).setDamping(1);
		}
		posHold = new PositionHold(softMode, -1, null);
	}
	
	public void waitPushGesture() {
		mf.saveRGB();
		mf.setRGB("B");
		kiwa.move(positionHold(stiffMode, -1, null).breakWhen(move.getJTConds()));
		waitMillis(500);
		mf.resetRGB();
	}
}