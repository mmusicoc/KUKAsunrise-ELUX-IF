https://www.robot-forum.com/robotforum/thread/28582-changing-velocity-via-process-data-in-the-background-task/

public class BackgroundTask extends RoboticsAPICyclicBackgroundTask {
private Controller cabinet;
private GripperSkills gripper;
private double ptpVelocity;
private BlueGearEugene bgEugene;



public void initialize() {
cabinet = getController("KUKA_Sunrise_Cabinet_1");
initializeCyclic(0, 500, TimeUnit.MILLISECONDS,
CycleBehavior.BestEffort);
bgEugene = new BlueGearEugene();
gripper = new GripperSkills(cabinet);
keyExample();
}



public void runCyclic() {
ptpVelocity= getApplicationData().getProcessData("ptpVelocity").getValue(); // Gets data from process data
bgEugene.getApplicationControl().setApplicationOverride(ptpVelocity); // Sets this data to main application
}

// Main app
@Override
public void initialize() {
cabinet = getController("KUKA_Sunrise_Cabinet_1");
iiwa = (LBR) getDevice(cabinet, "LBR_iiwa_7_R800_1");
world = World.Current.getRootFrame();
gripper = new GripperSkills(cabinet);
gripperTool = getApplicationData().createFromTemplate("Gripper");
gripperTool.attachTo(iiwa.getFlange());
bgTask = new BackgroundTask();
}


// CORRECTION

@Override
public void run() {
ptpVelocity = bgTask.getPtpVelocity();
iiwa.move(ptpHome().setJointVelocityRel(ptpVelocity));
iiwa.move(ptp(getApplicationData().getFrame("/BlueGear/StartPoint"))
.setJointVelocityRel(0.3));


public class testApp extends RoboticsAPIApplication {
	@Inject
	@Named("ToolTemplate")
	Tool myTool;
	@Override
	public void initialize() {
	}
	@Override
	public void run() {
		double ptpVelocity =  getApplicationData().getProcessData("YOUR PROCESS DATA").getValue();
		myTool.move(ptpHome().setJointVelocityRel(ptpVelocity));
	}
}

// GOOD WAY

public class BackgroundTask extends RoboticsAPICyclicBackgroundTask {
private Controller cabinet;
private GripperSkills gripper;
private static double ptpVelocity;
private BlueGearEugene bgEugene;
public void initialize() {
cabinet = getController("KUKA_Sunrise_Cabinet_1");
initializeCyclic(0, 500, TimeUnit.MILLISECONDS,
CycleBehavior.BestEffort);
bgEugene = new BlueGearEugene();
gripper = new GripperSkills(cabinet);
keyExample();
}



public void runCyclic() {
ptpVelocity= getApplicationData().getProcessData("ptpVelocity").getValue();
}
public static double getPtpVelocity() {
return ptpVelocity;
}

// Substitute bkg with:

public double getPTPVelocity(){
     return getApplicationData().getProcessData("ptpVelocity").getValue();
}

// Main app

public class BlueGearEugene extends RoboticsAPIApplication {
private Controller cabinet;
private LBR iiwa;
private ObjectFrame world;
private boolean status;
private GripperSkills gripper;
private Tool gripperTool;
private BackgroundTask bgTask;



@Override
public void initialize() {
cabinet = getController("KUKA_Sunrise_Cabinet_1");
iiwa = (LBR) getDevice(cabinet, "LBR_iiwa_7_R800_1");
world = World.Current.getRootFrame();
gripper = new GripperSkills(cabinet);
gripperTool = getApplicationData().createFromTemplate("Gripper");
gripperTool.attachTo(iiwa.getFlange());
}



@Override
public void run() {
iiwa.move(ptpHome().setJointVelocityRel(0.2));
iiwa.move(ptp(getApplicationData().getFrame("/BlueGear/StartPoint"))
.setJointVelocityRel(0.3));
getLogger().info("Getting started in 3...");
ThreadUtil.milliSleep(1000);
getLogger().info("Getting started in 2...");
ThreadUtil.milliSleep(1000);
getLogger().info("Getting started in 1...");
ThreadUtil.milliSleep(1000);
getLogger().info("Executing");
dialogWindow();
iiwa.move(ptpHome());
}



public void showGear() {
if (gearStatusCheck()) {
iiwa.move(ptp(getApplicationData().getFrame("/BlueGear/ShowPoint_1"))
.setJointVelocityRel(BackgroundTask.getPtpVelocity()));
iiwa.move(ptp(getApplicationData().getFrame("/BlueGear/ShowPoint_2"))
.setJointVelocityRel(BackgroundTask.getPtpVelocity()));
iiwa.move(ptp(getApplicationData().getFrame("/BlueGear/ShowPoint_3"))
.setJointVelocityRel(BackgroundTask.getPtpVelocity()));
iiwa.move(ptp(getApplicationData().getFrame("/BlueGear/ShowPoint_4"))
.setJointVelocityRel(BackgroundTask.getPtpVelocity()));
ThreadUtil.milliSleep(250);
}
}

