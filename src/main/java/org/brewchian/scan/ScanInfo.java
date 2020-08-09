package org.brewchian.scan;

import java.lang.reflect.Field;

import org.apache.felix.ipojo.util.Log;
import org.brewchain.scan.sys.entity.SYSDict;

import lombok.Data;

@Data
public class ScanInfo {
	/**
	 * 上一次整理区块时，校验过的最高的块高度。每次整理时，从最高块开始，截止到该高度为止。
	 */
	@ScanKeyFor("last_connect_block")
	public static String lastConnectCheckBlockHeight = "-1";
	
	@ScanKeyFor("last_connect_block_try")
	public static String lastConnectCheckTry = "0";
	
	@ScanKeyFor("last_connect_block_timestamp")
	public static String lastConnectCheckTimestamp = "";

	public static void parse(SYSDict oSYSDict) {
		Field[] fields = ScanInfo.class.getDeclaredFields();
		for (Field oField : fields) {
			ScanKeyFor oScanKeyFor = oField.getAnnotation(ScanKeyFor.class);
			if (oScanKeyFor!= null) {
				if (oScanKeyFor.value().equals(oSYSDict.getDictKey())) {
					try {
						oField.set(null, oSYSDict.getDictValue());
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}
}
