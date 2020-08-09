package org.brewchian.scan;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.brewchain.core.AccountHelper;
import org.brewchain.core.BlockChainHelper;
import org.brewchain.core.BlockHelper;
import org.brewchain.core.TransactionHelper;
import org.brewchain.core.config.ChainConfig;
import org.brewchain.core.cryptoapi.ICryptoHandler;
import org.brewchain.core.model.Block.BlockInfo;
import org.brewchain.scan.BlockSubscriber;
import org.brewchain.scan.browser.entity.OTCBrowserSlowblock;
import org.brewchain.scan.browser.entity.OTCBrowserTotal;
import org.brewchain.scan.main.entity.MAINAccountTokenBalance;
import org.brewchain.scan.main.entity.MAINBlock;
import org.brewchain.scan.main.entity.MAINContract;
import org.brewchain.scan.main.entity.MAINCrypto;
import org.brewchain.scan.main.entity.MAINToken;
import org.brewchain.scan.main.entity.MAINTransaction;
import org.brewchain.scan.main.entity.MAINTransactionCallContract;
import org.brewchain.scan.main.entity.MAINTransactionCrypto;
import org.brewchain.scan.main.entity.MAINTransactionToken;
import org.brewchain.scan.main.entity.MAINTransactionTransfer;
import org.brewchain.scan.network.entity.NETWORKPeer;
import org.brewchain.scan.sys.entity.SYSDict;
import org.fc.zippo.dispatcher.IActorDispatcher;

import lombok.Data;
import onight.osgi.annotation.NActorProvider;
import onight.osgi.annotation.iPojoBean;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.IJPAClient;
import onight.tfw.ojpa.api.OJpaDAO;
import onight.tfw.ojpa.api.annotations.StoreDAO;


@iPojoBean
@NActorProvider
@Provides(specifications = { ActorService.class, IJPAClient.class }, strategy = "SINGLETON")
@Instantiate(name = "scan_dao")
@Data
public class ScanDaos implements ActorService, IJPAClient {
	@ActorRequire(name = "bc_blockchain_helper", scope = "global")
	BlockChainHelper bcHelper;

	@ActorRequire(name = "bc_block_helper", scope = "global")
	BlockHelper blkHelper;

	@ActorRequire(name = "bc_transaction_helper", scope = "global")
	TransactionHelper txHelper;

	@ActorRequire(name = "bc_crypto", scope = "global")
	ICryptoHandler crypto;

	@ActorRequire(name = "bc_block_subscriber", scope = "global")
	BlockSubscriber blkSubscriber;

	@ActorRequire(name = "bc_chain_config", scope = "global")
	ChainConfig chainConfig;

	@ActorRequire(name = "zippo.ddc", scope = "global")
	IActorDispatcher ddc;

	@ActorRequire(name = "bc_account_helper", scope = "global")
	AccountHelper accountHelper;

//	boolean paused = false;
	
	public void setAccountHelper(AccountHelper _accountHelper) {
		accountHelper = _accountHelper;
	}

	public AccountHelper getAccountHelper() {
		return accountHelper;
	}

	public void setDdc(IActorDispatcher _ddc) {
		ddc = _ddc;
	}

	public IActorDispatcher getDdc() {
		return ddc;
	}

	public void setChainConfig(ChainConfig _chainConfig) {
		chainConfig = _chainConfig;
	}

	public ChainConfig getChainConfig() {
		return chainConfig;
	}

	public void setBcHelper(BlockChainHelper _bcHelper) {
		bcHelper = _bcHelper;
	}

	public BlockChainHelper getBcHelper() {
		return bcHelper;
	}

	public void setBlkHelper(BlockHelper _blkHelper) {
		blkHelper = _blkHelper;
	}

	public BlockHelper getBlkHelper() {
		return blkHelper;
	}

	public void setTxHelper(TransactionHelper _txHelper) {
		txHelper = _txHelper;
	}

	public TransactionHelper getTxHelper() {
		return txHelper;
	}

	public void setCrypto(ICryptoHandler _crypto) {
		crypto = _crypto;
	}

	public ICryptoHandler getCrypto() {
		return crypto;
	}

	public void setBlkSubscriber(BlockSubscriber _blkSubscriber) {
		blkSubscriber = _blkSubscriber;
	}

	public BlockSubscriber getBlkSubscriber() {
		return blkSubscriber;
	}

	public ConcurrentLinkedQueue<BlockInfo> oQueue = new ConcurrentLinkedQueue();

	@StoreDAO
	OJpaDAO<MAINBlock> mainBlockDao;
	@StoreDAO
	OJpaDAO<MAINTransaction> mainTransactionDao;
	@StoreDAO
	OJpaDAO<MAINTransactionTransfer> mainTransactionTransferDao;
	@StoreDAO
	OJpaDAO<MAINTransactionCallContract> mainTransactionCallContractDao;
	@StoreDAO
	OJpaDAO<MAINToken> mainTokenDao;
	@StoreDAO
	OJpaDAO<MAINTransactionToken> mainTransactionTokenDao;
	@StoreDAO
	OJpaDAO<MAINContract> mainContractDao;
	@StoreDAO
	OJpaDAO<SYSDict> sysDictDao;
	@StoreDAO
	OJpaDAO<MAINTransactionCrypto> mainTransactionCryptoDao;
	@StoreDAO
	OJpaDAO<MAINCrypto> mainCryptoDao;
	@StoreDAO
	OJpaDAO<MAINAccountTokenBalance> MainAccountTokenBalanceDao;
	@StoreDAO
	OJpaDAO<OTCBrowserTotal> otcBrowserTotalDao;
	@StoreDAO
	OJpaDAO<OTCBrowserSlowblock> otcBrowserSlowblockDao;
	@StoreDAO
	OJpaDAO<NETWORKPeer> networkPeerDao;
	
	
	@Override
	public void onDaoServiceAllReady() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDaoServiceReady(DomainDaoSupport arg0) {
		// TODO Auto-generated method stub
		
	}

}
