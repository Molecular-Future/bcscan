package org.brewchain.scan.core;

import java.util.List;

import org.apache.felix.ipojo.util.Log;
import org.brewchain.core.config.ChainConfig;
import org.brewchain.core.model.Block.BlockInfo;
import org.brewchain.scan.main.entity.MAINBlock;
import org.brewchain.scan.main.entity.MAINBlockExample;
import org.brewchain.scan.main.entity.MAINContractExample;
import org.brewchain.scan.main.entity.MAINCryptoExample;
import org.brewchain.scan.main.entity.MAINTokenExample;
import org.brewchain.scan.main.entity.MAINTransactionCallContractExample;
import org.brewchain.scan.main.entity.MAINTransactionCryptoExample;
import org.brewchain.scan.main.entity.MAINTransactionExample;
import org.brewchain.scan.main.entity.MAINTransactionTokenExample;
import org.brewchain.scan.main.entity.MAINTransactionTransferExample;
import org.brewchain.scan.sys.entity.SYSDict;
import org.brewchain.scan.sys.entity.SYSDictExample;
import org.brewchian.scan.ScanDaos;
import org.brewchian.scan.ScanInfo;
import org.brewchian.scan.ScanKeyFor;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.ojpa.api.TransactionExecutor;

@Slf4j
public class BlockValidator implements Runnable {
	ScanDaos daos;

	public BlockValidator(ScanDaos _daos) {
		this.daos = _daos;
	}

	@Override
	public void run() {
		log.error("scan start validate");
//		daos.setPaused(true);
		// 取数据库中的最高块，从最高块开始向前查找
//		ScanInfo.lastConnectCheckBlockHeight
		return;
//		
//		SYSDictExample oAllDictExample = new SYSDictExample();
//		oAllDictExample.createCriteria().andDictInitEqualTo(1).andDictKeyEqualTo("last_connect_block");
//		SYSDict oSYSDict = (SYSDict)daos.getSysDictDao().selectOneByExample(oAllDictExample);		
//		long lastCheckBlockTmp = Long.parseLong(oSYSDict.getDictValue());
//		lastCheckBlockTmp += 1;
//		log.error("scan start validate. start height:" + lastCheckBlockTmp);
//
//		MAINBlockExample oLastBlock = new MAINBlockExample();
//		oLastBlock.createCriteria().andHeightEqualTo(lastCheckBlockTmp);
//		oLastBlock.setOrderByClause("height desc");
//
//		List<Object> objBlocks = daos.getMainBlockDao().selectByExample(oLastBlock);
//		long saveHeight = lastCheckBlockTmp; 
//		while (lastCheckBlockTmp < (daos.getBcHelper().getMaxConnectHeight()
//				- daos.getChainConfig().getBlock_stable_count())) {
//			if (objBlocks != null && objBlocks.size() > 1) {
//				// 说明有分叉，尝试删除分叉区块，如果无法删除，停止继续检查，设置最后检查高度为该区块高度-1
//				BlockInfo[] bis = daos.getBcHelper().listBlockByHeight(lastCheckBlockTmp);
//				if (bis.length > 1) {
//					// 说明分叉区块还没有合并，只能等待合并后继续
//					lastCheckBlockTmp -= 1;
//					break;
//				} else if (bis.length == 1) {
//					// 说明分叉已经合并，移除分叉区块的数据
//					for (Object objBlock : objBlocks) {
//						MAINBlock block = (MAINBlock) objBlock;
//
//						MAINBlockExample oDelMainBlock = new MAINBlockExample();
//						oDelMainBlock.createCriteria().andBlockHashEqualTo(block.getBlockHash());
//						MAINTransactionExample oDelMainTransaction = new MAINTransactionExample();
//						oDelMainTransaction.createCriteria().andBlockHashEqualTo(block.getBlockHash());
//						MAINTransactionTokenExample oDelMainTransactionToken = new MAINTransactionTokenExample();
//						oDelMainTransactionToken.createCriteria().andBlockHashEqualTo(block.getBlockHash());
//						MAINTransactionCryptoExample oDelMainTransactionCrypto = new MAINTransactionCryptoExample();
//						oDelMainTransactionCrypto.createCriteria().andBlockHashEqualTo(block.getBlockHash());
//						MAINTransactionCallContractExample oDelMainTransactionCallContract = new MAINTransactionCallContractExample();
//						oDelMainTransactionCallContract.createCriteria().andBlockHashEqualTo(block.getBlockHash());
//						MAINTransactionTransferExample oDelMainTransactionTransfer = new MAINTransactionTransferExample();
//						oDelMainTransactionTransfer.createCriteria().andBlockHashEqualTo(block.getBlockHash());
//						MAINCryptoExample oDelMainCrypto = new MAINCryptoExample();
//						oDelMainCrypto.createCriteria().andBlockHashEqualTo(block.getBlockHash());
//						MAINContractExample oDelMainContract = new MAINContractExample();
//						oDelMainContract.createCriteria().andBlockHashEqualTo(block.getBlockHash());
//						MAINTokenExample oDelMainToken = new MAINTokenExample();
//						oDelMainToken.createCriteria().andBlockHashEqualTo(block.getBlockHash());
//
//						try {
//							daos.getMainBlockDao().doInTransaction(new TransactionExecutor() {
//								@Override
//								public Object doInTransaction() {
//									daos.getMainBlockDao().deleteByExample(oDelMainBlock);
//									daos.getMainTransactionDao().deleteByExample(oDelMainTransaction);
//									daos.getMainTransactionTokenDao().deleteByExample(oDelMainTransactionToken);
//									daos.getMainTransactionCryptoDao().deleteByExample(oDelMainTransactionCrypto);
//									daos.getMainTransactionCallContractDao()
//											.deleteByExample(oDelMainTransactionCallContract);
//									daos.getMainTransactionTransferDao().deleteByExample(oDelMainTransactionTransfer);
//									daos.getMainCryptoDao().deleteByExample(oDelMainCrypto);
//									daos.getMainContractDao().deleteByExample(oDelMainContract);
//									daos.getMainTokenDao().deleteByExample(oDelMainToken);
//
//									return true;
//								}
//							});
//						} catch (Exception e) {
//							lastCheckBlockTmp -= 1;
//							break;
//						}
//					}
//
//					lastCheckBlockTmp += 1;
//					oLastBlock = null;
//					oLastBlock = new MAINBlockExample();
//					oLastBlock.createCriteria().andHeightEqualTo(lastCheckBlockTmp);
//					objBlocks = daos.getMainBlockDao().selectByExample(oLastBlock);
//				}
//			} else if (objBlocks != null && objBlocks.size() == 1) {
//				log.error("releady add" + lastCheckBlockTmp);
//				lastCheckBlockTmp += 1;
//				saveHeight = lastCheckBlockTmp;
//				oLastBlock = null;
//				oLastBlock = new MAINBlockExample();
//				oLastBlock.createCriteria().andHeightEqualTo(lastCheckBlockTmp);
//				objBlocks = daos.getMainBlockDao().selectByExample(oLastBlock);
//			} else {
//				// 缺块
//				BlockInfo[] bis = daos.getBcHelper().listBlockByHeight(lastCheckBlockTmp);
//				if (bis.length > 1) {
//					// 说明分叉，无法保存缺少的块，直接结束
//					lastCheckBlockTmp -= 1;
//					break;
//				} else if (bis.length == 0) {
//					// 说明已到最高块
//					lastCheckBlockTmp -= 1;
//					break;
//				} else {
//					// 加到处理队列中，等待后续继续检查
//					daos.oQueue.add(bis[0]);
//					//lastCheckBlockTmp -= 1;
//					//break;
//					log.error("add oQueue" + lastCheckBlockTmp);
//					lastCheckBlockTmp += 1;
//					oLastBlock = null;
//					oLastBlock = new MAINBlockExample();
//					oLastBlock.createCriteria().andHeightEqualTo(lastCheckBlockTmp);
//					objBlocks = daos.getMainBlockDao().selectByExample(oLastBlock);
//				}
//			}
//		}
//
//		try {
//			final long finalLastBlock = saveHeight;
//			daos.getMainBlockDao().doInTransaction(new TransactionExecutor() {
//				@Override
//				public Object doInTransaction() {
//					try {
//						String connectBlockKey = ScanInfo.class.getDeclaredField("lastConnectCheckBlockHeight")
//								.getAnnotation(ScanKeyFor.class).value();
//						String connectBlockTryKey = ScanInfo.class.getDeclaredField("lastConnectCheckTry")
//								.getAnnotation(ScanKeyFor.class).value();
//						String connectBlockTimestampKey = ScanInfo.class.getDeclaredField("lastConnectCheckTimestamp")
//								.getAnnotation(ScanKeyFor.class).value();
//
//						// 更新最新处理结果
//						SYSDictExample oSysDict = new SYSDictExample();
//						try {
//							oSysDict.createCriteria().andDictKeyEqualTo(connectBlockKey)
//									.andDictValueEqualTo(String.valueOf(finalLastBlock));
//						} catch (Exception e2) {
//							// TODO Auto-generated catch block
//							log.error("" + e2);
//						} 
//						Object c = daos.getSysDictDao().selectOneByExample(oSysDict);
//						if (c == null) {
//							SYSDict oSysDictTry = new SYSDict();
//							oSysDictTry.setDictKey(connectBlockTryKey);
//							oSysDictTry.setDictValue("0");
//							daos.getSysDictDao().updateByPrimaryKeySelective(oSysDictTry);
//						} else {
//							SYSDictExample oSysDictTryExample = new SYSDictExample();
//							oSysDictTryExample.createCriteria().andDictKeyEqualTo(connectBlockTryKey);
//							SYSDict oExistsSYSDictTry = (SYSDict) daos.getSysDictDao()
//									.selectOneByExample(oSysDictTryExample);
//							oExistsSYSDictTry.setDictValue(
//									String.valueOf(Integer.parseInt(oExistsSYSDictTry.getDictValue()) + 1));
//							daos.getSysDictDao().updateByPrimaryKeySelective(oExistsSYSDictTry);
//						}
//
//						SYSDict oSysDictTimestamp = new SYSDict();
//						oSysDictTimestamp.setDictKey(connectBlockTimestampKey);
//						oSysDictTimestamp.setDictValue(String.valueOf(System.currentTimeMillis()));
//						daos.getSysDictDao().updateByPrimaryKeySelective(oSysDictTimestamp);
//
//						SYSDict oSysDictBlock = new SYSDict();
//						oSysDictBlock.setDictKey(connectBlockKey);
//
//						oSysDictBlock.setDictValue(String.valueOf(finalLastBlock));
//						
//						daos.getSysDictDao().updateByPrimaryKeySelective(oSysDictBlock);
//						ScanInfo.lastConnectCheckBlockHeight = String.valueOf(finalLastBlock);
//						return null;
//					} catch (Exception e) {
//						// TODO Auto-generated catch block
//						log.error("" + e);
//					} finally {
//						log.error("scan over validate");
////						daos.setPaused(false);
//					}
//					return null;
//				}
//			});
//		} catch (Throwable e) {
//			e.printStackTrace();
//		}
	}
}
