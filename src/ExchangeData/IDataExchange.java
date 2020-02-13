package ExchangeData;

import com.kuka.task.ITaskFunctionMonitor;
import com.kuka.task.properties.TaskFunctionProvider;


public interface IDataExchange extends ITaskFunctionMonitor
{
	public boolean NewScanReq();
	
	public void SetNewScanRequest();
}
