package org.brewchain.scan.core;

import org.brewchain.core.model.Block.BlockInfo;
import org.brewchain.scan.api.BlockObserver;
import org.brewchian.scan.ScanDaos;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BlockScanObserver implements BlockObserver {

	private ScanDaos _daos;
	public BlockScanObserver(ScanDaos daos) {
		this._daos = daos;
	}
	
	@Override
	public void onNotify(BlockInfo block) {
		_daos.oQueue.add(block);
	}

}
