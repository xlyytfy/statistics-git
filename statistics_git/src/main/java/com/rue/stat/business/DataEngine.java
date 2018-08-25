package com.rue.stat.business;

import java.sql.Connection;

import org.apache.log4j.Logger;

public class DataEngine extends com.cdoframework.cdolib.database.DruidDataEngine
{
	private Logger log=Logger.getLogger(this.getClass());

	public void onSQLStatement(Connection conn,String strSQL)
	{
		if(log.isInfoEnabled())
		{
			log.info("Connection"+conn.hashCode()+" SQL: "+strSQL);
		}
	}

	public void onException(String strText,Exception e)
	{				
		log.error("Database Exception: "+strText+", "+e,e);
	}

	//构造函数,所有构造函数在此定义------------------------------------------------------------------------------

	public DataEngine()
	{

	}
}
