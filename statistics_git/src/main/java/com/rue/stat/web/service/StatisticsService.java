package com.rue.stat.web.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.cdoframework.cdolib.base.Return;
import com.cdoframework.cdolib.data.cdo.CDO;
import com.cdoframework.cdolib.servlet.WebService;
import com.rue.stat.business.BusinessService;

/**
 * 接受logstash收集的日志
 * 
 * @author Administrator
 *
 */
public class StatisticsService extends WebService {

	private final Logger logger = Logger.getLogger(StatisticsService.class);

	public Return statistics(HttpServletRequest request, HttpServletResponse response, CDO cdoRequest,
			CDO cdoResponse) {
		try {
			// 封装参数
			CDO cdoEvent = CDO.newRequest("statistics", "onRecieved");
			String msginfo = cdoRequest.getStringValue("msginfo");

			if (!msginfo.startsWith("{")) {
				logger.error(msginfo + "不符合的信息,非json数据");
				return Return.valueOf(-1, "不符合的信息,非json数据");
			}

			// 设置分发参数
			cdoEvent.setStringValue("statInfo", msginfo);

			// 分发不同类型的埋点日志给监听者
			BusinessService.getInstance().raiseEvent(cdoEvent);

		} catch (Exception e) {
			logger.error("埋点分发到：" + request + "错误：" + e);
		}
		return Return.OK;
	}
}
