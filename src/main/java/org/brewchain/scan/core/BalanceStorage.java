package org.brewchain.scan.core;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.brewchain.core.model.Account.AccountInfo;
import org.brewchain.core.model.Account.AccountInfo.Builder;
import org.brewchain.tools.bytes.BytesHelper;
import org.brewchain.core.model.Account.AccountTokenValue;
import org.brewchain.scan.main.entity.MAINAccountTokenBalance;
import org.brewchain.scan.main.entity.MAINAccountTokenBalanceExample;
import org.brewchian.scan.ScanDaos;

import lombok.extern.slf4j.Slf4j;

/** 
* @ClassName: BalanceStorage 
* @Description: 货币入库
* @author KaminanGTO
* @date Aug 28, 2019 8:28:40 PM 
*  
*/
@Slf4j
public class BalanceStorage implements Runnable {

	ScanDaos daos;
	
	public BalanceStorage(ScanDaos _daos) {
		this.daos = _daos;
	}
	
	/** 
	* @Fields updateAccList : 更新帐号列表，先进先出
	*/ 
	public static List<String> updateAccList = new ArrayList<String>();
	
	/** 
	* @Fields accTokenCaches : 用户token货币缓存，用于加速查询。
	*/ 
	private static Map<String, Map<String, MAINAccountTokenBalance>> accTokenCaches = new HashMap<String, Map<String, MAINAccountTokenBalance>>();
	
	@Override
	public void run() {
		log.error("check BalanceStorage");
		
		while(!updateAccList.isEmpty())
		{
			try {
				String accAddress = updateAccList.remove(0);
				Map<String, MAINAccountTokenBalance> tokens = null;
				boolean isCache = false;
				// 先从缓存获取token信息
				if(accTokenCaches.containsKey(accAddress))
				{
					tokens = accTokenCaches.get(accAddress);
					isCache = true;
				}
				// 如果缓存没有，则从数据获取所有token信息
				if(tokens == null)
				{
					tokens = new HashMap<String, MAINAccountTokenBalance>();
					MAINAccountTokenBalanceExample example = new MAINAccountTokenBalanceExample();
					example.createCriteria().andAccountAddressEqualTo(accAddress);
					List<Object> datas = daos.getMainAccountTokenBalanceDao().selectByExample(example);
					if(datas != null && !datas.isEmpty())
					{
						for(Object data : datas)
						{
							MAINAccountTokenBalance token = (MAINAccountTokenBalance)data;
							tokens.put(token.getTokenName(), token);
						}
					}
				}
				
				// 更新列表
				List<Object> needUpdateList = new ArrayList<Object>();
				// 新建列表
				List<Object> needInsertList = new ArrayList<Object>();
				// 从链上获取帐号信息
				
				
				Builder b = daos.getAccountHelper().getAccount(daos.getCrypto().hexStrToBytes(accAddress));
				// AccountInfo accInfo = b.build();
				int tokensCount = b.getValue().getTokensCount();
				if(tokensCount == 0)
				{
					// 无token货币，直接更新数值
					// 只更新缓存中存在的内容
					if(!tokens.isEmpty())
					{
						// 遍历货币
						Iterator<Entry<String, MAINAccountTokenBalance>> iter = tokens.entrySet().iterator();
						while(iter.hasNext())
						{
							Entry<String, MAINAccountTokenBalance> entry = iter.next();
							MAINAccountTokenBalance tokenBalance = entry.getValue();
							// 判断是否已经没钱
							if(tokenBalance.getBalance() != null && !tokenBalance.getBalance().isEmpty() && !"0".equals(tokenBalance.getBalance()))
							{
								// 如果有钱，则清空钱，并且加入写库列表
								tokenBalance.setBalance("0");
								needUpdateList.add(tokenBalance);
							}
						}
					}
				}
				else
				{
					// 有token货币信息，遍历赋值
					for(AccountTokenValue tv : b.getValue().getTokensList())
					{
						String tokenName = tv.getToken().toStringUtf8();
						BigInteger tokenBalanceValue = BytesHelper.bytesToBigInteger(tv.getBalance().toByteArray());
						
						// 判断本地货币信息是否存在该货币
						MAINAccountTokenBalance tokenBalance = null;
						if(tokens.containsKey(tokenName))
						{
							tokenBalance = tokens.get(tokenName);
							
							// 判断货币数是否相等，如果相等，则不保存
							if(!tokenBalance.getBalance().equals(tokenBalanceValue.toString()))
							{
								tokenBalance.setBalance(tokenBalanceValue.toString());
								needUpdateList.add(tokenBalance);
							}
						}
						if(tokenBalance == null)
						{
							// 如果无本地数据，则创建一个
							tokenBalance = new MAINAccountTokenBalance();
							tokenBalance.setAccountAddress(accAddress);
							tokenBalance.setTokenName(tokenName);
							tokenBalance.setBalance(tokenBalanceValue.toString());
							needInsertList.add(tokenBalance);
						}
						
					}
				}
				// 开始写库
				if(!needUpdateList.isEmpty())
				{
					daos.getMainAccountTokenBalanceDao().batchUpdate(needUpdateList);
				}
				if(!needInsertList.isEmpty())
				{
					daos.getMainAccountTokenBalanceDao().batchInsert(needInsertList);
				}
				
				if(!needInsertList.isEmpty())
				{
					// 如果有插入数据，则清除缓存
					if(isCache)
					{
						accTokenCaches.remove(accAddress);
					}
				}
				// 如果无缓存，则加入缓存
				else if(!isCache)
				{
					accTokenCaches.put(accAddress, tokens);
				}
				
				// 休息100毫秒后继续操作
				Thread.sleep(100);
			} catch (Exception e) {
				log.error("" + e);
			}
		}
		
		
	}
}
