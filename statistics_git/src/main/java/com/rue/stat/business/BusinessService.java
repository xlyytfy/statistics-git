package com.rue.stat.business;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.cdoframework.cdolib.base.Return;
import com.cdoframework.cdolib.base.Utility;
import com.cdoframework.cdolib.business.App;
import com.cdoframework.cdolib.business.IServiceClient;
import com.cdoframework.cdolib.business.ServiceClient;
import com.cdoframework.cdolib.data.cdo.CDO;
import com.cdoframework.cdolib.servicebus.ServiceBus;

public class BusinessService extends App
{
	private Logger logger=Logger.getLogger(this.getClass());

	final private static BusinessService businessService=new BusinessService();

	private IServiceClient serviceClient;

	private ServiceBus serviceBus;

	private boolean bIsRunning;

	public static BusinessService getInstance()
	{
		return businessService;
	}

	public boolean isRunning()
	{
		return this.bIsRunning;
	}

	public Return raiseTrans(CDO cdoRequest)
	{
		Return ret=null;
		String strKey=cdoRequest.getStringValue("strServiceName")+'.'+cdoRequest.getStringValue("strTransName");

		aiRunningTransCount.incrementAndGet();
		long lStartTime=System.currentTimeMillis();
		try
		{
			ret=serviceClient.raiseTrans(cdoRequest);
		}
		catch(Throwable e)
		{
			logger.error("raiseTrans Exception",e);
		}
		finally
		{
			long lDelay=System.currentTimeMillis()-lStartTime;

			alTotleTransCount.incrementAndGet();
			aiRunningTransCount.decrementAndGet();

			if(ret==null)
			{
				return ret;
			}

			// 统计Trans数据
			TransData transCount=null;
			synchronized(hmActionData)
			{
				transCount=hmActionData.get(strKey);
				if(transCount==null)
				{
					transCount=new TransData();
					hmActionData.put(strKey,transCount);
				}
			}
			if(ret.getCode()==0)
			{
				transCount.alOKCount.incrementAndGet();
				if(lDelay>transCount.lMaxTime)
				{
					transCount.lMaxTime=lDelay;
				}
				transCount.alSumTime.addAndGet(lDelay);
				this.alOKTransCount.incrementAndGet();
			}
			else if(ret.getCode()==-1)
			{
				transCount.alErrorCount.incrementAndGet();
				this.alErrorTransCount.incrementAndGet();
			}
			else
			{
				transCount.alWarnCount.incrementAndGet();
				this.alWarnTransCount.incrementAndGet();
			}
		}
		return ret;
	}

	public Return handleTrans(CDO cdoRequest,CDO cdoResponse)
	{
		Return ret=null;
		String strKey=cdoRequest.getStringValue("strServiceName")+'.'+cdoRequest.getStringValue("strTransName");

		aiRunningTransCount.incrementAndGet();
		long lStartTime=System.currentTimeMillis();
		try
		{
			if(cdoRequest.getStringValue("strTransName").equalsIgnoreCase("getNodeAppData")==true)
			{
				ret=this.getAppData(cdoRequest,cdoResponse);
			}
			else
			{
				ret=serviceClient.handleTrans(cdoRequest,cdoResponse);
			}
		}
		catch(Throwable e)
		{
			logger.error("handleTrans Exception",e);
		}
		finally
		{
			long lDelay=System.currentTimeMillis()-lStartTime;

			alTotleTransCount.incrementAndGet();
			aiRunningTransCount.decrementAndGet();

			if(ret==null)
			{
				return ret;
			}

			// 统计Trans数据
			TransData transCount=null;
			synchronized(hmActionData)
			{
				transCount=hmActionData.get(strKey);
				if(transCount==null)
				{
					transCount=new TransData();
					hmActionData.put(strKey,transCount);
				}
			}
			if(ret.getCode()==0)
			{
				transCount.alOKCount.incrementAndGet();
				if(lDelay>transCount.lMaxTime)
				{
					transCount.lMaxTime=lDelay;
				}
				transCount.alSumTime.addAndGet(lDelay);
				this.alOKTransCount.incrementAndGet();
			}
			else if(ret.getCode()==-1)
			{
				transCount.alErrorCount.incrementAndGet();
				this.alErrorTransCount.incrementAndGet();
			}
			else
			{
				transCount.alWarnCount.incrementAndGet();
				this.alWarnTransCount.incrementAndGet();
			}
		}
		return ret;
	}

	public void handleEvent(CDO cdoEvent)
	{
		this.serviceClient.raiseEvent(cdoEvent);
		this.alTotleEventCount.incrementAndGet();
		String strKey=cdoEvent.getStringValue("strServiceName")+'.'+cdoEvent.getStringValue("strTransName");
		// 统计Trans数据
		TransData transCount=null;
		synchronized(hmActionData)
		{
			transCount=hmActionData.get(strKey);
			if(transCount==null)
			{
				transCount=new TransData();
				hmActionData.put(strKey,transCount);
			}
		}
	}

	public void raiseEvent(CDO cdoEvent)
	{
		serviceClient.raiseEvent(cdoEvent);
		this.alTotleEventCount.incrementAndGet();
		String strKey=cdoEvent.getStringValue("strServiceName")+'.'+cdoEvent.getStringValue("strTransName");
		// 统计Trans数据
		TransData transCount=null;
		synchronized(hmActionData)
		{
			transCount=hmActionData.get(strKey);
			if(transCount==null)
			{
				transCount=new TransData();
				hmActionData.put(strKey,transCount);
			}
		}
	}

	public Return checkToDo(CDO cdoRequest)
	{
		Return ret=this.serviceClient.checkToDo(cdoRequest);
		this.alTotleTodoCount.incrementAndGet();
		String strKey=cdoRequest.getStringValue("strServiceName")+'.'+cdoRequest.getStringValue("strTransName");
		// 统计Trans数据
		TransData transCount=null;
		synchronized(hmActionData)
		{
			transCount=hmActionData.get(strKey);
			if(transCount==null)
			{
				transCount=new TransData();
				hmActionData.put(strKey,transCount);
			}
		}
		return ret;
	}

	public Return start()
	{
		// 读取应用程序参数文件
		Return ret=this.loadConfigFromResource("config.properties",this.getClass().getClassLoader());
		if(ret.getCode()!=0)
		{
			return ret;
		}

		String strBusinessServiceBusXML=Utility.readTextResource("servicebus.xml","utf-8",this.getClass()
						.getClassLoader());
		// serviceBus
		this.serviceBus=new ServiceBus();
		this.serviceClient=new ServiceClient(this.serviceBus,"LocalCache.xml","utf-8",this.getClass().getClassLoader());
		ret=this.serviceBus.init(strBusinessServiceBusXML,this.getClass().getClassLoader());
		if(ret.getCode()!=0)
		{
			return ret;
		}
		// 启动服务总线
		ret=this.serviceBus.start();
		if(ret.getCode()!=0)
		{
			serviceBus.destroy();
			return ret;
		}
		// serviceBus启动成功
		this.serviceClient.start();
		if(ret.getCode()!=0)
		{
			return ret;
		}

		this.bIsRunning=true;
		return Return.OK;
	}

	public void stop()
	{
		this.bIsRunning=false;

		if(this.serviceClient!=null)
		{
			this.serviceClient.stop();
		}

		if(this.serviceBus!=null)
		{
			this.serviceBus.stop();
			this.serviceBus.destroy();
		}
	}

	private BusinessService()
	{
		this.bIsRunning=false;
		this.serviceBus=null;
		this.serviceClient=null;
		this.hmActionData=new HashMap<String,TransData>();
	}

	// 统计数据
	private AtomicLong alTotleTransCount=new AtomicLong(0);

	private AtomicLong alOKTransCount=new AtomicLong(0);

	private AtomicLong alErrorTransCount=new AtomicLong(0);

	private AtomicLong alWarnTransCount=new AtomicLong(0);

	private AtomicInteger aiRunningTransCount=new AtomicInteger(0);

	private AtomicLong alTotleTodoCount=new AtomicLong(0);

	private AtomicLong alTotleEventCount=new AtomicLong(0);

	private long lFreeMemSize=0;

	private long lTotleMemSize=0;

	private long lMaxMemSize=0;

	private long lLastTransCount=0;

	private long lLastTime=0;

	private HashMap<String,TransData> hmActionData;

	private String strIP;

	private String strServerName="";

	private class TransData
	{
		AtomicLong alOKCount=new AtomicLong(0);

		AtomicLong alErrorCount=new AtomicLong(0);

		AtomicLong alWarnCount=new AtomicLong(0);

		long lMaxTime=0;

		AtomicLong alSumTime=new AtomicLong(0);
	}

	public Return getAppData(CDO cdoRequest,CDO cdoResponse)
	{
		if(this.strIP==null)
		{
			try
			{
				strIP=Utility.getIPAddress();
				strServerName=Utility.getHostName();
			}
			catch(Exception e)
			{
			}
		}
		cdoResponse.setStringValue("strIP",strIP);
		cdoResponse.setStringValue("strServerName",strServerName);
		cdoResponse.setLongValue("lTotleMemSize",lTotleMemSize);
		cdoResponse.setLongValue("lFreeMemSize",lFreeMemSize);
		cdoResponse.setLongValue("lMaxMemSize",lMaxMemSize);
		long lTransCount=alTotleTransCount.longValue();
		cdoResponse.setLongValue("lTodoCount",this.alTotleTodoCount.get());
		cdoResponse.setLongValue("lTransCount",lTransCount);
		cdoResponse.setLongValue("lEventCount",this.alTotleEventCount.get());
		long lNow=System.currentTimeMillis();
		long lTPS=(lTransCount-lLastTransCount)*1000/(lNow-lLastTime);
		cdoResponse.setLongValue("lTPS",lTPS);
		lLastTransCount=lTransCount;
		lLastTime=lNow-1;
		cdoResponse.setLongValue("nRunningTransCount",aiRunningTransCount.longValue()-1);

		CDO[] cdosTransDataList=new CDO[hmActionData.size()];
		int i=0;
		Iterator<Entry<String,TransData>> iter=hmActionData.entrySet().iterator();
		while(iter.hasNext()==true)
		{
			Entry<String,TransData> entry=iter.next();
			String strKey=entry.getKey();
			TransData transData=entry.getValue();

			cdosTransDataList[i]=new CDO();
			cdosTransDataList[i].setStringValue("strServiceTransName",strKey);
			long lOKCount=transData.alOKCount.get();
			cdosTransDataList[i].setLongValue("lOKTransCount",lOKCount);
			cdosTransDataList[i].setLongValue("lWarnTransCount",transData.alWarnCount.get());
			cdosTransDataList[i].setLongValue("lErrorTransCount",transData.alErrorCount.get());
			cdosTransDataList[i].setLongValue("lMaxTime",transData.lMaxTime);
			long lAverageTime=0;
			if(lOKCount>0)
			{
				lAverageTime=transData.alSumTime.get()/lOKCount;
			}
			cdosTransDataList[i].setLongValue("lAverageTime",lAverageTime);
			i++;
		}
		cdoResponse.setCDOArrayValue("cdosTransDataList",cdosTransDataList);

		return Return.OK;
	}
}
