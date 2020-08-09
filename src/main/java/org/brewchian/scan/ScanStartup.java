package org.brewchian.scan;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.brewchain.core.BlockChainHelper;
import org.brewchain.core.config.ChainConfig;
import org.brewchain.scan.BlockSubscriber;
import org.brewchain.scan.core.BalanceStorage;
import org.brewchain.scan.core.BlockScanObserver;
import org.brewchain.scan.core.BlockStorage;
import org.brewchain.scan.core.BlockValidator;
import org.brewchain.scan.manager.ScanConfig;
import org.brewchain.scan.sys.entity.SYSDict;
import org.brewchain.scan.sys.entity.SYSDictExample;
import org.fc.zippo.dispatcher.IActorDispatcher;

import com.google.protobuf.Message;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;

@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Slf4j
@Data
public class ScanStartup extends SessionModules<Message> {
	@ActorRequire(name = "scan_dao", scope = "global")
	ScanDaos scanDaos;
	@ActorRequire(name = "scan_config", scope = "global")
	ScanConfig scanConfig;

	@Override
	public String[] getCmds() {
		return new String[] { "___" };
	}

	@Override
	public String getModule() {
		return "SCAN";
	}

	@Validate
	public void startup() {
		try {
			new Thread(new ScanStartThread()).start();
		} catch (Exception e) {
			log.error("dao注入异常", e);
		}
	}

	class ScanStartThread extends Thread {
		@Override
		public void run() {
			try {
				while (scanDaos == null || !scanDaos.getChainConfig().isNodeStart()) {
					log.debug("等待core启动完成");
					Thread.sleep(5000);
				}
				// 注册观察者
				scanDaos.getBlkSubscriber().attach(new BlockScanObserver(scanDaos));
				// 加载运行信息

				SYSDictExample oAllDictExample = new SYSDictExample();
				oAllDictExample.createCriteria().andDictInitEqualTo(1);

				List<Object> dicts = scanDaos.getSysDictDao().selectByExample(oAllDictExample);
				for (Object objDict : dicts) {
					ScanInfo.parse((SYSDict) objDict);
				}
				
				// ScanInfo.lastConnectCheckBlockHeight = 0;
				// 启动定时任务，用于定时校验整个链的块是否连续，直到上一次连续块结束
				scanDaos.getDdc().scheduleWithFixedDelay(new BlockValidator(scanDaos), 1, 1, TimeUnit.MINUTES);
				
				// 启动定时任务，用于执行区块入库操作
				scanDaos.getDdc().getExecutorService("block_storage").execute(new BlockStorage(scanDaos, scanConfig));
				
				// 启动定时任务，用于更新账户货币信息。--闲时2秒同步一次，忙时100毫秒同步一次
				// scanDaos.getDdc().scheduleWithFixedDelay(new BalanceStorage(scanDaos), 2, 2, TimeUnit.SECONDS);
			} catch (Exception e) {
				log.error("dao注入异常", e);
			}
		}
	}
}
