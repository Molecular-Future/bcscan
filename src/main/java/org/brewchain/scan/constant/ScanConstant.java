package org.brewchain.scan.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * 常量管理
 * @author KaminanGTO
 *
 */
public class ScanConstant {

	/**
	 * 方法名hex映射字典
	 */
	public static final Map<String, String> methodInfo = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;
		{
			// 用户方法
	        put("a8db00d8", ""); // 注册经销商基本信息 参数 bytes
	        put("569389f9", ""); // 新增支付方式 参数 uint8,bytes
	        put("2223e224", ""); // 新增修改支付方式方式 参数 uint8,uint8,bytes
	        put("edeeb49f", ""); // 新增删除支付方式方式 参数 uint8,uint8
	        
	        // 管理方法
	        put("b40a1388", ""); // 设置经销商配置 参数 address,bytes32,bytes
	        put("df8f1b25", ""); // 移除经销商配置 参数 address,bytes32
	        put("dc3237be", ""); // 请求修改管理组成员 参数 address,address
	        put("beda2483", ""); // 确认修改管理组成员 参数 bytes32
	        put("daea85c5", ""); // 经销商审批通过 参数 address
	        put("ab0da5a9", ""); // 经销商审批拒绝 参数 address
	        
	      
	    }
	};
	
}
