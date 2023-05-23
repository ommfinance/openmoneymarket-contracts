package finance.omm.score.core.fee.distribution.integration;

import com.eclipsesource.json.JsonObject;
import finance.omm.libs.address.Contracts;
import finance.omm.libs.structs.PrepDelegations;
import finance.omm.libs.test.integration.Environment;
import finance.omm.libs.test.integration.OMM;
import finance.omm.libs.test.integration.OMMClient;
import finance.omm.libs.test.integration.ScoreIntegrationTest;
import finance.omm.libs.test.integration.configs.Config;
import finance.omm.libs.test.integration.scores.StakingScoreClient;
import finance.omm.score.core.fee.distribution.integration.config.feeDistributionConfig;
import foundation.icon.jsonrpc.Address;
import org.junit.jupiter.api.BeforeAll;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static finance.omm.utils.math.MathUtils.ICX;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AbstractFeeDistributionIT implements ScoreIntegrationTest {
    public static OMMClient ownerClient;
    public static OMMClient testClient;
    public static OMMClient alice;
    public static OMMClient balancedClient;

    public static OMMClient stakingTestClient;

    public static Map<String, Address> addressMap;
    public final BigInteger time = BigInteger.valueOf(System.currentTimeMillis() / 1000);
    public static Set<Map.Entry<score.Address, String>> contributor = Environment.contributors.entrySet();
    public static final Map<String, OMMClient> clientMap = new HashMap<>() {
    };

    boolean transfer = false;

    @BeforeAll
    public static void setup() throws Exception {
        OMM omm = new OMM("conf/all-contracts.json");

        omm.setupOMM();
        addressMap = omm.getAddresses();
        Config config = new feeDistributionConfig();
        omm.runConfig(config);
        ownerClient = omm.defaultClient();
        testClient = omm.testClient();
        balancedClient = omm.testClient();

        // loading contributors
        for (Object wallet : contributor.toArray()) {
            String privKey = wallet.toString().substring(43);
            alice = omm.customClient(privKey);
            System.out.println("prr ");
            System.out.println("prr " + privKey);
            clientMap.put(privKey, alice);
        }
        stakingTestClient = omm.newClient();
        ownerClient.staking.setOmmLendingPoolCore(addressMap.get(Contracts.LENDING_POOL_CORE.getKey()));


    }

    public final Map<String, Address> feeAddress = new HashMap<>() {{
        put("fee-1", Faker.address(Address.Type.EOA));
        put("fee-2", Faker.address(Address.Type.CONTRACT));
    }};

    protected void sendSicx(Address to, BigInteger amount) {
        ((StakingScoreClient) ownerClient.staking).stakeICX(amount, ownerClient.getAddress(), "stake".getBytes());

        ownerClient.sICX.transfer(to, amount, "transfer".getBytes());
    }

    protected void setOmmDelegations(OMMClient client, int n) {
        Address lendingPoolCore = addressMap.get(Contracts.LENDING_POOL_CORE.getKey());
        sendSicx(lendingPoolCore, BigInteger.TEN.multiply(ICX));
        assertEquals(BigInteger.ZERO,
                ownerClient.sICX.balanceOf(ownerClient.getAddress()));

        PrepDelegations[] prepDelegations = prepDelegations(n);
        client.delegation.updateDelegations(prepDelegations, client.getAddress());
    }

    protected PrepDelegations[] prepDelegations(int n) {
        score.Address[] prepSet = Environment.preps.keySet().toArray(score.Address[]::new);
        PrepDelegations[] delegations = new PrepDelegations[n];
        BigInteger total = BigInteger.ZERO;
        for (int i = 0; i < n; i++) {
            BigInteger votesInPer = ICX.divide(BigInteger.valueOf(n));
            score.Address prep = prepSet[i];

            total = total.add(votesInPer);

            BigInteger dustVote = ICX.subtract(total);

            if (dustVote.compareTo(BigInteger.ONE) <= 0) {
                votesInPer = votesInPer.add(dustVote);
            }
            PrepDelegations delegation = new PrepDelegations(prep, votesInPer);
            delegations[i] = delegation;

        }
        return delegations;
    }

    protected void rewardDistribution() {
        ownerClient.reward.startDistribution();
        ownerClient.governance.enableRewardClaim();
        ownerClient.reward.distribute();

        ownerClient.governance.transferOmmToDaoFund(BigInteger.valueOf(4000000).multiply(ICX));

        ownerClient.governance.transferOmmFromDaoFund(BigInteger.valueOf(40000).multiply(ICX), ownerClient.getAddress(),
                "reward".getBytes());
    }

    protected void transferOmm() {
        rewardDistribution();

        ownerClient.ommToken.transfer(testClient.getAddress(), BigInteger.valueOf(20000).multiply(ICX),
                "transfer to testClient".getBytes());
    }

    protected void userLockOMM(OMMClient client) {
        // testClientLocks OMM -> default delagation -> contributors

        if (!transfer) {
            transferOmm();

        }
        transfer = true;
        Address to = addressMap.get(Contracts.BOOSTED_OMM.getKey());
        BigInteger value = BigInteger.valueOf(10).multiply(ICX);

        BigInteger unlockTimeMicro = getTimeAfter(2);

        byte[] data = createByteArray("createLock", unlockTimeMicro);
        client.ommToken.transfer(to, value, data);
    }

    protected BigInteger getTimeAfter(int n) {
        BigInteger microSeconds = BigInteger.TEN.pow(6);
        BigInteger currentTime = BigInteger.valueOf((long) n * 86400 * 365).multiply(microSeconds);
        return time.multiply(microSeconds).add(currentTime);

    }

    protected byte[] createByteArray(String methodName, BigInteger unlockTime) {

        JsonObject internalParameters = new JsonObject()
                .add("unlockTime", String.valueOf(unlockTime));


        JsonObject jsonData = new JsonObject()
                .add("method", methodName)
                .add("params", internalParameters);

        return jsonData.toString().getBytes();
    }

}
