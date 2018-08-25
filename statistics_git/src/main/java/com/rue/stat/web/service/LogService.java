package com.rue.stat.web.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.cdoframework.cdolib.base.Return;
import com.cdoframework.cdolib.data.cdo.CDO;
import com.cdoframework.cdolib.servlet.WebService;

/**
 * 统一埋点收集访问接口com.rue.com.rue.stat.web.service.MtaService.web.service.MtaService
 * @author Administrator
 *
 */
public class LogService extends WebService
{
	//log4j:日志记录
	private Logger logger = Logger.getLogger(LogService.class);
	
	public Return saveLog(HttpServletRequest request,HttpServletResponse response,CDO cdoRequest,CDO cdoResponse)
	{
		try {
			//封装埋点参数
			Map<String, Object> parameterMap = new HashMap<String, Object>();
			String[] fieldIds = cdoRequest.getFieldIds();
			for (String key : fieldIds) {
				parameterMap.put(key, cdoRequest.getObjectValue(key));
			}
			//parameterMap.put("statType","rue_burying_point");													//设置标记
			parameterMap.put("uuid",UUID.randomUUID().toString().replace("-", "").toLowerCase());				//唯一标识：防止重复插入
			
			String jsonParams = JSON.toJSONString(parameterMap);
			logger.info(jsonParams);
			
		} catch (Exception e) {
			logger.error("埋点记录收集请求参数：" + request + "错误信息："+e);
		}
		return Return.OK;
	}
}
