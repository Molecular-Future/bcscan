package org.brewchain.scan.utils;

import java.util.UUID;

import org.brewchain.scan.data.CallFun;

/**
 * 扫描工具类
 * @author KaminanGTO
 *
 */
public class ScanTools {

	/**
	 * 根据数据hex获取调用方法实体
	 * @param hexData
	 * @return
	 */
	public static CallFun makeCallFun(String hexData)
	{
		CallFun callFun = new CallFun();
		callFun.setFunHex(hexData.substring(0, 8));
		callFun.setParamsHex(hexData.substring(8));
		return callFun;
	}
	
	/**
	 * 获取uuid
	 * @return
	 */
	public static String getUUID() {  

		String uuid = UUID.randomUUID().toString();
		uuid = uuid.replace("-", "");
		return uuid;

	}  
}
