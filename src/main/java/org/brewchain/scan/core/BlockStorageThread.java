package org.brewchain.scan.core;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.core.model.Block.BlockHeader;
import org.brewchain.core.model.Block.BlockInfo;
import org.brewchain.core.model.Block.BlockMiner;
import org.brewchain.core.model.Transaction.TransactionData;
import org.brewchain.core.model.Transaction.TransactionInfo;
import org.brewchain.core.model.Transaction.TransactionData.CallContractData;
import org.brewchain.core.model.Transaction.TransactionData.OwnerTokenData;
import org.brewchain.core.model.Transaction.TransactionData.PublicContractData;
import org.brewchain.scan.browser.entity.OTCBrowserSlowblock;
import org.brewchain.scan.browser.entity.OTCBrowserTotal;
import org.brewchain.scan.browser.entity.OTCBrowserTotalExample;
import org.brewchain.scan.data.CallFun;
import org.brewchain.scan.main.entity.MAINBlock;
import org.brewchain.scan.main.entity.MAINBlockExample;
import org.brewchain.scan.main.entity.MAINContract;
import org.brewchain.scan.main.entity.MAINToken;
import org.brewchain.scan.main.entity.MAINTokenExample;
import org.brewchain.scan.main.entity.MAINTransaction;
import org.brewchain.scan.main.entity.MAINTransactionCallContract;
import org.brewchain.scan.main.entity.MAINTransactionToken;
import org.brewchain.scan.main.entity.MAINTransactionTransfer;
import org.brewchain.scan.manager.ScanConfig;
import org.brewchain.scan.network.entity.NETWORKPeer;
import org.brewchain.scan.network.entity.NETWORKPeerExample;
import org.brewchain.scan.utils.ScanTools;
import org.brewchain.tools.bytes.BytesHelper;
import org.brewchain.tools.rlp.RLP;
import org.brewchian.scan.ScanDaos;

import com.google.protobuf.ByteString;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.ojpa.api.TransactionExecutor;

@AllArgsConstructor
@Slf4j
public class BlockStorageThread implements Runnable {
	BlockInfo block;
	ScanDaos daos;
	ScanConfig config;
	Map<String, NETWORKPeer> peerCaches;
	
	@Override
	public void run() {
		// 保存区块
		MAINBlockExample oExistsMainBlockExample = new MAINBlockExample();
		oExistsMainBlockExample.createCriteria()
				.andBlockHashEqualTo(daos.getCrypto().bytesToHexStr(block.getHeader().getHash().toByteArray()));

		// 判断区块中有用的数据是否存在
		if (daos.getMainBlockDao().countByExample(oExistsMainBlockExample) == 0) {

			final String blockHash = daos.getCrypto().bytesToHexStr(block.getHeader().getHash().toByteArray());
			final String paretBlockHash = daos.getCrypto()
					.bytesToHexStr(block.getHeader().getParentHash().toByteArray());
			final long blockHeight = block.getHeader().getHeight();
			final BlockHeader blockHeader = block.getHeader();
			final BlockMiner blockMiner = block.getMiner();
			final int blockSize = block.toByteArray().length;
			// 存库
			daos.getMainBlockDao().doInTransaction(new TransactionExecutor() {
				@Override
				public Object doInTransaction() {
					// 块信息
					MAINBlock oMAINBlock = new MAINBlock();
					// 交易调用合约信息列表
					List<MAINTransactionCallContract> listMAINTransactionCallContract = new ArrayList<MAINTransactionCallContract>();
					// token信息字典（map保存，key为货币名称 token_name）新建列表
					Map<String, MAINToken> mapMainToken = new HashMap<String, MAINToken>();
					// token信息字典（map保存，key为货币名称 token_name）更新列表
					Map<String, MAINToken> mapUMainToken = new HashMap<String, MAINToken>();
					// MAINToken oMAINToken = null;
					// token发起，燃烧，增发信息记录列表
					List<MAINTransactionToken> listMAINTransactionToken = new ArrayList<MAINTransactionToken>();
					// 合约信息列表
					List<MAINContract> listMAINContract = new ArrayList<MAINContract>();
					// 交易信息列表
					List<MAINTransaction> listTxs = new ArrayList<>();
					// 交易转账信息列表
					List<MAINTransactionTransfer> listTxAccounts = new ArrayList<>();
					// 721信息字典（map保存）
					// Map<String, MAINCrypto> mapMainCrypto = new HashMap<String, MAINCrypto>();
					// 721发行记录列表
					// List<MAINTransactionCrypto> listMAINTransactionCrypto = new
					// ArrayList<MAINTransactionCrypto>();

					// 赋值并保存区块
					oMAINBlock.setBlockHash(blockHash);
					oMAINBlock.setCreated(new Date());
					oMAINBlock.setExtraData(blockHeader.getExtraData().toStringUtf8());
					oMAINBlock.setHeight(blockHeight);
					oMAINBlock.setMinnerAccountAddress(
							daos.getCrypto().bytesToHexStr(blockMiner.getAddress().toByteArray()));
					oMAINBlock.setParentBlockHash(paretBlockHash);
					oMAINBlock
							.setReceiptRoot(daos.getCrypto().bytesToHexStr(blockHeader.getReceiptRoot().toByteArray()));
					oMAINBlock.setStateRoot(daos.getCrypto().bytesToHexStr(blockHeader.getStateRoot().toByteArray()));
					oMAINBlock.setTimestamp(new Date(blockHeader.getTimestamp()));
					oMAINBlock.setTransactionCount(blockHeader.getTxHashsCount());
					oMAINBlock.setUpdated(new Date());
					oMAINBlock.setBlockSize(blockSize);
					oMAINBlock.setReward(
							String.valueOf(BytesHelper.bytesToBigInteger(blockMiner.getReward().toByteArray())));

					// 交易时间列表
					List<Integer> txTimeList = new ArrayList<Integer>();

					// 交易相关帐号列表
					Set<String> txAccSet = new HashSet<String>();

					for (ByteString txHash : blockHeader.getTxHashsList()) {
						// 处理交易列表
						try {
							String txHashHex = daos.getCrypto().bytesToHexStr(txHash.toByteArray());
							// 读取交易信息--从区块链
							TransactionInfo ti = daos.getTxHelper().getTransaction(txHash.toByteArray());
							// 交易信息表
							MAINTransaction oMAINTransaction = new MAINTransaction();
							oMAINTransaction.setBlockHash(blockHash);
							oMAINTransaction.setBlockHeight(blockHeight);
							oMAINTransaction.setSize(new Long(ti.toByteArray().length));
							// 赋值交易状态和返回信息--目前只有合约才有，合约失败后的失败原因
							if (ti.getStatus() != null) {
								if (ti.getStatus().getResult() != null
										&& !ByteString.EMPTY.equals(ti.getStatus().getResult())) {
									oMAINTransaction.setResult(ti.getStatus().getResult().toStringUtf8());
								}

								if (ti.getStatus().getStatus() != null
										&& !ByteString.EMPTY.equals(ti.getStatus().getStatus())) {
									oMAINTransaction.setStatus(ti.getStatus().getStatus().toStringUtf8());
								}
								oMAINTransaction.setExecTimestamp(new Date(ti.getStatus().getTimestamp()));
							}
							oMAINTransaction.setAccessTimestamp(new Date(ti.getAccepttimestamp()));
							oMAINTransaction.setTimestamp(new Date(ti.getBody().getTimestamp()));
							oMAINTransaction
									.setTransactionHash(daos.getCrypto().bytesToHexStr(ti.getHash().toByteArray()));
							// 赋值交易类型
							if (ti.getBody().getData() == null || ti.getBody().getData().getType() == null) {
								oMAINTransaction.setType(TransactionData.DataType.NONE_VALUE);
							} else {
								oMAINTransaction.setType(ti.getBody().getData().getTypeValue());
							}
							// 赋值交易创建节点
							oMAINTransaction
									.setPeerId(daos.getCrypto().bytesToHexStr(ti.getNode().getAddress().toByteArray()));
							// 赋值发起账户地址
							oMAINTransaction.setAccountAddress(
									daos.getCrypto().bytesToHexStr(ti.getBody().getAddress().toByteArray()));
							oMAINTransaction.setExtraData(
									daos.getCrypto().bytesToHexStr(ti.getBody().getExdata().toByteArray()));

							// 加入交易保存队列
							listTxs.add(oMAINTransaction);

							// 加入交易时间列表
							txTimeList.add((int) (oMAINBlock.getTimestamp().getTime()
									- oMAINTransaction.getTimestamp().getTime()));

							// 交易转账信息
							MAINTransactionTransfer oMAINTransactionTransferIn = new MAINTransactionTransfer();
							oMAINTransactionTransferIn.setTransactionTransferId(ScanTools.getUUID());
							oMAINTransactionTransferIn.setAccountAddress(oMAINTransaction.getAccountAddress());
							oMAINTransactionTransferIn.setTransactionHash(oMAINTransaction.getTransactionHash());
							oMAINTransactionTransferIn.setTransferType("IN");
							oMAINTransactionTransferIn.setBlockHeight(oMAINTransaction.getBlockHeight());
							oMAINTransactionTransferIn.setBlockHash(blockHash);
							oMAINTransactionTransferIn.setStatus(oMAINTransaction.getStatus());
							oMAINTransactionTransferIn.setTimestamp(oMAINTransaction.getTimestamp());
							oMAINTransactionTransferIn.setType(oMAINTransaction.getType());
							oMAINTransactionTransferIn.setAmount("0");
							oMAINTransactionTransferIn.setTokenAmount("0");

							// 加入交易参与者
							txAccSet.add(oMAINTransactionTransferIn.getAccountAddress());

							// 处理交易账户
							// 处理交易账户
							if (ti.getBody().getData() == null
									|| ti.getBody().getData().getType().equals(TransactionData.DataType.NONE)) {
								// 转账交易
								ti.getBody().getOutputsList().stream().forEach((txOutput) -> {
									MAINTransactionTransfer oMAINTransactionTransferOut = new MAINTransactionTransfer();
									oMAINTransactionTransferOut.setTransactionTransferId(ScanTools.getUUID());
									oMAINTransactionTransferOut.setAccountAddress(
											daos.getCrypto().bytesToHexStr(txOutput.getAddress().toByteArray()));

									BigInteger amount = BytesHelper
											.bytesToBigInteger(txOutput.getAmount().toByteArray());
									oMAINTransactionTransferOut.setAmount(amount.toString());

									oMAINTransactionTransferIn
											.setAmount(new BigInteger(oMAINTransactionTransferIn.getAmount())
													.add(amount).toString());

									// oMAINTransactionTransferOut.setContractAddress(daos.getCrypto()
									// .bytesToHexStr(txOutput.getAddress().toByteArray()));

									if (StringUtils.isNotBlank(txOutput.getSymbol().toStringUtf8())) {
										oMAINTransactionTransferOut.setCryptoName(txOutput.getSymbol().toStringUtf8());
									}

									if (StringUtils.isNotBlank(txOutput.getToken().toStringUtf8())) {
										BigInteger tokenAmount = BytesHelper
												.bytesToBigInteger(txOutput.getTokenAmount().toByteArray());
										oMAINTransactionTransferOut.setTokenAmount(tokenAmount.toString());
										oMAINTransactionTransferOut.setTokenName(txOutput.getToken().toStringUtf8());

										oMAINTransactionTransferIn.setTokenAmount(
												new BigInteger(oMAINTransactionTransferIn.getTokenAmount())
														.add(tokenAmount).toString());
										oMAINTransactionTransferIn.setTokenName(txOutput.getToken().toStringUtf8());
									}

									oMAINTransactionTransferOut
											.setTransactionHash(oMAINTransaction.getTransactionHash());
									oMAINTransactionTransferOut.setTransferType("OUT");
									oMAINTransactionTransferOut.setBlockHash(blockHash);
									oMAINTransactionTransferOut.setBlockHeight(oMAINTransaction.getBlockHeight());
									oMAINTransactionTransferOut.setStatus(oMAINTransaction.getStatus());
									oMAINTransactionTransferOut.setTimestamp(oMAINTransaction.getTimestamp());
									oMAINTransactionTransferOut.setType(oMAINTransaction.getType());
									listTxAccounts.add(oMAINTransactionTransferOut);

									// 加入交易参与者
									txAccSet.add(oMAINTransactionTransferOut.getAccountAddress());
								});
							} else {
								// 处理非转账交易
								switch (ti.getBody().getData().getType()) {
								case PUBLICCONTRACT: {
									// 发行合约
									// 合约数据
									PublicContractData oPublicContractData = ti.getBody().getData()
											.getPublicContractData();

									String contractAddress = daos.getCrypto()
											.bytesToHexStr(daos.getCrypto()
													.sha256(RLP.encodeList(ti.getBody().getAddress().toByteArray(),
															BytesHelper.intToBytes(ti.getBody().getNonce()))));

									// 发行合约
									MAINContract oMAINContract = new MAINContract();
									oMAINContract.setAccountAddress(oMAINTransaction.getAccountAddress());
									oMAINContract.setBlockHash(blockHash);
									oMAINContract.setBlockHeight(blockHeight);
									oMAINContract.setCode(daos.getCrypto()
											.bytesToHexStr(oPublicContractData.getCode().toByteArray()));
									oMAINContract.setCodeBin(daos.getCrypto()
											.bytesToHexStr(oPublicContractData.getData().toByteArray()));
									oMAINContract.setContractAddress(contractAddress);
									oMAINContract.setCreateTime(new Date());
									oMAINContract.setNeedRefresh(0);
									oMAINContract.setNonce(ti.getBody().getNonce());
									oMAINContract.setTransactionHash(txHashHex);

									// 加入列表
									listMAINContract.add(oMAINContract);

									// 增加合约调用
									MAINTransactionCallContract oMAINTransactionCallContract = new MAINTransactionCallContract();
									oMAINTransactionCallContract.setAmount(
											BytesHelper.bytesToBigInteger(oPublicContractData.getAmount().toByteArray())
													.toString());
									oMAINTransactionCallContract.setContractAddress(oMAINContract.getContractAddress());
									oMAINTransactionCallContract.setData(oMAINContract.getCodeBin());
									oMAINTransactionCallContract.setTransactionHash(txHashHex);
									oMAINTransactionCallContract.setBlockHash(blockHash);
									oMAINTransactionCallContract
											.setAccountAddress(oMAINTransaction.getAccountAddress());

									oMAINTransactionCallContract.setTimestamp(oMAINTransaction.getTimestamp());
									// 防双花号
									oMAINTransactionCallContract.setNonce(ti.getBody().getNonce());
									// 调用方法赋值
									CallFun callFun = ScanTools.makeCallFun(oMAINTransactionCallContract.getData());
									oMAINTransactionCallContract.setCallFun(callFun.getFunHex());

									// 新加交易需要数据
									oMAINTransactionCallContract.setBlockHeight(oMAINTransaction.getBlockHeight());
									oMAINTransactionCallContract.setStatus(oMAINTransaction.getStatus());
									oMAINTransactionCallContract.setResult(oMAINTransaction.getResult());
									oMAINTransactionCallContract.setType(oMAINTransaction.getType());
									oMAINTransactionCallContract.setExtraData(oMAINTransaction.getExtraData());
									// 加入保存队列
									listMAINTransactionCallContract.add(oMAINTransactionCallContract);
								}
									break;
								case CALLCONTRACT: {
									// 合约调用
									CallContractData oCallContractData = ti.getBody().getData().getCallContractData();

									MAINTransactionCallContract oMAINTransactionCallContract = new MAINTransactionCallContract();
									oMAINTransactionCallContract.setAmount(BytesHelper
											.bytesToBigInteger(oCallContractData.getAmount().toByteArray()).toString());
									oMAINTransactionCallContract.setContractAddress(daos.getCrypto()
											.bytesToHexStr(oCallContractData.getContract().toByteArray()));
									oMAINTransactionCallContract.setData(
											daos.getCrypto().bytesToHexStr(oCallContractData.getData().toByteArray()));
									oMAINTransactionCallContract.setTransactionHash(txHashHex);
									oMAINTransactionCallContract.setBlockHash(blockHash);
									oMAINTransactionCallContract
											.setAccountAddress(oMAINTransaction.getAccountAddress());

									oMAINTransactionCallContract.setTimestamp(oMAINTransaction.getTimestamp());
									oMAINTransactionCallContract.setExtraData(oMAINTransaction.getExtraData());
									// 防双花号
									oMAINTransactionCallContract.setNonce(ti.getBody().getNonce());
									// 调用方法赋值
									CallFun callFun = ScanTools.makeCallFun(oMAINTransactionCallContract.getData());
									oMAINTransactionCallContract.setCallFun(callFun.getFunHex());

									// 新加交易需要数据
									oMAINTransactionCallContract.setBlockHeight(oMAINTransaction.getBlockHeight());
									oMAINTransactionCallContract.setStatus(oMAINTransaction.getStatus());
									oMAINTransactionCallContract.setResult(oMAINTransaction.getResult());
									oMAINTransactionCallContract.setType(oMAINTransaction.getType());
									// 加入保存队列
									listMAINTransactionCallContract.add(oMAINTransactionCallContract);
								}
									break;
								case OWNERTOKEN:
									// Token管理，创建，增发，燃烧
									OwnerTokenData oOwnerTokenData = ti.getBody().getData().getOwnerTokenData();
									if (oOwnerTokenData.getOpCode() == OwnerTokenData.OwnerTokenOpCode.PUBLIC) {
										// token主数据创建
										MAINToken oMAINToken = new MAINToken();
										oMAINToken.setAccountAddress(oMAINTransaction.getAccountAddress());
										oMAINToken.setBlockHash(blockHash);
										oMAINToken.setBlockHeight(blockHeight);
										oMAINToken.setCreateTime(new Date());
										oMAINToken.setHolders(0);
										oMAINToken.setNeedRefresh(0);
										oMAINToken.setTokenName(oOwnerTokenData.getToken().toStringUtf8());
										oMAINToken.setTotalAmount(
												BytesHelper.bytesToBigInteger(oOwnerTokenData.getAmount().toByteArray())
														.toString());
										oMAINToken.setTransactionHash(txHashHex);
										oMAINToken.setTransfers(0);

										// 加入保存字典
										mapMainToken.put(oMAINToken.getTokenName(), oMAINToken);
										mapUMainToken.put(oMAINToken.getTokenName(), oMAINToken);

										// token操作记录数据
										MAINTransactionToken oMAINTransactionToken = new MAINTransactionToken();
										oMAINTransactionToken.setAccountAddress(oMAINToken.getAccountAddress());
										oMAINTransactionToken.setAfterOpt(oMAINToken.getTotalAmount());
										oMAINTransactionToken.setAmount(oMAINToken.getTotalAmount());
										oMAINTransactionToken.setBeforeOpt("0");
										oMAINTransactionToken.setBlockHash(blockHash);
										oMAINTransactionToken.setCreateTime(new Date());
										oMAINTransactionToken.setOptType(oOwnerTokenData.getOpCodeValue());
										oMAINTransactionToken.setTimestamp(new Date(ti.getBody().getTimestamp()));
										oMAINTransactionToken.setTokenName(oMAINToken.getTokenName());
										oMAINTransactionToken.setTransactionHash(txHashHex);
										// 加入保存列表
										listMAINTransactionToken.add(oMAINTransactionToken);
									} else if (oOwnerTokenData.getOpCode() == OwnerTokenData.OwnerTokenOpCode.BURN) {
										// 燃烧
										// token操作记录数据
										MAINTransactionToken oMAINTransactionToken = new MAINTransactionToken();
										oMAINTransactionToken.setAccountAddress(oMAINTransaction.getAccountAddress());
										oMAINTransactionToken.setAmount(
												BytesHelper.bytesToBigInteger(oOwnerTokenData.getAmount().toByteArray())
														.toString());
										oMAINTransactionToken.setBlockHash(blockHash);
										oMAINTransactionToken.setCreateTime(new Date());
										oMAINTransactionToken.setOptType(oOwnerTokenData.getOpCodeValue());
										oMAINTransactionToken.setTimestamp(new Date(ti.getBody().getTimestamp()));
										oMAINTransactionToken.setTokenName(oOwnerTokenData.getToken().toStringUtf8());
										oMAINTransactionToken.setTransactionHash(txHashHex);

										// 查询token主表
										MAINToken oExistsMainToken = null;
										String tokenName = oOwnerTokenData.getToken().toStringUtf8();
										// 先判断缓存是否存在
										if (mapUMainToken.containsKey(tokenName)) {
											oExistsMainToken = mapUMainToken.get(tokenName);
										} else {
											// 从数据库获取
											MAINTokenExample oSelectExample = new MAINTokenExample();
											oSelectExample.createCriteria().andTokenNameEqualTo(tokenName);
											oExistsMainToken = (MAINToken) daos.getMainTokenDao()
													.selectOneByExample(oSelectExample);
											if (oExistsMainToken != null) {
												// 存在则加入缓存
												mapUMainToken.put(tokenName, oExistsMainToken);
											}
										}

										if (oExistsMainToken == null) {
											// 缺少块，标记该数据为异常
											oMAINTransactionToken.setAfterOpt("-");
											oMAINTransactionToken.setBeforeOpt("-");

										} else {
											oMAINTransactionToken
													.setAfterOpt(new BigInteger(oExistsMainToken.getTotalAmount())
															.subtract(BytesHelper.bytesToBigInteger(
																	oOwnerTokenData.getAmount().toByteArray()))
															.toString());
											oMAINTransactionToken.setBeforeOpt(oExistsMainToken.getTotalAmount());

											// 刷新token主数据
											oExistsMainToken.setTotalAmount(oMAINTransactionToken.getAfterOpt());
										}
										// 加入保存列表
										listMAINTransactionToken.add(oMAINTransactionToken);
									} else if (oOwnerTokenData.getOpCode() == OwnerTokenData.OwnerTokenOpCode.MINT) {
										// 增发
										// token操作记录数据
										MAINTransactionToken oMAINTransactionToken = new MAINTransactionToken();
										oMAINTransactionToken.setAccountAddress(oMAINTransaction.getAccountAddress());
										oMAINTransactionToken.setAmount(
												BytesHelper.bytesToBigInteger(oOwnerTokenData.getAmount().toByteArray())
														.toString());
										oMAINTransactionToken.setBlockHash(blockHash);
										oMAINTransactionToken.setCreateTime(new Date());
										oMAINTransactionToken.setOptType(oOwnerTokenData.getOpCodeValue());
										oMAINTransactionToken.setTimestamp(new Date(ti.getBody().getTimestamp()));
										oMAINTransactionToken.setTokenName(oOwnerTokenData.getToken().toStringUtf8());
										oMAINTransactionToken.setTransactionHash(txHashHex);

										// 查询token主表
										MAINToken oExistsMainToken = null;
										String tokenName = oOwnerTokenData.getToken().toStringUtf8();
										// 先判断缓存是否存在
										if (mapUMainToken.containsKey(tokenName)) {
											oExistsMainToken = mapUMainToken.get(tokenName);
										} else {
											// 从数据库获取
											MAINTokenExample oSelectExample = new MAINTokenExample();
											oSelectExample.createCriteria().andTokenNameEqualTo(tokenName);
											oExistsMainToken = (MAINToken) daos.getMainTokenDao()
													.selectOneByExample(oSelectExample);
											if (oExistsMainToken != null) {
												// 存在则加入缓存
												mapUMainToken.put(tokenName, oExistsMainToken);
											}
										}

										if (oExistsMainToken == null) {
											// 缺少块，标记该数据为异常
											oMAINTransactionToken.setAfterOpt("-");
											oMAINTransactionToken.setBeforeOpt("-");

										} else {
											oMAINTransactionToken
													.setAfterOpt(new BigInteger(oExistsMainToken.getTotalAmount())
															.add(BytesHelper.bytesToBigInteger(
																	oOwnerTokenData.getAmount().toByteArray()))
															.toString());
											oMAINTransactionToken.setBeforeOpt(oExistsMainToken.getTotalAmount());

											// 刷新token主数据
											oExistsMainToken.setTotalAmount(oMAINTransactionToken.getAfterOpt());
										}
										// 加入保存列表
										listMAINTransactionToken.add(oMAINTransactionToken);
									}
									break;
								case PUBLICCRYPTOTOKEN:
									// TODO 发行721
									// PublicCryptoTokenData publicCryptoTokenData =
									// ti.getBody().getData().getCryptoTokenData();

									break;
								case PUBLICUNIONACCOUNT:
									// TODO 创建联合账户
									// UnionAccountData unionAccountData =
									// ti.getBody().getData().getUnionAccountData();

									break;
								// case CREATEUNIONACCOUNT: //已取消
								//
								// break;
								case UNIONACCOUNTCONFIRM:
									// TODO 联合账户确认交易
									// UnionAccountConfirmData unionAccountConfirmData =
									// ti.getBody().getData().getUnionAccountConfirmData();

									break;
								case USERTOKEN:
									// TODO Token操作，锁定
									// UserTokenData userTokenData =
									// ti.getBody().getData().getUserTokenData();

									break;
								case UNRECOGNIZED:
									// 未知类型
									break;
								default:
									break;
								}
							}

							// 加入转账保存队列
							listTxAccounts.add(oMAINTransactionTransferIn);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							log.error("", e);
						}
					}

					// 保存区块链首页缓存数据
					// 先从数据库查询
					OTCBrowserTotalExample oOTCBrowserTotalExample = new OTCBrowserTotalExample();
					oOTCBrowserTotalExample.setOrderByClause("id desc");
					OTCBrowserTotal lastBT = (OTCBrowserTotal) daos.getOtcBrowserTotalDao()
							.selectOneByExample(oOTCBrowserTotalExample);
					OTCBrowserTotal nowBT = new OTCBrowserTotal();

					int avgTxTime = 0;
					if (!txTimeList.isEmpty()) {
						int allTime = 0;
						for (int txTime : txTimeList) {
							allTime += txTime;
						}
						avgTxTime = allTime / txTimeList.size();
					}

					if (lastBT == null) {
						// 如果数据为空，说明是新数据
						nowBT.setBlockCreateTime(oMAINBlock.getTimestamp());
						nowBT.setBlockHash(oMAINBlock.getBlockHash());
						nowBT.setBlockHeight(oMAINBlock.getHeight());
						nowBT.setBlockMiner(oMAINBlock.getMinnerAccountAddress());

						nowBT.setCurTx(0);
						nowBT.setMaxTx(0);
						nowBT.setBlockTime(0);
						nowBT.setAvgBlockTime(0);
						nowBT.setAvgTxTime(avgTxTime);
						nowBT.setTotalBlockTime(0L);
						nowBT.setTotalTxCount((long) nowBT.getCurTx());
						daos.getOtcBrowserTotalDao().insert(nowBT);
					} else {
						nowBT.setId(lastBT.getId());
						nowBT.setBlockCreateTime(oMAINBlock.getTimestamp());
						nowBT.setBlockHash(oMAINBlock.getBlockHash());

						nowBT.setBlockMiner(oMAINBlock.getMinnerAccountAddress());

						// 打块时间
						// 先判断是否是当前记录的后一块
						int blockTime = 0;
						if (oMAINBlock.getHeight() == lastBT.getBlockHeight() + 1) {
							// 如果是后续块
							blockTime = (int) (oMAINBlock.getTimestamp().getTime()
									- lastBT.getBlockCreateTime().getTime());

						} else {
							// 如果不是后续块
							// 查询前一块数据
							MAINBlockExample example = new MAINBlockExample();
							example.createCriteria().andHeightEqualTo(oMAINBlock.getHeight() - 1);
							MAINBlock privBlock = (MAINBlock) daos.getMainBlockDao().selectOneByExample(example);
							if (privBlock == null) {
								// 如果上一块也没入库，则丢弃此数据
							} else {
								blockTime = (int) (oMAINBlock.getTimestamp().getTime()
										- privBlock.getTimestamp().getTime());
							}

						}
						// 如果时间小于0，赋值为0
						if (blockTime < 0)
							blockTime = 0;
						int tx = 0;
						// 判断新块的高度是否大于老块
						if (oMAINBlock.getHeight() > lastBT.getBlockHeight()) {
							nowBT.setBlockHeight(oMAINBlock.getHeight());
							// 并且赋值首页数据
							nowBT.setBlockTime(blockTime);

							tx = blockTime < 1 ? 0 : blockHeader.getTxHashsCount() * 1000 / blockTime;
						} else {
							nowBT.setBlockHeight(lastBT.getBlockHeight());
							nowBT.setBlockTime(lastBT.getBlockTime());

							tx = lastBT.getCurTx();
							avgTxTime = lastBT.getAvgTxTime();
						}
						long totalBlockTime = lastBT.getTotalBlockTime() + blockTime;
						long totalTxCount = lastBT.getTotalTxCount() + blockHeader.getTxHashsCount();

						nowBT.setAvgBlockTime((int) (totalBlockTime / nowBT.getBlockHeight()));
						nowBT.setAvgTxTime(avgTxTime);
						nowBT.setTotalBlockTime(totalBlockTime);
						nowBT.setTotalTxCount(totalTxCount);

						nowBT.setCurTx(tx);
						nowBT.setMaxTx(Math.max(nowBT.getCurTx(), lastBT.getMaxTx()));
						daos.getOtcBrowserTotalDao().updateByPrimaryKey(nowBT);

						// 记录低速打块记录
						if (blockTime > config.slowBlockTime) {
							OTCBrowserSlowblock sb = new OTCBrowserSlowblock();
							sb.setBlockHeight(oMAINBlock.getHeight());
							sb.setTimestamp(oMAINBlock.getTimestamp());
							sb.setUsedTime(blockTime);
							daos.getOtcBrowserSlowblockDao().insert(sb);
						}
					}
					// 更新节点信息
					// 先判断节点名字是否存在
					if (oMAINBlock.getMinnerAccountAddress() != null
							&& !oMAINBlock.getMinnerAccountAddress().isEmpty()) {
						boolean isNewPeer = false;
						NETWORKPeer peer = null;
						// 先判断缓存中是否存在
						if (peerCaches.containsKey(oMAINBlock.getMinnerAccountAddress())) {
//							peer = peerCaches.get(oMAINBlock.getMinnerAccountAddress());
//							peer.setTotalSendTxCount(peer.getTotalSendTxCount() + oMAINBlock.getTransactionCount());
//							peer.setTotalBlockCount(peer.getTotalBlockCount() + 1);
//							// 如果是最新块，则更新打块时间
//							if (nowBT.getBlockHeight() == oMAINBlock.getHeight()) {
//								peer.setLastBlockTime(oMAINBlock.getTimestamp());
//							}
						} else {
							// 不存在则读取数据库
//							NETWORKPeerExample oNETWORKPeerExample = new NETWORKPeerExample();
//							oNETWORKPeerExample.createCriteria()
//									.andPeerAddressEqualTo(oMAINBlock.getMinnerAccountAddress());
//							peer = (NETWORKPeer) daos.getNetworkPeerDao().selectOneByExample(oNETWORKPeerExample);
//							if (peer == null) {
//								// 数据库不存在则进行创建
//								isNewPeer = true;
//								peer = new NETWORKPeer();
//								peer.setPeerAddress(oMAINBlock.getMinnerAccountAddress());
//								peer.setLastBlockTime(oMAINBlock.getTimestamp());
//								peer.setTotalSendTxCount(oMAINBlock.getTransactionCount().longValue());
//								peer.setTotalBlockCount(1L);
//								peer.setPeerUri("http://test.com");
//							} else {
//								peer.setTotalSendTxCount(peer.getTotalSendTxCount() + oMAINBlock.getTransactionCount());
//								peer.setTotalBlockCount(peer.getTotalBlockCount() + 1);
//								// 如果是最新块，则更新打块时间
//								if (nowBT.getBlockHeight() == oMAINBlock.getHeight()) {
//									peer.setLastBlockTime(oMAINBlock.getTimestamp());
//								}
//							}
							// 加入缓存
//							peerCaches.put(peer.getPeerAddress(), peer);
						}

//						// 存库
//						if (isNewPeer) {
//							daos.getNetworkPeerDao().insert(peer);
//						} else {
//							daos.getNetworkPeerDao().updateByPrimaryKey(peer);
//						}
					}

					// save块信息
					daos.getMainBlockDao().insert(oMAINBlock);
					// 交易主表
					for (MAINTransaction mt : listTxs) {
						daos.getMainTransactionDao().insert(mt);
					}

					// 交易转账信息表
					for (MAINTransactionTransfer mtt : listTxAccounts) {
						daos.getMainTransactionTransferDao().insert(mtt);
					}

					// 合约调用交易表
					for (MAINTransactionCallContract oMAINTransactionCallContract : listMAINTransactionCallContract) {
						daos.getMainTransactionCallContractDao().insert(oMAINTransactionCallContract);
					}

					// token主表
					if (!mapUMainToken.isEmpty()) {
						Iterator<Entry<String, MAINToken>> iter = mapUMainToken.entrySet().iterator();
						while (iter.hasNext()) {
							Entry<String, MAINToken> entry = iter.next();
							// 先判断是否是新建
							if (mapMainToken.containsKey(entry.getKey())) {
								daos.getMainTokenDao().insert(entry.getValue());
							} else {
								daos.getMainTokenDao().updateByPrimaryKey(entry.getValue());
							}
						}
					}

					// token操作记录表
					for (MAINTransactionToken oMAINTransactionToken : listMAINTransactionToken) {
						daos.getMainTransactionTokenDao().insert(oMAINTransactionToken);
					}

					// // 721主表
					// if(!mapMainCrypto.isEmpty())
					// {
					// Iterator<Entry<String, MAINCrypto>> iter =
					// mapMainCrypto.entrySet().iterator();
					// while(iter.hasNext())
					// {
					// Entry<String, MAINCrypto> entry = iter.next();
					// daos.getMainCryptoDao().insert(entry.getValue());
					// }
					// }
					//
					// // 721发行记录列表
					// for(MAINTransactionCrypto oMAINTransactionCrypto : listMAINTransactionCrypto)
					// {
					// daos.getMainTransactionCryptoDao().insert(oMAINTransactionCrypto);
					// }

					// 合约主表
					for (MAINContract oMAINContract : listMAINContract) {
						daos.getMainContractDao().insert(oMAINContract);
					}

					return null;
				}
			});
		}
	}

}
