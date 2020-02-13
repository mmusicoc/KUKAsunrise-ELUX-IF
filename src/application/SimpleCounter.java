package application;


import java.util.concurrent.TimeUnit;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;


public class SimpleCounter extends RoboticsAPICyclicBackgroundTask {

	int counter;

	@Override
	public void initialize() {
		initializeCyclic(0, 50, TimeUnit.MILLISECONDS, CycleBehavior.BestEffort);
		counter=0;
	}

	@Override
	public void runCyclic() {
		
		if (counter > 9999) {
			counter=0;
		}
		getApplicationData().getProcessData("counter").setValue(counter++);
	}
}