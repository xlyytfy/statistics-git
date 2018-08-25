package com.rue.stat.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.cdoframework.cdolib.base.Return;
import com.cdoframework.cdolib.cache.LocalCache;
import com.rue.stat.business.BusinessService;

public class ApplicationListener implements ServletContextListener,ServletRequestListener
{
	private Logger log=Logger.getLogger(ApplicationListener.class);

	public void contextInitialized(ServletContextEvent contextEvent)
	{// web start business
		Return ret=Return.OK;
		BusinessService app=BusinessService.getInstance();
		try
		{
			ret=app.start();
			LocalCache.getInstance().start();
		}
		catch(Exception e)
		{
			log.error("StatisticsService  starts failed!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!",e);
			ret=Return.valueOf(-1,e.getLocalizedMessage());
		}
		if(ret.getCode()!=0)
		{
			return;
		}
		if(log.isInfoEnabled())
		{
			log.info("StatisticsService started-------------------------------------------------------------------------------------");
		}
	}

	public void contextDestroyed(ServletContextEvent arg0)
	{
		log.warn("StatisticsService stopping------------------------------------------------------------------------------------");

		BusinessService app=BusinessService.getInstance();
		if(app.isRunning()==false)
		{
			return;
		}
		app.stop();
		LocalCache.getInstance().stop();

		log.warn("StatisticsService stopped-------------------------------------------------------------------------------------");
	}

	public void requestInitialized(ServletRequestEvent arg0)
	{
		HttpServletRequest request=(HttpServletRequest)arg0.getServletRequest();
		String strClientIP=request.getRemoteAddr();
		String strURL=request.getRequestURL().toString();
		if(strURL.endsWith(".cdo")==false)
		{
			return;
		}

		if(request.getQueryString()!=null)
		{
			strURL+="?"+request.getQueryString();
		}
		if(log.isDebugEnabled())
		{
			log.debug(strClientIP+": "+strURL);
		}
	}

	public void requestDestroyed(ServletRequestEvent arg0)
	{
	}

}
