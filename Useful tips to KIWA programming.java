https://www.objc.io/issues/11-android/dependency-injection-in-java/
https://www.vogella.com/tutorials/DependencyInjection/article.html
https://www.w3schools.com/java/java_arraylist.asp
https://docs.oracle.com/javase/8/docs/api/java/util/ArrayList.html
https://github.com/IFL-CAMP/iiwa_stack/wiki/FAQ
https://www.tutorialspoint.com/java/java_generics.htm

// STACK LIGHT:
RED: Error, Alert
ORANGE/YELLOW: Warning
GREEN: Normal Automatic Operation
BLUE: Action required

// Positioning X,Y,Z,A,B,C == Tx, Ty, Tz, Rz, Ry, Rx
		EULER angles: rotations relative to previous elemental rotation!!

import... // Check "Used libraries & dependencies"

package: program type
	- application >> standard task program type
	- backgroundTask >> cyclic/bkg program type
	
Java annotations:
	- @Inject [all the private methods and properties that must be addedd to the class so the API has the inteded functionality]
	- @Override [redeclaration of existing methods in the parent class that has new definition in the child class]
		>> make the compiler inform you about the overriding status >> easier to debug if it is done incorrectly
	- @Named [variables that have a correspondant application data structure with the specified name]

########################################
# SOME IMPORTANT NOTES
########################################

- handGuiding() motion command belongs to HRCMotions class. 
		To activate the handGuiding mode you must press an enabling button configured by AMF safety file.
		Therefore it is more convenient to use Compliance Mode (CartesianImpedanceControlMode) 
		and parametrize Stiffness and Damping to minimum
- 

########################################
# Definition of standard runtime tasks:
########################################

public class [class name] extends RoboticsAPIApplication {		// Class name = file name
	// All applications are classes derived of RoboticsAPIApplication. They must include overrides of:
	@Override public void initialize() { 
		[init methods and variable assignments]...
	}
	@Override public void run() { }
	@Override public void dispose() { }		// What to do when killing the task after finish or runtime error then call it as super.dispose()
}

########################################
#Definition of background cyclic tasks:
########################################

public class [class name] extends RoboticsAPICyclicBackgroundTask {
	@Override public void initialize() {
		initializeCyclic(	[first cycle start delay],
							[cyclic periodicity of task in ms],
							TimeUnit.MILLISECONDS, 
							CycleBehavior.BestEffort);
		[other init methods and variable assignments]...
	}
	
	@Override public void runCyclic() {
		[all the private methods you want to call during cyclic runtime]...
	}
}
	
Init task call:
[class instance ID].initialize();

End task call:
[class instance ID].cancel();


Labelling at the beginning to poll in SunriseWorkbench IDE for help

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