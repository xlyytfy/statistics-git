package com.rue.stat.ext;

import org.apache.log4j.Logger;

import com.cdoframework.cdolib.base.Return;
import com.cdoframework.cdolib.base.Utility;
import com.cdoframework.cdolib.data.cdo.CDO;
import com.rue.stat.business.BusinessService;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


/**
 * @author zhangjinshun
 * 
 */
public class JedisClient 
{

	//静态对象,所有static在此声明并初始化------------------------------------------------------------------------

	//内部对象,所有在本类中创建并使用的对象在此声明--------------------------------------------------------------
	//private JedisCluster client;
	
	//属性对象,所有在本类中创建，并允许外部访问的对象在此声明并提供get/set方法-----------------------------------

	//引用对象,所有在外部创建并传入使用的对象在此声明并提供set方法-----------------------------------------------

	//内部方法,所有仅在本类或派生类中使用的函数在此定义为protected方法-------------------------------------------

	//私有方法 所有仅在本类或派生类中使用的函数在此定义为private方法-------------------------------------------

	//公共方法,所有可提供外部使用的函数在此定义为public方法------------------------------------------------------
	private Logger logger=Logger.getLogger(this.getClass());
    private JedisPool pool = null;
    
	private static JedisClient client=new JedisClient();
	
	public static JedisClient getInstance()
	{
		return client;
	}
	
	public Return start()
	{
		String redisURI=BusinessService.getInstance().getParameter("RedisURI");
		String redisPassWord=BusinessService.getInstance().getParameter("RedisPassWord");
		
		Return ret = this.open(redisURI,10*1000,redisPassWord);
		if(ret==null||ret.getCode()!=0)
		{
			logger.error("jedisClient start failed !!!" +ret);
			return ret;
		}
		
		return Return.OK;
	}
	
    /**
     * 开启
     * @param strHost
     * @param connectionTimeout
     * @param password
     * @return
     */
	public Return open(String strHost,int connectionTimeout,String password)
	{
		String[] strHosts=Utility.splitString(strHost,':');
		
		// Jedis连接池配置
		JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        // 最大空闲连接数, 默认8个
        jedisPoolConfig.setMaxIdle(10);
        // 最大连接数, 默认8个
        jedisPoolConfig.setMaxTotal(30);
        //最小空闲连接数, 默认0
        jedisPoolConfig.setMinIdle(1);
        // 获取连接时的最大等待毫秒数(如果设置为阻塞时BlockWhenExhausted),如果超时就抛异常, 小于零:阻塞不确定的时间,  默认-1
        jedisPoolConfig.setMaxWaitMillis(3000); // 设置6秒
        //对拿到的connection进行validateObject校验
        jedisPoolConfig.setTestOnBorrow(false);
        
		Return ret=Return.OK;
		try
		{
			 pool = new JedisPool(jedisPoolConfig, strHosts[0],Integer.parseInt(strHosts[1]), connectionTimeout, password);
			/* public JedisPool(GenericObjectPoolConfig poolConfig, String host, int port, int timeout, String password) {
					this(poolConfig, host, port, timeout, password, 0, (String) null);
				}*/
			logger.info("Jedis connect to redis server "+strHost);
		}
		catch(Exception e)
		{
			logger.error("Can not connect to"+strHost,e);

			ret=Return.valueOf(-1,"Can not connect to "+strHosts,e.getMessage());
		}

		return ret;
	}
	

   
    /**
     * 关闭
     */
	public void close()
	{
   		try 
   		{
			this.pool.close();
		}
   		catch (Exception e)
   		{
			e.printStackTrace();
		}
   		this.pool=null;
	}
	/**
     * 关闭
     */
	public void stop()
	{
		close();
	}
	/**
	 * obj必需可以序列化成String
	 */
    public boolean putCacheValue(String strCacheKey,Object obj)
    {
    	String strObject=null;
    	if(obj instanceof CDO)
    	{
    		strObject=((CDO)obj).toXML();
    	}
    	else
    	{
    		strObject=obj.toString();
    	}
		try
		{
			Jedis jedis=pool.getResource();
			jedis.set(strCacheKey,strObject);
			jedis.close();
		}
		catch(Exception e)
		{
			return false;
		}
		
		return true;
    }
	/**
	 * 添加值
	 * @param strCacheKey
	 * @param obj
	 * @param second
	 * @return
	 */
    public boolean putCacheValue(String strCacheKey,Object obj,int second)
    {
    	String strObject=null;
    	if(obj instanceof CDO)
    	{
    		strObject=((CDO)obj).toXML();
    	}
    	else
    	{
    		strObject=obj.toString();
    	}
    	try
    	{
    		Jedis jedis=pool.getResource();
    		jedis.set(strCacheKey,strObject);
    		Long result = jedis.expire(strCacheKey, second);
    		jedis.close();
    	}
    	catch(Exception e)
    	{
    		return false;
    	}
    	
    	return true;
    }
    /**
     * 获取值
     * @param strCacheKey
     * @return
     */
	public Object getCacheValue(String strCacheKey)
	{
    	if(strCacheKey==null)
    	{
    		return null;
    	}
    	Jedis jedis=pool.getResource();
		String strObject = jedis.get(strCacheKey);
		jedis.close();
		if(strObject!=null&&strObject.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?><CDO>")==true)
		{
			return CDO.fromXML(strObject);
		}
		
    	return strObject;
	}
	/**
	 * 删除值
	 * @param strCacheKey
	 * @return
	 */
	public boolean deleteCacheValue(String strCacheKey)
	{
		try
		{
			Jedis jedis=pool.getResource();
			jedis.del(strCacheKey);
			jedis.close();
			
		}
		catch(Exception e)
		{
			return false;
		}
		return true;
	}
	
	/**
	 * 添加到队列尾部
	 * @param strKey
	 * @param strTask
	 * @return
	 */
	public long addTaskAtTail(String strKey,String strTask)
	{
		try
		{
			Jedis jedis=pool.getResource();
			long lReturn=jedis.rpush(strKey,strTask);
			jedis.close();
			return lReturn;
		}
		catch(Exception e)
		{
			logger.error("addTaskAtTail failed to "+strKey+", "+strTask+": "+e.getMessage(),e);
			return -1;
		}
	}

	/**
	 * 添加到队列头部
	 * @param strKey
	 * @param strTask
	 * @return
	 */
	public long addTaskAtHead(String strKey,String strTask)
	{
		try
		{
			Jedis jedis=pool.getResource();
			long lReturn=jedis.lpush(strKey,strTask);
			jedis.close();
			return lReturn;
		}
		catch(Exception e)
		{
			logger.error("addTaskAtHead failed to "+strKey+", "+strTask+": "+e.getMessage(),e);
			return -1;
		}
	}
	/**
	 * 从头部移除
	 * @param strKey
	 * @return
	 */
	public String removeTaskFromHead(String strKey)
	{
		Jedis jedis=pool.getResource();
		String strResult=jedis.lpop(strKey);
		jedis.close();
		return strResult;
	}
	/**
	 * 从尾部移除
	 * @param strKey
	 * @return
	 */
	public String removeTaskFromTail(String strKey)
	{
		Jedis jedis=pool.getResource();
		String strResult=jedis.rpop(strKey);
		jedis.close();
		return strResult;
	}
	/**
	 * 获取队列任务条数
	 * @param strKey
	 * @return
	 */
	public long getTaskCount(String strKey)
	{
		try
		{
			Jedis jedis=pool.getResource();
			long lResult=jedis.llen(strKey);
			jedis.close();
			return lResult;
		}
		catch(Exception e)
		{
			return -1;
		}
	}
	
	public static void main(String[] strsArgs)
	{
		JedisClient jediss=new JedisClient();
		jediss.open("192.168.1.220:6379",3000,"Leyou85861200");
		jediss.deleteCacheValue("$alarmName$");
		/*jediss.putCacheValue("xxx","1231");
		System.out.println(jediss.getCacheValue("xxx"));*/
		
		long taskCount = jediss.getTaskCount("xxx");
		System.out.println(taskCount);
		jediss.close();
		/*
		Jedis jedis = new Jedis("192.168.1.220",6379); // 默认端口
		jedis.auth("yunjisuan"); // 指定密码
	        System.out.println("Connection to server sucessfully");
	// 设置 redis 字符串数据
	        jedis.set("redisTest", "Redis11 1");
	        // 获取存储的数据并输出
	        System.out.println("Stored string in redis:: " + jedis.get("redisTest")); 
	        System.out.println("redis : " + jedis.get("redis")); */
		
	
	}

}
