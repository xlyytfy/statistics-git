package com.rue.stat.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.cdoframework.cdolib.base.Return;
import com.cdoframework.cdolib.base.Utility;
import com.cdoframework.cdolib.data.cdo.CDO;
import com.cdoframework.cdolib.servlet.ICDOServlet;
import com.cdoframework.cdolib.servlet.IPostfixWebService;
import com.cdoframework.cdolib.servlet.IPrefixWebService;
import com.cdoframework.cdolib.servlet.IWebService;
import com.cdoframework.cdolib.servlet.PostfixWebService;
import com.cdoframework.cdolib.servlet.PrefixWebService;
import com.cdoframework.cdolib.servlet.xsd.transfilter.OnRefused;
import com.cdoframework.cdolib.servlet.xsd.transfilter.TransFilter;
import com.cdoframework.cdolib.servlet.xsd.transfilter.TransFilterChoice;
import com.cdoframework.cdolib.servlet.xsd.transfilter.TransFilterChoiceItem;
import com.cdoframework.cdolib.servlet.xsd.transfilter.types.ServicePermissionType;
import com.cdoframework.cdolib.webservicebus.OnTrans;
import com.cdoframework.cdolib.webservicebus.PostfixService;
import com.cdoframework.cdolib.webservicebus.PrefixService;
import com.cdoframework.cdolib.webservicebus.Service;
import com.cdoframework.cdolib.webservicebus.WebServiceBus;
import com.rue.stat.business.BusinessService;
import com.rue.stat.ext.CDOUtility;


/**
 * @author chenbuqiao@qq.com
 */
public class RestServlet extends HttpServlet implements ICDOServlet
{
	private static final long serialVersionUID=5726486225732644501L;

	private class MediaType
	{
		public static final String APPLICATION_JSON="application/json";
	}
	
	private class ServiceConfig
	{
		private Object webService;

		private HashMap<String,String> hmOnTrans;
	}

	private static Logger log=Logger.getLogger(RestServlet.class);

	// 内部对象,所有在本类中创建并使用的对象在此声明--------------------------------------------------------------
	private ArrayList<ServiceConfig> alPrefixWebService;
	private Hashtable<String,IWebService> hmWebService;
	private ArrayList<ServiceConfig> alPostfixWebService;
	private HashMap<String,Integer> hmTransFilter;
	private String strLocalServiceURL;
	private OnRefused onRefused;
	
	public void doGet(HttpServletRequest request,HttpServletResponse response) throws ServletException,IOException
	{
		doPost(request,response);
	}

	public static void main(String[] args)
	{
		String strURI="http://host/api/v1/servicename/transname";
		String[] arrArgs=strURI.split("/");
		String strTransName=arrArgs[arrArgs.length-1];
		String strServiceName=arrArgs[arrArgs.length-2];
		System.out.println(strServiceName+" "+strTransName);
	}
	
	protected boolean checkTrans(HttpServletRequest request,CDO cdoRequest)
	{
		return false;
	}
	
	protected void onTransChecked(HttpServletRequest request,CDO cdoRequest,boolean bAllowed)
	{
		
	}
	
	/**
	 * 1 http://host/api/v1/servicename/transname
	 * 2 get请求放在url参数里；post请求的content，直接传递json字符串 
	 * 3 header Accept:application/json可选 Content-Type: application/json;charset=utf-8 Token:xxxxxxxxxxxxxxx 
	 * 4   服务端返回格式：{"return":{"nCode":0,"strText":"OK","strInfo":"OK"},"response":{"username":"abu"}} 
	 * 5  客户端自行考虑同步异步
	 * 6 httpcode代表处理状态:
	 	400 错误的请求  		该请求是无效的。相应的描述信息会说明原因。
		401 未验证 			没有验证信息或者验证失败 
		403 被拒绝 			理解该请求，但不被接受。相应的描述信息会说明原因。
		404 无法找到 		资源不存在，请求的用户的不存在，请求的格式不被支持。 
		405 Method Not Allowed	该接口不支持该方法的请求。 
		410 已下线 			请求的资源已下线。请参考相关公告。 
		429 过多的请求 		请求超出了频率限制。相应的描述信息会解释具体的原因。 
		500 内部服务错误 		服务器内部出错了。请联系我们尽快解决问题。 
		502 无效的代理 		业务服务器下线了或者正在升级。请稍后重试。 
		503 服务暂时失效 		服务器无法响应请求。请稍后重试。 
		504 代理超时 		服务器在运行，但是无法响应请求。请稍后重试
	 **/
	public void doPost(HttpServletRequest request,HttpServletResponse response) throws ServletException,IOException
	{
		
		try
		{
			CDO cdoRequest=parseRequestData(request);
			cdoRequest.setStringValue("$$strRequestId$$",Utility.getRequestId(request));
			// TransFilter检查
			Return ret=null;
			if(this.hmTransFilter!=null)
			{//需要检查
				ret=checkTrans(request,response,cdoRequest);
				if(ret.getCode()!=0)
				{// 不允许执行，直接返回
					log.warn("CheckTrans failed: "+ret.getText()+", "+cdoRequest.toXML());
					if(this.onRefused!=null)
					{
						ret=Return.valueOf(onRefused.getReturn().getCode(),onRefused.getReturn().getText(),onRefused.getReturn().getInfo());
					}
					this.onTransChecked(request,cdoRequest,false);
					this.outputError(response,403,ret.toString());
					return;
				}
			}
			//无需检查或者允许执行
			ret=Return.OK;
			this.onTransChecked(request,cdoRequest,true);
			
			CDO cdoBody=parseBodyData(request,response);
			cdoRequest.copyFrom(cdoBody,false);
			
			CDO cdoResponse=new CDO();
			ret=this.service(request,response,cdoRequest,cdoResponse);
			if(ret==null)
			{//Trans not supported
				this.outputError(response,400,"Trans not supported: "+cdoRequest.getStringValue("strServiceName")+"."+cdoRequest.getStringValue("strTransName"));
				return;
			}
			else
			{//Correct Response
				CDO cdoOutput=new CDO();
				cdoOutput.setCDOValue("return",ret.toCDO());
				cdoOutput.setCDOValue("response",cdoResponse);
				this.outputData(response,cdoOutput.toJSON());
			}
		}
		catch(MyException400 e)
		{
			this.outputError(response,e);
		}
		catch (MyException405 e)
		{
			this.outputError(response,e);
		}
		catch(MyException500 e)
		{
			log.error("",e);
			this.outputError(response,e);
		}
		catch(Exception e)
		{
			log.error("",e);
			this.outputError(response,500,e.getMessage());
		}
	}
	
	public void init(ServletConfig config)
	{
		// 返回该类的类加载器
		ClassLoader classLoader=this.getClass().getClassLoader();

		// 初始化TransFilter
		String strTransFilterResource=config.getInitParameter("TransFilter.xml");
		if(strTransFilterResource!=null)
		{

			if(log.isInfoEnabled()==true)
			{
				log.info("Load trans filter config file "+strTransFilterResource+" ......");
			}

			this.hmTransFilter=new HashMap<String,Integer>();

			String strEncoding=config.getInitParameter("TransFilter.xml.Encoding");
			if(strEncoding==null)
			{
				strEncoding="utf-8";
			}
			String strXML=Utility.readTextResource(strTransFilterResource,strEncoding,classLoader);
			if(strXML==null)
			{
				throw new RuntimeException("Invalid xml file: "+strTransFilterResource);
			}

			TransFilter transFilter=null;
			try
			{
				transFilter=TransFilter.fromXML(strXML);
			}
			catch(Exception e)
			{
				throw new RuntimeException("Invalid xml file: "+strTransFilterResource,e);
			}

			TransFilterChoice[] choices=transFilter.getTransFilterChoice();
			for(TransFilterChoice choice:choices)
			{
				for(TransFilterChoiceItem item:choice.getTransFilterChoiceItem())
				{
					if(item.getService()!=null)
					{
						int nPermission=item.getService().getPermission().getType();
						String strKey=item.getService().getServiceName().toLowerCase();
						this.hmTransFilter.put(strKey,nPermission);
					}
					else if(item.getTrans()!=null)
					{
						int nPermission=item.getTrans().getPermission().getType();
						String strKey=item.getTrans().getServiceName().toLowerCase()+'.'+item.getTrans().getTransName().toLowerCase();
						this.hmTransFilter.put(strKey,nPermission);
					}
				}
			}
			this.hmTransFilter.put("",transFilter.getDefaultPermission().getType());
			this.strLocalServiceURL=transFilter.getLocalServiceURL();
			this.onRefused=transFilter.getOnRefused();
		}
		
		// 初始化WebService
		String strWebServiceBusConfig=config.getInitParameter("WebServiceBus.xml");
		if(strWebServiceBusConfig!=null)
		{
			String strEncoding=config.getInitParameter("WebServiceBus.xml.Encoding");
			if(strEncoding==null)
			{
				strEncoding="utf-8";
			}
			String strXML=Utility.readTextResource(strWebServiceBusConfig,strEncoding,classLoader);
			if(strXML==null)
			{
				throw new RuntimeException("Load webservicebus xml file failed: "+strWebServiceBusConfig);
			}
			WebServiceBus serviceBus=null;
			try
			{
				serviceBus=WebServiceBus.fromXML(strXML);
			}
			catch(Exception e)
			{
				throw new RuntimeException("Invalid xml file: "+strWebServiceBusConfig,e);
			}

			// 初始化prefixservice
			int nCount=serviceBus.getPrefixServiceCount();
			for(int i=0;i<nCount;i++)
			{
				PrefixService service=serviceBus.getPrefixService(i);
				String strServiceName=service.getName();
				String strClassPath=service.getClassPath();

				IPrefixWebService prefixWebService=null;
				try
				{
					prefixWebService=((IPrefixWebService)Class.forName(strClassPath,true,classLoader).newInstance());
					prefixWebService.setServlet(this);
					prefixWebService.setName(strServiceName);

					ServiceConfig sc=new ServiceConfig();
					sc.webService=prefixWebService;
					OnTrans[] ots=service.getOnTrans();
					if(ots.length>0)
					{
						sc.hmOnTrans=new HashMap<String,String>();
						for(OnTrans onTrans:ots)
						{
							String strKey=onTrans.getServiceName().toLowerCase()+'.'
											+onTrans.getTransName().toLowerCase();
							sc.hmOnTrans.put(strKey,onTrans.getMethodName());
						}
					}
					alPrefixWebService.add(sc);
					if(log.isInfoEnabled()==true)
					{
						log.info("Init web prefixservice "+strServiceName
										+" successfully...............................");
					}
				}
				catch(Exception e)
				{
					throw new RuntimeException("Init web prefixservice "+strServiceName+" failed: "+e.getMessage(),e);
				}
			}

			// 初始化service
			nCount=serviceBus.getServiceCount();
			for(int i=0;i<nCount;i++)
			{
				Service service=serviceBus.getService(i);
				String strServiceName=service.getName();
				String strClassPath=service.getClassPath();

				IWebService webService=null;
				try
				{
					webService=((IWebService)Class.forName(strClassPath,true,classLoader).newInstance());
					webService.setServlet(this);
					webService.setName(strServiceName);
					hmWebService.put(strServiceName.toLowerCase(),webService);
					if(log.isInfoEnabled()==true)
					{
						log.info("Init web service "+strServiceName+" successfully...............................");
					}
				}
				catch(Exception e)
				{
					throw new RuntimeException("Init web service "+strServiceName+" failed: "+e.getMessage(),e);
				}
			}

			// 初始化postfixservice
			nCount=serviceBus.getPostfixServiceCount();
			for(int i=0;i<nCount;i++)
			{
				PostfixService service=serviceBus.getPostfixService(i);
				String strServiceName=service.getName();
				String strClassPath=service.getClassPath();

				IPostfixWebService postfixWebService=null;
				try
				{
					postfixWebService=((IPostfixWebService)Class.forName(strClassPath,true,classLoader).newInstance());
					postfixWebService.setServlet(this);
					postfixWebService.setName(strServiceName);

					ServiceConfig sc=new ServiceConfig();
					sc.webService=postfixWebService;
					OnTrans[] ots=service.getOnTrans();
					if(ots.length>0)
					{
						sc.hmOnTrans=new HashMap<String,String>();
						for(OnTrans onTrans:ots)
						{
							String strKey=onTrans.getServiceName().toLowerCase()+'.'
											+onTrans.getTransName().toLowerCase();
							sc.hmOnTrans.put(strKey,onTrans.getMethodName());
						}
					}
					alPostfixWebService.add(sc);
					if(log.isInfoEnabled()==true)
					{
						log.info("Init web postfixservice "+strServiceName
										+" successfully...............................");
					}
				}
				catch(Exception e)
				{
					throw new RuntimeException("Init web postfixservice "+strServiceName+" failed: "+e.getMessage(),e);
				}
			}
			if(log.isInfoEnabled()==true)
			{
				log.info("Load webservicebus config file "+strWebServiceBusConfig+" successfully");
			}
		}
	}

	
	
	private void outputError(HttpServletResponse response,MyException e) throws ServletException
	{
		this.outputError(response,e.nCode,e.getMessage());
	}
	private void outputError(HttpServletResponse response,int nCode,String strMessage) throws ServletException
	{
		response.setStatus(nCode);
		CDO cdoOutput=new CDO();
		Return error=new Return(nCode,strMessage);
		cdoOutput.setCDOValue("return",error.toCDO());
		this.outputData(response,cdoOutput.toJSON());
	}
	
	private CDO parseRequestData(HttpServletRequest request) throws MyException405 
	{
		String strURI=request.getRequestURI();
		String[] arrArgs=strURI.split("/");
		if(arrArgs.length<2)
		{
			throw new MyException405("can not parse service/trans name from url");
		}
		String strTransName=arrArgs[arrArgs.length-1];
		String strServiceName=arrArgs[arrArgs.length-2];
		CDO cdoRequest=CDO.newRequest(strServiceName,strTransName);

		Enumeration pNames=request.getParameterNames();
		while(pNames.hasMoreElements())
		{
			String name=(String)pNames.nextElement();
			String value=request.getParameter(name);
			cdoRequest.setStringValue(name,value);
		}
		return cdoRequest;
	}

	private CDO parseBodyData(HttpServletRequest request,HttpServletResponse response) throws MyException500, MyException400
	{
		String strData = "";  
		try
		{
			BufferedReader br=new BufferedReader(new InputStreamReader(request.getInputStream(),"utf-8"));
			StringBuffer sb=new StringBuffer("");
			String temp;
			while((temp=br.readLine())!=null)
			{
				sb.append(temp);
			}
			br.close();
			strData=sb.toString();
			String strEncoding=request.getCharacterEncoding();
			strData=java.net.URLDecoder.decode(strData,strEncoding);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new MyException500(e.getMessage());
		}
		
		if(strData.length()>0)
		{
			try
			{
				return CDOUtility.JSON2CDO(strData);
			}
			catch(Exception e)
			{
				throw new MyException400("json format not accepted, "+e.getMessage());
			}
		}
		return new CDO();
	}
		
	public Return service(HttpServletRequest request,HttpServletResponse response,CDO cdoRequest,CDO cdoResponse) throws Exception
	{
		String strServiceName;
		String strTransName;
		Return ret=null;

		try
		{
			strServiceName=cdoRequest.getStringValue("strServiceName");
			strTransName=cdoRequest.getStringValue("strTransName");
		}
		catch(Exception e)
		{
			log.error("Invalid CDO request: "+e.getMessage()+"\n"+cdoRequest,e);
			throw new ServletException("Invalid CDO request from: "+request.getHeader("referer"));
		}

		//处理prefix
		for(ServiceConfig sc : alPrefixWebService)
		{
			PrefixWebService prefixWebService=(PrefixWebService)sc.webService;
			if(sc.hmOnTrans!=null)
			{
				String strKey=strServiceName.toLowerCase()+'.'+strTransName.toLowerCase();
				String strMethodName=sc.hmOnTrans.get(strKey);
				if(log.isDebugEnabled())
				{
					log.debug("PrefixWebService started "+prefixWebService.getName()+':'+strKey+" -> "+strMethodName);
				}
				if(strMethodName!=null)
				{
					Method method=null;
					try
					{
						method=prefixWebService.getClass().getMethod(strMethodName,new Class[]{HttpServletRequest.class,HttpServletResponse.class,CDO.class,CDO.class});
						ret=(Return)method.invoke(sc.webService,request,response,cdoRequest,cdoResponse);
					}
					catch (Exception e)
					{
						log.warn("PrefixWebService "+prefixWebService.getName()+" invoke method failed: "+strMethodName,e);
					}
					if(log.isDebugEnabled())
					{
						log.debug("PrefixWebService handled "+prefixWebService.getName()+':'+strKey+" -> "+strMethodName);
					}
				}
			}
			else
			{
				if(log.isDebugEnabled())
				{
					log.debug("PrefixWebService started "+prefixWebService.getName());
				}
				ret=prefixWebService.handleTrans(request,response,cdoRequest,cdoResponse);
				if(log.isDebugEnabled())
				{
					log.debug("PrefixWebService handled "+prefixWebService.getName());
				}
			}
			if(ret!=null)
			{
				break;
			}
		}
		
		//WebService
		if(ret==null)
		{//prefix未处理
			IWebService webService=this.hmWebService.get(strServiceName.toLowerCase());
			if(webService!=null)
			{// 首先尝试调用webService
				if(log.isDebugEnabled())
				{
					log.debug("WebService started "+webService.getName()+": "+cdoRequest.toXML());
				}
				ret=webService.handleTrans(request,response,cdoRequest,cdoResponse);
				if(log.isDebugEnabled())
				{
					log.debug("WebService handled "+webService.getName()+": "+ret.toString()+" cdoResponse="+cdoResponse.toXML());
				}
			}
		}
		if(ret==null)
		{// 没有WebService，直接调用handleTrans
			try
			{
				ret=handleTrans(request,response,cdoRequest,cdoResponse);
			}
			catch(Exception e)
			{
				log.error("handleTrans "+strServiceName+'.'+strTransName+" error: "+e.getMessage(),e);
				throw e;
			}
		}
		if(ret==null)
		{// 不支持的事务
				return null;
		}
		
		//处理postfix
		for(ServiceConfig sc : alPostfixWebService)
		{
			PostfixWebService postfixWebService=(PostfixWebService)sc.webService;
			if(sc.hmOnTrans!=null)
			{
				String strKey=strServiceName.toLowerCase()+'.'+strTransName.toLowerCase();
				String strMethodName=sc.hmOnTrans.get(strKey);

				log.debug("PostfixWebService started "+postfixWebService.getName()+':'+strKey+" -> "+strMethodName);
				if(strMethodName!=null)
				{
					Method method=null;
					try
					{
						method=postfixWebService.getClass().getMethod(strMethodName,new Class[]{HttpServletRequest.class,HttpServletResponse.class,CDO.class,CDO.class,Return.class});
						method.invoke(postfixWebService,request,response,cdoRequest,cdoResponse,ret);
					}
					catch (Exception e)
					{
						log.warn("PostfixWebService "+postfixWebService.getName()+" invoke method failed: "+strMethodName,e);
					}
					if(log.isDebugEnabled())
					{
						log.debug("PostfixWebService handled "+postfixWebService.getName()+':'+strKey+" -> "+strMethodName);
					}
				}
			}
			else
			{
				if(log.isDebugEnabled())
				{
					log.debug("PostfixWebService started "+postfixWebService.getName()+": "+cdoRequest.toXML());
				}
				try
				{
					postfixWebService.onTransHandled(request,response,cdoRequest,cdoResponse,ret);
					if(log.isDebugEnabled())
					{
						log.debug("PostfixWebService handled "+postfixWebService.getName()+" onTransHandled");
					}
				}
				catch (Exception e)
				{
					log.warn("PostfixWebService failed "+postfixWebService.getName()+" onTransHandled",e);
				}
			}
		}
		return ret;
	}

	public RestServlet()
	{
		super();

		this.alPrefixWebService=new ArrayList<ServiceConfig>();
		this.hmWebService=new Hashtable<String,IWebService>();
		this.alPostfixWebService=new ArrayList<ServiceConfig>();
	}


	@Override
	public Return handleTrans(HttpServletRequest arg0,HttpServletResponse arg1,CDO cdoRequest,CDO cdoResponse)
	{
		return BusinessService.getInstance().handleTrans(cdoRequest,cdoResponse);
	}

	
	private void outputData(HttpServletResponse response,String strData) throws ServletException
	{
		response.setCharacterEncoding("utf-8");
		response.setContentType(MediaType.APPLICATION_JSON);
		byte[] bysOutput=strData.getBytes(Utility.getCharset("utf-8"));
		response.setContentLength(bysOutput.length);
		response.setHeader("Content-Length",""+bysOutput.length);

		OutputStream stream=null;
		try
		{
			stream=response.getOutputStream();
			stream.write(bysOutput);
		}
		catch(Exception e)
		{
			log.error(e.getMessage(),e);
			throw new ServletException(e);
		}
		finally
		{
			Utility.closeStream(stream);
		}
	}
	
	private Return checkTrans(HttpServletRequest request,HttpServletResponse response,CDO cdoRequest)
	{
		Return ret=null;
		
		StringBuffer strbURL=request.getRequestURL();
		if(this.strLocalServiceURL!=null && strLocalServiceURL.length()>0)
		{// 通过本地域名访问
			if(strbURL.toString().startsWith(this.strLocalServiceURL)==true)
			{
				ret=Return.OK;
				return ret;
			}
		}

		String strServiceName=cdoRequest.getStringValue("strServiceName");
		String strTransName=cdoRequest.getStringValue("strTransName");

		// 通过对外域名访问
		String strKey=strServiceName+'.'+strTransName;
		Integer intValue=(Integer)this.hmTransFilter.get(strKey.toLowerCase());
		if(intValue==null)
		{
			intValue=(Integer)this.hmTransFilter.get(strServiceName.toLowerCase());
			if(intValue==null)
			{
				intValue=(Integer)this.hmTransFilter.get("");
			}
		}
		if(intValue==null)
		{
			throw new RuntimeException("Invalid TransFilter config");
		}

		if(intValue.intValue()==ServicePermissionType.REFUSED_TYPE)
		{
			ret=Return.valueOf(-1,"Calling "+strKey+" is not allowed");
			return ret;
		}
		if(intValue.intValue()==ServicePermissionType.NEEDCHECKED_TYPE)
		{
			if(!checkTrans(request,cdoRequest))
			{
				ret=Return.valueOf(-1,"Calling "+strKey+" need logined");
				return ret;
			}
			return Return.OK;
		}
		return Return.OK;
	}
	
	
	abstract class  MyException extends RuntimeException
	{
		public short nCode;
		private static final long serialVersionUID=-2485862101330365402L;
		String message;
		public MyException(String message)
		{
			this.message=message;
		}
		public String getMessage()
		{
			return this.message;
		}
	}
	
	class MyException405 extends MyException
	{
		public MyException405(String message)
		{
			super(message);
			super.nCode=this.nCode;
		}
		private static final long serialVersionUID=-1206722056520486922L;
		public short nCode=405;
	}
	
	class MyException400 extends MyException
	{
		private static final long serialVersionUID=-5011847226205548125L;
		public MyException400(String message)
		{
			super(message);
			super.nCode=this.nCode;
		}
		public short nCode=400;
	}
	
	class MyException500 extends MyException
	{
		private static final long serialVersionUID=-4950720654457742960L;
		public short nCode=500;
		public MyException500(String message)
		{
			super(message);
			super.nCode=this.nCode;
		}
	}
}
