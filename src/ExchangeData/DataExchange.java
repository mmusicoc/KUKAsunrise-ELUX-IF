package ExchangeData;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.swing.text.StyledEditorKit.ItalicAction;
import com.kuka.roboticsAPI.deviceModel.JointPosition;
import com.kuka.roboticsAPI.geometricModel.Frame;
import com.kuka.task.properties.TaskFunctionProvider;



public class DataExchange implements IDataExchange 
{
	//non toccare niente qui	
	public DataExchange ReturnInterface()
	{
		return this;		
	}

	//aggiungi qui le tue variabili
	public boolean PezzoBuono = false;
	public boolean PezzoScarto = false;
	public boolean newScanRequest = false;
	public boolean scanCompleted = false;
	
	@Override
	public void await() throws InterruptedException {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void await(long timeout, TimeUnit unit) throws InterruptedException,
			TimeoutException {
		// TODO Auto-generated method stub
		
	}
	@Override
	public boolean isAvailable() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean NewScanReq() {
		// TODO Auto-generated method stub
		return newScanRequest;
	}
	@Override
	public void SetNewScanRequest() {
		// TODO Auto-generated method stub
		newScanRequest = true;
	}
	
			
}
