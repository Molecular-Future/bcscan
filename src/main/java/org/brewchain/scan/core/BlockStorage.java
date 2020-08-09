package org.brewchain.scan.core;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.core.model.Block.BlockHeader;
import org.brewchain.core.model.Block.BlockInfo;
import org.brewchain.core.model.Block.BlockMiner;
import org.brewchain.core.model.Transaction.TransactionData;
import org.brewchain.core.model.Transaction.TransactionData.CallContractData;
import org.brewchain.core.model.Transaction.TransactionData.OwnerTokenData;
import org.brewchain.core.model.Transaction.TransactionData.PublicContractData;
import org.brewchain.core.model.Transaction.TransactionInfo;
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

import lombok.extern.slf4j.Slf4j;
import onight.tfw.ojpa.api.TransactionExecutor;

@Slf4j
public class BlockStorage implements Runnable {

	ScanDaos daos;
	ForkJoinPool mainPool = new ForkJoinPool(50);
	ScanConfig config;

	public BlockStorage(ScanDaos _daos, ScanConfig _config) {
		daos = _daos;
		config = _config;
	}

	/**
	 * 执行区块插入操作。如果已经存在（hash）存在，直接丢弃。如果不存在，执行插入操作。只做插入操作，不检查区块
	 * 
	 * @param block
	 */
	@Override
	public void run() {

		// 节点缓存
		Map<String, NETWORKPeer> peerCaches = new HashMap<String, NETWORKPeer>();

		while (true) {
			try {
//				if (!daos.isPaused()) {
				BlockInfo block = daos.oQueue.poll();
				while (block != null) {
					log.error("scan block =" + block.getHeader().getHeight() + " hash="
							+ daos.getCrypto().bytesToHexStr(block.getHeader().getHash().toByteArray()) + " txs="
							+ block.getHeader().getTxHashsCount() + " count=" + daos.oQueue.size() + " active="
							+ mainPool.getActiveThreadCount() + " pool=" + mainPool.getPoolSize());

					mainPool.submit(new BlockStorageThread(block, daos, config, peerCaches));
					block = daos.oQueue.poll();
				}
//				}
				Thread.sleep(100);
			} catch (Exception e) {
				log.error("" + e);
			}
		}
	}
}
