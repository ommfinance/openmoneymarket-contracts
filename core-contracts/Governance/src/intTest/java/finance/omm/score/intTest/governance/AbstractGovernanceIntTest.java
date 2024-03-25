package finance.omm.score.intTest.governance;

import finance.omm.libs.address.Contracts;
import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;
import finance.omm.score.intTest.governance.config.GovernanceConfig;
import foundation.icon.jsonrpc.Address;
import org.junit.jupiter.api.BeforeAll;

import java.math.BigInteger;
import java.util.Map;

import static finance.omm.utils.math.MathUtils.ICX;

public class AbstractGovernanceIntTest implements ScoreIntegrationTest {
    protected static OMMClient ownerClient;
    protected static OMMClient alice;
    protected static OMMClient bob;

    protected static Map<String, Address> addressMap;

    protected final BigInteger time = BigInteger.valueOf(System.currentTimeMillis()/ 1000);



    @BeforeAll
    static void setup() throws Exception {
        OMM omm = new OMM("conf/all-contracts.json");

        omm.setupOMM();
        addressMap = omm.getAddresses();

        Config config = new GovernanceConfig();
        omm.runConfig(config);

        ownerClient = omm.defaultClient();
        alice = omm.newClient(BigInteger.TEN.pow(24));
        bob = omm.newClient(BigInteger.TEN.pow(24));

    }

    protected void rewardDistribution(){
        Address feeProvider = addressMap.get(Contracts.FEE_PROVIDER.getKey());
        ownerClient.reward.startDistribution();
        ownerClient.governance.enableRewardClaim();
        ownerClient.reward.distribute();

        ownerClient.governance.transferOmmToDaoFund(BigInteger.valueOf(4000_000).multiply(ICX));

        // owner has 3000_000 omm
        ownerClient.governance.transferOmmFromDaoFund(BigInteger.valueOf(3000_000).multiply(ICX),
                ownerClient.getAddress(),"toOwner".getBytes());

        ownerClient.governance.transferOmmFromDaoFund(BigInteger.valueOf(1000).multiply(ICX),
                feeProvider,"toOwner".getBytes());

    }



}
