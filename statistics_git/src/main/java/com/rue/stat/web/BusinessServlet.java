package com.rue.stat.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.cdoframework.cdolib.base.Return;
import com.cdoframework.cdolib.data.cdo.CDO;
import com.cdoframework.cdolib.servlet.CDOServlet;
import com.rue.stat.business.BusinessService;

public class BusinessServlet extends CDOServlet
{

	@Override
	public Return checkToDo(HttpServletRequest arg0,CDO arg1)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean checkTrans(HttpServletRequest request,CDO cdoRequest)
	{
		return false;
	}

	@Override
	public void handleEvent(CDO cdoEvent)
	{
		BusinessService.getInstance().handleEvent(cdoEvent);

	}

	@Override
	public Return handleTrans(HttpServletRequest request,HttpServletResponse response,CDO cdoRequest,CDO cdoResponse)
	{
		return BusinessService.getInstance().handleTrans(cdoRequest,cdoResponse);
	}

	@Override
	protected void onTransChecked(HttpServletRequest arg0,CDO arg1,boolean arg2)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void raiseEvent(CDO cdoEvent)
	{
		BusinessService.getInstance().handleEvent(cdoEvent);

	}

}
