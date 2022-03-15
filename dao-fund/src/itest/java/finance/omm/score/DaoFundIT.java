package finance.omm.score;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.math.BigInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.icx.transport.jsonrpc.RpcItem;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import foundation.icon.test.Env;
import foundation.icon.test.ResultTimeoutException;
import foundation.icon.test.TestBase;
import foundation.icon.test.TransactionHandler;
import foundation.icon.test.score.Score;

public class DaoFundIT extends TestBase {

	private static Score daoFund;

	private static TransactionHandler txHandler;
	private static KeyWallet daoFundOwnerWallet;
	private static KeyWallet addressProviderWallet;
	private static Env.Chain chain = Env.getDefaultChain();
	private static IconService iconService;

	@BeforeAll
	static void init() throws Exception {
		iconService = new IconService(new HttpProvider(chain.getEndpointURL(3)));
		txHandler = new TransactionHandler(iconService, chain);

		// init wallets
		BigInteger amount = BigInteger.valueOf(500).multiply(ICX);

		daoFundOwnerWallet = KeyWallet.create();
		addressProviderWallet = KeyWallet.create();

		txHandler.transfer(daoFundOwnerWallet.getAddress(), amount);

		ensureIcxBalance(txHandler, daoFundOwnerWallet.getAddress(), BigInteger.ZERO, amount);

		daoFund = txHandler.deploy(daoFundOwnerWallet, Score.getFilePath("dao-fund"),
				new RpcObject.Builder()
				//looks like _addressProvider should be a score address instead of wallet's address
				.put("_addressProvider", new RpcValue(addressProviderWallet.getAddress()))
				.put("_update", new RpcValue(false))
				.build());

	}

	@AfterAll
	static void shutdown() throws Exception {
		txHandler.refundAll(daoFundOwnerWallet);
	}

	@Test
	void testGetAddressProvider() throws IOException, ResultTimeoutException{

		RpcItem addrItem = daoFund.call("getAddressProvider", new RpcObject.Builder().build());
		assertNotNull(addrItem);
		assertNotNull(addrItem.asAddress());
		assertEquals(addressProviderWallet.getAddress(), addrItem.asAddress());

	}
}
