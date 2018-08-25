package com.rue.stat.business.plugin.statService.wxActivity;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cdoframework.cdolib.base.Return;
import com.cdoframework.cdolib.data.cdo.CDO;
import com.cdoframework.cdolib.servicebus.TransService;
import com.rue.stat.ext.CDOUtility;

public class WXActivityService extends TransService {
	private Logger logger = Logger.getLogger(this.getClass());

	// 模板ID
	private String templetId = "WXActivityService";

	/**
	 * 
	 * 描述：保存数据到mongodb</br>
	 * @调用时机： @业务流程：
	 * 
	 * @param cdoRequest
	 * @param cdoResponse
	 * @return
	 */
	public Return save(CDO cdoRequest, CDO cdoResponse) {
		// 获取请求参数转化为CDO
		try {
			String statInfo = cdoRequest.getStringValue("statInfo");
			CDO statInfoCDO = CDOUtility.JSON2CDO(statInfo);

			if (!statInfoCDO.exists("templetId") || StringUtils.isBlank(statInfoCDO.getStringValue("templetId"))) {
				return Return.valueOf(1, "无法获取模板ID");
			}

			if (!this.templetId.equals(statInfoCDO.getStringValue("templetId"))) {
				return Return.valueOf(1, "模板ID不匹配");
			}

			/**
			 * 判断UUID是否已经存在
			 */
			CDO uuidRequest = CDO.newRequest("WXActivityService", "getDataByUuid");
			uuidRequest.setStringValue("uuid", statInfoCDO.getStringValue("uuid"));
			Return uuidRet = this.serviceBus.handleTrans(uuidRequest, cdoResponse);
			if (uuidRet == null || uuidRet.getCode() != 0) {
				return Return.valueOf(-1, "mongodb查询uuid报错");
			}

			if (cdoResponse.exists("cdosOutputText") && cdoResponse.getCDOArrayValue("cdosOutputText").length > 0) {
				return Return.valueOf(-1, "mongodb重复插入");
			}

			CDO mongodbRequest = CDO.newRequest("WXActivityService", "addWXActivity");
			mongodbRequest.setCDOValue("statInfoCDO", statInfoCDO);

			Return ret = this.serviceBus.handleTrans(mongodbRequest, cdoResponse);
			if (ret == null || ret.getCode() != 0) {
				return Return.valueOf(-1, "mongodb获取数据报错");
			}
		} catch (Exception e) {
			logger.error("mongod操作报错" + e);
			return Return.valueOf(-1, "参数:" + cdoRequest + ",保存到mongodb失败：" + e);
		}
		return Return.OK;
	}
}
