public void run() {
		// your application execution starts here
		robot.move(ptpHome());
		ObjectFrame refFrame =  getApplicationData().getFrame("/RefFrame");
		ObjectFrame fPickPoint = getApplicationData().getFrame("/RefFrame/PickPoint");
		// Creating a temporary Frame
		Frame myFrame = fPickPoint.copyWithRedundancy().setZ(fPickPoint.getZ()+200);
		// Create a new ObjectFrame under the ObjectFrame 'RefFrame'.
		// This Frame will be visible in the SWB after application ran once.
		// If you run this application twice you will get an exception, because a Frame with the name 'LiftUpFrame' already exists.
		ObjectFrame fLiftUpPoint = World.Current.addChildFrame("LiftUpFrame", refFrame, myFrame.getTransformationFromParent());
		robot.move(ptp(fLiftUpPoint));
		robot.move(lin(fPickPoint));
		closeGripper();
		robot.move(lin(fLiftUpPoint));
	}
