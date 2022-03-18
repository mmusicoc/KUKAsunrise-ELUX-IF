import static utils.Utils.*;
import utils.FrameWrapper;

import java.net.*;							// For ethernet external communication
import java.io.*;							// For ethernet external communication
import java.util.concurrent.TimeUnit;		// For timers, delays, cycles
import java.util.logging.Logger;			// Message logger for SmarPad

import javax.inject.Inject;					// Java injection API
import javax.inject.Named;					// Same

import com.sun.java.swing.plaf.nimbus.ButtonPainter;
import com.sun.org.apache.bcel.internal.generic.Select;
import com.sun.org.apache.bcel.internal.generic.NEW;

import com.kuka.common.ThreadUtil;			// All
import com.kuka.task.ITaskLogger;			// Logger

// WorkVisual API for I/O signals
import com.kuka.generated.ioAccess.MediaFlangeIOGroup;
import com.kuka.generated.ioAccess.Plc_inputIOGroup;
import com.kuka.generated.ioAccess.Plc_outputIOGroup;

import com.kuka.roboticsAPI.applicationModel.RoboticsAPIApplication;
import com.kuka.roboticsAPI.applicationModel.tasks.CycleBehavior;
import com.kuka.roboticsAPI.applicationModel.tasks.RoboticsAPICyclicBackgroundTask;

import com.kuka.roboticsAPI.controllerModel.Controller;		// All
import com.kuka.roboticsAPI.deviceModel.LBR;				// All
import com.kuka.roboticsAPI.deviceModel.JointEnum;			// Joint recognition
import com.kuka.roboticsAPI.conditionModel.JointTorqueCondition;	// Torque sensing
import com.kuka.roboticsAPI.conditionModel.ICondition;				// Collision detection
import com.kuka.roboticsAPI.executionModel.IFiredConditionInfo;		// Collision triggered action

import com.kuka.roboticsAPI.geometricModel.Frame;			// For frame management (edit,add data)
import com.kuka.roboticsAPI.geometricModel.ObjectFrame;
import com.kuka.roboticsAPI.geometricModel.CartDOF; 
import com.kuka.roboticsAPI.geometricModel.Tool;
import com.kuka.roboticsAPI.geometricModel.Workpiece;

import com.kuka.roboticsAPI.motionModel.*;
import com.kuka.roboticsAPI.motionModel.controlModeModel.CartesianImpedanceControlMode;
import com.kuka.roboticsAPI.motionModel.controlModeModel.HandGuidingControlMode;
import com.kuka.roboticsAPI.motionModel.BasicMotions.*; 	// DO STATIC, for movement PTP, LIN, CIRC
import com.kuka.roboticsAPI.motionModel.HRCMotions.*;		// DO STATIC
import com.kuka.roboticsAPI.motionModel.IMotionContainer;	// Interrupt motion for collision detection


// SmartPad pendant UI configuration
import com.kuka.roboticsAPI.uiModel.ApplicationDialogType;		// Message box / questions
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKey;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyBar;
import com.kuka.roboticsAPI.uiModel.userKeys.IUserKeyListener;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyAlignment;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyEvent;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyLED;
import com.kuka.roboticsAPI.uiModel.userKeys.UserKeyLEDSize;