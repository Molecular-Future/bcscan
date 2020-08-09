package org.brewchain.scan.manager;


import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import lombok.Data;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.outils.conf.PropHelper;

/** 
* @ClassName: ScanConfig 
* @Description: 扫块配置
* @author KaminanGTO
* @date Sep 14, 2019 5:48:47 PM 
*  
*/
@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "scan_config")
@Data
public class ScanConfig implements ActorService {

	
	private static PropHelper props = new PropHelper(null);
	
	/** 
	* @Fields slowBlockTime : 低速打块记录时间。打块间隔高于此数值时，将被记录
	*/ 
	public int slowBlockTime = Integer.parseInt(props.get("org.brewchain.scan.slowBlock", "5000"));
	
	
}
